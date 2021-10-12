package eu.europeana.iiif.config;

import eu.europeana.iiif.service.exception.IIIFException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by luthien on 04/10/2021.
 * @see <a href="https://programmerfriend.com/ultimate-guide-to-redis-cache-with-spring-boot-2-and-spring-data-redis/">this</a> blog post
 */
@Configuration
public class RedisConfig extends CachingConfigurerSupport {

    private static final Logger LOG = LogManager.getLogger(RedisConfig.class);

    private final RedisProperties properties;

    /**
     * Creates an instance of the RedisConfig bean with redis properties
     *
     * @param properties read from properties file
     */
    public RedisConfig(RedisProperties properties) {
        this.properties = properties;
    }

    private static RedisCacheConfiguration createCacheConfiguration(long timeoutInSeconds) {
        return RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofSeconds(timeoutInSeconds));
    }

    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        LOG.info("Redis (/ lettuce) configuration enabled with cache timeout "
                 + properties.getTimeout()
                 + " seconds.");

        RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration();
        redisStandaloneConfiguration.setHostName(properties.getHost());
        redisStandaloneConfiguration.setPort(properties.getPort());
        return new LettuceConnectionFactory(redisStandaloneConfiguration);
    }

    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory cf) {
        RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(cf);
        return redisTemplate;
    }

    @Bean
    public RedisCacheConfiguration cacheConfiguration() {
        return createCacheConfiguration(properties.getTimeout());
    }

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory, RedisProperties properties) {

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        for (Map.Entry<String, Long> cacheNameAndTimeout : properties.getCacheExpirations().entrySet()) {
            cacheConfigurations.put(cacheNameAndTimeout.getKey(),
                                    createCacheConfiguration(cacheNameAndTimeout.getValue()));
        }

        return RedisCacheManager.builder(redisConnectionFactory)
                                .cacheDefaults(cacheConfiguration())
                                .withInitialCacheConfigurations(cacheConfigurations)
                                .build();
    }
}
