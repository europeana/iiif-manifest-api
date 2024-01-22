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
class ValidationUtilsTest {
    @Test
    void testRecordId() throws IllegalArgumentException {
        Assertions.assertTrue(ValidateUtils.validateRecordIdFormat("/2023006/24062A51_priref_16913"));
    }

    @Test
    void testRecordIdFalse() {
        Assertions.assertThrows(IllegalArgumentException.class, () ->
                ValidateUtils.validateRecordIdFormat("/2023006/24062A51 priref_16913"));
    }

    @Test
    void testApiKey() throws IllegalArgumentException {
        Assertions.assertTrue(ValidateUtils.validateWskeyFormat("1aSd456"));
    }

    @Test
    void testApiKeyFalse() {
        Assertions.assertThrows(IllegalArgumentException.class, () ->
                Assertions.assertTrue(ValidateUtils.validateWskeyFormat("1a@d456")));
    }

    @Test
    void testRecordApiUrl() throws IllegalArgumentException, MalformedURLException {
        Assertions.assertTrue(ValidateUtils.validateApiUrlFormat(new URL("https://srch-api-test.eanadev.org")));
        Assertions.assertTrue(ValidateUtils.validateApiUrlFormat(new URL("https://srch-api.test.eanadev.org")));
        Assertions.assertTrue(ValidateUtils.validateApiUrlFormat(new URL("http://srch-api.acceptance.eanadev.org")));
    }

    @Test
    void testFullTextApiUrl() throws IllegalArgumentException, MalformedURLException {
        Assertions.assertTrue(ValidateUtils.validateApiUrlFormat(new URL("https://ft-api-test.eanadev.org")));
        Assertions.assertTrue(ValidateUtils.validateApiUrlFormat(new URL("http://ft-api.test.eanadev.org")));
    }

    @Test
    void testRecordApiUrlFalse() {
        Assertions.assertThrows(IllegalArgumentException.class, () ->
                Assertions.assertTrue(ValidateUtils.validateApiUrlFormat(new URL("https://search-api-test.google.nl"))));
    }

    @Test
    void testEuropeanaUrl() {
        Assertions.assertTrue(ValidateUtils.isEuropeanaUrl("https://iiif.europeana.eu/image/GGDNOQYY5N35KNXL7PZBCNRWDJN6RCWLCKN6XXPRD5632RSEEQIA/presentation_images/c7aaa970-fd11-11e5-bc8a-fa163e60dd72/node-3/image/NLE/Edasi/1922/03/15/1/19220315_1-0001/full/full/0/default.jpg"));
    }

    @Test
     void testNotEuropeanaUrl() {
        Assertions.assertFalse(ValidateUtils.isEuropeanaUrl("http://gallica.bnf.fr/iiif/ark:/12148/bpt6k555339z/f1/full/full/0/native.jpg"));
    }
    @Test
    void testFormatResourcepath(){
        Assertions.assertEquals("/val1/val2",ValidateUtils.formatResourcePath("/val1/val2"));
        Assertions.assertEquals("/val1/val2",ValidateUtils.formatResourcePath("/val1/val2/"));
        Assertions.assertEquals("/val1/val2",ValidateUtils.formatResourcePath("/val1//////val2///"));
        Assertions.assertEquals("/val1",ValidateUtils.formatResourcePath("val1"));
    }
    @Test
    void testFormatResourcepath_empty_input(){
        Assertions.assertNull(ValidateUtils.formatResourcePath(null));
        Assertions.assertEquals("",ValidateUtils.formatResourcePath(""));
    }



    @Test
    void testFormatBaseUrl(){
        Assertions.assertEquals("http://abc.com",ValidateUtils.formatBaseUrl("http://abc.com/"));
        Assertions.assertEquals("https://",ValidateUtils.formatBaseUrl ("https:///"));
        Assertions.assertEquals("https://abc/ccd",ValidateUtils.formatBaseUrl ("https://abc/ccd///"));
    }
    @Test
    void testFormatBaseUrl_empty_input(){
        Assertions.assertNull(ValidateUtils.formatBaseUrl(null));
        Assertions.assertEquals("",ValidateUtils.formatBaseUrl(""));
    }

}
