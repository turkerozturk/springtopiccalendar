package turkerozturk.ptt.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "notes")
public class Note {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String content;

    @OneToOne(optional = false)
    @JoinColumn(name = "entry_id", nullable = false, unique = true)
    private Entry entry;

    public Note() {
    }

    public Note(String content) {
        this.content = content;
    }



}

