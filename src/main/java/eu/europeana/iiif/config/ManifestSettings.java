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

import static eu.europeana.iiif.model.Definitions.getFulltextSummaryPath;

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

    @Value("${suppress-parse-exception}")
    private final Boolean suppressParseException = Boolean.FALSE; // default value if we run this outside of Spring (i.e. JUnit)

    /**
     * @return base url from where we should retrieve record json data
     */
    public String getRecordApiBaseUrl() {
        return recordApiBaseUrl;
    }

    /**
     * @return FulltextSummary resource path (should be appended to the record API base url)
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
        LOG.info("  Record API Url = {}{} ", this.getRecordApiBaseUrl(), this.getRecordApiPath());
        LOG.info("  Full-Text Summary Url = {}{} ", this.getFullTextApiBaseUrl(), getFulltextSummaryPath("<collectionId>/<itemId>"));
        LOG.info("  Suppress parse exceptions = {}", this.getSuppressParseException());
    }
}
