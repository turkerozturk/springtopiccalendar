package com.turkerozturk.dtt.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.turkerozturk.dtt.entity.Entry;
import com.turkerozturk.dtt.entity.Topic;
import com.turkerozturk.dtt.repository.EntryRepository;

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

    public List<Entry> findDonesByCategory(Long categoryId, Long dateMillisYmd) {

        return entryRepository.findByCategoryIdAndStatusOfDoneEntriesWithDateInterval(categoryId, dateMillisYmd);
    }

    public List<Entry> findByTopicIdAndDateInterval(Long topicId, long startDateMillis, long endDateMillis) {
        return entryRepository.findByTopicIdAndDateInterval(topicId, startDateMillis, endDateMillis);
    }


}
