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

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import turkerozturk.ptt.entity.Topic;

import java.util.List;

public interface TopicRepository extends JpaRepository<Topic, Long> {

    List<Topic> findByCategoryId(Long categoryId);

    List<Topic> findByCategoryIdOrderByPinnedDescNameAsc(Long categoryId);


    @Query(value = "SELECT t FROM Topic t " +
            "WHERE t.firstFutureNeutralEntryDateMillisYmd IS NOT NULL " +
            "AND t.firstFutureNeutralEntryDateMillisYmd >= :todayEpochMillis " +
            "ORDER BY t.firstFutureNeutralEntryDateMillisYmd DESC")
    List<Topic> findTop10FutureNeutralTopicsFromToday(@Param("todayEpochMillis") Long todayEpochMillis);

    /**
     * The difference from the query above is that it only contains one category group.
     * @param todayEpochMillis
     * @param categoryGroupNumber
     * @return
     */
    @Query("SELECT t FROM Topic t " +
            "LEFT JOIN t.category c " +
            "WHERE t.firstFutureNeutralEntryDateMillisYmd IS NOT NULL " +
            "AND t.firstFutureNeutralEntryDateMillisYmd >= :todayEpochMillis " +
            "AND c.categoryGroup.id = :categoryGroupNumber " +
            "ORDER BY t.firstFutureNeutralEntryDateMillisYmd DESC")
    List<Topic> findFutureNeutralTopicsFromTodayForCategoryGroup(
            @Param("todayEpochMillis") Long todayEpochMillis,
            @Param("categoryGroupNumber") Long categoryGroupNumber);


    @Query("SELECT t FROM Topic t " +
            "WHERE t.predictionDateMillisYmd IS NOT NULL " +
            "AND t.predictionDateMillisYmd <= :todayEpochMillis " +
            "ORDER BY t.predictionDateMillisYmd DESC")
    List<Topic> findTopicsWithPredictionDateBeforeOrEqualToday(@Param("todayEpochMillis") Long todayEpochMillis);

    /**
     * The difference from the query above is that it only contains one category group.
     * @param todayEpochMillis
     * @param categoryGroupNumber
     * @return
     */
    @Query("SELECT t FROM Topic t " +
            "LEFT JOIN t.category c " +
            "WHERE t.predictionDateMillisYmd IS NOT NULL " +
            "AND t.predictionDateMillisYmd <= :todayEpochMillis " +
            "AND c.categoryGroup.id = :categoryGroupNumber " +
            "ORDER BY t.predictionDateMillisYmd DESC")
    List<Topic> findTopicsWithPredictionDateBeforeOrEqualTodayForCategoryGroup(@Param("todayEpochMillis") Long todayEpochMillis,
                                                                               @Param("categoryGroupNumber") Long categoryGroupNumber);

    @Query("SELECT t FROM Topic t " +
            "WHERE t.firstWarningEntryDateMillisYmd IS NOT NULL " +
            "ORDER BY t.firstWarningEntryDateMillisYmd DESC")
    List<Topic> findAllByWarningDateNotNullOrderByWarningDateDesc();

    /**
     * The difference from the query above is that it only contains one category group.
     * @param categoryGroupNumber
     * @return
     */
    @Query("SELECT t FROM Topic t " +
            "LEFT JOIN t.category c " +
            "WHERE t.firstWarningEntryDateMillisYmd IS NOT NULL " +
            "AND c.categoryGroup.id = :categoryGroupNumber " +
            "ORDER BY t.firstWarningEntryDateMillisYmd DESC")
    List<Topic> findAllByWarningDateNotNullOrderByWarningDateDescForCategoryGroup(@Param("categoryGroupNumber") Long categoryGroupNumber);

    @Query("SELECT t FROM Topic t " +
            "WHERE t.lastPastEntryDateMillisYmd IS NOT NULL " +
            "ORDER BY t.lastPastEntryDateMillisYmd DESC")
    List<Topic> findAllByLastPastEntryDateNotNullOrderByDesc();

