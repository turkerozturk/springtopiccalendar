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
package com.turkerozturk.dtt.controller.web;


import com.turkerozturk.dtt.component.MarkdownService;
import com.turkerozturk.dtt.dto.notefieldstructures.NoteFieldStructure;
import com.turkerozturk.dtt.dto.notefieldstructures.ParserFactory;
import com.turkerozturk.dtt.dto.statistics.OverallStatisticsDTO;
import com.turkerozturk.dtt.dto.statistics.StreaksDTO;
import com.turkerozturk.dtt.dto.statistics.SuccessStatisticsDTO;
import com.turkerozturk.dtt.dto.statistics.WeeklyViewDTO;
import com.turkerozturk.dtt.helper.OccurrenceParser;
import com.turkerozturk.dtt.helper.SuccessAnalyzer;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.turkerozturk.dtt.component.AppTimeZoneProvider;
import com.turkerozturk.dtt.dto.Streak;
import com.turkerozturk.dtt.dto.TopicDto;
import com.turkerozturk.dtt.dto.TopicEntrySummaryDTO;
import com.turkerozturk.dtt.entity.Category;
import com.turkerozturk.dtt.entity.Entry;
import com.turkerozturk.dtt.entity.Note;
import com.turkerozturk.dtt.entity.Topic;
import com.turkerozturk.dtt.helper.DateUtils;
import com.turkerozturk.dtt.repository.CategoryRepository;
import com.turkerozturk.dtt.repository.EntryRepository;
import com.turkerozturk.dtt.repository.TopicRepository;
import com.turkerozturk.dtt.service.EntryService;
import com.turkerozturk.dtt.service.FilterService;
import com.turkerozturk.dtt.service.TopicService;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


@Controller
@RequestMapping("/entries")
public class EntryController {


    private final AppTimeZoneProvider timeZoneProvider;
    private final EntryRepository entryRepository;

    private final FilterService filterService;

    private final EntryService entryService;
    private final TopicRepository topicRepository;

    private final TopicService topicService;

    private final CategoryRepository categoryRepository;

    private final MarkdownService markdownService;

    @Value("${week.start.day:MONDAY}")
    private String startDayOfWeek;


    public EntryController(AppTimeZoneProvider timeZoneProvider, EntryRepository entryRepository, FilterService entryService, EntryService entryService1, TopicRepository topicRepository, TopicService topicService, CategoryRepository categoryRepository, MarkdownService markdownService) {
        this.timeZoneProvider = timeZoneProvider;
        this.entryRepository = entryRepository;
        this.filterService = entryService;
        this.entryService = entryService1;
        this.topicRepository = topicRepository;
        this.topicService = topicService;
        this.categoryRepository = categoryRepository;
        this.markdownService = markdownService;
    }

    @GetMapping
    public String listEntries(
            @RequestParam(name = "topicId", required = false) Long topicId,
            @RequestParam(name = "page", required = false) Integer page,  // Artık default yok
            @RequestParam(name = "size", defaultValue = "100") int size,
            Model model) {

        ZoneId zoneId = timeZoneProvider.getZoneId();

        DateUtils dateUtils = new DateUtils();
        dateUtils.setZoneId(zoneId);
        long todayMillisYmd = dateUtils.getEpochMillisToday();

        // Sayfa parametresi yoksa, bugüne en yakın entry'nin sırasına göre sayfa numarasını hesapla
        if (page == null) {
            int indexOfClosestEntry;

            if (topicId != null) {
                indexOfClosestEntry = entryRepository.countEntriesWithDateGreaterThanEqualAndTopicId(todayMillisYmd, topicId);
            } else {
                indexOfClosestEntry = entryRepository.countEntriesWithDateGreaterThanEqual(todayMillisYmd);
            }

            page = indexOfClosestEntry / size;
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "dateMillisYmd"));
        Page<Entry> entriesPage;

        if (topicId != null) {
            entriesPage = entryRepository.findByTopicIdOrderByDateMillisYmdDescIdDesc(topicId, pageable);
            Topic topic = topicRepository.findById(topicId).orElseThrow();
            model.addAttribute("topic", topic);
            String topicDescriptionAsHtml = convertUrlsToLinksSafe(topic.getDescription());
            model.addAttribute("topicDescriptionAsHtml", topicDescriptionAsHtml);

        } else {
            entriesPage = entryRepository.findAllByOrderByDateMillisYmdDescIdDesc(pageable);
        }

        model.addAttribute("entriesPage", entriesPage);
        model.addAttribute("topicId", topicId);

        final int pagingDistance = 4;

        int current = entriesPage.getNumber();
        int total = entriesPage.getTotalPages();
        int start = Math.max(0, current - pagingDistance);
        int end = Math.min(total - 1, current + pagingDistance);

        model.addAttribute("startPage", start);
        model.addAttribute("endPage", end);
        model.addAttribute("zoneId", zoneId);

