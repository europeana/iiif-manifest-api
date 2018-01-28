package eu.europeana.iiif;

import com.fasterxml.jackson.core.JsonProcessingException;
import eu.europeana.iiif.model.v3.Manifest;
import eu.europeana.iiif.service.ManifestService;
import eu.europeana.iiif.service.exception.IIIFException;
import eu.europeana.iiif.service.exception.InvalidApiKeyException;
import eu.europeana.iiif.service.exception.RecordNotFoundException;
import org.apache.commons.logging.LogFactory;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.io.IOException;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests for the general flow of generating output (getRecord, generate the manifest and serialize it)
 * @author Patrick Ehlert on 18-1-18.
 */

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ManifestServiceTest {


    private static final String TEST1_CHILD_RECORD_ID = "/9200408/BibliographicResource_3000117822022";
    private static final String TEST2_PARENT_RECORD_ID = "/9200356/BibliographicResource_3000100340004";
    private static final String TEST3_CHILD_RECORD_ID = "/9200385/BibliographicResource_3000117433317";
    private static final String APIKEY = "api2demo";

    private static ManifestService ms;

    @BeforeClass
    public static void setup() {
        ms = new ManifestService();
    }

    private String getRecord(String recordId) throws IIIFException {
        String json = ms.getRecordJson(recordId, APIKEY);
        assertNotNull(json);
        assertTrue(json.contains("\"about\":\""+recordId+"\""));
        return json;
    }

    private Manifest getManifest(String recordId) throws IIIFException {
        Manifest m = ms.generateManifest(getRecord(recordId));
        assertNotNull(m);
        assertTrue(m.getId().contains(recordId));
        return m;
    }

    /**
     * Test retrieval of record json data
     * @throws IIIFException
     */
    @Test
    public void testGetJsonRecord() throws IIIFException {
        getRecord(TEST1_CHILD_RECORD_ID);
    }

    /**
     * Test whether we get a RecordNotFoundException if we provide an incorrect id
     * @throws IIIFException
     */
    @Test(expected = RecordNotFoundException.class)
    public void testGetJsonRecordNotExist() throws IIIFException {
        getRecord("/NOTEXISTS/123");
    }

    /**
     * Test whether we get a InvalidApiKeyException if we provide an incorrect api key
     * @throws IIIFException
     */
    @Test(expected = InvalidApiKeyException.class)
    public void testGetJsonRecordApikeyInvalid() throws IIIFException {
        ms.getRecordJson(TEST1_CHILD_RECORD_ID, "INVALID");
    }

    /**
     * Test generation of Manifest
     * @throws IIIFException
     */
    @Test
    public void testGetManifest() throws IIIFException {
        getManifest(TEST2_PARENT_RECORD_ID);
    }

    /**
     * Test serializing manifest
     * @throws IIIFException
     */
    @Test
    public void testSerializeJsonLd() throws IIIFException {
        String recordId = TEST3_CHILD_RECORD_ID;
        String jsonLd = ms.serializeManifest(getManifest(recordId));
        assertNotNull(jsonLd);
        LogFactory.getLog(ManifestService.class).error("jsonld = "+jsonLd);
        assertTrue(jsonLd.contains("\"id\" : \"https://iiif.europeana.eu/presentation"+recordId+"/manifest"));
    }


}
