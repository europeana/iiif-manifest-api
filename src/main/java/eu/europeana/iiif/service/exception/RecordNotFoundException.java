package eu.europeana.iiif.service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception that is thrown is a record cannot be found (we get 404)
 * @author Patrick Ehlert
 * Created on 26-01-2018
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class RecordNotFoundException extends IIIFException {

    public RecordNotFoundException(String msg, Throwable t) {
        super(msg, t);
    }

    public RecordNotFoundException(String msg) {
        super(msg);
    }

}
