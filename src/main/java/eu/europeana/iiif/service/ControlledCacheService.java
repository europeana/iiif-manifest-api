package eu.europeana.iiif.service;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * Created by luthien on 04/10/2021.
 */
@Service
public class ControlledCacheService {

    private static final String CONTROLLED_PREFIX = "myControlledPrefix_";

    public static String getCacheKey(String relevant){
        return CONTROLLED_PREFIX + relevant;
    }

    @CachePut(cacheNames = "myControlledCache")
    public String populateCache() {
        return "this is it again!";
    }

    @Cacheable(cacheNames = "myControlledCache")
    public String getFromCache() {
        return null;
    }

    @CacheEvict(cacheNames = "myControlledCache")
        public void removeFromCache() {
    }

    @Cacheable(cacheNames = "myControlledCache", key = "'myControlledPrefix_'.concat(#relevant)")
    public String getFromCache(String relevant) {
        return null;
    }

    @CacheEvict(cacheNames = "myControlledCache", key = "'myControlledPrefix_'.concat(#relevant)")
    public void removeFromCache(String relevant) {
    }

    @CachePut(cacheNames = "myControlledCache", key = "'myControlledPrefix_'.concat(#relevant)")
    public String populateCache(String relevant, String unrelevantTrackingId) {
        return "this is it again!";
    }

}
