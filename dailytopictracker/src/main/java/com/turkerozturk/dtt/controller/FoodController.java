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
package com.turkerozturk.dtt.controller;


import com.turkerozturk.dtt.component.AppTimeZoneProvider;
import com.turkerozturk.dtt.dto.DateRangeFoodSummaryDto;
import com.turkerozturk.dtt.dto.FoodSummaryDto;
import com.turkerozturk.dtt.service.FoodService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

@Controller
@RequiredArgsConstructor
public class FoodController {

    private final FoodService foodService;
    private final AppTimeZoneProvider timeZoneProvider;


    @GetMapping("/food")
    public String getFoodPage(
            @RequestParam(required = false) Long dateMillis,
            Model model
    ) {
        ZoneId zoneId = timeZoneProvider.getZoneId(); // olusturdugumuz component. application.properties'den zone ceker.

        if (dateMillis == null) {
            // default: bugün (senin formatına göre ayarlarsın)

            dateMillis = LocalDate.now()
                    .atStartOfDay(zoneId)
                    .toInstant()
                    .toEpochMilli();
        }

        FoodSummaryDto summary = foodService.getDailyFoodSummary(dateMillis);

        model.addAttribute("summary", summary);
        model.addAttribute("dateMillis", dateMillis);
        model.addAttribute("zoneId", zoneId);

        return "food";
    }

    @GetMapping("/food-date-range")
    public String getFoodDateRangePage(
            @RequestParam(required = false) Long firstDateMillis,
            @RequestParam(required = false) Long lastDateMillis,
            Model model
    ) {
        ZoneId zoneId = timeZoneProvider.getZoneId();

        LocalDate lastDate;
        LocalDate firstDate;

        if (lastDateMillis == null) {
            lastDate = LocalDate.now(zoneId);
        } else {
            lastDate = Instant.ofEpochMilli(lastDateMillis)
                    .atZone(zoneId)
                    .toLocalDate();
        }

        if (firstDateMillis == null) {
            firstDate = lastDate.minusDays(6);
        } else {
            firstDate = Instant.ofEpochMilli(firstDateMillis)
                    .atZone(zoneId)
                    .toLocalDate();
        }

        if (lastDate.isBefore(firstDate)) {
            firstDate = lastDate;
        }

        long firstMillis = firstDate.atStartOfDay(zoneId).toInstant().toEpochMilli();
        long lastMillis = lastDate.atStartOfDay(zoneId).toInstant().toEpochMilli();

        DateRangeFoodSummaryDto summary =
                foodService.getDateRangeSummary(firstMillis, lastMillis);

        model.addAttribute("summary", summary);
        model.addAttribute("zoneId", zoneId);
        model.addAttribute("firstDateMillis", firstMillis);
        model.addAttribute("lastDateMillis", lastMillis);

        return "food-date-range";
    }

}
