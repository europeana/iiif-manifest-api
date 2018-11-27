package eu.europeana.iiif;

import eu.europeana.iiif.service.ValidateUtils;
import eu.europeana.iiif.service.exception.IllegalArgumentException;
import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URL;

import static org.junit.Assert.assertFalse;
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
    public void testRecordApiUrl() throws IllegalArgumentException, MalformedURLException {
        assertTrue(ValidateUtils.validateApiUrlFormat(new URL("https://search-api-test.eanadev.org")));
    }

    @Test
    public void testFullTextApiUrl() throws IllegalArgumentException, MalformedURLException {
        assertTrue(ValidateUtils.validateApiUrlFormat(new URL("https://fulltext-api-test.eanadev.org")));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRecordApiUrlFalse() throws IllegalArgumentException, MalformedURLException {
        assertTrue(ValidateUtils.validateApiUrlFormat(new URL("https://search-api-test.google.nl")));
    }

    @Test
    public void testEuropeanaUrl() {
        assertTrue(ValidateUtils.isEuropeanaUrl("https://iiif.europeana.eu/image/GGDNOQYY5N35KNXL7PZBCNRWDJN6RCWLCKN6XXPRD5632RSEEQIA/presentation_images/c7aaa970-fd11-11e5-bc8a-fa163e60dd72/node-3/image/NLE/Edasi/1922/03/15/1/19220315_1-0001/full/full/0/default.jpg"));
    }

    @Test
    public void testNotEuropeanaUrl() {
        assertFalse(ValidateUtils.isEuropeanaUrl("http://gallica.bnf.fr/iiif/ark:/12148/bpt6k555339z/f1/full/full/0/native.jpg"));
    }

}
