package eu.europeana.iiif;

import eu.europeana.iiif.model.v2.ManifestV2;
import eu.europeana.iiif.model.v3.ManifestV3;
import eu.europeana.iiif.service.ManifestService;
import eu.europeana.iiif.service.ManifestSettings;
import eu.europeana.iiif.service.exception.IIIFException;
import eu.europeana.iiif.service.exception.InvalidApiKeyException;
import eu.europeana.iiif.service.exception.RecordNotFoundException;
import org.apache.commons.logging.LogFactory;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests the general flow of generating output (getRecord, generate the manifest and serialize it) for both versions
 * of the manifest (v2 and v3).

 * @author Patrick Ehlert on 18-1-18.
 */

@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource("classpath:iiif-test.properties")
@SpringBootTest(classes = {ManifestService.class, ManifestSettings.class})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ManifestServiceTest {

    // TODO add mock record API

    @Value("${record-api.key:api2demo}")
    private String apikey;

    @Autowired
    private ManifestService ms;

    private String getRecord(String recordId) throws IIIFException {
        String json = ms.getRecordJson(recordId, apikey);
        assertNotNull(json);
        assertTrue(json.contains("\"about\":\""+recordId+"\""));
        return json;
    }

    private ManifestV2 getManifestV2(String recordId) throws IIIFException {
        ManifestV2 m = ms.generateManifestV2(getRecord(recordId));
        assertNotNull(m);
        assertTrue(m.getId().contains(recordId));
        return m;
    }

    private ManifestV3 getManifestV3(String recordId) throws IIIFException {
        ManifestV3 m = ms.generateManifestV3(getRecord(recordId));
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
        getRecord(ExampleRecordJson.EXAMPLE_PARENT_ID);
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
        ms.getRecordJson(ExampleRecordJson.EXAMPLE_CHILD_ID, "INVALID");
    }

    /**
     * Test generation of Manifest for version 2
     * @throws IIIFException
     */
    @Test
    public void testGetManifestV2() throws IIIFException {
        getManifestV2(ExampleRecordJson.EXAMPLE_CHILD_ID);
    }

    /**
     * Test generation of Manifest for version 3
     * @throws IIIFException
     */
    @Test
    public void testGetManifestV3() throws IIIFException {
        getManifestV3(ExampleRecordJson.EXAMPLE_PARENT_ID);
    }

    /**
     * Test serializing manifest for version 2
     * @throws IIIFException
     */
    @Test
    public void testSerializeJsonLdV2() throws IIIFException {
        String recordId = ExampleRecordJson.EXAMPLE_PARENT_ID;
        String jsonLd = ms.serializeManifest(getManifestV2(recordId));
        assertNotNull(jsonLd);
        LogFactory.getLog(ManifestService.class).info("jsonld v2 = " + jsonLd);
        assertTrue(jsonLd.contains("\"@id\" : \"https://iiif.europeana.eu/presentation" + recordId + "/manifest"));
        assertTrue(jsonLd.contains("\"http://iiif.io/api/presentation/2/context.json\""));
    }

    /**
     * Test serializing manifest for version 3
     * @throws IIIFException
     */
    @Test
    public void testSerializeJsonLdV3() throws IIIFException {
        String recordId = ExampleRecordJson.EXAMPLE_CHILD_ID;
        String jsonLd = ms.serializeManifest(getManifestV3(recordId));
        assertNotNull(jsonLd);
        LogFactory.getLog(ManifestService.class).info("jsonld v3 = "+jsonLd);
        assertTrue(jsonLd.contains("\"id\" : \"https://iiif.europeana.eu/presentation"+recordId+"/manifest"));
        assertTrue(jsonLd.contains("\"http://iiif.io/api/presentation/3/context.json\""));
    }


}
