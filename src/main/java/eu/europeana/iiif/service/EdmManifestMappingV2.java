package eu.europeana.iiif.service;

import com.jayway.jsonpath.Filter;
import com.jayway.jsonpath.JsonPath;
import eu.europeana.iiif.AcceptUtils;
import eu.europeana.iiif.config.ManifestSettings;
import eu.europeana.iiif.config.MediaTypes;
import eu.europeana.iiif.model.ManifestDefinitions;
import eu.europeana.iiif.model.MediaType;
import eu.europeana.iiif.model.WebResource;
import eu.europeana.iiif.model.v2.*;
import eu.europeana.iiif.model.v3.LanguageMap;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

import static com.jayway.jsonpath.Criteria.where;
import static com.jayway.jsonpath.Filter.filter;
import static eu.europeana.iiif.model.ManifestDefinitions.CANVAS_THUMBNAIL_POSTFIX;

/**
 * This class contains all the methods for mapping EDM record data to IIIF Manifest data for IIIF v2
 *
 * @author Patrick Ehlert
 * Created on 08-02-2018
 *
 * Updated By Srishti Singh
 * modified on 25-03-2020
 *
 * Updated By Lúthien
 * modified on 15-02-2023
 *
 */
// ignore sonarqube rule: we return null on purpose in this class
// ignore pmd rule:  we want to make a clear which objects are v2 and which v3
@SuppressWarnings({"squid:S1168", "pmd:UnnecessaryFullyQualifiedName"})
public final class EdmManifestMappingV2 {

    private static final Logger LOG = LogManager.getLogger(EdmManifestMappingV2.class);

    private static String THUMBNAIL_API_URL;

    private EdmManifestMappingV2() {
        // private constructor to prevent initialization
    }

    /**
     * Generates a IIIF v2 manifest based on the provided (parsed) json document
     * @param jsonDoc parsed json document
     * @return IIIF Manifest v2 object
     */
    static ManifestV2 getManifestV2(ManifestSettings settings, MediaTypes mediaTypes, Object jsonDoc) {
        THUMBNAIL_API_URL = settings.getThumbnailApiUrl();
        String europeanaId = EdmManifestUtils.getEuropeanaId(jsonDoc);
        String isShownBy = EdmManifestUtils.getValueFromDataProviderAggregation(jsonDoc, europeanaId, "edmIsShownBy");
        ManifestV2 manifest = new ManifestV2(europeanaId, settings.getManifestId(europeanaId), isShownBy);
        manifest.setService(getServiceDescriptionV2(settings, europeanaId));
        // EA-3325
//        manifest.setWithin(getWithinV2(jsonDoc));
        manifest.setLabel(getLabelsV2(jsonDoc));
        manifest.setDescription(getDescriptionV2(jsonDoc));
        manifest.setMetadata(getMetaDataV2(jsonDoc));
        manifest.setThumbnail(getThumbnailImageV2(europeanaId, jsonDoc));
        manifest.setNavDate(EdmManifestUtils.getNavDate(europeanaId, jsonDoc));
        manifest.setAttribution(getAttributionV2(europeanaId, isShownBy, jsonDoc));
        manifest.setLicense(getLicense(europeanaId, jsonDoc));
        manifest.setSeeAlso(getDataSetsV2(settings, europeanaId));
        Sequence [] sequences = getSequencesV2(settings, mediaTypes, europeanaId, isShownBy, jsonDoc);
        if (sequences != null) {
            manifest.setSequences(sequences);
            manifest.setStartCanvasPageNr(getStartCanvasV2(manifest.getSequences()[0].getCanvases(), isShownBy));
        } else {
            LOG.debug("No Canvas generated for europeanaId {}", europeanaId);
        }
        return manifest;
    }

    /**
     * Generates a serviceV2 description for the manifest
     */
    private static eu.europeana.iiif.model.v2.Service getServiceDescriptionV2(ManifestSettings settings, String europeanaId) {
        return new eu.europeana.iiif.model.v2.Service(settings.getContentSearchURL(europeanaId),
                                                      ManifestDefinitions.SEARCH_CONTEXT_VALUE,
                                                      ManifestDefinitions.SEARCH_PROFILE_VALUE);
    }

    /**
     * Return first proxy.dctermsIsPartOf that starts with "http://data.theeuropeanlibrary.org/ that we can find
     * @param jsonDoc parsed json document
     * @return
     */

