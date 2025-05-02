/*
 * This file is part of the DailyTopicTracker project.
 * Please refer to the project's README.md file for additional details.
 * https://github.com/turkerozturk/springtopiccalendar
 *
 * Copyright (c) 2025 Turker Ozturk
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/gpl-3.0.en.html>.
 */
package turkerozturk.ptt.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.stereotype.Repository;
import turkerozturk.ptt.entity.Entry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Repository
public class EntryRepositoryCustomImpl implements EntryRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    @Override
    public List<Entry> findEntriesByFilter(List<Long> topicIds, long startEpoch, long endEpoch, List<Integer> statuses) {

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Entry> cq = cb.createQuery(Entry.class);
        Root<Entry> root = cq.from(Entry.class);

        List<Predicate> predicates = new ArrayList<>();

        // dateMillisYmd >= startEpoch
        predicates.add(cb.greaterThanOrEqualTo(root.get("dateMillisYmd"), startEpoch));
        // dateMillisYmd <= endEpoch
        predicates.add(cb.lessThanOrEqualTo(root.get("dateMillisYmd"), endEpoch));

        // Topic filtresi (boş değilse)
        if (topicIds != null && !topicIds.isEmpty()) {
            predicates.add(root.get("topic").get("id").in(topicIds));
        }

        // Status filtresi (boş değilse)
        if (statuses != null && !statuses.isEmpty()) {
            predicates.add(root.get("status").in(statuses));
        }

        cq.where(predicates.toArray(new Predicate[0]));
        List<Entry> sorted = em.createQuery(cq).getResultList().stream()
                .sorted(Comparator.comparingLong(Entry::getDateMillisYmd).reversed())
                .collect(Collectors.toList());
        return sorted;
    }
}
