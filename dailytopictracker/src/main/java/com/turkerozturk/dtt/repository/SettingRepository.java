package com.turkerozturk.dtt.repository;

import com.turkerozturk.dtt.entity.Setting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface SettingRepository extends JpaRepository<Setting, Long> {
    Optional<Setting> findByKey(String key);

    @Transactional
    @Modifying
    @Query(value = """
            INSERT INTO settings (setting_key, setting_value) VALUES (:key, :value)
            ON CONFLICT (setting_key) DO UPDATE SET setting_value = excluded.setting_value
            """, nativeQuery = true)
    void upsert(String key, String value);
}
