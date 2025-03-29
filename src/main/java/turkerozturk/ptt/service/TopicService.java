package turkerozturk.ptt.service;


import turkerozturk.ptt.entity.Topic;
import turkerozturk.ptt.repository.TopicRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TopicService {
    @Autowired
    private TopicRepository topicRepository;

    public List<Topic> getAllTopics() {
        return topicRepository.findAll();
    }

    public Optional<Topic> getTopicById(Long id) {
        return topicRepository.findById(id);
    }

    public Topic saveTopic(Topic topic) {
        return topicRepository.save(topic);
    }

    public void deleteTopic(Long id) {
        Optional<Topic> optionalTopic = topicRepository.findById(id);
        if (optionalTopic.isPresent()) {
            Topic topic = optionalTopic.get();
            if (topic.getActivities() != null && !topic.getActivities().isEmpty()) {
                throw new IllegalStateException("Cannot delete topic with associated entries.");
            }
            topicRepository.delete(topic);
        }
    }

}
