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
package com.turkerozturk.dttupdater;

import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;

public class Downloader {

    public void downloadFile(String url, Path target) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS) // 302 hatasi vermemesi icin
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(url))
                .build();

        System.out.println("📦 Downloading from: " + url);
        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

        if (response.statusCode() != 200) {
            throw new IOException("Download failed: " + response.statusCode() + "\nURL: " + url);
        }

        long contentLength = response.headers()
                .firstValueAsLong("Content-Length")
                .orElse(-1);

        try (InputStream in = response.body();
             OutputStream out = Files.newOutputStream(target, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {

            byte[] buffer = new byte[8192];
            long downloaded = 0;
            long lastPrinted = 0;
            int read;

            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
                downloaded += read;

                if (contentLength > 0) {
                    int progress = (int) (100 * downloaded / contentLength);
                    if (progress - lastPrinted >= 2) { // her %2’de bir yaz
                        System.out.print("\r⬇️  Downloading: " + progress + "%");
                        lastPrinted = progress;
                    }
                }
            }
        }

        System.out.println("\r✅ Download complete: " + target.toAbsolutePath());
    }
}

