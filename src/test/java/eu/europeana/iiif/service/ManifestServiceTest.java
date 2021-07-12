package eu.europeana.iiif.service;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import eu.europeana.iiif.ExampleData;
import eu.europeana.iiif.model.info.FulltextSummaryCanvas;
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
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Map;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static eu.europeana.iiif.ExampleData.EXAMPLE_FULLTEXT_ID;
import static eu.europeana.iiif.ExampleData.EXAMPLE_FULLTEXT_SUMMARY_FRAGMENT;
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
@SpringBootTest(classes = {ManifestService.class, ManifestSettings.class, SpringContext.class})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
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
    private static final String API_V2_RECORD = "/record/v2";
    private static final String JSON_WSKEY = ".json?wskey=";
    private static final String TEST_BLA = "/test/bla";

    @Autowired
    private ManifestService ms;

    @Before
    public void setupApiStub() {
        LogManager.getLogger(ManifestServiceTest.class).info("Mock API port {}, httpsPort {}", wireMockRule.port(), wireMockRule.httpsPort());

        // Record API, return 401 for all unknown wskeys
        stubFor(get(urlPathMatching(API_V2_RECORD + "/.*"))
                .withQueryParam("wskey", matching(".*"))
                .willReturn(aResponse()
                        .withStatus(401)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody("{\"error\": \"Invalid API key\"}")));

        // Record API, return 404 for all unknown record ids (that have a valid wskey)
        stubFor(get(urlPathMatching(API_V2_RECORD + "/.*"))
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

        //Record API, minimal data
        stubFor(get(urlEqualTo(API_V2_RECORD + ExampleData.EXAMPLE_RECORD_MINIMAL_ID + JSON_WSKEY+EXAMPLE_WSKEY))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(ExampleData.EXAMPLE_RECORD_MINIMAL_RESPONSE)));

        // Record API, simulate timeout exception
        stubFor(get(urlEqualTo(API_V2_RECORD + EXAMPLE_ERROR_ID + JSON_WSKEY + EXAMPLE_WSKEY))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody("{\"error\": \"Server error\"}")));

        // Record API, simulate timeout exception
        stubFor(get(urlEqualTo(API_V2_RECORD + EXAMPLE_TIMEOUT_ID + JSON_WSKEY + EXAMPLE_WSKEY))
                .willReturn(aResponse()
                        .withFixedDelay(10000) // value should be longer than configured timeout for getRecord
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(ExampleData.EXAMPLE_RECORD_CHILD_RESPONSE)));



        // Full Text API, return 200 for proper Summary request
        stubFor(get(urlEqualTo(PRESENTATION + EXAMPLE_FULLTEXT_ID + ANNOPAGE))
                        .willReturn(aResponse()
                                            .withStatus(200)
                                            .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                                            .withBody(ExampleData.EXAMPLE_FULLTEXT_SUMMARY_RESPONSE)));

        // Full Text API, return 404 for unknown Summary request
        stubFor(get(urlEqualTo(PRESENTATION + TEST_BLA + ANNOPAGE))
                        .willReturn(aResponse()
                                            .withStatus(404)
                                            .withBody("{\"error\": \"Not Found\"}")));

        // Full Text API, simulate server error for Summary request
        stubFor(get(urlEqualTo(PRESENTATION + EXAMPLE_ERROR_ID + ANNOPAGE))
                        .willReturn(aResponse()
                                            .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                                            .withBody("{\"error\": \"Server error\"}")));

        // Full Text API, simulate (timeout?) exception for Summary request
        stubFor(get(urlEqualTo(PRESENTATION + EXAMPLE_TIMEOUT_ID + ANNOPAGE))
                        .willReturn(aResponse()
                                            .withFixedDelay(5000)
                                            .withStatus(200)
                                            .withHeader(CONTENT_LENGTH, "0")));

        // Full Text API, return 200 for EXAMPLE_RECORD_CHILD_ID
        stubFor(get(urlEqualTo(PRESENTATION + ExampleData.EXAMPLE_RECORD_CHILD_ID + ANNOPAGE))
                        .willReturn(aResponse()
                                            .withStatus(200)
                                            .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                                            .withBody(ExampleData.EXAMPLE_FULLTEXT_SUMMARY_RESPONSE)));


        // Full Text API, return 200 for EXAMPLE_RECORD_PARENT_ID
        stubFor(get(urlEqualTo(PRESENTATION + ExampleData.EXAMPLE_RECORD_PARENT_ID + ANNOPAGE))
                        .willReturn(aResponse()
                                            .withStatus(200)
                                            .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                                            .withBody(ExampleData.EXAMPLE_FULLTEXT_SUMMARY_RESPONSE)));
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

    private ManifestV2 getManifestV2(String recordId) throws IIIFException {
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
     * Test whether we get a valid response for the Fulltext summary check
     */
    @Test
    public void testFullTextSummaryExists() throws IIIFException {
        String                             url    = ms.generateFullTextSummaryUrl(EXAMPLE_FULLTEXT_ID, getFullTextApiUrl());
        Map<String, FulltextSummaryCanvas> result = ms.getFullTextSummary(url);
        Assert.assertEquals(1, result.keySet().size());
        Assert.assertEquals(2, result.get("1").getAnnoPageIDs().size());
        Assert.assertEquals(EXAMPLE_FULLTEXT_SUMMARY_FRAGMENT, Arrays.stream(result.get("1").getAnnoPageIDs().toArray(new String[0])).toArray()[0]);
    }

    /**
     * Test whether we get a null value if there is a server error
     */
    @Test
    public void testFullTextSummaryNotExists() throws IIIFException {
        String                url    = ms.generateFullTextSummaryUrl(TEST_BLA, getFullTextApiUrl());
        Map<String, FulltextSummaryCanvas> result = ms.getFullTextSummary(url);
        assertNull(result);
    }

    /**
     * I can't find a way how to provoke a HTTP 500 from the summary endpoint, so I'm not sure what it would return
     */
    @Test
    public void testFullTextServerError() throws IIIFException {
        String url = ms.generateFullTextSummaryUrl(EXAMPLE_ERROR_ID, getFullTextApiUrl());
        Map<String, FulltextSummaryCanvas> result = ms.getFullTextSummary(url);
        assertNull(result);
    }

    /**
     * Test whether we get a null value if a request for a full text existence times out
     */
    @Test
    public void testFullTextTimeout() throws IIIFException {
        String url = ms.generateFullTextSummaryUrl(EXAMPLE_TIMEOUT_ID, getFullTextApiUrl());
        Map<String, FulltextSummaryCanvas> result = ms.getFullTextSummary(url);
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
        getManifestV2(ExampleData.EXAMPLE_RECORD_MINIMAL_ID);
    }

    /**
     * Test generation of Manifest for version 3
     */
    @Test
    public void testGetManifestV3() throws IIIFException {
        getManifestV3(ExampleData.EXAMPLE_RECORD_PARENT_ID);
        getManifestV2(ExampleData.EXAMPLE_RECORD_MINIMAL_ID);
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
