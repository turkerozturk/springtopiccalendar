package com.turkerozturk.dtt.controller.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.Arrays;

final class PageReturnPath {
    private PageReturnPath() {}

    static String entryBrowse(String value, Long entryId) {
        String path = localPath(value, "/search", "/entry-results/page");
        return entryId == null ? path : path + "#result-entry-" + entryId;
    }

    static String topicList(String value) {
        return value == null || value.isEmpty() ? null : localPath(value, "/topics");
    }

    static String topicAnchor(String path, long topicId) {
        return path + "#topic-" + topicId;
    }

    private static String localPath(String value, String... allowedPaths) {
        URI uri;
        try {
            uri = URI.create(value == null ? "" : value);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        String path = uri.getRawPath();
        if (uri.isAbsolute() || uri.getRawAuthority() != null || uri.getRawFragment() != null
                || !Arrays.asList(allowedPaths).contains(path)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        if (uri.getRawQuery() != null) {
            path += "?" + uri.getRawQuery();
        }
        return path;
    }
}
