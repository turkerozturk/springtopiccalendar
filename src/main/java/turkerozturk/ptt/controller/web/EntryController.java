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
import turkerozturk.ptt.entity.Category;
import turkerozturk.ptt.entity.Entry;
import turkerozturk.ptt.entity.Note;
import turkerozturk.ptt.entity.Topic;
import turkerozturk.ptt.helper.DateUtils;
import turkerozturk.ptt.repository.CategoryRepository;
import turkerozturk.ptt.repository.EntryRepository;
import turkerozturk.ptt.repository.TopicRepository;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;


@Controller
@RequestMapping("/entries")
public class EntryController {


    private final AppTimeZoneProvider timeZoneProvider;
    private final EntryRepository entryRepository;
    private final TopicRepository topicRepository;

    private final CategoryRepository categoryRepository;


    public EntryController(AppTimeZoneProvider timeZoneProvider, EntryRepository entryRepository, TopicRepository topicRepository, CategoryRepository categoryRepository) {
        this.timeZoneProvider = timeZoneProvider;
        this.entryRepository = entryRepository;
        this.topicRepository = topicRepository;
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




        return "entries/list";
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
                ? topicRepository.findByCategoryId(categoryId)
                : topicRepository.findAll();
        model.addAttribute("entry", entry);
        model.addAttribute("topics", topics);
        //System.out.println("return page: " + returnPage);
        model.addAttribute("returnPage", returnPage);
        model.addAttribute("categoryId", categoryId);

        return "entries/form";
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
        List<Topic> topics = topicRepository.findByCategoryId(categoryId);



        // Entity -> DTO dönüştürme
        return topics.stream()
                .map(t -> new TopicDto(t.getId(), t.getName()))
                .toList();
    }


    @PostMapping("/save")
    public String saveEntry(@ModelAttribute("entry") Entry formEntry,
                            Model model,
                            HttpSession session,
                            HttpServletRequest request) {



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
                model.addAttribute("errorMessage", "Bu topic için bu tarihte zaten bir kayıt var!");
                model.addAttribute("entry", formEntry);
                model.addAttribute("topics", topicRepository.findAll());
                //return "entries/form";
            }

            // b) Not'u da two-way ilişkiyle bağla
            if (formEntry.getNote() != null) {
                formEntry.getNote().setEntry(formEntry);
            }

            // c) Artık güvenle kaydedebiliriz
            entryRepository.save(formEntry);

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
                model.addAttribute("errorMessage", "Bu topic için bu tarihte zaten bir kayıt var!");
                model.addAttribute("entry", formEntry);
                model.addAttribute("topics", topicRepository.findAll());
                //return "entries/form";
            }

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
        }

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




        // Hangi sayfadan gelindiğini kontrol ediyoruz.
        String returnPage = request.getParameter("returnPage");
        if (returnPage != null) {
            switch (returnPage) {
                case "topics":
                    return "redirect:/topics?categoryId=" + categoryId;
                case "pivottable":
                    return "redirect:/entry-filter/return?categoryId=" + categoryId;
                case "entries":
                    return "redirect:/entries?topicId=" + topicId;
                // Eğer ileride farklı sayfalardan gelme ihtimali varsa
                default:
                    return "redirect:/" + returnPage + "?categoryId=" + categoryId;
            }
        }

        // (1) Session'da filtre bilgisi var mı?
        FilterDto currentFilter = (FilterDto) session.getAttribute("currentFilterDto");
        if (currentFilter != null) {
            // Kullanıcı pivot'tan geldiyse oraya geri dönmek istiyoruz.
            // Ama pivot'ta tabloyu yeniden oluşturmak için applyFilter benzeri bir işlem yapmamız lazım.

            // Yöntem A: Sadece redirect "/entry-filter/return" gibi bir endpoint açar, orada
            // filter'ı tekrar applyFilter benzeri kodla çalıştırır.
            return "redirect:/entry-filter/return?categoryId=" + categoryId;
            //return "entry-filter/return";

            // Yöntem B: Direct "/entry-filter/form" a gider, ama orada filter'ı tekrar uygular.
            // => Bunu bir endpoint'te handle etmek daha temiz olur.
        }

        // (2) Session'da filtre yoksa normal entries listesine
        // Kayıt başarılı, listeye dön.
        return "redirect:/entries?categoryId=" + categoryId;
    }


    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id,
                               @RequestParam(name="returnPage", required=false) String returnPage,
                               @RequestParam(name="categoryId", required=false) Long categoryId,
                               Model model) {
        Entry entry = entryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Entry bulunamadı: " + id));

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
                ? topicRepository.findByCategoryId(categoryId)
                : topicRepository.findAll();
        model.addAttribute("entry", entry);
        model.addAttribute("entry", entry);
        model.addAttribute("topics", topics);
        model.addAttribute("returnPage", returnPage);
        model.addAttribute("categoryId", categoryId);
        return "entries/form";
    }

    @GetMapping("/delete/{id}")
    public String deleteEntry(@PathVariable Long id, HttpSession session) {
        entryRepository.deleteById(id);

        // Session'da currentFilterDto var mı, kontrol et
        FilterDto filterDto = (FilterDto) session.getAttribute("currentFilterDto");
        if (filterDto != null) {
            // Pivot'tan gelindiğini varsay => tekrar pivot görünümüne dön
            return "redirect:/entry-filter/return";
        } else {
            // Pivot bilgisi yok, normal entries listesine dön
            return "redirect:/entries";
        }
    }

}
