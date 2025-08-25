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
import com.lowagie.text.pdf.*;
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

import java.awt.*;
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
    public void print(HttpServletResponse response) throws Exception {
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
        Document document = new Document(PageSize.A4, 36, 36, 54, 54); // marginler
        PdfWriter writer = PdfWriter.getInstance(document, response.getOutputStream());

        // Header & Footer
        writer.setPageEvent(new PdfPageEventHelper() {
            @Override
            public void onEndPage(PdfWriter writer, Document document) {
                PdfContentByte cb = writer.getDirectContent();
                Phrase footer = new Phrase(String.format("Page %d", writer.getPageNumber()),
                        FontFactory.getFont(FontFactory.HELVETICA, 8));
                ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                        footer,
                        (document.right() - document.left()) / 2 + document.leftMargin(),
                        document.bottom() - 10, 0);
            }
        });

        document.open();

        // Başlık
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
        Paragraph title = new Paragraph("Daily Topic Tracker Report", headerFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);
        document.add(Chunk.NEWLINE);

        // Tablo: kolon sayısını ayarlayalım
        PdfPTable table = new PdfPTable(9);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1f, 2f, 2f, 1f, 2f, 1.5f, 2f, 3f, 1f}); // 15.5 f
        table.setWidths(new float[]{0.8f, 2f, 2f, 0.6f, 2.8f, 1f, 2.3f, 3f, 1f});

        table.setHeaderRows(1); // her sayfada tekrar eder

        Font thFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
        String[] headers = {"No", "Group", "Category", "Wt", "Date", "Diff", "Topic", "Note", "Status"};
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, thFont));
            cell.setBackgroundColor(Color.LIGHT_GRAY);
            table.addCell(cell);
        }

        // Satırlar
        int rowNum = 0;
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd EEE", Locale.ENGLISH);
        LocalDate today = LocalDate.now(zoneId);

        for (DatedTopicViewModel d : allItems) {
            rowNum++;

            // No
            table.addCell(String.format("%03d", rowNum));

            // Group
            table.addCell(limit(d.getTopic().getCategory().getCategoryGroup().getName(), 10));

            // Category
            table.addCell(limit(d.getTopic().getCategory().getName(), 10));

            // Weight
            table.addCell(String.format("%02d", d.getTopic().getWeight()));

            // Date
            table.addCell(d.getDateLocal().format(fmt));

            // Diff
            int diff = (int) ChronoUnit.DAYS.between(today, d.getDateLocal());
            table.addCell(String.format("%+03d", diff));

            // Topic
            table.addCell(limit(d.getTopic().getName(), 10));

            // Note
            String note = d.getEntry() != null && d.getEntry().getNote() != null && d.getEntry().getNote().getContent() != null
                    ? d.getEntry().getNote().getContent()
                    : "";
            table.addCell(limit(note, 20));

            // Status
            PdfPCell statusCell;
            switch (d.getStatus()) {
                case 1: statusCell = new PdfPCell(new Phrase("✔")); break;
                case 2: statusCell = new PdfPCell(new Phrase("!")); break;
                case 0:
                    statusCell = new PdfPCell(new Phrase(""));
                    statusCell.setBackgroundColor(Color.GRAY);
                    break;
                case 3:
                default: statusCell = new PdfPCell(new Phrase("")); break;
            }
            table.addCell(statusCell);
        }

        document.add(table);
        document.close();
    }

    private String limit(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) : s;
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
