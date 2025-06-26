package turkerozturk.ptt.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CategoryEntryStatsDto {
    private Long categoryId;
    private String categoryName;
    private long warningCount;      // status = 2
    private long futureNotMarked;   // status = 0 and date >= today
    private long todayDone;         // status = 1 and date == today
    private long predictionCount;

    // constructor, getters, setters
}

