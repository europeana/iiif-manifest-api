package eu.europeana.iiif.exception;

import eu.europeana.api.commons.error.EuropeanaApiException;
import org.springframework.http.HttpStatus;

/**
 * Exception that is thrown when the provided API key is not valid
 * @author Patrick Ehlert
 * Created on 26-01-2018
 */
public class InvalidApiKeyException extends EuropeanaApiException {

    private static final long serialVersionUID = 3639614753377679354L;

    public InvalidApiKeyException(String msg) {
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
        return HttpStatus.UNAUTHORIZED;
    }
}
