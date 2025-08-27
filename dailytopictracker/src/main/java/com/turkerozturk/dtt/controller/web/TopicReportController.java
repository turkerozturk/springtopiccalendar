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
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;

import com.lowagie.text.Document;
import com.lowagie.text.PageSize;


import com.turkerozturk.dtt.service.TopicReportCatGrpService;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import com.turkerozturk.dtt.component.AppTimeZoneProvider;
import com.turkerozturk.dtt.dto.DatedTopicViewModel;
import com.turkerozturk.dtt.entity.Entry;
import com.turkerozturk.dtt.entity.Topic;
import com.turkerozturk.dtt.repository.EntryRepository;
import com.turkerozturk.dtt.service.TopicService;
import org.springframework.web.bind.annotation.RequestParam;

import java.awt.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;

@Controller
@RequestMapping("/reports")
public class TopicReportController {

    @Autowired
    EntryRepository entryRepository;

    private final AppTimeZoneProvider timeZoneProvider;

    @Value("${neutral.items.limit:99999}")
    int neutralItemsLimit;

    /**
     * this limit is only for finished items "before today".
     * Today's finished items has no limit.
     */
    @Value("${finished.items.limit:99999}")
    int finishedItemsLimit;

    @Autowired
    private TopicService topicService;

    @Autowired
    private TopicReportCatGrpService topicReportCatGrpService;

    public TopicReportController(AppTimeZoneProvider timeZoneProvider) {
        this.timeZoneProvider = timeZoneProvider;
    }

    @GetMapping("/neutral")
    public String showFutureNeutralTopics(Model model) {
        List<Topic> topics = topicService.getNextNeutralTopics(neutralItemsLimit);
        model.addAttribute("topics", topics);
        return "future-neutral-topics";
    }


    @GetMapping("/prediction")
    public String showPredictionTopics(Model model) {
        List<Topic> topics = topicService.getTopicsWithPredictionDateBeforeOrEqualToday();
        model.addAttribute("topics", topics);
        return "future-neutral-topics";
    }


    @GetMapping("/warn")
    public String showWarnTopics(Model model) {
        List<Topic> topics = topicService.getAllUrgentTopicsSortedByMostRecentWarning();
        model.addAttribute("topics", topics);
        return "future-neutral-topics";
    }

    @GetMapping("/done")
    public String showDoneTopics(Model model) {
        List<Topic> topics = topicService.getLastActivitiesLimitedToN(finishedItemsLimit);
        model.addAttribute("topics", topics);
        return "future-neutral-topics";
    }


