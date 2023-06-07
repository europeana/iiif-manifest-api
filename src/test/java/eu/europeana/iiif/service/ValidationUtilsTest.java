package eu.europeana.iiif.service;

import eu.europeana.iiif.exception.IllegalArgumentException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author Patrick Ehlert
 * Created on 09-07-2018
 */
public class ValidationUtilsTest {

    @Test
    public void testRecordId() throws IllegalArgumentException {
        Assertions.assertTrue(ValidateUtils.validateRecordIdFormat("/2023006/24062A51_priref_16913"));
    }

    @Test
    public void testRecordIdFalse() {
        Assertions.assertThrows(IllegalArgumentException.class, () ->
                ValidateUtils.validateRecordIdFormat("/2023006/24062A51 priref_16913"));
    }

    @Test
    public void testApiKey() throws IllegalArgumentException {
        Assertions.assertTrue(ValidateUtils.validateWskeyFormat("1aSd456"));
    }

    @Test
    public void testApiKeyFalse() {
        Assertions.assertThrows(IllegalArgumentException.class, () ->
                Assertions.assertTrue(ValidateUtils.validateWskeyFormat("1a@d456")));
    }

    @Test
    public void testRecordApiUrl() throws IllegalArgumentException, MalformedURLException {
        Assertions.assertTrue(ValidateUtils.validateApiUrlFormat(new URL("https://srch-api-test.eanadev.org")));
        Assertions.assertTrue(ValidateUtils.validateApiUrlFormat(new URL("https://srch-api.test.eanadev.org")));
        Assertions.assertTrue(ValidateUtils.validateApiUrlFormat(new URL("http://srch-api.acceptance.eanadev.org")));
    }

    @Test
    public void testFullTextApiUrl() throws IllegalArgumentException, MalformedURLException {
        Assertions.assertTrue(ValidateUtils.validateApiUrlFormat(new URL("https://ft-api-test.eanadev.org")));
        Assertions.assertTrue(ValidateUtils.validateApiUrlFormat(new URL("http://ft-api.test.eanadev.org")));
    }

    @Test
    public void testRecordApiUrlFalse() {
        Assertions.assertThrows(IllegalArgumentException.class, () ->
                Assertions.assertTrue(ValidateUtils.validateApiUrlFormat(new URL("https://search-api-test.google.nl"))));
    }

    @Test
    public void testEuropeanaUrl() {
        Assertions.assertTrue(ValidateUtils.isEuropeanaUrl("https://iiif.europeana.eu/image/GGDNOQYY5N35KNXL7PZBCNRWDJN6RCWLCKN6XXPRD5632RSEEQIA/presentation_images/c7aaa970-fd11-11e5-bc8a-fa163e60dd72/node-3/image/NLE/Edasi/1922/03/15/1/19220315_1-0001/full/full/0/default.jpg"));
    }

    @Test
    public void testNotEuropeanaUrl() {
        Assertions.assertFalse(ValidateUtils.isEuropeanaUrl("http://gallica.bnf.fr/iiif/ark:/12148/bpt6k555339z/f1/full/full/0/native.jpg"));
    }

}
