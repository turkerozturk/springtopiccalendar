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
package com.turkerozturk.dttlauncher;
import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.*;
import java.util.Properties;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONObject;

/**
 * Commands to compile to jar and run:
 * javac LaunchDTT.java
 * jar cfe LaunchDTT.jar LaunchDTT *.class launcher-app-icon.png
 * java -jar LaunchDTT.jar
 * To not get "Could not find or load main class LaunchDTT" error while trying to run the jar file,
 * make sure that there is no "package " line in the beginning of this file. If it exists, delete it.
 */
public class LaunchDTT extends JFrame {
    // Durumlar
    enum AppStatus {
        STOPPED, STARTING, RUNNING, STOPPING
    }

    // Renkler (Swing'de RGB kullanıyoruz; AutoIt BGR idi)
    private static final Color COLOR_STOPPED  = Color.PINK;      // Kırmızı
    private static final Color COLOR_STARTING = Color.ORANGE;   // Turuncu
    private static final Color COLOR_RUNNING  = Color.GREEN;    // Yeşil
    private static final Color COLOR_STOPPING = Color.MAGENTA;  // Mor/Magenta

    // Parçalar
    private final JButton btnStartStop = new JButton("Start");
    private final JButton btnOpenApplicationFolder = new JButton("Open App Folder");

    private final JComboBox<String> cmbDatabaseList = new JComboBox<>();
    //private final JLabel lblAppVersion = new JLabel();
    //private final JTextArea textAreaLogs = new JTextArea();

    private final JButton btnOpenApplicationWebPage = new JButton("\uD83C\uDF10"); // unicode globe symbol
    private final JButton btnCopy = new JButton("📋"); // Unicode clipboard symbol
    private final JButton btnClear = new JButton("🗑"); // Unicode trash can
    // Uygulamanın port'u (application.properties'den okuyabilirsiniz ama burada sabit örnekliyoruz)
    private int serverPort;
    private String serverUrlScheme;
    private String dttVersion = "0.0.0";
    private boolean dttVersionCheck = true;

    private static final int DEFAULT_HTTP_PORT = 8080;
    private static final int DEFAULT_HTTPS_PORT = 8443;


    private void loadServerConfig() {
        String userDir = System.getProperty("user.dir");
        File propFile = new File(userDir, "application.properties");

        boolean sslEnabled = true; // default
        int httpPort = DEFAULT_HTTP_PORT;
        int httpsPort = DEFAULT_HTTPS_PORT;

        if (propFile.exists()) {
            Properties props = new Properties();
            try (FileInputStream in = new FileInputStream(propFile)) {
                props.load(in);

                dttVersionCheck = Boolean.parseBoolean(props.getProperty("version.check.new"));

                // read app.ssl.enabled
                String sslEnabledProp = props.getProperty("app.ssl.enabled");
                if (sslEnabledProp != null) {
                    sslEnabled = Boolean.parseBoolean(sslEnabledProp.trim());
                }

                // read server.http.port
                String httpPortProp = props.getProperty("server.http.port");
                if (httpPortProp != null) {
                    httpPort = Integer.parseInt(httpPortProp.trim());
                }

                // read server.port (https port)
                String httpsPortProp = props.getProperty("server.port");
                if (httpsPortProp != null) {
                    httpsPort = Integer.parseInt(httpsPortProp.trim());
                }


            } catch (IOException | NumberFormatException e) {
                appendLog("[WARN] Could not read application.properties, using defaults.\n");
            }
        }

        StringBuilder sb = new StringBuilder();
        if (sslEnabled) {
            this.serverPort = httpsPort;
            this.serverUrlScheme = "https://localhost:";
            sb.append("SSL is enabled on application.properties file. " + "\n");
        } else {
            this.serverPort = httpPort;
            this.serverUrlScheme = "http://localhost:";
            sb.append("SSL is disabled on application.properties file. " + "\n");
        }


        appendLog(sb + "The URL that will open after the application runs: " + serverUrlScheme + serverPort + "\n");
    }



