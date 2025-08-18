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

import com.turkerozturk.dtt.entity.CategoryGroup;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import com.turkerozturk.dtt.component.AppTimeZoneProvider;
import com.turkerozturk.dtt.dto.FilterDto;
import com.turkerozturk.dtt.dto.TopicDto;
import com.turkerozturk.dtt.entity.Category;
import com.turkerozturk.dtt.entity.Entry;
import com.turkerozturk.dtt.entity.Topic;
import com.turkerozturk.dtt.repository.CategoryRepository;
import com.turkerozturk.dtt.repository.EntryRepository;
import com.turkerozturk.dtt.repository.TopicRepository;
import com.turkerozturk.dtt.service.FilterService;

import java.text.Collator;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Controller
@RequestMapping("/entry-filter")
public class EntryFilterController {

    @Value("${pivot.table.date.format:yyyy-MM-dd}")
    private String dateFormat;

    @Value("${pivot.table.date.format.title:yyyy-MM-dd EEEE}")
    private String dateFormatTitle;

    // topiclist satirlarini dile gore dogru siralamak icin
    @Value("${app.locale:en}")
    private String appLocale;

    private final AppTimeZoneProvider timeZoneProvider;

    private final EntryRepository entryRepository;
    private final TopicRepository topicRepository;
    private final FilterService filterService;

    private final CategoryRepository categoryRepository;

    public EntryFilterController(AppTimeZoneProvider timeZoneProvider, EntryRepository entryRepository,
                                 TopicRepository topicRepository,
                                 FilterService filterService, CategoryRepository categoryRepository) {
        this.timeZoneProvider = timeZoneProvider;
        this.entryRepository = entryRepository;
        this.topicRepository = topicRepository;
        this.filterService = filterService;
        this.categoryRepository = categoryRepository;
    }

    @Value("${week.start.day:MONDAY}")
    private String startDayOfWeek;


