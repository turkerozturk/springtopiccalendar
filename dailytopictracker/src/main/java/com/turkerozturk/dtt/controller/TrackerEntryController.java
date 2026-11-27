package com.turkerozturk.dtt.controller;

import com.turkerozturk.dtt.component.AppTimeZoneProvider;
import com.turkerozturk.dtt.entity.Entry;
import com.turkerozturk.dtt.entity.Note;
import com.turkerozturk.dtt.entity.Topic;
import com.turkerozturk.dtt.repository.EntryRepository;
import com.turkerozturk.dtt.service.EntryService;
import com.turkerozturk.dtt.service.TopicService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Objects;

@RestController
@RequestMapping("/entry-filter/cell")
@RequiredArgsConstructor
public class TrackerEntryController {

    private final EntryRepository entryRepository;
    private final EntryService entryService;
    private final TopicService topicService;
    private final AppTimeZoneProvider timeZoneProvider;

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public CellEntry read(@PathVariable Long id) {
        return CellEntry.from(findEntry(id));
    }

    @PostMapping
    @Transactional
    public CellEntry saveNote(@RequestBody CellNote request) {
        if (request.topicId() == null || request.localDate() == null || request.content() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Topic, date and note are required");
        }

        Entry entry;
        if (request.entryId() == null) {
            if (request.content().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A new note cannot be empty");
            }
            Topic topic = topicService.getTopicById(request.topicId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Topic not found"));
            long dateMillis = request.localDate().atStartOfDay(timeZoneProvider.getZoneId()).toInstant().toEpochMilli();
            if (entryRepository.findByTopicIdAndDateMillisYmd(topic.getId(), dateMillis).isPresent()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "An entry already exists for this date");
            }
            entry = new Entry();
            entry.setTopic(topic);
            entry.setDateMillisYmd(dateMillis);
            entry.setStatus(0);
        } else {
            entry = findCellEntry(request.entryId(), request.topicId(), request.localDate());
        }

        if (entry.getNote() == null) {
            Note note = new Note();
            note.setEntry(entry);
            entry.setNote(note);
        }
        entry.getNote().setContent(request.content());
        Entry saved = entryRepository.save(entry);
        if (request.entryId() == null) {
            topicService.updateTopicStatus(request.topicId());
        }
        return CellEntry.from(saved);
    }

    @DeleteMapping("/{id}")
    @Transactional
    public DeletedCellEntry delete(@PathVariable Long id, @RequestParam Long topicId,
                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate localDate) {
        Topic topic = findCellEntry(id, topicId, localDate).getTopic();
        entryService.deleteEntryById(id);
        topicService.updateTopicStatus(topic.getId());
        return new DeletedCellEntry(topic.getPredictionDate());
    }

    private Entry findCellEntry(Long id, Long topicId, LocalDate localDate) {
        Entry entry = findEntry(id);
        if (!Objects.equals(entry.getTopic().getId(), topicId) || !Objects.equals(entry.getDate(), localDate)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The entry has moved to another cell");
        }
        return entry;
    }

    private Entry findEntry(Long id) {
        return entryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Entry not found"));
    }

    public record CellNote(Long entryId, Long topicId, LocalDate localDate, String content) {
    }

    public record CellEntry(Long id, Integer status, String content, boolean hasNote) {
        private static CellEntry from(Entry entry) {
            String content = entry.getNote() == null ? null : entry.getNote().getContent();
            return new CellEntry(entry.getId(), entry.getStatus(), content == null ? "" : content,
                    content != null && !content.isBlank());
        }
    }

    public record DeletedCellEntry(LocalDate predictionDate) {
    }
}
