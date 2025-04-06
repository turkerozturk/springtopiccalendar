package turkerozturk.ptt.controller;

import lombok.AllArgsConstructor;
import lombok.Data;
import turkerozturk.ptt.entity.Entry;
import turkerozturk.ptt.entity.Topic;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Pivot veri taşıyıcı:
 *  - dateRange: Filtre tarih aralığındaki günler (LocalDate listesi)
 *  - topicList: Filtre sonucunda gerçekten kullanılan Topic listesi (veya tüm Topic listesi)
 *  - pivotMap:  (topicId, localDate) -> List<Entry>
 *  - topicEntryCount: (topicId) -> o topic’in toplam entry sayısı
 */
@Data
@AllArgsConstructor
public class PivotData {
    private List<LocalDate> dateRange;
    private List<Topic> topicList;
    private Map<Long, Map<LocalDate, List<Entry>>> pivotMap;
    private Map<Long, Integer> topicEntryCount;
}
