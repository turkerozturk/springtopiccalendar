package turkerozturk.ptt.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import turkerozturk.ptt.component.AppTimeZoneProvider;
import turkerozturk.ptt.dto.FilterDto;
import turkerozturk.ptt.dto.TopicDto;
import turkerozturk.ptt.entity.Entry;
import turkerozturk.ptt.entity.Topic;
import turkerozturk.ptt.repository.CategoryRepository;
import turkerozturk.ptt.repository.EntryRepository;
import turkerozturk.ptt.repository.TopicRepository;
import turkerozturk.ptt.service.FilterService;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@Controller
@RequestMapping("/entry-filter")
public class EntryFilterController {

    @Value("${pivot.table.date.format:yyyy-MM-dd}")
    private String dateFormat;

    @Value("${pivot.table.date.format.title:yyyy-MM-dd EEEE}")
    private String dateFormatTitle;


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
    public String filterForm(Model model, HttpSession session) {
        // 1) Varsayılan DTO Oluştur
        FilterDto filterDto = new FilterDto();

        // 2) Bu haftanın başı ve sonu (örneğin pazartesi - pazar)
        // Özellikten gelen değeri DayOfWeek'e dönüştür
        DayOfWeek startDay = DayOfWeek.valueOf(startDayOfWeek.toUpperCase());
        ZoneId zone = timeZoneProvider.getZoneId();  // Hazır metodunuz
        LocalDate today = LocalDate.now(zone);       // Şu anki tarih ve saat dilimini kullan
        LocalDate startOfWeek = filterService.getStartOfWeek(today, startDay);
        LocalDate endOfWeek = startOfWeek.plusDays(6);

        filterDto.setStartDate(startOfWeek);
        filterDto.setEndDate(endOfWeek);
        // topicIds/statuses boş => “hepsi” gibi davranacak

        // 3) Filter sorgusunu çalıştır (ilk açılışta da verileri gösterelim)
        // 1) Filtre ile gelen kayıtlardan "entries" listesi
        List<Entry> filteredEntries = filterService.filterEntries(filterDto);




        // 2) dateRange listesi (örneğin startDate -> endDate arası)
        List<LocalDate> dateRange = buildDateRangeList(filterDto.getStartDate(), filterDto.getEndDate());

        // 3) Pivot Data hazırla
        PivotData pivotData = buildPivotData(filteredEntries, dateRange);

        // === ✅ Session'a kaydet (ilk açılışta da) ===
        session.setAttribute("currentFilterDto", filterDto);

        // 4) Model’e ekle

        model.addAttribute("filterDto", filterDto);
        model.addAttribute("entries", filteredEntries);  // Normal tablo
        model.addAttribute("pivotData", pivotData);      // Pivot tablo
        model.addAttribute("dateFormat", dateFormat);
        model.addAttribute("dateFormatTitle", dateFormatTitle);
        // Bugünün tarihini modele ekleyelim
        model.addAttribute("today", today);

        model.addAttribute("allTopics", topicRepository.findAll());

        model.addAttribute("allCategories", categoryRepository.findAll());
        model.addAttribute("topicsForSelectedCategory", List.of());
        return "entries/filter-form";




    }

    @ResponseBody
    @GetMapping("/topics-by-category")
    public List<TopicDto> getTopicsByCategory(@RequestParam("categoryId") Long categoryId) {
        if (categoryId == null) {
            return List.of();
        }
        List<Topic> topics = topicRepository.findByCategoryId(categoryId);

        // Entity -> DTO dönüştürme
        return topics.stream()
                .map(t -> new TopicDto(t.getId(), t.getName()))
                .toList();
    }



