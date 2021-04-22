package eu.europeana.iiif.service;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider;
import com.jayway.jsonpath.spi.json.JsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import com.jayway.jsonpath.spi.mapper.MappingProvider;
import eu.europeana.iiif.config.ManifestSettings;
import eu.europeana.iiif.model.info.SummaryAnnoPage;
import eu.europeana.iiif.model.info.SummaryCanvas;
import eu.europeana.iiif.model.info.SummaryManifest;
import eu.europeana.iiif.model.v2.ManifestV2;
import eu.europeana.iiif.model.v2.Sequence;
import eu.europeana.iiif.model.v3.AnnotationPage;
import eu.europeana.iiif.model.v3.Canvas;
import eu.europeana.iiif.model.v3.ManifestV3;
import eu.europeana.iiif.service.exception.*;
import ioinformarics.oss.jackson.module.jsonld.JsonldModule;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.net.URL;
import java.util.*;

/**
 * Service that loads record data, uses that to generate a Manifest object and serializes the manifest in JSON-LD
 *
  * @author Patrick Ehlert
 *  Created on 06-12-2017
 */
@Service
public class ManifestService {

    private static final Logger LOG = LogManager.getLogger(ManifestService.class);
    private static final String APIKEY_NOT_VALID = "API key is not valid";

    // create a single objectMapper for efficiency purposes (see https://github.com/FasterXML/jackson-docs/wiki/Presentation:-Jackson-Performance)
    private static ObjectMapper mapper = new ObjectMapper();

    private final ManifestSettings    settings;
    private final CloseableHttpClient gethttpClient;
    private final CloseableHttpClient headhttpClient;


    public ManifestService(ManifestSettings settings) {
        this.settings = settings;

        // configure http client
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(200);
        cm.setDefaultMaxPerRoute(100);

        // configure for head requests to Fulltext API (with specific timeouts)
        RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(8 * 1000)
                .setSocketTimeout(5 * 1000).build();
        headhttpClient = HttpClients.custom().setConnectionManager(cm)
                .setDefaultRequestConfig(requestConfig).build();

        //configure for get requests to SummaryManifest API
        gethttpClient = HttpClients.custom().setConnectionManager(cm).build();

        // configure jsonpath: we use jsonpath in combination with Jackson because that makes it easier to know what
        // type of objects are returned (see also https://stackoverflow.com/a/40963445)
        com.jayway.jsonpath.Configuration.setDefaults(new com.jayway.jsonpath.Configuration.Defaults() {

            private final JsonProvider jsonProvider = new JacksonJsonNodeJsonProvider();
            private final MappingProvider mappingProvider = new JacksonMappingProvider();

            @Override
            public JsonProvider jsonProvider() {
                return jsonProvider;
            }

            @Override
            public MappingProvider mappingProvider() {
                return mappingProvider;
            }

            @Override
            public Set<Option> options() {
                if (Boolean.TRUE.equals(settings.getSuppressParseException())) {
                    // we want to be fault tolerant in production, but for testing we may want to disable this option
                    return EnumSet.of(Option.SUPPRESS_EXCEPTIONS);
                } else {
                    return EnumSet.noneOf(Option.class);
                }
            }
        });

        // configure Jackson serialization
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        mapper.registerModule(new JsonldModule())
                // add support for Java 8 Optionals
                .registerModule(new Jdk8Module())
                // ignore empty optionals
                .setSerializationInclusion(JsonInclude.Include.NON_ABSENT);
    }

    protected ObjectMapper getJsonMapper() {
        return mapper;
    }

    /**
     * Return record information in Json format from the default configured SummaryManifest API
     *
     * @param recordId Europeana record id in the form of "/datasetid/recordid" (so with leading slash and without trailing slash)
     * @param wsKey api key to send to record API
     *
     * @return record information in json format
     * @throws IIIFException (
     *      IllegalArgumentException if a parameter has an illegal format,
     *      InvalidApiKeyException if the provide key is not valid,
     *      RecordNotFoundException if there was a 404,
     *      RecordRetrieveException on all other problems)
     */
    public String getRecordJson(String recordId, String wsKey) throws IIIFException {
        return getRecordJson(recordId, wsKey, null);
    }

