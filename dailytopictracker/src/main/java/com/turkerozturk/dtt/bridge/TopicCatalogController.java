package com.turkerozturk.dtt.bridge;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TopicCatalogController {
    private final JdbcTemplate jdbc;

    public TopicCatalogController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record Group(String id, String name) {}
    public record Category(String id, String name, String groupId) {}
    public record Topic(String id, String name, String categoryId) {}
    public record Catalog(List<Group> groups, List<Category> categories, List<Topic> topics) {}

    @GetMapping("/api/bridge/v1/topics")
    @Transactional(readOnly = true)
    public Catalog catalog() {
        var groups = jdbc.query("SELECT id, name FROM category_groups ORDER BY priority DESC, name, id",
                (row, index) -> new Group(row.getString(1), row.getString(2)));
        var categories = jdbc.query("SELECT id, name, category_group_number FROM categories ORDER BY name, id",
                (row, index) -> new Category(row.getString(1), row.getString(2), row.getString(3)));
        var topics = jdbc.query("SELECT id, name, category_id FROM topics ORDER BY name, id",
                (row, index) -> new Topic(row.getString(1), row.getString(2), row.getString(3)));
        return new Catalog(groups, categories, topics);
    }
}
