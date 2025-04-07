/*
 * This file is part of the DailyTopicTracker project.
 * Please refer to the project's README.md file for additional details.
 * https://github.com/turkerozturk/springtopiccalendar
 *
 * Copyright (c) 2025 Turker Ozturk
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/gpl-3.0.en.html>.
 */
import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.*;

/**
 * Commands to compile to jar and run:
 * javac LaunchDTT.java
 * jar cfe LaunchDTT.jar LaunchDTT *.class
 * java -jar LaunchDTT.jar
 */
public class LaunchDTT extends JFrame {
    // Durumlar
    enum AppStatus {
        STOPPED, STARTING, RUNNING, STOPPING
    }

    // Renkler (Swing'de RGB kullanıyoruz; AutoIt BGR idi)
    private static final Color COLOR_STOPPED  = Color.RED;      // Kırmızı
    private static final Color COLOR_STARTING = Color.ORANGE;   // Turuncu
    private static final Color COLOR_RUNNING  = Color.GREEN;    // Yeşil
    private static final Color COLOR_STOPPING = Color.MAGENTA;  // Mor/Magenta

    // Parçalar
    private final JButton btnStartStop = new JButton("Start");
    private final JTextArea textAreaLogs = new JTextArea();

    // Uygulamanın port'u (application.properties'den okuyabilirsiniz ama burada sabit örnekliyoruz)
    private int serverPort = 8080;

    // Process
    private Process process;
    private volatile AppStatus currentStatus = AppStatus.STOPPED;

