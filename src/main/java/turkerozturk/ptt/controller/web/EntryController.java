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

import java.util.Optional;


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
    public String saveEntry(@ModelAttribute("entry") Entry formEntry,
                            Model model) {

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
                return "entries/form";
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
                return "entries/form";
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

        // Kayıt başarılı, listeye dön.
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
