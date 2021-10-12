package eu.europeana.iiif.web;

import eu.europeana.iiif.service.CacheService;
import eu.europeana.iiif.service.ControlledCacheService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Created by luthien on 04/10/2021.
 */
@Service
public class CachingDemo {

    private static final Logger LOG = LogManager.getLogger(CachingDemo.class);

    private final CacheService cacheService;
    private final ControlledCacheService controlledCacheService;

    public CachingDemo(CacheService cacheService, ControlledCacheService controlledCacheService){
        this.cacheService = cacheService;
        this.controlledCacheService = controlledCacheService;
    }


    public String redisTest1(){
        return "First: " + cacheService.cacheThis() + "Second: " + cacheService.cacheThis();
    }

    public String redisTest2(){
        return "First: "
                    + cacheService.cacheThis()
                    + "\nSecond: "
                    + cacheService.cacheThis()
                    + "\nStarting controlled cache: -----------"
                    + "\nControlled First: "
                    + getFromControlledCache()
                    + "\nControlled Second: "
                    + getFromControlledCache()
                    + "\n";
    }

    public String redisTest3(){
        String result = "First: "
                    + cacheService.cacheThis()
                    + "\nSecond: "
                    + cacheService.cacheThis()
                    + "\nStarting controlled cache: -----------"
                    + "\nControlled First: "
                    + getFromControlledCache()
                    + "\nControlled Second: "
                    + getFromControlledCache()
                    + "\nClearing all cache entries:"
                        + "\n";
        cacheService.forgetAboutThis();
        controlledCacheService.removeFromCache();
        return result;
    }

    public String redisTest4(){
        return "First: "
               + cacheService.cacheThis("param1", UUID.randomUUID().toString())
               + "\nSecond: "
               + cacheService.cacheThis("param1", UUID.randomUUID().toString())
               + "\nThird: "
               + cacheService.cacheThis("AnotherParam", UUID.randomUUID().toString())
               + "\nFourth: "
               + cacheService.cacheThis("AnotherParam", UUID.randomUUID().toString())
               + "\nStarting controlled cache: -----------"
               + "\nControlled First: "
               + getFromControlledCache("first")
               + "\nControlled Second: "
               + getFromControlledCache("second")
               + "\n"
               + getFromControlledCache("first")
               + "\n"
               + getFromControlledCache("second")
               + "\n"
               + getFromControlledCache("third")
        + "\n";
    }

    private String getFromControlledCache() {
        StringBuilder sb2 = new StringBuilder();
        String fromCache = controlledCacheService.getFromCache();
        if (fromCache == null) {
            sb2.append("\nOups - Cache was empty. Going to populate it");
            sb2.append("\nPopulated Cache with: \n");
            sb2.append(controlledCacheService.populateCache());
            return sb2.toString();
        }
        sb2.append("\nReturning from Cache: \n");
        sb2.append(fromCache);
        return sb2.toString();
    }

    private String getFromControlledCache(String param) {
        StringBuilder sb2 = new StringBuilder();
        String fromCache = controlledCacheService.getFromCache(param);
        if (fromCache == null) {
            sb2.append("\nOups - Cache was empty. Going to populate it\n");
            sb2.append("\nPopulated Cache with: \n");
            sb2.append(controlledCacheService.populateCache(param, UUID.randomUUID().toString()));
            return sb2.toString();
        }
        sb2.append("\nReturning from Cache: \n");
        sb2.append(fromCache);
        return sb2.toString();
    }

}
