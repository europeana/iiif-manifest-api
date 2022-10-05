package eu.europeana.iiif.exception;

import eu.europeana.api.commons.error.EuropeanaApiException;

/**
 * Exception that is thrown is there is a problem checking if a full-text exists (no 200 or 404 is returned)
 * @author Patrick Ehlert
 * Created on 15-08-2018
 */
public class FullTextCheckException extends EuropeanaApiException {

    private static final long serialVersionUID = 6496277864645695187L;

    public FullTextCheckException(String msg, Throwable t) {
        super(msg, t);
    }

}
