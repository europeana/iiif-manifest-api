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
import eu.europeana.iiif.model.info.FulltextSummary;
import eu.europeana.iiif.model.info.FulltextSummaryCanvas;
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

import static eu.europeana.iiif.model.Definitions.getFulltextSummaryPath;

/**
 * Service that loads record data, uses that to generate a Manifest object and serializes the manifest in JSON-LD
 *
 * @author Patrick Ehlert
 * Created on 06-12-2017
 */
@Service
public class ManifestService {

    private static final Logger LOG              = LogManager.getLogger(ManifestService.class);
    private static final String APIKEY_NOT_VALID = "API key is not valid";

    // create a single objectMapper for efficiency purposes (see https://github.com/FasterXML/jackson-docs/wiki/Presentation:-Jackson-Performance)
    private static ObjectMapper mapper = new ObjectMapper();

    private final ManifestSettings    settings;
    private final CloseableHttpClient httpClient;


    public ManifestService(ManifestSettings settings) {
        this.settings = settings;

        // configure http client
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(200);
        cm.setDefaultMaxPerRoute(100);

        //configure for get requests to Record API
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
     * Return record information in Json format from the default configured Record API
     *
     * @param recordId Europeana record id in the form of "/datasetid/recordid" (so with leading slash and without trailing slash)
     * @param wsKey    api key to send to record API
     * @return record information in json format
     * @throws IIIFException (
     *                       IllegalArgumentException if a parameter has an illegal format,
     *                       InvalidApiKeyException if the provide key is not valid,
     *                       RecordNotFoundException if there was a 404,
     *                       RecordRetrieveException on all other problems)
     */
    public String getRecordJson(String recordId, String wsKey) throws IIIFException {
        return getRecordJson(recordId, wsKey, null);
    }

    /**
     * Return record information in Json format from an instance of the Record API
     *
     * @param recordId     Europeana record id in the form of "/datasetid/recordid" (so with leading slash and without trailing slash)
     * @param wsKey        api key to send to record API
     * @param recordApiUrl if not null we will use the provided URL as the address of the Record API instead of the default configured address
     * @return record information in json format     *
     * @throws IIIFException (
     *                       IllegalArgumentException if a parameter has an illegal format,
     *                       InvalidApiKeyException if the provide key is not valid,
     *                       RecordNotFoundException if there was a 404,
     *                       RecordRetrieveException on all other problems)
     */
    public String getRecordJson(String recordId, String wsKey, URL recordApiUrl) throws IIIFException {
        String result = null;

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
                    throw new InvalidApiKeyException(APIKEY_NOT_VALID);
                } else if (responseCode == HttpStatus.SC_NOT_FOUND) {
                    throw new RecordNotFoundException("Record with id '" + recordId + "' not found");
                } else if (responseCode != HttpStatus.SC_OK) {
                    LOG.error("Error retrieving record {}, reason {}", recordId, response.getStatusLine().getReasonPhrase());
                    throw new RecordRetrieveException("Error retrieving record: " + response.getStatusLine().getReasonPhrase());
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
     *
     * @param fullTextApiUrl optional, if not specified then the default Full-Text API specified in .properties is used
     */
    String generateFullTextSummaryUrl(String europeanaId, URL fullTextApiUrl) {
        return fullTextBaseUrl(fullTextApiUrl) + getFulltextSummaryPath(europeanaId);
    }

    private String fullTextBaseUrl(URL fullTextApiUrl) {
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
     *
     * @param fullTextUrl url to FullText Summary endpoint
     * @return Map with key PageId and as value an array of AnnoPage ID strings
     * @throws IIIFException when there is an error retrieving the fulltext AnnoPage summary
     */
    Map<String, FulltextSummaryCanvas> getFullTextSummary(String fullTextUrl) throws IIIFException {
        FulltextSummary summary = null;
        boolean         result;
        try {
            try (CloseableHttpResponse response = httpClient.execute(new HttpGet(fullTextUrl))) {
                int responseCode = response.getStatusLine().getStatusCode();
                LOG.debug("Full-Text summary GET request: {}, status code = {}", fullTextUrl, responseCode);
                if (responseCode == HttpStatus.SC_UNAUTHORIZED) {
                    throw new InvalidApiKeyException(APIKEY_NOT_VALID);
                } else if (responseCode == HttpStatus.SC_NOT_FOUND) {
                    result = false;
                } else {
                    result = responseCode == HttpStatus.SC_OK;
                }
                HttpEntity entity = response.getEntity();
                if (result && entity != null) {
                    summary = getJsonMapper().readValue(EntityUtils.toString(entity), FulltextSummary.class);
                    EntityUtils.consume(entity); // make sure entity is consumed fully so connection can be reused
                } else {
                    LOG.warn("No response from Fulltext API received");
                }
            }
        } catch (IOException e) {
            LOG.error("Error connecting to Fulltext API at {}", fullTextUrl, e);
            result = false;
        }
        if (result && null != summary) {
            return createSummaryCanvasMap(summary);
        } else {
            return null;
        }
    }

    private Map<String, FulltextSummaryCanvas> createSummaryCanvasMap(FulltextSummary summary) {
        LinkedHashMap<String, FulltextSummaryCanvas> summaryCanvasMap = new LinkedHashMap<>();
        for (FulltextSummaryCanvas fulltextSummaryCanvas : summary.getCanvases()) {
            summaryCanvasMap.put(fulltextSummaryCanvas.getPageNumber(), fulltextSummaryCanvas);
        }
        return summaryCanvasMap;
    }

    /**
     * Generates a manifest object for IIIF v2 filled with data that is extracted from the provided JSON
     *
     * @param json        record data in JSON format
     * @param addFullText if true then for each canvas we will check if a full text exists and add the link to it's
     *                    annotation page
     * @param fullTextApi optional, if provided this url will be used to check if a full text is available or not
     * @return Manifest v2 object
     */
    public ManifestV2 generateManifestV2(String json, boolean addFullText, URL fullTextApi) {
        long start = System.currentTimeMillis();
        Object document = com.jayway.jsonpath.Configuration.defaultConfiguration().jsonProvider().parse(json);
        ManifestV2 result = EdmManifestMappingV2.getManifestV2(document);

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
     *
     * @param json        record data in JSON format
     * @param addFullText if true then for each canvas we will check if a full text exists and add the link to it's
     *                    annotation page
     * @param fullTextApi optional, if provided this url will be used to check if a full text is available or not
     * @return Manifest v3 object
     */
    public ManifestV3 generateManifestV3(String json, boolean addFullText, URL fullTextApi) {
        long start = System.currentTimeMillis();
        Object document = com.jayway.jsonpath.Configuration.defaultConfiguration().jsonProvider().parse(json);
        ManifestV3 result = EdmManifestMappingV3.getManifestV3(document);

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
     * From EA-2604 on, originalLanguage is available on the FulltextSummaryCanvas and copied to the AnnotationBody if
     * motivation = 'sc:painting'
     *
     */
    private void fillInFullTextLinksV2(ManifestV2 manifest, URL fullTextApi) throws IIIFException {
        if (manifest.getSequences() != null && manifest.getSequences().length > 0) {
            // there is always only 1 sequence
            Sequence sequence = manifest.getSequences()[0];
            // Get all the available AnnoPages incl translations from the summary endpoint of Fulltext
            String fullTextSummaryUrl = generateFullTextSummaryUrl(manifest.getEuropeanaId(), fullTextApi);
            Map<String, FulltextSummaryCanvas> summaryCanvasMap = getFullTextSummary(fullTextSummaryUrl);
            if (null != summaryCanvasMap) {
                // loop over canvases to add full-text link(s) to all
                for (eu.europeana.iiif.model.v2.Canvas canvas : sequence.getCanvases()) {
                    addFulltextLinkToCanvasV2(manifest.getEuropeanaId(), canvas,
                            summaryCanvasMap.get(Integer.toString(canvas.getPageNr())));
                }
            }
        }
    }

    private void addFulltextLinkToCanvasV2(String recordId, eu.europeana.iiif.model.v2.Canvas canvas, FulltextSummaryCanvas summaryCanvas) {
        if (summaryCanvas == null) {
            LOG.warn("Inconsistent data! No Fulltext annotation page found for record {} page {}", recordId, canvas.getPageNr());
            return;
        }

        canvas.setOtherContent(summaryCanvas.getAnnoPageIDs().toArray(new String[0]));
        for (eu.europeana.iiif.model.v2.Annotation ann : canvas.getImages()){
            if (StringUtils.equalsAnyIgnoreCase(ann.getMotivation(), "sc:painting")){
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
    private void fillInFullTextLinksV3(ManifestV3 manifest, URL fullTextApi) throws IIIFException {
        eu.europeana.iiif.model.v3.Canvas[] canvases = manifest.getItems();
        if (canvases != null) {
            // Get all the available AnnoPages incl translations from the summary endpoint of Fulltext
            String fullTextSummaryUrl = generateFullTextSummaryUrl(manifest.getEuropeanaId(), fullTextApi);
            Map<String, FulltextSummaryCanvas> summaryCanvasMap = getFullTextSummary(fullTextSummaryUrl);
            if (null != summaryCanvasMap) {
                // loop over canvases to add full-text link(s) to all
                for (eu.europeana.iiif.model.v3.Canvas canvas : canvases) {
                    addFulltextLinkToCanvasV3(manifest.getEuropeanaId(), canvas,
                            summaryCanvasMap.get(Integer.toString(canvas.getPageNr())));
                }
            }
        }
    }

    private void addFulltextLinkToCanvasV3(String recordId, eu.europeana.iiif.model.v3.Canvas canvas, FulltextSummaryCanvas summaryCanvas) {
        if (summaryCanvas == null) {
            LOG.warn("Inconsistent data! No Fulltext annotation page found for record {} page {}", recordId, canvas.getPageNr());
            return;
        }

        List<AnnotationPage> summaryAnnoPages = new ArrayList<>();
        Map<String, String> annoPageIDLang = summaryCanvas.getAnnoPageIDLang();
        for (Map.Entry<String, String> entry : annoPageIDLang.entrySet()) {
            summaryAnnoPages.add(new AnnotationPage(entry.getKey(), entry.getValue()));
        }
        canvas.setAnnotations(summaryAnnoPages.toArray(new AnnotationPage[0]));
        for (eu.europeana.iiif.model.v3.AnnotationPage ap : canvas.getItems()){
            for (eu.europeana.iiif.model.v3.Annotation ann : ap.getItems()){
                if (StringUtils.equalsAnyIgnoreCase(ann.getMotivation(), "painting")){
                    ann.getBody().setOriginalLanguage(summaryCanvas.getOriginalLanguage());
                }
            }
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
        if (this.httpClient != null) {
            LOG.info("Closing get request http-client...");
            this.httpClient.close();
        }
    }

}
