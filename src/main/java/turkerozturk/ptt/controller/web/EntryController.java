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
package turkerozturk.ptt.controller.web;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import turkerozturk.ptt.component.AppTimeZoneProvider;
import turkerozturk.ptt.dto.FilterDto;
import turkerozturk.ptt.dto.TopicDto;
import turkerozturk.ptt.dto.TopicEntrySummaryDTO;
import turkerozturk.ptt.entity.Category;
import turkerozturk.ptt.entity.Entry;
import turkerozturk.ptt.entity.Note;
import turkerozturk.ptt.entity.Topic;
import turkerozturk.ptt.helper.DateUtils;
import turkerozturk.ptt.repository.CategoryRepository;
import turkerozturk.ptt.repository.EntryRepository;
import turkerozturk.ptt.repository.TopicRepository;
import turkerozturk.ptt.service.FilterService;
import turkerozturk.ptt.service.TopicService;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;


@Controller
@RequestMapping("/entries")
public class EntryController {


    private final AppTimeZoneProvider timeZoneProvider;
    private final EntryRepository entryRepository;

    private final FilterService entryService;
    private final TopicRepository topicRepository;

    private final TopicService topicService;

    private final CategoryRepository categoryRepository;


    public EntryController(AppTimeZoneProvider timeZoneProvider, EntryRepository entryRepository, FilterService entryService, TopicRepository topicRepository, TopicService topicService, CategoryRepository categoryRepository) {
        this.timeZoneProvider = timeZoneProvider;
        this.entryRepository = entryRepository;
        this.entryService = entryService;
        this.topicRepository = topicRepository;
        this.topicService = topicService;
        this.categoryRepository = categoryRepository;
    }

    @GetMapping
    public String listEntries(
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



        return "entries/entry-list";
    }

    @GetMapping("/new")
    public String showCreateForm(
            @RequestParam(name="topicId", required=false) Long topicId,
            @RequestParam(name="dateYmd", required=false) String dateString,
            @RequestParam(name="returnPage", required=false) String returnPage,
            @RequestParam(name="categoryId", required=false) Long categoryId,
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
                + (topicId != null ? "&topicId=" + topicId : "");




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

        return "entries/entry-form";
    }


    @GetMapping("/delete/{id}")
    public String deleteEntry(@PathVariable Long id,
                              @RequestParam(name="returnPage", required=true) String returnPage) {

        Entry entry = entryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Entry bulunamadı: " + id));
        Long topicId = entry.getTopic().getId();
        Long categoryId = entry.getTopic().getCategory().getId();

        entryRepository.deleteById(id);

        // predictionDate'i tekrar hesapla ve kaydet
        topicService.updateTopicStatus(topicId);

        return "redirect:/entries/redirect"
                + "?returnPage=" + returnPage
                + (categoryId != null ? "&categoryId=" + categoryId : "")
                + (topicId != null ? "&topicId=" + topicId : "");
    }

    // EntriesController veya uygun controller
    @GetMapping("/{id}/note-full")
    @ResponseBody
    public String getFullNote(@PathVariable Long id) {
        return entryRepository.findById(id)
                .map(e -> e.getNote() != null ? e.getNote().getContent() : "")
                .orElse("");
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
                           @RequestParam(required = false) Long topicId) {

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

        List<TopicEntrySummaryDTO> summaries = entryService.getFilteredEntries(status, weight, startingDate, endingDate);
        model.addAttribute("entries", summaries);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedWeight", weight);
        model.addAttribute("selectedStartDate", startLocalDate.toString());
        model.addAttribute("selectedEndDate", endLocalDate.toString());
        model.addAttribute("zoneId", zoneId);

        return "view-entry-summary/entry-summary";
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
