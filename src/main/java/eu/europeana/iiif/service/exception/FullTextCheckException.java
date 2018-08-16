package eu.europeana.iiif.service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception that is thrown is there is a problem checking if a full-text exists (no 200 or 404 is returned)
 * @author Patrick Ehlert
 * Created on 15-08-2018
 */
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class FullTextCheckException extends IIIFException {

    public FullTextCheckException(String msg, Throwable t) {
        super(msg, t);
    }

    public FullTextCheckException(String msg) {
        super(msg);
    }
}
