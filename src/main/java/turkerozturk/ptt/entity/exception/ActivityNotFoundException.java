package turkerozturk.ptt.entity.exception;

import java.text.MessageFormat;

public class ActivityNotFoundException extends RuntimeException {

    public ActivityNotFoundException(final Long id) {
        super(MessageFormat.format("Could not find activity with id: {0}", id));
    }

}
