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
import eu.europeana.api.commons.error.EuropeanaApiException;
import eu.europeana.iiif.config.ManifestSettings;
import eu.europeana.iiif.config.MediaTypes;
import eu.europeana.iiif.exception.IllegalArgumentException;
import eu.europeana.iiif.model.ManifestDefinitions;
import eu.europeana.iiif.model.info.FulltextSummaryManifest;
import eu.europeana.iiif.model.info.FulltextSummaryAnnoPage;
import eu.europeana.iiif.model.info.FulltextSummaryCanvas;
import eu.europeana.iiif.model.v2.ManifestV2;
import eu.europeana.iiif.model.v2.Sequence;
import eu.europeana.iiif.model.v3.AnnotationPage;
import eu.europeana.iiif.model.v3.ManifestV3;
import eu.europeana.iiif.exception.*;
import ioinformarics.oss.jackson.module.jsonld.JsonldModule;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.cache.CacheResponseStatus;
import org.apache.http.client.cache.HttpCacheContext;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.cache.CacheConfig;
import org.apache.http.impl.client.cache.CachingHttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.*;


/**
 * Service that loads record data, uses that to generate a Manifest object and serializes the manifest in JSON-LD
 *
 * @author Patrick Ehlert
 * Created on 06-12-2017
 */
@Service
public class ManifestService {

    private static final Logger LOG               = LogManager.getLogger(ManifestService.class);
    private static final String APIKEY_NOT_VALID  = "API key is not valid";
    private static final String ITEM_FETCHED = "{} fetched in {} ms {}";

    // set this to FALSE to disable http caching for fulltext summary and record json
    private static final boolean USE_HTTP_CLIENT_CACHING = false;

    private static final int MAX_TOTAL_CONNECTIONS    = 200;
    private static final int DEFAULT_MAX_PER_ROUTE    = 100;
    private static final int MAX_CACHED_ENTRIES       = 1000;
    private static final int MAX_CACHED_OBJECT_SIZE   = 65536;

    private static final int RECORD_CONNECT_TIMEOUT = 10_000;
    protected static final int RECORD_SOCKET_TIMEOUT  = 30_000;
    private static final int FULLTEXT_CONNECT_TIMEOUT = 8_000;
    protected static final int FULLTEXT_SOCKET_TIMEOUT  = 20_000;

    // create a single objectMapper for efficiency purposes (see https://github.com/FasterXML/jackson-docs/wiki/Presentation:-Jackson-Performance)
    private static final ObjectMapper mapper = new ObjectMapper();

    private final ManifestSettings    settings;
    private final CloseableHttpClient recordHttpClient;
    private final CloseableHttpClient fulltextHttpClient;
    private HttpCacheContext    httpCacheContext = null;
    private final MediaTypes mediaTypes;