        return "entries/entry-list";
    }



    public LocalDate getEndOfWeek(LocalDate date, DayOfWeek startDayOfWeek) {
        int startOrdinal = startDayOfWeek.getValue();
        int endOrdinal = (startOrdinal + 6) % 7;
        DayOfWeek endDayOfWeek = DayOfWeek.of(endOrdinal == 0 ? 7 : endOrdinal);
        return date.with(TemporalAdjusters.nextOrSame(endDayOfWeek));
    }

    @GetMapping("weekly-calendar")
    public String listEntriesWeekView(
            HttpServletRequest request,
            @RequestParam(name = "topicId", required = false) Long topicId,
            @RequestParam(required = false) String startDateString,
            @RequestParam(required = false) String endDateString,
            @RequestParam(required = false, defaultValue = "false") boolean fragment, // aymi html dosyasini hem htmx ile hem de dogrudan cagirdigimizda ayirdedici olmasi icin.
            Model model) {

        int totalDays = 364;
        model.addAttribute("totalDays", totalDays);

        // bunu chartlar icin yaptim. Ileride sablonlari mobil ve masaustu icin duzenleyince gerek kalmayabilir.
        String userAgent = request.getHeader("User-Agent");
        boolean isMobile = userAgent != null && userAgent.toLowerCase().matches(".*(mobile|android|iphone|ipad).*");
        model.addAttribute("isMobile", isMobile);


        ZoneId zoneId = timeZoneProvider.getZoneId();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        LocalDate today = LocalDate.now(zoneId);
        model.addAttribute("today", today);


        LocalDate endDate = (endDateString != null && !endDateString.isBlank())
                ? LocalDate.parse(endDateString, formatter)
                : LocalDate.now(zoneId); // Since no end date is specified, it is determined as today's date.
        model.addAttribute("finishDate", endDate);


        LocalDate startDate = (startDateString != null && !startDateString.isBlank())
                ? LocalDate.parse(startDateString, formatter)
                : endDate.minusDays(totalDays); // Since the start date is not specified, it is determined by
                                                // subtracting a fixed number of days from the end date.
        model.addAttribute("beginDate", startDate);


        long startDateMillis = startDate.atStartOfDay(zoneId).toInstant().toEpochMilli();
        long endDateMillis = endDate.atStartOfDay(zoneId).toInstant().toEpochMilli();


        // Repository'den verileri al
        List<Entry> entries = entryService.findByTopicIdAndDateInterval(topicId, startDateMillis, endDateMillis);
        if (entries == null || entries.size() == 0) {
            return "entries/entry-list-weekly-calendar-empty";
        }
        //System.out.println(startDateMillis + " " + endDateMillis + " " + entries.size());
        for(Entry ent : entries) {
            if(ent.getStatus() == 1) {
                model.addAttribute("oldestDoneDate", Instant.ofEpochMilli(ent.getDateMillisYmd()).atZone(zoneId).toLocalDate());
                break;
            }
        }


        DayOfWeek startDay = DayOfWeek.valueOf(startDayOfWeek.toUpperCase());
        List<String> dayNames = new ArrayList<>();
        for (int j = 0; j < 7; j++) {
            dayNames.add(startDay.plus(j).getDisplayName(TextStyle.FULL, Locale.ENGLISH));
        }
        model.addAttribute("dayNames", dayNames);
        String startDateDayName = startDate.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        model.addAttribute("startDateDayName", startDateDayName);
        String todayDateDayName = today.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        //System.out.println("BUGUNN: " + todayDateDayName);
        model.addAttribute("todayDateDayName", todayDateDayName);
        String endDateDayName = endDate.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        //model.addAttribute("endDateDayName", endDateDayName);

        LocalDate startDateAlignedToWeek = filterService.getStartOfWeek(startDate, startDay);
        LocalDate endDateAlignedToWeek = getEndOfWeek(endDate, startDay);
        List<LocalDate> dateRange = filterService.buildDateRangeList(startDateAlignedToWeek, endDateAlignedToWeek);





        // Entry'leri tarihiyle eşle
        Map<LocalDate, Entry> entryMap = entries.stream()
                .collect(Collectors.toMap(
                        e -> Instant.ofEpochMilli(e.getDateMillisYmd()).atZone(zoneId).toLocalDate(),
                        Function.identity()
                ));

        WeeklyViewDTO weeklyViewDTO = calculateCalendarRows(entryMap, dateRange, startDate, endDate, zoneId);

        // basla counter
        int countt = weeklyViewDTO.getTop1ColumnDates().size();
        List<Integer> weekNumbers = new ArrayList<>();
        for(int i = 0; i < countt; i++) {
            weekNumbers.add(countt-i);
        }
        model.addAttribute("weekNumbers", weekNumbers);
        // bitti counter

        model.addAttribute("top1ColumnDates", weeklyViewDTO.getTop1ColumnDates());
        model.addAttribute("top2ColumnDates", weeklyViewDTO.getTop2ColumnDates());
        model.addAttribute("top3ColumnDates", weeklyViewDTO.getTop3ColumnDates());
        model.addAttribute("bottom1ColumnDates", weeklyViewDTO.getBottom1ColumnDates());
        model.addAttribute("bottom2ColumnDates", weeklyViewDTO.getBottom2ColumnDates());
        model.addAttribute("bottom3ColumnDates", weeklyViewDTO.getBottom3ColumnDates());

        List<Map<LocalDate, Entry>> weeklyMaps = weeklyViewDTO.getWeeklyMaps();

        // frontenddeki takvimde hesaplama disinda kalan takvim gunlerini farkli renkte gostermek icin yaptigimiz islem:
        // BASLA calendar olusturan weeklyMaps'in 7 elemanina, tarih araliginin dikkate alinmayan bastaki ve en sondaki gunleri icin fake entry ekler
        long npcStartDayCount = ChronoUnit.DAYS.between(startDateAlignedToWeek, startDate);
        long npcEndDayCount = ChronoUnit.DAYS.between(endDate, endDateAlignedToWeek);
        //System.out.println(npcStartDayCount + " " + npcEndDayCount); TODO
        //List<Map<LocalDate, Entry>> weeklyMapsWithFakeEntries = addNpcFakeEntriesToWeeklyMaps(weeklyMaps, npcStartDayCount, npcEndDayCount, zoneId);
        // BITTI calendar olusturan weeklyMaps'in 7 elemanina, tarih araliginin dikkate alinmayan bastaki ve en sondaki gunleri icin fake entry ekler

        model.addAttribute("weeklyMaps", weeklyMaps);


        Topic topic = topicRepository.findById(topicId).get();
        String descriptionHtml = markdownService.render(topic.getDescription());
        topic.setDescriptionAsHtml(descriptionHtml);
        model.addAttribute("topic", topic);

        Long topicBaseDateMillis = null;
        LocalDate topicBaseDate = null;
        //LocalDate topicEndDateAlignedToWeek = null;
        //LocalDate topicBaseDateAlignedToWeek = null;
        if (topic.getBaseDateMillisYmd() != null) { // TODO ve basedate arrayde yoksa, cok eskiyse veya todayden veya enddatedan yeniyse olmayacak sekilde kural koy.
            topicBaseDateMillis = topic.getBaseDateMillisYmd();
            topicBaseDate = topic.getBaseDate();
            //topicBaseDateAlignedToWeek = filterService.getStartOfWeek(topic.getBaseDate(), startDay);
        } else {
            topicBaseDateMillis = startDateMillis;
            topicBaseDate = startDate;
            //topicBaseDateAlignedToWeek = startDateAlignedToWeek;
        }

        Long topicEndDateMillis = null;
        LocalDate topicEndDate = null;
        if (topic.getEndDateMillisYmd() != null) { // TODO ve enddate arrayde yoksa, cok eskiyse veya basedateten eskiyse veya bugunden yeniyse olmayacak sekilde kural koy.
            topicEndDateMillis = topic.getEndDateMillisYmd();
            topicEndDate = topic.getEndDate();
            //topicEndDateAlignedToWeek = getEndOfWeek(topic.getEndDate(), startDay);
        } else {
            topicEndDateMillis = endDateMillis;
            topicEndDate = endDate;
            //topicEndDateAlignedToWeek = endDateAlignedToWeek;
        }

        // ONEMLI: varsa topicdeki base ve enddate tarih araligina gore daha kisitli veri getirir.
        // Yani 365 gunluk entries in alt kumesi veya aynisi olabilir.
        List<Entry> manualEntries = entryService.findByTopicIdAndDateInterval(topicId, topicBaseDateMillis, topicEndDateMillis);

        // Kisitli tarih araligindaki Entry'leri tarihiyle eşle
        Map<LocalDate, Entry> manualEntryMap = manualEntries.stream()
                .collect(Collectors.toMap(
                        e -> Instant.ofEpochMilli(e.getDateMillisYmd()).atZone(zoneId).toLocalDate(),
                        Function.identity()
                ));
        List<LocalDate> manualDateRange = filterService.buildDateRangeList(topicBaseDate, topicEndDate);
        WeeklyViewDTO manualWeeklyViewDTO = calculateCalendarRows(manualEntryMap, manualDateRange, topicBaseDate, topicEndDate, zoneId);
        List<Map<LocalDate, Entry>> manualWeeklyMaps = manualWeeklyViewDTO.getWeeklyMaps();

        // basla bu kisim entry.note lerin topic.dataClassName'da yazan sinif adina gore parse edilmesi

        // The critical difference is that isBlank() returns true for whitespace characters, like some escape sequences.
        // On the other hand, isEmpty() only returns true when the String doesn’t contain any character.
        if(topic.getDataClassName() != null && !topic.getDataClassName().isBlank()) {
            NoteFieldStructure customParserClass = ParserFactory.create(topic.getDataClassName());


            customParserClass.parseRawData(manualEntries);

            model.addAttribute("parserReport", customParserClass.getReport());
            model.addAttribute("parsedDataAsJSON", customParserClass.getParsedDataAsJSON());

            //System.out.println(parser.getParsedDataAsJSON());
        }

        // bitti bu kisim entry.note lerin topic.dataClassName'da yazan sinif adina gore parse edilmesi


        // basla bu kisim patternSuccessRate ile ilgili

        OccurrenceParser occurrenceParser = new OccurrenceParser();

        occurrenceParser.parse(topic.getIntervalRule());
        //System.out.println(topic.getIntervalRule());

        //successStatisticsDTO.setIntervalLength(occurrenceParser.getPartitionLength());

        SuccessStatisticsDTO successStatisticsDTO = calculateSuccessStatistics( topic,
                entryMap,
                entries,
                endDate,
                startDateAlignedToWeek,
                occurrenceParser);
        model.addAttribute("patternSuccessRate", successStatisticsDTO.getPatternSuccessRate());
        model.addAttribute("patternSuccessText", successStatisticsDTO.getPatternSuccessText());
        model.addAttribute("patternFillText", successStatisticsDTO.getPatternFillText());
        model.addAttribute("patternDebugResult", successStatisticsDTO.getDebugResult());



        // basla istatistik

        List<Integer> status0 = new ArrayList<>();
        List<Integer> status1 = new ArrayList<>();
        List<Integer> status2 = new ArrayList<>();
        int totalStatus0 = 0, totalStatus1 = 0, totalStatus2 = 0;

        for (Map<LocalDate, Entry> map : manualWeeklyMaps) {
            int count0 = 0;
            int count1 = 0;
            int count2 = 0;

            for (Entry entry : map.values()) {
                if (entry != null) {
                    switch (entry.getStatus()) {
                        case 0:
                            count0++;
                            totalStatus0++;
                            break;
                        case 1:
                            count1++;
                            totalStatus1++;
                            break;
                        case 2:
                            count2++;
                            totalStatus2++;
                            break;
                    }
                }
            }

            status0.add(count0);
            status1.add(count1);
            status2.add(count2);
        }

        List<DayOfWeek> dayOfWeeks = manualWeeklyViewDTO.getDayOfWeeks();
        List<String> dayNamesShort = new ArrayList<>();
        List<String> dayNamesLong = new ArrayList<>();

        for (int z = 0; z < status1.size(); z++) {
            dayNamesShort.add(dayOfWeeks.get(z).getDisplayName(TextStyle.SHORT, Locale.ENGLISH));
            dayNamesLong.add(dayOfWeeks.get(z).getDisplayName(TextStyle.FULL, Locale.ENGLISH));

        }
        model.addAttribute("dayNamesShort", dayNamesShort);
        model.addAttribute("dayNamesLong", dayNamesLong);

        /*
        for (int z = 0; z < status1.size(); z++) {
            System.out.println(status1.get(z) + " " + dayNamesShort.get(z));
        }
        */

        model.addAttribute("status0", status0);
        model.addAttribute("status1", status1);
        model.addAttribute("status2", status2);
        model.addAttribute("totalStatus0", totalStatus0);
        model.addAttribute("totalStatus1", totalStatus1);
        model.addAttribute("totalStatus2", totalStatus2);


        // bitti istatistik


        // BASLA - istatistik - streak



        List<Integer> matchResults = successStatisticsDTO.getMatchResult();
        int intervalLength = successStatisticsDTO.getIntervalLength();

        // TODO dateRange ve totalDays artık manualEntries ve manual EntryMap dikkate alınarak hesaplamali asagida:
        Long firstDoneEntryDateMillisYmd1 = entries.stream()
                .filter(e -> e.getStatus() == 1)
                .map(Entry::getDateMillisYmd)
                .min(Long::compareTo)
                .orElse(null);
        // yukarida offset null degilse dedigimiz icin bu da null olmaz, o yuzden kontrol eklemedik.
        LocalDate firstDoneEntryDate1 = Instant
                .ofEpochMilli(firstDoneEntryDateMillisYmd1)
                .atZone(AppTimeZoneProvider.getZone())
                .toLocalDate();

        if (topic.getBaseDateMillisYmd() == null) {
            topicBaseDate = firstDoneEntryDate1;
        }

        StreaksDTO streaksDTO = calculateStreaks(topicBaseDate, matchResults,intervalLength,zoneId);
        //StreaksDTO streaksDTO = calculateStreaks ( manualEntryMap,  manualDateRange, manualEntryMap.size());
        model.addAttribute("newestStreak", streaksDTO.getNewestStreak());
        model.addAttribute("uniqueTopStreaks", streaksDTO.getUniqueTopStreaks());
        model.addAttribute("streakCounters", streaksDTO.getStreakCounters());
        model.addAttribute("streakTotalDays", manualDateRange.size());
        model.addAttribute("allStreaks", streaksDTO.getAllStreaks());


        // BITTI - istatistik - streak


        // BASLA simdi de baska istatistikler olusturmaya çalışalım.

        OverallStatisticsDTO overallStatisticsDTO = calculateOverallStatistics(entries, topic, zoneId);

        model.addAttribute("daysSinceFirstDoneEntry", overallStatisticsDTO.getDaysSinceFirstDoneEntry());
        model.addAttribute("daysSinceFirstDoneEntryToPrediction", overallStatisticsDTO.getDaysSinceFirstDoneEntryToPrediction());
        model.addAttribute("numberOfDaysToBeConsidered", overallStatisticsDTO.getNumberOfDaysToBeConsidered());
        model.addAttribute("doneDayCount", overallStatisticsDTO.getDoneDayCount());
        model.addAttribute("emptyDayCount", overallStatisticsDTO.getEmptyDayCount());
        model.addAttribute("averageDoneInterval", overallStatisticsDTO.getAverageDoneInterval());
        model.addAttribute("successRate", overallStatisticsDTO.getSuccessRate());
        model.addAttribute("realAverage", overallStatisticsDTO.getRealAverage());
        model.addAttribute("realSuccessRate", overallStatisticsDTO.getRealSuccessRate());


        model.addAttribute("intervals", overallStatisticsDTO.getIntervals());
        model.addAttribute("mean", overallStatisticsDTO.getMean());
        model.addAttribute("std", overallStatisticsDTO.getStd());
        model.addAttribute("successRate2", overallStatisticsDTO.getSuccessRate2());
        model.addAttribute("trendSlope", overallStatisticsDTO.getTrendSlope());
        model.addAttribute("trendStatus", overallStatisticsDTO.getTrendStatus());

        // BITTI simdi de baska istatistikler olusturmaya çalışalım.


        model.addAttribute("zoneId", zoneId);

        return fragment ? "entries/entry-list-weekly-calendar :: content"
                        : "entries/entry-list-weekly-calendar";

    }




    private OverallStatisticsDTO calculateOverallStatistics(List<Entry> entries, Topic topic, ZoneId zoneId) {

        OverallStatisticsDTO overallStatisticsDTO = new OverallStatisticsDTO();

        Long firstDoneEntryDateMillisYmd = entries.stream()
                .filter(e -> e.getStatus() == 1)
                .map(Entry::getDateMillisYmd)
                .min(Long::compareTo)
                .orElse(null);

        if(firstDoneEntryDateMillisYmd != null) {
            LocalDate firstDoneEntryDate = Instant
                    .ofEpochMilli(firstDoneEntryDateMillisYmd)
                    .atZone(AppTimeZoneProvider.getZone())
                    .toLocalDate();

            long daysSinceFirstDoneEntry = ChronoUnit.DAYS.between(firstDoneEntryDate, LocalDate.now());
            //model.addAttribute("daysSinceFirstDoneEntry", daysSinceFirstDoneEntry);
            overallStatisticsDTO.setDaysSinceFirstDoneEntry(daysSinceFirstDoneEntry);

            long numberOfDaysToBeConsidered = daysSinceFirstDoneEntry; // bizim icin bu sayac onemli, tahmin hesaplamalarinda.
            if(topic.getPredictionDate() != null) {
                long daysSinceFirstDoneEntryToPrediction = ChronoUnit.DAYS.between(firstDoneEntryDate, topic.getPredictionDate());
                //model.addAttribute("daysSinceFirstDoneEntryToPrediction", daysSinceFirstDoneEntryToPrediction);
                overallStatisticsDTO.setDaysSinceFirstDoneEntryToPrediction(daysSinceFirstDoneEntryToPrediction);
                // ust limit olarak bugunun veya gelecekteki predictionun tarihini aliyoruz, gecmiz predictionu almiyoruz cunku artik bugun gelmis.
                numberOfDaysToBeConsidered = Math.max(daysSinceFirstDoneEntry, daysSinceFirstDoneEntryToPrediction);

            }
            //model.addAttribute("numberOfDaysToBeConsidered", numberOfDaysToBeConsidered);
            overallStatisticsDTO.setNumberOfDaysToBeConsidered(numberOfDaysToBeConsidered);

            Set<Long> filledDates = entries.stream()
                    .filter(e -> e.getStatus() == 1)
                    .map(Entry::getDateMillisYmd)
                    .collect(Collectors.toSet());

            // long totalDays = ChronoUnit.DAYS.between(firstEntryDate, LocalDate.now()) + 1;
            long doneDayCount = filledDates.size();
            long emptyDayCount = daysSinceFirstDoneEntry - doneDayCount;
            //model.addAttribute("doneDayCount", doneDayCount);
            overallStatisticsDTO.setDoneDayCount(doneDayCount);
            //model.addAttribute("emptyDayCount", emptyDayCount);
            overallStatisticsDTO.setEmptyDayCount(emptyDayCount);

            double averageDoneInterval = (double) numberOfDaysToBeConsidered / doneDayCount;
            //model.addAttribute("averageDoneInterval", averageDoneInterval);
            overallStatisticsDTO.setAverageDoneInterval(averageDoneInterval);



            if(topic.getSomeTimeLater() != null && topic.getSomeTimeLater() > 0) {
                double successRate = (topic.getSomeTimeLater() / averageDoneInterval) * 100;
                //model.addAttribute("successRate", successRate);
                overallStatisticsDTO.setSuccessRate(successRate);
            }


            //  DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");


            List<LocalDate> filledDatesSorted = entries.stream()
                    .filter(e -> e.getStatus() == 1)
                    .map(e -> Instant.ofEpochMilli(e.getDateMillisYmd())
                            .atZone(zoneId)
                            .toLocalDate())
                    .sorted()
                    .collect(Collectors.toList());

            List<Long> intervals = new ArrayList<>();
            for (int k = 1; k < filledDatesSorted.size(); k++) {
                long diff = ChronoUnit.DAYS.between(filledDatesSorted.get(k - 1), filledDatesSorted.get(k));
                intervals.add(diff);
            }

            StringBuilder sb = new StringBuilder();
            for (Long l : intervals) {
                sb.append(l + ", ");
            }
            //System.out.println(sb.toString() + "\n\n");

            double realAverage = intervals.stream()
                    .mapToLong(Long::longValue)
                    .average()
                    .orElse(0);


            //model.addAttribute("realAverage", realAverage);
            overallStatisticsDTO.setRealAverage(realAverage);

            if(topic.getSomeTimeLater() != null && topic.getSomeTimeLater() > 0) {
                double realSuccessRate = (topic.getSomeTimeLater() / realAverage) * 100;
                //model.addAttribute("realSuccessRate", realSuccessRate);
                overallStatisticsDTO.setRealSuccessRate(realSuccessRate);
            }



            // basla -------------------------------------

            //     double target = topic.getSomeTimeLater();

            // Ortalama
            double mean = intervals.stream()
                    .mapToDouble(Long::doubleValue)
                    .average()
                    .orElse(0.0);

            // Standart sapma
            double std = Math.sqrt(intervals.stream()
                    .mapToDouble(iiii -> Math.pow(iiii - mean, 2))
                    .average()
                    .orElse(0.0));

            // Başarı oranı: 4 <= i <= 6
            long successCount = intervals.stream()
                    .filter(jjjj -> jjjj >= 4 && jjjj <= 6)
                    .count();
            double successRate2 = (double) successCount / intervals.size() * 100;

            // Trend eğimi (basit doğrusal regresyon)
            int n = intervals.size();
            double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;

            for (int inte = 0; inte < n; inte++) {
                double x = inte;
                double y = intervals.get(inte);
                sumX += x;
                sumY += y;
                sumXY += x * y;
                sumX2 += x * x;
            }

            double trendSlope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
            String trendStatus = (trendSlope < -0.1) ? "OLUMLU"
                    : (trendSlope > 0.1) ? "OLUMSUZ"
                    : "DURAĞAN";

            // Thymeleaf'e aktar
            //model.addAttribute("intervals", intervals);
            //model.addAttribute("mean", mean);
            //model.addAttribute("std", std);
            //model.addAttribute("successRate2", successRate2);
            //model.addAttribute("trendSlope", trendSlope);
            //model.addAttribute("trendStatus", trendStatus);
            overallStatisticsDTO.setIntervals(intervals);
            overallStatisticsDTO.setMean(mean);
            overallStatisticsDTO.setStd(std);
            overallStatisticsDTO.setSuccessRate2(successRate2);
            overallStatisticsDTO.setTrendSlope(trendSlope);
            overallStatisticsDTO.setTrendStatus(trendStatus);
            // bitti --------------------------------------------






        }

        return overallStatisticsDTO;

    }

    //        int totalDays = (int) ChronoUnit.DAYS.between(baseDate, endDate) + 1;
