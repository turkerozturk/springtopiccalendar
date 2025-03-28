package turkerozturk.ptt.entity;


import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(
        name = "entries",
        uniqueConstraints = {
                @UniqueConstraint(name = "UniqueTopicAndDate", columnNames = { "topic_id", "date_millis_ymd" })
        }
)
public class Entry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id", nullable = true)
    private Topic topic;

    // 13 haneli epoch time (sadece tarih, saat bilgileri 0)
    @Column(name = "date_millis_ymd")
    private Long dateMillisYmd;

    // status (0=not marked, 1=done, 2=warning)
    private Integer status;

    @OneToOne(mappedBy = "entry", cascade = CascadeType.ALL, orphanRemoval = true)
    @PrimaryKeyJoinColumn
    private Note note;

    public Entry() {
    }



}


