package eu.europeana.iiif.service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception that is thrown is there is a problem retrieving a record (other than a 404)
 * @author Patrick Ehlert
 * Created on 26-01-2018
 */
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class RecordRetrieveException extends IIIFException {

    public RecordRetrieveException(String msg, Throwable t) {
        super(msg, t);
    }

    public RecordRetrieveException(String msg) {
        super(msg);
    }
}
