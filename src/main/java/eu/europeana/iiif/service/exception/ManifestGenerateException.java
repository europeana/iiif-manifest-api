package eu.europeana.iiif.service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * @author Patrick Ehlert
 * Created on 26-01-2018
 */
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class ManifestGenerateException extends IIIFException {

    public ManifestGenerateException(String msg, Throwable t) {
        super(msg, t);
    }

    public ManifestGenerateException(String msg) {
        super(msg);
    }
}
