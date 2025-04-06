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


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import turkerozturk.ptt.entity.Entry;

import java.util.List;
import java.util.Optional;

@Repository
public interface EntryRepository extends JpaRepository<Entry, Long>, EntryRepositoryCustom {

    Optional<Entry> findByTopicIdAndDateMillisYmd(Long topicId, Long dateMillisYmd);

    // Veya eğer birden fazla varsa:
    // List<Entry> findAllByTopicIdAndDateMillisYmd(Long topicId, Long dateMillisYmd);

    // Entry entity'sinde "topic" adında bir alan var,
    // bu alanın "id"si üzerinden filtre yapmak için:
    List<Entry> findByTopicId(Long topicId);
}