    // EA-3325
//    static String getWithinV2(Object jsonDoc) {
//        List<String> result = EdmManifestUtils.getEuropeanaLibraryCollections(jsonDoc);
//        if (result.isEmpty()) {
//            return null;
//        }
//        return result.get(0);
//    }

    /**
     * We first check all proxies for a title. If there are no titles, then we check the description fields
     * @param jsonDoc parsed json document
     * @return array of LanguageObject
     */
    static LanguageObject[] getLabelsV2(Object jsonDoc) {
        // we read everything in as LanguageMap[] because that best matches the EDM implementation, then we convert to LanguageObjects[]
        LanguageMap labelsV3 = EdmManifestMappingV3.getLabelsV3(jsonDoc);
        if (labelsV3 == null) {
            return null;
        }
        return LanguageMapUtils.langMapToObjects(labelsV3);
    }

    /**
     * Returns the values from the proxy.dcDescription fields, but only if they aren't used as a label yet.
     * @param jsonDoc parsed json document
     * @return
     */
    static LanguageObject[] getDescriptionV2(Object jsonDoc) {
        // we read everything in as LanguageMap[] because that best matches the EDM implementation, then we convert to LanguageObjects[]
        LanguageMap descriptionsV3 = EdmManifestMappingV3.getDescriptionV3(jsonDoc);
        if (descriptionsV3 == null) {
            return null;
        }
        return LanguageMapUtils.langMapToObjects(EdmManifestMappingV3.getDescriptionV3(jsonDoc));
    }

    /**
     * Reads the dcDate, dcFormat, dcRelation, dcType, dcLanguage and dcSource values from all proxies and puts them in a
     * map with the appropriate label
     * @param jsonDoc parsed json document
     * @return
     */
    static eu.europeana.iiif.model.v2.MetaData[] getMetaDataV2(Object jsonDoc) {
        Map<String, List<LanguageObject>> data = new LinkedHashMap<>();
        addMetaDataV2(data, JsonPath.parse(jsonDoc).read("$.object.proxies[*].dcDate", LanguageMap[].class), "date");
        addMetaDataV2(data, JsonPath.parse(jsonDoc).read("$.object.proxies[*].dcFormat", LanguageMap[].class), "format");
        addMetaDataV2(data, JsonPath.parse(jsonDoc).read("$.object.proxies[*].dcRelation", LanguageMap[].class), "relation");
        addMetaDataV2(data, JsonPath.parse(jsonDoc).read("$.object.proxies[*].dcType", LanguageMap[].class), "type");
        addMetaDataV2(data, JsonPath.parse(jsonDoc).read("$.object.proxies[*].dcLanguage", LanguageMap[].class), "language");
        addMetaDataV2(data, JsonPath.parse(jsonDoc).read("$.object.proxies[*].dcSource", LanguageMap[].class), "source");

        List<eu.europeana.iiif.model.v2.MetaData> result = new ArrayList<>(data.entrySet().size());
        for (Map.Entry<String, List<LanguageObject>> entry : data.entrySet()) {
            String label = entry.getKey();
            List<LanguageObject> values = entry.getValue();
            result.add(new eu.europeana.iiif.model.v2.MetaData(label, values.toArray(new LanguageObject[0])));
        }

        if (result.isEmpty()) {
            return null;
        }
        return result.toArray(new eu.europeana.iiif.model.v2.MetaData[0]);
    }

    /**
     * We read in metadata as a LanguageMap[], but we need to convert it to Map consisting of labels and List<LanguageObjects>
     * Also if the key is 'def' we should leave that out (for v2)
     */
    private static void addMetaDataV2(Map<String, List<LanguageObject>> metaData, LanguageMap[] dataToAdd, String fieldName) {
        for (LanguageMap map : dataToAdd) {
            for (Map.Entry<String, String[]> entry : map.entrySet()) {
                String language = entry.getKey();
                String[] values = entry.getValue();
                for (String value: values) {
                    processMetaDataField(fieldName, metaData, language, value);
                }
            }
        }
    }

    private static void processMetaDataField(String fieldName,  Map<String, List<LanguageObject>> metaData, String language, String value) {
        List<LanguageObject> langObjects;
        if (!metaData.containsKey(fieldName)) {
            langObjects = new ArrayList<>();
            metaData.put(fieldName, langObjects);
        } else {
            langObjects = metaData.get(fieldName);
        }
        langObjects.add(new LanguageObject(language, value));
    }

