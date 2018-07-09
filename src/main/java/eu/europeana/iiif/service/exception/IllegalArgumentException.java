package eu.europeana.iiif.service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when we find illegal user input
 * @author Patrick Ehlert
 * Created on 09-07-2018
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class IllegalArgumentException extends IIIFException {

    public IllegalArgumentException(String msg, Throwable t) {
        super(msg, t);
    }

    public IllegalArgumentException(String msg) {
        super(msg);
    }

    /**
     * @return false because we don't want to explicitly log this type of exception
     */
    @Override
    public boolean doLog() {
        return false;
    }
}
