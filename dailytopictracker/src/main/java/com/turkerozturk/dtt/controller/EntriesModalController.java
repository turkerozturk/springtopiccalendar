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

import com.turkerozturk.dtt.dto.TopicEntryView;
import com.turkerozturk.dtt.service.EntryModalService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.function.ServerResponse;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

// importlar kısaltıldı — gerekli importları ekle
@RestController
@RequestMapping("/api")
public class EntriesModalController {

    private final EntryModalService entryModalService;
    private final TemplateEngine templateEngine; // thymeleaf spring template engine

    public EntriesModalController(EntryModalService entryModalService,
                                  TemplateEngine templateEngine) {
        this.entryModalService = entryModalService;
        this.templateEngine = templateEngine;
    }

    /** HTML fragment döndürür: modal içeriği */
    @GetMapping(value = "/entries-modal", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> getEntriesModalHtml(
            @RequestParam("day") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate day,
            @RequestParam("topicIds") List<Long> topicIds,
            Locale locale) {

        // service'den view DTO'ları al
        List<TopicEntryView> rows = entryModalService.getTopicEntriesForDay(topicIds, day);

        Context ctx = new Context(locale);
        ctx.setVariable("day", day);
        ctx.setVariable("rows", rows);

        // dateFormat ve dateFormatTitle gibi thymeleaf context değişkenleri varsa onları da ekle
        ctx.setVariable("dateFormat", "dd.MM.yyyy");
        ctx.setVariable("dateFormatTitle", "EEEE, dd MMMM yyyy");
        ctx.setVariable("topicIds", topicIds); // ✅ burası eklendi

        // fragments/entries-modal.html dosyasındaki fragment id'sini render et
        String html = templateEngine.process("fragments/entries-modal", ctx);

        return ResponseEntity.ok(html);
    }

    /** Basit PDF endpoint (örnek). Gerçek PDF üretimini isteğe göre iText/Apache PDFBox ile yap. */
    @GetMapping(value = "/entries-modal/pdf", produces = "application/pdf")
    public ResponseEntity<byte[]> downloadPdf(
            @RequestParam("day") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate day,
            @RequestParam("topicIds") List<Long> topicIds) {



        byte[] pdfBytes = entryModalService.createPdfForDay(topicIds, day); // implement et

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.builder("attachment")
                .filename(String.format("entries-%s.pdf", day.toString()))
                .build());
        headers.setContentType(MediaType.APPLICATION_PDF);

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
}

