package eu.europeana.iiif.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

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

    @Value("${record-api.baseurl}")
    private String recordApiBaseUrl;
    @Value("${record-api.path}")
    private String recordApiPath;

    @Value("${fulltext-api.baseurl}")
    private String fullTextApiBaseUrl;

    @Value("${fulltext-api.path}")
    private String fullTextApiPath;

    @Value("${fulltext.summary.path}")
    private String fullTextSummaryPath;

    @Value("${suppress-parse-exception}")
    private Boolean suppressParseException = Boolean.FALSE; // default value if we run this outside of Spring (i.e. JUnit)

    /**
     * @return base url from where we should retrieve record json data
     */
    public String getRecordApiBaseUrl() {
        return recordApiBaseUrl;
    }

    /**
     * @return SummaryManifest resource path (should be appended to the record API base url)
     */
    public String getRecordApiPath() {
        return recordApiPath;
    }

    /**
     * @return base url from where we can do a HEAD request to check if a full-text is available
     */
    public String getFullTextApiBaseUrl() {
        return fullTextApiBaseUrl;
    }

    /**
     * @return Fulltext Api AnnoPage path (note that <collectionId>, <itemId>, and <pageId> should be replaced with actual values
     */
    public String getFullTextApiPath() {
        return fullTextApiPath;
    }

    /**
     * @return Fulltext Api AnnoPage summary path (note that <collectionId> and <itemId> should be replaced with actual values
     */
    public String getFullTextSummaryPath() {
        return fullTextSummaryPath;
    }

    /**
     * For production we want to suppress exceptions that arise from parsing record data, but for testing/debugging we
     * want to see those exceptions
     */
    public Boolean getSuppressParseException() {
        return suppressParseException;
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
        LOG.info("  SummaryManifest API Url = {}{} ", this.getRecordApiBaseUrl(), this.getRecordApiPath());
        LOG.info("  Full-Text API Url = {}{} ", this.getFullTextApiBaseUrl(), this.getFullTextApiPath());
        LOG.info("  Suppress parse exceptions = {}", this.getSuppressParseException());
    }
}
