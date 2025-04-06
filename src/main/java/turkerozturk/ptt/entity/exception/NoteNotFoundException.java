package turkerozturk.ptt.entity.exception;

import java.text.MessageFormat;

public class NoteNotFoundException extends RuntimeException {

    public NoteNotFoundException(final Long id) {
        super(MessageFormat.format("Could not find note with id: {0}", id));
    }

}