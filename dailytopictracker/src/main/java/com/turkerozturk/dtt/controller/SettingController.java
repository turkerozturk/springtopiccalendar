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
import com.turkerozturk.dtt.service.SettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/settings")
@RequiredArgsConstructor
public class SettingController {

    private final SettingService settingService;

    @GetMapping
    public String page(Model model) {

        model.addAttribute("settings", settingService.findAll());

        StringBuilder sb = new StringBuilder();
        sb.append("<br/>");
        sb.append("Programdaki bazı değişkenler bu sayfadan ayarlanırsa, onlar için application.properties dosyası veya kodlar içine gömülmüş varsayılan değerler yerine, buradaki değerler geçerli olacaktır. Örnekler:");
        sb.append("<br/>");
        sb.append("");
        sb.append("<br/>");
        sb.append("Kalori Hesaplamasında Kullanılanlar");
        sb.append("<dl>");

        sb.append("<dt>");
        sb.append("human.age");
        sb.append("<dd/>");
        sb.append("Yaş");


        sb.append("<dt>");
        sb.append("human.gender");
        sb.append("<dd/>");
        for (Gender g : Gender.values()) {
            sb.append(g.toString());
            sb.append("&nbsp;");
        }
        sb.append("<dt>");
        sb.append("human.height");
        sb.append("<dd/>");
        sb.append("Boy");

        sb.append("<dt>");
        sb.append("human.ActivityLevel");
        sb.append("<dd>");

        for (ActivityLevel a :ActivityLevel.values()) {
            sb.append(a.toString());
            sb.append("&nbsp;");
            sb.append(a.getMultiplier());
            sb.append("<br/>");
        }
        sb.append("");
        sb.append("<br/>");
        sb.append("");
        sb.append("<br/>");
        sb.append("");

        String helpText = sb.toString();

        model.addAttribute("helpText", helpText);

        return "settings/index";
    }

    @PostMapping("/save")
    @ResponseBody
    public ResponseEntity<?> save(
            @RequestParam(required = false) Long id,
            @RequestParam String key,
            @RequestParam String value
    ) {


        Setting setting;

        if (id != null) {
            setting = settingService.findById(id);
        } else {
            setting = new Setting();
        }

        setting.setKey(key);
        setting.setValue(value);

        settingService.save(setting);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/delete/{id}")
    @ResponseBody
    public ResponseEntity<?> delete(@PathVariable Long id) {

        settingService.deleteById(id);

        return ResponseEntity.ok().build();
    }
}