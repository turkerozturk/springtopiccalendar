package turkerozturk.ptt.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import turkerozturk.ptt.entity.Entry;

@Repository
public interface EntryRepository extends JpaRepository<Entry, Long> {
}