    @GetMapping("/all")
    public String showDoneWarnPredNeutTopics(Model model) {
        ZoneId zoneId = AppTimeZoneProvider.getZone();

        List<Topic> importants = topicService.getAllUrgentTopicsSortedByMostRecentWarning();
        List<Topic> neutrals = topicService.getNextNeutralTopics(neutralItemsLimit);
        List<Topic> predictions = topicService.getTopicsWithPredictionDateBeforeOrEqualToday();
        List<Topic> finisheds = topicService.getLastActivitiesLimitedToTodayAndThenToN(finishedItemsLimit);

        List<DatedTopicViewModel> allItems = new ArrayList<>();

        List<DatedTopicViewModel> importantItems = new ArrayList<>();
        importants.forEach(t -> {
            if (t.getFirstWarningEntryDateMillisYmd() != null) {

                Entry entry = entryRepository.findByTopicIdAndDateMillisYmd(t.getId(), t.getFirstWarningEntryDateMillisYmd()).get();
                importantItems.add(new DatedTopicViewModel(t, t.getFirstWarningEntryDateMillisYmd(), 2, zoneId, entry));


                // Ayni gun prediction da varsa, o predictionu listeden cikariyoruz ki iki farkli kayit gibi anlasilmasin.
                Iterator<Topic> predIterator = predictions.iterator();
                while (predIterator.hasNext()) {
                    Topic predTopic = predIterator.next();
                    if (t.getFirstWarningEntryDateMillisYmd() != null && t.getFirstWarningEntryDateMillisYmd().equals(predTopic.getPredictionDateMillisYmd())
                            && t.getId().equals(predTopic.getId())
                    ) {
                        predIterator.remove(); // Güvenli şekilde siler
                    }
                }

            }
        });

        List<DatedTopicViewModel> neutralItems = new ArrayList<>();
        neutrals.forEach(t -> {
            if (t.getFirstFutureNeutralEntryDateMillisYmd() != null) {

                Entry entry = entryRepository.findByTopicIdAndDateMillisYmd(t.getId(), t.getFirstFutureNeutralEntryDateMillisYmd()).get();

                neutralItems.add(new DatedTopicViewModel(t, t.getFirstFutureNeutralEntryDateMillisYmd(), 0, zoneId, entry));

                // Ayni gun prediction da varsa, o predictionu listeden cikariyoruz ki iki farkli kayit gibi anlasilmasin.
                Iterator<Topic> predIterator = predictions.iterator();
                while (predIterator.hasNext()) {
                    Topic predTopic = predIterator.next();
                    if (t.getFirstFutureNeutralEntryDateMillisYmd() != null && t.getFirstFutureNeutralEntryDateMillisYmd().equals(predTopic.getPredictionDateMillisYmd())
                            && t.getId().equals(predTopic.getId())
                    ) {
                        predIterator.remove(); // Güvenli şekilde siler
                    }
                }
            }

        });

        List<DatedTopicViewModel> finishedItems = new ArrayList<>();
        finisheds.forEach(t -> {
            if (t.getLastPastEntryDateMillisYmd() != null) {
                Entry entry = entryRepository.findByTopicIdAndDateMillisYmd(t.getId(), t.getLastPastEntryDateMillisYmd()).get();

                finishedItems.add(new DatedTopicViewModel(t, t.getLastPastEntryDateMillisYmd(), 1, zoneId, entry));

                // Ayni gun prediction da varsa, o predictionu listeden cikariyoruz ki iki farkli kayit gibi anlasilmasin.
                Iterator<Topic> predIterator = predictions.iterator();
                while (predIterator.hasNext()) {
                    Topic predTopic = predIterator.next();
                    if (t.getLastPastEntryDateMillisYmd() != null && t.getLastPastEntryDateMillisYmd().equals(predTopic.getPredictionDateMillisYmd())
                            && t.getId().equals(predTopic.getId())
                    ) {
                        predIterator.remove(); // Güvenli şekilde siler
                    }
                }

            }

        });

        List<DatedTopicViewModel> predictionItems = new ArrayList<>();
        predictions.forEach(t -> {
            if (t.getPredictionDateMillisYmd() != null)
                predictionItems.add(new DatedTopicViewModel(t, t.getPredictionDateMillisYmd(), 3, zoneId, null));
        });

        // kodlarin siralamasi onemli bu metodda. Once important, sonra neutral, sonra predictions, en son done olanlar.
        allItems.addAll(importantItems);
        allItems.addAll(neutralItems);
        allItems.addAll(predictionItems);
        allItems.addAll(finishedItems);



        allItems.sort(Comparator.comparing(DatedTopicViewModel::getDateLocal).reversed());


        // simdi asagida ekstra bir filtreleme yapiyoruz. weight yani onem degeri negatif olanlari once allItems
        // listesinden cikarip baska listeye ekliyoruz. Ardindan allItems listesinin en sonuna yeniden ekliyoruz.
        // Boylece az once warn,not marked,prediction,done olarak siralanmis olan liste, artik o sirayi bozmadan
        // sifir ve pozitif onemi olanlar olarak gorunecek, hemen ardindan yine kendi icinde o siralamada olan
        // fakat onem derecesi negatif olan liste elemanlari gorunecek. Goruncecek derken bu liste frontend'de
        // tablo olarak goruntuleniyor. Butun ogeler en ustte en yeni tarih olmak uzere eskiye dogru gider.
        // frontendde bulunan renklendirme kodlari ile bakildiginda ne ise yaradiklarini anlamak kolay olacaktir.
        List<DatedTopicViewModel> negativeWeightItems = new ArrayList<>();
        // default 0, weight = -1 olarak belirledigim topicleri listeden cikarir:
        Iterator<DatedTopicViewModel> iterator = allItems.iterator();
        while (iterator.hasNext()) {
            DatedTopicViewModel t = iterator.next();
            if (t.getTopic().getWeight() < 0) {
                negativeWeightItems.add(t);
                iterator.remove(); // Güvenli şekilde siler
            }
        }

        negativeWeightItems.sort(Comparator.comparing(DatedTopicViewModel::getDateLocal).reversed());



        // Arsivlenmis categorilere ait topicleri allItems'den cikarip archivedItems listesine ekliyoruz.
        // Bu allItems icinde negatif weight itemler yok, onlari ayirmistik, sonra onlar icin de aynisini yapiyoruz daha ileride.
        List<DatedTopicViewModel> archivedItems = new ArrayList<>();
        // archived olarak belirledigim categorylerdeki topicleri listeden cikarir:
        Iterator<DatedTopicViewModel> iter = allItems.iterator();
        while (iter.hasNext()) {
            DatedTopicViewModel t = iter.next();
            if (t.getTopic().getCategory().isArchived()) {
                archivedItems.add(t);
                iter.remove(); // Güvenli şekilde siler
            }
        }

        // Arsivlenmis categorilere ait topicleri negativeWeightItems'den cikarip negativeWeightArchivedItems listesine ekliyoruz.
        List<DatedTopicViewModel> negativeWeightArchivedItems = new ArrayList<>();
        // archived olarak belirledigim categorylerdeki topicleri listeden cikarir:
        Iterator<DatedTopicViewModel> iterrr = negativeWeightItems.iterator();
        while (iterrr.hasNext()) {
            DatedTopicViewModel t = iterrr.next();
            if (t.getTopic().getCategory().isArchived()) {
                negativeWeightArchivedItems.add(t);
                iterrr.remove(); // Güvenli şekilde siler
            }
        }

        model.addAttribute("allItems", allItems);
        model.addAttribute("today", LocalDate.now(zoneId));
        model.addAttribute("zoneId", zoneId);

        // bu ikisini simdilik frontendde dogrudan kullanmiyoruz. TODO Kullanacagin zaman yukaridaki  su satiri sil: allItems.addAll(negativeWeightItems);
        model.addAttribute("negativeWeightItems", negativeWeightItems);
        model.addAttribute("archivedItems", archivedItems);
        model.addAttribute("negativeWeightArchivedItems", negativeWeightArchivedItems);


        return "view-intelligent/report-all-statuses";
    }


