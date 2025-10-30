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
package com.turkerozturk.dtt.service;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;

import com.turkerozturk.dtt.component.AppTimeZoneProvider;
//import com.turkerozturk.dtt.controller.web.HeaderFooterPageEvent;
import com.turkerozturk.dtt.component.MarkdownService;
import com.turkerozturk.dtt.dto.TopicEntryView;
import com.turkerozturk.dtt.entity.Entry;
import com.turkerozturk.dtt.entity.Topic;
import com.turkerozturk.dtt.repository.EntryRepository;
import com.turkerozturk.dtt.repository.TopicRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class EntryModalService {

    private final MarkdownService markdownService;

    private final TopicRepository topicRepository; // mevcut Topic repository
    private final EntryRepository entryRepository; // mevcut Entry repository
    // entryRepository içinde tarih bazlı sorgu ekleyeceğiz

    public EntryModalService(MarkdownService markdownService, TopicRepository topicRepository,
                             EntryRepository entryRepository) {
        this.markdownService = markdownService;
        this.topicRepository = topicRepository;
        this.entryRepository = entryRepository;
    }

    public List<TopicEntryView> getTopicEntriesForDay(List<Long> topicIds, LocalDate day) {
        if (topicIds == null || topicIds.isEmpty()) return Collections.emptyList();

        // epoch millis aralığı (günün başı ve sonu) — AppTimeZoneProvider ile aynı zone'u kullan
        ZoneId zone = AppTimeZoneProvider.getZone();
        Instant start = day.atStartOfDay(zone).toInstant();
        Instant end = day.plusDays(1).atStartOfDay(zone).toInstant().minusMillis(1);

        long startMillis = start.toEpochMilli();
        long endMillis = end.toEpochMilli();

        // topic'leri getir (id -> name)
        List<Topic> topics = topicRepository.findAllById(topicIds);

        // İlgili tarih aralığındaki entryleri çek (topic id'ye göre)
        List<Entry> entries = entryRepository.findByTopicIdInAndDateMillisYmdBetween(topicIds, startMillis, endMillis);

        // Map topicId -> entry
        Map<Long, Entry> entryMap = entries.stream()
                .collect(Collectors.toMap(e -> e.getTopic().getId(), Function.identity(), (a, b) -> a));

        // Build view list in the order of topicIds (korumak istersen)
        List<TopicEntryView> views = new ArrayList<>();
        Map<Long, Topic> topicById = topics.stream().collect(Collectors.toMap(Topic::getId, t -> t));

        for (Long tid : topicIds) {
            Topic t = topicById.get(tid);
            String name = t != null ? t.getName() : ("#" + tid);
            Entry e = entryMap.get(tid);
            TopicEntryView v = new TopicEntryView();
            v.setTopicId(tid);
            v.setTopicName(name);
            if (e != null) {
                v.setEntryStatus(e.getStatus());

                if (e.getNote() != null) {
                    String noteHtml = markdownService.render(e.getNote().getContent());
                    v.setNoteContent(noteHtml);
                }
            }
            views.add(v);
        }
        return views;
    }




    private BaseFont loadFont(String resourcePath) throws IOException, DocumentException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new FileNotFoundException("Font not found: " + resourcePath);
            }
            byte[] fontBytes = is.readAllBytes();
            return BaseFont.createFont(resourcePath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, fontBytes, null);
        }
    }

    public byte[] createPdfForDay(List<Long> topicIds, LocalDate day) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            Document document = new Document(new Rectangle(PageSize.A4), 36, 36, 54, 54);
            PdfWriter writer = PdfWriter.getInstance(document, out);
           //todo bulamiyor: writer.setPageEvent(new HeaderFooterPageEvent()); // eğer sınıf mevcutsa
            document.open();

            // === FONTLAR ===



            String fontPath = "fonts/NotoSans-VariableFont_wdth,wght.ttf"; // https://fonts.google.com/noto/specimen/Noto+Sans

            String fontPathCourier = "fonts/unifont-16.0.04.ttf";

            BaseFont bf = loadFont(fontPath);
            Font headerFont = new Font(bf, 14, Font.BOLD);
            Font cellFont = new Font(bf, 10);

            BaseFont bfCourier = loadFont(fontPathCourier);
            Font cellFontCourier = new Font(bfCourier, 10);

            // === BAŞLIK ===
            Paragraph title = new Paragraph("Entries on selected topics for a specific day", headerFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            String dateFormatted = day.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            Paragraph subtitle = new Paragraph("Date: " + dateFormatted + " - Daily Topic Tracker", cellFont);
            subtitle.setAlignment(Element.ALIGN_CENTER);
            document.add(subtitle);
            document.add(Chunk.NEWLINE);

            // === TABLO ===
            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{2f, 8f});
            table.setHeaderRows(1);

            // Sütun başlıkları
            PdfPCell cell1 = new PdfPCell(new Phrase("Topic", headerFont));
            PdfPCell cell2 = new PdfPCell(new Phrase("Entry / Note", headerFont));
            cell1.setBackgroundColor(Color.LIGHT_GRAY);
            cell2.setBackgroundColor(Color.LIGHT_GRAY);
            table.addCell(cell1);
            table.addCell(cell2);

            // === VERİLER ===
            List<TopicEntryView> rows = getTopicEntriesForDay(topicIds, day);

            for (TopicEntryView row : rows) {
                // topic adı
                table.addCell(new Phrase(row.getTopicName(), cellFont));

                // entry içeriği
                if (row.getEntryStatus() != null) {
                    Integer status = row.getEntryStatus();
                   // Note note = row.getEntry().getNote();

                    String symbol;
                    if (status != null) {
                        switch (status) {
                            case 1 -> symbol = "■"; // done
                            case 2 -> symbol = "▲"; // warning
                            default -> symbol = "□"; // not marked
                        }
                    } else {
                        symbol = "□";
                    }


                    String content = symbol + " " + (row.getNoteContent() != null ? row.getNoteContent() : "");
                    Phrase phrase = new Phrase(content, cellFontCourier);
                    table.addCell(phrase);
                } else {
                    table.addCell(""); // boş hücre
                }
            }

            document.add(table);
            document.close();

        } catch (Exception e) {
            throw new RuntimeException("PDF oluşturulamadı", e);
        }

        return out.toByteArray();
    }


}
