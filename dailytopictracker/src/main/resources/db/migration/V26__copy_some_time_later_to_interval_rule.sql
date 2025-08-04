-- V26__copy_some_time_later_to_interval_rule.sql

-- Set interval_rule = null if some_time_later is null, empty, or less than 1
UPDATE topics
SET interval_rule = NULL
WHERE some_time_later IS NULL
   OR TRIM(some_time_later) = ''
   OR CAST(some_time_later AS REAL) < 1;

-- Otherwise, set interval_rule as '1/value'
UPDATE topics
SET interval_rule = '1/' || some_time_later
WHERE some_time_later IS NOT NULL
  AND TRIM(some_time_later) != ''
  AND CAST(some_time_later AS REAL) >= 1;
