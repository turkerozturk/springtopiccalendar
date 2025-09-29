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
        if (relative == null || relative.isBlank()) {
            return baseDir;
        }
        Path target = baseDir.resolve(relative).normalize();
        if (!target.startsWith(baseDir)) {
            throw new SecurityException("Invalid path");
        }
        return target;
    }

    public List<FileInfo> list(String relativePath) throws IOException {
        Path dir = resolveSafe(relativePath);
        if (!Files.isDirectory(dir)) throw new NoSuchFileException("Not a directory");

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            List<FileInfo> list = new ArrayList<>();
            for (Path p : stream) {
                FileInfo fi = new FileInfo();
                fi.setName(p.getFileName().toString());
                fi.setRelativePath(baseDir.relativize(p).toString().replace("\\","/"));
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

