package com.turkerozturk.dtt.service;

import com.turkerozturk.dtt.component.AppTimeZoneProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.sqlite.Collation;
import org.sqlite.Function;
import org.sqlite.SQLiteConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.Collator;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class TopicListService {
    public static final long ALL_CATEGORIES = -1;
    private static final int PAGE_SIZE = 25;
    private static final Pattern MARKS = Pattern.compile("\\p{M}+");
    private static final String MATCH = "dtt_topic_match";
    private static final String FOLD = "dtt_topic_fold";
    private static final String ORDER = "dtt_topic_order";
    private static final String FROM = " FROM topics t JOIN categories c ON c.id = t.category_id WHERE 1=1";

    private final JdbcTemplate jdbc;
    private final AppTimeZoneProvider timeZone;
    private final Locale locale;

    public TopicListService(JdbcTemplate jdbc, AppTimeZoneProvider timeZone, @Value("${app.locale:en}") String language) {
        this.jdbc = jdbc;
        this.timeZone = timeZone;
        this.locale = Locale.forLanguageTag(language);
    }

    public record TopicRow(long id, String name, String categoryName, boolean pinned, int weight) {}

    @Transactional(readOnly = true)
    public Page<TopicRow> browse(Long categoryId, TopicListCriteria criteria) {
        if (categoryId == null) {
            return Page.empty(PageRequest.of(0, PAGE_SIZE));
        }
        var filter = new StringBuilder(FROM);
        List<Object> values = new ArrayList<>();
        if (categoryId != ALL_CATEGORIES) {
            equalTo(filter, values, "t.category_id", categoryId);
        }
        String query = fold(criteria.q());
        if (!query.isBlank()) {
            filter.append(" AND dtt_topic_match(t.name) >= 0");
        }
        equalTo(filter, values, "t.id", criteria.id());
        equalTo(filter, values, "t.is_pinned", criteria.pinned() == null ? null : criteria.pinned() ? 1 : 0);
        equalTo(filter, values, "t.weight", criteria.weight());
        equalTo(filter, values, "t.some_time_later", criteria.predictionDays());
        contains(filter, values, "t.description", criteria.description());
        contains(filter, values, "t.interval_rule", criteria.intervalRule());
        contains(filter, values, "t.data_class_name", criteria.parser());
        contains(filter, values, "t.image_file_name", criteria.image());
        date(filter, values, "t.base_date_millis_ymd", criteria.baseDate());
        date(filter, values, "t.end_date_millis_ymd", criteria.endDate());
        return jdbc.execute((ConnectionCallback<Page<TopicRow>>) connection -> {
            var sqlite = connection.unwrap(SQLiteConnection.class);
            var collator = Collator.getInstance(locale);
            collator.setStrength(Collator.SECONDARY);
            Function.create(sqlite, MATCH, new Function() {
                @Override
                protected void xFunc() throws SQLException {
                    result(query.isBlank() ? 0 : rank(fold(value_text(0)), query));
                }
            }, 1, Function.FLAG_DETERMINISTIC);
            try {
                Function.create(sqlite, FOLD, new Function() {
                    @Override
                    protected void xFunc() throws SQLException {
                        result(fold(value_text(0)));
                    }
                }, 1, Function.FLAG_DETERMINISTIC);
                try {
                    Collation.create(sqlite, ORDER, new Collation() {
                        @Override
                        protected int xCompare(String left, String right) {
                            return collator.compare(left, right);
                        }
                    });
                    try {
                        return loadPage(connection, filter.toString(), values, criteria);
                    } finally {
                        Collation.destroy(sqlite, ORDER);
                    }
                } finally {
                    Function.destroy(sqlite, FOLD, 1);
                }
            } finally {
                Function.destroy(sqlite, MATCH, 1);
            }
        });
    }

    private Page<TopicRow> loadPage(Connection connection, String filter, List<Object> values, TopicListCriteria criteria)
            throws SQLException {
        long total;
        try (var statement = statement(connection, "SELECT COUNT(*)" + filter, values);
             var rows = statement.executeQuery()) {
            rows.next();
            total = rows.getLong(1);
        }
        int pageIndex = (int) Math.min(criteria.page(), Math.max(0, (total - 1) / PAGE_SIZE));
        var page = PageRequest.of(pageIndex, PAGE_SIZE);
        if (total == 0) return Page.empty(page);

        var sql = """
                SELECT t.id, t.name, c.name AS category_name, t.is_pinned, t.weight, dtt_topic_match(t.name) AS match_rank
                """ + filter + " ORDER BY " + criteria.sort().sql + " LIMIT ? OFFSET ?";
        var topics = new ArrayList<TopicRow>();
        try (var statement = statement(connection, sql, values)) {
            statement.setInt(values.size() + 1, page.getPageSize());
            statement.setLong(values.size() + 2, page.getOffset());
            try (var rows = statement.executeQuery()) {
                while (rows.next()) {
                    topics.add(new TopicRow(rows.getLong("id"), rows.getString("name"), rows.getString("category_name"),
                            rows.getBoolean("is_pinned"), rows.getInt("weight")));
                }
            }
        }
        return new PageImpl<>(topics, page, total);
    }

    private static PreparedStatement statement(Connection connection, String sql, List<Object> values) throws SQLException {
        var statement = connection.prepareStatement(sql);
        try {
            for (int i = 0; i < values.size(); i++) statement.setObject(i + 1, values.get(i));
            return statement;
        } catch (SQLException exception) {
            statement.close();
            throw exception;
        }
    }

    private static void equalTo(StringBuilder filter, List<Object> values, String column, Object value) {
        if (value != null) {
            filter.append(" AND ").append(column).append(" = ?");
            values.add(value);
        }
    }

    private void contains(StringBuilder filter, List<Object> values, String column, String text) {
        if (!text.isBlank()) {
            filter.append(" AND instr(dtt_topic_fold(").append(column).append("), ?) > 0");
            values.add(fold(text));
        }
    }

    private void date(StringBuilder filter, List<Object> values, String column, LocalDate day) {
        if (day == null) return;
        if (day.getYear() < 1 || day.getYear() > 9999) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        var zone = timeZone.getZoneId();
        filter.append(" AND ").append(column).append(" >= ? AND ").append(column).append(" < ?");
        values.add(day.atStartOfDay(zone).toInstant().toEpochMilli());
        values.add(day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli());
    }

    private String fold(String text) {
        if (text == null) return "";
        var normalized = Normalizer.normalize(text.toLowerCase(locale).replace('ı', 'i'), Normalizer.Form.NFD);
        return MARKS.matcher(normalized).replaceAll("");
    }

    private static int rank(String name, String query) {
        if (query.isBlank() || name.equals(query)) return 0;
        if (name.startsWith(query)) return 1;
        if (name.contains(query)) return 2;

        int from = 0;
        for (int offset = 0; offset < query.length();) {
            int letter = query.codePointAt(offset);
            offset += Character.charCount(letter);
            if (Character.isWhitespace(letter)) continue;
            int position = name.indexOf(letter, from);
            if (position < 0) return -1;
            from = position + Character.charCount(letter);
        }
        return 3;
    }
}
