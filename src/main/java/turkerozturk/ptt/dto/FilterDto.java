package turkerozturk.ptt.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import turkerozturk.ptt.validation.ValidDateRange;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@ValidDateRange
public class FilterDto {

    private Long categoryId;

    // Çoklu topic seçimi için topic id listesi
    private List<Long> topicIds = new ArrayList<>();

    // Çoklu status seçimi
    private List<Integer> statuses = new ArrayList<>();

    // Tarih aralığı
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate; // varsayılan bu haftanın başı

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;   // varsayılan bu haftanın sonu
}