    // print report:

    private List<DatedTopicViewModel> getImportant(List<Topic> importants, List<Topic> predictions, ZoneId zoneId) {
        List<DatedTopicViewModel> importantItems = new ArrayList<>();
        importants.forEach(t -> {
            if (t.getFirstWarningEntryDateMillisYmd() != null) {

                Entry entry = entryRepository.findByTopicIdAndDateMillisYmd(t.getId(), t.getFirstWarningEntryDateMillisYmd()).get();
                importantItems.add(new DatedTopicViewModel(t, t.getFirstWarningEntryDateMillisYmd(), 2, zoneId, entry));


                // Ayni gun prediction da varsa, o predictionu listeden cikariyoruz ki iki farkli kayit gibi anlasilmasin.
                Iterator<Topic> predIterator = predictions.iterator();
                while (predIterator.hasNext()) {
                    Topic predTopic = predIterator.next();
                    if (t.getFirstWarningEntryDateMillisYmd() != null && t.getFirstWarningEntryDateMillisYmd().equals(predTopic.getPredictionDateMillisYmd())
                            && t.getId().equals(predTopic.getId())
                    ) {
                        predIterator.remove(); // Güvenli şekilde siler
                    }
                }

            }
        });
        return importantItems;
    }

    private List<DatedTopicViewModel> getFinisheds(List<Topic> finisheds, List<Topic> predictions, ZoneId zoneId) {

        List<DatedTopicViewModel> finishedItems = new ArrayList<>();
        finisheds.forEach(t -> {
            if (t.getLastPastEntryDateMillisYmd() != null) {
                Entry entry = entryRepository.findByTopicIdAndDateMillisYmd(t.getId(), t.getLastPastEntryDateMillisYmd()).get();

                finishedItems.add(new DatedTopicViewModel(t, t.getLastPastEntryDateMillisYmd(), 1, zoneId, entry));

                // Ayni gun prediction da varsa, o predictionu listeden cikariyoruz ki iki farkli kayit gibi anlasilmasin.
                Iterator<Topic> predIterator = predictions.iterator();
                while (predIterator.hasNext()) {
                    Topic predTopic = predIterator.next();
                    if (t.getLastPastEntryDateMillisYmd() != null && t.getLastPastEntryDateMillisYmd().equals(predTopic.getPredictionDateMillisYmd())
                            && t.getId().equals(predTopic.getId())
                    ) {
                        predIterator.remove(); // Güvenli şekilde siler
                    }
                }

            }

        });
        return finishedItems;
    }

