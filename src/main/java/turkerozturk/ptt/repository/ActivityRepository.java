package turkerozturk.ptt.repository;

import turkerozturk.ptt.entity.Entry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityRepository extends JpaRepository<Entry, Long> {
}
