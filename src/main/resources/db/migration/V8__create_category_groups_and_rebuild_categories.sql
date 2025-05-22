-- steal-table trick for SQLite
ALTER TABLE categories     RENAME TO categories_old;
CREATE TABLE category_groups (
    id   INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT    NOT NULL UNIQUE
);
CREATE TABLE categories (
    id                     INTEGER PRIMARY KEY AUTOINCREMENT,
    name                   TEXT    NOT NULL UNIQUE,
    is_archived            INTEGER NOT NULL DEFAULT 0,
    category_group_number  INTEGER NOT NULL DEFAULT 0,
    -- …all your other columns here…
    FOREIGN KEY (category_group_number)
      REFERENCES category_groups(id)
      ON DELETE CASCADE
);
-- copy data across (new column will be NULL/0 by default)
INSERT INTO categories (
    id,
    name,
    is_archived,
    category_group_number
    -- …plus any other columns you listed above…
)
SELECT
    id,
    name,
    is_archived,
    category_group_number
    -- …plus those same other columns…
FROM categories_old;
DROP TABLE categories_old;
