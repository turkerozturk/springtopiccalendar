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

import com.turkerozturk.dtt.entity.Category;
import com.turkerozturk.dtt.entity.CategoryProfile;
import com.turkerozturk.dtt.entity.Setting;
import com.turkerozturk.dtt.repository.CategoryProfileRepository;
import com.turkerozturk.dtt.repository.CategoryRepository;
import com.turkerozturk.dtt.repository.SettingRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CategoryProfileService {

    private final CategoryRepository categoryRepository;
    private final CategoryProfileRepository categoryProfileRepository;
    private final SettingRepository settingRepository;

    private static final String SELECTED_PROFILE_KEY = "selected_profile_id";

    @Transactional
    public void activateProfile(Long profileId) {
        // Önce tüm kategorileri archived=true yap
        categoryRepository.findAll().forEach(c -> c.setArchived(true));
        categoryRepository.flush();

        // Seçilen profildeki kategorileri archived=false yap
        CategoryProfile profile = categoryProfileRepository.findById(profileId)
                .orElseThrow(() -> new RuntimeException("Profile not found"));

        for (Category c : profile.getCategories()) {
            c.setArchived(false);
        }
        categoryRepository.flush();

        // Settings tablosuna yaz
        Setting setting = settingRepository.findByKey(SELECTED_PROFILE_KEY)
                .orElse(new Setting());
        setting.setKey(SELECTED_PROFILE_KEY);
        setting.setValue(profileId.toString());
        settingRepository.save(setting);
    }


    public Optional<Long> getActiveProfileId() {
        return settingRepository.findByKey(SELECTED_PROFILE_KEY)
                .filter(s -> s.getValue() != null) // null olan değerleri at
                .map(s -> Long.valueOf(s.getValue()));
    }


    @Transactional
    public void resetAllCategoriesUnarchived() {
        categoryRepository.findAll().forEach(c -> c.setArchived(false));
        categoryRepository.flush();

        // Ayarı da sıfırlıyoruz
        settingRepository.findByKey(SELECTED_PROFILE_KEY).ifPresent(settingRepository::delete);
    }


    public Optional<CategoryProfile> findById(Long categoryProfileId) {
        return categoryProfileRepository.findById(categoryProfileId);
    }

    /**
     * CategoryProfile içindeki tüm Category ID’lerini döner
     */
    public List<Long> getCategoryIdsForProfile(Long profileId) {
        return categoryProfileRepository.findById(profileId)
                .map(profile -> profile.getCategories().stream()
                        .map(Category::getId)
                        .toList())
                .orElse(List.of());
    }

}


