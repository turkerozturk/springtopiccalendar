package com.turkerozturk.dtt.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class CategoryListService {
    private static final int PAGE_SIZE = 25;

    private final JdbcTemplate jdbc;
    private final Locale locale;

    public CategoryListService(JdbcTemplate jdbc, @Value("${app.locale:en}") String language) {
        this.jdbc = jdbc;
        this.locale = Locale.forLanguageTag(language);
    }

    public record Row(long id, String name, long groupId, String groupName, boolean archived, long topicCount, String iconKey) {}

    public record Filter(String q, Long groupId, Boolean archived, Sort sort, Integer page) {
        public Filter {
            q = q == null ? "" : q.strip();
            sort = sort == null ? Sort.RELEVANCE : sort;
            page = page == null ? 0 : Math.max(0, page);
        }

        public String path(int pageIndex) {
            var uri = UriComponentsBuilder.fromPath("/categories")
                    .queryParamIfPresent("groupId", Optional.ofNullable(groupId))
                    .queryParamIfPresent("archived", Optional.ofNullable(archived));
            if (!q.isEmpty()) uri.queryParam("q", "{query}");
            if (sort != Sort.RELEVANCE) uri.queryParam("sort", sort);
            if (pageIndex > 0) uri.queryParam("page", pageIndex);
            return uri.encode().buildAndExpand(Map.of("query", q)).toUriString();
        }
    }

    public enum Sort {
        RELEVANCE("Best match / Name A–Z"),
        NAME_ASC("Name A–Z"),
        NAME_DESC("Name Z–A"),
        GROUP("Group A–Z"),
        MOST_TOPICS("Most topics"),
        FEWEST_TOPICS("Fewest topics"),
        NEWEST("ID descending"),
        OLDEST("ID ascending");

        private final String label;

        Sort(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    public Page<Row> browse(Filter filter, Long profileId) {
        // Fetch only category summaries and count topics without loading entries or JPA collections.
        var summaries = jdbc.query("""
                SELECT c.id, c.name, c.category_group_number AS group_id,
                       COALESCE(g.name, 'Unassigned') AS group_name, c.is_archived, COUNT(t.id) AS topic_count, c.icon_key
                FROM categories c
                LEFT JOIN category_groups g ON g.id = c.category_group_number
                LEFT JOIN topics t ON t.category_id = c.id
                WHERE (? IS NULL OR c.category_group_number = ?)
                  AND (? IS NULL OR c.is_archived = ?)
                  AND (? IS NULL OR c.id IN (
                      SELECT category_id FROM category_profile_categories WHERE profile_id = ?
                  ))
                GROUP BY c.id, c.name, c.category_group_number, g.name, c.is_archived, c.icon_key
                """, (row, number) -> new Row(row.getLong("id"), row.getString("name"),
                row.getLong("group_id"), row.getString("group_name"), row.getBoolean("is_archived"),
                row.getLong("topic_count"), row.getString("icon_key")), filter.groupId(), filter.groupId(), filter.archived(), filter.archived(), profileId, profileId);

        var collator = Collator.getInstance(locale);
        collator.setStrength(Collator.SECONDARY);
        Comparator<Row> byName = Comparator.comparing(Row::name, collator);
        String query = CatalogText.fold(filter.q(), locale);
        Comparator<Match> order = switch (filter.sort()) {
            case RELEVANCE -> Comparator.comparingInt(Match::rank).thenComparing(Match::row, byName);
            case NAME_ASC -> Comparator.comparing(Match::row, byName);
            case NAME_DESC -> Comparator.comparing(Match::row, byName.reversed());
            case GROUP -> Comparator.comparing(Match::row, Comparator.comparing(Row::groupName, collator).thenComparing(byName));
            case MOST_TOPICS -> Comparator.comparing(Match::row, Comparator.comparingLong(Row::topicCount).reversed().thenComparing(byName));
            case FEWEST_TOPICS -> Comparator.comparing(Match::row, Comparator.comparingLong(Row::topicCount).thenComparing(byName));
            case NEWEST -> Comparator.comparingLong((Match match) -> match.row().id()).reversed();
            case OLDEST -> Comparator.comparingLong(match -> match.row().id());
        };
        var matches = summaries.stream()
                .map(row -> new Match(row, query.isBlank() ? 0 : CatalogText.rank(CatalogText.fold(row.name(), locale), query)))
                .filter(match -> match.rank() >= 0)
                .sorted(order.thenComparingLong(match -> match.row().id()))
                .map(Match::row)
                .toList();
        int total = matches.size();
        var page = PageRequest.of(Math.min(filter.page(), Math.max(0, (total - 1) / PAGE_SIZE)), PAGE_SIZE);
        int start = (int) page.getOffset();
        return new PageImpl<>(matches.subList(start, Math.min(total, start + PAGE_SIZE)), page, total);
    }

    private record Match(Row row, int rank) {}
}
