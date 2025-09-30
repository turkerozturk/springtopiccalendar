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
package com.turkerozturk.dtt.service.upload;

// FileService.java
import com.turkerozturk.dtt.dto.upload.FileInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FileService {

    private final Path baseDir;

    public Path getBaseDir() {
        return baseDir;
    }

    public FileService(@Value("${filemanager.base-dir}") String baseDirStr) {
        this.baseDir = Paths.get(baseDirStr).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.baseDir);
        } catch (IOException e) {
            throw new RuntimeException("Could not create base directory: " + this.baseDir, e);
        }
    }

    private Path resolveSafe(String relative) {
        //System.out.println("FileService.java icindeki resolveSafe metoduna gelen deger: "+ relative);

        if (relative == null || relative.isBlank()) {
            //System.out.println("donen deger baseDir: " + baseDir);
            return baseDir;
        }
        Path target = baseDir.resolve(relative).normalize();
        if (!target.startsWith(baseDir)) {
            throw new SecurityException("Invalid path");
        }
        //System.out.println("donen deger target: " + target);
        return target;
    }

    public List<FileInfo> list(String relativePath) throws IOException {
        Path dir = resolveSafe(relativePath);
        if(!Files.exists(dir)) throw new NoSuchFileException("Path '" + dir + "' not exist");
        if (!Files.isDirectory(dir)) throw new NoSuchFileException("Not a directory");

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            List<FileInfo> list = new ArrayList<>();
            for (Path p : stream) {
                FileInfo fi = new FileInfo();
                fi.setName(p.getFileName().toString());
                String fileRelativePath = baseDir.relativize(p).toString().replace("\\","/");
                //System.out.println("fileRelativePath: " + fileRelativePath);
                fi.setRelativePath(fileRelativePath);
                fi.setDirectory(Files.isDirectory(p));
                fi.setSizeBytes(Files.isDirectory(p) ? 0L : Files.size(p));
                fi.setSizeKbFormatted(Files.isDirectory(p) ? "-" : String.format("%.1f", Files.size(p)/1024.0));
                fi.setLastModified(Files.getLastModifiedTime(p).toMillis());
                list.add(fi);
            }
            // Sort: directories first then files, both alphabetically
            list.sort(Comparator
                    .comparing(FileInfo::isDirectory).reversed()
                    .thenComparing(f -> f.getName().toLowerCase(Locale.ROOT)));
            return list;
        }
    }

    public void createFolder(String relativeParent, String folderName) throws IOException {
        if (folderName == null || folderName.isBlank()) throw new IllegalArgumentException("Invalid name");
        Path parent = resolveSafe(relativeParent);
        Path newFolder = parent.resolve(folderName).normalize();
        if (!newFolder.startsWith(baseDir)) throw new SecurityException("Invalid path");
        if (Files.exists(newFolder)) throw new FileAlreadyExistsException("Exists");
        Files.createDirectory(newFolder);
    }

    public void delete(String relativePath) throws IOException {
        Path target = resolveSafe(relativePath);
        if (!Files.exists(target)) throw new NoSuchFileException("Not exists");
        if (Files.isDirectory(target)) {
            // recursive delete
            Files.walk(target)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(java.io.File::delete);
        } else {
            Files.delete(target);
        }
    }

    public void rename(String relativePath, String newName) throws IOException {
        Path target = resolveSafe(relativePath);
        Path resolvedNew = target.resolveSibling(newName).normalize();
        if (!resolvedNew.startsWith(baseDir)) throw new SecurityException("Invalid new path");
        if (Files.exists(resolvedNew)) throw new FileAlreadyExistsException("Exists");
        Files.move(target, resolvedNew);
    }

    public void move(String relativeSource, String relativeDestDir) throws IOException {
        Path source = resolveSafe(relativeSource);
        Path destDir = resolveSafe(relativeDestDir);
        if (!Files.isDirectory(destDir)) throw new NotDirectoryException("Destination not directory");
        Path dest = destDir.resolve(source.getFileName()).normalize();
        if (Files.exists(dest)) throw new FileAlreadyExistsException("Exists at dest");
        Files.move(source, dest);
    }

    public void store(MultipartFile file, String relativeTargetDir) throws IOException {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Empty");
        long max = 300L * 1024L;
        if (file.getSize() > max) throw new IOException("TOO_LARGE");
        String filename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        if (filename.contains("..")) throw new SecurityException("Invalid filename");
        Path targetDir = resolveSafe(relativeTargetDir);
        if (!Files.isDirectory(targetDir)) Files.createDirectories(targetDir);
        Path target = targetDir.resolve(filename);
        if (Files.exists(target)) throw new FileAlreadyExistsException("Exists");
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
    }

    public Resource loadAsResource(String relativePath) throws MalformedURLException {
        Path file = resolveSafe(relativePath);
        if (!Files.exists(file) || Files.isDirectory(file)) return null;
        Resource resource = new UrlResource(file.toUri());
        if (resource.exists() && resource.isReadable()) return resource;
        else return null;
    }
}

