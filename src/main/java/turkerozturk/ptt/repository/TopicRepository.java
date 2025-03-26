package turkerozturk.ptt.repository;

import turkerozturk.ptt.entity.Topic;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TopicRepository extends JpaRepository<Topic, Long> {
}
