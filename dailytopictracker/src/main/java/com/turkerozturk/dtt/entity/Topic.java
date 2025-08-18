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


import com.turkerozturk.dtt.helper.OccurrenceParser;
import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import com.turkerozturk.dtt.component.AppTimeZoneProvider;

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

    @Column(name = "data_class_name")
    private String dataClassName;

    @Column(name = "image_file_name")
    private String imageFileName;

    @Column(name = "interval_rule")
    private String intervalRule;

    @Transient
    private String intervalRuleInfo;

    public String getIntervalRuleInfo() {
        OccurrenceParser occurrenceParser = new OccurrenceParser();
        occurrenceParser.parse(getIntervalRule());
        return occurrenceParser.toHTML();
    }

    // for markdown, related with MarkdownService.java component and flexmark library.
    @Transient
    private String descriptionAsHtml;

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

    // 13 haneli epoch time (sadece tarih, saat bilgileri 0)
    @Column(name = "first_warning_entry_date_millis_ymd")
    private Long firstWarningEntryDateMillisYmd;

    /** Veritabanında kaydı yok, sadece hesaplamak için */
    @Transient
    private LocalDate firstWarningEntryDate;

    public void setFirstWarningEntryDateMillisYmd(Long firstWarningEntryDateMillisYmd) {
        this.firstWarningEntryDateMillisYmd = firstWarningEntryDateMillisYmd;
        // isteğe bağlı: burada da güncelleyebilirsiniz
        this.firstWarningEntryDate = null;
    }

    /**
     * ZoneId’yi AppTimeZoneProvider’dan alıp
     * sadece tarih (LocalDate) kısmını hesaplayan getter
     */
    public LocalDate getFirstWarningEntryDate() {
        if (firstWarningEntryDate == null && firstWarningEntryDateMillisYmd != null) {
            firstWarningEntryDate = Instant
                    .ofEpochMilli(firstWarningEntryDateMillisYmd)
                    .atZone(AppTimeZoneProvider.getZone())
                    .toLocalDate();
        }
        return firstWarningEntryDate;
    }

    // 13 haneli epoch time (sadece tarih, saat bilgileri 0)
    @Column(name = "first_future_neutral_entry_date_millis_ymd")
    private Long firstFutureNeutralEntryDateMillisYmd;

    /** Veritabanında kaydı yok, sadece hesaplamak için */
    @Transient
    private LocalDate firstFutureNeutralEntryDate;

    public void setFirsFutureNeutralEntryDateMillisYmd(Long firstFutureNeutralEntryDateMillisYmd) {
        this.firstFutureNeutralEntryDateMillisYmd = firstFutureNeutralEntryDateMillisYmd;
        // isteğe bağlı: burada da güncelleyebilirsiniz
        this.firstFutureNeutralEntryDate = null;
    }

    /**
     * ZoneId’yi AppTimeZoneProvider’dan alıp
     * sadece tarih (LocalDate) kısmını hesaplayan getter
     */
    public LocalDate getFirstFutureNeutralEntryDate() {
        if (firstFutureNeutralEntryDate == null && firstFutureNeutralEntryDateMillisYmd != null) {
            firstFutureNeutralEntryDate = Instant
                    .ofEpochMilli(firstFutureNeutralEntryDateMillisYmd)
                    .atZone(AppTimeZoneProvider.getZone())
                    .toLocalDate();
        }
        return firstFutureNeutralEntryDate;
    }

    // start base_date_millis_ymd
    // 13 haneli epoch time (sadece tarih, saat bilgileri 0)
    @Getter
    @Column(name = "base_date_millis_ymd")
    private Long baseDateMillisYmd;

    /** Veritabanında kaydı yok, sadece hesaplamak için */
    @Transient
    private LocalDate baseDate;

    public void setBaseDateMillisYmd(Long baseDateMillisYmd) {
        this.baseDateMillisYmd = baseDateMillisYmd;
        // isteğe bağlı: burada da güncelleyebilirsiniz
        this.baseDate = null;
    }

    /**
     * ZoneId’yi AppTimeZoneProvider’dan alıp
     * sadece tarih (LocalDate) kısmını hesaplayan getter
     */
    public LocalDate getBaseDate() {
        if (baseDate == null && baseDateMillisYmd != null) {
            baseDate = Instant
                    .ofEpochMilli(baseDateMillisYmd)
                    .atZone(AppTimeZoneProvider.getZone())
                    .toLocalDate();
        }
        return baseDate;
    }

    // end base_date_millis_ymd


    // start end_date_millis_ymd
    // 13 haneli epoch time (sadece tarih, saat bilgileri 0)
    @Getter
    @Column(name = "end_date_millis_ymd")
    private Long endDateMillisYmd;

    /** Veritabanında kaydı yok, sadece hesaplamak için */
    @Transient
    private LocalDate endDate;

    public void setEndDateMillisYmd(Long endDateMillisYmd) {
        this.endDateMillisYmd = endDateMillisYmd;
        // isteğe bağlı: burada da güncelleyebilirsiniz
        this.endDate = null;
    }

    /**
     * ZoneId’yi AppTimeZoneProvider’dan alıp
     * sadece tarih (LocalDate) kısmını hesaplayan getter
     */
    public LocalDate getEndDate() {
        if (endDate == null && endDateMillisYmd != null) {
            endDate = Instant
                    .ofEpochMilli(endDateMillisYmd)
                    .atZone(AppTimeZoneProvider.getZone())
                    .toLocalDate();
        }
        return endDate;
    }
    // end end_date_millis_ymd




    /**
     *  indicates the importance of a topic. user can use it manually for severity or something else.
    **/
    private int weight;

    /**
     * Eğer JPA load sonrası otomatik set etmek isterseniz:
     */
    @PostLoad
    private void onPostLoad() {
        getPredictionDate();
        getLastPastEntryDate();
        getFirstWarningEntryDate();
        getFirstFutureNeutralEntryDate();
    }

    public Topic() {
    }

}
