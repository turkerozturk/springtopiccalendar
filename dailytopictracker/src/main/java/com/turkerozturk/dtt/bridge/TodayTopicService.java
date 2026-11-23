package com.turkerozturk.dtt.bridge;

import com.turkerozturk.dtt.component.AppTimeZoneProvider;
import com.turkerozturk.dtt.entity.Entry;
import com.turkerozturk.dtt.entity.Note;
import com.turkerozturk.dtt.entity.Topic;
import com.turkerozturk.dtt.repository.EntryRepository;
import com.turkerozturk.dtt.repository.TopicRepository;
import com.turkerozturk.dtt.service.EntryService;
import com.turkerozturk.dtt.service.TopicService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TodayTopicService {
    private final TopicRepository topics;
    private final EntryRepository entries;
    private final AppTimeZoneProvider timeZone;
    private final TopicService topicService;
    private final EntryService entryService;

    public TodayTopicService(TopicRepository topics, EntryRepository entries, AppTimeZoneProvider timeZone,
                             TopicService topicService, EntryService entryService) {
        this.topics = topics;
        this.entries = entries;
        this.timeZone = timeZone;
        this.topicService = topicService;
        this.entryService = entryService;
    }

    public record Snapshot(String topicId, String topicName, LocalDate date, TodayEntry entry, String revision) {}
    public record TodayEntry(Integer status, String note) {}
    public record Update(LocalDate date, String revision, Integer status, String note) {}

    @Transactional(readOnly = true)
    public Snapshot read(long topicId) {
        LocalDate date = LocalDate.now(timeZone.getZoneId());
        Topic topic = topic(topicId);
        return snapshot(topic, date, findEntry(topicId, date));
    }

    @Transactional
    public Snapshot save(long topicId, Update update) {
        if (update == null || update.status() == null || update.status() < 0 || update.status() > 2
                || update.note() == null || update.note().length() > 65536) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        Topic topic = topic(topicId);
        LocalDate date = requireToday(update.date());
        Entry entry = findEntry(topicId, date);
        checkRevision(entry, update.revision());
        if (entry == null) {
            entry = new Entry();
            entry.setTopic(topic);
            entry.setDateMillisYmd(midnight(date));
            topic.getActivities().add(entry);
        }
        if (entry.getNote() == null) {
            Note note = new Note();
            note.setEntry(entry);
            entry.setNote(note);
        }
        entry.getNote().setContent(update.note());
        entry.setStatus(update.status());
        entries.saveAndFlush(entry);
        topicService.updateTopicStatus(topicId);
        return snapshot(topic, date, entry);
    }

    @Transactional
    public Snapshot delete(long topicId, LocalDate requestedDate, String revision) {
        Topic topic = topic(topicId);
        LocalDate date = requireToday(requestedDate);
        Entry entry = findEntry(topicId, date);
        checkRevision(entry, revision);
        if (entry != null) {
            entryService.deleteEntryById(entry.getId());
            entries.flush();
            topicService.updateTopicStatus(topicId);
        }
        return snapshot(topic, date, null);
    }

    private Topic topic(long topicId) {
        return topics.findById(topicId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private Entry findEntry(long topicId, LocalDate date) {
        return entries.findByTopicIdAndDateMillisYmd(topicId, midnight(date)).orElse(null);
    }

    private long midnight(LocalDate date) {
        return date.atStartOfDay(timeZone.getZoneId()).toInstant().toEpochMilli();
    }

    private LocalDate requireToday(LocalDate requested) {
        LocalDate date = LocalDate.now(timeZone.getZoneId());
        if (!date.equals(requested)) throw new ResponseStatusException(HttpStatus.CONFLICT);
        return date;
    }

    private static Snapshot snapshot(Topic topic, LocalDate date, Entry entry) {
        TodayEntry today = entry == null ? null : new TodayEntry(entry.getStatus(), note(entry));
        return new Snapshot(topic.getId().toString(), topic.getName(), date, today, revision(entry));
    }

    private static String note(Entry entry) {
        return entry.getNote() == null ? "" : Objects.toString(entry.getNote().getContent(), "");
    }

    private static void checkRevision(Entry entry, String expected) {
        if (!revision(entry).equals(expected)) throw new ResponseStatusException(HttpStatus.CONFLICT);
    }

    private static String revision(Entry entry) {
        if (entry == null) return "new";
        String value = entry.getId() + ":" + entry.getStatus() + ":" + note(entry);
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