    /**
     * Filtre formunda 'Filtrele' butonuna tıklayınca çalışır.
     * Validasyon hatası yoksa sonuçları gösterir.
     */
    @PostMapping("/apply")
    public String applyFilter(@Valid @ModelAttribute("filterDto") FilterDto filterDto,
                              BindingResult bindingResult,
                              Model model,
                              HttpSession session) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("allCategories", categoryRepository.findAll());
            // Eğer isterseniz tekrar topics çekebilirsiniz (seçili kategoriye göre)
            model.addAttribute("topicsForSelectedCategory",
                    topicRepository.findByCategoryId(filterDto.getCategoryId()));
            return "entries/filter-form";
        }




        // Aynı filtre sorgusunu çalıştır
        List<Entry> filteredEntries = filterService.filterEntries(filterDto);

        // 3) Date range listesi oluştur
        List<LocalDate> dateRange = buildDateRangeList(filterDto.getStartDate(), filterDto.getEndDate());

        // 4) Pivot Data oluştur
        PivotData pivotData = buildPivotData(filteredEntries, dateRange);

        // === (1) Session'a filtre bilgisini saklayalım ===
        session.setAttribute("currentFilterDto", filterDto);

        model.addAttribute("entries", filteredEntries);
        model.addAttribute("pivotData", pivotData);
        model.addAttribute("dateFormat", dateFormat);
        model.addAttribute("dateFormatTitle", dateFormatTitle);
        // Bugünün tarihini modele ekleyelim
        ZoneId zone = timeZoneProvider.getZoneId();  // Hazır metodunuz
        LocalDate today = LocalDate.now(zone);       // Şu anki tarih ve saat dilimini kullan
        model.addAttribute("today", today);

        model.addAttribute("filterDto", filterDto);
        model.addAttribute("allTopics", topicRepository.findAll());

        // Tekrar form gösterirseniz, seçili kategori vs. kaybolmasın
        model.addAttribute("allCategories", categoryRepository.findAll());
        model.addAttribute("topicsForSelectedCategory",
                topicRepository.findByCategoryId(filterDto.getCategoryId()));




        return "entries/filter-form";
    }

    /**
     * Tarih aralığını 'önceki periyot' kadar kaydırıp sonuçları tekrar gösterir.
     * Yani 'Filtrele' butonuna basmaya gerek kalmadan veriler listelenecek.
     */
    @PostMapping("/previous")
    public String previousRange(@ModelAttribute("filterDto") FilterDto filterDto,
                                Model model) {

        // 1) Tarih aralığını kaydır
        int rangeLength = filterService.getRangeLength(filterDto);
        filterDto.setStartDate(filterDto.getStartDate().minusDays(rangeLength));
        filterDto.setEndDate(filterDto.getEndDate().minusDays(rangeLength));

        // 2) Filtreye uyan entries'leri çek
        List<Entry> filteredEntries = filterService.filterEntries(filterDto);

        // 3) Date range listesi oluştur
        List<LocalDate> dateRange = buildDateRangeList(filterDto.getStartDate(), filterDto.getEndDate());

        // 4) Pivot Data oluştur
        PivotData pivotData = buildPivotData(filteredEntries, dateRange);

        // 5) Modele ekle
        model.addAttribute("entries", filteredEntries);
        model.addAttribute("pivotData", pivotData);
        model.addAttribute("dateFormat", dateFormat);
        model.addAttribute("dateFormatTitle", dateFormatTitle);
        // Bugünün tarihini modele ekleyelim
        ZoneId zone = timeZoneProvider.getZoneId();  // Hazır metodunuz
        LocalDate today = LocalDate.now(zone);       // Şu anki tarih ve saat dilimini kullan
        model.addAttribute("today", today);

        model.addAttribute("filterDto", filterDto);
        model.addAttribute("allTopics", topicRepository.findAll());


        // Tekrar form gösterirseniz, seçili kategori vs. kaybolmasın
        model.addAttribute("allCategories", categoryRepository.findAll());
        model.addAttribute("topicsForSelectedCategory",
                topicRepository.findByCategoryId(filterDto.getCategoryId()));

        // 6) Aynı form sayfasına dön
        return "entries/filter-form";
    }


    /**
     * Tarih aralığını 'sonraki periyot' kadar kaydırıp sonuçları tekrar gösterir.
     */
    @PostMapping("/next")
    public String nextRange(@ModelAttribute("filterDto") FilterDto filterDto,
                            Model model) {
        // Kaç günlük aralık?
        int rangeLength = filterService.getRangeLength(filterDto);
        // Tarihi ileri kaydır
        filterDto.setStartDate(filterDto.getStartDate().plusDays(rangeLength));
        filterDto.setEndDate(filterDto.getEndDate().plusDays(rangeLength));

        // Aynı filtre sorgusunu çalıştır
        List<Entry> filteredEntries = filterService.filterEntries(filterDto);

        // 3) Date range listesi oluştur
        List<LocalDate> dateRange = buildDateRangeList(filterDto.getStartDate(), filterDto.getEndDate());

        // 4) Pivot Data oluştur
        PivotData pivotData = buildPivotData(filteredEntries, dateRange);

        model.addAttribute("entries", filteredEntries);
        model.addAttribute("pivotData", pivotData);
        model.addAttribute("dateFormat", dateFormat);
        model.addAttribute("dateFormatTitle", dateFormatTitle);
        // Bugünün tarihini modele ekleyelim
        ZoneId zone = timeZoneProvider.getZoneId();  // Hazır metodunuz
        LocalDate today = LocalDate.now(zone);       // Şu anki tarih ve saat dilimini kullan
        model.addAttribute("today", today);

        model.addAttribute("filterDto", filterDto);
        model.addAttribute("allTopics", topicRepository.findAll());

        // Tekrar form gösterirseniz, seçili kategori vs. kaybolmasın
        model.addAttribute("allCategories", categoryRepository.findAll());
        model.addAttribute("topicsForSelectedCategory",
                topicRepository.findByCategoryId(filterDto.getCategoryId()));

        return "entries/filter-form";
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
     * startDate ile endDate arasındaki tüm günleri bir liste halinde döndürür.
     */
    private List<LocalDate> buildDateRangeList(LocalDate start, LocalDate end) {
        List<LocalDate> result = new ArrayList<>();
        LocalDate current = start;
        while (!current.isAfter(end)) {
            result.add(current);
            current = current.plusDays(1);
        }
        return result;
    }

    /**
     * filteredEntries içinden pivotMap oluşturur.
     */
    private PivotData buildPivotData(List<Entry> filteredEntries, List<LocalDate> dateRange) {
        // 1) Tüm topicId'leri bulalım ve bunları entity olarak toplayalım.
        //    (Aslında, map içerisinde dağınık. Aşağıda grouping yaparken elde edeceğiz.)
        Map<Long, Topic> topicMap = new HashMap<>();

        // pivotMap: topicId -> (LocalDate -> List<Entry>)
        Map<Long, Map<LocalDate, List<Entry>>> pivotMap = new HashMap<>();

        // topicEntryCount: topicId -> integer (toplam entry sayısı)
        Map<Long, Integer> topicEntryCount = new HashMap<>();

        for (Entry e : filteredEntries) {
            if (e.getTopic() == null) continue;  // topic yoksa atla
            Long tid = e.getTopic().getId();
            topicMap.put(tid, e.getTopic());

            // Tarihi LocalDate'e çeviriyoruz (çünkü dateMillisYmd = sadece gün)
            // (Mantık: 13 haneli milisi LocalDate'e dönüştürmek)
            LocalDate entryDate = convertMillisToLocalDate(e.getDateMillisYmd());

            // pivotMap içinde topicId var mı?
            pivotMap.putIfAbsent(tid, new HashMap<>());
            Map<LocalDate, List<Entry>> dayMap = pivotMap.get(tid);

            // dayMap içinde entryDate var mı?
            dayMap.putIfAbsent(entryDate, new ArrayList<>());
            dayMap.get(entryDate).add(e);

            // topicEntryCount için +1
            topicEntryCount.put(tid, topicEntryCount.getOrDefault(tid, 0) + 1);
        }

        // Tüm Topic’leri ID sırasına göre bir listeye alalım.
        List<Topic> topicList = new ArrayList<>(topicMap.values());
        topicList.sort(Comparator.comparing(Topic::getName)); // isterseniz ID'ye göre

        return new PivotData(dateRange, topicList, pivotMap, topicEntryCount);
    }

    /**
     * dateMillisYmd (13 haneli epoch -> sadece gün) => LocalDate
     * (Uygulama mantığınıza göre timezone vs. ayarlamanız gerekebilir)
     */
    private LocalDate convertMillisToLocalDate(Long dateMillis) {
        if (dateMillis == null) return null;
        return Instant.ofEpochMilli(dateMillis)
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
    }


    @GetMapping("/return")
    public String returnToFilterForm(HttpSession session, Model model) {
        FilterDto filterDto = (FilterDto) session.getAttribute("currentFilterDto");
        if (filterDto == null) {
            // FilterDto yoksa mecburen sıfırdan sayfa açabilir veya entries'e gidebilirsiniz.
            return "redirect:/entries";
        }

        // Tekrar filtre sonuçlarını oluşturup sayfaya bas
        List<Entry> filteredEntries = filterService.filterEntries(filterDto);
        List<LocalDate> dateRange = buildDateRangeList(filterDto.getStartDate(), filterDto.getEndDate());
        PivotData pivotData = buildPivotData(filteredEntries, dateRange);

        model.addAttribute("entries", filteredEntries);
        model.addAttribute("pivotData", pivotData);
        model.addAttribute("dateFormat", dateFormat);
        model.addAttribute("dateFormatTitle", dateFormatTitle);
        // Bugünün tarihini modele ekleyelim
        ZoneId zone = timeZoneProvider.getZoneId();  // Hazır metodunuz
        LocalDate today = LocalDate.now(zone);       // Şu anki tarih ve saat dilimini kullan
        model.addAttribute("today", today);

        model.addAttribute("filterDto", filterDto);
        model.addAttribute("allTopics", topicRepository.findAll());


        // Tekrar form gösterirseniz, seçili kategori vs. kaybolmasın
        model.addAttribute("allCategories", categoryRepository.findAll());
        model.addAttribute("topicsForSelectedCategory",
                topicRepository.findByCategoryId(filterDto.getCategoryId()));

        return "entries/filter-form";
    }





}
