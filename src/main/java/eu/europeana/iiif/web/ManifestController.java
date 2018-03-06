package eu.europeana.iiif.web;

import eu.europeana.iiif.model.Definitions;
import eu.europeana.iiif.service.ManifestService;
import eu.europeana.iiif.service.exception.IIIFException;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URL;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Rest controller that handles manifest requests
 *
 * @author Patrick Ehlert
 * Created on 06-12-2017
 */
@RestController
public class ManifestController {

    private ManifestService manifestService;

    /* for parsing accept headers */
    private Pattern acceptProfilePattern = Pattern.compile("profiles=\"(.*?)\"");


    public ManifestController(ManifestService manifestService) {
        this.manifestService = manifestService;
    }

    /**
     * Handles manifest requests for version 2
     * @param collectionId (required field)
     * @param recordId (required field)
     * @param wskey apikey (required field)
     * @param version
     * @param recordApi
     * @param request
     * @return JSON-LD string containing manifest
     * @throws IIIFException when something goes wrong during processing
     */
    @CrossOrigin
    @RequestMapping(value = "/presentation/{collectionId}/{recordId}/manifest", method = RequestMethod.GET,
            produces = {Definitions.MEDIA_TYPE_IIIF_JSONLD_V3,
                        Definitions.MEDIA_TYPE_IIIF_JSONLD_V2,
                        Definitions.MEDIA_TYPE_JSONLD,
                        MediaType.APPLICATION_JSON_VALUE})
    public String manifest(@PathVariable String collectionId,
                           @PathVariable String recordId,
                           @RequestParam(value = "wskey", required = true) String wskey,
                           @RequestParam(value = "v", required = false) String version,
                           @RequestParam(value = "recordApi", required = false) URL recordApi,
                           HttpServletRequest request)
                    throws IIIFException {
        // TODO integrate with apikey service?? (or leave it like this?)
        String id = "/"+collectionId+"/"+recordId;
        String json = manifestService.getRecordJson(id, wskey, recordApi);

        // if no version was provided as request param, then we check the accept header for a profiles= value
        String v = version;
        if (version == null) {
            v = versionFromAcceptHeader(request);
        }

        Object manifest;
        if ("3".equalsIgnoreCase(v)) {
            manifest = manifestService.generateManifestV3(json);
        } else {
            manifest = manifestService.generateManifestV2(json); // fallback option
        }
        return manifestService.serializeManifest(manifest);
    }


    private String versionFromAcceptHeader(HttpServletRequest request) {
        String result = "2"; // default version if no accept header is present

        String accept = request.getHeader("Accept");
        if (StringUtils.isNotEmpty(accept)) {
            Matcher m = acceptProfilePattern.matcher(accept);
            if (m.find()) {
                String profiles = m.group(1);
                if (profiles.toLowerCase(Locale.getDefault()).contains(Definitions.MEDIA_TYPE_IIIF_V3)) {
                    result = "3";
                } else {
                    result = "2";
                }
            }
        }
        return result;
    }
}
