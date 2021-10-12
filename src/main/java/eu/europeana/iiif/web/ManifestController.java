package eu.europeana.iiif.web;

import eu.europeana.iiif.model.Definitions;
import eu.europeana.iiif.service.CacheUtils;
import eu.europeana.iiif.service.EdmManifestUtils;
import eu.europeana.iiif.service.ManifestService;
import eu.europeana.iiif.service.ValidateUtils;
import eu.europeana.iiif.service.exception.IIIFException;
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
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static eu.europeana.iiif.model.Definitions.MEDIA_TYPE_IIIF_JSONLD_V2;
import static eu.europeana.iiif.model.Definitions.MEDIA_TYPE_IIIF_JSONLD_V3;
import static eu.europeana.iiif.model.Definitions.MEDIA_TYPE_IIIF_JSON_V2;
import static eu.europeana.iiif.model.Definitions.MEDIA_TYPE_IIIF_JSON_V3;
import static eu.europeana.iiif.model.Definitions.MEDIA_TYPE_JSON;
import static eu.europeana.iiif.model.Definitions.MEDIA_TYPE_JSONLD;

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

    private static final String ACCEPT_HEADER = "Accept";
    private static final String  CONTENT_TYPE           = "Content-Type";
    /* for parsing accept headers */
    private static final Pattern ACCEPT_PROFILE_PATTERN = Pattern.compile("profile=\"(.*?)\"");
    private static final String  ACCEPT_JSON            = "Accept=" + MEDIA_TYPE_JSON;
    private static final String  ACCEPT_JSONLD          = "Accept=" + MEDIA_TYPE_JSONLD;

    private ManifestService manifestService;
    private CachingDemo cachingDemo;

    public ManifestController(ManifestService manifestService, CachingDemo cachingDemo) {
        this.manifestService = manifestService;
        this.cachingDemo = cachingDemo;
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
     * @throws IIIFException when something goes wrong during processing
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
            HttpServletResponse response) throws IIIFException {
        return handleRequest(collectionId, recordId, wskey, version, recordApi, addFullText, fullTextApi, true, request);
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
            HttpServletResponse response) throws IIIFException {
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
            HttpServletRequest request
                                                ) throws IIIFException {
        String id = "/" + collectionId + "/" + recordId;
        ValidateUtils.validateWskeyFormat(wskey);
        ValidateUtils.validateRecordIdFormat(id);

        if (recordApi != null) {
            ValidateUtils.validateApiUrlFormat(recordApi);
        }
        if (fullTextApi != null) {
            ValidateUtils.validateApiUrlFormat(fullTextApi);
        }

        if (!isAcceptHeaderOK(request)) {
            return new ResponseEntity<>(HttpStatus.NOT_ACCEPTABLE);
        }
        // if no version was provided as request param, then we check the accept header for a profiles= value
        String iiifVersion = version;
        if (iiifVersion == null) {
            iiifVersion = versionFromAcceptHeader(request);
        }

        String json = manifestService.getRecordJson(id, wskey, recordApi);
        ZonedDateTime lastModified = EdmManifestUtils.getRecordTimestampUpdate(json);
        String eTag = generateETag(id, lastModified, iiifVersion);
        HttpHeaders headers = CacheUtils.generateCacheHeaders("no-cache", eTag, lastModified, ACCEPT_HEADER);
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
        addContentTypeToResponseHeader(headers, iiifVersion, isJson);
        return new ResponseEntity<>(manifestService.serializeManifest(manifest), headers, HttpStatus.OK);
    }

    private String versionFromAcceptHeader(HttpServletRequest request) {
        String result = "2"; // default version if no accept header is present
        String accept = request.getHeader(ACCEPT_HEADER);
        if (StringUtils.isNotEmpty(accept)) {
            Matcher m = ACCEPT_PROFILE_PATTERN.matcher(accept);
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

    private boolean isAcceptHeaderOK(HttpServletRequest request) {
        String accept = request.getHeader(ACCEPT_HEADER);
        return (StringUtils.isBlank(accept)) ||
               (StringUtils.containsIgnoreCase(accept, "*/*")) ||
               (StringUtils.containsIgnoreCase(accept, "application/json")) ||
               (StringUtils.containsIgnoreCase(accept, "application/ld+json"));
    }

    private String generateETag(String recordId, ZonedDateTime recordUpdated, String iiifVersion) {
        String hashData = recordId + recordUpdated + manifestService.getSettings().getAppVersion() + iiifVersion;
        return CacheUtils.generateETag(hashData, true);
    }

    /**
     * Adds content-type header to response.
     *
     * @param headers response header
     * @param version response version (based on request)
     * @param isJson  indicates whether application/json header should be added to response.
     */
    private void addContentTypeToResponseHeader(HttpHeaders headers, String version, boolean isJson) {
        if ("3".equalsIgnoreCase(version)) {
            if (isJson) {
                headers.add(CONTENT_TYPE, MEDIA_TYPE_IIIF_JSON_V3);
            } else {
                headers.add(CONTENT_TYPE, MEDIA_TYPE_IIIF_JSONLD_V3);
            }
        } else {
            if (isJson) {
                headers.add(CONTENT_TYPE, MEDIA_TYPE_IIIF_JSON_V2);
            } else {
                headers.add(CONTENT_TYPE, MEDIA_TYPE_IIIF_JSONLD_V2);
            }
        }
    }

    @GetMapping(value = "/redis")
    public String redisDemo(
            @RequestParam(value = "demo") String demo) {
        String result;
        switch (demo) {
            case "1":
                result = cachingDemo.redisTest1();
                break;
            case "2":
                result = cachingDemo.redisTest2();
                break;
            case "3":
                result = cachingDemo.redisTest3();
                break;
            default:
                result = cachingDemo.redisTest4();
        }

        return result;
    }
}
