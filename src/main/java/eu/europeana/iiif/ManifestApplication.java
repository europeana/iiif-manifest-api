package eu.europeana.iiif;

import org.apache.logging.log4j.LogManager;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.Collections;

/**
 * Main application
 *
 * @author Patrick Ehlert
 * Created on 6-12-2017
 */
@SpringBootApplication
//@EnableHystrixDashboard
//@EnableCircuitBreaker
@PropertySource(value = "classpath:build.properties", ignoreResourceNotFound = true)
public class ManifestApplication extends SpringBootServletInitializer {


    /**
     * Setup CORS for all requests
     *
     * For some reason the default Spring-Boot way of configuring Cors via WebMvcConfigurer class doesn't
     * work for Swagger, so we configure it here
     */
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedHeaders(Collections.singletonList("*"));
        config.setAllowedOrigins(Collections.singletonList("*"));
        config.setAllowedMethods(Collections.singletonList("*"));
        config.setExposedHeaders(Collections.singletonList("Allow, Vary, ETag, Last-Modified"));
        config.setMaxAge(1000L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }

    /**
     * This method is called when starting as a Spring-Boot application (e.g. from your IDE)
     * @param args main application paramaters
     */
    @SuppressWarnings("squid:S2095") // to avoid sonarqube false positive (see https://stackoverflow.com/a/37073154/741249)
    public static void main(String[] args) {
        LogManager.getLogger(ManifestApplication.class).info("CF_INSTANCE_INDEX  = {}, CF_INSTANCE_GUID = {}, CF_INSTANCE_IP  = {}",
                System.getenv("CF_INSTANCE_INDEX"), System.getenv("CF_INSTANCE_GUID"), System.getenv("CF_INSTANCE_IP"));
        SpringApplication.run(ManifestApplication.class, args);
    }
    
}
