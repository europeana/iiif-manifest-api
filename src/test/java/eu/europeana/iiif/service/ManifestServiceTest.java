package eu.europeana.iiif.service;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import eu.europeana.api.commons.error.EuropeanaApiException;
import eu.europeana.iiif.ExampleData;
import eu.europeana.iiif.config.ManifestSettings;
import eu.europeana.iiif.exception.InvalidApiKeyException;
import eu.europeana.iiif.exception.RecordNotFoundException;
import eu.europeana.iiif.exception.RecordRetrieveException;
import eu.europeana.iiif.model.info.FulltextSummaryCanvas;
import eu.europeana.iiif.model.v2.ManifestV2;
import eu.europeana.iiif.model.v3.ManifestV3;
import org.apache.logging.log4j.LogManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static eu.europeana.iiif.ExampleData.EXAMPLE_FULLTEXT_ID;
import static eu.europeana.iiif.ExampleData.EXAMPLE_FULLTEXT_SUMMARY_FRAGMENT;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;


/**
 * This is actually an integration test that tests the ManifestService class making requests to mocked external resources
 * such as the Record API and Full-Text API. For this we use a mock API created with the WireMock library
 *
 * @author Patrick Ehlert on 18-01-18.
 */
@WireMockTest(httpsEnabled = true)
@TestPropertySource(locations = "classpath:iiif-test.properties")
@SpringBootTest(classes = {ManifestService.class, ManifestSettings.class})
public class ManifestServiceTest {

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

