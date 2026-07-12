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
import com.turkerozturk.dtt.dto.FoodSummaryDto;
import com.turkerozturk.dtt.service.FoodService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.ZoneId;

@Controller
@RequiredArgsConstructor
public class DayViewController {

    //private final FoodService foodService;
    private final AppTimeZoneProvider timeZoneProvider;


    @GetMapping("/dayview")
    public String getFoodPage(
            @RequestParam(required = false) Long dateMillisYmd,
            Model model
    ) {
        ZoneId zoneId = timeZoneProvider.getZoneId(); // olusturdugumuz component. application.properties'den zone ceker.

        if (dateMillisYmd == null) {
            // default: bugün (senin formatına göre ayarlarsın)

            dateMillisYmd = LocalDate.now()
                    .atStartOfDay(zoneId)
                    .toInstant()
                    .toEpochMilli();
        }

        //FoodSummaryDto summary = foodService.getDailyFoodSummary(dateMillis);

        //model.addAttribute("summary", summary);
        // ASLINDA YMD string dehil, dateMillis yani bu. Ama hem food hem de dailyview'de o sekilde kullanmisim URL parametresi ve redirectionda.
        // O yuzden calisan kodu simdilik gelistirme geregi duymadim.
        model.addAttribute("dateMillisYmd", dateMillisYmd);
        model.addAttribute("zoneId", zoneId);

        return "entries/entries-dayview";
    }
}
