package turkerozturk.ptt.entity.exception;

import java.text.MessageFormat;

public class TopicNotFoundException extends RuntimeException {

    public TopicNotFoundException (final Long id){
        super(MessageFormat.format("Could not find topic with id: {0}", id));
    }

}
