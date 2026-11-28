package com.turkerozturk.dtt.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class TopicTagService {
    private final NamedParameterJdbcTemplate jdbc;
    private final Locale locale;

    public TopicTagService(NamedParameterJdbcTemplate jdbc, @Value("${app.locale:en}") String language) {
        this.jdbc = jdbc;
        this.locale = Locale.forLanguageTag(language);
    }

    public List<String> allTags() {
        return sorted(jdbc.queryForList("SELECT DISTINCT tag FROM topic_tags", Map.of(), String.class));
    }

    public List<String> tagsForTopic(long topicId) {
        return sorted(jdbc.queryForList("SELECT tag FROM topic_tags WHERE topic_id = :topicId",
                Map.of("topicId", topicId), String.class));
    }

    public List<String> parseTags(String lines) {
        if (lines == null) return List.of();
        return lines.lines().map(String::strip).filter(tag -> !tag.isEmpty()).distinct().toList();
    }

    @Transactional
    public void addTags(long topicId, List<String> tags) {
        if (tags.isEmpty()) return;
        jdbc.batchUpdate("""
                INSERT INTO topic_tags (topic_id, tag) VALUES (:topicId, :tag)
                ON CONFLICT (topic_id, tag) DO NOTHING
                """, tags.stream().map(tag -> new MapSqlParameterSource("topicId", topicId).addValue("tag", tag))
                .toArray(MapSqlParameterSource[]::new));
    }

    @Transactional
    public void replaceTags(long topicId, List<String> tags) {
        jdbc.update("DELETE FROM topic_tags WHERE topic_id = :topicId", Map.of("topicId", topicId));
        addTags(topicId, tags);
    }

    public List<DayViewTopic> searchDayView(String query, List<String> include, List<String> exclude) {
        var sql = new StringBuilder("""
                SELECT t.id, t.name, c.name, t.weight
                FROM topics t JOIN categories c ON c.id = t.category_id
                WHERE t.name IS NOT NULL
                """);
        var parameters = new MapSqlParameterSource();
        appendTopicFilter(sql, parameters, include, exclude);

        // Keep the existing locale-aware substring search and alphabetical order.
        String needle = query.toLowerCase(locale);
        var matches = jdbc.query(sql.toString(), parameters, rows -> {
            var result = new ArrayList<DayViewTopic>();
            while (rows.next()) {
                String topicName = rows.getString(2);
                if (topicName.toLowerCase(locale).contains(needle)) {
                    result.add(DayViewTopic.from(rows.getLong(1), topicName, rows.getString(3), rows.getInt(4)));
                }
            }
            return result;
        });
        matches.sort(Comparator.comparing(DayViewTopic::topicName, collator()).thenComparingLong(DayViewTopic::id));
        return matches;
    }

    // The caller's topic table must use the alias t.
    static void appendTopicFilter(StringBuilder sql, MapSqlParameterSource parameters,
                                  List<String> include, List<String> exclude) {
        if (!include.isEmpty()) {
            sql.append("""
                     AND t.id IN (SELECT topic_id FROM topic_tags WHERE tag IN (:include))
                    """);
            parameters.addValue("include", include);
        }
        if (!exclude.isEmpty()) {
            sql.append("""
                     AND NOT EXISTS (SELECT 1 FROM topic_tags tt WHERE tt.topic_id = t.id AND tt.tag IN (:exclude))
                    """);
            parameters.addValue("exclude", exclude);
        }
    }

    private List<String> sorted(List<String> tags) {
        return tags.stream().sorted(collator().thenComparing(Comparator.naturalOrder())).toList();
    }

    private Comparator<String> collator() {
        Collator collator = Collator.getInstance(locale);
        collator.setStrength(Collator.PRIMARY);
        return collator::compare;
    }

    public record DayViewTopic(long id, String name, String topicName, String categoryName, int weight) {
        private static DayViewTopic from(long id, String topicName, String categoryName, int weight) {
            String name = HtmlUtils.htmlEscape(topicName) + " 🡰 <span class='category-name-in-search-box'>"
                    + HtmlUtils.htmlEscape(categoryName) + "[" + weight + "]</span>";
            return new DayViewTopic(id, name, topicName, categoryName, weight);
        }
    }
}
