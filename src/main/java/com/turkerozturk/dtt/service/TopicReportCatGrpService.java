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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import com.turkerozturk.dtt.component.AppTimeZoneProvider;
import com.turkerozturk.dtt.entity.Topic;
import com.turkerozturk.dtt.repository.EntryRepository;
import com.turkerozturk.dtt.repository.TopicRepository;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
public class TopicReportCatGrpService {


    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private EntryRepository entryRepository;


    public List<Topic> getNextNeutralTopics(int limit, Long categoryGroupId) {
        ZoneId zoneId = AppTimeZoneProvider.getZone(); // ZoneId dynamic
        LocalDate today = LocalDate.now(zoneId);

        // Saat 00:00 epoch millis
        long todayEpochMillis = today
                .atStartOfDay(zoneId)
                .toInstant()
                .toEpochMilli();

        //List<Topic> result = topicRepository.findTop10FutureNeutralTopicsFromToday(todayEpochMillis);
        List<Topic> result = topicRepository.findFutureNeutralTopicsFromTodayForCategoryGroup(todayEpochMillis, categoryGroupId);


        // elle LIMIT 10 uygula (ekstra güvenlik için)
        return result.stream().limit(limit).toList();
    }

    // Yeni metod: prediction_date_millis_ymd <= bugünün millis değeri olanlar
    public List<Topic> getTopicsWithPredictionDateBeforeOrEqualToday(Long categoryGroupId) {
        ZoneId zoneId = AppTimeZoneProvider.getZone();
        LocalDate today = LocalDate.now(zoneId);
        long todayEpochMillis = today.atStartOfDay(zoneId).toInstant().toEpochMilli();

        return topicRepository.findTopicsWithPredictionDateBeforeOrEqualTodayForCategoryGroup(todayEpochMillis, categoryGroupId);
    }

    public List<Topic> getAllUrgentTopicsSortedByMostRecentWarning(Long categoryGroupId) {
        return topicRepository.findAllByWarningDateNotNullOrderByWarningDateDescForCategoryGroup(categoryGroupId);
    }

    public List<Topic> getLastActivitiesLimitedToN(int limit, Long categoryGroupId) {
        List<Topic> result = topicRepository.findAllByLastPastEntryDateNotNullOrderByDescForCategoryGroup(categoryGroupId);
        return result.stream().limit(limit).toList();
    }

    public List<Topic> getLastActivitiesLimitedToTodayAndThenToN(int limit, Long categoryGroupId) { // TODO
        ZoneId zoneId = AppTimeZoneProvider.getZone();
        LocalDate today = LocalDate.now(zoneId);
        long todayEpochMillis = today.atStartOfDay(zoneId).toInstant().toEpochMilli();

        List<Topic> todayList = topicRepository.findAllByLastPastEntryDateIsTodayForCategoryGroup(todayEpochMillis, categoryGroupId);
        List<Topic> previousList = topicRepository.findTopNByLastPastEntryDateBeforeTodayForCategoryGroup(todayEpochMillis,
                PageRequest.of(0, limit),
                categoryGroupId);

        List<Topic> combined = new ArrayList<>();
        combined.addAll(todayList);
        combined.addAll(previousList);
        return combined;
    }


}