//        System.out.println("intervalStarts.size(): " + intervalStarts.size());

    // intervallere gore hesap yapabilen yeni streak metodu
    private StreaksDTO calculateStreaks(LocalDate baseDate, List<Integer> intervalResults, int intervalLength, ZoneId zoneId) {
        StreaksDTO streaksDTO = new StreaksDTO();
        List<Streak> streaks = new ArrayList<>();

        // 1️⃣ Interval tarihlerini üret
        List<LocalDate> intervalStarts = new ArrayList<>();
        LocalDate current = baseDate;
        for (int i = 0; i < intervalResults.size(); i++) {
            intervalStarts.add(current);
            //System.out.println(i + ": " + current);

            current = current.plusDays(intervalLength);
        }

        int debugCounter = 1;
        int streakLength;
        List<String> debugList = new ArrayList<>();
        // 2️⃣ Ardışık 1'lerden streak listesi oluştur
        int streakStartIdx = -1;
        for (int i = 0; i < intervalResults.size(); i++) {
            if (intervalResults.get(i) == 1) {
                if (streakStartIdx == -1) {
                    streakStartIdx = i; // streak başlangıcı
                }
            } else {
                if (streakStartIdx != -1) {
                    // streak bitti
                    //basla BLOK
                    LocalDate streakStartDate = intervalStarts.get(streakStartIdx);
                    LocalDate streakEndDate = intervalStarts.get(i - 1).plusDays(intervalLength - 1);
                    streakLength = (int) ( ChronoUnit.DAYS.between(streakStartDate, streakEndDate) + 1 ) / intervalLength;

                    Streak streak = new Streak(streakStartDate,
                            streakEndDate,
                            intervalLength);
                    streaks.add(streak);
                    debugList.add(debugCounter + ". " + streakStartDate + " streakLength: " + streakLength + " " + streakEndDate + " intervalLength: " + intervalLength);
                    debugCounter++;
                    // bitti BLOK
                    streakStartIdx = -1;
                }
            }
        }

        // Listenin sonunda streak bitmemişse (yani sonu 1 ile bitiyorsa) ekle
        if (streakStartIdx != -1) {
            LocalDate streakStartDate = intervalStarts.get(streakStartIdx);
            LocalDate streakEndDate = intervalStarts.get(intervalResults.size() - 1).plusDays(intervalLength - 1);
            streakLength = (int) (ChronoUnit.DAYS.between(streakStartDate, streakEndDate) + 1) / intervalLength;

            Streak streak = new Streak(streakStartDate,
                    streakEndDate,
                    intervalLength);
            streaks.add(streak);
            debugList.add(debugCounter + ". " + streakStartDate + " streakLength: " + streakLength + " " + streakEndDate + " intervalLength: " + intervalLength);
        }


        //System.out.println("streaks.size(): " + streaks.size());
        /*
        for(String s: debugList) {
            System.out.println(s);
        }
        */
       // System.out.println("streaks2.size(): " + streaks.size());

        if (streaks.isEmpty()) {
            streaksDTO.setNewestStreak(null);
            streaksDTO.setUniqueTopStreaks(Collections.emptyList());
            streaksDTO.setStreakCounters(Collections.emptyList());
            return streaksDTO;
        }

        // 3️⃣ newestStreak: bitiş tarihi en yeni olan
        Streak newestStreak = streaks.stream()
                .max(Comparator.comparing(Streak::getEndDate))
                .orElse(null);
       // System.out.println("\n\n -----------------------------");
       // System.out.println(newestStreak.getIntervalCount());
       // System.out.println(newestStreak.getStartDate());
       // System.out.println(newestStreak.getEndDate());
        streaksDTO.setNewestStreak(newestStreak);

        // 4️⃣ uniqueTopStreaks ve streakCounters
        // önce uzunluk -> tüm streakler map'i
        Map<Integer, List<Streak>> groupedByLength = streaks.stream()
                .collect(Collectors.groupingBy(Streak::getStreakLength));

        // her uzunluktan en yeni olanı seç
        List<Streak> uniqueTopStreaks = groupedByLength.values().stream()
                .map(list -> list.stream()
                        .max(Comparator.comparing(Streak::getEndDate))
                        .orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // sıralama: newest streak en başta, sonra tarihe göre
        uniqueTopStreaks.sort(Comparator.comparing(Streak::getEndDate).reversed());

        // counter listesi (paralel sıra)
        List<Integer> streakCounters = uniqueTopStreaks.stream()
                .map(s -> groupedByLength.get(s.getStreakLength()).size())
                .collect(Collectors.toList());

        streaksDTO.setUniqueTopStreaks(uniqueTopStreaks);
        streaksDTO.setStreakCounters(streakCounters);

        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < streakCounters.size(); i++) {
            sb.append(streakCounters.get(i));
            sb.append("\t");
            sb.append(uniqueTopStreaks.get(i).getStreakLength());
            sb.append("\n");
        }
       // System.out.println(sb);

        return streaksDTO;
    }





        // ardisik gunleri hesaplayan eski streak hesabi metodu.
    /*
        private StreaksDTO calculateStreaks (Map<LocalDate, Entry> entryMap, List<LocalDate> dateRange, int totalDays) {

        StreaksDTO streaksDTO = new StreaksDTO();

        List<Streak> streaks = new ArrayList<>();
        Streak currentStreak = null;
        for (LocalDate date : dateRange) {
            Entry entry = entryMap.get(date);

            if (entry != null && entry.getStatus() == 1) {
                // Geçerli ve status=1 entry varsa streak başlat ya da devam et
                if (currentStreak == null) {
                    currentStreak = new Streak(date, date, totalDays);
                } else {
                    currentStreak = new Streak(currentStreak.getStartDate(), date, totalDays);
                }
            } else {
                // Entry yok ya da status != 1 → streak biter
                if (currentStreak != null) {
                    streaks.add(currentStreak);
                    currentStreak = null;
                }
                // ❌ break yok, döngü devam eder
            }
        }
        // Loop sonunda aktif bir streak kaldıysa ekle
        if (currentStreak != null) {
            streaks.add(currentStreak);
        }

        // statistic: only last streak
        if(!streaks.isEmpty()) {
            Streak newestStreak = streaks.get(streaks.size()-1);
            streaksDTO.setNewestStreak(newestStreak);
        }

        // select top 10 streaks
        List<Streak> topStreaks = streaks.stream()
                .sorted(Comparator.comparingLong(Streak::getIntervalCount).reversed()
                )
                //.limit(10)
                .collect(Collectors.toList());

        Collections.reverse(topStreaks); // en büyük en üstte görünür
        topStreaks.sort(Comparator.comparing(Streak::getStartDate).reversed());

        Map<Integer, Streak> uniqueByDayCount = new LinkedHashMap<>();

        for (Streak streak : topStreaks) {
            if (!uniqueByDayCount.containsKey(streak.getIntervalCount())) {
                uniqueByDayCount.put(streak.getIntervalCount(), streak); // ilk karşılaşılan (yani en güncel) eklenecek
            }
        }

        List<Streak> uniqueTopStreaks = new ArrayList<>(uniqueByDayCount.values());


        // for(Streak streak : streaks) {
        //  System.out.println(streak.getDayCount() + ": " + streak.getStartDate() + " --- " + streak.getEndDate());
        // }

        streaksDTO.setUniqueTopStreaks(uniqueTopStreaks);


        return streaksDTO;
    }

*/

    private WeeklyViewDTO calculateCalendarRows(Map<LocalDate, Entry> entryMap, List<LocalDate> dateRange,
                                                LocalDate startDate, LocalDate endDate, ZoneId zoneId) {

        WeeklyViewDTO weeklyViewDTO = new WeeklyViewDTO();

        DateTimeFormatter formatter1 = DateTimeFormatter.ofPattern("yy");
        DateTimeFormatter formatter2 = DateTimeFormatter.ofPattern("MM");
        DateTimeFormatter formatter3 = DateTimeFormatter.ofPattern("dd");
        DateTimeFormatter formatter4 = DateTimeFormatter.ofPattern("dd");
        DateTimeFormatter formatter5 = DateTimeFormatter.ofPattern("MM");
        DateTimeFormatter formatter6 = DateTimeFormatter.ofPattern("yy");


        List<DayOfWeek> dayOfWeeks = new ArrayList<>();
        // 7 adet gün Map'i oluştur
        List<Map<LocalDate, Entry>> weeklyMaps = new ArrayList<>();
        DayOfWeek dow = startDate.getDayOfWeek();//DayOfWeek.valueOf(startDayOfWeek);
        int startOrdinal = dow.getValue();

        for (int i = 0; i < 7; i++) {
            weeklyMaps.add(new LinkedHashMap<>());


            int ordinal = (startOrdinal + i) % 7;
            DayOfWeek dowww = DayOfWeek.of(ordinal == 0 ? 7 : ordinal);
            dayOfWeeks.add(dowww);


        }

        List<String> top1ColumnDates = new LinkedList<>();
        List<String> top2ColumnDates = new LinkedList<>();
        List<String> top3ColumnDates = new LinkedList<>();
        List<String> bottom1ColumnDates = new LinkedList<>();
        List<String> bottom2ColumnDates = new LinkedList<>();
        List<String> bottom3ColumnDates = new LinkedList<>();


        // bitti bu kisim patternSuccessRate ile ilgili

        int i = 0;
        for(LocalDate d : dateRange) {

            if(i == 0) {
                top1ColumnDates.add(d.format(formatter1));
            }
            if(i == 0) {
                top2ColumnDates.add(d.format(formatter2));
            }
            if(i == 0) {
                top3ColumnDates.add(d.format(formatter3));
            }

            if(entryMap.containsKey(d)) {
                weeklyMaps.get(i).put(d, entryMap.get(d));
            } else {

                if (d.isBefore(startDate) || d.isAfter(endDate)) {
                    // d, startDate'ten küçük VEYA endDate'ten büyükse burası çalışır
                    //System.out.println("Dahil değil: " + d);

                    //  sahte Entry ekle
                    int npcEntryStatusCode = -1;
                    Entry fakeEntry = new Entry();
                    fakeEntry.setStatus(npcEntryStatusCode);
                    long epochMilli = d
                            .atStartOfDay(zoneId) // Günün başlangıç zamanı ile ZonedDateTime oluştur
                            .toInstant()
                            .toEpochMilli();
                    fakeEntry.setDateMillisYmd(epochMilli);
                    weeklyMaps.get(i).put(d, fakeEntry);

                } else {
                    weeklyMaps.get(i).put(d, null);
                }


            }

            if(i == 6) {
                bottom1ColumnDates.add(d.format(formatter4));
            }
            if(i == 6) {
                bottom2ColumnDates.add(d.format(formatter5));
            }
            if(i == 6) {
                bottom3ColumnDates.add(d.format(formatter6));
            }

            i = (i + 1) % 7;
        }

        weeklyViewDTO.setBottom1ColumnDates(bottom1ColumnDates);
        weeklyViewDTO.setBottom2ColumnDates(bottom2ColumnDates);
        weeklyViewDTO.setBottom3ColumnDates(bottom3ColumnDates);
        weeklyViewDTO.setTop1ColumnDates(top1ColumnDates);
        weeklyViewDTO.setTop2ColumnDates(top2ColumnDates);
        weeklyViewDTO.setTop3ColumnDates(top3ColumnDates);
        weeklyViewDTO.setWeeklyMaps(weeklyMaps);
        weeklyViewDTO.setDayOfWeeks(dayOfWeeks);

        return weeklyViewDTO;

    }




    private SuccessStatisticsDTO calculateSuccessStatistics(Topic topic,
                                                           Map<LocalDate, Entry> entryMap,
                                                           List<Entry> entries,
                                                           LocalDate today,
                                                           LocalDate startDateAlignedToWeek,
                                                            OccurrenceParser occurrenceParser) {

        SuccessStatisticsDTO successStatisticsDTO = new SuccessStatisticsDTO();

        // basla bu kisim patternSuccessRate ile ilgili


        List<LocalDate> dateRangeBetweenAlignedStartDateAndToday =
                filterService.buildDateRangeList(startDateAlignedToWeek, today);
        List<String> rawArray = new ArrayList<>(); // tarih araligi boyunca 1 ve 0 lardan olusan doluluk bosluk dizisi.

        for(LocalDate d : dateRangeBetweenAlignedStartDateAndToday) {
            if(entryMap.containsKey(d)) {
                rawArray.add(entryMap.get(d).getStatus() == 1 ? "1" : "0"); // entry.status 1 olanlar dolu yani 1, digerleri 0 yani bos.
            } else {
                rawArray.add("0"); // entry zaten yok ve bos yani 0.
            }
        }

        Integer offsetA = null; // bu offseti belirleyen iki kural var, ya baseDate'in offseti(hicbir entry olmasa da olur), veya ilk done entry nin offseti.
        if (topic.getBaseDateMillisYmd() != null) { // TODO ve basedate arrayde yoksa, cok eskiyse veya todayden veya enddatedan yeniyse olmayacak sekilde kural koy.
            if(dateRangeBetweenAlignedStartDateAndToday.contains(topic.getBaseDate())){
                offsetA = dateRangeBetweenAlignedStartDateAndToday.indexOf(topic.getBaseDate());
                //System.out.println("offsetA: " + offsetA);
            }
        } else {
            offsetA = SuccessAnalyzer.findFirstDoneOffsetOfArray(rawArray); // sifirlari gecerek dizinin ilk 1 olan elemaninin indeksini bulur.
        }

        Integer offsetB = null; // bu offseti belirleyen
        if (topic.getEndDateMillisYmd() != null) { // TODO ve enddate arrayde yoksa, cok eskiyse veya basedateten eskiyse veya bugunden yeniyse olmayacak sekilde kural koy.
            if(dateRangeBetweenAlignedStartDateAndToday.contains(topic.getEndDate())){
                offsetB = dateRangeBetweenAlignedStartDateAndToday.indexOf(topic.getEndDate());
                //System.out.println("offsetB: " + offsetB);
            }
        }

        //System.out.println("offset: " + offset);
        //int divider = topic.getSomeTimeLater() == null || topic.getSomeTimeLater() <= 0 ? 1 : topic.getSomeTimeLater().intValue(); // prediction degeri olmayan topiclerde gunde 1 olarak varsaymis olur.
        //System.out.println("divider: " + divider);



        //
        if(offsetA != null) {
            SuccessAnalyzer successAnalyzer = new SuccessAnalyzer();
            List<Integer> reducedArray = successAnalyzer.getSuccessArrayNew(
                    rawArray,
                    offsetA,
                    offsetB,
                    occurrenceParser);
            /*.getPartitionLength(),

                    occurrenceParser.getRandomOccuranceCount(),
                    occurrenceParser.getOccurancesListInOrder());
                    */
            //System.out.println("reduced size: " + reducedArray.size());
            successStatisticsDTO.setDebugResult(successAnalyzer.getDebugResult());

            //System.out.println("reduced: " + reducedArray);
            double patternSuccessRate = SuccessAnalyzer.getSuccessRate(reducedArray);
            successStatisticsDTO.setPatternSuccessRate(patternSuccessRate);


            Long firstDoneEntryDateMillisYmd1 = entries.stream()
                    .filter(e -> e.getStatus() == 1)
                    .map(Entry::getDateMillisYmd)
                    .min(Long::compareTo)
                    .orElse(null);
            // yukarida offset null degilse dedigimiz icin bu da null olmaz, o yuzden kontrol eklemedik.
            LocalDate firstDoneEntryDate1 = Instant
                    .ofEpochMilli(firstDoneEntryDateMillisYmd1)
                    .atZone(AppTimeZoneProvider.getZone())
                    .toLocalDate();

            long daysSinceFirstDoneEntryPlusToday = ChronoUnit.DAYS.between(firstDoneEntryDate1, today) + 1;



            long patternSuccessCount = SuccessAnalyzer.getSuccessCount(reducedArray);
            StringBuilder patternSuccessText = new StringBuilder();
            patternSuccessText.append("(" + patternSuccessCount + " / " + reducedArray.size() + " intervals)");
            /*
            patternSuccessText.append("<br/>");
            patternSuccessText.append("<br/>");
            patternSuccessText.append(firstDoneEntryDate1 + " to " + today);
            patternSuccessText.append("<br/>");
            patternSuccessText.append("within " + daysSinceFirstDoneEntryPlusToday + " days");
            patternSuccessText.append("<br/>");
            patternSuccessText.append("(from first 'done' day to today)");
            */
            successStatisticsDTO.setPatternSuccessText(patternSuccessText.toString());


            // bu iki degisken onemli cunku geri dondukten sonra baska hesaplamalarda da kullanilacak.
            successStatisticsDTO.setMatchResult(reducedArray);  // streak gibi hesaplar icin gerekli.
            successStatisticsDTO.setIntervalLength(occurrenceParser.getPartitionLength()); // streak gibi hesaplar icin gerekli.


            // bu kisim prediction yoksa yani divider 1 den kucukse yani topic.someTimeLater 0 veya null ise doluluk orani gostermek icin.
            // divideri 1 kabul ediyoruz o zaman doluluk orani veriyor. // TODO sonradan burasini kaldirip kafa karisikligi ve hsap hatasina mahal vermeyecek bicimde guncelle.
            /*
            List<Integer> notReducedArray = SuccessAnalyzer.getSuccessArray(rawArray, offsetA, offsetB,1, 1, null);

            long patternFillCount = SuccessAnalyzer.getSuccessCount(notReducedArray);
            StringBuilder patternFillText = new StringBuilder();
            patternFillText.append("(" + patternFillCount + " / " + notReducedArray.size() + " days are filled with 'done')");
            patternFillText.append("<br/>");
            patternFillText.append("<br/>");
            patternFillText.append(firstDoneEntryDate1 + " to " + today);
            patternFillText.append("<br/>");
            patternFillText.append("within " + daysSinceFirstDoneEntryPlusToday + " days");
            patternFillText.append("<br/>");
            patternFillText.append("(from first 'done' day to today)");
            successStatisticsDTO.setPatternFillText(patternFillText.toString());
            */

        }
        // bitti bu kisim patternSuccessRate ile ilgili


        return successStatisticsDTO;
    }

    @GetMapping("weekly-calendar-old")
    public String listEntriesWeekView(
            @RequestParam(name = "topicId", required = false) Long topicId,
            @RequestParam(name = "page",    defaultValue = "0")  int page,
            @RequestParam(name = "size",    defaultValue = "20") int size,
            Model model) {

        // sayfa isteğini oluşturuyoruz (tarih alanına göre azalan)
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "dateMillisYmd"));
        Page<Entry> entriesPage;

        if (topicId != null) {
            entriesPage = entryRepository.findByTopicId(topicId, pageable);
            model.addAttribute("topic", topicRepository.findById(topicId).orElseThrow());
        } else {
            entriesPage = entryRepository.findAll(pageable);
        }

        model.addAttribute("entriesPage", entriesPage);
        model.addAttribute("topicId",     topicId);

        int current = entriesPage.getNumber();
        int total   = entriesPage.getTotalPages();
        int start   = Math.max(0, current - 5);
        int end     = Math.min(total - 1, current + 5);

        model.addAttribute("startPage", start);
        model.addAttribute("endPage",   end);

        ZoneId zoneId = timeZoneProvider.getZoneId();  // Hazır metodunuz

        model.addAttribute("zoneId", zoneId);









        DayOfWeek startDay = DayOfWeek.valueOf(startDayOfWeek.toUpperCase());

        model.addAttribute("startDayOfWeek", startDay); // Enum, örn: MONDAY




        // start *****



