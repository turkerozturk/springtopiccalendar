
-- V24: Set base_date_millis_ymd to NULL if it is equal to last_past_entry_date_millis_ymd
UPDATE topics
SET base_date_millis_ymd = NULL
WHERE base_date_millis_ymd = last_past_entry_date_millis_ymd;
