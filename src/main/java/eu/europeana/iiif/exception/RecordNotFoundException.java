package eu.europeana.iiif.exception;

import eu.europeana.api.commons.error.EuropeanaApiException;
import org.springframework.http.HttpStatus;

/**
 * Exception that is thrown is a record cannot be found (we get 404)
 * @author Patrick Ehlert
 * Created on 26-01-2018
 */
public class RecordNotFoundException extends EuropeanaApiException {

    private static final long serialVersionUID = 1255711334519327124L;

    public RecordNotFoundException(String msg) {
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
        return HttpStatus.NOT_FOUND;
    }
}
