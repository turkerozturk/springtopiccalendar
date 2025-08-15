-- V27__fill_data_class_name_in_topics.sql

UPDATE topics
SET data_class_name = 'GeneralParser'
WHERE (data_class_name IS NULL OR TRIM(data_class_name) = '');
