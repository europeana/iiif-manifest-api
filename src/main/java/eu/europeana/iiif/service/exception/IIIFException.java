package eu.europeana.iiif.service.exception;

/**
 * Base error class for this application
 * @author Patrick Ehlert
 * Created on 26-01-2018
 */
public class IIIFException extends Exception {

    public IIIFException(String msg, Throwable t) {
        super(msg, t);
    }

    public IIIFException(String msg) {
        super(msg);
    }

}
