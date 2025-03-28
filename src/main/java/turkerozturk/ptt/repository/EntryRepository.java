package turkerozturk.ptt.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import turkerozturk.ptt.entity.Entry;

import java.util.Optional;

@Repository
public interface EntryRepository extends JpaRepository<Entry, Long>, EntryRepositoryCustom {

    Optional<Entry> findByTopicIdAndDateMillisYmd(Long topicId, Long dateMillisYmd);

    // Veya eğer birden fazla varsa:
    // List<Entry> findAllByTopicIdAndDateMillisYmd(Long topicId, Long dateMillisYmd);
}
