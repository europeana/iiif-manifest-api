package eu.europeana.iiif.config;

import static eu.europeana.iiif.model.ManifestDefinitions.getFulltextSummaryPath;

import eu.europeana.iiif.IIIFDefinitions;
import eu.europeana.iiif.model.ManifestDefinitions;
import eu.europeana.iiif.service.ValidateUtils;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import javax.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

/**
 * Container for all manifest settings that we load from the iiif.properties file. Note that we also have hard-coded
 * properties in the Definitions class
 * @author Patrick Ehlert
 * Created on 19-02-2018
 */
@Configuration
@Component
@PropertySource("classpath:iiif.properties")
@PropertySource(value = "classpath:iiif.user.properties", ignoreResourceNotFound = true)
public class ManifestSettings {

    private static final Logger LOG = LogManager.getLogger(ManifestSettings.class);

    @Value("${manifest-api.baseurl}")
    private String manifestApiBaseUrl;

    @Value("${manifest-api.presentation.path}")
    private String manifestApiPresentationPath;

    @Value("${manifest-api.id.placeholder}")
    private String manifestApiIdPlaceholder;

    @Value("${content-search-api.baseurl}")
    private String contentSearchBaseUrl;

    @Value("${fulltext-api.baseurl}")
    private String fullTextApiBaseUrl;

    @Value("${record-api.baseurl.internal}")
    private String recordApiBaseUrl;

    @Value("${record-api.baseurl.external}")
    private String recordApiBaseUrlExternal;

    @Value("${record-api.path}")
    private String recordApiPath;

    @Value("${thumbnail-api.baseurl}")
    private String thumbnailApiBaseUrl;

    @Value("${thumbnail-api.path}")
    private String thumbnailApiPath;

    @Value("${suppress-parse-exception}")
    private final Boolean suppressParseException = Boolean.FALSE; // default value if we run this outside of Spring (i.e. JUnit)

    @Value("${media.config}")
    private String mediaXMLConfig;

    public String getMediaXMLConfig() {
        return mediaXMLConfig;
    }

    /**
     * Get the value for Manifest API base URL
     * @return if defined, returns the value from iiif.properties
     * If not, returns the value for IIIF Europeana Base URL from IIIFDefinitions
     * if that's not found, returns the value for fulltext Base URL defined in iiif.properties
     */
    public String getManifestApiBaseUrl() {
        if (StringUtils.isNotBlank(manifestApiBaseUrl)){
            String baseUrlForManifest = ValidateUtils.formatBaseUrl(manifestApiBaseUrl);
            LOG.debug("Using formatted  Manifest base URL from iiif.properties: {}", manifestApiBaseUrl);
            return baseUrlForManifest;
        } else if (StringUtils.isNotBlank(IIIFDefinitions.IIIF_EUROPENA_BASE_URL)){
            LOG.debug("Using IIIFDefinitions value forManifest base URL: {}", IIIFDefinitions.IIIF_EUROPENA_BASE_URL);
            return IIIFDefinitions.IIIF_EUROPENA_BASE_URL;
        } else if (StringUtils.isNotBlank(fullTextApiBaseUrl)){
            String baseUrlForFullText = ValidateUtils.formatBaseUrl(fullTextApiBaseUrl);
            LOG.warn("Falling back to Fulltext API base URL {} in iiif.properties for Manifest API formatted base URL", baseUrlForFullText);
            return baseUrlForFullText;
        } else {
            LOG.error("No value found for Manifest base URL, Fulltext base URL or IIIFDefinitions.IIIF_EUROPENA_BASE_URL!");
            return null;
        }
    }

    /**
     * Get the value for Manifest API presentation path
     * @return the value from iiif.properties
     * If not defined, use the value from IIIFDefinitions
     */
    public String getManifestApiPresentationPath() {
        if (StringUtils.isNotBlank(manifestApiPresentationPath)){
            String path = ValidateUtils.formatResourcePath(manifestApiPresentationPath);
            LOG.debug("Using Presentation path found in iiif.properties: {}", path);
            return path;
        } else if (StringUtils.isNotBlank(IIIFDefinitions.PRESENTATION_PATH)){
            LOG.debug("Using Presentation path from IIIFDefinitions: {}", IIIFDefinitions.PRESENTATION_PATH);
            return IIIFDefinitions.PRESENTATION_PATH;
        } else {
            LOG.error("No value for presentation path found in iiif.properties or IIIFDefinitions!");
            return null;
        }
    }

