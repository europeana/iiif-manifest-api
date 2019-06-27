package eu.europeana.iiif.service.exception;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Global exception handler that catches all errors and logs the interesting ones
 * @author Patrick Ehlert
 * Created on 20-02-2018
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG = LogManager.getLogger(GlobalExceptionHandler.class);

    /**
     * Checks if we should log an error and rethrows it
     * @param e caught exception
     * @throws IIIFException rethrown exception
     */
    @ExceptionHandler(IIIFException.class)
    public void handleIiifException(IIIFException e) throws IIIFException {
        if (e.doLog()) {
            LOG.error("Caught exception", e);
        }
        throw e;
    }
}
