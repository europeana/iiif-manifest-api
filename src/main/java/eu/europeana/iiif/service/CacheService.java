package eu.europeana.iiif.service;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

/**
 * Created by luthien on 30/09/2021.
 */
@Service
public class CacheService {

    private static final Logger LOG = LogManager.getLogger(CacheService.class);

    @Cacheable(cacheNames = "myCache")
    public String cacheThis(){
        LOG.info("Returning NOT from cache!");
        return "this is it";
    }

    @CacheEvict(cacheNames = "myCache")
    public void forgetAboutThis(){
        LOG.info("Forgetting everything about this!");
    }

    @Cacheable(cacheNames = "myCache", key = "'myPrefix_'.concat(#relevant)")
    public String cacheThis(String relevant, String unrelevantTrackingId){
        LOG.info("Returning NOT from cache. Tracking: {}!", unrelevantTrackingId);
        return "this is it";
    }

    @CacheEvict(cacheNames = "myCache", key = "'myPrefix_'.concat(#relevant)")
    public void forgetAboutThis(String relevant){
        LOG.info("Forgetting everything about this '{}'!", relevant);
    }

}
