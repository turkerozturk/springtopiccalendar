CREATE TABLE category_profile_categories (
    profile_id INTEGER NOT NULL,
    category_id INTEGER NOT NULL,
    PRIMARY KEY (profile_id, category_id),
    FOREIGN KEY (profile_id) REFERENCES category_profiles(id) ON DELETE CASCADE,
    FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE
);