    /**
     * Get the value for ID PLACEHOLDER
     * @return the value from iiif.properties
     * If not defined, return the hard-coded value in ManifestDefinitions
     */
    public String getManifestApiIdPlaceholder() {
        if (StringUtils.isNotBlank(manifestApiIdPlaceholder)){
            LOG.debug("Using ID PLACEHOLDER from iiif.properties: {}", manifestApiIdPlaceholder);
            return manifestApiIdPlaceholder;
        } else if (StringUtils.isNotBlank(ManifestDefinitions.ID_PLACEHOLDER)){
            LOG.debug("Using ID PLACEHOLDER hard-coded in ManifestDefinitions: {}", ManifestDefinitions.ID_PLACEHOLDER);
            return IIIFDefinitions.PRESENTATION_PATH;
        } else {
            LOG.error("No value found for ID_PLACEHOLDER!");
            return null;
        }
    }

    /**
     * Get the value for Content Search Base URL
     * @return the value from iiif.properties
     * If not defined, return the Fulltext Base URL from iiif.properties
     * TODO check if this is the right order, or if we could also fallback to Manifest API Base URL?
     */
    public String getContentSearchBaseUrl() {
        if (StringUtils.isNotBlank(contentSearchBaseUrl)){
            String baseUrl = ValidateUtils.formatBaseUrl(contentSearchBaseUrl);
            LOG.debug("Using Content Search Base URL from iiif.properties: {}", baseUrl);
            return baseUrl;
        } else if (StringUtils.isNotBlank(fullTextApiBaseUrl)){
            LOG.debug("Using Fulltext Base URL for Context Search Base URL: {}", fullTextApiBaseUrl);
            return fullTextApiBaseUrl;
        } else {
            LOG.error("No value found for Content Search Base URL!");
            return null;
        }
    }

    /**
     * @return Fulltext Base URL defined in iiif.properties from where we can do a HEAD request to check if a full-text is available
     * This value is not used while presenting the manifest output and only used to call api.
     */
    public String getFullTextApiBaseUrl() {
        return ValidateUtils.formatBaseUrl(fullTextApiBaseUrl);
    }

    /**
     * @return Record API Base URL from where we should retrieve record json data.This is URL includes the internal rout
     *  to search and record api.
     */
    public String getRecordApiBaseUrl() {
        return ValidateUtils.formatBaseUrl(recordApiBaseUrl);
    }

    public String getRecordApiBaseUrlExternal() {
        return ValidateUtils.formatBaseUrl(recordApiBaseUrlExternal);
    }

    /**
     * @return Record API resource path (should be appended to the record API base url)
     */
    public String getRecordApiPath() {
        return ValidateUtils.formatResourcePath(recordApiPath);
    }

    /**
     * @return Record API endpoint: record API Base URL + record API resource path, this endpoint
     * uses the external url of the record api and should be used accordingly
     */
    public String getRecordApiEndpoint() {
        return getRecordApiBaseUrlExternal() + getRecordApiPath();
    }

    /**
     * @return Thumbnail url, concatenates base URL + path to endpoint; used to create canvas thumbnails
     */
    public String getThumbnailApiUrl() {
        return ValidateUtils.formatBaseUrl(thumbnailApiBaseUrl) + ValidateUtils.formatResourcePath(thumbnailApiPath);
    }

    /**
     * For production we want to suppress exceptions that arise from parsing record data, but for testing/debugging we
     * want to see those exceptions
     */
    public Boolean getSuppressParseException() {
        return suppressParseException;
    }

    /**
     * Base URL used for generation the various types of IDs
     */
    public String getIIIFPresentationBaseUrl(){
        return getManifestApiBaseUrl() + getManifestApiPresentationPath() + getManifestApiIdPlaceholder();
    }

    /**
     * Url of all manifest IDs but with placeholder for the actual dataset and record ID
     */
    public String getManifestIdTemplate() {
        return getIIIFPresentationBaseUrl() + "/manifest";
    }

