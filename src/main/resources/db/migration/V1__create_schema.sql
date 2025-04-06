-- This file is part of the DailyTopicTracker project.
-- Please refer to the project's README.md file for additional details.
-- https://github.com/turkerozturk/springtopiccalendar
CREATE TABLE categories (id integer, name varchar(255) not null unique, primary key (id));
CREATE TABLE topics (category_id bigint not null, id integer, description varchar(255), name varchar(255), primary key (id));
CREATE TABLE entries (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    topic_id BIGINT NOT NULL,
    date_millis_ymd BIGINT,
    status INTEGER,
    FOREIGN KEY (topic_id) REFERENCES topics(id),
    UNIQUE (topic_id, date_millis_ymd)
);
CREATE TABLE notes (entry_id bigint not null unique, id integer, content TEXT, primary key (id));