package eu.europeana.iiif;

import org.apache.logging.log4j.LogManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.cloud.netflix.hystrix.dashboard.EnableHystrixDashboard;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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
@PropertySource("classpath:iiif.properties")
@PropertySource(value = "classpath:iiif.user.properties", ignoreResourceNotFound = true)
public class ManifestApplication extends SpringBootServletInitializer {

    @Value("${features.security.enable}")
    private boolean securityEnable;

    @Value("${security.config.ipRanges}")
    private String ipRanges;

    /**
     * Setup CORS for all requests
     * @return WebMvcConfigurer that exposes CORS headers
     */
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**").allowedOrigins("*").maxAge(1000)
                        .exposedHeaders("Allow, Vary, ETag, Last-Modified");
            }
        };
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

    @EnableWebSecurity
    @Configuration
    class SecurityConfig extends WebSecurityConfigurerAdapter {

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            if(securityEnable) {
                http.authorizeRequests()
                        .antMatchers("/presentation/**").access(createHasIpRangeExpression());
            }
        }

        /**
         * creates the string for authorizing request for the provided ipRanges
         */
        private String createHasIpRangeExpression() {
            List<String> validIps = Arrays.asList(ipRanges.split("\\s*,\\s*"));
            return validIps.stream()
                    .collect(Collectors.joining("') or hasIpAddress('", "hasIpAddress('", "')"));
        }
    }
    
}