    // Process
    private Process process;
    private volatile AppStatus currentStatus = AppStatus.STOPPED;

    // Thread/Scheduler
    private ScheduledExecutorService scheduler;
    private Future<?> logReaderFuture;
    private Future<?> healthCheckFuture;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            LaunchDTT frame = new LaunchDTT();
            frame.setVisible(true);
        });
    }

    public LaunchDTT() {

        super("Daily Topic Tracker Launcher"); // gecici baslik

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);

        loadServerConfig();

        logPane = new JTextPane();
        logPane.setEditable(false);
        logPane.setBackground(Color.BLACK);
        logPane.setForeground(Color.WHITE);
        Font font = new Font(Font.MONOSPACED, Font.PLAIN, 12);
        logPane.setFont(font);
        ((DefaultCaret) logPane.getCaret()).setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);


        styledDocument = logPane.getStyledDocument();

        JScrollPane scrollPane = new JScrollPane(logPane);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        addContextMenu(logPane); // adds right click context menu.

        try {
            // Local versiyonu oku
            String localVersion = getLocalVersion();
            appendLog("Current version: " + localVersion + "\n");
            this.setTitle("Daily Topic Tracker Launcher - DailyTopicTracker "
                    + localVersion + " - Turker Ozturk");

            if(dttVersionCheck) {

                // Remote versiyon bilgisi al
                JSONObject remoteInfo = fetchRemoteVersion();
                String remoteVersionDateAsString = remoteInfo.getString("date");
                String remoteVersion = remoteInfo.getString("version");
                String notes = remoteInfo.optString("notes", "")
                        .replace("\\r\\n", "\n")  // CRLF kaçışlarını da düzelt
                        .replace("\\n", "\n")
                        .replace("\\r", "\n");


                appendLog("Remote version: " + remoteVersion + "\n");

                if (isNewerVersion(localVersion, remoteVersion)) {

                    // Çok satırlı metin alanı oluştur
                    JTextArea textArea = new JTextArea(10, 40); // 10 satır, 40 sütun
                    textArea.setText(notes);
                    textArea.setWrapStyleWord(true);
                    textArea.setLineWrap(true);
                    textArea.setEditable(false);

                    // JScrollPane içinde kaydırılabilir hale getir
                    JScrollPane scrollPane2 = new JScrollPane(textArea);
                    scrollPane2.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
                    scrollPane2.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

                    // Mesaj içeriğini oluştur
                    Object[] message = {
                            "New version of DailyTopicTracker found: " + remoteVersion + " (" + remoteVersionDateAsString + ")",
                            "\nRelease Notes:",
                            scrollPane2,
                            "\nDo you want to update?"
                    };




                    // Evet / Hayır diyalogu göster
                    int choice = JOptionPane.showConfirmDialog(
                            null,
                            message,
                            "Update Daily Topic Tracker",
                            JOptionPane.YES_NO_OPTION
                    );


                    if (choice == JOptionPane.YES_OPTION) {
                        launchUpdater();
                        try {
                            Thread.sleep(1000); // 1 saniye updater başlasın diye sonra kendini kapatır.
                        } catch (InterruptedException ignored) {}
                        System.exit(0);
                    }

                } else {
                    appendLog("The application is up to date." + "\n");
                    // Burada esas uygulamayı başlatabilirsin (daily-topic-tracker.jar)
                    // ama manuel olarak butona tiklayarak baslatiyoruz. startApp();
                    // startApplication();
                }

            } else {
                appendLog("New version checking is disabled.\n" +
                        "If version.check.new=true is set in the application.properties file, " +
                        "it will check for an updated version online and, if approved, " +
                        "it will be downloaded and replaced with the new version." + "\n");

            }

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null,
                    "Update check failed!\n" + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            // Güncelleme başarısız olsa da uygulamayı yine başlatabilirsin
           // ama manuel olarak butona tiklayarak baslatiyoruz.
           // startApplication();
        }




        //serverPort = readServerPort();

        // loads *.db filenames into a list box if they exist. If not, you cannot see the list box.
        loadDbFileNames();

        // Pencere kapanırken Process'i durdur, Executor'u kapat
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {

                int result = JOptionPane.showConfirmDialog(
                        LaunchDTT.this,
                        "The application will be closed, stopping the web server while closing. Are you sure?",
                        "Closing confirmation",
                        JOptionPane.YES_NO_OPTION
                );

                if (result == JOptionPane.YES_OPTION) {
                    stopApplication();
                    if (scheduler != null && !scheduler.isShutdown()) {
                        scheduler.shutdownNow();
                    }                    dispose();
                    System.exit(0);
                }



            }
        });





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

        btnOpenApplicationWebPage.setToolTipText("Open " + serverUrlScheme + serverPort);
        btnOpenApplicationWebPage.addActionListener( e -> {

            openBrowser(serverUrlScheme + serverPort);

        });


        JLabel linkLabel = new JLabel("<html><a href=''>Open Website</a></html>");
        linkLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        linkLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    Desktop.getDesktop().browse(new URI("https://github.com/turkerozturk/springtopiccalendar"));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });



        btnCopy.setToolTipText("Copy logs to clipboard");
        btnCopy.addActionListener(e -> {
            StringSelection selection = new StringSelection(logPane.getText());
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(selection, null);

            // Temporary "Copied" message
            String oldText = btnCopy.getText();
            btnCopy.setText("✔ Copied!");
            Timer timer = new Timer(1500, ev -> btnCopy.setText(oldText));
            timer.setRepeats(false);
            timer.start();
        });

        btnClear.setToolTipText("Clear logs");
        btnClear.addActionListener(e -> logPane.setText(""));






        // Basit layout
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(btnStartStop);

        btnOpenApplicationFolder.addActionListener(e -> {
                onOpenDirectory(e);
        });
        topPanel.add(btnOpenApplicationFolder);

        topPanel.add(cmbDatabaseList);
        //topPanel.add(lblAppVersion);
        topPanel.add(linkLabel);

        JButton licenseButton = new JButton("License");
        licenseButton.addActionListener((ActionEvent e) -> {
            showLicenseDialog(this, "These LaunchDTT and DailyTopicTracker applications are part of the DailyTopicTracker project.\n" +
                    "Please refer to the project's README.md file for additional details.\n" +
                    "https://github.com/turkerozturk/springtopiccalendar\n" +
                    "\n" +
                    "Copyright (c) 2025 Turker Ozturk\n" +
                    "\n" +
                    "This program is free software: you can redistribute it and/or modify\n" +
                    "it under the terms of the GNU General Public License as published by\n" +
                    "the Free Software Foundation, either version 3 of the License, or\n" +
                    "(at your option) any later version.\n" +
                    "\n" +
                    "This program is distributed in the hope that it will be useful,\n" +
                    "but WITHOUT ANY WARRANTY; without even the implied warranty of\n" +
                    "MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the\n" +
                    "GNU General Public License for more details.\n" +
                    "\n" +
                    "You should have received a copy of the GNU General Public License\n" +
                    "along with this program. If not, see <https://www.gnu.org/licenses/gpl-3.0.en.html>.");
        });
        topPanel.add(licenseButton);

        topPanel.add(btnOpenApplicationWebPage);
        topPanel.add(btnCopy);
        topPanel.add(btnClear);

        // Ana panel
        setLayout(new BorderLayout());
        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        ImageIcon icon = new ImageIcon(getClass().getResource("/launcher-app-icon.png"));
        setIconImage(icon.getImage());


        // Thread scheduler (log okuma, health check vb. işlerde kullanacağız)
        scheduler = Executors.newScheduledThreadPool(2);


        if (isPortInUse(serverPort)) {
            appendLog("[ERROR] Port " + serverPort + " is already in use.\n");
            btnStartStop.setEnabled(false);
            cmbDatabaseList.setEnabled(false);
            StringBuilder sb = new StringBuilder();
            sb.append("You can try running the application with a different port number.");
            sb.append("\n");
            sb.append("To do that;");
            sb.append("\n");
            sb.append("Open the application.properties file with a text editor.");
            sb.append("\n");
            sb.append("Change the port number in the line starting with;");
            sb.append("\n");
            sb.append("server.port=");
            sb.append("\n");
            sb.append("and save it. Then run this application again.");
            sb.append("\n");
            sb.append("\n");
            sb.append("\n");
            sb.append("Finding out the port allocation of an application:");
            sb.append("\n");
            sb.append("From Windows commandline, type:");
            sb.append("\n");
            sb.append("netstat -ano | find \"" + serverPort + "\"");
            sb.append("\n");
            sb.append("Note down the corresponding process ID number.");
            sb.append("\n");
            sb.append("Go to Processes/Services in the Task Manager (for processes, select \"Show processes of all users\").");
            sb.append("\n");
            sb.append("\"Select View\\Columns -> Enable PID (Process ID)\".");
            sb.append("\n");
            sb.append("Now you can see which program or service uses which PID and you know what occupies the corresponding port.");
            sb.append("\n");
            sb.append("\n");
            sb.append("From Linux commandline, type:");
            sb.append("\n");
            sb.append("lsof -i :" + serverPort);
            sb.append("\n");
            sb.append("Note down the corresponding process ID number.");
            sb.append("\n");
            sb.append("To find which program is listening on port " + serverPort);
            sb.append("\n");
            sb.append("ps -fp WritePIDNumberHere");
            appendLog(sb.toString());

            return;
        }
    }

    private synchronized void startApplication() {
        if (process != null && process.isAlive()) {
            appendLog("[WARN] Application is already running.\n");
            return;
        }
        setStatus(AppStatus.STARTING);

        // Komut listesini hazırla
        List<String> cmd = new ArrayList<>();
        cmd.add("java");
        cmd.add("-jar");
        cmd.add("daily-topic-tracker.jar");

        // Eğer dbList görünür ve bir öğe seçiliyse, ek parametre olarak ekle
        if (cmbDatabaseList.isVisible()) {
            String selectedDb = cmbDatabaseList.getSelectedItem().toString();
            if (selectedDb != null && !selectedDb.isEmpty()) {
                cmd.add(selectedDb);
            }
        }

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true); // stdout + stderr

        // Process'i başlat
        try {
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
                // Actuator /health'e GET isteği atıp, "status":"UP" içeriyorsa true döner
                HealthChecker healthChecker = new HealthChecker();

                boolean up = healthChecker.isApplicationUp(serverUrlScheme + serverPort + "/actuator/health");
                if (up) {
                    appendLog("[INFO] Application is UP. Opening browser...\n");
                    setStatus(AppStatus.RUNNING);
                    // Tarayıcı aç (isteğe bağlı; Windows)
                    openBrowser(serverUrlScheme + serverPort);
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
                    cmbDatabaseList.setEnabled(true);
                    break;
                case STARTING:
                    btnStartStop.setText("Starting...");
                    btnStartStop.setBackground(COLOR_STARTING);
                    cmbDatabaseList.setEnabled(false);
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






    // Metin alanına log ekleme (Swing thread'i üzerinden)
    private void appendLog(String text) {
        //SwingUtilities.invokeLater(() -> textAreaLogs.append(text));
        SwingUtilities.invokeLater(() -> addStringLineToLog(text));
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


    private void onOpenDirectory(ActionEvent e) {
        // Çalışma dizinini alıyoruz:
        File dir = new File(".").getAbsoluteFile();

        // 1) Desktop API ile dene
        if (Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(Desktop.Action.OPEN)) {
                try {
                    desktop.open(dir);
                    return;
                } catch (IOException ex) {
                    // bir sorun oluştu, fallback yapacağız
                }
            }
        }

        // 2) Fallback: OS'e göre komut çalıştır
        String os = System.getProperty("os.name").toLowerCase();
        Runtime rt = Runtime.getRuntime();
        try {
            if (os.contains("win")) {
                // Windows Explorer
                rt.exec(new String[]{"explorer.exe", dir.getAbsolutePath()});
            } else if (os.contains("mac")) {
                // macOS Finder
                rt.exec(new String[]{"open", dir.getAbsolutePath()});
            } else {
                // Muhtemelen Linux; xdg-open genelde yüklüdür
                rt.exec(new String[]{"xdg-open", dir.getAbsolutePath()});
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "Dosya yöneticisi açılamadı:\n" + ex.getMessage(),
                    "Hata", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadDbFileNames() {
        File dir = new File(System.getProperty("user.dir")); // Uygulamanın çalıştığı klasör
        File[] dbFiles = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".db"));

        if (dbFiles != null && dbFiles.length > 0) {
            Arrays.sort(dbFiles, Comparator.comparing(File::getName));
            DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
            for (File f : dbFiles) {
                model.addElement(f.getName());
            }
            cmbDatabaseList.setModel(model);
            cmbDatabaseList.setVisible(true);
        } else {
            // Hiç .db yoksa tamamen gizle
            cmbDatabaseList.setVisible(false);
        }


    }




    private boolean isPortInUse(int port) {
        try (ServerSocket socket = new ServerSocket(port)) {
            return false; // Port boş
        } catch (IOException e) {
            return true; // Port kullanımda
        }
    }

    private static void showLicenseDialog(JFrame parent, String licenseText) {
        JDialog dialog = new JDialog(parent, "License", true); // modal

        JTextArea textArea = new JTextArea(licenseText);
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(400, 300));

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dialog.dispose());

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(closeButton);

        dialog.getContentPane().add(scrollPane, BorderLayout.CENTER);
        dialog.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }

    /**
     * secili log satirlarini panoya kopyalamak icin sag tus context menu
     * @param textPane
     */
    private void addContextMenu(JTextPane textPane) {
        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem copyItem = new JMenuItem("Copy");
        popupMenu.add(copyItem);

        copyItem.addActionListener(e -> {
            String selectedText = textPane.getSelectedText();
            if (selectedText != null && !selectedText.isEmpty()) {
                StringSelection selection = new StringSelection(selectedText);
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(selection, null);
            }
        });

        // Mouse listener to trigger popup on right click
        textPane.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                showPopupIfTriggered(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                showPopupIfTriggered(e);
            }

            private void showPopupIfTriggered(MouseEvent e) {
                if (e.isPopupTrigger() && textPane.getSelectedText() != null) {
                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
    }



    // code below is for multi colored logs
    private final JTextPane logPane;
    private final StyledDocument styledDocument;

    private void addStringLineToLog(String line) {
        if (line.trim().isEmpty()) {
            appendToStyledDocument(line + "\n", Color.WHITE);
            return;
        }

        // Tarihle başlayan satır
        Pattern datePattern = Pattern.compile("^(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d+([+-]\\d{2}:?\\d{2})?)\\s+(INFO|WARN|DEBUG|ERROR|TRACE)");
        Matcher matcher = datePattern.matcher(line);

        if (matcher.find()) {
            String datePart = matcher.group(1);
            String level = matcher.group(3);

            int levelStart = line.indexOf(level, datePart.length());
            int messageStart = levelStart + level.length();

            appendToStyledDocument(datePart, Color.GRAY);
            appendToStyledDocument(" " + level, getLevelColor(level));
            appendToStyledDocument(line.substring(messageStart) + "\n", Color.WHITE);
            return;
        }

        // [LEVEL] formatı
        Pattern tagPattern = Pattern.compile("^\\[(INFO|WARN|ERROR|DEBUG|TRACE)]");
        matcher = tagPattern.matcher(line);
        if (matcher.find()) {
            String level = matcher.group(1);
            int levelEnd = matcher.end();
            appendToStyledDocument(line.substring(0, levelEnd), getLevelColor(level));
            appendToStyledDocument(line.substring(levelEnd) + "\n", Color.WHITE);
            return;
        }

        // WARNING: ... veya ERROR: ... gibi
        Pattern colonPattern = Pattern.compile("^(INFO|WARN|ERROR|DEBUG|TRACE):");
        matcher = colonPattern.matcher(line);
        if (matcher.find()) {
            String level = matcher.group(1);
            int levelEnd = matcher.end();
            appendToStyledDocument(line.substring(0, levelEnd), getLevelColor(level));
            appendToStyledDocument(line.substring(levelEnd) + "\n", Color.WHITE);
            return;
        }

        // Diğerleri beyaz ve \n olmadan daha iyi:
        appendToStyledDocument(line, Color.WHITE);
    }

    private void appendToStyledDocument(String text, Color color) {
        Style style = logPane.addStyle("Style", null);
        StyleConstants.setForeground(style, color);
        try {
            styledDocument.insertString(styledDocument.getLength(), text, style);
        } catch (BadLocationException ex) {
            ex.printStackTrace();
        }
    }

    private Color getLevelColor(String level) {
        return switch (level.toUpperCase()) {
            case "INFO" -> new Color(0x00FF00);     // Yeşil
            case "WARN", "WARNING" -> new Color(0xFFA500); // Turuncu
            case "ERROR" -> Color.RED;
            case "DEBUG" -> Color.CYAN;
            case "TRACE" -> Color.LIGHT_GRAY;
            default -> Color.WHITE;
        };
    }

    // codes below are related with updating the software


    private static final String VERSION_URL =
            "https://raw.githubusercontent.com/turkerozturk/springtopiccalendar/main/version.json";



    private String getLocalVersion() throws IOException {
        Path versionFile = Paths.get("version.json");
        if (Files.exists(versionFile)) {
            String jsonText = Files.readString(versionFile, StandardCharsets.UTF_8);
            try {
                JSONObject localInfo = new JSONObject(jsonText);
                if (localInfo.has("version")) {
                    this.dttVersion = localInfo.getString("version");
                }
            } catch (Exception e) {
                appendLog("⚠️ Local version.json file does not exist, could not be read, or invalid JSON: " + e.getMessage());
            }
        }
        return this.dttVersion;
    }

    private JSONObject fetchRemoteVersion() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest req = HttpRequest.newBuilder()
                .uri(new URI(VERSION_URL))
                .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        if (resp.statusCode() != 200) {
            throw new IOException("Could not get " + VERSION_URL + "! HTTP " + resp.statusCode());
        }
        return new JSONObject(resp.body());
    }

    private boolean isNewerVersion(String local, String remote) {
        String[] lv = local.split("\\.");
        String[] rv = remote.split("\\.");
        for (int i = 0; i < Math.max(lv.length, rv.length); i++) {
            int li = (i < lv.length) ? Integer.parseInt(lv[i]) : 0;
            int ri = (i < rv.length) ? Integer.parseInt(rv[i]) : 0;
            if (ri > li) return true;
            if (ri < li) return false;
        }
        return false;
    }

    private void launchUpdater() throws IOException {
        // Çalışan jar'ın bulunduğu dizini al
        Path currentDir = Paths.get(System.getProperty("user.dir"));
        File updaterJar = new File(currentDir.toFile(), "dttupdater.jar");

        if (!updaterJar.exists()) {
            JOptionPane.showMessageDialog(null,
                    "Updater file not found: " + updaterJar.getAbsolutePath(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        ProcessBuilder pb = new ProcessBuilder("java", "-jar", updaterJar.getAbsolutePath());
        pb.directory(currentDir.toFile());
        pb.inheritIO(); // konsol çıktılarını aynen aktarır (isteğe bağlı)
        pb.start();

        appendLog("The updater has been started from: " + updaterJar.getAbsolutePath() + "\n");
    }







}
