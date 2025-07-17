
ALTER TABLE category_groups RENAME TO category_groups_old;

CREATE TABLE category_groups (
    id   INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT    NOT NULL UNIQUE,
    priority INTEGER NOT NULL UNIQUE
);

INSERT INTO category_groups (
    id,
    name,
    priority
)
SELECT
    id,
    name,
    id as priority
FROM category_groups_old;

DROP TABLE category_groups_old;