    /**
     * Filtre formunu ilk kez açarken varsayılan değerleri doldurup
     * otomatik olarak filtre sorgusunu çalıştırıp ekrana sonuçları gönderir.
     */
    @GetMapping("/form")
    public String filterForm(Model model,
                             HttpSession session,
                             @RequestParam(value = "reportType", required = false, defaultValue = "pivot") String reportType,
                             @RequestParam(value = "categoryId", required = false) Long categoryId)  {

        FilterDto filterDto = (FilterDto) session.getAttribute("currentFilterDto");
        if (filterDto == null) {
            filterDto = new FilterDto();
        }


        // 2) Bu haftanın başı ve sonu (örneğin pazartesi - pazar)
        ZoneId zoneId = timeZoneProvider.getZoneId();  // Hazır metodunuz
        LocalDate today = LocalDate.now(zoneId);       // Şu anki tarih ve saat dilimini kullan
        // Özellikten gelen değeri DayOfWeek'e dönüştür
        DayOfWeek startDay = DayOfWeek.valueOf(startDayOfWeek.toUpperCase());
        LocalDate startOfWeek = filterService.getStartOfWeek(today, startDay);
        model.addAttribute("startOfWeek", startOfWeek);

        // By default, the initial date range is 3 weeks(21 days);
        // This week (7 days of date range)
        LocalDate endOfWeek = startOfWeek.plusDays(6);
        // I decided to add previous week to the date range too:
        filterDto.setStartDate(startOfWeek.minusDays(7));
        // I decided to add next week to the date range too:
        filterDto.setEndDate(endOfWeek.plusDays(7));

        // topicIds/statuses boş => “hepsi” gibi davranacak

        List<Entry> filteredEntries;
        if(categoryId!=null) {
            filterDto.setCategoryId(categoryId);
            Optional<Category> categoryGroupOpt = categoryRepository.findById(categoryId);
            filterDto.setCategoryGroupId(categoryGroupOpt.get().getId());
        } else {
            List<Category> cats = categoryRepository.findAllByArchivedIsFalseOrderByCategoryGroup_PriorityDescNameAsc();
            if (!cats.isEmpty()) {
                categoryId = cats.get(0).getId();
                filterDto.setCategoryId(categoryId);
                CategoryGroup categoryGroup = cats.get(0).getCategoryGroup();
                filterDto.setCategoryGroupId(categoryGroup.getId());
            }
        }
        List<Topic> topicsOfTheCategory = topicRepository.findByCategoryIdOrderByPinnedDescNameAsc(filterDto.getCategoryId());

        List<Long> topicIds = new ArrayList<>();
        for (Topic topic : topicsOfTheCategory) {
            topicIds.add(topic.getId());
        }
        filterDto.setTopicIds(topicIds);

            // 3) Filter sorgusunu çalıştır (ilk açılışta da verileri gösterelim)
            // 1) Filtre ile gelen kayıtlardan "entries" listesi
            filteredEntries = filterService.filterEntries(filterDto);




        // 2) dateRange listesi (örneğin startDate -> endDate arası)
        List<LocalDate> dateRange = filterService.buildDateRangeList(filterDto.getStartDate(), filterDto.getEndDate());



        // === ✅ Session'a kaydet (ilk açılışta da) ===
        session.setAttribute("currentFilterDto", filterDto);

        // 4) Model’e ekle

        model.addAttribute("filterDto", filterDto);
        if(reportType.equals("normal")) {
            model.addAttribute("entries", filteredEntries);  // Normal tablo
        } else if(reportType.equals("pivot")){
            // 3) Pivot Data hazırla
            // seçili tüm topicId’leri DB’den entity olarak yükleyelim
            List<Topic> selectedTopics = topicRepository.findAllById(filterDto.getTopicIds());

            // PivotData’yı oluştururken selectedTopics’u da geçiyoruz
            PivotData pivotData = buildPivotData(filteredEntries, dateRange, selectedTopics,
                    filterDto.getEntriesThreshold());
            model.addAttribute("pivotData", pivotData);      // Pivot tablo
        }
        model.addAttribute("dateFormat", dateFormat);
        model.addAttribute("dateFormatTitle", dateFormatTitle);
        // Bugünün tarihini modele ekleyelim


        model.addAttribute("today", today);

        model.addAttribute("allTopics", topicRepository.findAll());

        model.addAttribute("allCategories", categoryRepository.findAllByArchivedIsFalseOrderByCategoryGroup_PriorityDescNameAsc());
        if(categoryId != null) {
            model.addAttribute("topicsForSelectedCategory",
                    topicRepository.findByCategoryIdOrderByPinnedDescNameAsc(filterDto.getCategoryId()));
        } else {
            model.addAttribute("topicsForSelectedCategory", List.of()); // bu satiri birakmamin sebebi,
            // mevcut kodlarda hala tracker tablosu aslinda sadece bir kategori icin degil, topicIdler icin yapilmis
            // olmasi ve eskiden ilk gelen gorunumde tabloda category sutunu da gorunurdu, bu durumda filter criteria
            // kutusunda secili bir topic listesi olmamaliydi ve hata vermemeliydi.
        }


        model.addAttribute("zoneId", zoneId);

        // we need this to get the "archived" value
        Category selectedCategory = categoryRepository.findById(filterDto.getCategoryId()).get();
        model.addAttribute("selectedCategory", selectedCategory);

        return "view-tracker/filter-form";




    }

    /**
     * JSON olarak bir kategorideki topiclerin id ve name degerlerini dondurur.
     * Bu metodu filtre seceneklerinde bir kategori secince seceneklerin altinda
     * topic listesi gorunmesi icin kullaniyorum. Fakat artik fragment donduren diger
     * metod sayfanin sol tarafinda daha kullanisli bir gorunum icinde ortaya cikiyor, onu
     * kullaniyorum.
     * @param categoryId
     * @return
     */
    @ResponseBody
    @GetMapping("/topics-by-category")
    public List<TopicDto> getTopicsByCategory(@RequestParam("categoryId") Long categoryId) {
        if (categoryId == null) {
            return List.of();
        }
        List<Topic> topics = topicRepository.findByCategoryIdOrderByPinnedDescNameAsc(categoryId);

        // Entity -> DTO dönüştürme
        return topics.stream()
                .map(t -> new TopicDto(t.getId(), t.getName()))
                .toList();
    }


