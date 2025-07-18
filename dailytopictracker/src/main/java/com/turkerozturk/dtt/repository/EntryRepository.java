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
package com.turkerozturk.dtt.repository;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.turkerozturk.dtt.dto.TopicEntrySummaryDTO;
import com.turkerozturk.dtt.entity.Entry;

import java.util.List;
import java.util.Optional;

@Repository
public interface EntryRepository extends JpaRepository<Entry, Long>, EntryRepositoryCustom {

    // topicId filtreli, sayfalı sonuç döndürecek metot
    Page<Entry> findByTopicId(Long topicId, Pageable pageable);

    Optional<Entry> findByTopicIdAndDateMillisYmd(Long topicId, Long dateMillisYmd);

    // Veya eğer birden fazla varsa:
    // List<Entry> findAllByTopicIdAndDateMillisYmd(Long topicId, Long dateMillisYmd);

    // Entry entity'sinde "topic" adında bir alan var,
    // bu alanın "id"si üzerinden filtre yapmak için:
    List<Entry> findByTopicId(Long topicId);

    Page<Entry> findAllByOrderByDateMillisYmdDesc(Pageable pageable);
    Page<Entry> findByTopicIdOrderByDateMillisYmdDesc(Long topicId, Pageable pageable);



    List<Entry> findByTopicIdAndStatus(Long topicId, int i);


    @Query(value = """
        SELECT 
            cg.name AS categoryGroupName,
            c.name AS categoryName,
            t.name AS topicName,
            COUNT(*) AS itemCount,
            e.topic_id AS topicId,
            t.category_id AS categoryId,
            c.category_group_number AS categoryGroupNumber,
            t.prediction_date_millis_ymd AS predictionDateMillisYmd,
            t.last_past_entry_date_millis_ymd AS lastPastEntryDateMillisYmd,
            t.first_warning_entry_date_millis_ymd AS firstWarningEntryDateMillisYmd,
            t.first_future_neutral_entry_date_millis_ymd AS firstFutureNeutralEntryDateMillisYmd,
            t.some_time_later AS someTimeLater,
            t.weight AS weight,
            t.is_pinned AS pinned,
            c.is_archived AS archived,
            e.status AS status,
            ( COUNT(*) / ( ( (:endDateYmd - :startDateYmd) / (1000 * 86400) ) * 1.0 / t.some_time_later ) ) * 100 as ratio
        FROM entries e
        LEFT JOIN topics t ON e.topic_id = t.id
        LEFT JOIN categories c ON t.category_id = c.id
        LEFT JOIN category_groups cg ON c.category_group_number = cg.id
        WHERE e.status = :entryStatusNumber
          AND t.weight >= :topicWeight
          AND e.date_millis_ymd BETWEEN :startDateYmd AND :endDateYmd
        GROUP BY e.topic_id
        ORDER BY ratio DESC, itemCount DESC
        """, nativeQuery = true)
    List<TopicEntrySummaryDTO> findFilteredSummaries(
            @Param("entryStatusNumber") Integer entryStatusNumber,
            @Param("topicWeight") Integer topicWeight,
            @Param("startDateYmd") Long startDateYmd,
            @Param("endDateYmd") Long endDateYmd
    );


    @Query("SELECT COUNT(e) FROM Entry e WHERE e.dateMillisYmd >= :todayMillisYmd")
    int countEntriesWithDateGreaterThanEqual(@Param("todayMillisYmd") long todayMillisYmd);

    @Query("SELECT COUNT(e) FROM Entry e WHERE e.dateMillisYmd >= :todayMillisYmd AND e.topic.id = :topicId")
    int countEntriesWithDateGreaterThanEqualAndTopicId(@Param("todayMillisYmd") long todayMillisYmd,
                                                       @Param("topicId") Long topicId);


    /**
     * total warning entries of a category (from all topics of the category)
     * @param categoryId
     * @param status
     * @return
     */
    @Query("""
    SELECT e FROM Entry e
    WHERE e.status = 2 AND e.topic.category.id = :categoryId
    ORDER BY e.dateMillisYmd DESC
    """)
    List<Entry> findByCategoryIdAndStatusOfWarningEntries(@Param("categoryId") Long categoryId, @Param("status") int status);

    /**
     * total not marked entries of a category (from all topics of the category)
     * from specified date(usually today's date) to future dates
     * @param categoryId
     * @param todayMillisYmd
     * @return
     */
    @Query("""
    SELECT e FROM Entry e
    WHERE e.status = 0 AND e.topic.category.id = :categoryId
    AND e.dateMillisYmd >= :todayMillisYmd
    ORDER BY e.dateMillisYmd DESC
    """)
    List<Entry> findByCategoryIdAndStatusOfNeutralEntriesWithDateInterval(@Param("categoryId") Long categoryId, @Param("todayMillisYmd") Long todayMillisYmd);

    @Query("""
    SELECT e FROM Entry e
    WHERE e.status = 1 AND e.topic.category.id = :categoryId
    AND e.dateMillisYmd = :todayMillisYmd
    ORDER BY e.dateMillisYmd DESC
    """)
    List<Entry> findByCategoryIdAndStatusOfDoneEntriesWithDateInterval(@Param("categoryId") Long categoryId, @Param("todayMillisYmd") Long todayMillisYmd);

    @Query("SELECT e FROM Entry e WHERE " +
            "(:topicId IS NULL OR e.topic.id = :topicId) AND " +
            "e.dateMillisYmd BETWEEN :startDateMillis AND :endDateMillis")
    List<Entry> findByTopicIdAndDateInterval(
            @Param("topicId") Long topicId,
            @Param("startDateMillis") long startDateMillis,
            @Param("endDateMillis") long endDateMillis);


}
