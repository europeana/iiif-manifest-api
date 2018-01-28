package eu.europeana.iiif.service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception that is thrown when the provided API key is not valid
 * @author Patrick Ehlert
 * Created on 26-01-2018
 */
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class InvalidApiKeyException extends IIIFException {

    public InvalidApiKeyException(String msg, Throwable t) {
        super(msg, t);
    }

    public InvalidApiKeyException(String msg) {
        super(msg);
    }
}
