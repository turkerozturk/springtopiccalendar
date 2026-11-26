package com.turkerozturk.dtt.controller.web;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.util.Optional;

public record TopicHistoryFilter(
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
        Integer historyPage) {

    boolean isActive() {
        return fromDate != null || toDate != null;
    }

    String validationMessage() {
        if (!isSupportedDate(fromDate) || !isSupportedDate(toDate)) {
            return "Enter a date between years 1 and 9999.";
        }
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            return "From date must be on or before the through date.";
        }
        return null;
    }

    int pageIndex() {
        return historyPage == null || historyPage < 1 ? 0 : historyPage - 1;
    }

    String returnPath(long topicId, long listCategoryId) {
        var uri = UriComponentsBuilder.fromPath("/topics/" + topicId).queryParam("listCategoryId", listCategoryId);
        if (isActive() && validationMessage() == null) {
            uri.queryParamIfPresent("fromDate", Optional.ofNullable(fromDate))
                    .queryParamIfPresent("toDate", Optional.ofNullable(toDate));
            if (pageIndex() > 0) {
                uri.queryParam("historyPage", historyPage);
            }
        }
        return uri.build().toUriString();
    }

    private static boolean isSupportedDate(LocalDate date) {
        return date == null || (date.getYear() >= 1 && date.getYear() <= 9999);
    }
}
