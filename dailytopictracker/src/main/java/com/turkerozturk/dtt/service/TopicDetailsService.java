package com.turkerozturk.dtt.service;

import com.turkerozturk.dtt.component.AppTimeZoneProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
    private static final int HISTORY_PAGE_SIZE = 25;
    private static final String ENTRY_DATE_RANGE = "WHERE e.topic_id = ? AND e.date_millis_ymd BETWEEN ? AND ?";

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
                detail.categoryId(), detail.categoryName(),
                findEntries(topicId, Long.MIN_VALUE, Long.MAX_VALUE, PageRequest.of(0, RECENT_ENTRY_LIMIT))));
    }

    @Transactional(readOnly = true)
    public Page<RecentEntry> findHistory(long topicId, LocalDate fromDate, LocalDate toDate, int pageIndex) {
        var zone = timeZone.getZoneId();
        long fromMillis = fromDate == null ? Long.MIN_VALUE : fromDate.atStartOfDay(zone).toInstant().toEpochMilli();
        long throughMillis = toDate == null ? Long.MAX_VALUE :
                toDate.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1;

        long total = jdbc.queryForObject("SELECT COUNT(*) FROM entries e " + ENTRY_DATE_RANGE,
                Long.class, topicId, fromMillis, throughMillis);
        long lastPageIndex = total == 0 ? 0 : (total - 1) / HISTORY_PAGE_SIZE;
        var page = PageRequest.of((int) Math.min(pageIndex, lastPageIndex), HISTORY_PAGE_SIZE);
        return new PageImpl<>(findEntries(topicId, fromMillis, throughMillis, page), page, total);
    }

    private List<RecentEntry> findEntries(long topicId, long fromMillis, long throughMillis, Pageable page) {
        var zone = timeZone.getZoneId();
        return jdbc.query("""
                SELECT e.date_millis_ymd, e.status, n.content
                FROM entries e
                LEFT JOIN notes n ON n.entry_id = e.id
                """ + ENTRY_DATE_RANGE + """

                ORDER BY e.date_millis_ymd DESC, e.id DESC
                LIMIT ? OFFSET ?
                """, (row, number) -> new RecentEntry(
                Instant.ofEpochMilli(row.getLong("date_millis_ymd")).atZone(zone).toLocalDate(),
                row.getObject("status", Integer.class), row.getString("content")),
                topicId, fromMillis, throughMillis, page.getPageSize(), page.getOffset());
    }
}
