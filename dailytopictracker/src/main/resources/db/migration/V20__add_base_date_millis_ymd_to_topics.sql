ALTER TABLE topics
  ADD COLUMN base_date_millis_ymd BIGINT;

UPDATE topics
  SET base_date_millis_ymd = last_past_entry_date_millis_ymd;
