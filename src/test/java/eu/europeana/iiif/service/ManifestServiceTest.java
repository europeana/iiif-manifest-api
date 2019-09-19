package eu.europeana.iiif.service;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import eu.europeana.iiif.ExampleData;
import eu.europeana.iiif.model.v2.ManifestV2;
import eu.europeana.iiif.model.v3.ManifestV3;
import eu.europeana.iiif.config.ManifestSettings;
import eu.europeana.iiif.service.exception.IIIFException;
import eu.europeana.iiif.service.exception.InvalidApiKeyException;
import eu.europeana.iiif.service.exception.RecordNotFoundException;
import eu.europeana.iiif.service.exception.RecordRetrieveException;
import org.apache.logging.log4j.LogManager;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.net.MalformedURLException;
import java.net.URL;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static junit.framework.TestCase.assertNull;
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
// The following 2 annotations are needed to enable hystrix so we can test timeouts and fallbacks
@EnableCircuitBreaker
@EnableAspectJAutoProxy
public class ManifestServiceTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().dynamicPort().dynamicHttpsPort());

    private static final String EXAMPLE_WSKEY = "junit";
    private static final String EXAMPLE_ERROR_ID = "/server/error";
    private static final String EXAMPLE_TIMEOUT_ID = "/timeout/1234";
    private static final String ANNOPAGE = "/annopage/";
    private static final String PRESENTATION = "/presentation";
    private static final String CONTENT_LENGTH = "Content-Length";
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String APPLICATION_JSON = "application/json;charset=UTF-8";
    private static final String API_V2_RECORD = "/api/v2/record";
    private static final String JSON_WSKEY = ".json?wskey=";

    @Autowired
    private ManifestService ms;

    @Before
    public void setupApiStub() {
        LogManager.getLogger(ManifestServiceTest.class).info("Mock API port {}, httpsPort {}", wireMockRule.port(), wireMockRule.httpsPort());

        // Record API, return 401 for all unknown wskeys
        stubFor(get(urlPathMatching("/api/v2/record/.*"))
                .withQueryParam("wskey", matching(".*"))
                .willReturn(aResponse()
                        .withStatus(401)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody("{\"error\": \"Invalid API key\"}")));

        // Record API, return 404 for all unknown record ids (that have a valid wskey)
        stubFor(get(urlPathMatching("/api/v2/record/.*"))
                .withQueryParam("wskey", equalTo(EXAMPLE_WSKEY))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody("{\"error\": \"Invalid record identifier\"}")));

        // Record API, parent record
        stubFor(get(urlEqualTo(API_V2_RECORD + ExampleData.EXAMPLE_RECORD_PARENT_ID + JSON_WSKEY+EXAMPLE_WSKEY))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(ExampleData.EXAMPLE_RECORD_PARENT_RESPONSE)));

        // Record API, child record
        stubFor(get(urlEqualTo(API_V2_RECORD + ExampleData.EXAMPLE_RECORD_CHILD_ID + JSON_WSKEY+EXAMPLE_WSKEY))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(ExampleData.EXAMPLE_RECORD_CHILD_RESPONSE)));

        // Record API, simulate timeout exception
        stubFor(get(urlEqualTo(API_V2_RECORD + EXAMPLE_ERROR_ID + JSON_WSKEY + EXAMPLE_WSKEY))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody("{\"error\": \"Server error\"}")));

        // Record API, simulate timeout exception
        stubFor(get(urlEqualTo(API_V2_RECORD + EXAMPLE_TIMEOUT_ID + JSON_WSKEY + EXAMPLE_WSKEY))
                .willReturn(aResponse()
                        .withFixedDelay(60000) // value should be longer than configured timeout for getRecord
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(ExampleData.EXAMPLE_RECORD_CHILD_RESPONSE)));

        // Full Text API, return 404 for all unknown annotation pages
        stubFor(head(urlPathMatching("/presentation/.*/.*/annopage/.*"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withHeader(CONTENT_LENGTH, "0")));

        // Full Text API, return 200 for proper HEAD request
        stubFor(head(urlEqualTo(PRESENTATION + ExampleData.EXAMPLE_FULLTEXT_ID + ANNOPAGE + ExampleData.EXAMPLE_FULLTEXT_PAGENR))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_LENGTH, "0")));

        // Full Text API, simulate server error
        stubFor(head(urlEqualTo(PRESENTATION + EXAMPLE_ERROR_ID + ANNOPAGE + ExampleData.EXAMPLE_FULLTEXT_PAGENR))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader(CONTENT_LENGTH, "0")));

        // Full Text API, simulate (timeout?) exception
        stubFor(head(urlEqualTo(PRESENTATION + EXAMPLE_TIMEOUT_ID + ANNOPAGE + ExampleData.EXAMPLE_FULLTEXT_PAGENR))
                .willReturn(aResponse()
                        .withFixedDelay(30000)
                        .withStatus(200)
                        .withHeader(CONTENT_LENGTH, "0")));
    }

    private URL getRecordApiUrl() {
        try {
            // Uncomment the line below to test against real life environment, but don't forget to set a proper wskey
            // Also some exception tests won't work properly (e.g. tests with timeout or 50x exceptions)
            //return new URL("https://api.europeana.eu");

            return new URL("http://localhost:" + wireMockRule.port());
        } catch (MalformedURLException mue) {
            throw new RuntimeException("Malformed server URL");
        }
    }

    private URL getFullTextApiUrl() {
        try {
            // Uncomment the line below to test against real life environment, but don't forget to set a proper wskey
            // Also some exception tests won't work properly (e.g. tests with timeout or 50x exceptions)
            //return new URL("https://fulltext-test.eanadev.org");

            return new URL("http://localhost:" + wireMockRule.port());
        } catch (MalformedURLException mue) {
            throw new RuntimeException("Malformed server URL");
        }
    }

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

    /**
     * Test whether we get a true value for an existing full text page
     */
    @Test
    public void testFullTextExists() throws IIIFException {
        String url = ms.generateFullTextUrl(ExampleData.EXAMPLE_FULLTEXT_ID, ExampleData.EXAMPLE_FULLTEXT_PAGENR, getFullTextApiUrl());
        Boolean result = ms.existsFullText(url);
        assertTrue(result);
    }

    /**
     * Test whether we get a null value if there is a server error
     */
    @Test
    public void testFullTextNotExists() throws IIIFException {
        String url = ms.generateFullTextUrl("/test/bla", "9999", getFullTextApiUrl());
        Boolean result = ms.existsFullText(url);
        assertFalse(result);
    }

    /**
     * Test whether we get a false value if a full text doesn't exists
     */
    @Test
    public void testFullTextServerError() throws IIIFException {
        String url = ms.generateFullTextUrl(EXAMPLE_ERROR_ID, ExampleData.EXAMPLE_FULLTEXT_PAGENR,
                getFullTextApiUrl());
        Boolean result = ms.existsFullText(url);
        assertNull(result);
    }

    /**
     * Test whether we get a null value if a request for a full text existence times out
     */
    @Test
    public void testFullTextTimeout() throws IIIFException {
        String url = ms.generateFullTextUrl(EXAMPLE_TIMEOUT_ID, ExampleData.EXAMPLE_FULLTEXT_PAGENR,
                getFullTextApiUrl());
        Boolean result = ms.existsFullText(url);
        assertNull(result);
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
     * Test whether we get a RecordRetrieveException if we get a 500 response from the server
     */
    @Test(expected = RecordRetrieveException.class)
    public void testGetJsonRecordServerError() throws IIIFException {
        getRecord(EXAMPLE_ERROR_ID);
    }

    /**
     * Test whether we get a HysterixRuntimeException if a getRecord operation times out
     */
    @Ignore // TODO temporarily disabled because we need to fix hysterix
    @Test(expected = HystrixRuntimeException.class)
    public void testGetJsonRecordTimeout() throws IIIFException {
        getRecord(EXAMPLE_TIMEOUT_ID);
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