    /**
     * Creates an instance of the ManifestService bean with provided settings
     * @param settings read from properties file
     * @param mediaTypes
     */
    public ManifestService(ManifestSettings settings, MediaTypes mediaTypes) {
        this.settings = settings;
        this.mediaTypes = mediaTypes;

        // configure http client
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(MAX_TOTAL_CONNECTIONS);
        cm.setDefaultMaxPerRoute(DEFAULT_MAX_PER_ROUTE);

        if (USE_HTTP_CLIENT_CACHING) {
            recordHttpClient = initCachingHttpClient(cm, true);
            fulltextHttpClient = initCachingHttpClient(cm, false);
            httpCacheContext  = HttpCacheContext.create();
        } else {
            recordHttpClient = initNormalHttpClient(cm, true);
            fulltextHttpClient = initNormalHttpClient(cm, false);
        }

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

    private CloseableHttpClient initNormalHttpClient(PoolingHttpClientConnectionManager cm, boolean recordApi){
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(recordApi ? RECORD_CONNECT_TIMEOUT : FULLTEXT_CONNECT_TIMEOUT)
                .setSocketTimeout(recordApi ? RECORD_SOCKET_TIMEOUT : FULLTEXT_SOCKET_TIMEOUT)
                .build();

        return HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .setConnectionManager(cm).build();
    }

    private CloseableHttpClient initCachingHttpClient(PoolingHttpClientConnectionManager cm, boolean recordApi){
        CacheConfig cacheConfig = CacheConfig.custom()
                                             .setMaxCacheEntries(MAX_CACHED_ENTRIES)
                                             .setMaxObjectSize(MAX_CACHED_OBJECT_SIZE)
                                             .build();

        RequestConfig requestConfig = RequestConfig.custom()
                                                   .setConnectTimeout(recordApi ? RECORD_CONNECT_TIMEOUT : FULLTEXT_CONNECT_TIMEOUT)
                                                   .setSocketTimeout(recordApi ? RECORD_CONNECT_TIMEOUT : FULLTEXT_SOCKET_TIMEOUT)
                                                   .build();

        return CachingHttpClients.custom().setCacheConfig(cacheConfig)
                                 .setDefaultRequestConfig(requestConfig)
                                 .setConnectionManager(cm)
                                 .build();
    }

    protected ObjectMapper getJsonMapper() {
        return mapper;
    }

    /**
     * Return record information in Json format using the Record API base URL defined in the iiif.properties
     *
     * @param recordId Europeana record id in the form of "/datasetid/recordid" (so with leading slash and without trailing slash)
     * @param wsKey    api key to send to record API
     * @return record information in json format
     * @throws EuropeanaApiException (IllegalArgumentException if a parameter has an illegal format,
     *                       InvalidApiKeyException if the provide key is not valid,
     *                       RecordNotFoundException if there was a 404,
     *                       RecordRetrieveException on all other problems)
     */
    public String getRecordJson(String recordId, String wsKey) throws EuropeanaApiException {
        String recordUrl = buildRecordUrl(recordId, wsKey, settings.getRecordApiBaseUrlInternal());
        return fetchRecordJson(recordId, recordUrl);
    }

    /**
     * Return record information in Json format using the provided Record API url if not null; from iiif.properties
     * if it is null
     *
     * @param recordId     Europeana record id in the form of "/datasetid/recordid" (with leading slash and without trailing slash)
     * @param wsKey        api key to send to record API
     * @param recordApiUrl base URL of the Record API to use
     * @return record information in json format     *
     * @throws EuropeanaApiException (IllegalArgumentException if a parameter has an illegal format,
     *                       InvalidApiKeyException if the provide key is not valid,
     *                       RecordNotFoundException if there was a 404,
     *                       RecordRetrieveException on all other problems)
     */
    public String getRecordJson(String recordId, String wsKey, URL recordApiUrl) throws EuropeanaApiException {
        String recordUrl;
        if (null != recordApiUrl) {
            recordUrl = buildRecordUrl(recordId, wsKey, recordApiUrl.toString());
        } else {

            recordUrl = buildRecordUrl(recordId, wsKey, settings.getRecordApiBaseUrlInternal());
        }
        return fetchRecordJson(recordId, recordUrl);
    }

    private String buildRecordUrl(String recordId, String wsKey, String recordApiUrl) throws EuropeanaApiException {
        if (StringUtils.isBlank(recordApiUrl)){
            throw new IllegalArgumentException("Record API base url should not be empty");
        }
        StringBuilder url = new StringBuilder(recordApiUrl);
        if (settings.getRecordApiPath() != null) {
            url.append(settings.getRecordApiPath());
        }
        url.append(recordId);
        url.append(".json?wskey=");
        url.append(wsKey);
        return url.toString();
    }

    private String fetchRecordJson(String recordId, String recordUrl) throws EuropeanaApiException {
        String result;
        Instant start = Instant.now();
        try (CloseableHttpResponse response = recordHttpClient.execute(new HttpGet(recordUrl), httpCacheContext)) {
            Instant finish = Instant.now();

            logCaching("Record", start, finish, (httpCacheContext == null ? null : httpCacheContext.getCacheResponseStatus()));
            handleResponseCode(recordId,
                               response.getStatusLine().getStatusCode(),
                               response.getStatusLine().getReasonPhrase());
            result = consumeEntity(response.getEntity(), recordId);
        } catch (IOException e) {
            throw new RecordRetrieveException("Error retrieving record", e);
        }
        return result;
    }

    private void logCaching(String type, Instant start, Instant finish, CacheResponseStatus responseStatus) {
        String responseType = "";
        if (USE_HTTP_CLIENT_CACHING) {
            switch (responseStatus) {
                case CACHE_HIT:
                    responseType = "cache hit (no request sent)";
                    break;
                case CACHE_MODULE_RESPONSE:
                    responseType = "cache module response (response generated directly by caching module)";
                    break;
                case CACHE_MISS:
                    responseType = "cache miss (response from upstream server)";
                    break;
                case VALIDATED:
                    responseType = "validated (response generated after validating with origin server)";
                    break;
            }
        } else {
            responseType = "not using caching (disabled)";
        }
        LOG.debug(ITEM_FETCHED, type, Duration.between(start, finish).toMillis(), responseType);

    }

    private void handleResponseCode(String recordId, int responseCode, String reasonPhrase) throws EuropeanaApiException {
        LOG.debug("Record request {}, status code = {}", recordId, responseCode);

        if (responseCode == HttpStatus.SC_UNAUTHORIZED) {
            throw new InvalidApiKeyException(APIKEY_NOT_VALID);
        } else if (responseCode == HttpStatus.SC_NOT_FOUND) {
            throw new RecordNotFoundException("Record with id '" + recordId + "' not found");
        } else if (responseCode != HttpStatus.SC_OK) {
            LOG.error("Error retrieving record {}, reason {}", recordId, reasonPhrase);
            throw new RecordRetrieveException("Error retrieving record: " + reasonPhrase);
        }
    }

    private String consumeEntity(HttpEntity entity, String recordId) throws IOException {
        String result = null;
        if (entity != null) {
            result = EntityUtils.toString(entity);
            LOG.trace("Record request {}, response = {}", recordId, result);
            EntityUtils.consume(entity); // make sure entity is consumed fully so connection can be reused
        } else {
            LOG.warn("Request entity = null");
        }
        return result;
    }

    /**
     * Generates a url to a full text resource
     *
     * @param fullTextApiUrl optional, if not specified then the default Full-Text API specified in .properties is used
     * @param europeanaId identifier to include in the path
     */
    String generateFullTextSummaryUrl(String europeanaId, URL fullTextApiUrl) {
        if (fullTextApiUrl == null) {
            return settings.getFullTextApiBaseUrl() + ManifestDefinitions.getFulltextSummaryPath(europeanaId);
        } else {
            return fullTextApiUrl + ManifestDefinitions.getFulltextSummaryPath(europeanaId);
        }
    }

    /**
     * Performs a GET request for a particular EuropeanaID that:
     * - lists all canvases found for that EuropeanaID;
     * - lists all original and all translated AnnoPages for every canvas
     * It uses a CachingHTTPClient if USE_HTTP_CLIENT_CACHING = TRUE; a non-caching client when FALSE
     *
     * @param fullTextUrl url to FullText Summary endpoint
     * @return Map with key PageId and as value an array of AnnoPage ID strings
     * @throws EuropeanaApiException when there is an error retrieving the fulltext AnnoPage summary
     */
    Map<String, FulltextSummaryCanvas> getFullTextSummary(String fullTextUrl) throws EuropeanaApiException {

        FulltextSummaryManifest summary = null;
        Instant                 start   = Instant.now();

        try (CloseableHttpResponse response = fulltextHttpClient.execute(new HttpGet(fullTextUrl), httpCacheContext)) {
            Instant finish = Instant.now();
            logCaching("Fulltext", start, finish, (httpCacheContext == null ? null : httpCacheContext.getCacheResponseStatus()));
            summary = handleSummaryResponse(response, fullTextUrl);
        } catch (FullTextCheckException|IOException e) {
            LOG.error("Error connecting to Fulltext API at {}", fullTextUrl, e);
        }

        if (null != summary) {
            return createSummaryCanvasMap(summary);
        } else {
            return null;
        }
    }

    private FulltextSummaryManifest handleSummaryResponse(CloseableHttpResponse response, String fullTextUrl) throws EuropeanaApiException {
        boolean                 hasResult;
        FulltextSummaryManifest summary      = null;
        int                     responseCode = response.getStatusLine().getStatusCode();
        LOG.debug("Fulltext request {}, status code = {}", fullTextUrl, responseCode);

        hasResult = checkResponseCode(responseCode);
        HttpEntity entity = response.getEntity();

        if (hasResult && entity != null) {
            try {
                summary = getJsonMapper().readValue(EntityUtils.toString(entity), FulltextSummaryManifest.class);
                EntityUtils.consume(entity); // make sure entity is consumed fully so connection can be reused
            } catch (IOException ioe) {
                throw new FullTextCheckException("Error reading answer from Fulltext API", ioe);
            }
        }
        return summary;
    }

    private boolean checkResponseCode(int responseCode) throws InvalidApiKeyException {
        if (responseCode == HttpStatus.SC_UNAUTHORIZED) {
            throw new InvalidApiKeyException(APIKEY_NOT_VALID);
        } else if (responseCode == HttpStatus.SC_NOT_FOUND) {
            return false;
        } else {
            return responseCode == HttpStatus.SC_OK;
        }
    }

    /**
     * Creates FulltextSummaryCanvas from FulltextSummaryManifest
     * @param summary Fulltext AnnoPage info response object
     *
     * For translations with same dsId, LcID and pgID, multiple annoations Page will exists
     * Hence add the Annotations only.
     * Also for now, fulltextSummaryCanvas.getFTSummaryAnnoPages() will have only one annotation
     * @see <a href="https://github.com/europeana/fulltext-api/blob/7bfbb90981a760ff4d5231a0106307e6405eec51/api/src/main/java/eu/europeana/fulltext/api/service/FTService.java#L227">
     * #collectionAnnoPageInfo</a>
     *
     * @return
     */
    private Map<String, FulltextSummaryCanvas> createSummaryCanvasMap(FulltextSummaryManifest summary) {
        LinkedHashMap<String, FulltextSummaryCanvas> summaryCanvasMap = new LinkedHashMap<>();
        for (FulltextSummaryCanvas fulltextSummaryCanvas : summary.getCanvases()) {
            FulltextSummaryCanvas canvas = summaryCanvasMap.get(fulltextSummaryCanvas.getPageNumber());
            if (canvas != null) {
               canvas.addFTSummaryAnnoPage(fulltextSummaryCanvas.getFTSummaryAnnoPages().get(0));
                // in this case there will be no original language (translations)
                if (canvas.getOriginalLanguage() != null) {
                    canvas.setOriginalLanguage(null);
               }
            } else {
                summaryCanvasMap.put(fulltextSummaryCanvas.getPageNumber(), fulltextSummaryCanvas);
            }
        }
        return summaryCanvasMap;
    }

    /**
     * Generates a manifest object for IIIF v2 filled with data that is extracted from the provided JSON
     *
     * @param json        record data in JSON format
     * @return Manifest v2 object
     */
    public ManifestV2 generateManifestV2(String json) {
        long start = System.currentTimeMillis();
        Object document = com.jayway.jsonpath.Configuration.defaultConfiguration().jsonProvider().parse(json);
        ManifestV2 result = EdmManifestMappingV2.getManifestV2(settings, mediaTypes, document);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Generated in {} ms", System.currentTimeMillis() - start);
        }
        return result;
    }