    private List<DatedTopicViewModel> getNeutrals(List<Topic> neutrals, List<Topic> predictions, ZoneId zoneId) {
        List<DatedTopicViewModel> neutralItems = new ArrayList<>();
        neutrals.forEach(t -> {
            if (t.getFirstFutureNeutralEntryDateMillisYmd() != null) {

                Entry entry = entryRepository.findByTopicIdAndDateMillisYmd(t.getId(), t.getFirstFutureNeutralEntryDateMillisYmd()).get();

                neutralItems.add(new DatedTopicViewModel(t, t.getFirstFutureNeutralEntryDateMillisYmd(), 0, zoneId, entry));

                // Ayni gun prediction da varsa, o predictionu listeden cikariyoruz ki iki farkli kayit gibi anlasilmasin.
                Iterator<Topic> predIterator = predictions.iterator();
                while (predIterator.hasNext()) {
                    Topic predTopic = predIterator.next();
                    if (t.getFirstFutureNeutralEntryDateMillisYmd() != null && t.getFirstFutureNeutralEntryDateMillisYmd().equals(predTopic.getPredictionDateMillisYmd())
                            && t.getId().equals(predTopic.getId())
                    ) {
                        predIterator.remove(); // Güvenli şekilde siler
                    }
                }
            }

        });
        return neutralItems;
    }

    private List<DatedTopicViewModel> getNegatives(List<DatedTopicViewModel> allItems) {
        // simdi asagida ekstra bir filtreleme yapiyoruz. weight yani onem degeri negatif olanlari once allItems
        // listesinden cikarip baska listeye ekliyoruz. Ardindan allItems listesinin en sonuna yeniden ekliyoruz.
        // Boylece az once warn,not marked,prediction,done olarak siralanmis olan liste, artik o sirayi bozmadan
        // sifir ve pozitif onemi olanlar olarak gorunecek, hemen ardindan yine kendi icinde o siralamada olan
        // fakat onem derecesi negatif olan liste elemanlari gorunecek. Goruncecek derken bu liste frontend'de
        // tablo olarak goruntuleniyor. Butun ogeler en ustte en yeni tarih olmak uzere eskiye dogru gider.
        // frontendde bulunan renklendirme kodlari ile bakildiginda ne ise yaradiklarini anlamak kolay olacaktir.
        List<DatedTopicViewModel> negativeWeightItems = new ArrayList<>();
        // default 0, weight = -1 olarak belirledigim topicleri listeden cikarir:
        Iterator<DatedTopicViewModel> iterator = allItems.iterator();
        while (iterator.hasNext()) {
            DatedTopicViewModel t = iterator.next();
            if (t.getTopic().getWeight() < 0) {
                negativeWeightItems.add(t);
                iterator.remove(); // Güvenli şekilde siler
            }
        }

        negativeWeightItems.sort(Comparator.comparing(DatedTopicViewModel::getDateLocal).reversed());
        return negativeWeightItems;
    }

/*
    @GetMapping("/allPrint")
    public String print(Model model) {
        ZoneId zoneId = AppTimeZoneProvider.getZone();

        List<Topic> importants = topicService.getAllUrgentTopicsSortedByMostRecentWarning();
        List<Topic> neutrals = topicService.getNextNeutralTopics(neutralItemsLimit);
        List<Topic> predictions = topicService.getTopicsWithPredictionDateBeforeOrEqualToday();
        List<Topic> finisheds = topicService.getLastActivitiesLimitedToTodayAndThenToN(finishedItemsLimit);

        // ADIM 1
        List<DatedTopicViewModel> importantItems = getImportant(importants, predictions, zoneId);

        // ADIM 2
        List<DatedTopicViewModel> neutralItems = getNeutrals(neutrals, predictions, zoneId);

        // ADIM 3
        List<DatedTopicViewModel> finishedItems = getFinisheds(finisheds, predictions, zoneId);

        // ADIM 4
        List<DatedTopicViewModel> predictionItems = getPredictions(predictions, zoneId);

        // kodlarin siralamasi onemli bu metodda. Once important, sonra neutral, sonra predictions, en son done olanlar.
        List<DatedTopicViewModel> allItems = new ArrayList<>();
        allItems.addAll(importantItems);
        allItems.addAll(neutralItems);
        allItems.addAll(predictionItems);
        allItems.addAll(finishedItems);
        allItems.sort(Comparator.comparing(DatedTopicViewModel::getDateLocal).reversed());


        List<DatedTopicViewModel> negativeWeightItems = getNegatives(allItems);

        List<DatedTopicViewModel> archivedItems = getArchiveds(allItems);

        List<DatedTopicViewModel> negativeWeightArchivedItems = getNegativeArchiveds(negativeWeightItems);

        int rowNum = 0;
        for(DatedTopicViewModel d : allItems) {
            rowNum++;
            StringBuilder sb = new StringBuilder();

            sb.append(rowNum); // 3 basamaga tamamlansin yani zero padding.
            sb.append("\t");

            sb.append(d.getTopic().getCategory().getCategoryGroup().getName()); // ilk 10 karakter
            sb.append("\t");

            sb.append(d.getTopic().getCategory().getName()); // ilk 10 karakter
            sb.append("\t");

            sb.append(d.getTopic().getWeight()); // 2 basamaga tamamlansin yani zero padding.
            sb.append("\t");

            sb.append(d.getDateLocal()); // 2025-08-25 Mon formatinda olsun.
            sb.append("\t");

            sb.append(d.getDateLocal()); // burada bugun ile d.getDateLocal() arasındaki gün farkı alınacak, 0 ise 0 yazacak, negatif veya pozitif ise işareti ile birlikte 3 basamakli zero padding ile yazılacak.
            sb.append("\t");

            sb.append(d.getTopic().getName()); // ilk 10 karakter
            sb.append("\t");

            sb.append(d.getEntry().getNote()); // null degilse ilk 20 karakter
            sb.append("\t");


            sb.append(d.getStatus()); // 1 ise checkmark karakteri, 3 ise boş, 0 ise gri arkaplan, 2 ise ünlem olsun.
            sb.append("\t");

            System.out.println(sb);

        }



        return "view-intelligent/report-all-statuses";
    }
*/

