CREATE TABLE settings (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    setting_key TEXT NOT NULL UNIQUE,
    setting_value TEXT
);

-- Başlangıç satırı (seçili profil yok)
INSERT INTO settings (setting_key, setting_value) VALUES ('selected_profile_id', NULL);