    /**
     * Generates a manifest object for IIIF v2 filled with data that is extracted from the provided JSON.
     * It checks for each canvas if a full text exists; and if so, adds the link to its annotation page
     *
     * @param json        record data in JSON format
     * @param fullTextApi optional, if provided this url will be used to check if a full text is available or not
     * @return Manifest v2 object
     */
    public ManifestV2 generateManifestV2(String json, URL fullTextApi) {
        long start = System.currentTimeMillis();
        Object document = com.jayway.jsonpath.Configuration.defaultConfiguration().jsonProvider().parse(json);
        ManifestV2 result = EdmManifestMappingV2.getManifestV2(settings, mediaTypes, document);

        try {
            fillInFullTextLinksV2(result, fullTextApi);
        } catch (EuropeanaApiException ie) {
            LOG.error("Error adding full text links", ie);
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Generated in {} ms", System.currentTimeMillis() - start);
        }
        return result;
    }

    /**
     * Generates a manifest object for IIIF v3 filled with data that is extracted from the provided JSON
     *
     * @param json        record data in JSON format
     * @return Manifest v3 object
     */
    public ManifestV3 generateManifestV3(String json) {
        long start = System.currentTimeMillis();
        Object document = com.jayway.jsonpath.Configuration.defaultConfiguration().jsonProvider().parse(json);
        ManifestV3 result = EdmManifestMappingV3.getManifestV3(settings, mediaTypes, document);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Generated in {} ms ", System.currentTimeMillis() - start);
        }
        return result;
    }

    /**
     * Generates a manifest object for IIIF v3 filled with data that is extracted from the provided JSON
     * It checks for each canvas if a full text exists; and if so, adds the link to its annotation page
     *
     * @param json        record data in JSON format
     * @param fullTextApi optional, if provided this url will be used to check if a full text is available or not
     * @return Manifest v3 object
     */
    public ManifestV3 generateManifestV3(String json, URL fullTextApi) {
        long start = System.currentTimeMillis();
        Object document = com.jayway.jsonpath.Configuration.defaultConfiguration().jsonProvider().parse(json);
        ManifestV3 result = EdmManifestMappingV3.getManifestV3(settings, mediaTypes, document);
        try {
            fillInFullTextLinksV3(result, fullTextApi);
        } catch (EuropeanaApiException ie) {
            LOG.error("Error adding full text links", ie);
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Generated in {} ms ", System.currentTimeMillis() - start);
        }
        return result;
    }

    /**
     * We generate all full text links in one place, so we can raise a timeout if retrieving the necessary
     * data for all full texts is too slow.
     * From EA-2604 on, originalLanguage is available on the FulltextSummaryCanvas and copied to the AnnotationBody if
     * motivation = 'sc:painting'
     *
     */
    private void fillInFullTextLinksV2(ManifestV2 manifest, URL fullTextApi) throws EuropeanaApiException {
        Map<String, FulltextSummaryCanvas> summaryCanvasMap;
        if (manifest.getSequences() != null && manifest.getSequences().length > 0) {
            // there is always only 1 sequence
            Sequence sequence = manifest.getSequences()[0];
            // Get all the available AnnoPages incl translations from the summary endpoint of Fulltext
            String fullTextSummaryUrl = generateFullTextSummaryUrl(manifest.getEuropeanaId(), fullTextApi);
            summaryCanvasMap = getFullTextSummary(fullTextSummaryUrl);
            if (null != summaryCanvasMap) {
                // loop over canvases to add full-text link(s) to all
                for (eu.europeana.iiif.model.v2.Canvas canvas : sequence.getCanvases()) {
                    // we need to generate the same annopageId hash based on imageId
                    String apHash = GenerateUtils.derivePageId(canvas.getStartImageAnnotation().getResource().getId());
                    FulltextSummaryCanvas ftCanvas = summaryCanvasMap.get(apHash);
                    if (ftCanvas == null) {
                        // This warning can be logged for empty pages that do not have a fulltext, but if we get a lot
                        // then Record API and Fulltext API are not in sync (or the hashing algorithm changed)
                        LOG.warn("Possible inconsistent data. No fulltext annopage found for record {} page {}. Generated hash = {}",
                               manifest.getEuropeanaId(), canvas.getPageNr(), apHash);
                    } else {
                        addFulltextLinkToCanvasV2(canvas, ftCanvas);
                    }
                }
            }
        } else {
            LOG.debug("Not checking for fulltext because record doesn't have any sequences");
        }
    }

    private void addFulltextLinkToCanvasV2(eu.europeana.iiif.model.v2.Canvas canvas, FulltextSummaryCanvas summaryCanvas) {
        canvas.setOtherContent(summaryCanvas.getAnnoPageIDs().toArray(new String[0]));
        for (eu.europeana.iiif.model.v2.Annotation ann : canvas.getImages()){
            // original language will be null for translation
            if (StringUtils.equalsAnyIgnoreCase(ann.getMotivation(), "sc:painting") && summaryCanvas.getOriginalLanguage() != null){
                ann.getResource().setOriginalLanguage(summaryCanvas.getOriginalLanguage());
            }
        }
    }

    /**
     * We generate all full text links in one place, so we can raise a timeout if retrieving the necessary
     * data for all full texts is too slow.
     * From EA-2604 on, originalLanguage is available on the FulltextSummaryCanvas and copied to the AnnotationBody if
     * motivation = 'painting'
     */
    private void fillInFullTextLinksV3(ManifestV3 manifest, URL fullTextApi) throws EuropeanaApiException {
        Map<String, FulltextSummaryCanvas> summaryCanvasMap;
        eu.europeana.iiif.model.v3.Canvas[] canvases = manifest.getItems();
        if (canvases != null) {
            // Get all the available AnnoPages incl translations from the summary endpoint of Fulltext
            String fullTextSummaryUrl = generateFullTextSummaryUrl(manifest.getEuropeanaId(), fullTextApi);
            summaryCanvasMap = getFullTextSummary(fullTextSummaryUrl);
            if (null != summaryCanvasMap) {
                // loop over canvases to add full-text link(s) to all
                for (eu.europeana.iiif.model.v3.Canvas canvas : canvases) {
                    // we need to generate the same annopageId hash based on imageId
                    String apHash = GenerateUtils.derivePageId(canvas.getStartCanvasAnnotation().getBody().getId());
                    FulltextSummaryCanvas ftCanvas = summaryCanvasMap.get(apHash);
                    if (ftCanvas == null) {
                        // This warning is logged for empty pages that do not have a fulltext
                        // This happens quite often in production, so we lowered log severity from WARN to DEBUG
                        LOG.debug("Inconsistent data! No fulltext annopage found for record {} page {}. Generated hash = {}",
                                manifest.getEuropeanaId(), canvas.getPageNr(), apHash);
                    } else {
                        addFulltextLinkToCanvasV3(canvas, ftCanvas);
                    }
                }
            }
        } else {
            LOG.debug("Not checking for fulltext because record doesn't have any canvases");
        }
    }

    private void addFulltextLinkToCanvasV3(eu.europeana.iiif.model.v3.Canvas canvas, FulltextSummaryCanvas summaryCanvas) {
        List<AnnotationPage> summaryAnnoPages = new ArrayList<>();
        createFTSummaryAnnoPages(summaryAnnoPages, summaryCanvas);
        canvas.setFtSummaryAnnoPages(summaryAnnoPages.toArray(new AnnotationPage[0]));
        for (eu.europeana.iiif.model.v3.AnnotationPage ap : canvas.getItems()){
            for (eu.europeana.iiif.model.v3.Annotation ann : ap.getItems()){
                // for translations originalLanguage will be null
                if (StringUtils.equalsAnyIgnoreCase(ann.getMotivation(), "painting") && summaryCanvas.getOriginalLanguage() != null){
                    ann.getBody().setOriginalLanguage(summaryCanvas.getOriginalLanguage());
                }
            }
        }
    }

    private void createFTSummaryAnnoPages(List<AnnotationPage> summaryAnnoPages, FulltextSummaryCanvas summaryCanvas) {
        for (FulltextSummaryAnnoPage sap : summaryCanvas.getFTSummaryAnnoPages()) {
            summaryAnnoPages.add(new AnnotationPage(sap.getId(), sap.getLanguage(), sap.getTextGranularity(), sap.getSource()));
        }
    }

    /**
     * Serialize manifest to JSON-LD
     *
     * @param m manifest
     * @return JSON-LD string
     * @throws RecordParseException when there is a problem parsing
     */
    public String serializeManifest(Object m) throws RecordParseException {
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(m);
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
        if (this.recordHttpClient != null) {
            LOG.info("Closing get request http-client...");
            this.recordHttpClient.close();
        }
    }

}
