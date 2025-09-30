package com.turkerozturk.dtt.controller.upload;

// FileManagerController.java
import com.turkerozturk.dtt.dto.upload.FileInfo;
import com.turkerozturk.dtt.service.upload.FileService;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/file-manager")
public class FileManagerController {

    private final FileService fileService;

    public FileManagerController(FileService fileService) {
        this.fileService = fileService;
    }


    @GetMapping
    public String index(@RequestParam(value="path", required=false) String path, Model model) throws IOException {
        Path basePath = fileService.getBaseDir();
        Path currentPath = (path == null || path.isEmpty())
                ? basePath
                : basePath.resolve(path).normalize();

        // güvenlik
        if (!currentPath.startsWith(basePath)) {
            currentPath = basePath;
            path = "";
        }

        String safePath = (path == null) ? "" : path;
        List<FileInfo> list = fileService.list(safePath);

        // parentPath hesapla
        String parentPath = "";
        if (!safePath.isEmpty()) {
            Path parent = currentPath.getParent();
            if (parent != null && parent.startsWith(basePath)) {
                parentPath = basePath.relativize(parent).toString().replace("\\", "/");
            }
        }

        model.addAttribute("files", list);
        model.addAttribute("currentPath", safePath.replace("\\","/"));
        model.addAttribute("parentPath", parentPath);

        return "file-manager";
    }



    @GetMapping("/list")
    @ResponseBody
    public ResponseEntity<?> listJson(@RequestParam(value="path", required=false) String path) {
        try {
            Path basePath = fileService.getBaseDir();
            Path currentPath = (path == null || path.isEmpty())
                    ? basePath
                    : basePath.resolve(path).normalize();

            if (!currentPath.startsWith(basePath)) {
                currentPath = basePath;
                path = "";
            }

            String safePath = (path == null) ? "" : path;
            List<FileInfo> list = fileService.list(safePath);

            // parentPath hesapla
            String parentPath = "";
            if (!safePath.isEmpty()) {
                Path parent = currentPath.getParent();
                if (parent != null && parent.startsWith(basePath)) {
                    parentPath = basePath.relativize(parent).toString().replace("\\", "/");
                }
            }
            System.out.println("FileManagerController listJson metodundaki currentPath: " + currentPath);

            return ResponseEntity.ok(Map.of(
                    "files", list,
                    "currentPath", safePath,
                    "parentPath", parentPath
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }


    @PostMapping("/create-folder")
    @ResponseBody
    public ResponseEntity<?> createFolder(@RequestParam String parent, @RequestParam String name) {
        try {
            fileService.createFolder(parent, name);
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (FileAlreadyExistsException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "exists"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/upload")
    @ResponseBody
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file,
                                    @RequestParam(value="target", required=false) String target) {
        try {
            fileService.store(file, target);
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (FileAlreadyExistsException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "exists"));
        } catch (IOException ex) {
            if ("TOO_LARGE".equals(ex.getMessage())) {
                return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(Map.of("error", "too_large"));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", ex.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/delete")
    @ResponseBody
    public ResponseEntity<?> delete(@RequestParam String path) {
        try {
            fileService.delete(path);
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (NoSuchFileException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "not_found"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/rename")
    @ResponseBody
    public ResponseEntity<?> rename(@RequestParam String path, @RequestParam String newName) {
        try {
            fileService.rename(path, newName);
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (FileAlreadyExistsException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "exists"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/move")
    @ResponseBody
    public ResponseEntity<?> move(@RequestParam String src, @RequestParam String destDir) {
        try {
            fileService.move(src, destDir);
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (FileAlreadyExistsException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "exists"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/preview")
    public ResponseEntity<Resource> preview(@RequestParam String path) throws MalformedURLException {
        Resource resource = fileService.loadAsResource(path);
        if (resource == null) return ResponseEntity.notFound().build();
        String contentType = "application/octet-stream";
        try {
            contentType = Files.probeContentType(resource.getFile().toPath());
        } catch (Exception ignored) {}
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType));
        // for inline preview
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"");
        return new ResponseEntity<>(resource, headers, HttpStatus.OK);
    }

    @GetMapping("/download")
    public ResponseEntity<Resource> download(@RequestParam String path) throws MalformedURLException {
        Resource resource = fileService.loadAsResource(path);
        if (resource == null) return ResponseEntity.notFound().build();
        String contentType = "application/octet-stream";
        try {
            contentType = Files.probeContentType(resource.getFile().toPath());
        } catch (Exception ignored) {}
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.setContentDisposition(ContentDisposition.attachment().filename(resource.getFilename()).build());
        return new ResponseEntity<>(resource, headers, HttpStatus.OK);
    }
}
