package turkerozturk.ptt.controller;

import turkerozturk.ptt.entity.Entry;
import turkerozturk.ptt.service.ActivityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/activities")
public class ActivityController {
    @Autowired
    private ActivityService activityService;

    @GetMapping
    public List<Entry> getAllActivities() {
        return activityService.getAllActivities();
    }

    @GetMapping("/{id}")
    public Optional<Entry> getActivityById(@PathVariable Long id) {
        return activityService.getActivityById(id);
    }

    @PostMapping
    public Entry createActivity(@RequestBody Entry entry) {
        return activityService.saveActivity(entry);
    }

    @DeleteMapping("/{id}")
    public void deleteActivity(@PathVariable Long id) {
        activityService.deleteActivity(id);
    }
}