    @RegisterExtension
    static WireMockExtension wmExtension = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort().dynamicHttpsPort())
            .build();

    @Autowired
    private ManifestService ms;

    @BeforeEach
    public void setupApiStub() {
        LogManager.getLogger(ManifestServiceTest.class).info("Mock API port {}, httpsPort {}",
                wmExtension.getPort(), wmExtension.getHttpsPort());

        // Record API, return 401 for all unknown wskeys
        wmExtension.stubFor(get(urlPathMatching(API_V2_RECORD + "/.*"))
                        .withQueryParam("wskey", matching(".*"))
                        .willReturn(aResponse()
                                            .withStatus(401)
                                            .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                                            .withBody("{\"error\": \"Invalid API key\"}")));

        // Record API, return 404 for all unknown record ids (that have a valid wskey)
        wmExtension.stubFor(get(urlPathMatching(API_V2_RECORD + "/.*"))
                        .withQueryParam("wskey", equalTo(EXAMPLE_WSKEY))
                        .willReturn(aResponse()
                                            .withStatus(404)
                                            .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                                            .withBody("{\"error\": \"Invalid record identifier\"}")));

        // Record API, parent record
        wmExtension.stubFor(get(urlEqualTo(API_V2_RECORD + ExampleData.EXAMPLE_RECORD_PARENT_ID + JSON_WSKEY+EXAMPLE_WSKEY))
                        .willReturn(aResponse()
                                            .withStatus(200)
                                            .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                                            .withBody(ExampleData.EXAMPLE_RECORD_PARENT_RESPONSE)));

        // Record API, child record
        wmExtension.stubFor(get(urlEqualTo(API_V2_RECORD + ExampleData.EXAMPLE_RECORD_CHILD_ID + JSON_WSKEY+EXAMPLE_WSKEY))
                        .willReturn(aResponse()
                                            .withStatus(200)
                                            .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                                            .withBody(ExampleData.EXAMPLE_RECORD_CHILD_RESPONSE)));

        //Record API, minimal data
        wmExtension.stubFor(get(urlEqualTo(API_V2_RECORD + ExampleData.EXAMPLE_RECORD_MINIMAL_ID + JSON_WSKEY+EXAMPLE_WSKEY))
                        .willReturn(aResponse()
                                            .withStatus(200)
                                            .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                                            .withBody(ExampleData.EXAMPLE_RECORD_MINIMAL_RESPONSE)));

        // Record API, simulate server error
        wmExtension.stubFor(get(urlEqualTo(API_V2_RECORD + EXAMPLE_ERROR_ID + JSON_WSKEY + EXAMPLE_WSKEY))
                        .willReturn(aResponse()
                                            .withStatus(500)
                                            .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                                            .withBody("{\"error\": \"Server error\"}")));

        // Record API, simulate timeout exception
        wmExtension.stubFor(get(urlEqualTo(API_V2_RECORD + EXAMPLE_TIMEOUT_ID + JSON_WSKEY + EXAMPLE_WSKEY))
                        .willReturn(aResponse()
                                            .withFixedDelay(ManifestService.RECORD_SOCKET_TIMEOUT + 500)
                                            .withStatus(200)
                                            .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                                            .withBody(ExampleData.EXAMPLE_RECORD_CHILD_RESPONSE)));


        // Full Text API, return 200 for proper Summary request
        wmExtension.stubFor(get(urlEqualTo(PRESENTATION + EXAMPLE_FULLTEXT_ID + ANNOPAGE))
                        .willReturn(aResponse()
                                            .withStatus(200)
                                            .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                                            .withBody(ExampleData.EXAMPLE_FULLTEXT_SUMMARY_RESPONSE)));

        // Full Text API, return 404 for unknown Summary request
        wmExtension.stubFor(get(urlEqualTo(PRESENTATION + TEST_BLA + ANNOPAGE))
                        .willReturn(aResponse()
                                            .withStatus(404)
                                            .withBody("{\"error\": \"Not Found\"}")));

        // Full Text API, simulate server error for Summary request
        wmExtension.stubFor(get(urlEqualTo(PRESENTATION + EXAMPLE_ERROR_ID + ANNOPAGE))
                        .willReturn(aResponse()
                                            .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                                            .withStatus(500)
                                            .withBody("{\"error\": \"Server error\"}")));

        // Full Text API, simulate (timeout?) exception for Summary request
        wmExtension.stubFor(get(urlEqualTo(PRESENTATION + EXAMPLE_TIMEOUT_ID + ANNOPAGE))
                        .willReturn(aResponse()
                                .withFixedDelay(ManifestService.FULLTEXT_SOCKET_TIMEOUT + 500)
                                            .withStatus(200)
                                            .withHeader(CONTENT_LENGTH, "0")));

        // Full Text API, return 200 for EXAMPLE_RECORD_CHILD_ID
        wmExtension.stubFor(get(urlEqualTo(PRESENTATION + ExampleData.EXAMPLE_RECORD_CHILD_ID + ANNOPAGE))
                        .willReturn(aResponse()
                                            .withStatus(200)
                                            .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                                            .withBody(ExampleData.EXAMPLE_FULLTEXT_SUMMARY_RESPONSE)));


        // Full Text API, return 200 for EXAMPLE_RECORD_PARENT_ID
        wmExtension.stubFor(get(urlEqualTo(PRESENTATION + ExampleData.EXAMPLE_RECORD_PARENT_ID + ANNOPAGE))
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

            return new URL("http://localhost:" + wmExtension.getPort());
        } catch (MalformedURLException mue) {
            throw new RuntimeException("Malformed server URL");
        }
    }

    private URL getFullTextApiUrl() {
        try {
            // Uncomment the line below to test against real life environment, but don't forget to set a proper wskey
            // Also some exception tests won't work properly (e.g. tests with timeout or 50x exceptions)
            //return new URL("https://fulltext-test.eanadev.org");

            return new URL("http://localhost:" + wmExtension.getPort());
        } catch (MalformedURLException mue) {
            throw new RuntimeException("Malformed server URL");
        }
    }

    private String getRecord(String recordId) throws EuropeanaApiException {
        String json = ms.getRecordJson(recordId, EXAMPLE_WSKEY, getRecordApiUrl());
        assertNotNull(json);
        assertTrue(json.contains("\"about\":\""+recordId+"\""));
        return json;
    }

    private ManifestV2 getManifestV2(String recordId) throws EuropeanaApiException {
        ManifestV2 m = ms.generateManifestV2(getRecord(recordId), getFullTextApiUrl());
        assertNotNull(m);
        assertTrue(m.getId().contains(recordId));
        return m;
    }

    private ManifestV3 getManifestV3(String recordId) throws EuropeanaApiException {
        ManifestV3 m = ms.generateManifestV3(getRecord(recordId), getFullTextApiUrl());
        assertNotNull(m);
        assertTrue(m.getId().contains(recordId));
        return m;
    }

    /**
     * Test whether we get a valid response for the Fulltext summary check
     */
    @Test
    public void testFullTextSummaryExists() throws EuropeanaApiException {
        String url = ms.generateFullTextSummaryUrl(EXAMPLE_FULLTEXT_ID, getFullTextApiUrl());
        Map<String, FulltextSummaryCanvas> result = ms.getFullTextSummary(url);
        assertEquals(1, result.keySet().size());
        assertEquals(2, result.get("1").getAnnoPageIDs().size());
        assertEquals(EXAMPLE_FULLTEXT_SUMMARY_FRAGMENT, Arrays.stream(result.get("1").getAnnoPageIDs().toArray(new String[0])).toArray()[0]);
    }

    /**
     * Test whether we get a null value if there is a server error
     */
    @Test
    public void testFullTextSummaryNotExists() throws EuropeanaApiException {
        String url = ms.generateFullTextSummaryUrl(TEST_BLA, getFullTextApiUrl());
        Map<String, FulltextSummaryCanvas> result = ms.getFullTextSummary(url);
        assertNull(result);
    }

    /**
     * I can't find a way how to provoke a HTTP 500 from the summary endpoint, so I'm not sure what it would return
     */
    @Test
    public void testFullTextServerError() throws EuropeanaApiException {
        String url = ms.generateFullTextSummaryUrl(EXAMPLE_ERROR_ID, getFullTextApiUrl());
        Map<String, FulltextSummaryCanvas> result = ms.getFullTextSummary(url);
        assertNull(result);
    }

    /**
     * Test whether we get a null value if a request for a full text existence times out.
     * Note that if the test is configured properly, we should see a SocketTimeout being logged (but no exception thrown)
     */
    @Test
    public void testFullTextTimeout() throws EuropeanaApiException {
        String url = ms.generateFullTextSummaryUrl(EXAMPLE_TIMEOUT_ID, getFullTextApiUrl());
        Map<String, FulltextSummaryCanvas> result = ms.getFullTextSummary(url);
        assertNull(result);
    }

    /**
     * Test retrieval of record json data
     */
    @Test
    public void testGetJsonRecord() throws EuropeanaApiException {
        getRecord(ExampleData.EXAMPLE_RECORD_PARENT_ID);
    }

    /**
     * Test whether we get a RecordNotFoundException if we provide an incorrect id
     */
    @Test
    public void testGetJsonRecordNotExist() {
        Assertions.assertThrows(RecordNotFoundException.class, () ->
            getRecord("/NOTEXISTS/123"));
    }

    /**
     * Test whether we get a RecordRetrieveException if we get a 500 response from the server
     */
    @Test
    public void testGetJsonRecordServerError() {
        Assertions.assertThrows(RecordRetrieveException.class, () ->
            getRecord(EXAMPLE_ERROR_ID));
    }

    /**
     * Test whether we get a InvalidApiKeyException if we provide an incorrect api key
     */
    @Test
    public void testGetJsonRecordApikeyInvalid() {
        Assertions.assertThrows(InvalidApiKeyException.class, () ->
            ms.getRecordJson(ExampleData.EXAMPLE_RECORD_CHILD_ID, "INVALID", getRecordApiUrl()));
    }

    /**
     * Test generation of Manifest for version 2
     */
    @Test
    public void testGetManifestV2() throws EuropeanaApiException {
        getManifestV2(ExampleData.EXAMPLE_RECORD_CHILD_ID);
        getManifestV2(ExampleData.EXAMPLE_RECORD_MINIMAL_ID);
    }

    /**
     * Test generation of Manifest for version 3
     */
    @Test
    public void testGetManifestV3() throws EuropeanaApiException {
        getManifestV3(ExampleData.EXAMPLE_RECORD_PARENT_ID);
        getManifestV2(ExampleData.EXAMPLE_RECORD_MINIMAL_ID);
    }

    /**
     * Test serializing manifest for version 2
     */
    @Test
    public void testSerializeJsonLdV2() throws EuropeanaApiException {
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
    public void testSerializeJsonLdV3() throws EuropeanaApiException {
        String recordId = ExampleData.EXAMPLE_RECORD_CHILD_ID;
        String jsonLd = ms.serializeManifest(getManifestV3(recordId));
        assertNotNull(jsonLd);
        LogManager.getLogger(ManifestService.class).info("jsonld v3 = "+jsonLd);
        assertTrue(jsonLd.contains("\"id\" : \"https://iiif.europeana.eu/presentation"+recordId+"/manifest"));
        assertTrue(jsonLd.contains("\"http://iiif.io/api/presentation/3/context.json\""));
    }

}
