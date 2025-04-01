package turkerozturk.ptt.helper;

import org.springframework.beans.factory.annotation.Autowired;
import turkerozturk.ptt.component.AppTimeZoneProvider;

import java.time.LocalDate;
import java.time.ZoneId;

public class DateUtils {

    @Autowired
    private AppTimeZoneProvider timeZoneProvider;

   // private static String timeZone = "UTC";
    ZoneId zone = timeZoneProvider.getZoneId(); // olusturdugumuz component. application.properties'den zone ceker.
    public long getEpochMillis(int year, int month, int day) {
        // Create a LocalDate instance
        LocalDate date = LocalDate.of(year, month, day);

        // Convert to epoch milliseconds (midnight in UTC)
        return date.atStartOfDay(zone).toInstant().toEpochMilli();
    }

    public long getEpochMillisToday() {
        // Create a LocalDate instance
        LocalDate date = LocalDate.now();

        // Convert to epoch milliseconds (midnight in UTC)
        return date.atStartOfDay(zone).toInstant().toEpochMilli();
    }


}
