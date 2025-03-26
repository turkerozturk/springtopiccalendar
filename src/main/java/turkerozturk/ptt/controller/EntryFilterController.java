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
     * Filtre formunu ilk kez açarken varsayılan değerleri dolduralım:
     * - Bu haftanın başlangıcı (parametrik, default Monday)
     * - Bu haftanın bitişi
     */
    @GetMapping("/form")
    public String filterForm(Model model) {

        FilterDto filterDto = new FilterDto();

        // Parametrik haftanın ilk günü => Pazartesi (veya sistem ayarınıza göre)
        LocalDate today = LocalDate.now();

        // Örnek: pazartesiyi bulalım
        LocalDate startOfWeek = filterService.getStartOfWeek(today, DayOfWeek.MONDAY);
        // Aynı şekilde haftanın son günü
        LocalDate endOfWeek = startOfWeek.plusDays(6);

        filterDto.setStartDate(startOfWeek);
        filterDto.setEndDate(endOfWeek);

        // topic ve status çoklu seçim için şu an boş
        // filterDto.getTopicIds() => boş
        // filterDto.getStatuses() => boş

        model.addAttribute("filterDto", filterDto);
        model.addAttribute("allTopics", topicRepository.findAll());
        return "entries/filter-form";
    }

    /**
     * Filtre uygulanınca => validasyon + sonuç listesi
     */
    @PostMapping("/apply")
    public String applyFilter(@Valid @ModelAttribute("filterDto") FilterDto filterDto,
                              BindingResult bindingResult,
                              Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("allTopics", topicRepository.findAll());
            return "entries/filter-form";
        }

        // Validasyon hatası yoksa DB'den filtreye uyan kayıtları çekelim:
        List<Entry> filteredEntries = filterService.filterEntries(filterDto);
        model.addAttribute("entries", filteredEntries);
        // Filtre formunu da tekrar gösterelim ama entries-list.html gibi de yapabilirsiniz
        model.addAttribute("filterDto", filterDto);
        model.addAttribute("allTopics", topicRepository.findAll());
        return "entries/filter-form";
    }

    /**
     * Tarih aralığını 'önceki periyot' kadar kaydırıp tekrar formu gösterir.
     */
    @PostMapping("/previous")
    public String previousRange(@ModelAttribute("filterDto") FilterDto filterDto,
                                Model model) {
        int rangeLength = filterService.getRangeLength(filterDto);
        // startDate ve endDate'i rangeLength kadar geri kaydıralım
        filterDto.setStartDate(filterDto.getStartDate().minusDays(rangeLength));
        filterDto.setEndDate(filterDto.getEndDate().minusDays(rangeLength));

        model.addAttribute("allTopics", topicRepository.findAll());
        return "entries/filter-form";
    }

    /**
     * Tarih aralığını 'sonraki periyot' kadar kaydırıp tekrar formu gösterir.
     */
    @PostMapping("/next")
    public String nextRange(@ModelAttribute("filterDto") FilterDto filterDto,
                            Model model) {
        int rangeLength = filterService.getRangeLength(filterDto);
        filterDto.setStartDate(filterDto.getStartDate().plusDays(rangeLength));
        filterDto.setEndDate(filterDto.getEndDate().plusDays(rangeLength));

        model.addAttribute("allTopics", topicRepository.findAll());
        return "entries/filter-form";
    }

    /**
     * Filtreyi temizle => varsayılan değerlere dön
     */
    @PostMapping("/clear")
    public String clearFilter(Model model) {
        return "redirect:/entry-filter/form"; // yeniden varsayılan doldurup form göster
    }
}
