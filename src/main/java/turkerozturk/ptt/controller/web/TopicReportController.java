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
package turkerozturk.ptt.controller.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import turkerozturk.ptt.component.AppTimeZoneProvider;
import turkerozturk.ptt.dto.DatedTopicViewModel;
import turkerozturk.ptt.entity.Topic;
import turkerozturk.ptt.service.TopicService;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

@Controller
@RequestMapping("/reports")
public class TopicReportController {

    private final AppTimeZoneProvider timeZoneProvider;



    @Autowired
    private TopicService topicService;

    public TopicReportController(AppTimeZoneProvider timeZoneProvider) {
        this.timeZoneProvider = timeZoneProvider;
    }

    @GetMapping("/neutral")
    public String showFutureNeutralTopics(Model model) {
        List<Topic> topics = topicService.getNextNeutralTopics(10);
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
        List<Topic> topics = topicService.getLastActivitiesLimitedToN(1000);
        model.addAttribute("topics", topics);
        return "future-neutral-topics";
    }


    @GetMapping("/all")
    public String showDoneWarnPredNeutTopics(Model model) {
        ZoneId zoneId = AppTimeZoneProvider.getZone();

        List<Topic> importants = topicService.getAllUrgentTopicsSortedByMostRecentWarning();
        List<Topic> neutrals = topicService.getNextNeutralTopics(10);
        List<Topic> predictions = topicService.getTopicsWithPredictionDateBeforeOrEqualToday();
        List<Topic> finisheds = topicService.getLastActivitiesLimitedToTodayAndThenToN(10);

        List<DatedTopicViewModel> allItems = new ArrayList<>();
        List<DatedTopicViewModel> negativeWeightItems = new ArrayList<>();

        importants.forEach(t -> {
            if (t.getFirstWarningEntryDateMillisYmd() != null) {
                allItems.add(new DatedTopicViewModel(t, t.getFirstWarningEntryDateMillisYmd(), 2, zoneId));


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

        neutrals.forEach(t -> {
            if (t.getFirstFutureNeutralEntryDateMillisYmd() != null)
                allItems.add(new DatedTopicViewModel(t, t.getFirstFutureNeutralEntryDateMillisYmd(), 0, zoneId));


            Iterator<Topic> predIterator = predictions.iterator();
            while (predIterator.hasNext()) {
                Topic predTopic = predIterator.next();
                if (t.getFirstFutureNeutralEntryDateMillisYmd() != null && t.getFirstFutureNeutralEntryDateMillisYmd().equals(predTopic.getPredictionDateMillisYmd())
                        && t.getId().equals(predTopic.getId())
                ) {
                    predIterator.remove(); // Güvenli şekilde siler
                }
            }


        });

        finisheds.forEach(t -> {
            if (t.getLastPastEntryDateMillisYmd() != null)
                allItems.add(new DatedTopicViewModel(t, t.getLastPastEntryDateMillisYmd(), 1, zoneId));

            Iterator<Topic> predIterator = predictions.iterator();
            while (predIterator.hasNext()) {
                Topic predTopic = predIterator.next();
                if (t.getLastPastEntryDateMillisYmd() != null && t.getLastPastEntryDateMillisYmd().equals(predTopic.getPredictionDateMillisYmd())
                        && t.getId().equals(predTopic.getId())
                ) {
                    predIterator.remove(); // Güvenli şekilde siler
                }
            }



        });


        predictions.forEach(t -> {
            if (t.getPredictionDateMillisYmd() != null)
                allItems.add(new DatedTopicViewModel(t, t.getPredictionDateMillisYmd(), 3, zoneId));
        });



        allItems.sort(Comparator.comparing(DatedTopicViewModel::getDateLocal).reversed());

        // predictionlar cakisiyorsa:
        /*
        predictions.forEach(t -> {
            Iterator<DatedTopicViewModel> iteratorForDuplicates = allItems.iterator();
            while (iteratorForDuplicates.hasNext()) {
                DatedTopicViewModel dt = iteratorForDuplicates.next();
                if (dt.getTopic().getFirstFutureNeutralEntryDate() !=null && dt.getTopic().getFirstFutureNeutralEntryDate().equals(t.getPredictionDate())
                && dt.getTopic().getId().equals(t.getId())

                ) {

                    iteratorForDuplicates.remove();
                } else
                if (dt.getTopic().getFirstWarningEntryDate() !=null && dt.getTopic().getFirstWarningEntryDate().equals(t.getPredictionDate())
                        && dt.getTopic().getId().equals(t.getId())

                ) {
                    iteratorForDuplicates.remove();
                } else
                if (dt.getTopic().getLastPastEntryDate() !=null && dt.getTopic().getLastPastEntryDate().equals(t.getPredictionDate())

                        && dt.getTopic().getId().equals(t.getId())

                ) {
                    iteratorForDuplicates.remove();
                }
            }
        });
        */


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

        allItems.addAll(negativeWeightItems);

        model.addAttribute("allTopics", allItems);
        model.addAttribute("today", LocalDate.now(zoneId));
        model.addAttribute("zoneId", zoneId);
        model.addAttribute("negativeWeightItems", negativeWeightItems);

        return "report-all-statuses";
    }



}