    @GetMapping("/allPrint")
    public void print(HttpServletResponse response,
                      Model model,
                      @RequestParam(name = "categoryGroupId", required=false) Long categoryGroupId) throws Exception {
        ZoneId zoneId = AppTimeZoneProvider.getZone();

        List<Topic> importants, neutrals, predictions, finisheds;

        if(categoryGroupId == null) {
            importants = topicService.getAllUrgentTopicsSortedByMostRecentWarning();
            neutrals = topicService.getNextNeutralTopics(neutralItemsLimit);
            predictions = topicService.getTopicsWithPredictionDateBeforeOrEqualToday();
            finisheds = topicService.getLastActivitiesLimitedToTodayAndThenToN(finishedItemsLimit);
        } else {
            importants = topicReportCatGrpService.getAllUrgentTopicsSortedByMostRecentWarning(categoryGroupId);
            neutrals = topicReportCatGrpService.getNextNeutralTopics(neutralItemsLimit, categoryGroupId);
            predictions = topicReportCatGrpService.getTopicsWithPredictionDateBeforeOrEqualToday(categoryGroupId);
            finisheds = topicReportCatGrpService.getLastActivitiesLimitedToTodayAndThenToN(finishedItemsLimit, categoryGroupId);
        }

        // ADIM 1
        List<DatedTopicViewModel> importantItems = getImportant(importants, predictions, zoneId);

        // ADIM 2
        List<DatedTopicViewModel> neutralItems = getNeutrals(neutrals, predictions, zoneId);

        // ADIM 3
        List<DatedTopicViewModel> finishedItems = getFinisheds(finisheds, predictions, zoneId);

        // ADIM 4
        List<DatedTopicViewModel> predictionItems = getPredictions(predictions, zoneId);

        // kodlarin siralamasi onemli bu metodda. Once important, sonra neutral, sonra predictions, en son done olanlar.
        List<DatedTopicViewModel> allItems = new ArrayList<>();
        allItems.addAll(importantItems);
        allItems.addAll(neutralItems);
        allItems.addAll(predictionItems);
        allItems.addAll(finishedItems); // TODO bunlari enable disable edilebilir yap veya printer icin ayri yap.
        allItems.sort(Comparator.comparing(DatedTopicViewModel::getDateLocal).reversed());


        List<DatedTopicViewModel> negativeWeightItems = getNegatives(allItems);

        List<DatedTopicViewModel> archivedItems = getArchiveds(allItems);

        List<DatedTopicViewModel> negativeWeightArchivedItems = getNegativeArchiveds(negativeWeightItems);


        // Burada senin gerçek datanı getirmen gerekiyor
        //List<DatedTopicViewModel> allItems = service.getAllItems();

        // Dosya adı
        String filename = "dttreport" + LocalDate.now(zoneId).format(DateTimeFormatter.BASIC_ISO_DATE) + ".pdf";

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=" + filename);

        // PDF dokümanı

        Document document = new Document(new Rectangle(PageSize.A4), 36, 36, 54, 54); // marginler
        PdfWriter writer = PdfWriter.getInstance(document, response.getOutputStream());
        // Header & Footer, PdfPageEventHelper sinifini extend ettik cunku toplam sayfa no'yu gormek istiyoruz:
        writer.setPageEvent(new HeaderFooterPageEvent());
        document.open();

        // src/main/resources/fonts/NotoSans-VariableFont_wdth,wght.ttf
        String fontPath = "fonts/NotoSans-VariableFont_wdth,wght.ttf"; // https://fonts.google.com/noto/specimen/Noto+Sans
        String fontPathCourier = "fonts/unifont-16.0.04.ttf";
        // Normal yazilar icin
        BaseFont bf = loadFont(fontPath);
        Font headerFont = new Font(bf, 14, Font.BOLD);
        Font cellFont   = new Font(bf, 10);

        // Semboller icin
        BaseFont bfCourier = loadFont(fontPathCourier);
        Font cellFontCourier = new Font(bfCourier, 10);

        // Başlık
        //Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, "UTF8", 14);
        Paragraph title = new Paragraph("Daily Topic Tracker Report", headerFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);
        document.add(Chunk.NEWLINE);

        // Tablo: kolon sayısını ayarlayalım
        PdfPTable table = new PdfPTable(9);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{0.6f, 2f, 2.6f, 0.5f, 2.2f, 0.8f, 3.5f, 2.8f, 0.5f});