    /**
     * Return an with the id of the thumbnail as defined in 'europeanaAggregation.edmPreview'
     * @param jsonDoc parsed json document
     * @return Image object, or null if no edmPreview was found
     */
    static eu.europeana.iiif.model.v2.Image getThumbnailImageV2(String europeanaId, Object jsonDoc) {
        String thumbnailId = EdmManifestUtils.getThumbnailId(europeanaId, jsonDoc);
        if (StringUtils.isEmpty(thumbnailId)) {
            return null;
        }
        return new eu.europeana.iiif.model.v2.Image(EdmManifestUtils.getThumbnailId(europeanaId, jsonDoc), null, null);
    }

    /**
     * EA-3325 Return array with the id of the canvas-specific thumbnail created from the Webresource id
     * @param webresourceId hasview image ID
     * @return Image object, or null if either provided String was null
     */
    static eu.europeana.iiif.model.v2.Image getCanvasThumbnailImageV2(String webresourceId, String ThumbnailApiUrl) {
        if (StringUtils.isAnyEmpty(ThumbnailApiUrl, webresourceId)) {
            return null;
        }
        return new eu.europeana.iiif.model.v2.Image(
            ThumbnailApiUrl + webresourceId + CANVAS_THUMBNAIL_POSTFIX,
            null,
            null);
    }

    /**
     * Return attribution text as a String
     * We look for the webResource that corresponds to our edmIsShownBy and return the 'textAttributionSnippet' for that.
     * @param europeanaId consisting of dataset ID and record ID separated by a slash (string should have a leading slash and not trailing slash)
     * @param isShownBy edmIsShownBy value
     * @param jsonDoc parsed json document
     * @return
     */
    static String getAttributionV2(String europeanaId, String isShownBy, Object jsonDoc) {
        Filter isShownByFilter = filter(where(EdmManifestUtils.ABOUT).is(isShownBy));
        String[] attributions = JsonPath.parse(jsonDoc).
                read("$.object.aggregations[*].webResources[?]." + EdmManifestUtils.TEXT_ATTRIB_SNIPPET, String[].class, isShownByFilter);
        return (String) EdmManifestUtils.getFirstValueArray(EdmManifestUtils.TEXT_ATTRIB_SNIPPET, europeanaId, attributions);
    }


    /**
     * Generates 3 datasets with the appropriate ID and format (one for rdf/xml, one for json and one for json-ld)
     * @param europeanaId consisting of dataset ID and record ID separated by a slash (string should have a leading slash and not trailing slash)
     * @return array of 3 datasets
     */
    static eu.europeana.iiif.model.v2.DataSet[] getDataSetsV2(ManifestSettings settings, String europeanaId) {
        eu.europeana.iiif.model.v2.DataSet[] result = new eu.europeana.iiif.model.v2.DataSet[3];
        result[0] = new eu.europeana.iiif.model.v2.DataSet(settings.getDatasetId(europeanaId, ".json-ld"),
                AcceptUtils.MEDIA_TYPE_JSONLD);
        result[1] = new eu.europeana.iiif.model.v2.DataSet(settings.getDatasetId(europeanaId, ".json"),
                org.springframework.http.MediaType.APPLICATION_JSON_VALUE);
        result[2] = new eu.europeana.iiif.model.v2.DataSet(settings.getDatasetId(europeanaId, ".rdf"),
                ManifestDefinitions.MEDIA_TYPE_RDF);
        return result;
    }

    /**
     * @param europeanaId consisting of dataset ID and record ID separated by a slash (string should have a leading slash and not trailing slash)
     * @param isShownBy
     * @param jsonDoc parsed json document
     * @return
     */
    static eu.europeana.iiif.model.v2.Sequence[] getSequencesV2(ManifestSettings settings, MediaTypes mediaTypes,String europeanaId, String isShownBy, Object jsonDoc) {
        // generate canvases in a same order as the web resources
        List<WebResource> sortedResources = EdmManifestUtils.getSortedWebResources(europeanaId, isShownBy, jsonDoc);
        if (sortedResources.isEmpty()) {
            return null;
        }
        int order = 1;
        Map<String, Object>[] services = JsonPath.parse(jsonDoc).read("$.object[?(@.services)].services[*]", Map[].class);
        List<eu.europeana.iiif.model.v2.Canvas> canvases = new ArrayList<>(sortedResources.size());
        for (WebResource webResource: sortedResources) {
            Canvas canvas = getCanvasV2(settings, mediaTypes, europeanaId, order, webResource, services);
            // for non supported media types we do not create any canvas. Case-4 of media type handling : See-EA-3413
            if (canvas != null) {
                canvases.add(canvas);
                order++;
            }
        }
        // if there are canvas generated add the sequence
        if (!canvases.isEmpty()) {
            // there should be only 1 sequence, so sequence number is always 1
            eu.europeana.iiif.model.v2.Sequence[] result = new eu.europeana.iiif.model.v2.Sequence[1];
            result[0] = new eu.europeana.iiif.model.v2.Sequence();
            result[0].setStartCanvas(settings.getCanvasId(europeanaId, 1));
            result[0].setCanvases(canvases.toArray(new eu.europeana.iiif.model.v2.Canvas[0]));
            return result;
        }
        return null;
    }


