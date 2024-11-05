package eu.europeana.iiif;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.PropertySource;

/**
 * Main application
 *
 * @author Patrick Ehlert
 * Created on 6-12-2017
 */
@SpringBootApplication(scanBasePackages = {"eu.europeana.iiif", "eu.europeana.api"})  // needed to find EuropeanaApiErrorController in api-commons
@PropertySource(value = "classpath:build.properties", ignoreResourceNotFound = true)
public class ManifestApplication extends SpringBootServletInitializer {




    /**
     * This method is called when starting as a Spring-Boot application (e.g. from your IDE)
     * @param args main application paramaters
     */
    public static void main(String[] args) {
        SpringApplication.run(ManifestApplication.class, args);
    }
    
}
