package com.turkerozturk.dtt.service;

import com.turkerozturk.dtt.component.AppTimeZoneProvider;
import com.turkerozturk.dtt.helper.EntityIcons;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.Collator;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class TopicTreeService {
    private final NamedParameterJdbcTemplate jdbc;
    private final AppTimeZoneProvider timeZone;
    private final Locale locale;

    public TopicTreeService(NamedParameterJdbcTemplate jdbc, AppTimeZoneProvider timeZone,
                            @Value("${app.locale:en}") String language) {
        this.jdbc = jdbc;
        this.timeZone = timeZone;
        this.locale = Locale.forLanguageTag(language);
    }

    public List<Node> groups(Long profileId, List<String> include, List<String> exclude) {
        var sql = new StringBuilder("""
                SELECT g.id AS group_id, g.name AS group_name, g.icon_key AS group_icon,
                       c.id AS category_id, c.name AS category_name, c.icon_key AS category_icon,
                       t.id AS topic_id, COALESCE(NULLIF(t.name, ''), 'Untitled topic') AS topic_name, t.icon_key AS topic_icon, tt.tag,
                       (SELECT MAX(e.date_millis_ymd) FROM entries e WHERE e.topic_id = t.id) AS last_entry
                FROM category_groups g
                LEFT JOIN categories c ON c.category_group_number = g.id AND c.is_archived = 0
                    AND (:profile IS NULL OR c.id IN (
                        SELECT category_id FROM category_profile_categories WHERE profile_id = :profile
                    ))
                LEFT JOIN topics t ON t.category_id = c.id
                LEFT JOIN topic_tags tt ON tt.topic_id = t.id
                WHERE (:profile IS NULL OR c.id IS NOT NULL)
                """);
        var parameters = new MapSqlParameterSource("profile", profileId);
        // During tag filtering, empty branches have no matching topics.
        if (!include.isEmpty() || !exclude.isEmpty()) sql.append(" AND t.id IS NOT NULL ");
        TopicTagService.appendTopicFilter(sql, parameters, include, exclude);
        sql.append(" ORDER BY g.priority DESC, g.id, c.id, t.id, tt.tag ");

        var collator = Collator.getInstance(locale);
        collator.setStrength(Collator.PRIMARY);
        Comparator<Node> byName = Comparator.comparing(Node::title, collator);
        return jdbc.query(sql.toString(), parameters, rows -> {
            Map<Long, Node> groups = new LinkedHashMap<>();
            Map<Long, Node> categories = new LinkedHashMap<>();
            Map<Long, Node> topics = new LinkedHashMap<>();
            while (rows.next()) {
                long groupId = rows.getLong("group_id");
                Node group = groups.get(groupId);
                if (group == null) {
                    group = folder("group-" + groupId, rows.getString("group_name"), rows.getString("group_icon"), "layer-group");
                    groups.put(groupId, group);
                }
                long categoryId = rows.getLong("category_id");
                if (rows.wasNull()) continue;
                Node category = categories.get(categoryId);
                if (category == null) {
                    category = folder("category-" + categoryId, rows.getString("category_name"), rows.getString("category_icon"), "folder");
                    categories.put(categoryId, category);
                    group.children().add(category);
                }
                long topicId = rows.getLong("topic_id");
                if (rows.wasNull()) continue;
                Node topic = topics.get(topicId);
                if (topic == null) {
                    topic = new Node("topic-" + topicId, rows.getString("topic_name"), false,
                            knownIcon(rows.getString("topic_icon"), "file-lines"), topicId,
                            entryDate(rows, "last_entry"), new ArrayList<>(), List.of());
                    topics.put(topicId, topic);
                    category.children().add(topic);
                }
                String tag = rows.getString("tag");
                if (tag != null) topic.tags().add(tag);
            }
            groups.values().forEach(group -> group.children().sort(byName));
            categories.values().forEach(category -> category.children().sort(byName));
            return List.copyOf(groups.values());
        });
    }

    public EntrySummary entrySummary(long topicId) {
        return jdbc.queryForObject("""
                SELECT MAX(date_millis_ymd) AS last_entry, COUNT(*) AS total
                FROM entries WHERE topic_id = :topic
                """, Map.of("topic", topicId), (rows, index) ->
                new EntrySummary(entryDate(rows, "last_entry"), rows.getLong("total")));
    }

    public record EntrySummary(LocalDate lastEntryDate, long count) {
    }

    private LocalDate entryDate(ResultSet rows, String column) throws SQLException {
        long millis = rows.getLong(column);
        return rows.wasNull() ? null : Instant.ofEpochMilli(millis).atZone(timeZone.getZoneId()).toLocalDate();
    }

    private static Node folder(String key, String title, String icon, String fallback) {
        return new Node(key, title, true, knownIcon(icon, fallback), null, null, List.of(), new ArrayList<>());
    }

    private static String knownIcon(String icon, String fallback) {
        return icon != null && EntityIcons.catalog().containsKey(icon) ? icon : fallback;
    }

    public record Node(String key, String title, boolean folder, String iconKey, Long topicId,
                       LocalDate lastEntryDate, List<String> tags, List<Node> children) {
    }
}
