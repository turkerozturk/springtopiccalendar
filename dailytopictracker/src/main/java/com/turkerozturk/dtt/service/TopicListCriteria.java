package com.turkerozturk.dtt.service;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

public record TopicListCriteria(String q, Sort sort, Integer page, Long id, String description, Boolean pinned,
                                Integer weight, Long predictionDays, String intervalRule, String parser, String image,
                                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseDate,
                                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

    public TopicListCriteria {
        q = clean(q);
        description = clean(description);
        intervalRule = clean(intervalRule);
        parser = clean(parser);
        image = clean(image);
        sort = sort == null ? Sort.RELEVANCE : sort;
        page = page == null ? 0 : Math.max(0, page);
    }

    private static String clean(String value) {
        if (value == null) return "";
        return value.strip();
    }

    public boolean advanced() {
        return id != null || !description.isEmpty() || pinned != null || weight != null || predictionDays != null
                || !intervalRule.isEmpty() || !parser.isEmpty() || !image.isEmpty() || baseDate != null || endDate != null;
    }

    public String path(Long categoryId, int pageIndex) {
        var values = new LinkedHashMap<String, Object>();
        values.put("categoryId", categoryId);
        values.put("q", q);
        values.put("sort", sort == Sort.RELEVANCE ? null : sort);
        values.put("page", pageIndex == 0 ? null : pageIndex);
        values.put("id", id);
        values.put("description", description);
        values.put("pinned", pinned);
        values.put("weight", weight);
        values.put("predictionDays", predictionDays);
        values.put("intervalRule", intervalRule);
        values.put("parser", parser);
        values.put("image", image);
        values.put("baseDate", baseDate);
        values.put("endDate", endDate);

        var uri = UriComponentsBuilder.fromPath("/topics");
        Map<String, Object> variables = new LinkedHashMap<>();
        values.forEach((name, value) -> {
            if (value != null && !value.toString().isEmpty()) {
                uri.queryParam(name, "{" + name + "}");
                variables.put(name, value);
            }
        });
        return uri.encode().buildAndExpand(variables).toUriString();
    }

    public enum Sort {
        RELEVANCE("Best match / Name A–Z", "match_rank, t.name COLLATE dtt_topic_order, t.id"),
        NAME_ASC("Name A–Z", "t.name COLLATE dtt_topic_order ASC, t.id"),
        NAME_DESC("Name Z–A", "t.name COLLATE dtt_topic_order DESC, t.id"),
        ID_ASC("ID ascending", "t.id ASC"),
        ID_DESC("ID descending", "t.id DESC"),
        CATEGORY("Category A–Z", "c.name COLLATE dtt_topic_order, t.name COLLATE dtt_topic_order, t.id"),
        PINNED("Pinned first", "t.is_pinned DESC, t.name COLLATE dtt_topic_order, t.id"),
        WEIGHT("Weight high to low", "t.weight DESC, t.name COLLATE dtt_topic_order, t.id");

        private final String label;
        final String sql;

        Sort(String label, String sql) {
            this.label = label;
            this.sql = sql;
        }

        public String getLabel() {
            return label;
        }
    }
}