    /**
     * Return the first license description we find in any 'aggregation.edmRights' field. Note that we first try the europeanaAggregation and if
     * that doesn't contain an edmRights, we check the other aggregations
     * @param europeanaId consisting of dataset ID and record ID separated by a slash (string should have a leading slash and not trailing slash)
     * @param jsonDoc parsed json document
     * @return String containing rights information
     */
    static String getLicense(String europeanaId, Object jsonDoc) {
        return EdmManifestUtils.getLicenseText(europeanaId, jsonDoc);
    }

    /**
     * @return Integer containing the page number of the canvas that refers to the edmIsShownBy, or else just the first
     *  Canvas. Null if there are no canvases
     */
    static Integer getStartCanvasV2(eu.europeana.iiif.model.v2.Canvas[] items, String edmIsShownBy) {
        if (items == null) {
            LOG.trace("Start canvas = null (no canvases present)");
            return null;
        }

        eu.europeana.iiif.model.v2.Canvas result = null;
        for (eu.europeana.iiif.model.v2.Canvas c : items) {
            String annotationBodyId = c.getStartImageAnnotation().getResource().getId();
            if (!StringUtils.isEmpty(edmIsShownBy) && edmIsShownBy.equals(annotationBodyId)) {
                result = c;
                LOG.trace("Start canvas = {} (matches with edmIsShownBy)", result.getPageNr());
                break;
            }
        }
        // nothing found, return first canvas
        if (result == null) {
            result = items[0];
            LOG.trace("Start canvas = {} (no match with edmIsShownBy, select first)", result.getPageNr());
        }
        return result.getPageNr();
    }

    /**
     * Generates a new canvas, but note that we do not fill the otherContent (Full-Text) here. That is done later
     */
    private static eu.europeana.iiif.model.v2.Canvas getCanvasV2(ManifestSettings settings,
                                                                 MediaTypes mediaTypes,
                                                                 String europeanaId,
                                                                 int order,
                                                                 WebResource webResource,
                                                                 Map<String, Object>[] services) {
        eu.europeana.iiif.model.v2.Canvas c =
                new eu.europeana.iiif.model.v2.Canvas(settings.getCanvasId(europeanaId, order), order);

        c.setLabel("p. "+order);

        setCanvasHeightAndWidth(webResource, c);

        String attributionText = (String) webResource.get(EdmManifestUtils.TEXT_ATTRIB_SNIPPET);
        if (!StringUtils.isEmpty(attributionText)){
            c.setAttribution(attributionText);
        }

        //EA-3325: check if the webResource has a "svcsHasService"; if not, add a thumbnail
        if (Objects.isNull(webResource.get(EdmManifestUtils.SVCS_HAS_SERVICE))){
            c.setThumbnail(getCanvasThumbnailImageV2(URLEncoder.encode(webResource.getId(), StandardCharsets.UTF_8), settings.getThumbnailApiUrl()));
        }

        LinkedHashMap<String, ArrayList<String>> license = (LinkedHashMap<String, ArrayList<String>>) webResource.get(EdmManifestUtils.WEB_RESOURCE_EDM_RIGHTS);
        if (license != null && !license.values().isEmpty()) {
            c.setLicense(license.values().iterator().next().get(0));
        }

        // canvas has 1 annotation (image field)
        c.setImages(new eu.europeana.iiif.model.v2.Annotation[1]);
        c.getImages()[0] = new eu.europeana.iiif.model.v2.Annotation();
        c.getImages()[0].setOn(c.getId());

        // MEDIA TYPE HANDLING ....

        // Fetch the mime type from the web resource
        String ebuCoreMimeType = (String) webResource.get(EdmManifestUtils.EBUCORE_HAS_MIMETYPE);
        MediaType mediaType = null;

        // get the configured media type of the mimetype
        Optional<MediaType> media = mediaTypes.getMediaType(ebuCoreMimeType);
        if (media.isPresent()) {
            mediaType = media.get();
        }

        // ignored cases CASE 4 for version 2
        if (mediaType == null || ifSupportedMediaTypeIsVideoOrSound(mediaType)) {
            LOG.debug("No canvas added for webresource {} as the media type - {} is invalid or not supported.",
                    webResource.get(EdmManifestUtils.ABOUT),
                    ebuCoreMimeType);
            return null;
        }

        // Now create the annotation body based on the media type (annotation has 1 annotationBody)
        eu.europeana.iiif.model.v2.AnnotationBody annoBody = new eu.europeana.iiif.model.v2.AnnotationBody((String) webResource.get(EdmManifestUtils.ABOUT));

        // EA- 3436 add technical metadata for case 2 and 3
        // case 2
         if (mediaType.isBrowserSupported() && !mediaType.isVideoOrSound()) {
             annoBody.setFormat(mediaType.getMimeType());
             addTechnicalMetadata(c, annoBody);
         }

         // case 3 - Now we are generating canvas for video/sound in v2
        if (mediaType.isRendered()) {
            //EA-3745 For specialized formats, generate the image url (which is actually a thumbnail url) based on the media type
            annoBody = new eu.europeana.iiif.model.v2.AnnotationBody(
                    EdmManifestUtils.getIdForAnnotation((String) webResource.get(EdmManifestUtils.ABOUT), mediaType,THUMBNAIL_API_URL));

            if (c.getThumbnail() != null) {
                annoBody = new eu.europeana.iiif.model.v2.AnnotationBody(c.getThumbnail().getId());
            }
            // update height and width
            setHeightWidthForRendered(c);
            // set rendering
            c.setRendering(new Rendering((String) webResource.get(EdmManifestUtils.ABOUT), mediaType.getMimeType(), mediaType.getLabel()));
            addTechnicalMetadata(c, annoBody);
        }

        // body can have a service. EA-3475 Do not add service for specialized formats
        if(!mediaType.isRendered()) {
            setServiceForAnnotation(europeanaId, webResource, services, annoBody);
        }
        c.getImages()[0].setResource(annoBody);
        return c;
    }

