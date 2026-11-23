package com.turkerozturk.dtt.bridge;

import java.time.LocalDate;
import org.sqlite.SQLiteErrorCode;
import org.sqlite.SQLiteException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/bridge/v1/topics/{topicId}")
public class TodayTopicController {
    private final TodayTopicService service;

    public TodayTopicController(TodayTopicService service) {
        this.service = service;
    }

    @GetMapping("/today")
    public TodayTopicService.Snapshot read(@PathVariable long topicId) {
        return service.read(topicId);
    }

    @PutMapping("/today")
    public TodayTopicService.Snapshot save(@PathVariable long topicId, @RequestBody TodayTopicService.Update update) {
        return service.save(topicId, update);
    }

    @DeleteMapping("/today")
    public TodayTopicService.Snapshot delete(@PathVariable long topicId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam String revision) {
        return service.delete(topicId, date, revision);
    }

    @GetMapping("/entries/{date}")
    public TodayTopicService.Snapshot readDate(@PathVariable long topicId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.readDate(topicId, date);
    }

    @PutMapping("/entries/{date}")
    public TodayTopicService.Snapshot saveDate(@PathVariable long topicId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestBody TodayTopicService.Update update) {
        return service.saveDate(topicId, date, update);
    }

    @DeleteMapping("/entries/{date}")
    public TodayTopicService.Snapshot deleteDate(@PathVariable long topicId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam String revision) {
        return service.deleteDate(topicId, date, revision);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Void> requestFailure(ResponseStatusException error) {
        return ResponseEntity.status(error.getStatusCode()).build();
    }

    @ExceptionHandler({ServletRequestBindingException.class, TypeMismatchException.class,
            HttpMessageNotReadableException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public void malformedRequest() {}

    @ExceptionHandler({DataIntegrityViolationException.class, PessimisticLockingFailureException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public void conflictingWrite() {}

    @ExceptionHandler(SQLiteException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public void sqliteConflict(SQLiteException error) throws SQLiteException {
        int primary = error.getResultCode().code & 0xff;
        if (primary != SQLiteErrorCode.SQLITE_BUSY.code && primary != SQLiteErrorCode.SQLITE_LOCKED.code
                && error.getResultCode() != SQLiteErrorCode.SQLITE_CONSTRAINT_UNIQUE) {
            throw error;
        }
    }
}
