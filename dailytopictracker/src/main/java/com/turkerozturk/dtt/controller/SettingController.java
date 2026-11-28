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
package com.turkerozturk.dtt.controller;



import com.turkerozturk.dtt.dto.ActivityLevel;
import com.turkerozturk.dtt.dto.Gender;
import com.turkerozturk.dtt.entity.Setting;
import com.turkerozturk.dtt.helper.EntityIcons;
import com.turkerozturk.dtt.service.SettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import com.turkerozturk.dtt.helper.SettingHelper;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/settings")
@RequiredArgsConstructor
public class SettingController {

    private final SettingService settingService;
    private final SettingHelper settingHelper;

    @GetMapping
    public String page(Model model) {

        model.addAttribute("settings", settingService.findAll());
        model.addAttribute("showTreeDate", settingHelper.getBoolean(TopicTreeController.SHOW_DATE, true));
        model.addAttribute("showTreeTags", settingHelper.getBoolean(TopicTreeController.SHOW_TAGS, true));

        model.addAttribute("genders", Gender.values());
        model.addAttribute("activityLevels", ActivityLevel.values());

        return "settings/index";
    }

    @PostMapping("/entity-icons")
    public String setEntityIcons(@RequestParam(defaultValue = "false") boolean enabled) {
        settingService.setValue(EntityIcons.ENABLED_KEY, Boolean.toString(enabled));
        return "redirect:/settings";
    }

    @PostMapping("/topic-tree")
    @Transactional
    public String setTopicTreePreferences(@RequestParam(defaultValue = "false") boolean showLastEntryDate,
                                          @RequestParam(defaultValue = "false") boolean showTags) {
        settingService.setValue(TopicTreeController.SHOW_DATE, Boolean.toString(showLastEntryDate));
        settingService.setValue(TopicTreeController.SHOW_TAGS, Boolean.toString(showTags));
        return "redirect:/settings";
    }

    @PostMapping("/save")
    @ResponseBody
    public ResponseEntity<String> save(@RequestParam(required = false) Long id,
                                       @RequestParam String key, @RequestParam String value) {
        if (key.isBlank()) {
            return ResponseEntity.badRequest().body("Enter a setting key.");
        }

        Setting setting = id == null ? new Setting() : settingService.findById(id).orElse(null);
        if (setting == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("This setting no longer exists.");
        }

        setting.setKey(key);
        setting.setValue(value);

        try {
            settingService.save(setting);
        } catch (SettingService.DuplicateKeyException | DataIntegrityViolationException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("A setting with this key already exists.");
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping("/delete/{id}")
    @ResponseBody
    public ResponseEntity<?> delete(@PathVariable Long id) {

        settingService.deleteById(id);

        return ResponseEntity.ok().build();
    }
}