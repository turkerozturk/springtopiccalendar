package com.turkerozturk.dtt.controller.web;

import org.springframework.web.util.UriComponentsBuilder;

import java.util.Optional;

public record EntryListLocation(Long listTopicId, Integer listPage, Integer listSize) {

    String path(Long entryId) {
        var uri = UriComponentsBuilder.fromPath("/entries")
                .queryParamIfPresent("topicId", Optional.ofNullable(listTopicId))
                .queryParamIfPresent("page", Optional.ofNullable(listPage))
                .queryParamIfPresent("size", Optional.ofNullable(listSize));
        if (entryId != null) {
            uri.fragment("entry-" + entryId);
        }
        return uri.build().toUriString();
    }
}
