
ALTER TABLE category_groups RENAME TO category_groups_old;

CREATE TABLE category_groups (
    id   INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT    NOT NULL UNIQUE,
    priority INTEGER NOT NULL UNIQUE,
    background_color TEXT NOT NULL
);

INSERT INTO category_groups (
    id,
    name,
    priority,
    background_color
)
SELECT
    id,
    name,
    priority,
    '#f5f5f5'
FROM category_groups_old;

DROP TABLE category_groups_old;