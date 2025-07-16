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
package com.turkerozturk.dtt;

import com.turkerozturk.dtt.configuration.banner.CustomBanner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DailyTopicTrackerApplication {



    public static void main(String[] args) {
        // Eğer args[0] varsa, sqlite dosya adı olarak kullan
        if (args.length > 0 && !args[0].isBlank()) {
            System.setProperty("spring.datasource.url", "jdbc:sqlite:" + args[0]);
        }
        //SpringApplication.run(DailyTopicTrackerApplication.class, args);
        SpringApplication app = new SpringApplication(DailyTopicTrackerApplication.class);
        app.setBanner(new CustomBanner()); // DailyTopicTracker asciiart on console
        app.run(args);
    }
}
