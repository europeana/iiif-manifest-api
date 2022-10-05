package eu.europeana.iiif.exception;

import eu.europeana.api.commons.error.EuropeanaApiException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when the provided format parameter is not a supported value
 */
public class InvalidRequestParamException extends EuropeanaApiException {

    private static final long serialVersionUID = 2048581559311721229L;

    public InvalidRequestParamException(String param, String paramValue) {
        super("Invalid request parameter value. " + param + ":" + paramValue);
    }

    @Override
    public boolean doLog() {
        return false;
    }

    @Override
    public HttpStatus getResponseStatus() {
        return HttpStatus.BAD_REQUEST;
    }
}
