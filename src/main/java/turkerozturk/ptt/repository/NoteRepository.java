package turkerozturk.ptt.repository;

import turkerozturk.ptt.entity.Note;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoteRepository extends JpaRepository<Note, Long> {
}
