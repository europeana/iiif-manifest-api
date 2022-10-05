package eu.europeana.iiif.web;

import eu.europeana.api.commons.error.EuropeanaApiException;
import eu.europeana.iiif.AcceptUtils;
import eu.europeana.iiif.exception.InvalidIIIFVersionException;
import eu.europeana.iiif.service.CacheUtils;
import eu.europeana.iiif.service.EdmManifestUtils;
import eu.europeana.iiif.service.ManifestService;
import eu.europeana.iiif.service.ValidateUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URL;
import java.time.ZonedDateTime;

import static eu.europeana.iiif.AcceptUtils.*;

/**
 * Rest controller that handles manifest requests
 *
 * @author Patrick Ehlert
 * Created on 06-12-2017
 */
@RestController
@RequestMapping("/presentation")
public class ManifestController {

    private static final Logger LOG = LogManager.getLogger(ManifestController.class);

    private ManifestService manifestService;

    public ManifestController(ManifestService manifestService) {
        this.manifestService = manifestService;
    }

    /**
     * Handles manifest requests
     *
     * @param collectionId (required field)
     * @param recordId     (required field)
     * @param wskey        apikey (required field)
     * @param version      (optional) indicates which IIIF version to generate, either '2' or '3'
     * @param recordApi    (optional) alternative recordApi baseUrl to use for retrieving record data
     * @param addFullText  (optional) perform fulltext exists check or not`1
     * @param fullTextApi  (optional) alternative fullTextApi baseUrl to use for retrieving record data
     * @return JSON-LD string containing manifest
     * @throws EuropeanaApiException when something goes wrong during processing
     */
    @SuppressWarnings("squid:S00107") // too many parameters -> we cannot avoid it.

    @GetMapping(value = "/{collectionId}/{recordId}/manifest", headers = ACCEPT_JSON)
    public ResponseEntity<String> manifestRequestJson(
            @PathVariable String collectionId,
            @PathVariable String recordId,
            @RequestParam(value = "wskey", required = true) String wskey,
            @RequestParam(value = "format", required = false) String version,
            @RequestParam(value = "recordApi", required = false) URL recordApi,
            @RequestParam(value = "fullText", required = false, defaultValue = "true") Boolean addFullText,
            @RequestParam(value = "fullTextApi", required = false) URL fullTextApi,
            HttpServletRequest request,
            HttpServletResponse response) throws EuropeanaApiException {
        return handleRequest(collectionId, recordId, wskey, version, recordApi, addFullText, fullTextApi, true, request);
    }

    @GetMapping(value = "/test/error")
    public void testError() throws InvalidIIIFVersionException {
        throw new InvalidIIIFVersionException("This is a test");
    }


    @GetMapping(value = "/{collectionId}/{recordId}/manifest", headers = ACCEPT_JSONLD)
    public ResponseEntity<String> manifestRequestJsonLd(
            @PathVariable String collectionId,
            @PathVariable String recordId,
            @RequestParam(value = "wskey", required = true) String wskey,
            @RequestParam(value = "format", required = false) String version,
            @RequestParam(value = "recordApi", required = false) URL recordApi,
            @RequestParam(value = "fullText", required = false, defaultValue = "true") Boolean addFullText,
            @RequestParam(value = "fullTextApi", required = false) URL fullTextApi,
            HttpServletRequest request,
            HttpServletResponse response) throws EuropeanaApiException {
        return handleRequest(collectionId, recordId, wskey, version, recordApi, addFullText, fullTextApi, false, request);
    }

    private ResponseEntity<String> handleRequest( String collectionId,
            String recordId,
            String wskey,
            String version,
            URL recordApi,
            boolean addFullText,
            URL fullTextApi,
            boolean isJson,
            HttpServletRequest request) throws EuropeanaApiException {
        String id = "/" + collectionId + "/" + recordId;
        ValidateUtils.validateWskeyFormat(wskey);
        ValidateUtils.validateRecordIdFormat(id);

        if (recordApi != null) {
            ValidateUtils.validateApiUrlFormat(recordApi);
        }
        if (fullTextApi != null) {
            ValidateUtils.validateApiUrlFormat(fullTextApi);
        }

        String iiifVersion = AcceptUtils.getRequestVersion(request, version);
        if (StringUtils.isEmpty(iiifVersion)) {
            throw new InvalidIIIFVersionException(ACCEPT_VERSION_INVALID);
        }

        String json = manifestService.getRecordJson(id, wskey, recordApi);
        ZonedDateTime lastModified = EdmManifestUtils.getRecordTimestampUpdate(json);
        String eTag = generateETag(id, lastModified, iiifVersion);
        HttpHeaders headers = CacheUtils.generateCacheHeaders("no-cache", eTag, lastModified, ACCEPT);
        ResponseEntity cached = CacheUtils.checkCached(request, headers, lastModified, eTag);
        if (cached != null) {
            LOG.debug("Returning 304 response");
            return cached;
        }

        Object manifest;
        if ("3".equalsIgnoreCase(iiifVersion)) {
            if (addFullText){
                manifest = manifestService.generateManifestV3(json, fullTextApi);
            } else {
                manifest = manifestService.generateManifestV3(json);
            }
        } else {
            if (addFullText){
                manifest = manifestService.generateManifestV2(json, fullTextApi); // fallback option
            } else {
                manifest = manifestService.generateManifestV2(json); // fallback option
            }
        }
        AcceptUtils.addContentTypeToResponseHeader(headers, iiifVersion, isJson);
        return new ResponseEntity<>(manifestService.serializeManifest(manifest), headers, HttpStatus.OK);
    }


    private String generateETag(String recordId, ZonedDateTime recordUpdated, String iiifVersion) {
        String hashData = recordId + recordUpdated + manifestService.getSettings().getAppVersion() + iiifVersion;
        return CacheUtils.generateETag(hashData, true);
    }

}