// 1. Entry listesini tarihsel olarak sırala
        List<Entry> entries = new ArrayList<>(entriesPage.getContent());
        entries.sort(Comparator.comparingLong(Entry::getDateMillisYmd));

// 2. En erken ve en geç tarihleri al
        LocalDate firstEntryDate = Instant.ofEpochMilli(entries.get(0).getDateMillisYmd())
                .atZone(zoneId)
                .toLocalDate();
        LocalDate lastEntryDate = Instant.ofEpochMilli(entries.get(entries.size() - 1).getDateMillisYmd())
                .atZone(zoneId)
                .toLocalDate();

// 3. Başlangıç tarihini haftanın başlangıcına çek
        int diffToStartOfWeek = (7 + firstEntryDate.getDayOfWeek().getValue() - startDay.getValue()) % 7;
        LocalDate mapStartDate = firstEntryDate.minusDays(diffToStartOfWeek);

// 4. Bitiş tarihini haftanın sonuna çek
        int diffToEndOfWeek = (7 - ((lastEntryDate.getDayOfWeek().getValue() - startDay.getValue() + 7) % 7) - 1);
        LocalDate mapEndDate = lastEntryDate.plusDays(diffToEndOfWeek);

// 5. Entry'leri hızlı erişim için Map'e koy
        Map<Long, Entry> dateToEntryMap = entries.stream()
                .collect(Collectors.toMap(
                        Entry::getDateMillisYmd,
                        e -> e,
                        (e1, e2) -> e1)); // aynı tarihe denk gelirse ilkini al

