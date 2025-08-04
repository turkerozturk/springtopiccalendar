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
package com.turkerozturk.dtt.configuration.banner;

import org.springframework.boot.Banner;
import org.springframework.boot.SpringBootVersion;
import org.springframework.core.env.Environment;

import java.io.PrintStream;

public class CustomBanner implements Banner {
    @Override
    public void printBanner(Environment environment, Class<?> sourceClass, PrintStream out) {
        // https://budavariam.github.io/asciiart-text/
        out.println("  ____        _ _      _____           _     _____               _             ");
        out.println(" |  _ \\  __ _(_) |_   |_   _|__  _ __ (_) __|_   _| __ __ _  ___| | _____ _ __ ");
        out.println(" | | | |/ _` | | | | | || |/ _ \\| '_ \\| |/ __|| || '__/ _` |/ __| |/ / _ \\ '__|");
        out.println(" | |_| | (_| | | | |_| || | (_) | |_) | | (__ | || | | (_| | (__|   <  __/ |   ");
        out.println(" |____/ \\__,_|_|_|\\__, ||_|\\___/| .__/|_|\\___||_||_|  \\__,_|\\___|_|\\_\\___|_|   ");
        out.println("                  |___/         |_|                                            ");
        out.println();
        out.println("Turker Ozturk");
        out.println();
        out.println(":: Spring Boot :: (v" + SpringBootVersion.getVersion() + ")");
        out.println();

    }
}
