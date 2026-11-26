package com.turkerozturk.dtt.controller.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;

final class EntryBrowseReturn {
    private EntryBrowseReturn() {}

    static String path(String value, Long entryId) {
        URI uri;
        try {
            uri = URI.create(value == null ? "" : value);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        String path = uri.getRawPath();
        if (uri.isAbsolute() || uri.getRawAuthority() != null || uri.getRawFragment() != null
                || (!"/search".equals(path) && !"/entry-results/page".equals(path))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        if (uri.getRawQuery() != null) {
            path += "?" + uri.getRawQuery();
        }
        return entryId == null ? path : path + "#result-entry-" + entryId;
    }
}
