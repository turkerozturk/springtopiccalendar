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

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
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



}
