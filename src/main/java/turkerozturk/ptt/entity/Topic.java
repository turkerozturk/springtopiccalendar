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
package turkerozturk.ptt.entity;


import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import turkerozturk.ptt.component.AppTimeZoneProvider;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "topics")
public class Topic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    private String name;
    private String description;

    @OneToMany(mappedBy = "topic", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<Entry> activities = new ArrayList<>();

    @Column(name = "some_time_later")
    private Long someTimeLater;

    @Column(name = "is_pinned", nullable = false)
    private boolean pinned;

    /**
     * -- GETTER --
     * Veritabanında kaydı yok, 2025-05-15 gibi string biçiminde ymd tarih için
     */ /*
    @Transient
    private String predictionDateFormatted;
    */ // --- normal getter/setter for predictionDateMillisYmd ---
    // 13 haneli epoch time (sadece tarih, saat bilgileri 0)
    @Getter
    @Column(name = "prediction_date_millis_ymd")
    private Long predictionDateMillisYmd;


    /** Veritabanında kaydı yok, sadece hesaplamak için */
    @Transient
    private LocalDate predictionDate;

    public void setPredictionDateMillisYmd(Long predictionDateMillisYmd) {
        this.predictionDateMillisYmd = predictionDateMillisYmd;
        // isteğe bağlı: burada da güncelleyebilirsiniz
        this.predictionDate = null;
    }

    /**
     * ZoneId’yi AppTimeZoneProvider’dan alıp
     * sadece tarih (LocalDate) kısmını hesaplayan getter
     */
    public LocalDate getPredictionDate() {
        if (predictionDate == null && predictionDateMillisYmd != null) {
            predictionDate = Instant
                    .ofEpochMilli(predictionDateMillisYmd)
                    .atZone(AppTimeZoneProvider.getZone())
                    .toLocalDate();
        }
        return predictionDate;
    }

    // 13 haneli epoch time (sadece tarih, saat bilgileri 0)
    @Column(name = "last_past_entry_date_millis_ymd")
    private Long lastPastEntryDateMillisYmd;

    /** Veritabanında kaydı yok, sadece hesaplamak için */
    @Transient
    private LocalDate lastPastEntryDate;

    public void setLastPastEntryDateMillisYmd(Long lastPastEntryDateMillisYmd) {
        this.lastPastEntryDateMillisYmd = lastPastEntryDateMillisYmd;
        // isteğe bağlı: burada da güncelleyebilirsiniz
        this.lastPastEntryDate = null;
    }

    /**
     * ZoneId’yi AppTimeZoneProvider’dan alıp
     * sadece tarih (LocalDate) kısmını hesaplayan getter
     */
    public LocalDate getLastPastEntryDate() {
        if (lastPastEntryDate == null && lastPastEntryDateMillisYmd != null) {
            lastPastEntryDate = Instant
                    .ofEpochMilli(lastPastEntryDateMillisYmd)
                    .atZone(AppTimeZoneProvider.getZone())
                    .toLocalDate();
        }
        return lastPastEntryDate;
    }


    /**
     * Eğer JPA load sonrası otomatik set etmek isterseniz:
     */
    @PostLoad
    private void onPostLoad() {
        getPredictionDate();
        getLastPastEntryDate();
    }

    public Topic() {
    }

}
