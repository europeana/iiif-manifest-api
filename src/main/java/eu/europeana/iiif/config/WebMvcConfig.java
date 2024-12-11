package eu.europeana.iiif.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Setup CORS for all requests and setup default Content-type
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * Setup CORS for all GET, HEAD and OPTIONS, requests.
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*")
                .allowedMethods("*")
                .allowedHeaders("*")
                .exposedHeaders("Allow, Vary, ETag, Last-Modified")
                .allowCredentials(false)
                .maxAge(1000L); // in seconds
    }

}