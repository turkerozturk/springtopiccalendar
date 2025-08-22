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

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import com.turkerozturk.dtt.entity.Category;
import com.turkerozturk.dtt.entity.CategoryGroup;
import com.turkerozturk.dtt.entity.Entry;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface CategoryGroupRepository extends JpaRepository<CategoryGroup, Long> {

    /**
     * Returns true if a CategoryGroup with the given name already exists.
     * Spring Data derives the query automatically from the method name.
     */
    boolean existsByName(String name);

    List<CategoryGroup> findAllByOrderByIdDesc();

    @EntityGraph(attributePaths = "categories")
    List<CategoryGroup> findAllByOrderByPriorityDesc();


    Optional<CategoryGroup> findByName(String name);

    @Query("SELECT MAX(c.priority) FROM CategoryGroup c")
    Optional<Integer> findMaxPriority();

}