    // Thread/Scheduler
    private ScheduledExecutorService scheduler;
    private Future<?> logReaderFuture;
    private Future<?> healthCheckFuture;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            DailyTrackerLauncher frame = new LaunchDTT();
            frame.setVisible(true);
        });
    }

    public LaunchDTT() {
        super("Daily Topic Tracker Control");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);

        // Pencere kapanırken Process'i durdur, Executor'u kapat
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                stopApplication();
                if (scheduler != null && !scheduler.isShutdown()) {
                    scheduler.shutdownNow();
                }
                dispose();
                System.exit(0);
            }
        });

        // Log metin alanı ayarları
        textAreaLogs.setEditable(false);
        // Otomatik en alta kayması için:
        ((DefaultCaret) textAreaLogs.getCaret()).setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        JScrollPane scrollPane = new JScrollPane(textAreaLogs);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        // Start/Stop butonu
        btnStartStop.setBackground(COLOR_STOPPED);
        btnStartStop.addActionListener(e -> {
            if (currentStatus == AppStatus.STOPPED) {
                startApplication();
            } else if (currentStatus == AppStatus.RUNNING) {
                stopApplication();
            } else {
                // STARTING veya STOPPING durumunda tıklandıysa
                appendLog("[INFO] Already in " + currentStatus + " state.\n");
            }
        });

        // Basit layout
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(btnStartStop);

        // Ana panel
        setLayout(new BorderLayout());
        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        // Thread scheduler (log okuma, health check vb. işlerde kullanacağız)
        scheduler = Executors.newScheduledThreadPool(2);
    }

    private synchronized void startApplication() {
        if (process != null && process.isAlive()) {
            appendLog("[WARN] Application is already running.\n");
            return;
        }
        setStatus(AppStatus.STARTING);

        // Process'i başlat
        try {
            ProcessBuilder pb = new ProcessBuilder("java", "-jar", "daily-topic-tracker-1.0.0.jar");
            pb.redirectErrorStream(true); // stdout + stderr
            process = pb.start();
        } catch (IOException ex) {
            appendLog("[ERROR] Failed to start process: " + ex + "\n");
            setStatus(AppStatus.STOPPED);
            return;
        }

        appendLog("[INFO] Application starting...\n");

        // Log okuma işini bir Runnable ile
        logReaderFuture = scheduler.submit(() -> {
            try (InputStream is = process.getInputStream();
                 BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
                String line;
                while ((line = br.readLine()) != null) {
                    // Uygulamanın log satırlarını textArea'ya basalım (EDT üzerinden)
                    appendLog(line + "\n");
                }
            } catch (IOException e) {
                appendLog("[ERROR] Reading process output failed: " + e + "\n");
            }
            // Process bittiğinde
            appendLog("[INFO] Process ended.\n");
            setStatus(AppStatus.STOPPED);
        });

        // Health check: periyodik /actuator/health sorgusu
        healthCheckFuture = scheduler.scheduleAtFixedRate(() -> {
            // Eğer process ölürse, STOPPED'e geçelim
            if (process != null && !process.isAlive() && currentStatus != AppStatus.STOPPED) {
                appendLog("[INFO] Process is not alive. Stopping.\n");
                setStatus(AppStatus.STOPPED);
                return;
            }
            // Eğer STARTING moddaysak actuator'a bak
            if (currentStatus == AppStatus.STARTING) {
                boolean up = isApplicationUp("http://localhost:" + serverPort + "/actuator/health");
                if (up) {
                    appendLog("[INFO] Application is UP. Opening browser...\n");
                    setStatus(AppStatus.RUNNING);
                    // Tarayıcı aç (isteğe bağlı; Windows)
                    openBrowser("http://localhost:" + serverPort);
                }
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    private synchronized void stopApplication() {
        if (currentStatus == AppStatus.STOPPED) {
            appendLog("[INFO] Application already stopped.\n");
            return;
        }
        setStatus(AppStatus.STOPPING);

        // Process kapat
        if (process != null && process.isAlive()) {
            process.destroy();
            // Gerekirse destroyForcibly() de kullanılabilir.
        }
        appendLog("[INFO] Stopping application...\n");

        // Biraz bekleyelim ki process sonlansın (async)
        scheduler.schedule(() -> setStatus(AppStatus.STOPPED), 1, TimeUnit.SECONDS);
    }

    private void setStatus(AppStatus newStatus) {
        SwingUtilities.invokeLater(() -> {
            currentStatus = newStatus;
            switch (newStatus) {
                case STOPPED:
                    btnStartStop.setText("Start");
                    btnStartStop.setBackground(COLOR_STOPPED);
                    break;
                case STARTING:
                    btnStartStop.setText("Starting...");
                    btnStartStop.setBackground(COLOR_STARTING);
                    break;
                case RUNNING:
                    btnStartStop.setText("Stop");
                    btnStartStop.setBackground(COLOR_RUNNING);
                    break;
                case STOPPING:
                    btnStartStop.setText("Stopping...");
                    btnStartStop.setBackground(COLOR_STOPPING);
                    break;
            }
        });
    }

    // Actuator /health'e GET isteği atıp, "status":"UP" içeriyorsa true döner
    private boolean isApplicationUp(String healthUrl) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(healthUrl).openConnection();
            conn.setConnectTimeout(1000);
            conn.setReadTimeout(2000);
            int code = conn.getResponseCode();
            if (code == 200) {
                // Yanıt metnini oku
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line);
                    }
                    // "status":"UP"
                    if (sb.indexOf("\"status\":\"UP\"") >= 0) {
                        return true;
                    }
                }
            }
        } catch (IOException e) {
            // Sessiz geçebiliriz, demek ki UP değil ya da bağlantı hatası
        }
        return false;
    }

    // Metin alanına log ekleme (Swing thread'i üzerinden)
    private void appendLog(String text) {
        SwingUtilities.invokeLater(() -> textAreaLogs.append(text));
    }

    // Varsayılan tarayıcıda URL açma (Windows ortamında)
    private void openBrowser(String url) {
        // Her platformda %SystemRoot%\system32\rundll32.exe url.dll,FileProtocolHandler URL
        try {
            Runtime.getRuntime().exec(new String[]{"rundll32", "url.dll,FileProtocolHandler", url});
        } catch (IOException e) {
            appendLog("[ERROR] Cannot open browser: " + e + "\n");
        }
    }
}
