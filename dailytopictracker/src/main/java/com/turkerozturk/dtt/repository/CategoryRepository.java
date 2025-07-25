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

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.turkerozturk.dtt.dto.CategoryEntryStatsDto;
import com.turkerozturk.dtt.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findAllByArchivedIsFalseOrderByCategoryGroupIdDescNameAsc();
    List<Category> findAllByArchivedIsFalseOrderByCategoryGroup_PriorityDescNameAsc();


    /* asagidaki sorguda son yani dorduncu SUM u kullanmiyoruz artik. Sorgudan gelen veri DTO listesini servis
    katmanindaki baska bir sorgu ile elde ettigimiz dogru degerleri o SUM ile gelen yanlis degerlerin yerine yaziyoruz.
    Peki niye burada silmedik cunku onu silersek DTO ya bir parametre eksik gittigi icin uygulama acilmadan hata verir.
    Peki servis katmaninda duzeltme yapmasaydik bu haliyle sorgu ise yariyor mu? Yariyor ama predictionCount degerini
    yanlis gosterir. Nasil yariyor o zaman, en azindan bir prediction olup olmadigini gosterir ki bu yeterli. Cunku
    bu bilgiyi kategori gruplari sayfasinda tiklanabilir bir link gosterip gostermemeye karar vermek icin kullaniyoruz.
    Ama artik servis katmaninda o parametreyi dogru sayisal predictionCount degeri ile duzelttikten sonra kullaniyoruz.
     */
    @Query(value = """
    SELECT new com.turkerozturk.dtt.dto.CategoryEntryStatsDto(
        c.id,
        c.name,
        SUM(CASE WHEN e.status = 2 THEN 1 ELSE 0 END),
        SUM(CASE WHEN e.status = 0 AND e.dateMillisYmd >= :today THEN 1 ELSE 0 END),
        SUM(CASE WHEN e.status = 1 AND e.dateMillisYmd = :today THEN 1 ELSE 0 END),
        SUM(CASE WHEN t.predictionDateMillisYmd <= :today THEN 1 ELSE 0 END)
    )
    FROM Category c
    LEFT JOIN c.topics t
    LEFT JOIN t.activities e
    GROUP BY c.id, c.name
    ORDER BY c.name
    """)
    List<CategoryEntryStatsDto> getCategoryEntryStats(@Param("today") Long todayDateYmd);


}