// 6. entriesMap oluştur
        Map<Long, Entry> entriesMap = new LinkedHashMap<>();
        LocalDate currentDate = mapStartDate;

        while (!currentDate.isAfter(mapEndDate)) {
            long millisYmd = currentDate.atStartOfDay(zoneId).toInstant().toEpochMilli();
            entriesMap.put(millisYmd, dateToEntryMap.getOrDefault(millisYmd, null));
            currentDate = currentDate.plusDays(1);
        }

        // end  *****

    // Thymeleaf’in 2D tablo olusturabilmesi icin entriesMap'i haftalik liste listesine cevirelim:
        // *********** start
        List<List<Entry>> weeklyRows = new ArrayList<>();

        List<Entry> currentWeek = new ArrayList<>();
        int dayCounter = 0;

        for (Map.Entry<Long, Entry> mapEntry : entriesMap.entrySet()) {
            currentWeek.add(mapEntry.getValue());
            dayCounter++;

            if (dayCounter == 7) {
                weeklyRows.add(new ArrayList<>(currentWeek));
                currentWeek.clear();
                dayCounter = 0;
            }
        }

        model.addAttribute("weeklyRows", weeklyRows);
        // *********** end



        return "entries/entry-list-weekly-calendar-old";
    }

    @GetMapping("/new")
    public String showCreateForm(
            @RequestParam(name="topicId", required=false) Long topicId,
            @RequestParam(name="dateYmd", required=false) String dateString,
            @RequestParam(name="returnPage", required=false) String returnPage,
            @RequestParam(name="categoryId", required=false) Long categoryId,
            @RequestParam(name="categoryGroupId", required=false) Long categoryGroupId,
            Model model) {

        // System.out.println("dateYmd: " + dateString );
        Entry entry = new Entry();
        entry.setNote(new Note());
        entry.setStatus(0); // default olarak "not marked".

        // Topic set
        if (topicId != null) {
            Topic topic = topicRepository.findById(topicId).orElse(null);
            entry.setTopic(topic);
        }
        ZoneId zone = timeZoneProvider.getZoneId(); // olusturdugumuz component. application.properties'den zone ceker.

        if (dateString != null) {
            // "2025-03-27" gibi bir tarih formatını LocalDate'e parse edip epoch milise çeviriyoruz
            LocalDate localDate = LocalDate.parse(dateString, DateTimeFormatter.ofPattern("yyyy-MM-dd"));

            long epochMillis = localDate.atStartOfDay(zone).toInstant().toEpochMilli();
            // System.out.println(dateString + " " + localDate + " " + epochMillis);
            entry.setDateMillisYmd(epochMillis);
        } else {
            // Aksi halde bugünün tarih milisini varsayılan yap
            DateUtils dateUtils = new DateUtils();
            dateUtils.setZoneId(zone);
            entry.setDateMillisYmd(dateUtils.getEpochMillisToday());
        }

        // to make topic selection easier from gui, we are sending categories to selection box:
        List<Category> categories = categoryRepository.findAll();
        model.addAttribute("categories", categories);
// categoryId doluysa sadece o kategoriye ait topicleri çekelim:
        List<Topic> topics = (categoryId != null)
                ? topicRepository.findByCategoryIdOrderByPinnedDescNameAsc(categoryId)
                : topicRepository.findAll();
        model.addAttribute("entry", entry);
        model.addAttribute("topics", topics);
        //System.out.println("return page: " + returnPage);
        model.addAttribute("returnPage", returnPage);
        model.addAttribute("categoryId", categoryId);

        if(categoryId != null) {
            categoryGroupId = categoryRepository.findById(categoryId).get().getCategoryGroup().getId();
            model.addAttribute("categoryGroupId", categoryGroupId);
        } else if(categoryGroupId != null) {
            model.addAttribute("categoryGroupId", categoryGroupId);
        }

        return "entries/entry-form";
    }

    @GetMapping("/api/topicsByCategory")
    @ResponseBody
    public List<TopicDto> getTopicsByCategory(@RequestParam("categoryId") Long categoryId) {
        // İstediğiniz sorguya göre Topics döndürün
        // Örneğin: List<Topic> list = topicRepository.findByCategoryId(categoryId);
        // Veya custom sorgu...
        if (categoryId == null) {
            return List.of();
        }
        List<Topic> topics = topicRepository.findByCategoryIdOrderByPinnedDescNameAsc(categoryId);



        // Entity -> DTO dönüştürme
        return topics.stream()
                .map(t -> new TopicDto(t.getId(), t.getName()))
                .toList();
    }


    @PostMapping("/save")
    public String saveEntry(@ModelAttribute("entry") Entry formEntry,
                            Model model,
                            HttpServletRequest request) {

        String returnPage = request.getParameter("returnPage");
        List<Category> categories = categoryRepository.findAll(); // ayni tarihte cakisma varsa lazim oluyor formu tekrar gosterirken.

        // start **** category id nin elde edilmesi (ugly code)
        // 1) Burada önce formdan gelen categoryId değerini alıyoruz:
        String paramCategoryId = request.getParameter("categoryId");
        Long categoryIdFromForm = null;
        if (paramCategoryId != null && !paramCategoryId.isEmpty()) {
            categoryIdFromForm = Long.valueOf(paramCategoryId);
        }
        // 2) Elinizde hem "topic üzerinden gelen" hem de "formdan gizli input ile gelen"
        // categoryId bilgisi var. Hangisini kullanmak istediğinize karar verin:
        Long categoryId = formEntry.getTopic() != null && formEntry.getTopic().getCategory() != null
                ? formEntry.getTopic().getCategory().getId()
                : null;
        // Eğer "topic.category" null ise, en azından formdan gelen categoryId'yi kullanın
        if (categoryId == null && categoryIdFromForm != null) {
            categoryId = categoryIdFromForm;
        }
        // end   **** category id nin elde edilmesi (ugly code)
        Long categoryGroupId = categoryRepository.findById(categoryId).get().getCategoryGroup().getId();


        // 1) Duplicate kayıt kontrolü için gerekli bilgileri alıyoruz
        Long topicId = formEntry.getTopic() != null ? formEntry.getTopic().getId() : null;
        Long dateMillisYmd = formEntry.getDateMillisYmd();

        // (Opsiyonel) Null topic veya date durumuna karşı da tedbir alabilirsiniz.
        if (topicId == null || dateMillisYmd == null) {
            // İsterseniz hata veya uyarı verebilirsiniz.
        }

        // 2) Yeni kayıt mı yoksa güncelleme mi kontrol et
        if (formEntry.getId() == null) {
            // *** YENİ KAYIT SENARYOSU ***

            // a) Aynı topic+date'li kayıt var mı?
            Optional<Entry> existing = entryRepository.findByTopicIdAndDateMillisYmd(topicId, dateMillisYmd);
            if (existing.isPresent()) {
                // Kayıt varsa formu tekrar göster ve hata mesajı ver
                model.addAttribute("errorMessage", "There is already an entry for this topic on this date! Select a different topic or date or edit the existing entry.");
                model.addAttribute("entry", formEntry);
                model.addAttribute("topics", topicRepository.findAll());
                model.addAttribute("categories", categories);
                model.addAttribute("returnPage", returnPage);
                model.addAttribute("categoryId", categoryId);
                model.addAttribute("categoryGroupId", categoryGroupId);

                return "entries/entry-form";
            }

            // b) Not'u da two-way ilişkiyle bağla
            if (formEntry.getNote() != null) {
                formEntry.getNote().setEntry(formEntry);
            }

            // c) Artık güvenle kaydedebiliriz
            entryRepository.save(formEntry);

            topicService.updateTopicStatus(topicId);

        } else {
            // *** GÜNCELLEME SENARYOSU ***

            Entry dbEntry = entryRepository.findById(formEntry.getId())
                    .orElseThrow(() -> new RuntimeException("Entry bulunamadı: " + formEntry.getId()));

            // a) Aynı topic+date var mı diye kontrol et
            // Ama bulduğumuz kayıt kendi id'si ise problem değil;
            // farklı bir kaydın id'siyse problem.
            Optional<Entry> existing = entryRepository.findByTopicIdAndDateMillisYmd(topicId, dateMillisYmd);
            if (existing.isPresent() && !existing.get().getId().equals(formEntry.getId())) {
                // Yine hata mesajını modele ekleyip formu gösteriyoruz
                model.addAttribute("errorMessage", "There is already an entry for this topic on this date! Select a different topic or date or edit the existing entry.");
                model.addAttribute("entry", formEntry);
                model.addAttribute("topics", topicRepository.findAll());
                model.addAttribute("categories", categories);
                model.addAttribute("returnPage", returnPage);
                model.addAttribute("categoryId", categoryId);
                model.addAttribute("categoryGroupId", categoryGroupId);

                return "entries/entry-form";
            }

            Long oldTopicId = dbEntry.getTopic().getId(); // this is required. We will use this after entry save.
            boolean topicHasChanged = !Objects.equals(oldTopicId, formEntry.getTopic().getId());

            // b) Güncellenecek alanları setle
            dbEntry.setTopic(formEntry.getTopic());
            dbEntry.setDateMillisYmd(formEntry.getDateMillisYmd());
            dbEntry.setStatus(formEntry.getStatus());

            // c) Note güncelle
            if (dbEntry.getNote() == null) {
                Note newNote = new Note();
                newNote.setEntry(dbEntry);
                dbEntry.setNote(newNote);
            }
            dbEntry.getNote().setContent(
                    formEntry.getNote() != null ? formEntry.getNote().getContent() : ""
            );

            // d) DB'ye güncellenmiş halini kaydet
            entryRepository.save(dbEntry);

            // we need to update both of the topic statuses(for old and new topic) after saving the entry.
            if(topicHasChanged) {
                topicService.updateTopicStatus(oldTopicId);
            }
            topicService.updateTopicStatus(topicId);
        }





        return "redirect:/entries/redirect"
                + "?returnPage=" + returnPage
                + (categoryId != null ? "&categoryId=" + categoryId : "")
                + (topicId != null ? "&topicId=" + topicId : "")
                + (categoryGroupId != null ? "&categoryGroupId=" + categoryGroupId : "");




    }


    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable(name="id") Long entryId,
                               @RequestParam(name="topicId", required=false) Long topicId,
                               @RequestParam(name="returnPage", required=false) String returnPage,
                               @RequestParam(name="categoryId", required=false) Long categoryId,
                               Model model) {
        Entry entry = entryRepository.findById(entryId)
                .orElseThrow(() -> new RuntimeException("Entry bulunamadı: " + entryId));

        // Not nesnesi null ise formda null dönmemesi için burda oluşturabilirsiniz.
        if (entry.getNote() == null) {
            Note note = new Note();
            note.setEntry(entry);
            entry.setNote(note);
        }

        // to make topic selection easier from gui, we are sending categories to selection box:
        List<Category> categories = categoryRepository.findAll();
        model.addAttribute("categories", categories);
        List<Topic> topics = (categoryId != null)
                ? topicRepository.findByCategoryIdOrderByPinnedDescNameAsc(categoryId)
                : topicRepository.findAll();
        model.addAttribute("entry", entry);
        model.addAttribute("topics", topics);
        model.addAttribute("returnPage", returnPage);

        //Long topicId = entry.getTopic().getId();
        categoryId = entry.getTopic().getCategory().getId();
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("topicId", topicId);

        return "entries/entry-form";
    }

    @GetMapping("/editByDate")
    public String showEditFormByDate(@RequestParam(name="ymd", required=true) String ymd,
                               @RequestParam(name="returnPage", required=true) String returnPage,
                               @RequestParam(name="topicId", required=true) Long topicId,
                               Model model) {

        ZoneId zoneId = AppTimeZoneProvider.getZone();
        LocalDate localDate = LocalDate.parse(ymd);
        ZonedDateTime zdt = localDate.atStartOfDay(zoneId);
        long dateMillisYmd = zdt.toInstant().toEpochMilli();

        Entry entry = entryRepository.findByTopicIdAndDateMillisYmd(topicId, dateMillisYmd)
                .orElseThrow(() -> new RuntimeException("Entry bulunamadı: topicId=" + topicId + ", date=" + ymd));


        // Not nesnesi null ise formda null dönmemesi için burda oluşturabilirsiniz.
        if (entry.getNote() == null) {
            Note note = new Note();
            note.setEntry(entry);
            entry.setNote(note);
        }

        Long categoryId = entry.getTopic().getCategory().getId();
        Long categoryGroupId = entry.getTopic().getCategory().getCategoryGroup().getId();

        // to make topic selection easier from gui, we are sending categories to selection box:
        List<Category> categories = categoryRepository.findAll();
        model.addAttribute("categories", categories);
        List<Topic> topics = (categoryId != null)
                ? topicRepository.findByCategoryIdOrderByPinnedDescNameAsc(categoryId)
                : topicRepository.findAll();
        model.addAttribute("entry", entry);
        model.addAttribute("topics", topics);
        model.addAttribute("returnPage", returnPage);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("topicId", topicId);
        model.addAttribute("categoryGroupId", categoryGroupId);

        return "entries/entry-form";
    }


    @GetMapping("/delete/{id}")
    public String deleteEntry(@PathVariable Long id,
                              @RequestParam(name="returnPage", required=true) String returnPage) {


        Entry entry = entryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Entry bulunamadı: " + id));
        Long topicId = entry.getTopic().getId();
        Long categoryId = entry.getTopic().getCategory().getId();
        Long categoryGroupId = entry.getTopic().getCategory().getCategoryGroup().getId();
        //System.out.println("cg: " + categoryGroupId + ",c: " + categoryId + ",t: " + topicId + ",e :" + id + ", note: " );

        entryService.deleteEntryById(id);

      //  entryRepository.deleteById(id);
        //entryRepository.flush(); // delete hemen DB'ye gönderilir

        // predictionDate'i tekrar hesapla ve kaydet
        topicService.updateTopicStatus(topicId);

        return "redirect:/entries/redirect"
                + "?returnPage=" + returnPage
                + (categoryId != null ? "&categoryId=" + categoryId : "")
                + (topicId != null ? "&topicId=" + topicId : "")
                + (categoryGroupId != null ? "&categoryGroupId=" + categoryGroupId : "");

    }

    // EntriesController veya uygun controller
    @GetMapping("/{id}/note-full")
    @ResponseBody
    public String getFullNote(@PathVariable Long id) {
        return entryRepository.findById(id)
                .map(e -> e.getNote() != null ? convertToMarkdownHtml(e.getNote().getContent()) : "")
                .orElse("");
    }

    public String convertToMarkdownHtml(String text) {
        String converted = markdownService.render(text);
        return converted;
    }

    public String convertUrlsToLinksSafe(String text) {

        if (text == null) return "";

        // 1. HTML karakterlerini escape et (örneğin: <, >, &, ")
        String escaped = text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");

        // 2. Kaçmış metin içindeki URL'leri <a> etiketi ile sarmala
        String urlRegex = "(https?://\\S+)";
        return escaped.replaceAll(urlRegex,
                "<a href=\"$1\" target=\"_blank\" rel=\"noopener noreferrer\">$1</a>");
    }





    /**
     * entry form sayfasinda new veya update veya delete entry veya cancel islemleri sonrasinda
     * nereden gelindiyse oraya donmesi
     * icin genel bir yonlendirme metodu.
     * @param returnPage
     * @param categoryId
     * @param topicId
     * @return
     */
    @RequestMapping(value = "/redirect", method = {RequestMethod.GET, RequestMethod.POST})
    public String redirect(@RequestParam(required = false) String returnPage,
                           @RequestParam(required = false) Long categoryId,
                           @RequestParam(required = false) Long topicId,
                           @RequestParam(required = false) Long categoryGroupId) {

        if (returnPage != null) {
            switch (returnPage) {
                case "home":
                    return "redirect:/";
                case "topics":
                    return "redirect:/topics?categoryId=" + categoryId;
                case "pivottable":
                    return "redirect:/entry-filter/return?categoryId=" + categoryId;
                case "entries":
                    if(topicId != null) {
                        return "redirect:/entries?topicId=" + topicId;
                    } else {
                        // on entries page, there is an add new entry button, beacuse at that state there is no topicId,
                        // if you press cancel button on new entry form page, we are redirecting to entries page without
                        // topic id parameter. This is fix for the mentioned error.
                        return "redirect:/entries";
                    }
                case "reporttable":
                    return "redirect:/reports/all";
                case "cg":
                    return "redirect:/reports/cg?categoryGroupId=" + categoryGroupId;
                case "categories":
                    return "redirect:/categories";
                case "categorygroups":
                    return "redirect:/category-groups";

                // Eğer ileride farklı sayfalardan gelme ihtimali varsa
                default:
                    return "redirect:/";
            }
        }

        return "redirect:/";
    }


    @RequestMapping(value={"/entry-summary-report"},
            method={RequestMethod.GET,RequestMethod.POST})
    public String showFilteredEntries(
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer weight,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            Model model
    ) {
        if (status == null) status = 1;
        if (weight == null) weight = 0;

        ZoneId zoneId = timeZoneProvider.getZoneId();

        LocalDate today = LocalDate.now(zoneId);
        LocalDate defaultStart = today.minusDays(30);
        LocalDate defaultEnd = today;

        LocalDate startLocalDate = (startDate != null && !startDate.isEmpty()) ? LocalDate.parse(startDate) : defaultStart;
        LocalDate endLocalDate = (endDate != null && !endDate.isEmpty()) ? LocalDate.parse(endDate) : defaultEnd;

        long startingDate = startLocalDate.atStartOfDay(zoneId).toInstant().toEpochMilli();
        long endingDate = endLocalDate.atStartOfDay(zoneId).toInstant().toEpochMilli();

        List<TopicEntrySummaryDTO> summaries = filterService.getFilteredEntries(status, weight, startingDate, endingDate);
        model.addAttribute("entries", summaries);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedWeight", weight);
        model.addAttribute("selectedStartDate", startLocalDate.toString());
        model.addAttribute("selectedEndDate", endLocalDate.toString());
        model.addAttribute("zoneId", zoneId);

        return "view-entry-summary/entry-summary";
    }

    @GetMapping("/warnings/category/{id}")
    public String getWarningEntriesByCategory(@PathVariable("id") Long categoryId, Model model) {
        List<Entry> warningEntries = entryService.findWarningsByCategory(categoryId);
        model.addAttribute("warningEntries", warningEntries);
      todo:  return "fragments/fragwarningentries :: fragwarningentries";
    }


    @GetMapping("/neutrals/category/{id}")
    public String getNeutralEntriesByCategory(@PathVariable("id") Long categoryId, Model model) {
        ZoneId zoneId = timeZoneProvider.getZoneId();

        LocalDate today = LocalDate.now(zoneId);

        long dateMillisYmd = today.atStartOfDay(zoneId).toInstant().toEpochMilli();
        List<Entry> neutralEntries = entryService.findNeutralsByCategory(categoryId, dateMillisYmd);
        model.addAttribute("neutralEntries", neutralEntries);
        todo:  return "fragments/fragneutralentries :: fragneutralentries";
    }

    @GetMapping("/dones/category/{id}")
    public String getDoneEntriesByCategory(@PathVariable("id") Long categoryId, Model model) {
        ZoneId zoneId = timeZoneProvider.getZoneId();

        LocalDate today = LocalDate.now(zoneId);

        long dateMillisYmd = today.atStartOfDay(zoneId).toInstant().toEpochMilli();
        List<Entry> doneEntries = entryService.findDonesByCategory(categoryId, dateMillisYmd);
        model.addAttribute("doneEntries", doneEntries);
        todo:  return "fragments/fragdoneentries :: fragdoneentries";
    }

    @GetMapping("/predictions/category/{id}")
    public String getPredictionsByCategory(@PathVariable("id") Long categoryId, Model model) {
        ZoneId zoneId = timeZoneProvider.getZoneId();

        LocalDate today = LocalDate.now(zoneId);

        long dateMillisYmd = today.atStartOfDay(zoneId).toInstant().toEpochMilli();
        List<Topic> predictionTopics = topicService.getTopicsWithPredictionDateBeforeOrEqualToday(categoryId, dateMillisYmd);


        model.addAttribute("predictionTopics", predictionTopics);
        todo:  return "fragments/fragpredictiontopics :: fragpredictiontopics";
    }


    /* these methods below are working, but we did it in SQLite query.

    public static long convertToMillis(String dateStr, ZoneId zoneId) {
        LocalDate localDate = LocalDate.parse(dateStr); // "2025-06-30"
        ZonedDateTime zonedDateTime = localDate.atStartOfDay(zoneId); // 2025-06-30T00:00 at zone
        return zonedDateTime.toInstant().toEpochMilli(); // milisaniye cinsinden
    }

    private Double calculateSuccessPercentage(Integer dayInterval, Integer doneCount, Integer someTimeLater) {
        if (dayInterval == null || doneCount == null || someTimeLater == null) return null;
        if (dayInterval <= 0 || doneCount < 0 || someTimeLater <= 0) return null;

        double expectedCount = (double) dayInterval / someTimeLater;
        if (expectedCount == 0) return null;

        double actualCount = doneCount;
        double successRate = actualCount / expectedCount;
        return successRate * 100;
    }
    */

}
