package turkerozturk.ptt.component;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.ZoneId;

@Component
public class AppTimeZoneProvider {

    @Value("${app.timezone:UTC}")
    private String timeZoneId;

    public ZoneId getZoneId() {
        return ZoneId.of(timeZoneId);
    }
}

