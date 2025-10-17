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
package com.turkerozturk.dtt.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

@Data
@Entity
@Table(name = "notes")
@Indexed // 🔍 Bu entity indekslenecek
public class Note {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @GenericField(name = "idSort", sortable = Sortable.YES) // ← sortable olarak indeksle. dogrudan "id" yazimini kullanamadigimiz icin boyle bir isim veriyoruz. hibernate search ile ilgili.
    private Long id;

    @FullTextField //(analyzer = "turkish") // Türkçe dil analizoru, kullanmadim simdilik cunku tek dile gore.
    @Column(columnDefinition = "TEXT")
    private String content;

    @OneToOne(optional = false)
    @JoinColumn(name = "entry_id", nullable = false, unique = true)
    private Entry entry;

    public Note() {
    }

    public Note(String content) {
        this.content = content;
    }



}

