package eu.europeana.iiif.service;

import eu.europeana.iiif.service.exception.IllegalArgumentException;
import org.apache.commons.logging.LogFactory;
import org.apache.logging.log4j.LogManager;

import java.util.regex.Pattern;

/**
 * Validation helper functions to prevent injecting of characters into a request
 * @author Patrick Ehlert
 * Created on 09-07-2018
 */
public class ValidateUtils {

    private ValidateUtils() {
        // empty constructor to prevent initialization
    }

    public static final Pattern RECORD_ID = Pattern.compile("^/[a-zA-Z0-9_]*/[a-zA-Z0-9_]*$");

    public static final Pattern WSKEY = Pattern.compile("^[a-zA-Z0-9]*$");

    public static final Pattern RECORD_API_URL = Pattern.compile("^(https?://)[a-zA-Z0-9_-]+\\.(eanadev.org|europeana.eu)$");

    /**
     * Checks if the provided recordId has the correct format (no illegal characters that may mess up the query)
     * @param recordId
     * @return
     */
    public static final boolean validateRecordIdFormat(String recordId) throws IllegalArgumentException {
        if (!RECORD_ID.matcher(recordId).matches()) {
            throw new IllegalArgumentException("Illegal recordId "+ recordId);
        }
        return true;
    }

    /**
     * This checks if the provided API key has the correct format (no illegal characters that may mess up the query)
     * WARNING! This does not check if the API itself is a valid key!
     * @param wsKey
     * @return
     */
    public static final boolean validateWskeyFormat(String wsKey) throws IllegalArgumentException {
        if (!WSKEY.matcher(wsKey).matches()) {
            throw new IllegalArgumentException("Illegal API key "+ wsKey);
        }
        return true;
    }

    /**
     * This checks if the provided API key has the correct format (no illegal characters that may mess up the query)
     * WARNING! This does not check if the API itself is a valid key!
     * @param recordApiUrl
     * @return
     */
    public static final boolean validateRecordApiUrlFormat(String recordApiUrl) throws IllegalArgumentException {
        if (!RECORD_API_URL.matcher(recordApiUrl).matches()) {
            LogManager.getLogger(ValidateUtils.class).error("validate recordAPI");
            throw new IllegalArgumentException("Illegal Record API url "+ recordApiUrl);
        }
        return true;
    }



}
