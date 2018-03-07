package eu.europeana.iiif.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

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
    @Value("${suppress-parse-exception}")
    private Boolean suppressParseException = false; // default value if we run this outside of Spring

    @Value("${canvas.height}")
    private Integer canvasHeight;
    @Value("${canvas.width}")
    private Integer canvasWidth;

    /**
     * @return base url from where we should retrieve record json data
     */
    public String getRecordApiBaseUrl() {
        return recordApiBaseUrl;
    }

    /**
     * @return Record resource path (should be appended to the record API base url)
     */
    public String getRecordApiApiPath() {
        return recordApiPath;
    }

    /**
     * For production we want to suppress exceptions that arise from parsing record data, but for testing/debugging we
     * want to see those exceptions
     * @return
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

    @PostConstruct
    private void logImportantSettings() {
        LOG.info("Manifest settings:");
        LOG.info("  Record API Url = {}{} ", this.getRecordApiBaseUrl(), this.getRecordApiApiPath());
        LOG.info("  Suppress parse exceptions = {}", this.getSuppressParseException());
    }
}
