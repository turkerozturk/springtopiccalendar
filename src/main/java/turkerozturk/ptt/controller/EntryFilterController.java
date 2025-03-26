package turkerozturk.ptt.controller;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import turkerozturk.ptt.dto.FilterDto;
import turkerozturk.ptt.entity.Entry;
import turkerozturk.ptt.repository.EntryRepository;
import turkerozturk.ptt.repository.TopicRepository;
import turkerozturk.ptt.service.FilterService;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
@Controller
@RequestMapping("/entry-filter")
public class EntryFilterController {

    private final EntryRepository entryRepository;
    private final TopicRepository topicRepository;
    private final FilterService filterService;

    public EntryFilterController(EntryRepository entryRepository,
                                 TopicRepository topicRepository,
                                 FilterService filterService) {
        this.entryRepository = entryRepository;
        this.topicRepository = topicRepository;
        this.filterService = filterService;
    }

    /**
     * Filtre formunu ilk kez açarken varsayılan değerleri doldurup
     * otomatik olarak filtre sorgusunu çalıştırıp ekrana sonuçları gönderir.
     */
    @GetMapping("/form")
    public String filterForm(Model model) {
        // 1) Varsayılan DTO Oluştur
        FilterDto filterDto = new FilterDto();

        // 2) Bu haftanın başı ve sonu (örneğin pazartesi - pazar)
        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = filterService.getStartOfWeek(today, DayOfWeek.MONDAY);
        LocalDate endOfWeek = startOfWeek.plusDays(6);

        filterDto.setStartDate(startOfWeek);
        filterDto.setEndDate(endOfWeek);
        // topicIds/statuses boş => “hepsi” gibi davranacak

        // 3) Filter sorgusunu çalıştır (ilk açılışta da verileri gösterelim)
        List<Entry> filteredEntries = filterService.filterEntries(filterDto);

        // 4) Model’e ekle
        model.addAttribute("filterDto", filterDto);
        model.addAttribute("entries", filteredEntries);
        model.addAttribute("allTopics", topicRepository.findAll());
        return "entries/filter-form";
    }

    /**
     * Filtre formunda 'Filtrele' butonuna tıklayınca çalışır.
     * Validasyon hatası yoksa sonuçları gösterir.
     */
    @PostMapping("/apply")
    public String applyFilter(@Valid @ModelAttribute("filterDto") FilterDto filterDto,
                              BindingResult bindingResult,
                              Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("allTopics", topicRepository.findAll());
            return "entries/filter-form";
        }

        // Filtre sorgusu
        List<Entry> filteredEntries = filterService.filterEntries(filterDto);
        model.addAttribute("entries", filteredEntries);
        model.addAttribute("filterDto", filterDto);
        model.addAttribute("allTopics", topicRepository.findAll());
        return "entries/filter-form";
    }

    /**
     * Tarih aralığını 'önceki periyot' kadar kaydırıp sonuçları tekrar gösterir.
     * Yani 'Filtrele' butonuna basmaya gerek kalmadan veriler listelenecek.
     */
    @PostMapping("/previous")
    public String previousRange(@ModelAttribute("filterDto") FilterDto filterDto,
                                Model model) {
        // Kaç günlük aralık?
        int rangeLength = filterService.getRangeLength(filterDto);
        // Tarihi geri kaydır
        filterDto.setStartDate(filterDto.getStartDate().minusDays(rangeLength));
        filterDto.setEndDate(filterDto.getEndDate().minusDays(rangeLength));

        // Aynı filtre sorgusunu çalıştır
        List<Entry> filteredEntries = filterService.filterEntries(filterDto);

        model.addAttribute("entries", filteredEntries);
        model.addAttribute("filterDto", filterDto);
        model.addAttribute("allTopics", topicRepository.findAll());
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

        model.addAttribute("entries", filteredEntries);
        model.addAttribute("filterDto", filterDto);
        model.addAttribute("allTopics", topicRepository.findAll());
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
}
