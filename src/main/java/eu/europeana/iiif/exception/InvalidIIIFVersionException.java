package eu.europeana.iiif.exception;

import eu.europeana.api.commons.error.EuropeanaApiException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when the provided format parameter or profile header is not a supported value
 */
public class InvalidIIIFVersionException extends EuropeanaApiException {

    private static final long serialVersionUID = -3607803722931838987L;

    /**
     * Initialise a new exception for which there is no root cause
     *
     * @param msg error message
     */
    public InvalidIIIFVersionException(String msg) {
        super(msg);
    }

    /**
     * We don't want to log the stack trace for this exception
     *
     * @return false
     */
    @Override
    public boolean doLog() {
        return false;
    }

    /**
     * We don't want to log the stack trace for this exception
     *
     * @return false
     */
    @Override
    public boolean doLogStacktrace() {
        return true;
    }

    @Override
    public HttpStatus getResponseStatus() {
        return HttpStatus.BAD_REQUEST;
    }
}
