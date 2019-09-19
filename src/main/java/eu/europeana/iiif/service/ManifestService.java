package eu.europeana.iiif.service;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider;
import com.jayway.jsonpath.spi.json.JsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import com.jayway.jsonpath.spi.mapper.MappingProvider;
import eu.europeana.iiif.config.ManifestSettings;
import eu.europeana.iiif.model.v2.ManifestV2;
import eu.europeana.iiif.model.v2.Sequence;
import eu.europeana.iiif.model.v3.AnnotationPage;
import eu.europeana.iiif.model.v3.ManifestV3;
import eu.europeana.iiif.service.exception.*;
import ioinformarics.oss.jackson.module.jsonld.JsonldModule;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
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

    // create a single objectMapper for efficiency purposes (see https://github.com/FasterXML/jackson-docs/wiki/Presentation:-Jackson-Performance)
    private static ObjectMapper mapper = new ObjectMapper();

    private ManifestSettings settings;
    private CloseableHttpClient httpClient;

    public ManifestService(ManifestSettings settings) {
        this.settings = settings;

        // configure http client
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(200);
        cm.setDefaultMaxPerRoute(100);
        httpClient = HttpClients.custom().setConnectionManager(cm).build();

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
        mapper.registerModule(new JsonldModule());
    }

    protected ObjectMapper getJsonMapper() {
        return mapper;
    }

    /**
     * Return record information in Json format from the default configured Record API
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
     * Return record information in Json format from an instance of the Record API
     *
     * @param recordId Europeana record id in the form of "/datasetid/recordid" (so with leading slash and without trailing slash)
     * @param wsKey api key to send to record API
     * @param recordApiUrl if not null we will use the provided URL as the address of the Record API instead of the default configured address
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
            try (CloseableHttpResponse response = httpClient.execute(new HttpGet(recordUrl))) {
                int responseCode = response.getStatusLine().getStatusCode();
                LOG.debug("Record request: {}, status code = {}", recordId, responseCode);
                if (responseCode == HttpStatus.SC_UNAUTHORIZED) {
                    throw new InvalidApiKeyException("API key is not valid");
                } else if (responseCode == HttpStatus.SC_NOT_FOUND) {
                    throw new RecordNotFoundException("Record with id '"+recordId+"' not found");
                } else if (responseCode != HttpStatus.SC_OK) {
                    throw new RecordRetrieveException("Error retrieving record: "+response.getStatusLine().getReasonPhrase());
                }

                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    result = EntityUtils.toString(entity);
                    LOG.debug("Record request: {}, response = {}", recordId, result);
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
        StringBuilder url;
        if (fullTextApiUrl == null) {
            url = new StringBuilder(settings.getFullTextApiBaseUrl());
        } else {
            url = new StringBuilder(fullTextApiUrl.toString());
        }
        String path = settings.getFullTextApiPath().replace("/<collectionId>/<itemId>", europeanaId).replace("<pageId>", pageId);
        url.append(path);
        return url.toString();
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
    Boolean existsFullText(String fullTextUrl) throws IIIFException {
        Boolean result;
        try {
            try (CloseableHttpResponse response = httpClient.execute(new HttpHead(fullTextUrl))) {
                int responseCode = response.getStatusLine().getStatusCode();
                LOG.debug("Full-Text head request: {}, status code = {}", fullTextUrl, responseCode);
                if (responseCode == HttpStatus.SC_UNAUTHORIZED) {
                    throw new InvalidApiKeyException("API key is not valid");
                } else if (responseCode == HttpStatus.SC_NOT_FOUND) {
                    result = Boolean.FALSE;
                } else if (responseCode == HttpStatus.SC_OK) {
                    result = Boolean.TRUE;
                } else {
                    // TODO when hysterix is enabled again, we can simply throw an error again
                    //throw new FullTextCheckException("Error checking if full text exists: "+response.getStatusLine().getReasonPhrase());
                    LOG.error(String.format("Error checking if full text exists: %s", response.getStatusLine().getReasonPhrase()));
                    result = null;
                }
            }
        } catch (IOException e) {
            // TODO when hysterix is enabled again, we can simply throw an error again
            //throw new FullTextCheckException("Error checking if full text exists", e);
            LOG.error("Error checking if full text exists", e);
            result = null;
        }
        return result;
    }

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
        ManifestV2 result = EdmManifestMapping.getManifestV2(settings, document);

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
        ManifestV3 result = EdmManifestMapping.getManifestV3(settings, document);

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
            Sequence s = manifest.getSequences()[0];

            // We don't want to check for all images if they have a fulltext because that takes too long
            // Instead we use only do a fulltext exists check for the canvas returned by findMatchingCanvas()
            String canvasId = findMatchingCanvas(manifest.getIsShownBy(), s.getCanvases(), null);
            if (canvasId != null) {
                // do the actual fulltext check
                String fullTextUrl = generateFullTextUrl(manifest.getEuropeanaId(), canvasId, fullTextApi);
                if (Boolean.TRUE.equals(existsFullText(fullTextUrl))) {
                    // loop over canvases to add full-text link to all
                    for (eu.europeana.iiif.model.v2.Canvas c : s.getCanvases()) {
                        String ftUrl = generateFullTextUrl(manifest.getEuropeanaId(), Integer.toString(c.getPageNr()),
                                fullTextApi);
                        // always 1 value in array
                        String[] ft = new String[1];
                        ft[0] = ftUrl;
                        c.setOtherContent(ft);
                    }
                }
            }
        }
    }

    /**
     * Find the canvas that has an image with resource id that matches the provided edmIsShownBy.
     * We do this because the number of this canvas will be used in doing the existsFullText check
     * Note that we use this method both for either v2 and v3 (depending on which is parameter is set)
     */
    private String findMatchingCanvas(String edmIsShownBy, eu.europeana.iiif.model.v2.Canvas[] canvasesV2, eu.europeana.iiif.model.v3.Canvas[] canvasesV3) {
        String result = null;
        if (StringUtils.isEmpty(edmIsShownBy)) {
            LOG.trace ("No full-text check because edmIsShownBy is empty");
            return result;
        } else if (canvasesV2 == null && canvasesV3 == null) {
            LOG.trace ("No full-text check because there are no canvases");
            return result;
        }

        if (canvasesV2 != null) {
            for (eu.europeana.iiif.model.v2.Canvas c : canvasesV2) {
                String annotationBodyId = c.getImages()[0].getResource().getId();
                if (edmIsShownBy.equals(annotationBodyId)) {
                    result = Integer.toString(c.getPageNr());
                    LOG.trace("Canvas {} matches with edmIsShownBy", result);
                    break;
                }
            }
        } else {
            for (eu.europeana.iiif.model.v3.Canvas c : canvasesV3) {
                String annotationBodyId = c.getItems()[0].getItems()[0].getBody().getId();
                if (edmIsShownBy.equals(annotationBodyId)) {
                    result = Integer.toString(c.getPageNr());
                    LOG.trace("Canvas {} matches with edmIsShownBy", result);
                    break;
                }
            }
        }
        if (result == null) {
            LOG.trace("No full-text check because there was no match with edmIsShownBy");
        }
        return result;
    }

    /**
     * We generate all full text links in one place, so we can raise a timeout if retrieving the necessary
     * data for all full texts is too slow.
     */
    private void fillInFullTextLinksV3(ManifestV3 manifest, URL fullTextApi) throws IIIFException {
        eu.europeana.iiif.model.v3.Canvas[] canvases = manifest.getItems();
        if (canvases != null) {

            // We don't want to check for all images if they have a fulltext because that takes too long
            // Instead we use only do a fulltext exists check for the canvas returned by findMatchingCanvas()
            String canvasId = findMatchingCanvas(manifest.getIsShownBy(), null, canvases);
            if (canvasId != null) {
                // do the actual fulltext check
                String fullTextUrl = generateFullTextUrl(manifest.getEuropeanaId(), canvasId, fullTextApi);
                if (Boolean.TRUE.equals(existsFullText(fullTextUrl))) {
                    // loop over canvases to add an extra annotation page
                    for (eu.europeana.iiif.model.v3.Canvas c : canvases) {
                        String ftUrl = generateFullTextUrl(manifest.getEuropeanaId(), Integer.toString(c.getPageNr()),
                                fullTextApi);
                        addFullTextAnnotationPageV3(c, ftUrl);
                    }
                }
            }
        }
    }

    /**
     *  If there is a full text available we have to add a new annotation page with just the full text url as id
     */
    private void addFullTextAnnotationPageV3(eu.europeana.iiif.model.v3.Canvas c, String fullTextUrl) {
        List<AnnotationPage> aps = new ArrayList<>();

        if (c.getItems() != null && c.getItems().length > 0) {
            aps.addAll(Arrays.asList(c.getItems()));
        }
        aps.add(new AnnotationPage(fullTextUrl));
        c.setItems(aps.toArray(new AnnotationPage[0]));
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
            throw new RecordParseException("Error serializing data: "+e.getMessage(), e);
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
        if (this.httpClient != null) {
            LOG.info("Closing http-client...");
            this.httpClient.close();
        }
    }

}
