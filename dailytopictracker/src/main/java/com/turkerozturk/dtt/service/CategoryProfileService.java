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
                .map(s -> Long.valueOf(s.getValue()));
    }

    @Transactional
    public void resetAllCategoriesUnarchived() {
        categoryRepository.findAll().forEach(c -> c.setArchived(false));
        categoryRepository.flush();

        // Ayarı da sıfırlıyoruz
        settingRepository.findByKey(SELECTED_PROFILE_KEY).ifPresent(settingRepository::delete);
    }

}


