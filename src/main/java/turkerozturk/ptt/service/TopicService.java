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



import org.springframework.data.domain.PageRequest;
import turkerozturk.ptt.component.AppTimeZoneProvider;
import turkerozturk.ptt.entity.Entry;
import turkerozturk.ptt.entity.Topic;
import turkerozturk.ptt.repository.EntryRepository;
import turkerozturk.ptt.repository.TopicRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
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

    /*
      We need to run this on each topic or entry related event.
      Also run this for both old and new topic, if we move an existing entry to another topic.
     */
    public void updateTopicStatus(Long topicId) {
        Topic t = getTopicById(topicId).get();
        recalcPredictionDate(t);
        recalcLastPastEntryDate(t);
        recalcLastWarningEntryDate(t);
        recalcFirstFutureNeutralEntryDate(t);
        saveTopic(t);
    }

    /**
     * someTimeLater ve status=1 entry'ler üzerinden
     * topic.predictionDateMillisYmd değerini günceller.
     */
    private void recalcPredictionDate(Topic topic) {
        Long days = topic.getSomeTimeLater();
        if (days == null || days == 0) {
            topic.setPredictionDateMillisYmd(null);
            return;
        }


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


    private void recalcLastPastEntryDate(Topic topic) {
        List<Entry> entries = entryRepository.findByTopicIdAndStatus(topic.getId(), 1);
        if (entries.isEmpty()) {
            topic.setLastPastEntryDateMillisYmd(null);
            return;
        }

        ZoneId zoneId = AppTimeZoneProvider.getZone();
        LocalDate today = LocalDate.now(zoneId);

        // Geçmişte kalan, bugüne en yakın tarihli entry'yi bul
        Optional<Long> closestPastMillisOpt = entries.stream()
                .map(Entry::getDateMillisYmd)
                .filter(millis -> {
                    LocalDate entryDate = Instant.ofEpochMilli(millis)
                            .atZone(zoneId)
                            .toLocalDate();
                    return (entryDate.isBefore(today) || entryDate.isEqual(today));
                })
                .max(Long::compareTo); // bugüne en yakın olanı almak için max kullanıyoruz

        if (closestPastMillisOpt.isPresent()) {
            topic.setLastPastEntryDateMillisYmd(closestPastMillisOpt.get());
        } else {
            topic.setLastPastEntryDateMillisYmd(null);
        }
    }


    private void recalcLastWarningEntryDate(Topic topic) {
        ZoneId zoneId = AppTimeZoneProvider.getZone();

        Optional<LocalDate> maybeEarliestDate = topic.getActivities().stream()
                .filter(activity -> activity.getStatus() == 2)
                .map(activity -> Instant.ofEpochMilli(activity.getDateMillisYmd())
                        .atZone(zoneId)
                        .toLocalDate())
                .min(Comparator.naturalOrder());

        if (maybeEarliestDate.isPresent()) {
            LocalDate earliestDate = maybeEarliestDate.get();

            // Saat kısmı 00:00 olacak şekilde epoch millis
            long epochMillisYmd = earliestDate
                    .atStartOfDay(zoneId)
                    .toInstant()
                    .toEpochMilli();

            topic.setFirstWarningEntryDateMillisYmd(epochMillisYmd);
        } else {
            // Uygun entry yoksa null atanabilir
            topic.setFirstWarningEntryDateMillisYmd(null);
        }
    }


    private void recalcFirstFutureNeutralEntryDate(Topic topic) {
        ZoneId zoneId = AppTimeZoneProvider.getZone();
        LocalDate today = LocalDate.now(zoneId);

        // status = 0 olan ve tarihi bugünden büyük olan ilk activity tarihi
        Optional<LocalDate> firstFutureNeutralDateOpt = topic.getActivities().stream()
                .filter(activity -> activity.getStatus() == 0)
                .map(activity -> Instant.ofEpochMilli(activity.getDateMillisYmd()).atZone(zoneId).toLocalDate())
                .filter(entryDate -> (entryDate.isAfter(today) || entryDate.isEqual(today)))
                .sorted()
                .findFirst();

        if (firstFutureNeutralDateOpt.isPresent()) {
            LocalDate firstDate = firstFutureNeutralDateOpt.get();
            long millisYmd = firstDate.atStartOfDay(zoneId).toInstant().toEpochMilli();
            topic.setFirsFutureNeutralEntryDateMillisYmd(millisYmd);
        } else {
            topic.setFirsFutureNeutralEntryDateMillisYmd(null);
        }
    }

// TODO asagidaki metodlari ornegin bir topic raporlama servisine tasi

    public List<Topic> getNextNeutralTopics(int limit) {
        ZoneId zoneId = AppTimeZoneProvider.getZone(); // ZoneId dynamic
        LocalDate today = LocalDate.now(zoneId);

        // Saat 00:00 epoch millis
        long todayEpochMillis = today
                .atStartOfDay(zoneId)
                .toInstant()
                .toEpochMilli();

        List<Topic> result = topicRepository.findTop10FutureNeutralTopicsFromToday(todayEpochMillis);

        // elle LIMIT 10 uygula (ekstra güvenlik için)
        return result.stream().limit(limit).toList();
    }

    // Yeni metod: prediction_date_millis_ymd <= bugünün millis değeri olanlar
    public List<Topic> getTopicsWithPredictionDateBeforeOrEqualToday() {
        ZoneId zoneId = AppTimeZoneProvider.getZone();
        LocalDate today = LocalDate.now(zoneId);
        long todayEpochMillis = today.atStartOfDay(zoneId).toInstant().toEpochMilli();

        return topicRepository.findTopicsWithPredictionDateBeforeOrEqualToday(todayEpochMillis);
    }

    public List<Topic> getAllUrgentTopicsSortedByMostRecentWarning() {
        return topicRepository.findAllByWarningDateNotNullOrderByWarningDateDesc();
    }

    public List<Topic> getLastActivitiesLimitedToN(int limit) {
        List<Topic> result = topicRepository.findAllByLastPastEntryDateNotNullOrderByDesc();
        return result.stream().limit(limit).toList();
    }

    public List<Topic> getLastActivitiesLimitedToTodayAndThenToN(int limit) {
        ZoneId zoneId = AppTimeZoneProvider.getZone();
        LocalDate today = LocalDate.now(zoneId);
        long todayEpochMillis = today.atStartOfDay(zoneId).toInstant().toEpochMilli();

        List<Topic> todayList = topicRepository.findAllByLastPastEntryDateIsToday(todayEpochMillis);
        List<Topic> previousList = topicRepository.findTopNByLastPastEntryDateBeforeToday(todayEpochMillis, PageRequest.of(0, limit));

        List<Topic> combined = new ArrayList<>();
        combined.addAll(todayList);
        combined.addAll(previousList);
        return combined;
    }

    public List<Topic> getTopicsWithPredictionDateBeforeOrEqualToday(Long categoryId, Long dateMillisYmd) {
       return topicRepository.findByCategoryIdAndDateOfPredictionsWithDateInterval(categoryId, dateMillisYmd);
    }


}