    private static void setServiceForAnnotation(String europeanaId, WebResource webResource,
        Map<String, Object>[] services, AnnotationBody annoBody) {
        String serviceId = EdmManifestUtils.getServiceId(webResource, europeanaId);
        if (serviceId != null) {
            Service service = new Service(serviceId, ManifestDefinitions.IMAGE_CONTEXT_VALUE);
            service.setProfile(EdmManifestUtils.lookupServiceDoapImplements(services, serviceId,
                europeanaId));
            annoBody.setService(service);
        }
    }

    private static void setCanvasHeightAndWidth(WebResource webResource, Canvas c) {
        Object obj = webResource.get(EdmManifestUtils.EBUCORE_HEIGHT);
        if (obj instanceof Integer){
            c.setHeight((Integer) obj);
        }

        obj = webResource.get(EdmManifestUtils.EBUCORE_WIDTH);
        if (obj instanceof Integer){
            c.setWidth((Integer) obj);
        }
    }

    /**
     * If media type is present and
     * is either browser but has type video or sound
     * return true
     *
     * @param mediaType
     * @return
     */
    private static boolean ifSupportedMediaTypeIsVideoOrSound(MediaType mediaType) {
        return mediaType != null && (mediaType.isBrowserSupported() && mediaType.isVideoOrSound());
    }

    /**
     * Adds the technical metadata in the annotation body of the canvas
     * @param canvas
     * @param body
     */
    private static void addTechnicalMetadata(Canvas canvas, AnnotationBody body) {
        body.setHeight(canvas.getHeight());
        body.setWidth(canvas.getWidth());
    }

    /**
     * Update the width and height of the canvas based
     * on few conditons
     * @param c
     */
    private static void setHeightWidthForRendered(Canvas c) {
        Integer height = c.getHeight();
        Integer width = c.getWidth();

        if (height != null && width != null) {
            // width is higher (and equal) than 400px - Set width = 400 ; Set height = (height / width) x 400
            if (width >= 400) {
                c.setWidth(400);
                c.setHeight((int) ((height/(width.doubleValue())) * 400));
            }
        } else {
            // if the WebResource does not have width or height
            // Set width and height to 400 (this is the size of the default icon which is what will likely be displayed)
            c.setHeight(400);
            c.setWidth(400);
        }
    }
}
