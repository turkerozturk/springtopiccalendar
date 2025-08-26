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
package com.turkerozturk.dtt.controller.web;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.pdf.*;
import com.turkerozturk.dtt.component.AppTimeZoneProvider;
import org.apache.pdfbox.io.IOUtils;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * PdfWriter.getPageNumber() ile o anki sayfa numarasını alabiliyoruz ama toplam sayfa sayısı henüz belli olmuyor. PDF motoru sayfa sayısını ancak doküman kapandıktan sonra biliyor.
 *
 * Bunu çözmek için PdfTemplate kullanılır (iText / OpenPDF standardı). Mantık şu:
 *
 * Her sayfada Page X of kısmını yaz,
 *
 * of kısmına küçük bir PdfTemplate (boş kutu) koy,
 *
 * Doküman kapandığında bu şablona toplam sayfa sayısını yaz.
 */
class HeaderFooterPageEvent extends PdfPageEventHelper {
    PdfTemplate total;
    Font fontBlack = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.BLACK);
    Font fontGray  = FontFactory.getFont(FontFactory.HELVETICA, 8, new Color(128,128,128));
    Font fontGraySmall = FontFactory.getFont(FontFactory.HELVETICA, 7, new Color(128,128,128));

    @Override
    public void onOpenDocument(PdfWriter writer, Document document) {
        total = writer.getDirectContent().createTemplate(20, 16); // 20pt genişlik
    }

    @Override
    public void onEndPage(PdfWriter writer, Document document) {

        //SADECE ILK SAYFADA LOGO
        if (writer.getPageNumber() == 1) {
            float docWidth = document.getPageSize().getWidth();
            float docHeight = document.getPageSize().getHeight();

            InputStream logoStream = getClass().getResourceAsStream("/static/images/logo.png");
            Image logo = null;
            try {
                logo = Image.getInstance(IOUtils.toByteArray(logoStream));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            logo.scaleToFit(60, 60); // kare, title’dan biraz büyük
            float x = (docWidth / 2) + 37;  // ortalamak için - yarı genişlik
            float y = (docHeight / 2) + 322;         // sayfanın ortası civarı

            PdfContentByte canvas = writer.getDirectContentUnder();
            PdfGState gs1 = new PdfGState();
            gs1.setFillOpacity(0.4f); // şeffaflık
            canvas.saveState();
            canvas.setGState(gs1);
            logo.setAbsolutePosition(x, y);
            canvas.addImage(logo);
            canvas.restoreState();
        }




        //FOOTER
        PdfContentByte cb = writer.getDirectContent();
        float centerX = (document.right() - document.left()) / 2 + document.leftMargin();
        float baseY = document.bottom() - 15;

        // 1) Page X of Y (siyah, ortada)
        Phrase pagePhrase = new Phrase("Page " + writer.getPageNumber() + " of ", fontBlack);
        ColumnText.showTextAligned(cb, Element.ALIGN_CENTER, pagePhrase, centerX, baseY, 0);
        cb.addTemplate(total, centerX + 20, baseY);

        // 2) Generated info (sağa yaslı, gri)
        ZoneId zoneId = AppTimeZoneProvider.getZone();
        LocalDateTime now = LocalDateTime.now(zoneId);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm EEE", Locale.ENGLISH);
        Phrase genPhrase = new Phrase("Generated on: " + now.format(fmt), fontGray);
        ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT, genPhrase, document.right(), baseY - 12, 0);

        // 3) Visit info (sola yaslı, gri küçük)
        Phrase visitPhrase = new Phrase(
                "Visit DailyTopicTracker at https://github.com/turkerozturk/springtopiccalendar",
                fontGraySmall); // TODO url yi application.properties gibi merkezi bir yere al.
        ColumnText.showTextAligned(cb, Element.ALIGN_LEFT, visitPhrase, document.left(), baseY - 24, 0);
    }

    @Override
    public void onCloseDocument(PdfWriter writer, Document document) {
        Phrase totalPhrase = new Phrase(String.valueOf(writer.getPageNumber() - 1), fontBlack);
        ColumnText.showTextAligned(total, Element.ALIGN_LEFT, totalPhrase, 0, 0, 0);
    }
}