    /**
     * Url of a sequence IDs but with placeholder for the actual dataset and record ID. Note that there the order number
     * is not included here (so first sequence should be /sequence/s1)
     */
    public String getSequenceIDTemplate() {
        return getIIIFPresentationBaseUrl() + "/sequence/s";
    }

    /**
     * Url for canvas IDs but with placeholder for the actual dataset and record ID Note that there the order number is
     * not included here (so first canvas should be /canvas/p1)
     */
    public String getCanvasIDTemplate(){
        return getIIIFPresentationBaseUrl() + "/canvas/p";
    }

    /**
     * Create the IIIF manifest ID
     *
     * @param europeanaId consisting of dataset ID and record ID separated by a slash (string should have a leading
     *                    slash and not trailing slash)
     * @return string containing the IIIF manifest ID
     */
    public String getManifestId(String europeanaId) {
        return getManifestIdTemplate().replace(getManifestApiIdPlaceholder(), europeanaId);
    }

    /**
     * Create a canvas ID
     *
     * @param europeanaId consisting of dataset ID and record ID separated by a slash (string should have a leading
     *                    slash and not trailing slash)
     * @param order       number
     * @return String containing the canvas ID
     */
    public String getCanvasId(String europeanaId, int order) {
        return getCanvasIDTemplate().replace(getManifestApiIdPlaceholder(), europeanaId).concat(
            Integer.toString(order));
    }

    /**
     * Create a dataset ID (datasets information are part of the manifest)
     *
     * @param europeanaId consisting of dataset ID and record ID separated by a slash (string should have a leading
     *                    slash and not trailing slash)
     * @return string containing the dataset ID consisting of a base url, Europeana ID and postfix (rdf/xml, json or
     * json-ld)
     */
    public String getDatasetId(String europeanaId, String postFix) {
        return getRecordApiEndpoint() + europeanaId + postFix;
    }

    /**
     * Get the Content Search URL used in the Manifest Service Description
     * @param europeanaId
     * @return URL built from Content search base URL, manifest API presentation path, Europeana ID and Fulltext search path
     */
    public String getContentSearchURL(String europeanaId){
        return getContentSearchBaseUrl() + getManifestApiPresentationPath() + europeanaId + IIIFDefinitions.FULLTEXT_SEARCH_PATH;
    }


    /**
     * Note: this does not work when running the exploded build from the IDE because the values in the build.properties
     * are substituted only in the .war file. It returns 'default' in that case.
     * @return String containing app version, used in the eTag SHA hash generation
     */
    public String getAppVersion() {
        InputStream resourceAsStream = this.getClass().getResourceAsStream("/build.properties");
        if (resourceAsStream == null) {
            return "no version set";
        }
        try {
            Properties buildProperties = new Properties();
            buildProperties.load(resourceAsStream);
            return buildProperties.getProperty("info.app.version");
        } catch (IOException | RuntimeException e) {
            LOG.warn("Error reading version from build.properties file", e);
            return "no version set";
        }
    }

    @PostConstruct
    private void logImportantSettings() {
        LOG.info("Manifest settings:");
        if (StringUtils.isNotBlank(manifestApiBaseUrl)){
            LOG.info("  Manifest API Base Url set to {} ", manifestApiBaseUrl);
        }

        LOG.info("  Manifest API presentation path set to {} ", getManifestApiPresentationPath());

        if (StringUtils.isNotBlank(manifestApiIdPlaceholder)){
            LOG.info("  Manifest API ID placeholder set to {} ", manifestApiIdPlaceholder);
        }
        if (StringUtils.isNotBlank(contentSearchBaseUrl)){
            LOG.info(" Content Search API base URL set to {} ", contentSearchBaseUrl);
        }
        LOG.info("  Record API endpoint = {} ", getRecordApiEndpoint());
        LOG.info("  Thumbnail API Url = {} ", this.getThumbnailApiUrl());
        LOG.info("  Full-Text Summary Url = {}{} ", this.getFullTextApiBaseUrl(), getFulltextSummaryPath("/<collectionId>/<itemId>"));
        LOG.info("  Suppress parse exceptions = {}", this.getSuppressParseException());
    }

}