    /**
     * Return record information in Json format from an instance of the SummaryManifest API
     *
     * @param recordId Europeana record id in the form of "/datasetid/recordid" (so with leading slash and without trailing slash)
     * @param wsKey api key to send to record API
     * @param recordApiUrl if not null we will use the provided URL as the address of the SummaryManifest API instead of the default configured address
     * @throws IIIFException (
     *      IllegalArgumentException if a parameter has an illegal format,
     *      InvalidApiKeyException if the provide key is not valid,
     *      RecordNotFoundException if there was a 404,
     *      RecordRetrieveException on all other problems)
     * @return record information in json format     *
     *
     */
    // TODO only use hysterix for default connection!? Not for custom recordApiUrls?
//    @HystrixCommand(groupKey = "record", commandKey = "record", threadPoolKey = "record",
//                    ignoreExceptions = {InvalidApiKeyException.class, RecordNotFoundException.class}, commandProperties = {
//                @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "30000"),
//                @HystrixProperty(name = "fallback.enabled", value="false"),
//                @HystrixProperty(name = "circuitBreaker.enabled", value="true"),
//                @HystrixProperty(name = "circuitBreaker.requestVolumeThreshold", value="10") // short-circuit after 10 faulty requests
//            },
//            threadPoolProperties = {
//                    @HystrixProperty(name = "coreSize", value = "3"),
//                    //@HystrixProperty(name = "maximumSize", value="110"),
//                    //@HystrixProperty(name = "allowMaximumSizeToDivergeFromCoreSize", value = "true") 
//                    @HystrixProperty(name = "maxQueueSize", value = "-1") // use SynchronousQueue instead of LinkedBlockingQueue
//            }
//    )
    public String getRecordJson(String recordId, String wsKey, URL recordApiUrl) throws IIIFException {
        String result= null;

        StringBuilder url;
        if (recordApiUrl == null) {
            url = new StringBuilder(settings.getRecordApiBaseUrl());
        } else {
            url = new StringBuilder(recordApiUrl.toString());
        }
        if (settings.getRecordApiPath() != null) {
            url.append(settings.getRecordApiPath());
        }
        url.append(recordId);
        url.append(".json?wskey=");
        url.append(wsKey);

        try {
            String recordUrl = url.toString();
            try (CloseableHttpResponse response = gethttpClient.execute(new HttpGet(recordUrl))) {
                int responseCode = response.getStatusLine().getStatusCode();
                LOG.debug("SummaryManifest request: {}, status code = {}", recordId, responseCode);
                if (responseCode == HttpStatus.SC_UNAUTHORIZED) {
                    throw new InvalidApiKeyException(APIKEY_NOT_VALID);
                } else if (responseCode == HttpStatus.SC_NOT_FOUND) {
                    throw new RecordNotFoundException("SummaryManifest with id '"+recordId+"' not found");
                } else if (responseCode != HttpStatus.SC_OK) {
                    throw new RecordRetrieveException("Error retrieving record: "+response.getStatusLine().getReasonPhrase());
                }

                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    result = EntityUtils.toString(entity);
                    LOG.debug("SummaryManifest request: {}, response = {}", recordId, result);
                    EntityUtils.consume(entity); // make sure entity is consumed fully so connection can be reused
                } else {
                    LOG.warn("Request entity = null");
                }
            }
        } catch (IOException e) {
            throw new RecordRetrieveException("Error retrieving record", e);
        }

