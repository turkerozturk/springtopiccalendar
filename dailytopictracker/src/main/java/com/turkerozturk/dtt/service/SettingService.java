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




import com.turkerozturk.dtt.entity.Setting;
import com.turkerozturk.dtt.repository.SettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SettingService {

    private final SettingRepository settingRepository;

    public List<Setting> findAll() {
        return settingRepository.findAll();
    }

    public Setting findById(Long id) {
        return settingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Setting not found"));
    }

    public Setting save(Setting setting) {

        settingRepository.findByKey(setting.getKey())
                .ifPresent(existing -> {

                    boolean differentEntity =
                            setting.getId() == null ||
                                    !existing.getId().equals(setting.getId());

                    if (differentEntity) {
                        throw new RuntimeException("Setting key already exists");
                    }
                });

        return settingRepository.save(setting);
    }

    public void deleteById(Long id) {
        settingRepository.deleteById(id);
    }
}
