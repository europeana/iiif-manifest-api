package eu.europeana.iiif.exception;

import eu.europeana.api.commons.error.EuropeanaGlobalExceptionHandler;
import org.springframework.web.bind.annotation.ControllerAdvice;

/**
 * Global exception handler that catches all errors and logs the interesting ones
 * @author Patrick Ehlert
 * Created on 20-02-2018
 */
@ControllerAdvice
public class GlobalExceptionHandler extends EuropeanaGlobalExceptionHandler {



}
