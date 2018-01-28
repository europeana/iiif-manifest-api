package eu.europeana.iiif.service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception that is thrown is there is a problem parsing or serializing an exception
 * @author Patrick Ehlert
 * Created on 26-01-2018
 */
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class RecordParseException extends IIIFException {

    public RecordParseException(String msg, Throwable t) {
        super(msg, t);
    }

    public RecordParseException(String msg) {
        super(msg);
    }
}
