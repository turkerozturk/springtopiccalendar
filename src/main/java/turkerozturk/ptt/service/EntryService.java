package turkerozturk.ptt.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import turkerozturk.ptt.entity.Entry;
import turkerozturk.ptt.entity.Topic;
import turkerozturk.ptt.repository.EntryRepository;

import java.util.List;

@Service
public class EntryService {

    private final EntryRepository entryRepository;

    public EntryService(EntryRepository entryRepository) {
        this.entryRepository = entryRepository;
    }

    @Transactional
    public void deleteEntryById(Long id) {
        Entry entry = entryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Entry bulunamadı: " + id));

        Topic topic = entry.getTopic();
        topic.getActivities().remove(entry); // ilişkiyi kopar

        entryRepository.deleteById(id);
    }

    public List<Entry> findWarningsByCategory(Long categoryId) {

        return entryRepository.findByCategoryIdAndStatusOfWarningEntries(categoryId, 2);
    }

    public List<Entry> findNeutralsByCategory(Long categoryId, Long dateMillisYmd) {

        return entryRepository.findByCategoryIdAndStatusOfNeutralEntriesWithDateInterval(categoryId, dateMillisYmd);
    }


}