    /**
     * The difference from the query above is that it only contains one category group.
     * @param categoryGroupNumber
     * @return
     */
    @Query("SELECT t FROM Topic t " +
            "LEFT JOIN t.category c " +
            "WHERE t.lastPastEntryDateMillisYmd IS NOT NULL " +
            "AND c.categoryGroup.id = :categoryGroupNumber " +
            "ORDER BY t.lastPastEntryDateMillisYmd DESC")
    List<Topic> findAllByLastPastEntryDateNotNullOrderByDescForCategoryGroup(@Param("categoryGroupNumber") Long categoryGroupNumber);

    @Query("SELECT t FROM Topic t " +
            "WHERE t.lastPastEntryDateMillisYmd = :todayEpochMillis " +
            "ORDER BY t.lastPastEntryDateMillisYmd DESC")
    List<Topic> findAllByLastPastEntryDateIsToday(@Param("todayEpochMillis") Long todayEpochMillis);

    /**
     * The difference from the query above is that it only contains one category group.
     * @param todayEpochMillis
     * @param categoryGroupNumber
     * @return
     */
    @Query("SELECT t FROM Topic t " +
            "LEFT JOIN t.category c " +
            "WHERE t.lastPastEntryDateMillisYmd = :todayEpochMillis " +
            "AND c.categoryGroup.id = :categoryGroupNumber " +
            "ORDER BY t.lastPastEntryDateMillisYmd DESC")
    List<Topic> findAllByLastPastEntryDateIsTodayForCategoryGroup(@Param("todayEpochMillis") Long todayEpochMillis,
                                                  @Param("categoryGroupNumber") Long categoryGroupNumber);

    @Query("SELECT t FROM Topic t " +
            "WHERE t.lastPastEntryDateMillisYmd IS NOT NULL " +
            "AND t.lastPastEntryDateMillisYmd < :todayEpochMillis " +
            "ORDER BY t.lastPastEntryDateMillisYmd DESC")
    List<Topic> findTopNByLastPastEntryDateBeforeToday(@Param("todayEpochMillis") Long todayEpochMillis, Pageable pageable);

    /**
     * The difference from the query above is that it only contains one category group.
     * @param todayEpochMillis
     * @param pageable
     * @param categoryGroupNumber
     * @return
     */
    @Query("SELECT t FROM Topic t " +
            "LEFT JOIN t.category c " +
            "WHERE t.lastPastEntryDateMillisYmd IS NOT NULL " +
            "AND t.lastPastEntryDateMillisYmd < :todayEpochMillis " +
            "AND c.categoryGroup.id = :categoryGroupNumber " +
            "ORDER BY t.lastPastEntryDateMillisYmd DESC")
    List<Topic> findTopNByLastPastEntryDateBeforeTodayForCategoryGroup(@Param("todayEpochMillis") Long todayEpochMillis, Pageable pageable,
                                                                       @Param("categoryGroupNumber") Long categoryGroupNumber);

    @Query("""
    SELECT t FROM Topic t
    LEFT JOIN t.category c                
    WHERE c.id = :categoryId
    AND t.predictionDateMillisYmd <= :todayMillisYmd
    AND t.weight >= -1
    ORDER BY t.predictionDateMillisYmd DESC
    """)
    List<Topic> findByCategoryIdAndDateOfPredictionsWithDateInterval(@Param("categoryId") Long categoryId, @Param("todayMillisYmd") Long todayMillisYmd);


    @Query("""
    SELECT t.category.id, COUNT(t)
    FROM Topic t
    WHERE t.predictionDateMillisYmd <= :todayMillisYmd
    GROUP BY t.category.id
    """)
    List<Object[]> getPredictionCountsPerCategory(@Param("todayMillisYmd") Long todayMillisYmd);


}
