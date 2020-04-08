package eu.europeana.iiif.service;

import eu.europeana.iiif.service.exception.IllegalArgumentException;
import org.springframework.util.StringUtils;

import java.net.URL;
import java.util.regex.Pattern;

/**
 * Validation helper functions to prevent injecting of characters into a request
 * @author Patrick Ehlert
 * Created on 09-07-2018
 */
public final class ValidateUtils {

    private static final Pattern RECORD_ID = Pattern.compile("^/[a-zA-Z0-9_]*/[a-zA-Z0-9_]*$");

    private static final Pattern WSKEY = Pattern.compile("^[a-zA-Z0-9]*$");

    private static final Pattern API_BASEURL = Pattern.compile("^(https?://)[a-zA-Z0-9_-]+\\.(eanadev.org|europeana.eu)$");

    private static final Pattern EUROPEANA_URL = Pattern.compile("^(https?://)[a-zA-Z0-9_-]+\\.(eanadev.org|europeana.eu)/(.+)$");


    private ValidateUtils() {
        // empty constructor to prevent initialization
    }

    /**
     * Checks if the provided recordId has the correct format (no illegal characters that may mess up the query)
     * @param europeanaId string that should consist of "/<collectionId>/<itemId>"
     * @return true if it has a valid format
     * @throws IllegalArgumentException thrown when the provided recordId doesn't adhere to the expected format
     */
    public static final boolean validateRecordIdFormat(String europeanaId) throws IllegalArgumentException {
        if (!RECORD_ID.matcher(europeanaId).matches()) {
            throw new IllegalArgumentException("Illegal recordId "+ europeanaId);
        }
        return true;
    }

    /**
     * This checks if the provided API key has the correct format (no illegal characters that may mess up the query)
     * WARNING! This does not check if the API itself is a valid key!
     * @param wsKey string that should consist of characters and numbers only
     * @return true if it has a valid format
     * @throws IllegalArgumentException thrown when the provided recordId doesn't adhere to the expected format
     */
    public static final boolean validateWskeyFormat(String wsKey) throws IllegalArgumentException {
        if (StringUtils.isEmpty(wsKey)) {
            throw new IllegalArgumentException("Empty API key");
        }
        if (!WSKEY.matcher(wsKey).matches()) {
            throw new IllegalArgumentException("Illegal API key "+ wsKey);
        }
        return true;
    }

    /**
     * Check if the provided API url is a valid Europeana API url (*.eanadev.org or *.europeana.eu)
     * @param apiUrl URL to a particular Europeana API
     * @return true if it has a valid format
     * @throws IllegalArgumentException thrown when the provided string doesn't adhere to the expected format
     */
    public static final boolean validateApiUrlFormat(URL apiUrl) throws IllegalArgumentException {
        if (!API_BASEURL.matcher(apiUrl.toString()).matches()) {
            throw new IllegalArgumentException("Illegal API url "+ apiUrl);
        }
        return true;
    }

    /**
     * This check is similar to validateApiUrlFormat but doesn't throw an error
     * @param url
     * @return true if the provided String is a valid Europeana API url (*.eanadev.org or *.europeana.eu), otherwise false
     */
    public static final boolean isEuropeanaUrl(String url) {
        return EUROPEANA_URL.matcher(url).matches();
    }

}