        table.setHeaderRows(1); // her sayfada tekrar eder

        Font thFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
        // wt = weight, s = status, sutunu asagi kaydirmasin diye kisalttim.
        String[] headers = {"No", "Group", "Category", "Wt", "Date", "Diff", "Topic", "Note", "S"};
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, thFont));
            cell.setBackgroundColor(Color.LIGHT_GRAY);
            table.addCell(cell);
        }

        // Satırlar
        int rowNum = 0;
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd EEE", Locale.ENGLISH);
        LocalDate today = LocalDate.now(zoneId);

       // PdfContentByte cb = writer.getDirectContent();
       // Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

        for (DatedTopicViewModel d : allItems) {
            rowNum++;

            // No
            PdfPCell cell0 = fitTextToCell(String.format("%03d", rowNum), cellFontCourier, 25f);
            cell0.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell0.setVerticalAlignment(Element.ALIGN_MIDDLE);
            table.addCell(cell0);

            // Group
            table.addCell(fitTextToCell(d.getTopic().getCategory().getCategoryGroup().getName(),
                    cellFont, 70f));

            // Category
            table.addCell(fitTextToCell(d.getTopic().getCategory().getName(), cellFont, 90f));

            // Weight
            PdfPCell cell3 = fitTextToCell(String.format("%02d", d.getTopic().getWeight()), cellFontCourier, 30f);
            cell3.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell3.setVerticalAlignment(Element.ALIGN_MIDDLE);
            table.addCell(cell3);

            // Date
            PdfPCell cell4 = fitTextToCell(d.getDateLocal().format(fmt), cellFontCourier, 80f);
            cell4.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell4.setVerticalAlignment(Element.ALIGN_MIDDLE);
            table.addCell(cell4);


            // Diff
            int diff = (int) ChronoUnit.DAYS.between(today, d.getDateLocal());
            PdfPCell cell5 = fitTextToCell(String.format("%+05d", diff), cellFontCourier, 30f);
            cell5.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell5.setVerticalAlignment(Element.ALIGN_MIDDLE);
            table.addCell(cell5);

            // Topic
            table.addCell(fitTextToCell(d.getTopic().getName(), cellFont, 120f));

            // Note
            String note = d.getEntry() != null && d.getEntry().getNote() != null && d.getEntry().getNote().getContent() != null
                    ? d.getEntry().getNote().getContent()
                    : "";
            table.addCell(fitTextToCell(note, cellFont, 95f));

            // Status
            PdfPCell statusCell;
            switch (d.getStatus()) {
                case 1: statusCell = new PdfPCell(new Phrase("✔", cellFontCourier)); break; // ✔ = \u2714
                case 2: statusCell = new PdfPCell(new Phrase("⚠", cellFontCourier)); break; // ⚠
                case 0:
                    statusCell = new PdfPCell(new Phrase("", cellFontCourier));
                    statusCell.setBackgroundColor(Color.GRAY);
                    break;
                case 3:
                default: statusCell = new PdfPCell(new Phrase("☐", cellFontCourier)); break; // ☐
            }
            statusCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            statusCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            table.addCell(statusCell);
        }

        document.add(table);
        document.close();
    }

    private String limit(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) : s;
    }




    private PdfPCell fitTextToCell(String text, Font font, float maxWidth) {
        if (text == null) return null;

        // İlk satırı al (CR, LF, CRLF hepsi için)
        String firstLine = text.split("\r\n|\r|\n", 2)[0];

        // Hücreyi oluştur (önce boş içerikle, padding değerlerini okuyabilmek için)
        PdfPCell dummyCell = new PdfPCell();
        float paddingLeft = dummyCell.getPaddingLeft();
        float paddingRight = dummyCell.getPaddingRight();

        // Metin için gerçekten kullanılabilir alan
        float effectiveWidth = maxWidth - (paddingLeft + paddingRight);

        float width = font.getBaseFont().getWidthPoint(firstLine, font.getSize());
        if (width <= effectiveWidth) {
            PdfPCell cell = new PdfPCell(new Phrase(firstLine, font));
            cell.setNoWrap(true);
            return cell;
        }

        String tilda = "~";
        float tildaWidth = font.getBaseFont().getWidthPoint(tilda, font.getSize());

        StringBuilder sb = new StringBuilder();
        for (char c : firstLine.toCharArray()) {
            float w = font.getBaseFont().getWidthPoint(sb.toString() + c, font.getSize());

            int totalWidth = (int) Math.floor(w + tildaWidth);
            int limit = (int) Math.floor(effectiveWidth);

            if (totalWidth >= limit) break;
            sb.append(c);
        }

        PdfPCell pdfPCell = new PdfPCell(new Phrase(sb.toString() + tilda, font));
        pdfPCell.setNoWrap(true);

        return pdfPCell;
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



    private List<DatedTopicViewModel> getNegativeArchiveds(List<DatedTopicViewModel> negativeWeightItems) {
        // Arsivlenmis categorilere ait topicleri negativeWeightItems'den cikarip negativeWeightArchivedItems listesine ekliyoruz.
        List<DatedTopicViewModel> negativeWeightArchivedItems = new ArrayList<>();
        // archived olarak belirledigim categorylerdeki topicleri listeden cikarir:
        Iterator<DatedTopicViewModel> iterrr = negativeWeightItems.iterator();
        while (iterrr.hasNext()) {
            DatedTopicViewModel t = iterrr.next();
            if (t.getTopic().getCategory().isArchived()) {
                negativeWeightArchivedItems.add(t);
                iterrr.remove(); // Güvenli şekilde siler
            }
        }
        return  negativeWeightArchivedItems;

    }

    private List<DatedTopicViewModel> getArchiveds(List<DatedTopicViewModel> allItems) {
        // Arsivlenmis categorilere ait topicleri allItems'den cikarip archivedItems listesine ekliyoruz.
        // Bu allItems icinde negatif weight itemler yok, onlari ayirmistik, sonra onlar icin de aynisini yapiyoruz daha ileride.
        List<DatedTopicViewModel> archivedItems = new ArrayList<>();
        // archived olarak belirledigim categorylerdeki topicleri listeden cikarir:
        Iterator<DatedTopicViewModel> iter = allItems.iterator();
        while (iter.hasNext()) {
            DatedTopicViewModel t = iter.next();
            if (t.getTopic().getCategory().isArchived()) {
                archivedItems.add(t);
                iter.remove(); // Güvenli şekilde siler
            }
        }
        return  archivedItems;

    }

    private List<DatedTopicViewModel> getPredictions(List<Topic> predictions, ZoneId zoneId) {

        List<DatedTopicViewModel> predictionItems = new ArrayList<>();
        predictions.forEach(t -> {
            if (t.getPredictionDateMillisYmd() != null)
                predictionItems.add(new DatedTopicViewModel(t, t.getPredictionDateMillisYmd(), 3, zoneId, null));
        });

        return predictionItems;

    }


}
