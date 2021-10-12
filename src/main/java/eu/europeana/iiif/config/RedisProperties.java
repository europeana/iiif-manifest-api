package eu.europeana.iiif.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by luthien on 04/10/2021.
 */
@Configuration
@Component
@ConfigurationProperties(prefix = "redis")
@Data
@PropertySource("classpath:iiif.properties")
@PropertySource(value = "classpath:iiif.user.properties", ignoreResourceNotFound = true)
public class RedisProperties {

    private final Map<String, Long> cacheExpirations = new HashMap<>();
    private       String            host;
    private       int               port;
    private       int               database;
    private       long              timeout;

}
