package eu.europeana.iiif;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import eu.europeana.iiif.model.v2.ManifestV2;
import eu.europeana.iiif.model.v3.ManifestV3;
import eu.europeana.iiif.service.ManifestService;
import eu.europeana.iiif.service.ManifestSettings;
import eu.europeana.iiif.service.exception.IIIFException;
import eu.europeana.iiif.service.exception.InvalidApiKeyException;
import eu.europeana.iiif.service.exception.RecordNotFoundException;
import org.apache.logging.log4j.LogManager;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.net.MalformedURLException;
import java.net.URL;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static com.github.tomakehurst.wiremock.client.WireMock.*;



/**
 * This is actually an integration test that tests the ManifestService class making requests to mocked external resources
 * such as the Record API and Full-Text API. For this we use a mock API created with the WireMock library
 *
 * @author Patrick Ehlert on 18-01-18.
 */

@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource(locations = "classpath:iiif-test.properties")
@SpringBootTest(classes = {ManifestService.class, ManifestSettings.class})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ManifestServiceTest {

    private static final String EXAMPLE_WSKEY = "junit";

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().dynamicPort().dynamicHttpsPort());

    @Before
    public void setupApiStub() {
        LogManager.getLogger(ManifestServiceTest.class).info("Mock API port {}, httpsPort {}", wireMockRule.port(), wireMockRule.httpsPort());

        // Record API, return 401 for all unknown wskeys
        stubFor(get(urlPathMatching("/api/v2/record/.*"))
                .withQueryParam("wskey", matching(".*"))
                .willReturn(aResponse()
                        .withStatus(401)
                        .withHeader("Content-Type", "application/json;charset=UTF-8")
                        .withBody("{\"error\": \"Invalid API key\"}")));

        // Record API, return 404 for all unknown record ids (that have a valid wskey)
        stubFor(get(urlPathMatching("/api/v2/record/.*"))
                .withQueryParam("wskey", equalTo(EXAMPLE_WSKEY))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withHeader("Content-Type", "application/json;charset=UTF-8")
                        .withBody("{\"error\": \"Invalid record identifier\"}")));

        // Record API, parent record
        stubFor(get(urlEqualTo("/api/v2/record" +ExampleData.EXAMPLE_RECORD_PARENT_ID+ ".json?wskey="+EXAMPLE_WSKEY))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json;charset=UTF-8")
                        .withBody(ExampleData.EXAMPLE_RECORD_PARENT_RESPONSE)));

        // Record API, child record
        stubFor(get(urlEqualTo("/api/v2/record" +ExampleData.EXAMPLE_RECORD_CHILD_ID+ ".json?wskey="+EXAMPLE_WSKEY))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json;charset=UTF-8")
                        .withBody(ExampleData.EXAMPLE_RECORD_CHILD_RESPONSE)));

        // Full Text API, return 404 for all unknown annotation pages
        stubFor(head(urlPathMatching("/presentation/.*/.*/annopage/.*"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withHeader("Content-Length", "0")));

        // Full Text API, return 200 for proper HEAD request
        stubFor(head(urlEqualTo("/presentation" + ExampleData.EXAMPLE_FULLTEXT_ID + "/annopage/"+ExampleData.EXAMPLE_FULLTEXT_PAGENR))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Length", "0")));
    }

    private URL getRecordApiUrl() {
        try {
            // Uncomment the line below to test against real life environment, but don't forget to set a proper wskey
            //return new URL("https://api.europeana.eu");

            return new URL("http://localhost:" + wireMockRule.port());
        } catch (MalformedURLException mue) {
            throw new RuntimeException("Malformed server URL");
        }
    }

    private URL getFullTextApiUrl() {
        try {
            // Uncomment the line below to test against real life environment, but don't forget to set a proper wskey
            //return new URL("https://fulltext-test.eanadev.org");

            return new URL("http://localhost:" + wireMockRule.port());
        } catch (MalformedURLException mue) {
            throw new RuntimeException("Malformed server URL");
        }
    }

    @Autowired
    private ManifestService ms;

    private String getRecord(String recordId) throws IIIFException {
        String json = ms.getRecordJson(recordId, EXAMPLE_WSKEY, getRecordApiUrl());
        assertNotNull(json);
        assertTrue(json.contains("\"about\":\""+recordId+"\""));
        return json;
    }

    private ManifestV2 getManifestV2(String recordId) throws IIIFException{
        ManifestV2 m = ms.generateManifestV2(getRecord(recordId), true, getFullTextApiUrl());
        assertNotNull(m);
        assertTrue(m.getId().contains(recordId));
        return m;
    }

    private ManifestV3 getManifestV3(String recordId) throws IIIFException {
        ManifestV3 m = ms.generateManifestV3(getRecord(recordId), true, getFullTextApiUrl());
        assertNotNull(m);
        assertTrue(m.getId().contains(recordId));
        return m;
    }

    @Test
    public void testFullTextExists() throws IIIFException {
        String url = ms.generateFullTextUrl(ExampleData.EXAMPLE_FULLTEXT_ID, ExampleData.EXAMPLE_FULLTEXT_PAGENR, getFullTextApiUrl());
        Boolean result = ms.existsFullText(url);
        assertTrue(result);
    }

    @Test
    public void testFullTextNotExists() throws IIIFException {
        String url = ms.generateFullTextUrl("/test/bla", "9999", getFullTextApiUrl());
        Boolean result = ms.existsFullText(url);
        assertFalse(result);
    }

    /**
     * Test retrieval of record json data
     */
    @Test
    public void testGetJsonRecord() throws IIIFException {
        getRecord(ExampleData.EXAMPLE_RECORD_PARENT_ID);
    }

    /**
     * Test whether we get a RecordNotFoundException if we provide an incorrect id
     */
    @Test(expected = RecordNotFoundException.class)
    public void testGetJsonRecordNotExist() throws IIIFException {
        getRecord("/NOTEXISTS/123");
    }

    /**
     * Test whether we get a InvalidApiKeyException if we provide an incorrect api key
     */
    @Test(expected = InvalidApiKeyException.class)
    public void testGetJsonRecordApikeyInvalid() throws IIIFException {
        ms.getRecordJson(ExampleData.EXAMPLE_RECORD_CHILD_ID, "INVALID", getRecordApiUrl());
    }

    /**
     * Test generation of Manifest for version 2
     */
    @Test
    public void testGetManifestV2() throws IIIFException {
        getManifestV2(ExampleData.EXAMPLE_RECORD_CHILD_ID);
    }

    /**
     * Test generation of Manifest for version 3
     */
    @Test
    public void testGetManifestV3() throws IIIFException {
        getManifestV3(ExampleData.EXAMPLE_RECORD_PARENT_ID);
    }

    /**
     * Test serializing manifest for version 2
     */
    @Test
    public void testSerializeJsonLdV2() throws IIIFException {
        String recordId = ExampleData.EXAMPLE_RECORD_PARENT_ID;
        String jsonLd = ms.serializeManifest(getManifestV2(recordId));
        assertNotNull(jsonLd);
        LogManager.getLogger(ManifestService.class).info("jsonld v2 = " + jsonLd);
        assertTrue(jsonLd.contains("\"@id\" : \"https://iiif.europeana.eu/presentation" + recordId + "/manifest"));
        assertTrue(jsonLd.contains("\"http://iiif.io/api/presentation/2/context.json\""));
    }

    /**
     * Test serializing manifest for version 3
     */
    @Test
    public void testSerializeJsonLdV3() throws IIIFException {
        String recordId = ExampleData.EXAMPLE_RECORD_CHILD_ID;
        String jsonLd = ms.serializeManifest(getManifestV3(recordId));
        assertNotNull(jsonLd);
        LogManager.getLogger(ManifestService.class).info("jsonld v3 = "+jsonLd);
        assertTrue(jsonLd.contains("\"id\" : \"https://iiif.europeana.eu/presentation"+recordId+"/manifest"));
        assertTrue(jsonLd.contains("\"http://iiif.io/api/presentation/3/context.json\""));
    }


}
