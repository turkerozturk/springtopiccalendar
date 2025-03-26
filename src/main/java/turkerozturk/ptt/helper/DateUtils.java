package turkerozturk.ptt.helper;

import java.time.LocalDate;
import java.time.ZoneId;

public class DateUtils {

    private static String timeZone = "UTC";

    public static long getEpochMillis(int year, int month, int day) {
        // Create a LocalDate instance
        LocalDate date = LocalDate.of(year, month, day);

        // Convert to epoch milliseconds (midnight in UTC)
        return date.atStartOfDay(ZoneId.of(timeZone)).toInstant().toEpochMilli();
    }

    public static long getEpochMillisToday() {
        // Create a LocalDate instance
        LocalDate date = LocalDate.now();

        // Convert to epoch milliseconds (midnight in UTC)
        return date.atStartOfDay(ZoneId.of(timeZone)).toInstant().toEpochMilli();
    }

    public static void main(String[] args) {
        // Example usage
        long epochMillis = getEpochMillis(2025, 1, 30);
        System.out.println("Epoch Milliseconds: " + epochMillis);
    }
}
