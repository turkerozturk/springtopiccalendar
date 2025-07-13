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

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.turkerozturk.dtt.entity.Entry;
import com.turkerozturk.dtt.entity.Topic;
import com.turkerozturk.dtt.repository.EntryRepository;

import java.util.List;

@Service
public class EntryService {

    private final EntryRepository entryRepository;

    public EntryService(EntryRepository entryRepository) {
        this.entryRepository = entryRepository;
    }

    @Transactional
    public void deleteEntryById(Long id) {
        Entry entry = entryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Entry bulunamadı: " + id));

        Topic topic = entry.getTopic();
        topic.getActivities().remove(entry); // ilişkiyi kopar

        entryRepository.deleteById(id);
    }

    public List<Entry> findWarningsByCategory(Long categoryId) {

        return entryRepository.findByCategoryIdAndStatusOfWarningEntries(categoryId, 2);
    }

    public List<Entry> findNeutralsByCategory(Long categoryId, Long dateMillisYmd) {

        return entryRepository.findByCategoryIdAndStatusOfNeutralEntriesWithDateInterval(categoryId, dateMillisYmd);
    }

    public List<Entry> findDonesByCategory(Long categoryId, Long dateMillisYmd) {

        return entryRepository.findByCategoryIdAndStatusOfDoneEntriesWithDateInterval(categoryId, dateMillisYmd);
    }

    public List<Entry> findByTopicIdAndDateInterval(Long topicId, long startDateMillis, long endDateMillis) {
        return entryRepository.findByTopicIdAndDateInterval(topicId, startDateMillis, endDateMillis);
    }


}
