package turkerozturk.ptt.service;

import turkerozturk.ptt.entity.Entry;
import turkerozturk.ptt.repository.ActivityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ActivityService {
    @Autowired
    private ActivityRepository activityRepository;

    public List<Entry> getAllActivities() {
        return activityRepository.findAll();
    }

    public Optional<Entry> getActivityById(Long id) {
        return activityRepository.findById(id);
    }

    public Entry saveActivity(Entry entry) {
        return activityRepository.save(entry);
    }

    public void deleteActivity(Long id) {
        activityRepository.deleteById(id);
    }
}