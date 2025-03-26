package turkerozturk.ptt.service;

import org.springframework.stereotype.Service;
import turkerozturk.ptt.dto.FilterDto;
import turkerozturk.ptt.entity.Entry;
import turkerozturk.ptt.repository.EntryRepository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

@Service
public class FilterService {

    private final EntryRepository entryRepository;

    public FilterService(EntryRepository entryRepository) {
        this.entryRepository = entryRepository;
    }

    /**
     * Haftanın başlangıç gününü bulur (parametrik).
     * Örnek: Monday ise LocalDate.now() baz alarak pazartesiyi bulur.
     */
    public LocalDate getStartOfWeek(LocalDate current, DayOfWeek startDayOfWeek) {
        // Örnek bir hesaplama
        while (current.getDayOfWeek() != startDayOfWeek) {
            current = current.minusDays(1);
        }
        return current;
    }

    /**
     * Tarih aralığının (startDate, endDate) kaç gün sürdüğünü hesaplar.
     */
    public int getRangeLength(FilterDto filterDto) {
        // (endDate - startDate + 1 gün)
        return (int) (filterDto.getEndDate().toEpochDay() - filterDto.getStartDate().toEpochDay() + 1);
    }

    /**
     * Filtreye uygun Entry kayıtlarını getirir.
     */
    public List<Entry> filterEntries(FilterDto filterDto) {
        // 1) startDate ve endDate'i epoch'a dönüştürelim (eğer dateMillisYmd = günün epoch milisiyse)
        //    Sizin dateMillisYmd, 13 haneli milis (sadece tarih), bu kısım mantığa göre düzenlenmeli.

        long startEpoch = filterDto.getStartDate().atStartOfDay().toEpochSecond(java.time.ZoneOffset.UTC) * 1000;
        long endEpoch = filterDto.getEndDate().atStartOfDay().toEpochSecond(java.time.ZoneOffset.UTC) * 1000;

        // 2) Topic seçilmemişse => tüm topic'ler
        //    Status seçilmemişse => tüm status'ler
        //    Bu logic'i repository veya custom sorgu ile yönetebilirsiniz.
        //    Kısa tutmak adına, pseudo-code gibi gösteriyorum:

        // Örnek: Basit bir custom method,
        // eğer tam esnek isterseniz Specifications veya Criteria API kullanabilirsiniz.
        return entryRepository.findEntriesByFilter(
                filterDto.getTopicIds(),
                startEpoch,
                endEpoch,
                filterDto.getStatuses()
        );
    }
}