        return result;
    }

    /**
     * Generates a url to a full text resource
     * @param fullTextApiUrl optional, if not specified then the default Full-Text API specified in .properties is used
     */
    String generateFullTextUrl(String europeanaId, String pageId, URL fullTextApiUrl) {
        StringBuilder url = new StringBuilder(fullTextBaseUrl(fullTextApiUrl));
        String path = settings.getFullTextApiPath().replace("/<collectionId>/<itemId>", europeanaId).replace("<pageId>", pageId);
        url.append(path);
        return url.toString();
    }

    /**
     * Generates a url to a full text resource
     * @param fullTextApiUrl optional, if not specified then the default Full-Text API specified in .properties is used
     */
    String generateFullTextSummaryUrl(String europeanaId, URL fullTextApiUrl) {
        return fullTextBaseUrl(fullTextApiUrl) + settings.getFullTextSummaryPath()
                                                         .replace("/<collectionId>/<itemId>", europeanaId);
    }

    private String fullTextBaseUrl(URL fullTextApiUrl){
        if (fullTextApiUrl == null) {
            return settings.getFullTextApiBaseUrl();
        } else {
            return fullTextApiUrl.toString();
        }
    }

    /**
     * Performs a GET request for a particular EuropeanaID that:
     * - replaces the 'exists' check;
     * - lists all canvases found for that EuropeanaID;
     * - lists all original and all translated AnnoPages for every canvas
     * @param fullTextUrl url to which HEAD request is sent
     * @return Map with key PageId and as value an array of AnnoPage ID strings
     * @throws IIIFException when there is an error retrieving the fulltext AnnoPage summary
     */
    Map<String, String[]> getFullTextSummary(String fullTextUrl) throws IIIFException {
        SummaryManifest summary = null;
        boolean         result;
        try {
            try (CloseableHttpResponse response = gethttpClient.execute(new HttpGet(fullTextUrl))) {
                int responseCode = response.getStatusLine().getStatusCode();
                LOG.debug("Full-Text summary GET request: {}, status code = {}", fullTextUrl, responseCode);
                if (responseCode == HttpStatus.SC_UNAUTHORIZED) {
                    throw new InvalidApiKeyException(APIKEY_NOT_VALID);
                } else if (responseCode == HttpStatus.SC_NOT_FOUND) {
                    result = false;
                } else result = responseCode == HttpStatus.SC_OK;
                HttpEntity entity = response.getEntity();
                if (result && entity != null) {
                    summary = getJsonMapper().readValue(EntityUtils.toString(entity), SummaryManifest.class);
                    EntityUtils.consume(entity); // make sure entity is consumed fully so connection can be reused
                } else {
                    LOG.warn("Request entity = null");
                }
            }
        } catch (IOException e) {
            LOG.error("Error checking if full text exists", e);
            result = false;
        }
        if (result && null != summary) {
            return createAnnoPageMap(summary);
        } else {
            return null;
        }
    }

    private Map<String, String[]> createAnnoPageMap(SummaryManifest summary){
        LinkedHashMap<String, String[]> annoPageMap = new LinkedHashMap<>();
        for (SummaryCanvas summaryCanvas : summary.getCanvases()){
            List<String> annoPageIDs = new ArrayList<>();
            List<SummaryAnnoPage> annoPageList = summaryCanvas.getAnnotations();
            for (SummaryAnnoPage sap : annoPageList){
                annoPageIDs.add(sap.getId());
            }
            annoPageMap.put(summaryCanvas.getPageNumber(), annoPageIDs.toArray(String[]::new));
        }
        return annoPageMap;
    }


    /**
     * Performs a HEAD request for a particular annotation page to see if the full text page exists or not
     * @param fullTextUrl url to which HEAD request is sent
     * @return true if it exists, false if it doesn't exists, null if we got no response
     * @throws IIIFException when there is an error checking if a fulltext exists
     */
