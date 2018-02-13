package eu.europeana.iiif.web;

import eu.europeana.iiif.model.AbstractManifest;
import eu.europeana.iiif.model.Definitions;
import eu.europeana.iiif.service.ManifestService;
import eu.europeana.iiif.service.exception.IIIFException;
import org.apache.catalina.servlet4preview.http.HttpServletRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


/**
 * Rest controller that handles manifest requests
 *
 * @author Patrick Ehlert
 * Created on 06-12-2017
 */
@RestController
public class ManifestController {

    private static final Logger LOG = LogManager.getLogger(ManifestController.class);

    private ManifestService manifestService;

    public ManifestController(ManifestService manifestService) {
        this.manifestService = manifestService;
    }

    /**
     * Handles manifest requests
     * @param collectionId (required field)
     * @param recordId (required field)
     * @param v IIIF version (optional field)
     * @param wskey apikey (required field)
     * @return JSON-LD string containing manifest
     * @throws IIIFException when something goes wrong during processing
     */
    @CrossOrigin
    @RequestMapping(value = "/presentation/{collectionId}/{recordId}/manifest", method = RequestMethod.GET, produces = {Definitions.MEDIA_TYPE_JSONLD_V3, MediaType.APPLICATION_JSON_VALUE})
    public String manifest(@PathVariable String collectionId,
                           @PathVariable String recordId,
                           @RequestParam(value = "v", required = false) Integer v,
                           @RequestParam(value = "wskey", required = true) String wskey,
                           HttpServletRequest request)
                    throws IIIFException {
        // TODO integrate with apikey service (for now we let record API do the check when we retrieve record JSON data)
        String id = "/"+collectionId+"/"+recordId;
        String json = manifestService.getRecordJson(id, wskey);
        LOG.debug("v = "+v);

        // if no version was provided as request param, then we check the acceptheader
        Integer version = v;
        if (version == null) {
            String accept = request.getHeader("Accept");
            LOG.debug("Manifest, Accept = {}", accept);
            // TODO confirm if we will specify version 2 in this way or not
            if (accept.toLowerCase().contains("https://iiif.io/api/presentation/2/context.json")) {
                version = 2;
            } else {
                version = 3;
            }
        }

        AbstractManifest manifest = null;
        if (version == 2) {
            manifest = manifestService.generateManifestV2(json);
        } else if (version == 3){
            manifest = manifestService.generateManifestV3(json); // default
        }
        return manifestService.serializeManifest(manifest);
    }


}
