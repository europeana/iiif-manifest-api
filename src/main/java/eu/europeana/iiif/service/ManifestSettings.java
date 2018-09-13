package eu.europeana.iiif.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnResource;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
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

    @Value("${suppress-parse-exception}")
    private Boolean suppressParseException = Boolean.FALSE; // default value if we run this outside of Spring

    @Value("${canvas.height}")
    private Integer canvasHeight;
    @Value("${canvas.width}")
    private Integer canvasWidth;

    @Autowired
    private Environment environment;

    /**
     * @return base url from where we should retrieve record json data
     */
    public String getRecordApiBaseUrl() {
        return recordApiBaseUrl;
    }

    /**
     * @return Record resource path (should be appended to the record API base url)
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
     * @return record resource path (note that <collectionId>, <itemId>, and <pageId> should be replaced with actual values
     */
    public String getFullTextApiPath() {
        return fullTextApiPath;
    }

    /**
     * For production we want to suppress exceptions that arise from parsing record data, but for testing/debugging we
     * want to see those exceptions
     */
    public Boolean getSuppressParseException() {
        return suppressParseException;
    }

    /**
     * @return Integer containing canvas height
     */
    public Integer getCanvasHeight() {
        return canvasHeight;
    }

    /**
     * @return Integer containing canvas width
     */
    public Integer getCanvasWidth() {
        return canvasWidth;
    }

    /**
     * Note: this does not work when running the exploded build from the IDE because the values in the build.properties
     * are substituted only in the .war file. It returns 'default' in that case.
     * @return String containing app version, used in the eTag SHA hash generation
     */
    public String getAppVersion() {
        Properties buildProperties = new Properties();
        InputStream resourceAsStream = this.getClass().getResourceAsStream("/build.properties");
        try {
            buildProperties.load(resourceAsStream);
            return environment.getProperty("info.app.version");
        } catch (Exception e) {
            return "default";
        }
    }

    @PostConstruct
    private void logImportantSettings() {
        LOG.info("Manifest settings:");
        LOG.info("  Record API Url = {}{} ", this.getRecordApiBaseUrl(), this.getRecordApiPath());
        LOG.info("  Full-Text API Url = {}{} ", this.getFullTextApiBaseUrl(), this.getFullTextApiPath());
        LOG.info("  Suppress parse exceptions = {}", this.getSuppressParseException());
    }
}
