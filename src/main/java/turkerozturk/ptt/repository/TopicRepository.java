package turkerozturk.ptt.repository;

import turkerozturk.ptt.entity.Topic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TopicRepository extends JpaRepository<Topic, Long> {

    List<Topic> findByCategoryId(Long categoryId);
}
