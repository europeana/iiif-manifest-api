package eu.europeana.iiif.exception;

import eu.europeana.api.commons.error.EuropeanaApiException;

/**
 * Exception that is thrown is there is a problem parsing or serializing an exception
 * @author Patrick Ehlert
 * Created on 26-01-2018
 */
public class RecordParseException extends EuropeanaApiException {

    private static final long serialVersionUID = 1007865165313316802L;

    public RecordParseException(String msg, Throwable t) {
        super(msg, t);
    }

}
