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

import com.turkerozturk.dtt.component.HumanConfig;
import com.turkerozturk.dtt.dto.ActivityLevel;
import com.turkerozturk.dtt.dto.Gender;
import com.turkerozturk.dtt.dto.NutritionResultDto;
import com.turkerozturk.dtt.entity.Entry;
import com.turkerozturk.dtt.entity.Topic;
import com.turkerozturk.dtt.helper.NutritionCalculator;
import com.turkerozturk.dtt.helper.WeightParser;
import com.turkerozturk.dtt.repository.EntryRepository;
import com.turkerozturk.dtt.repository.TopicRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class NutritionService {

    private final HumanConfig humanConfig;
    private final TopicRepository topicRepository;
    private final EntryRepository entryRepository;

    private static final String HUMAN_WEIGHT_TAG = "#human.weight";

    public NutritionService(HumanConfig humanConfig,
                            TopicRepository topicRepository,
                            EntryRepository entryRepository) {
        this.humanConfig = humanConfig;
        this.topicRepository = topicRepository;
        this.entryRepository = entryRepository;
    }

    boolean weightUpdated = false;


    public NutritionResultDto calculate(Long dateMillisYmd) {

        double weight = humanConfig.getWeight();
        boolean weightUpdated = false;

        Optional<Topic> weightTopicOpt =
                topicRepository.findFirstByDescriptionContaining(HUMAN_WEIGHT_TAG);

        Optional<Double> parsedWeightOpt = weightTopicOpt
                .flatMap(topic ->
                        entryRepository.findEntryByTopicAndDate(dateMillisYmd, topic.getId())
                )
                .map(Entry::getNote)
                .filter(note -> note != null && note.getContent() != null)
                .flatMap(note -> WeightParser.extractWeight(note.getContent()));

        if (parsedWeightOpt.isPresent()) {
            weight = parsedWeightOpt.get();
            weightUpdated = true;
        }

        Long topicId = weightTopicOpt.map(Topic::getId).orElse(null);
        Long categoryId = weightTopicOpt
                .map(t -> t.getCategory().getId())
                .orElse(null);

        return NutritionCalculator.calculate(
                weight,
                weightUpdated,
                humanConfig.getHeight(),
                humanConfig.getAge(),
                Gender.valueOf(humanConfig.getGender()),
                ActivityLevel.valueOf(humanConfig.getActivityLevel()),
                topicId,
                categoryId
        );
    }
}