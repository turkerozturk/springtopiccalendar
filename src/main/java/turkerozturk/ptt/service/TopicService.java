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
package turkerozturk.ptt.service;



import turkerozturk.ptt.entity.Entry;
import turkerozturk.ptt.entity.Topic;
import turkerozturk.ptt.repository.EntryRepository;
import turkerozturk.ptt.repository.TopicRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
// 1) Controller sınıfınıza ekleyin:



@Service
public class TopicService {
    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private EntryRepository entryRepository;

    public List<Topic> getAllTopics() {
        return topicRepository.findAll();
    }

    public List<Topic> getTopicsByCategoryId(Long categoryId) {
        return topicRepository.findByCategoryIdOrderByPinnedDescNameAsc(categoryId);
    }


    public Optional<Topic> getTopicById(Long id) {
        return topicRepository.findById(id);
    }

    public Topic saveTopic(Topic topic) {
        return topicRepository.save(topic);
    }

    public void deleteTopic(Long id) {
        Optional<Topic> optionalTopic = topicRepository.findById(id);
        if (optionalTopic.isPresent()) {
            Topic topic = optionalTopic.get();
            if (topic.getActivities() != null && !topic.getActivities().isEmpty()) {
                throw new IllegalStateException("Cannot delete topic with associated entries.");
            }
            topicRepository.delete(topic);
        }
    }




// ...

    /**
     * someTimeLater ve status=1 entry'ler üzerinden
     * topic.predictionDateMillisYmd değerini günceller.
     */
    public void recalcPredictionDate(Topic topic) {
        Long days = topic.getSomeTimeLater();
        if (days == null || days == 0) {
            topic.setPredictionDateMillisYmd(null);
            return;
        }

        // EntryRepository'de böyle bir metot yoksa
        // List<Entry> entries = entryRepository.findByTopicId(topic.getId());
        // olarak alıp filtreleyebilirsiniz.
        List<Entry> entries = entryRepository.findByTopicIdAndStatus(topic.getId(), 1);
        if (entries.isEmpty()) {
            topic.setPredictionDateMillisYmd(null);
            return;
        }

        long maxDateMillis = entries.stream()
                .mapToLong(Entry::getDateMillisYmd)
                .max()
                .getAsLong();

        LocalDate lastDate = Instant
                .ofEpochMilli(maxDateMillis)
                .atZone(ZoneId.systemDefault())
                .toLocalDate();

        LocalDate predDate = lastDate.plusDays(days);
        long predMillis = predDate
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();

        topic.setPredictionDateMillisYmd(predMillis);
    }



}
