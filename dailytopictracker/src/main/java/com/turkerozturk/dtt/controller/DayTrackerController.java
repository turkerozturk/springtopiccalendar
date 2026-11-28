package com.turkerozturk.dtt.controller;

import com.turkerozturk.dtt.component.AppTimeZoneProvider;
import com.turkerozturk.dtt.entity.CategoryProfile;
import com.turkerozturk.dtt.service.CategoryProfileService;
import com.turkerozturk.dtt.service.DayTrackerService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

@Controller
@RequestMapping("/day-tracker")
@RequiredArgsConstructor
public class DayTrackerController {
    private final AppTimeZoneProvider timeZone;
    private final CategoryProfileService profiles;
    private final DayTrackerService tracker;

    @GetMapping
    public String show(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                       @RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "asc") String sort,
                       Model model) {
        LocalDate today = LocalDate.now(timeZone.getZoneId());
        var profile = profiles.getActiveProfile().orElse(null);
        model.addAttribute("today", today);
        model.addAttribute("profileName", profile == null ? null : profile.getName());
        populate(model, date == null ? today : date, q, sort, 0, profile);
        return "view-tracker/day-tracker";
    }

    @GetMapping("/chunk")
    public String chunk(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                        @RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "asc") String sort,
                        @RequestParam(defaultValue = "0") int page, @RequestParam long scope, Model model) {
        var profile = profiles.getActiveProfile().orElse(null);
        if (scope != scope(profile)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The active category profile has changed");
        }
        populate(model, date, q, sort, page, profile);
        return "view-tracker/day-tracker :: chunk";
    }

    private void populate(Model model, LocalDate date, String query, String sort, int page, CategoryProfile profile) {
        if (page < 0 || page == Integer.MAX_VALUE || date.getYear() < 1 || date.getYear() > 9999) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        String q = query.strip();
        boolean descending = "desc".equals(sort);
        var chunk = tracker.browse(date, profile == null ? null : profile.getId(), q, descending, page);
        model.addAttribute("topics", chunk.topics());
        model.addAttribute("nextPage", chunk.hasMore() ? page + 1 : null);
        model.addAttribute("scope", scope(profile));
        model.addAttribute("selectedDate", date);
        model.addAttribute("q", q);
        model.addAttribute("sort", descending ? "desc" : "asc");
    }

    private static long scope(CategoryProfile profile) {
        return profile == null ? 0 : profile.getId();
    }
}
