package eu.europeana.iiif.exception;

import eu.europeana.api.commons.error.EuropeanaApiException;

/**
 * Exception that is thrown is there is a problem retrieving a record (other than a 404)
 * @author Patrick Ehlert
 * Created on 26-01-2018
 */
public class RecordRetrieveException extends EuropeanaApiException {

    private static final long serialVersionUID = 4916818362571684986L;

    public RecordRetrieveException(String msg, Throwable t) {
        super(msg, t);
    }

    public RecordRetrieveException(String msg) {
        super(msg);
    }
}
