package com.turkerozturk.dtt.service;
import com.turkerozturk.dtt.dto.SleepDurationDto;
import com.turkerozturk.dtt.entity.Entry;
import com.turkerozturk.dtt.entity.Topic;
import com.turkerozturk.dtt.helper.SleepParser;
import com.turkerozturk.dtt.repository.EntryRepository;
import com.turkerozturk.dtt.repository.TopicRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class SleepService {



    private final TopicRepository topicRepository;
    private final EntryRepository entryRepository;

    private static final String SLEEP_DURATION_TAG = "#sleep.duration";

    public SleepService(
                            TopicRepository topicRepository,
                            EntryRepository entryRepository) {
        this.topicRepository = topicRepository;
        this.entryRepository = entryRepository;
    }




    public SleepDurationDto calculate(Long dateMillisYmd) {

        double sleepDuration = 0.0;
        boolean durationUpdated = false;

        Optional<Topic> sleepDurationTopicOpt =
                topicRepository.findFirstByDescriptionContaining(SLEEP_DURATION_TAG);

        Optional<long[]> parsedSleepDurationsOpt = sleepDurationTopicOpt
                .flatMap(topic ->
                        entryRepository.findEntryByTopicAndDate(dateMillisYmd, topic.getId())
                )
                .map(Entry::getNote)
                .filter(note -> note != null && note.getContent() != null)
                .flatMap(note -> SleepParser.extractSleepSeconds(note.getContent()));

        if (parsedSleepDurationsOpt.isPresent()) {

            long[] sleepDurations = parsedSleepDurationsOpt.get();

            // örnek:
            // toplam uyku süresi gerekiyorsa
            long totalSleepDuration = 0;

            for (long duration : sleepDurations) {
                totalSleepDuration += duration;
            }

            sleepDuration = totalSleepDuration;

            durationUpdated = true;
        }

        Long topicId = sleepDurationTopicOpt.map(Topic::getId).orElse(null);
        Long categoryId = sleepDurationTopicOpt
                .map(t -> t.getCategory().getId())
                .orElse(null);

        SleepDurationDto sleepDurationDto = new SleepDurationDto(sleepDuration, durationUpdated, topicId, categoryId);

        return sleepDurationDto;
    }
}
