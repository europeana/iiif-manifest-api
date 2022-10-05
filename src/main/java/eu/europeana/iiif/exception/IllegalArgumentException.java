package eu.europeana.iiif.exception;

import eu.europeana.api.commons.error.EuropeanaApiException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when we find illegal user input
 * @author Patrick Ehlert
 * Created on 09-07-2018
 */
public class IllegalArgumentException extends EuropeanaApiException {

    private static final long serialVersionUID = 6920934255738422247L;

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

    @Override
    public HttpStatus getResponseStatus() {
        return HttpStatus.BAD_REQUEST;
    }
}
