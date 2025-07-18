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
package com.turkerozturk.dtt.controller;

import com.turkerozturk.dtt.dto.SwapRequest;
import com.turkerozturk.dtt.entity.CategoryGroup;
import com.turkerozturk.dtt.repository.CategoryGroupRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/category-groups")
public class CategoryGroupRestController {

    private final CategoryGroupRepository repo;

    public CategoryGroupRestController(CategoryGroupRepository repo) {
        this.repo = repo;
    }

    @PostMapping("/reorder")
    public ResponseEntity<Void> reorder(@RequestBody List<Long> idsInOrder) {
        for (int i = 0; i < idsInOrder.size(); i++) {
            Long id = idsInOrder.get(i);
            CategoryGroup group = repo.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid ID: " + id));
            group.setPriority(i + 1); // Priority 1'den başlasın
            repo.save(group);
        }
        return ResponseEntity.ok().build();
    }

    /**
     * Bu işlem transactional yapılırsa veritabanı tutarlılığı da garanti altına alınır:
     * Transactional
     * Ama SQLite'ta bu işlem tek connection'da olduğu sürece zaten atomik olacaktır.
     * @param request
     * @return
     */
    @PostMapping("/swap-priority")
    public ResponseEntity<Void> swapPriority(@RequestBody SwapRequest request) {
        if (request.getDraggedId().equals(request.getTargetId())) {
            return ResponseEntity.badRequest().build();
        }

        CategoryGroup dragged = repo.findById(request.getDraggedId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid dragged ID"));
        CategoryGroup target = repo.findById(request.getTargetId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid target ID"));

        Integer draggedPriority = dragged.getPriority();
        Integer targetPriority = target.getPriority();

        // 1. dragged'ı geçici değere al (örneğin -1)
        dragged.setPriority(-1);
        repo.save(dragged);

        // 2. target'a dragged'ın eski değerini ver
        target.setPriority(draggedPriority);
        repo.save(target);

        // 3. dragged'a target'ın eski değerini ver
        dragged.setPriority(targetPriority);
        repo.save(dragged);

        return ResponseEntity.ok().build();
    }



}

