package eu.europeana.iiif.service.exception;

/**
 * Error that is thrown if we found a data inconsistency (e.g. unexpected loops or items not found in the webresource isNextInSequence)
 * @author Patrick Ehlert
 * Created on 08-03-2018
 */
public class DataInconsistentException extends IIIFException {

    public DataInconsistentException(String msg) {
        super(msg);
    }

}
