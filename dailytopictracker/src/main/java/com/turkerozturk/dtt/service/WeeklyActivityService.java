package com.turkerozturk.dtt.service;

import com.turkerozturk.dtt.component.AppTimeZoneProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Arrays;

@Service
public class WeeklyActivityService {
    private static final int PAGE_SIZE = 8;
    private static final String DATED_ENTRIES = """
            FROM entries e
            JOIN topics t ON t.id = e.topic_id
            JOIN categories c ON c.id = t.category_id
            WHERE e.date_millis_ymd >= ? AND e.date_millis_ymd < ?
            """;

    private final JdbcTemplate jdbc;
    private final AppTimeZoneProvider timeZone;
    private final DayOfWeek weekStart;

    public WeeklyActivityService(JdbcTemplate jdbc, AppTimeZoneProvider timeZone,
                                 @Value("${week.start.day:MONDAY}") String weekStart) {
        this.jdbc = jdbc;
        this.timeZone = timeZone;
        String configuredDay = weekStart.trim();
        this.weekStart = Arrays.stream(DayOfWeek.values())
                .filter(day -> day.name().equalsIgnoreCase(configuredDay))
                .findFirst().orElse(DayOfWeek.MONDAY);
    }

    public record TopicActivity(long topicId, String topicName, String categoryName, LocalDate latestEntryDate,
                                long entryCount, long doneCount) {}

    public record WeeklyActivity(LocalDate fromDate, LocalDate throughDate, Page<TopicActivity> topics) {}

    @Transactional(readOnly = true)
    public WeeklyActivity currentWeek(int pageNumber) {
        var zone = timeZone.getZoneId();
        LocalDate today = LocalDate.now(zone);
        LocalDate fromDate = today.with(TemporalAdjusters.previousOrSame(weekStart));
        long fromMillis = fromDate.atStartOfDay(zone).toInstant().toEpochMilli();
        long untilMillis = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli();

        long topicCount = jdbc.queryForObject("SELECT COUNT(DISTINCT t.id) " + DATED_ENTRIES,
                Long.class, fromMillis, untilMillis);
        long lastPageIndex = topicCount == 0 ? 0 : (topicCount - 1) / PAGE_SIZE;
        int pageIndex = (int) Math.min(Math.max(pageNumber, 1) - 1, lastPageIndex);
        var page = PageRequest.of(pageIndex, PAGE_SIZE);
        var topics = jdbc.query("""
                SELECT t.id, t.name, c.name AS category_name, MAX(e.date_millis_ymd) AS latest,
                       COUNT(*) AS entry_count, COUNT(CASE WHEN e.status = 1 THEN 1 END) AS done_count
                """ + DATED_ENTRIES + """
                GROUP BY t.id, t.name, c.name
                ORDER BY latest DESC, t.name COLLATE NOCASE, t.id
                LIMIT ? OFFSET ?
                """, (row, number) -> new TopicActivity(row.getLong("id"), row.getString("name"),
                row.getString("category_name"),
                Instant.ofEpochMilli(row.getLong("latest")).atZone(zone).toLocalDate(),
                row.getLong("entry_count"), row.getLong("done_count")),
                fromMillis, untilMillis, page.getPageSize(), page.getOffset());

        return new WeeklyActivity(fromDate, today, new PageImpl<>(topics, page, topicCount));
    }
}
