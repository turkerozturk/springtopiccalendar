package com.turkerozturk.dtt.service;

import com.turkerozturk.dtt.component.AppTimeZoneProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class TopicDetailsService {
    private static final int RECENT_ENTRY_LIMIT = 5;

    private final JdbcTemplate jdbc;
    private final AppTimeZoneProvider timeZone;

    public TopicDetailsService(JdbcTemplate jdbc, AppTimeZoneProvider timeZone) {
        this.jdbc = jdbc;
        this.timeZone = timeZone;
    }

    public record RecentEntry(LocalDate date, Integer status, String note) {}

    public record TopicDetails(long id, String name, String description, long categoryId, String categoryName,
                               List<RecentEntry> recentEntries) {
        public LocalDate latestEntryDate() {
            return recentEntries.isEmpty() ? null : recentEntries.get(0).date();
        }
    }

    @Transactional(readOnly = true)
    public Optional<TopicDetails> findById(long topicId) {
        var topic = jdbc.query("""
                SELECT t.name, t.description, t.category_id, c.name AS category_name
                FROM topics t
                JOIN categories c ON c.id = t.category_id
                WHERE t.id = ?
                """, (row, number) -> new TopicDetails(topicId, row.getString("name"),
                row.getString("description"), row.getLong("category_id"),
                row.getString("category_name"), List.of()), topicId).stream().findFirst();

        return topic.map(detail -> new TopicDetails(detail.id(), detail.name(), detail.description(),
                detail.categoryId(), detail.categoryName(), recentEntries(topicId)));
    }

    private List<RecentEntry> recentEntries(long topicId) {
        var zone = timeZone.getZoneId();
        return jdbc.query("""
                SELECT e.date_millis_ymd, e.status, n.content
                FROM entries e
                LEFT JOIN notes n ON n.entry_id = e.id
                WHERE e.topic_id = ? AND e.date_millis_ymd IS NOT NULL
                ORDER BY e.date_millis_ymd DESC, e.id DESC
                LIMIT ?
                """, (row, number) -> new RecentEntry(
                Instant.ofEpochMilli(row.getLong("date_millis_ymd")).atZone(zone).toLocalDate(),
                row.getObject("status", Integer.class), row.getString("content")), topicId, RECENT_ENTRY_LIMIT);
    }
}
