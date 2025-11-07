package com.turkerozturk.dtt.controller.export;

import com.turkerozturk.dtt.service.CategoryGroupExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.nio.file.Path;
import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/export")
public class ExportController {

    private final CategoryGroupExportService exportService;

    @PostMapping("/category-groups")
    public ResponseEntity<Resource> exportCategoryGroups(@RequestParam List<Long> ids) throws Exception {
        Path filePath = exportService.exportCategoryGroups(ids);
        Resource resource = new FileSystemResource(filePath);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filePath.getFileName())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @GetMapping("/test")
    public ResponseEntity<Resource> testExport() throws Exception {
        Path filePath = exportService.createEmptyExportDatabase();
        Resource resource = new FileSystemResource(filePath);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filePath.getFileName())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

}

