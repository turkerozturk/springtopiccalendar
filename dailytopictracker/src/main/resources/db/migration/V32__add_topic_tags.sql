CREATE TABLE topic_tags (
    topic_id INTEGER NOT NULL REFERENCES topics(id) ON DELETE CASCADE,
    tag TEXT NOT NULL CHECK (length(trim(tag)) > 0),
    PRIMARY KEY (topic_id, tag)
);

CREATE INDEX idx_topic_tags_tag_topic ON topic_tags (tag, topic_id);

INSERT INTO topic_tags (topic_id, tag)
SELECT t.id, trim(c.name)
FROM topics t JOIN categories c ON c.id = t.category_id
WHERE length(trim(c.name)) > 0;

CREATE TRIGGER topics_add_default_tag
AFTER INSERT ON topics
BEGIN
    INSERT INTO topic_tags (topic_id, tag)
    SELECT NEW.id, trim(name) FROM categories
    WHERE id = NEW.category_id AND length(trim(name)) > 0;
END;

-- SQLite connections may have foreign-key enforcement disabled.
CREATE TRIGGER topics_remove_tags
AFTER DELETE ON topics
BEGIN
    DELETE FROM topic_tags WHERE topic_id = OLD.id;
END;
