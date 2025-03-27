package turkerozturk.ptt.controller.web;


import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import turkerozturk.ptt.entity.Entry;
import turkerozturk.ptt.entity.Note;
import turkerozturk.ptt.entity.Topic;
import turkerozturk.ptt.helper.DateUtils;
import turkerozturk.ptt.repository.EntryRepository;
import turkerozturk.ptt.repository.TopicRepository;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Controller
@RequestMapping("/entries")
public class EntryController {

    private final EntryRepository entryRepository;
    private final TopicRepository topicRepository;

    public EntryController(EntryRepository entryRepository, TopicRepository topicRepository) {
        this.entryRepository = entryRepository;
        this.topicRepository = topicRepository;
    }

    @GetMapping
    public String listEntries(Model model) {
        model.addAttribute("entries", entryRepository.findAll());
        return "entries/list";
    }

    @GetMapping("/new")
    public String showCreateForm(
            @RequestParam(name="topicId", required=false) Long topicId,
            @RequestParam(name="dateYmd", required=false) String dateString,
            Model model) {

        // System.out.println("dateYmd: " + dateString );
        Entry entry = new Entry();
        entry.setNote(new Note());

        // Topic set
        if (topicId != null) {
            Topic topic = topicRepository.findById(topicId).orElse(null);
            entry.setTopic(topic);
        }

        if (dateString != null) {
            // "2025-03-27" gibi bir tarih formatını LocalDate'e parse edip epoch milise çeviriyoruz
            LocalDate localDate = LocalDate.parse(dateString, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            long epochMillis = localDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            // System.out.println(dateString + " " + localDate + " " + epochMillis);
            entry.setDateMillisYmd(epochMillis);
        } else {
            // Aksi halde bugünün tarih milisini varsayılan yap
            entry.setDateMillisYmd(DateUtils.getEpochMillisToday());
        }

        model.addAttribute("entry", entry);
        model.addAttribute("topics", topicRepository.findAll());
        return "entries/form";
    }



    @PostMapping("/save")
    public String saveEntry(@ModelAttribute("entry") Entry formEntry) {
        // formEntry, thymeleaf’ten gelen Entry objesi. İçinde note de gelebilir.
        if (formEntry.getId() == null) {
            // 1) YENİ KAYIT
            // Eğer note boş değilse two-way ilişkiyi kur
            if (formEntry.getNote() != null) {
                formEntry.getNote().setEntry(formEntry);
            }
            entryRepository.save(formEntry);
        } else {
            // 2) GÜNCELLEME
            // DB’den ilgili kaydı çekiyoruz
            Entry dbEntry = entryRepository.findById(formEntry.getId())
                    .orElseThrow(() -> new RuntimeException("Entry bulunamadı: " + formEntry.getId()));

            // GÜNCELLENECEK ALANLARI SETLE
            dbEntry.setTopic(formEntry.getTopic());
            dbEntry.setDateMillisYmd(formEntry.getDateMillisYmd());
            dbEntry.setStatus(formEntry.getStatus());

            // Note güncelle
            if (dbEntry.getNote() == null) {
                // DB’de note yoksa yeni oluştur
                Note newNote = new Note();
                newNote.setEntry(dbEntry);
                dbEntry.setNote(newNote);
            }
            // content'i formdan gelenle güncelle
            dbEntry.getNote().setContent(formEntry.getNote() != null ? formEntry.getNote().getContent() : "");

            entryRepository.save(dbEntry);
        }
        return "redirect:/entries";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Entry entry = entryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Entry bulunamadı: " + id));

        // Not nesnesi null ise formda null dönmemesi için burda oluşturabilirsiniz.
        if (entry.getNote() == null) {
            Note note = new Note();
            note.setEntry(entry);
            entry.setNote(note);
        }

        model.addAttribute("entry", entry);
        model.addAttribute("topics", topicRepository.findAll());
        return "entries/form";
    }

    @GetMapping("/delete/{id}")
    public String deleteEntry(@PathVariable Long id) {
        entryRepository.deleteById(id);
        return "redirect:/entries";
    }
}
