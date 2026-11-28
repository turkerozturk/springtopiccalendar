package com.turkerozturk.dtt.service;

import com.turkerozturk.dtt.component.AppTimeZoneProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.sqlite.Collation;
import org.sqlite.Function;
import org.sqlite.SQLiteConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.text.Collator;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class DayTrackerService {
    private static final int PAGE_SIZE = 45;
    private static final String MATCH = "dtt_day_match";
    private static final String ORDER = "dtt_day_order";

    private final JdbcTemplate jdbc;
    private final AppTimeZoneProvider timeZone;
    private final Locale locale;

    public DayTrackerService(JdbcTemplate jdbc, AppTimeZoneProvider timeZone, @Value("${app.locale:en}") String language) {
        this.jdbc = jdbc;
        this.timeZone = timeZone;
        this.locale = Locale.forLanguageTag(language);
    }

    public Chunk browse(LocalDate date, Long profileId, String query, boolean descending, int page) {
        var zone = timeZone.getZoneId();
        long start = date.atStartOfDay(zone).toInstant().toEpochMilli();
        long end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli();
        String foldedQuery = CatalogText.fold(query, locale);
        return jdbc.execute((ConnectionCallback<Chunk>) connection -> {
            var sqlite = connection.unwrap(SQLiteConnection.class);
            var collator = Collator.getInstance(locale);
            collator.setStrength(Collator.PRIMARY);
            Collation.create(sqlite, ORDER, new Collation() {
                @Override
                protected int xCompare(String left, String right) {
                    return collator.compare(left, right);
                }
            });
            try {
                Function.create(sqlite, MATCH, new Function() {
                    @Override
                    protected void xFunc() throws SQLException {
                        boolean matches = foldedQuery.isBlank()
                                || CatalogText.rank(CatalogText.fold(value_text(0), locale), foldedQuery) >= 0;
                        result(matches ? 1 : 0);
                    }
                }, 1, Function.FLAG_DETERMINISTIC);
                try {
                    return loadChunk(connection, profileId, start, end, descending, page);
                } finally {
                    Function.destroy(sqlite, MATCH, 1);
                }
            } finally {
                Collation.destroy(sqlite, ORDER);
            }
        });
    }

    private Chunk loadChunk(Connection connection, Long profileId, long start, long end, boolean descending,
                            int page) throws SQLException {
        String sql = """
                SELECT cg.id AS group_id, cg.name AS group_name, c.id AS category_id, c.name AS category_name,
                       t.id AS topic_id, t.name AS topic_name, e.id AS entry_id, e.status,
                       NULLIF(trim(n.content, char(9,10,11,12,13,32)), '') IS NOT NULL AS has_note,
                       COALESCE(t.prediction_date_millis_ymd >= ? AND t.prediction_date_millis_ymd < ?, 0) AS predicted
                FROM topics t
                JOIN categories c ON c.id = t.category_id
                JOIN category_groups cg ON cg.id = c.category_group_number
                LEFT JOIN entries e ON e.id = (SELECT de.id FROM entries de
                                             WHERE de.topic_id = t.id AND de.date_millis_ymd >= ? AND de.date_millis_ymd < ?
                                             ORDER BY de.date_millis_ymd LIMIT 1)
                LEFT JOIN notes n ON n.entry_id = e.id
                WHERE c.is_archived = 0
                  AND (? IS NULL OR EXISTS (SELECT 1 FROM category_profile_categories pc
                                           WHERE pc.profile_id = ? AND pc.category_id = c.id))
                  AND dtt_day_match(t.name) = 1
                ORDER BY cg.priority DESC, cg.id, c.name COLLATE dtt_day_order, c.id,
                         t.name COLLATE dtt_day_order %s, t.id
                LIMIT ? OFFSET ?
                """.formatted(descending ? "DESC" : "ASC");
        var result = new ArrayList<TopicRow>(PAGE_SIZE + 1);
        try (var statement = connection.prepareStatement(sql)) {
            statement.setLong(1, start);
            statement.setLong(2, end);
            statement.setLong(3, start);
            statement.setLong(4, end);
            statement.setObject(5, profileId);
            statement.setObject(6, profileId);
            statement.setInt(7, PAGE_SIZE + 1);
            statement.setLong(8, (long) page * PAGE_SIZE);
            try (var rows = statement.executeQuery()) {
                while (rows.next()) {
                    long id = rows.getLong("entry_id");
                    Long entryId = rows.wasNull() ? null : id;
                    int code = rows.getInt("status");
                    Integer status = rows.wasNull() ? null : code;
                    result.add(new TopicRow(rows.getLong("group_id"), rows.getString("group_name"),
                            rows.getLong("category_id"), rows.getString("category_name"),
                            rows.getLong("topic_id"), rows.getString("topic_name"),
                            entryId, status,
                            rows.getBoolean("has_note"), rows.getBoolean("predicted")));
                }
            }
        }
        boolean hasMore = result.size() > PAGE_SIZE;
        if (hasMore) result.remove(PAGE_SIZE);
        return new Chunk(result, hasMore);
    }

    public record Chunk(List<TopicRow> topics, boolean hasMore) {}

    public record TopicRow(long groupId, String groupName, long categoryId, String categoryName,
                           long topicId, String topicName, Long entryId, Integer status,
                           boolean hasNote, boolean predicted) {
        public String statusLabel() {
            if (status == null) return "No entry";
            return switch (status) {
                case 1 -> "Done";
                case 2 -> "Warning";
                default -> "Not marked";
            };
        }
    }
}
