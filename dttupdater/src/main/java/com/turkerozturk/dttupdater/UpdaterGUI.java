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
package com.turkerozturk.dttupdater;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.Date;

import static com.turkerozturk.dttupdater.Updater.UPDATE_LOG_FILE_NAME;

public class UpdaterGUI extends JFrame {

    private JTextArea logArea;
    private JCheckBox chkNoBackup;
    private JCheckBox chkNoUpdate;
    private JButton btnStart, btnCancel;

    private Path logFile;


    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new UpdaterGUI().setVisible(true));
    }

    public UpdaterGUI() {
        setTitle("Daily Topic Tracker Updater");
        setSize(600, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        chkNoBackup = new JCheckBox("Skip Backup (--nobackup)");
        chkNoUpdate = new JCheckBox("Skip Update (--noupdate)");
        topPanel.add(chkNoBackup);
        topPanel.add(chkNoUpdate);

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(logArea);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnStart = new JButton("Start Update");
        btnCancel = new JButton("Cancel & Quit");
        bottomPanel.add(btnCancel);
        bottomPanel.add(btnStart);

        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        // Log dosyası ayarla
        Path currentDir = Paths.get("").toAbsolutePath();
        logFile = currentDir.resolve(UPDATE_LOG_FILE_NAME);

        // Buton işlemleri
        btnCancel.addActionListener(e -> System.exit(0));
        btnStart.addActionListener(this::onStart);
    }

    private void onStart(ActionEvent e) {
        btnStart.setEnabled(false);
        btnCancel.setEnabled(false);
        new Thread(this::runUpdateProcess).start();
    }

    private void runUpdateProcess() {
        try {
            Path currentDir = Paths.get("").toAbsolutePath();
            appendLog("Working folder: " + currentDir);

            boolean skipBackup = chkNoBackup.isSelected();
            boolean skipUpdate = chkNoUpdate.isSelected();

            if (!skipBackup) {
                appendLog("Starting backup...");
                Updater.backupCurrentDir(currentDir, false, this::appendLog);
            } else {
                appendLog("Backup skipped (--nobackup).");
            }

            Path tempZip = null;
            Path tempDir = null;

            if (!skipUpdate) {
                appendLog("Downloading update...");
                tempZip = Files.createTempFile("dttupdate", ".zip");
                Updater.downloadFileWithLog(tempZip, this::appendLog);

                appendLog("Extracting zip...");
                tempDir = Files.createTempDirectory("dttunzip");
                Updater.unzipWithLog(tempZip, tempDir, this::appendLog);

                appendLog("Copying updated files...");
                Updater.copyUpdatedFiles(tempDir, currentDir, this::appendLog);
            } else {
                appendLog("Update skipped (--noupdate).");
            }

            // Temizlik
            if (tempZip != null && Files.exists(tempZip)) {
                Files.deleteIfExists(tempZip);
                appendLog("Temporary zip deleted.");
            }
            if (tempDir != null && Files.exists(tempDir)) {
                Files.walk(tempDir)
                        .sorted((a, b) -> b.compareTo(a)) // önce alt klasörler
                        .forEach(p -> {
                            try {
                                Files.deleteIfExists(p);
                            } catch (IOException ex) {
                                appendLog("Failed to delete " + p);
                            }
                        });
                appendLog("Temporary folder cleaned.");
            }

            appendLog("✅ Update process completed.");

            SwingUtilities.invokeLater(() -> askToLaunch(currentDir));

        } catch (Exception ex) {
            appendLog("❌ Update failed: " + ex.getMessage());
            ex.printStackTrace();
        } finally {
            SwingUtilities.invokeLater(() -> btnStart.setEnabled(true));
        }
    }

    private void askToLaunch(Path dir) {
        int option = JOptionPane.showConfirmDialog(this,
                "Update finished.\nDo you want to launch Daily Topic Tracker now?",
                "Update Completed",
                JOptionPane.YES_NO_OPTION);

        if (option == JOptionPane.YES_OPTION) {
            try {
                Updater.restartLaunchDTT(dir);
                appendLog("LaunchDTT started.");
            } catch (IOException ex) {
                appendLog("Failed to launch: " + ex.getMessage());
            }
        }

        System.exit(0);
    }

    private void appendLog(String msg) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        String line = "[" + timestamp + "] " + msg;
        SwingUtilities.invokeLater(() -> {
            logArea.append(line + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });

        try {
            Files.writeString(logFile, line + System.lineSeparator(),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {
        }
    }
}
