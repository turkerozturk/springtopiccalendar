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

import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.zip.*;



public class Updater {

    // Release ZIP dosyasının URL'si (GitHub Releases linki)
    private static final String DOWNLOAD_URL =
            "https://github.com/turkerozturk/springtopiccalendar/releases/latest/download/daily-topic-tracker.zip";

    public static String UPDATE_LOG_FILE_NAME = "updatedtt.log";

    public static String BACKUP_LOG_FILE_NAME = "backupdtt.log";



    public static void backupCurrentDir(Path dir, boolean isBackupDisabled, java.util.function.Consumer<String> logger) throws IOException {
        // Eğer yedekleme devre dışıysa çık
        if (isBackupDisabled) {
            log(dir, "Backup disabled via argument. Skipping backup.");
            return;
        }

        // backup/full klasörünü oluştur
        Path backupRoot = dir.resolve("backup");
        Path fullBackupDir = backupRoot.resolve("full");
        if (!Files.exists(fullBackupDir)) {
            Files.createDirectories(fullBackupDir);
            log(dir, "Created backup/full directory.");
        }

        // Exclude listeleri
        List<String> excludedFolders = List.of("backup", "nonessential", "JRE");
        List<String> excludedFiles = List.of(UPDATE_LOG_FILE_NAME);

        // ZIP dosyasının adı
        String date = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        Path backupZip = fullBackupDir.resolve("backupdtt-" + date + ".zip");

        // Disk alanını kontrol et
        long totalBytesToBackup = Files.walk(dir)
                .filter(Files::isRegularFile)
                .filter(path -> !isExcluded(path, dir, excludedFolders, excludedFiles))
                .mapToLong(path -> {
                    try {
                        return Files.size(path);
                    } catch (IOException e) {
                        return 0L;
                    }
                })
                .sum();

        FileStore store = Files.getFileStore(dir);
        long usableSpace = store.getUsableSpace();

        // %10 güvenlik payı bırakalım
        long requiredSpace = (long) (totalBytesToBackup * 1.1);
        if (usableSpace < requiredSpace) {
            log(dir, String.format("❌ Not enough disk space. Required: %.2f MB, Available: %.2f MB",
                    requiredSpace / 1_000_000.0, usableSpace / 1_000_000.0));
            return;
        }

        // Backup başlasın
        log(dir, "Starting backup to " + backupZip);
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(backupZip))) {
            Files.walk(dir)
                    .filter(Files::isRegularFile)
                    .filter(path -> !isExcluded(path, dir, excludedFolders, excludedFiles))
                    .forEach(path -> {
                        try {
                            String entryName = dir.relativize(path).toString();
                            zos.putNextEntry(new ZipEntry(entryName));
                            Files.copy(path, zos);
                            zos.closeEntry();
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
        }
        log(dir, "✅ Backup complete: " + backupZip);
    }

    private static boolean isExcluded(Path path, Path baseDir,
                                      List<String> excludedFolders, List<String> excludedFiles) {
        String relative = baseDir.relativize(path).toString().replace("\\", "/");
        for (String folder : excludedFolders) {
            if (relative.startsWith(folder + "/")) {
                return true;
            }
        }
        for (String file : excludedFiles) {
            if (relative.equalsIgnoreCase(file)) {
                return true;
            }
        }
        return false;
    }

    public  static void log(Path dir, String message) {
        try {
            Path logFile = dir.resolve("backup").resolve(BACKUP_LOG_FILE_NAME);
            Files.createDirectories(logFile.getParent());
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            Files.writeString(logFile, "[" + timestamp + "] " + message + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            System.out.println(message);
        } catch (IOException e) {
            System.err.println("Log yazılamadı: " + e.getMessage());
        }
    }





    /*
    public  static void downloadFile(String url, Path target) throws Exception {
        Downloader downloader = new Downloader();
        downloader.downloadFile(url, target);

    }

    public  static void unzip(Path zipFile, Path destDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile.toFile()))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path newFile = destDir.resolve(entry.getName()).normalize();
                if (entry.isDirectory()) {
                    Files.createDirectories(newFile);
                } else {
                    Files.createDirectories(newFile.getParent());
                    try (OutputStream os = Files.newOutputStream(newFile)) {
                        zis.transferTo(os);
                    }
                }
            }
        }
    }

    */

    public static void copyUpdatedFiles(Path sourceDir, Path targetDir, java.util.function.Consumer<String> logger) throws IOException {
        String[] importantFiles = {"LaunchDTT.jar",
                "LaunchDTT.exe",
                "daily-topic-tracker.jar",
                "version.json"
        };

        for (String fileName : importantFiles) {
            Path src = sourceDir.resolve(fileName);
            Path dest = targetDir.resolve(fileName);
            if (Files.exists(src)) {
                FileUtils.copyFile(src.toFile(), dest.toFile());
                System.out.println(fileName + " updated.");
            } else {
                System.out.println(fileName + " not found, skipping.");
            }
        }
    }

    public static void restartLaunchDTT(Path dir) throws IOException {
        ProcessBuilder pb = new ProcessBuilder("java", "-jar", "LaunchDTT.jar");
        pb.directory(dir.toFile());
        pb.start();
        System.out.println("LaunchDTT.jar has been restarted.");
    }

    /**
     * GitHub Releases’ta /latest/download/... URL’si HTTP 302 redirect doner,
     * cunku GitHub seni gercek dosyanin CDN (Amazon S3) adresine yonlendirir.
     * @param target
     * @throws Exception
     */
    public static void downloadFileWithLog(Path target, java.util.function.Consumer<String> logger) throws Exception {
        Downloader downloader = new Downloader();
        downloader.downloadFile(
                DOWNLOAD_URL,
                target,
                logger
        );
    }

    public static void unzipWithLog(Path zipFile, Path destDir, java.util.function.Consumer<String> logger) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile.toFile()))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path newFile = destDir.resolve(entry.getName()).normalize();
                if (entry.isDirectory()) {
                    Files.createDirectories(newFile);
                } else {
                    Files.createDirectories(newFile.getParent());
                    try (OutputStream os = Files.newOutputStream(newFile)) {
                        zis.transferTo(os);
                    }
                }
                logger.accept("Extracted: " + entry.getName());
            }
        }
    }



}
