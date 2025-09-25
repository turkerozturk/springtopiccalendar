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

import com.turkerozturk.dtt.component.AppTimeZoneProvider;
import com.turkerozturk.dtt.entity.Category;
import com.turkerozturk.dtt.entity.Entry;
import com.turkerozturk.dtt.entity.Topic;
import com.turkerozturk.dtt.service.EntryService;
import com.turkerozturk.dtt.service.TopicService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.info.GitProperties;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import com.turkerozturk.dtt.repository.CategoryGroupRepository;
import com.turkerozturk.dtt.repository.CategoryRepository;
import com.turkerozturk.dtt.repository.EntryRepository;
import com.turkerozturk.dtt.repository.TopicRepository;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Controller
public class IndexController {

    @Autowired(required = false)
    private BuildProperties buildProperties;

    @Autowired(required = false)
    private GitProperties gitProperties;

    @Autowired
    CategoryGroupRepository categoryGroupRepository;

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    TopicRepository topicRepository;

    @Autowired
    EntryRepository entryRepository;



    @GetMapping
    public String homePage(Model model) {

        ZoneId zoneId = AppTimeZoneProvider.getZone();

        // cok iyi anlatiyor: https://www.baeldung.com/spring-boot-build-properties
        // target/classes/META-INF/build-info.properties dosyasinda asagidaki degerler yaziyorsa burada kullanilabilir.
        // bu dosya mvn package komutunu calistirinca olusur.
        // pom.xml'deki spring-boot-maven-plugin ayarlariyla baglantili.
        model.addAttribute("buildVersion", buildProperties.getVersion());
        model.addAttribute("buildName", buildProperties.getName());
        model.addAttribute("buildDescription", buildProperties.get("description"));
        model.addAttribute("buildUrl", buildProperties.get("url"));
        model.addAttribute("buildOwner", buildProperties.get("owner"));

        String buildTime = buildProperties.getTime()
                .atZone(zoneId)
                .toLocalDateTime()
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        model.addAttribute("buildTime", buildTime);

        if (gitProperties != null) {
            // target/classes/git.properties dosyasinda asagidaki degerler yaziyorsa burada kullanilabilir.
            // Bu dosyayi olusturmamasi icin pom.xml'de
            // https://www.baeldung.com/spring-git-information

            model.addAttribute("commitId", gitProperties.getCommitId());
            model.addAttribute("commitLink", "https://github.com/turkerozturk/springtopiccalendar/commit/" + gitProperties.getCommitId());
            model.addAttribute("commitIdShort", gitProperties.getShortCommitId());
            model.addAttribute("commitIsDirty", gitProperties.get("dirty"));
            model.addAttribute("commitTime", gitProperties.get("commit.time"));
            model.addAttribute("commitCount", gitProperties.get("total.commit.count"));

        }

        model.addAttribute("categoryGroupsCount", categoryGroupRepository.count());
        model.addAttribute("categoriesCount", categoryRepository.count());
        model.addAttribute("topicsCount", topicRepository.count());
        model.addAttribute("entriesCount", entryRepository.count());





        return "index";
    }



}