    /**
     * There is a thymeleaf fragment named "topicList" inside the filter-form.html template.
     * This method returns data to fill it.
     * a button with htmx code is calling this method. To show the topics list inside a offcanvas
     * on the left side of webpage.
     * @param categoryId
     * @param model
     * @return
     */
    @GetMapping("/fragment-topics-by-category")
    public String topicsByCategory(
            @RequestParam Long categoryId,
            Model model) {
        List<Topic> topics = topicRepository.findByCategoryIdOrderByPinnedDescNameAsc(categoryId);
        model.addAttribute("topics", topics);
        model.addAttribute("categoryId", categoryId);
        return "entries/fragments/_topics-by-category :: topicList";
    }




    /**
     * Filtre formunda 'Filtrele' butonuna tıklayınca çalışır.
     * Validasyon hatası yoksa sonuçları gösterir.
     */
    @PostMapping("/apply")
    public String applyCategoryShiftingFilter(@Valid @ModelAttribute("filterDto") FilterDto filterDto,
                                              BindingResult bindingResult,
                                              Model model,
                                              HttpSession session,
                                              @RequestParam(value = "reportType", required = false, defaultValue = "pivot") String reportType) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("allCategories", categoryRepository.findAllByArchivedIsFalseOrderByCategoryGroup_PriorityDescNameAsc());
            // Eğer isterseniz tekrar topics çekebilirsiniz (seçili kategoriye göre)
            model.addAttribute("topicsForSelectedCategory",
                    topicRepository.findByCategoryIdOrderByPinnedDescNameAsc(filterDto.getCategoryId()));
            return "view-tracker/filter-form";
        }




        // Aynı filtre sorgusunu çalıştır
        List<Entry> filteredEntries = filterService.filterEntries(filterDto);

        // 3) Date range listesi oluştur
        List<LocalDate> dateRange = filterService.buildDateRangeList(filterDto.getStartDate(), filterDto.getEndDate());




        // === (1) Session'a filtre bilgisini saklayalım ===
        session.setAttribute("currentFilterDto", filterDto);



        if(reportType.equals("normal")) {
            model.addAttribute("entries", filteredEntries);  // Normal tablo
        } else if(reportType.equals("pivot")){
            // 3) Pivot Data hazırla
            // seçili tüm topicId’leri DB’den entity olarak yükleyelim
            List<Topic> selectedTopics = topicRepository.findAllById(filterDto.getTopicIds());

            // PivotData’yı oluştururken selectedTopics’u da geçiyoruz
            PivotData pivotData = buildPivotData(filteredEntries, dateRange, selectedTopics,
                    filterDto.getEntriesThreshold());

            model.addAttribute("pivotData", pivotData);      // Pivot tablo
        }
        model.addAttribute("dateFormat", dateFormat);
        model.addAttribute("dateFormatTitle", dateFormatTitle);
        // Bugünün tarihini modele ekleyelim
        ZoneId zoneId = timeZoneProvider.getZoneId();  // Hazır metodunuz
        LocalDate today = LocalDate.now(zoneId);       // Şu anki tarih ve saat dilimini kullan
        // Özellikten gelen değeri DayOfWeek'e dönüştür
        DayOfWeek startDay = DayOfWeek.valueOf(startDayOfWeek.toUpperCase());
        LocalDate startOfWeek = filterService.getStartOfWeek(today, startDay);
        model.addAttribute("startOfWeek", startOfWeek);

        model.addAttribute("today", today);

        model.addAttribute("filterDto", filterDto);
        model.addAttribute("allTopics", topicRepository.findAll());

        // Tekrar form gösterirseniz, seçili kategori vs. kaybolmasın
        model.addAttribute("allCategories", categoryRepository.findAllByArchivedIsFalseOrderByCategoryGroup_PriorityDescNameAsc());
        model.addAttribute("topicsForSelectedCategory",
                topicRepository.findByCategoryIdOrderByPinnedDescNameAsc(filterDto.getCategoryId()));


        model.addAttribute("zoneId", zoneId);

        // we need this to get the "archived" value
        Category selectedCategory = categoryRepository.findById(filterDto.getCategoryId()).get();
        model.addAttribute("selectedCategory", selectedCategory);

        return "view-tracker/filter-form";
    }

    /**
     * Tarih aralığını 'önceki periyot' kadar kaydırıp sonuçları tekrar gösterir.
     * Yani 'Filtrele' butonuna basmaya gerek kalmadan veriler listelenecek.
     */
    @PostMapping("/previous")
    public String previousRange(@ModelAttribute("filterDto") FilterDto filterDto,
                                Model model,
                                HttpSession session,
                                @RequestParam(value = "reportType", required = false, defaultValue = "pivot") String reportType) {

        // 1) Tarih aralığını kaydır
        int rangeLength = filterService.getRangeLength(filterDto);
        filterDto.setStartDate(filterDto.getStartDate().minusDays(rangeLength));
        filterDto.setEndDate(filterDto.getEndDate().minusDays(rangeLength));

        // 2) Filtreye uyan entries'leri çek
        List<Entry> filteredEntries = filterService.filterEntries(filterDto);

        // 3) Date range listesi oluştur
        List<LocalDate> dateRange = filterService.buildDateRangeList(filterDto.getStartDate(), filterDto.getEndDate());



        // === (1) Session'a filtre bilgisini saklayalım ===
        session.setAttribute("currentFilterDto", filterDto);

        // 5) Modele ekle
        if(reportType.equals("normal")) {
            model.addAttribute("entries", filteredEntries);  // Normal tablo
        } else if(reportType.equals("pivot")){
            // 3) Pivot Data hazırla
            // seçili tüm topicId’leri DB’den entity olarak yükleyelim
            List<Topic> selectedTopics = topicRepository.findAllById(filterDto.getTopicIds());

            // PivotData’yı oluştururken selectedTopics’u da geçiyoruz
            PivotData pivotData = buildPivotData(filteredEntries, dateRange, selectedTopics,
                    filterDto.getEntriesThreshold());
            model.addAttribute("pivotData", pivotData);      // Pivot tablo
        }
        model.addAttribute("dateFormat", dateFormat);
        model.addAttribute("dateFormatTitle", dateFormatTitle);
        // Bugünün tarihini modele ekleyelim
        ZoneId zoneId = timeZoneProvider.getZoneId();  // Hazır metodunuz
        LocalDate today = LocalDate.now(zoneId);       // Şu anki tarih ve saat dilimini kullan
        // Özellikten gelen değeri DayOfWeek'e dönüştür
        DayOfWeek startDay = DayOfWeek.valueOf(startDayOfWeek.toUpperCase());
        LocalDate startOfWeek = filterService.getStartOfWeek(today, startDay);
        model.addAttribute("startOfWeek", startOfWeek);

        model.addAttribute("today", today);

        model.addAttribute("filterDto", filterDto);
        model.addAttribute("allTopics", topicRepository.findAll());


        // Tekrar form gösterirseniz, seçili kategori vs. kaybolmasın
        model.addAttribute("allCategories", categoryRepository.findAllByArchivedIsFalseOrderByCategoryGroup_PriorityDescNameAsc());
        model.addAttribute("topicsForSelectedCategory",
                topicRepository.findByCategoryIdOrderByPinnedDescNameAsc(filterDto.getCategoryId()));

        model.addAttribute("zoneId", zoneId);

        // we need this to get the "archived" value
        Category selectedCategory = categoryRepository.findById(filterDto.getCategoryId()).get();
        model.addAttribute("selectedCategory", selectedCategory);

        CategoryGroup categoryGroup = selectedCategory.getCategoryGroup();
        filterDto.setCategoryGroupId(categoryGroup.getId());

        return "redirect:/entry-filter/return"; // To prevent document expired error https://en.wikipedia.org/wiki/Post/Redirect/Get
        // return "view-tracker/filter-form"; // gives Document Expired error Bu sorunun standart cozumu PRG (Post-Redirect-Get) desenidir.

    }


    /**
     * Tarih aralığını 'sonraki periyot' kadar kaydırıp sonuçları tekrar gösterir.
     */
    @PostMapping("/next")
    public String nextRange(@ModelAttribute("filterDto") FilterDto filterDto,
                            Model model,
                            HttpSession session,
                            @RequestParam(value = "reportType", required = false, defaultValue = "pivot") String reportType) {


        // Kaç günlük aralık?
        int rangeLength = filterService.getRangeLength(filterDto);
        // Tarihi ileri kaydır
        filterDto.setStartDate(filterDto.getStartDate().plusDays(rangeLength));
        filterDto.setEndDate(filterDto.getEndDate().plusDays(rangeLength));

        // Aynı filtre sorgusunu çalıştır
        List<Entry> filteredEntries = filterService.filterEntries(filterDto);

        // 3) Date range listesi oluştur
        List<LocalDate> dateRange = filterService.buildDateRangeList(filterDto.getStartDate(), filterDto.getEndDate());


        // === (1) Session'a filtre bilgisini saklayalım ===
        session.setAttribute("currentFilterDto", filterDto);

        if(reportType.equals("normal")) {
            model.addAttribute("entries", filteredEntries);  // Normal tablo
        } else if(reportType.equals("pivot")){
            // 3) Pivot Data hazırla
            // seçili tüm topicId’leri DB’den entity olarak yükleyelim
            List<Topic> selectedTopics = topicRepository.findAllById(filterDto.getTopicIds());

            // PivotData’yı oluştururken selectedTopics’u da geçiyoruz
            PivotData pivotData = buildPivotData(filteredEntries, dateRange, selectedTopics,
                    filterDto.getEntriesThreshold());
            model.addAttribute("pivotData", pivotData);      // Pivot tablo
        }
        model.addAttribute("dateFormat", dateFormat);
        model.addAttribute("dateFormatTitle", dateFormatTitle);
        // Bugünün tarihini modele ekleyelim
        ZoneId zoneId = timeZoneProvider.getZoneId();  // Hazır metodunuz
        LocalDate today = LocalDate.now(zoneId);       // Şu anki tarih ve saat dilimini kullan
        // Özellikten gelen değeri DayOfWeek'e dönüştür
        DayOfWeek startDay = DayOfWeek.valueOf(startDayOfWeek.toUpperCase());
        LocalDate startOfWeek = filterService.getStartOfWeek(today, startDay);
        model.addAttribute("startOfWeek", startOfWeek);


        model.addAttribute("today", today);

        model.addAttribute("filterDto", filterDto);
        model.addAttribute("allTopics", topicRepository.findAll());

        // Tekrar form gösterirseniz, seçili kategori vs. kaybolmasın
        model.addAttribute("allCategories", categoryRepository.findAllByArchivedIsFalseOrderByCategoryGroup_PriorityDescNameAsc());
        model.addAttribute("topicsForSelectedCategory",
                topicRepository.findByCategoryIdOrderByPinnedDescNameAsc(filterDto.getCategoryId()));

        model.addAttribute("zoneId", zoneId);

        // we need this to get the "archived" value
        Category selectedCategory = categoryRepository.findById(filterDto.getCategoryId()).get();
        model.addAttribute("selectedCategory", selectedCategory);

        CategoryGroup categoryGroup = selectedCategory.getCategoryGroup();
        filterDto.setCategoryGroupId(categoryGroup.getId());

        return "redirect:/entry-filter/return"; // To prevent document expired error https://en.wikipedia.org/wiki/Post/Redirect/Get
        // return "view-tracker/filter-form"; // gives Document Expired error Bu sorunun standart cozumu PRG (Post-Redirect-Get) desenidir.

    }

    /**
     * Filtreyi temizle => varsayılan değerlere dön (yani /form endpointine yönlendir).
     * Orada sayfa açılırken otomatik filtre sorgusu yapıyor zaten.
     */
    @PostMapping("/clear")
    public String clearFilter() {
        return "redirect:/entry-filter/form";
    }












    /**
     * filteredEntries içinden pivotMap oluşturur.
     * @param filteredEntries   entries already globally filtered
     * @param dateRange         list of all LocalDates in the period
     * @param selectedTopics    all Topics the user selected
     * @param entriesThreshold  if >0, only topics with at least this many entries will be shown
     */
    private PivotData buildPivotData(
            List<Entry> filteredEntries,
            List<LocalDate> dateRange,
            List<Topic> selectedTopics,
            Integer entriesThreshold
    ) {
        // --- 1) pre-count entries per topic ---
        Map<Long, Integer> preCount = new HashMap<>();
        for (Topic t : selectedTopics) {
            preCount.put(t.getId(), 0);
        }
        for (Entry e : filteredEntries) {
            if (e.getTopic() == null) continue;
            Long tid = e.getTopic().getId();
            preCount.computeIfPresent(tid, (k, c) -> c + 1);
        }

        // --- 2) only include topics meeting threshold ---
        Map<Long, Topic> topicMap = new HashMap<>();
        Map<Long, Map<LocalDate, List<Entry>>> pivotMap = new HashMap<>();
        Map<Long, Integer> topicEntryCount = new HashMap<>();

        for (Topic t : selectedTopics) {
            Long tid = t.getId();
            int count = preCount.getOrDefault(tid, 0);
            if (entriesThreshold != null
                    && entriesThreshold > 0
                    && count < entriesThreshold) {
                continue;
            }
            topicMap.put(tid, t);
            pivotMap.put(tid, new HashMap<>());
            topicEntryCount.put(tid, count);
        }

        // --- 3) distribute entries into the pivotMap only for kept topics ---
        for (Entry e : filteredEntries) {
            if (e.getTopic() == null) continue;
            Long tid = e.getTopic().getId();
            Map<LocalDate, List<Entry>> dayMap = pivotMap.get(tid);
            if (dayMap == null) continue;

            LocalDate entryDate = convertMillisToLocalDate(e.getDateMillisYmd());
            dayMap.computeIfAbsent(entryDate, d -> new ArrayList<>())
                    .add(e);
        }

        // --- 4b) inject synthetic "predictionDate" entries if no entry on that date ---
        for (Map.Entry<Long, Topic> me : topicMap.entrySet()) {
            Long tid = me.getKey();
            Topic topic = me.getValue();
            LocalDate predDate = topic.getPredictionDate();  // your @Transient hesaplanan tarih
            if (predDate == null) continue;

            Map<LocalDate, List<Entry>> dayMap = pivotMap.get(tid);
            List<Entry> entriesAtPred = dayMap.get(predDate);
            // o tarihte hiç entry (gerçek ya da synthetic) yoksa ekle
            if (entriesAtPred != null && !entriesAtPred.isEmpty()) continue;

            ZoneId zoneId = timeZoneProvider.getZoneId();  // Hazır metodunuz

            Entry syntheticPred = new Entry();
            syntheticPred.setTopic(topic);
            syntheticPred.setStatus(3);
            syntheticPred.setDateMillisYmd(
                    predDate.atStartOfDay(zoneId)
                            .toInstant().toEpochMilli()
            );

            dayMap.computeIfAbsent(predDate, d -> new ArrayList<>())
                    .add(syntheticPred);
        }

        // --- 5) sorted topic list for the UI ---
        List<Topic> topicList = new ArrayList<>(topicMap.values());

        // 1) Locale nesnesi
        Locale locale = Locale.forLanguageTag(appLocale);

        // 2) Collator: case-insensitive ve aksansız karşılaştırma
        Collator collator = Collator.getInstance(locale);
        collator.setStrength(Collator.PRIMARY);

        // 3) Listeyi ada göre sıralayıp tekrar set edin
        List<Topic> sortedTopics = topicList.stream()
                .sorted(Comparator.comparing(Topic::getName, collator))
                .collect(Collectors.toList());


        // Önce pinned = 1 olanlar (true/int 1) -> sonra pinned = 0
        // Her iki grubun içinde de name A→Z
        sortedTopics.sort(
                // eğer pinned bir boolean ise:
                Comparator.comparing(Topic::isPinned).reversed()
                       // .thenComparing(Topic::getName)

                /* eğer pinned bir int/Integer ise, şu satırı kullanın:
                Comparator.comparingInt(Topic::getPinned).reversed()
                  .thenComparing(Topic::getName)
                */
        );




        return new PivotData(dateRange, sortedTopics, pivotMap, topicEntryCount);
    }







    /**
     * dateMillisYmd (13 haneli epoch -> sadece gün) => LocalDate
     * (Uygulama mantığınıza göre timezone vs. ayarlamanız gerekebilir)
     */
    private LocalDate convertMillisToLocalDate(Long dateMillis) {
        if (dateMillis == null) return null;
        ZoneId zoneId = timeZoneProvider.getZoneId();  // Hazır metodunuz

        return Instant.ofEpochMilli(dateMillis)
                .atZone(zoneId)
                .toLocalDate();
    }


    @GetMapping("/return")
    public String returnToFilterForm(HttpSession session,
                                     Model model,
                                     @RequestParam(value = "reportType", required = false, defaultValue = "pivot") String reportType) {
        FilterDto filterDto = (FilterDto) session.getAttribute("currentFilterDto");
        if (filterDto == null) {
            // FilterDto yoksa mecburen sıfırdan sayfa açabilir veya entries'e gidebilirsiniz.
            return "redirect:/entries";
        }

        // Tekrar filtre sonuçlarını oluşturup sayfaya bas
        List<Entry> filteredEntries = filterService.filterEntries(filterDto);
        List<LocalDate> dateRange = filterService.buildDateRangeList(filterDto.getStartDate(), filterDto.getEndDate());
        if(reportType.equals("normal")) {
            model.addAttribute("entries", filteredEntries);  // Normal tablo
        } else if(reportType.equals("pivot")){
            // 3) Pivot Data hazırla
            // seçili tüm topicId’leri DB’den entity olarak yükleyelim
            List<Topic> selectedTopics = topicRepository.findAllById(filterDto.getTopicIds());

            // PivotData’yı oluştururken selectedTopics’u da geçiyoruz
            PivotData pivotData = buildPivotData(filteredEntries, dateRange, selectedTopics,
                    filterDto.getEntriesThreshold());
            model.addAttribute("pivotData", pivotData);      // Pivot tablo
        }
        model.addAttribute("dateFormat", dateFormat);
        model.addAttribute("dateFormatTitle", dateFormatTitle);
        // Bugünün tarihini modele ekleyelim
        ZoneId zoneId = timeZoneProvider.getZoneId();  // Hazır metodunuz
        LocalDate today = LocalDate.now(zoneId);       // Şu anki tarih ve saat dilimini kullan
        DayOfWeek startDay = DayOfWeek.valueOf(startDayOfWeek.toUpperCase());

        LocalDate startOfWeek = filterService.getStartOfWeek(today, startDay);
        model.addAttribute("startOfWeek", startOfWeek);

        model.addAttribute("today", today);

        model.addAttribute("filterDto", filterDto);
        model.addAttribute("allTopics", topicRepository.findAll());


        // Tekrar form gösterirseniz, seçili kategori vs. kaybolmasın
        model.addAttribute("allCategories", categoryRepository.findAllByArchivedIsFalseOrderByCategoryGroup_PriorityDescNameAsc());
        model.addAttribute("topicsForSelectedCategory",
                topicRepository.findByCategoryIdOrderByPinnedDescNameAsc(filterDto.getCategoryId()));

        model.addAttribute("zoneId", zoneId);

        // we need this to get the "archived" value
        Category selectedCategory = categoryRepository.findById(filterDto.getCategoryId()).get();
        model.addAttribute("selectedCategory", selectedCategory);

        return "view-tracker/filter-form";
    }



    /**
     * ortak filtre uygulama logic’i
     * Please look at the previousCategory and nextCategory methods first. This method is part of them.
     * If the user clicks on to ◀️cat or cat▶️ form buttons from pivot table tracker page,
     * previousCategory, nextCategory and this method is responsible.
     * @param filterDto
     * @param model
     * @param session
     * @param reportType
     * @return
     */
    private String applyCategoryShiftingFilter(FilterDto filterDto,
                                               Model model,
                                               HttpSession session,
                                               String reportType) {

        // start. We need to get topic ID's and set to filterDto. Because this method is for category shift.
        List<Topic> topicsOfTheCategory = topicRepository.findByCategoryIdOrderByPinnedDescNameAsc(filterDto.getCategoryId());
        List<Long> topicIds = new ArrayList<>();
        for(Topic topic : topicsOfTheCategory) {
            topicIds.add(topic.getId());
        }
        filterDto.setTopicIds(topicIds);
        // 1) session’a kaydet
        session.setAttribute("currentFilterDto", filterDto);
        // end.



        // 2) filteredEntries
        List<Entry> filtered = filterService.filterEntries(filterDto);

        // 3) dateRange
        List<LocalDate> dateRange = filterService.buildDateRangeList(filterDto.getStartDate(), filterDto.getEndDate());

        // 4) model’e pivot ya da normal tablo
        if ("normal".equals(reportType)) {
            model.addAttribute("entries", filtered);
        } else {

            PivotData pivot = buildPivotData(filtered, dateRange, topicsOfTheCategory,
                    filterDto.getEntriesThreshold());
            model.addAttribute("pivotData", pivot);
        }

        // 5) ortak model attrs
        model.addAttribute("dateFormat", dateFormat);
        model.addAttribute("dateFormatTitle", dateFormatTitle);
        // Özellikten gelen değeri DayOfWeek'e dönüştür
        DayOfWeek startDay = DayOfWeek.valueOf(startDayOfWeek.toUpperCase());
        LocalDate today = LocalDate.now(timeZoneProvider.getZoneId());
        LocalDate startOfWeek = filterService.getStartOfWeek(today, startDay);
        model.addAttribute("startOfWeek", startOfWeek);

        model.addAttribute("today", today);
        model.addAttribute("filterDto", filterDto);

        model.addAttribute("allCategories", categoryRepository.findAllByArchivedIsFalseOrderByCategoryGroup_PriorityDescNameAsc());
        model.addAttribute("topicsForSelectedCategory",
                topicRepository.findByCategoryIdOrderByPinnedDescNameAsc(filterDto.getCategoryId()));
        // … gerekirse allTopics vs.

        ZoneId zoneId = timeZoneProvider.getZoneId();  // Hazır metodunuz

        model.addAttribute("zoneId", zoneId);


        // we need this to get the "archived" value
        Category selectedCategory = categoryRepository.findById(filterDto.getCategoryId()).get();
        model.addAttribute("selectedCategory", selectedCategory);

        return "redirect:/entry-filter/return"; // To prevent document expired error https://en.wikipedia.org/wiki/Post/Redirect/Get
        // return "view-tracker/filter-form"; // gives Document Expired error Bu sorunun standart cozumu PRG (Post-Redirect-Get) desenidir.
    }

    /**
     * Shifts categoryID to previous one. If the beginning has come, it starts from the end.
     * @param filterDto
     * @param model
     * @param session
     * @param reportType
     * @return
     */
    @PostMapping("/previousCategory")
    public String previousCategory(@ModelAttribute("filterDto") FilterDto filterDto,
                                   Model model,
                                   HttpSession session,
                                   @RequestParam(value = "reportType", defaultValue = "pivot") String reportType) {
        List<Category> cats = categoryRepository.findAllByArchivedIsFalseOrderByCategoryGroup_PriorityDescNameAsc();
        int size = cats.size();

        // mevcut indexi bul
        Long current = filterDto.getCategoryId();
        int idx = IntStream.range(0, size)
                .filter(i -> cats.get(i).getId().equals(current))
                .findFirst()
                .orElse(-1);

        // yeni index: eğer null ya da ilkse sona git, değilse idx-1
        int newIdx = (idx <= 0) ? size - 1 : idx - 1;
        filterDto.setCategoryId(cats.get(newIdx).getId());

        CategoryGroup categoryGroup = cats.get(newIdx).getCategoryGroup();
        filterDto.setCategoryGroupId(categoryGroup.getId());

        return applyCategoryShiftingFilter(filterDto, model, session, reportType);
    }

    /**
     * Shifts categoryID to next one. If the end is reached, it starts from the beginning.
     * @param filterDto
     * @param model
     * @param session
     * @param reportType
     * @return
     */
    @PostMapping("/nextCategory")
    public String nextCategory(@ModelAttribute("filterDto") FilterDto filterDto,
                               Model model,
                               HttpSession session,
                               @RequestParam(value = "reportType", defaultValue = "pivot") String reportType) {


        List<Category> cats = categoryRepository.findAllByArchivedIsFalseOrderByCategoryGroup_PriorityDescNameAsc();
        int size = cats.size();

        Long current = filterDto.getCategoryId();
        int idx = IntStream.range(0, size)
                .filter(i -> cats.get(i).getId().equals(current))
                .findFirst()
                .orElse(-1);

        // yeni index: eğer null ya da son ise başa git, değilse idx+1
        int newIdx = (idx < 0 || idx == size - 1) ? 0 : idx + 1;
        filterDto.setCategoryId(cats.get(newIdx).getId());

        CategoryGroup categoryGroup = cats.get(newIdx).getCategoryGroup();
        filterDto.setCategoryGroupId(categoryGroup.getId());

        return applyCategoryShiftingFilter(filterDto, model, session, reportType);
    }
}


