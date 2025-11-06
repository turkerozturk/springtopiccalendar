package com.turkerozturk.dtt.controller;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/config")
public class ConfigController {

    @Value("${filemanager.max-image-size-kb:300}")
    private long maxImageSizeKb;

    @Value("${spring.servlet.multipart.max-file-size:10MB}")
    private String maxFileSizeServerRaw;

    @GetMapping
    public Map<String, Object> getConfig() {
        return Map.of(
                "maxImageSizeKb", maxImageSizeKb,
                "maxFileSizeServerKb", parseSizeToKb(maxFileSizeServerRaw)
        );
    }

    /**
     * "10MB", "500KB", "2GB" gibi değerleri KB cinsinden döndürür.
     */
    private long parseSizeToKb(String sizeStr) {
        if (sizeStr == null || sizeStr.isBlank()) {
            return 0;
        }

        String normalized = sizeStr.trim().toUpperCase();

        try {
            if (normalized.endsWith("KB")) {
                return Long.parseLong(normalized.replace("KB", "").trim());
            } else if (normalized.endsWith("MB")) {
                double mb = Double.parseDouble(normalized.replace("MB", "").trim());
                return (long) (mb * 1024);
            } else if (normalized.endsWith("GB")) {
                double gb = Double.parseDouble(normalized.replace("GB", "").trim());
                return (long) (gb * 1024 * 1024);
            } else {
                // sadece sayı verilmişse (örneğin "5000")
                return Long.parseLong(normalized);
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid size format: " + sizeStr, e);
        }
    }
}