//    @HystrixCommand(commandProperties = {
//            @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "5000"),
//            @HystrixProperty(name = "fallback.enabled", value="true")
//    }, fallbackMethod = "fallbackExistsFullText")
//    Boolean existsFullTextOld(String fullTextUrl) throws IIIFException {
//        Boolean result;
//        try {
//            try (CloseableHttpResponse response = headhttpClient.execute(new HttpHead(fullTextUrl))) {
//                int responseCode = response.getStatusLine().getStatusCode();
//                LOG.debug("Full-Text head request: {}, status code = {}", fullTextUrl, responseCode);
//                if (responseCode == HttpStatus.SC_UNAUTHORIZED) {
//                    throw new InvalidApiKeyException(APIKEY_NOT_VALID);
//                } else if (responseCode == HttpStatus.SC_NOT_FOUND) {
//                    result = Boolean.FALSE;
//                } else if (responseCode == HttpStatus.SC_OK) {
//                    result = Boolean.TRUE;
//                } else {
//                    // TODO when hysterix is enabled again, we can simply throw an error again
//                    //throw new FullTextCheckException("Error checking if full text exists: "+response.getStatusLine().getReasonPhrase());
//                    LOG.error("Error checking if full text exists: {}", response.getStatusLine().getReasonPhrase());
//                    result = null;
//                }
//            }
//        } catch (IOException e) {
//            // TODO when hysterix is enabled again, we can simply throw an error again
//            //throw new FullTextCheckException("Error checking if full text exists", e);
//            LOG.error("Error checking if full text exists", e);
//            result = null;
//        }
//        return result;
//    }

    @SuppressWarnings({"unused", "squid:S2447"}) // method is used by hysterix as fallback
    private Boolean fallbackExistsFullText(String fullTextUrl) {
        return null; // we return null, meaning that we were not able to check if a full text exists or not.
    }

    /**
     * Generates a manifest object for IIIF v2 filled with data that is extracted from the provided JSON
     * @param json record data in JSON format
     * @param addFullText if true then for each canvas we will check if a full text exists and add the link to it's
     *                   annotation page
     * @param fullTextApi optional, if provided this url will be used to check if a full text is available or not
     * @return Manifest v2 object
     */
    public ManifestV2 generateManifestV2 (String json, boolean addFullText, URL fullTextApi)    {
        long start = System.currentTimeMillis();
        Object document = com.jayway.jsonpath.Configuration.defaultConfiguration().jsonProvider().parse(json);
        ManifestV2 result = EdmManifestMapping.getManifestV2(document);

        if (addFullText) {
            try {
                fillInFullTextLinksV2(result, fullTextApi);
            } catch (IIIFException ie) {
                LOG.error("Error adding full text links", ie);
            }
        } else {
            LOG.debug("Skipping full text link generation");
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Generated in {} ms", System.currentTimeMillis() - start);
        }
        return result;
    }

    /**
     * Generates a manifest object for IIIF v3 filled with data that is extracted from the provided JSON
     * @param json record data in JSON format
     * @param addFullText if true then for each canvas we will check if a full text exists and add the link to it's
     *                   annotation page
     * @param fullTextApi optional, if provided this url will be used to check if a full text is available or not
     * @return Manifest v3 object
     */
    public ManifestV3 generateManifestV3 (String json, boolean addFullText, URL fullTextApi)  {
        long start = System.currentTimeMillis();
        Object document = com.jayway.jsonpath.Configuration.defaultConfiguration().jsonProvider().parse(json);
        ManifestV3 result = EdmManifestMapping.getManifestV3(document);

        if (addFullText) {
            try {
                fillInFullTextLinksV3(result, fullTextApi);
            } catch (IIIFException ie) {
                LOG.error("Error adding full text links", ie);
            }
        } else {
            LOG.debug("Skipping full text link generation");
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Generated in {} ms ", System.currentTimeMillis() - start);
        }
        return result;
    }

    /**
     * We generate all full text links in one place, so we can raise a timeout if retrieving the necessary
     * data for all full texts is too slow.
     */
    private void fillInFullTextLinksV2(ManifestV2 manifest, URL fullTextApi) throws IIIFException {
        if (manifest.getSequences() != null && manifest.getSequences().length > 0) {
            // there is always only 1 sequence
            Sequence sequence = manifest.getSequences()[0];

            // Get all the available AnnoPages incl translations from the summary endpoint of Fulltext
            String fullTextSummaryUrl = generateFullTextSummaryUrl(manifest.getEuropeanaId(), fullTextApi);
            Map<String, String[]> fulltextSummary = getFullTextSummary(fullTextSummaryUrl);
            if (null != fulltextSummary){
                // loop over canvases to add full-text link(s) to all
                for (eu.europeana.iiif.model.v2.Canvas canvas : sequence.getCanvases()) {
                    canvas.setOtherContent(fulltextSummary.get(Integer.toString(canvas.getPageNr())));
                }
            }
        }
    }

    /**
     * We generate all full text links in one place, so we can raise a timeout if retrieving the necessary
     * data for all full texts is too slow.
     */
    private void fillInFullTextLinksV3(ManifestV3 manifest, URL fullTextApi) throws IIIFException {
        eu.europeana.iiif.model.v3.Canvas[] canvases = manifest.getItems();
        if (canvases != null) {

            // Get all the available AnnoPages incl translations from the summary endpoint of Fulltext
            String fullTextSummaryUrl = generateFullTextSummaryUrl(manifest.getEuropeanaId(), fullTextApi);
            Map<String, String[]> fulltextSummary = getFullTextSummary(fullTextSummaryUrl);
            if (null != fulltextSummary){
                // loop over canvases to add full-text link(s) to all
                for (eu.europeana.iiif.model.v3.Canvas canvas : canvases) {
                    List<AnnotationPage> summaryAnnoPages = new ArrayList<>();

                    for (String annoPageId : fulltextSummary.get(Integer.toString(canvas.getPageNr()))){
                        summaryAnnoPages.add(new AnnotationPage(annoPageId));
                    }
                    canvas.setAnnotations(summaryAnnoPages.toArray(new AnnotationPage[0]));
                }
            }
        }
    }

   /**
     * Serialize manifest to JSON-LD
     * @param m manifest
     * @return JSON-LD string
     * @throws RecordParseException when there is a problem parsing
     */
    public String serializeManifest(Object m) throws RecordParseException {
        try {
            return mapper.
                    writerWithDefaultPrettyPrinter().
                    writeValueAsString(m);
        } catch (IOException e) {
            throw new RecordParseException(String.format("Error serializing data: %s", e.getMessage()), e);
        }
    }

    /**
     * @return ManifestSettings object containing settings loaded from properties file
     */
    public ManifestSettings getSettings() {
        return settings;
    }

    @PreDestroy
    public void close() throws IOException {
        if (this.gethttpClient != null) {
            LOG.info("Closing get request http-client...");
            this.gethttpClient.close();
        }
        if (this.headhttpClient != null) {
            LOG.info("Closing head request http-client...");
            this.headhttpClient.close();
        }
    }

}
