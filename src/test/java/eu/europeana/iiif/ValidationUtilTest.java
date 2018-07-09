package eu.europeana.iiif;

import eu.europeana.iiif.service.ValidateUtils;
import eu.europeana.iiif.service.exception.IllegalArgumentException;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @author Patrick Ehlert
 * Created on 09-07-2018
 */
public class ValidationUtilTest {

    @Test
    public void testRecordId() throws IllegalArgumentException {
        assertTrue(ValidateUtils.validateRecordIdFormat("/2023006/24062A51_priref_16913"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRecordIdFalse() throws IllegalArgumentException {
        ValidateUtils.validateRecordIdFormat("/2023006/24062A51 priref_16913");
    }

    @Test
    public void testApiKey() throws IllegalArgumentException {
        assertTrue(ValidateUtils.validateWskeyFormat("1aSd456"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testApiKeyFalse() throws IllegalArgumentException {
        assertTrue(ValidateUtils.validateWskeyFormat("1a@d456"));
    }

    @Test
    public void testRecordApiUrl() throws IllegalArgumentException {
        assertTrue(ValidateUtils.validateRecordApiUrlFormat("https://search-api-test.eanadev.org"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRecordApiUrlFalse() throws IllegalArgumentException {
        assertTrue(ValidateUtils.validateRecordApiUrlFormat("https://search-api-test.google.nl"));
    }
}
