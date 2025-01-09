package eu.europeana.iiif.service;

import com.jayway.jsonpath.Filter;
import com.jayway.jsonpath.JsonPath;
import eu.europeana.iiif.AcceptUtils;
import eu.europeana.iiif.config.ManifestSettings;
import eu.europeana.iiif.config.MediaTypes;
import eu.europeana.iiif.model.ManifestDefinitions;
import eu.europeana.iiif.model.MediaType;
import eu.europeana.iiif.model.WebResource;
import eu.europeana.iiif.model.WebResourceSorter;
import eu.europeana.iiif.model.v3.Collection;
import eu.europeana.iiif.model.v3.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

import static com.jayway.jsonpath.Criteria.where;
import static com.jayway.jsonpath.Filter.filter;
import static eu.europeana.iiif.model.ManifestDefinitions.ATTRIBUTION_STRING;
import static eu.europeana.iiif.model.ManifestDefinitions.CANVAS_THUMBNAIL_POSTFIX;
import static eu.europeana.iiif.model.MediaType.VIDEO;
import static eu.europeana.iiif.model.MediaType.SOUND;

/**
 * This class contains all the methods for mapping EDM record data to IIIF Manifest data for IIIF v3
 *
 * @author Srishti Singh
 * Created on 25-03-2020
 *
 * Updated By Lúthien
 * modified on 15-02-2023
 */
// ignore sonarqube rule: we return null on purpose in this class
// ignore pmd rule:  we want to make a clear which objects are v2 and which v3
public final class EdmManifestMappingV3 {

    private static final Logger LOG = LogManager.getLogger(EdmManifestMappingV3.class);

    private static String thumbnailApiUrl;

    private EdmManifestMappingV3() {
    }


    /**
     * Eu screen items are only checked in format/ iiif version 3
     * If there is a edm:isShownAt or edm:isShownBy starting with http(s)://www.euscreen.eu/item.html
     * and a proxy with edmType = SOUND or VIDEO, then generate a Canvas with that URL
     *
     * @param jsonDoc
     * @param europeanaId
     * @param isShownBy
     * @return
     */
    private static MediaType ifEuScreenGetMediaType(MediaTypes mediaTypes, Object jsonDoc, String europeanaId, String isShownBy) {
        MediaType euScreenTypeHack = null;
        //1. find edmType (try first Europeana Proxy, use other proxies as fallback)
        String edmType = (String) EdmManifestUtils.getFirstValueArray("edmType", europeanaId,
                JsonPath.parse(jsonDoc).read("$.object.proxies[?(@.europeanaProxy == true)].edmType", String[].class));
        if (StringUtils.isEmpty(edmType)) {
            edmType = (String) EdmManifestUtils.getFirstValueArray("edmType", europeanaId,
                    JsonPath.parse(jsonDoc).read("$.object.proxies[?(!@.lineage && @.europeanaProxy != true )].edmType", String[].class));
        }

        //2. get isShownAt
        String isShownAt = EdmManifestUtils.getValueFromDataProviderAggregation(jsonDoc, europeanaId, "edmIsShownAt");
        LOG.debug("isShownAt = {}", isShownAt);

        // 3. check if it's a EUScreen item
        if (isEuScreenItem(edmType, isShownBy, isShownAt)) {
            LOG.debug("Item is EUScreen :  edmType - {}, isShownBy - {}", edmType, isShownBy);
            // if the item is EUscreen then the value will always be present
            euScreenTypeHack=  mediaTypes.getEUScreenType(edmType).orElse(null);
            isShownBy = isShownAt; // replace isShownBy with IsShownAt for EU Screen items
        }
        return euScreenTypeHack;
    }

    /**
     * If there is a edm:isShownAt or edm:isShownBy starting with http(s)://www.euscreen.eu/item.html
     * and a proxy with edmType = SOUND or VIDEO
     *
     * @param edmType
     * @param isShownBy
     * @param isShownAt
     * @return
     */
    private static boolean isEuScreenItem(String edmType, String isShownBy, String isShownAt) {
        String isShownAtOrBy = StringUtils.isBlank(isShownBy) ? isShownAt : isShownBy;
        return (isShownAtOrBy != null && (VIDEO.equalsIgnoreCase(edmType) || SOUND.equalsIgnoreCase(edmType))  &&
                (isShownAtOrBy.startsWith("http://www.euscreen.eu/item.html") ||
                        isShownAtOrBy.startsWith("https://www.euscreen.eu/item.html")));
    }

    /**
     * Generates a IIIF v3 manifest based on the provided (parsed) json document
     * @param jsonDoc parsed json document
     * @return IIIF Manifest v3 object
     */
    static ManifestV3 getManifestV3(ManifestSettings ms, MediaTypes mediaTypes, Object jsonDoc) {
        thumbnailApiUrl = ms.getThumbnailApiUrl();
        String europeanaId = EdmManifestUtils.getEuropeanaId(jsonDoc);
        String isShownBy = EdmManifestUtils.getValueFromDataProviderAggregation(jsonDoc, europeanaId, "edmIsShownBy");

        // if Item is EU screen then get the mediaTypevalue and the isShownBy value is replaced with isShownAt if empty
        MediaType euScreenTypeHack = ifEuScreenGetMediaType(mediaTypes, jsonDoc, europeanaId, isShownBy);
        ManifestV3 manifest = new ManifestV3(europeanaId, ms.getManifestId(europeanaId), isShownBy);
        manifest.setService(getServiceDescriptionV3(ms, europeanaId));
        // EA-3325
//        manifest.setPartOf(getWithinV3(jsonDoc));
        manifest.setLabel(getLabelsV3(jsonDoc));
        manifest.setSummary(getDescriptionV3(jsonDoc));
        manifest.setMetadata(getMetaDataV3(jsonDoc));
        manifest.setThumbnail(getThumbnailImageV3(europeanaId, jsonDoc));
        manifest.setNavDate(EdmManifestUtils.getNavDate(europeanaId, jsonDoc));
        manifest.setHomePage(EdmManifestUtils.getHomePage(europeanaId, jsonDoc));
        manifest.setRequiredStatement(getAttributionV3Root(europeanaId, isShownBy, jsonDoc));
        manifest.setRights(getRights(europeanaId, jsonDoc));
        manifest.setSeeAlso(getDataSetsV3(ms, europeanaId));
        // get the canvas items and if present add to manifest
        Canvas[] items = getItems(ms, mediaTypes, europeanaId, isShownBy, jsonDoc, euScreenTypeHack);
        if (items != null && items.length > 0) {
            manifest.setItems(items);
            manifest.setStart(getStartCanvasV3(manifest.getItems(), isShownBy));
        } else {
            LOG.debug("No Canvas generated for europeanaId {}", europeanaId);
        }
        return manifest;
    }

    /**
     * Generates Service descriptions for the manifest
     */
    private static Service[] getServiceDescriptionV3(ManifestSettings ms, String europeanaId) {
        return new Service[]{new Service(ms.getContentSearchURL(europeanaId),
                null,
                ManifestDefinitions.SEARCH_CONTEXT_VALUE,
                ManifestDefinitions.SEARCH_PROFILE_VALUE)};
    }

    /**
     * Create a collection for all proxy.dctermsIsPartOf that start with "http://data.theeuropeanlibrary.org/
     * @param jsonDoc parsed json document
     * @return
     */
    static Collection[] getWithinV3(Object jsonDoc) {
        List<String> collections = EdmManifestUtils.getEuropeanaLibraryCollections(jsonDoc);
        if (collections.isEmpty()) {
            return null;
        }
        List<Collection> result = new ArrayList<>(collections.size());
        for (String collection : collections) {
            result.add(new Collection(collection));
        }
        return result.toArray(new Collection[0]);
    }

    /**
     * We first check all proxies for a title. If there are no titles, then we check the description fields
     * @param jsonDoc parsed json document
     * @return
     */
    static LanguageMap getLabelsV3(Object jsonDoc)  {
        LanguageMap[] maps = JsonPath.parse(jsonDoc).read("$.object.proxies[*].dcTitle", LanguageMap[].class);
        if (maps == null || maps.length == 0) {
            maps = JsonPath.parse(jsonDoc).read("$.object.proxies[*].dcDescription", LanguageMap[].class);
        }
        return LanguageMapUtils.mergeLanguageMaps(maps);
    }


    /**
     * Returns the values from the proxy.dcDescription fields, but only if they aren't used as a label yet.
     * @param jsonDoc parsed json document
     * @return
     */
    static LanguageMap getDescriptionV3(Object jsonDoc) {
        if (JsonPath.parse(jsonDoc).read("$.object.proxies[*].dcTitle", LanguageMap[].class).length > 0) {
            return LanguageMapUtils.mergeLanguageMaps(JsonPath.parse(jsonDoc).read("$.object.proxies[*].dcDescription", LanguageMap[].class));
        }
        return null;
    }

    /**
     * Reads the dcDate, dcFormat, dcRelation, dcType, dcLanguage and dcSource values from all proxies and puts them in a
     * LanguageMap with the appropriate label
     * @param jsonDoc parsed json document
     * @return
     */
    static eu.europeana.iiif.model.v3.MetaData[] getMetaDataV3(Object jsonDoc) {
        List<eu.europeana.iiif.model.v3.MetaData> metaData = new ArrayList<>();
        addMetaDataV3(metaData, "date", jsonDoc, "$.object.proxies[*].dcDate");
        addMetaDataV3(metaData, "format", jsonDoc, "$.object.proxies[*].dcFormat");
        addMetaDataV3(metaData, "relation", jsonDoc, "$.object.proxies[*].dcRelation");
        addMetaDataV3(metaData, "type", jsonDoc, "$.object.proxies[*].dcType");
        addMetaDataV3(metaData, "language", jsonDoc,"$.object.proxies[*].dcLanguage");
        addMetaDataV3(metaData, "source", jsonDoc, "$.object.proxies[*].dcSource");
        if (!metaData.isEmpty()) {
            return metaData.toArray(new eu.europeana.iiif.model.v3.MetaData[0]);
        }
        return null;
    }


    private static void addMetaDataV3(List<eu.europeana.iiif.model.v3.MetaData> metaData, String fieldName, Object jsonDoc, String jsonPath) {
        // We go over all meta data values and check if it's an url or not.
        // Non-url values are always included as is. If it's an url then we wrap that with an html anchor tag.
        // Additionally we check if the url is also present in object.timespans, agents, concepts or places. If so we
        // add the corresponding preflabels (in all available languages) as well.

        LanguageMap[] metaDataValues = JsonPath.parse(jsonDoc).read(jsonPath, LanguageMap[].class);

        for (LanguageMap metaDataValue : metaDataValues) {
            LanguageMap metaDataLabel = new LanguageMap(LanguageMap.DEFAULT_METADATA_KEY, fieldName);
            LOG.trace("START '{}' value map: {} ", fieldName, metaDataValue);

            List<LanguageMap> extraPrefLabelMaps = new ArrayList<>(); // keep track of extra prefLabels we need to add
            for (Map.Entry<String, String[]> entry : metaDataValue.entrySet()) {
                String      key = entry.getKey();
                String[] values = entry.getValue();

                LOG.trace("  checking key {} with {} values", key, values.length);

                List<String> newValues = new ArrayList<>(); // recreate all values (because we may change one)
                for (String value : values) {
                    processMetaDataValue(value, newValues, jsonDoc, extraPrefLabelMaps);
                }

                // replace old values with new ones for the current key
                LOG.trace("  done checking key = {}, new values = {}", key, newValues);
                metaDataValue.replace(key, newValues.toArray(new String[0]));
            }
            // if there are extra prefLabel maps, we merge all into our metaDataValues map
            if (!extraPrefLabelMaps.isEmpty()) {
                LOG.trace("  adding extra preflabels = {}", extraPrefLabelMaps);
                // add the original languagemap
                extraPrefLabelMaps.add(0, metaDataValue);
                metaDataValue = LanguageMapUtils.mergeLanguageMaps(extraPrefLabelMaps.toArray(new LanguageMap[0]));
            }
            LOG.trace("FINISH '{}' value map = {}", fieldName, metaDataValue);

            metaData.add(new eu.europeana.iiif.model.v3.MetaData(metaDataLabel, metaDataValue));
        }
    }


    static void processMetaDataValue(String value, List<String> newValues, Object jsonDoc,
                                     List<LanguageMap> extraPrefLabelMaps) {
        LOG.trace("  processing value {}", value);
        if (EdmManifestUtils.isUrl(value)) {
            // 1. add html anchor tag to current value
            String newValue = "<a href='" + value + "'>" + value + "</a>";
            LOG.trace("    isUrl -> newValue = {} ", newValue);
            newValues.add(newValue);

            // 2. check if we should add extra preflabels
            LanguageMap extraPrefLabelMap = getTimespanAgentConceptOrPlaceLabels(jsonDoc, value);
            if (extraPrefLabelMap != null) {
                LOG.trace("    isUrl -> extraLabels = {}", extraPrefLabelMap);
                extraPrefLabelMaps.add(extraPrefLabelMap);
            }
        } else {
            // no url, we keep it.
            newValues.add(value);
        }
    }

    public static LanguageMap getTimespanAgentConceptOrPlaceLabels(Object jsonDoc, String value) {
        LanguageMap result = getEntityPrefLabels(jsonDoc, "timespans", value);
        if (result != null) {
            return result;
        }
        result = getEntityPrefLabels(jsonDoc, "agents", value);
        if (result != null) {
            return result;
        }
        result = getEntityPrefLabels(jsonDoc, "concepts", value);
        if (result != null) {
            return result;
        }
        return getEntityPrefLabels(jsonDoc, "places", value);
    }

    private static LanguageMap getEntityPrefLabels(Object jsonDoc, String entityName, String value) {
        Filter aboutFilter = filter(where(EdmManifestUtils.ABOUT).is(value));
        LanguageMap[] labels = JsonPath.parse(jsonDoc).
                read("$.object[?(@." + entityName + ")]." + entityName + "[?].prefLabel", LanguageMap[].class, aboutFilter);
        if (labels.length > 0) {
            return labels[0];
        }
        return null;
    }

    /**
     * Return the first license description we find in any 'aggregation.edmRights' field. Note that we first try the europeanaAggregation and if
     * that doesn't contain an edmRights, we check the other aggregations
     * @param europeanaId consisting of dataset ID and record ID separated by a slash (string should have a leading slash and not trailing slash)
     * @param jsonDoc parsed json document
     * @return Rights object containing rights information
     */
    public static Rights getRights(String europeanaId, Object jsonDoc) {
        String licenseText = EdmManifestUtils.getLicenseText(europeanaId, jsonDoc);
        if (StringUtils.isEmpty(licenseText)) {
            return null;
        }
        return new Rights(licenseText);
    }

    /**
     * Return array with the id of the thumbnail as defined in 'europeanaAggregation.edmPreview'
     * @param jsonDoc parsed json document
     * @return Image object, or null if no edmPreview was found
     */
    static eu.europeana.iiif.model.v3.Image[] getThumbnailImageV3(String europeanaId, Object jsonDoc) {
        String thumbnailId = EdmManifestUtils.getThumbnailId(europeanaId, jsonDoc);
        if (StringUtils.isEmpty(thumbnailId)) {
//            return new eu.europeana.iiif.model.v3.Image[] {};
            return null;
        }
        return new eu.europeana.iiif.model.v3.Image[] {new eu.europeana.iiif.model.v3.Image(thumbnailId)};
    }

    /**
     * EA-3325 Return array with the id of the canvas-specific thumbnail created from the Webresource id
     * @param webresourceId hasview image ID
     * @return Image object, or null if either provided String was null
     */
    static eu.europeana.iiif.model.v3.Image[] getCanvasThumbnailImageV3(String webresourceId) {
        if (StringUtils.isAnyEmpty(thumbnailApiUrl, webresourceId)) {
            return new eu.europeana.iiif.model.v3.Image[] {};
        }
        return new eu.europeana.iiif.model.v3.Image[] {new eu.europeana.iiif.model.v3.Image(
            thumbnailApiUrl + webresourceId + CANVAS_THUMBNAIL_POSTFIX)};
    }


    /**
     * Return attribution text as a String
     * We look for the webResource that corresponds to our edmIsShownBy and return the attribution snippet for that.
     * @param europeanaId consisting of dataset ID and record ID separated by a slash (string should have a leading slash and not trailing slash)
     * @param isShownBy edmIsShownBy value
     * @param jsonDoc parsed json document
     * @return
     */
    static RequiredStatementMap getAttributionV3Root(String europeanaId, String isShownBy, Object jsonDoc) {
        Filter isShownByFilter = filter(where(EdmManifestUtils.ABOUT).is(isShownBy));
        String[] attributions = JsonPath.parse(jsonDoc).
                read("$.object.aggregations[*].webResources[?]."+ EdmManifestUtils.HTML_ATTRIB_SNIPPET, String[].class, isShownByFilter);
        String attribution = (String) EdmManifestUtils.getFirstValueArray(EdmManifestUtils.HTML_ATTRIB_SNIPPET, europeanaId, attributions);
        return createRequiredStatementMap(attribution);
    }

    static RequiredStatementMap createRequiredStatementMap(String attribution){
        if (StringUtils.isEmpty(attribution)) {
            return null;
        }
        return new RequiredStatementMap(new LanguageMap(LanguageMap.DEFAULT_METADATA_KEY, ATTRIBUTION_STRING),
                                        new LanguageMap(LanguageMap.DEFAULT_METADATA_KEY, attribution));
    }

    /**
     * Generates 3 datasets with the appropriate ID and format (one for rdf/xml, one for json and one for json-ld)
     * @param europeanaId consisting of dataset ID and record ID separated by a slash (string should have a leading slash and not trailing slash)
     * @return array of 3 datasets
     */
    static eu.europeana.iiif.model.v3.DataSet[] getDataSetsV3(ManifestSettings settings, String europeanaId) {
        eu.europeana.iiif.model.v3.DataSet[] result = new eu.europeana.iiif.model.v3.DataSet[3];
        result[0] = new eu.europeana.iiif.model.v3.DataSet(settings.getDatasetId(europeanaId, ".json-ld"),
                AcceptUtils.MEDIA_TYPE_JSONLD);
        result[1] = new eu.europeana.iiif.model.v3.DataSet(settings.getDatasetId(europeanaId, ".json"),
                org.springframework.http.MediaType.APPLICATION_JSON_VALUE);
        result[2] = new eu.europeana.iiif.model.v3.DataSet(settings.getDatasetId(europeanaId, ".rdf"),
                ManifestDefinitions.MEDIA_TYPE_RDF);
        return result;
    }



    /**
     * @return the {@link eu.europeana.iiif.model.v3.Canvas} that refers to edmIsShownBy, or else just the first Canvas
     */
    static eu.europeana.iiif.model.v3.Canvas getStartCanvasV3(eu.europeana.iiif.model.v3.Canvas[] items, String edmIsShownBy) {
        if (items == null) {
            LOG.trace("Start canvas = null (no canvases present)");
            return null;
        }

        eu.europeana.iiif.model.v3.Canvas result = null;
        for (eu.europeana.iiif.model.v3.Canvas c : items) {
            String annotationBodyId = c.getStartCanvasAnnotation().getBody().getId();
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
        return new eu.europeana.iiif.model.v3.Canvas(result.getId(), result.getPageNr());
    }

    /**
     * Generates an ordered array of {@link Canvas}es referring to edmIsShownBy and hasView {@link WebResource}s.
     * For more information about the ordering @see {@link WebResourceSorter}
     * @param europeanaId
     * @param isShownBy
     * @param jsonDoc
     * @return array of Canvases
     */
    static eu.europeana.iiif.model.v3.Canvas[] getItems(ManifestSettings settings, MediaTypes mediaTypes, String europeanaId, String isShownBy, Object jsonDoc, MediaType euScreenTypeHack) {
        // generate canvases in a same order as the web resources
        List<WebResource> sortedResources = EdmManifestUtils.getSortedWebResources(europeanaId, isShownBy, jsonDoc);
        if (sortedResources.isEmpty()) {
            return null;
        }
        int order = 1;
        Map<String, Object>[] services = JsonPath.parse(jsonDoc).read("$.object[?(@.services)].services[*]", Map[].class);
        List<eu.europeana.iiif.model.v3.Canvas> canvases = new ArrayList<>(sortedResources.size());
        for (WebResource webResource: sortedResources) {
            Canvas canvas = getCanvasV3(settings, mediaTypes, europeanaId, order, webResource, services, euScreenTypeHack);
            // for non supported media types we do not create any canvas. Case-4 of media type handling : See-EA-3413
            if (canvas != null) {
                canvases.add(canvas);
                order++;
            }
        }
        return canvases.toArray(new eu.europeana.iiif.model.v3.Canvas[0]);
    }


    /**
     * Generates a new canvas, but note that we do not fill the otherContent (Full-Text) here. That's done later.
     */
    private static eu.europeana.iiif.model.v3.Canvas getCanvasV3(ManifestSettings settings,
                                                                 MediaTypes mediaTypes,
                                                                 String europeanaId,
                                                                 int order,
                                                                 WebResource webResource,
                                                                 Map<String, Object>[] services,
                                                                 MediaType euScreenTypeHack) {
        eu.europeana.iiif.model.v3.Canvas c =
                new eu.europeana.iiif.model.v3.Canvas(settings.getCanvasId(europeanaId, order), order);

        c.setLabel(new LanguageMap(null, "p. "+order));

        Object obj = webResource.get(EdmManifestUtils.EBUCORE_HEIGHT);
        if (obj instanceof Integer){
            c.setHeight((Integer) obj);
        }

        obj = webResource.get(EdmManifestUtils.EBUCORE_WIDTH);
        if (obj instanceof Integer){
            c.setWidth((Integer) obj);
        }

        String durationText = (String) webResource.get(EdmManifestUtils.EBUCORE_DURATION);
        if (durationText != null) {
            Long durationInMs = Long.valueOf(durationText);
            c.setDuration(durationInMs / 1000D);
        }

        String attributionText = (String) webResource.get(EdmManifestUtils.HTML_ATTRIB_SNIPPET);
        if (!StringUtils.isEmpty(attributionText)){
            c.setRequiredStatement(createRequiredStatementMap(attributionText));
        }

        LinkedHashMap<String, ArrayList<String>> license = (LinkedHashMap<String, ArrayList<String>>) webResource.get("webResourceEdmRights");
        if (license != null && !license.values().isEmpty()) {
            c.setRights(new Rights(license.values().iterator().next().get(0)));
        }

        //EA-3325: check if the webResource has a "svcsHasService"; if not, add a thumbnail
        if (Objects.isNull(webResource.get(EdmManifestUtils.SVCS_HAS_SERVICE))){
            c.setThumbnail(getCanvasThumbnailImageV3(URLEncoder.encode(webResource.getId(), StandardCharsets.UTF_8)));
        }

        // a canvas has 1 annotation page by default (an extra annotation page is added later if there is a full text available)
        AnnotationPage annoPage = new AnnotationPage(null); // id is not really necessary in this case
        c.setItems(new AnnotationPage[] {annoPage});

        // Add annotation - annotation page has 1 annotation
        Annotation anno = new Annotation(null);
        annoPage.setItems(new Annotation[] { anno });
        anno.setTarget(c.getId());

        // Fetch the mime type from the web resource
        String ebucoreMimeType = (String) webResource.get(EdmManifestUtils.EBUCORE_HAS_MIMETYPE);
        MediaType mediaType = null;
        // MEDIA TYPE handling..
        // case 1 -  EU screen items. Override the media type
        if (euScreenTypeHack != null) {
            LOG.debug("Override mediaType {} with {} because of EUScreen hack", mediaType, euScreenTypeHack);
            mediaType = euScreenTypeHack;
            anno.setTimeMode("trim"); // as it's AV
        } else {
            // get the mediaType from the mimetype fetched
            Optional<MediaType> media = mediaTypes.getMediaType(ebucoreMimeType);
            if (media.isPresent()) {
                mediaType = media.get();
            }
        }

        // CASE 4 -  No canvas should be generated -
        // if media type is not supported (media type is null)
        // OR if item is not EU screen and media type is not either browser or rendered
        // See - EA-3413
        if (mediaType == null || (euScreenTypeHack == null && ifMediaTypeIsNotBrowserOrRendered(mediaType))) {
            LOG.debug("No canvas added for webresource {} as the media type - {} is invalid or not supported.",
                    webResource.get(EdmManifestUtils.ABOUT),
                    ebucoreMimeType);
            return null;
        }

        // Now create the annotation body with webresource url and media type
        // EA- 3436 add technical metadata for case 2 and 3
        AnnotationBody annoBody = getAnnotationBody(webResource, mediaType, anno,c);
        // annotation has 1 annotationBody
        anno.setBody(annoBody);
        // body can have a service.
        // EA-3475 Do not add service for specialized formats
        if(!mediaType.isRendered()) {
            setServiceIdForAnnotation(europeanaId, webResource, services, annoBody);
        }
        return c;
    }

    private static Canvas createCanvas(ManifestSettings settings, String europeanaId, int order) {
        Canvas c =
                new Canvas(settings.getCanvasId(europeanaId, order), order);
        c.setLabel(new LanguageMap(null, "p. "+ order));
        return c;
    }

    private static AnnotationBody getAnnotationBody(WebResource webResource, MediaType mediaType,
        Annotation anno, Canvas c) {
        AnnotationBody annoBody = new AnnotationBody((String) webResource.get(EdmManifestUtils.ABOUT), mediaType.getType());
        // case 2 - browser supported
        if (mediaType.isBrowserSupported() ) {
            annoBody.setFormat(mediaType.getMimeType());
            // add timeMode for AV
            if (mediaType.isVideoOrSound()) {
                anno.setTimeMode("trim");
            }
            addTechnicalMetadata(c, annoBody);
        }
        // case 3 - rendered - No time mode added as we paint an image here
        if(mediaType.isRendered()) {
            //EA-3745 rendered ones are specialized formats.Generate the image url (which is actually a thumbnail url) based on the media type
            String idForAnnotation = EdmManifestUtils.getIdForAnnotation((String) webResource.get(EdmManifestUtils.ABOUT),mediaType,thumbnailApiUrl);
            annoBody = new AnnotationBody(idForAnnotation, EdmManifestUtils.IMAGE);
            // Use the URL of the thumbnail for the respective WebResource as id of the Annotation Body
            if(c.getThumbnail()!=null && c.getThumbnail().length>0) {
                annoBody = new AnnotationBody(c.getThumbnail()[0].getId(), EdmManifestUtils.IMAGE);
            }
            // update the width and height
            setHeightWidthForRendered(c);
           //EA-3745 - use media type 'service' for oembed mimeTypes
            String mediaTypeValue= mediaType.isOEmbed() ? EdmManifestUtils.SERVICE: mediaType.getType();
            // add rendering in canvas for original web resource url
            c.setRendering(new Rendering((String) webResource.get(EdmManifestUtils.ABOUT),
                    mediaTypeValue,
                    mediaType.getMimeType(),
                    new LanguageMap(EdmManifestUtils.LINGUISTIC, mediaType.getLabel())));
            addTechnicalMetadata(c, annoBody);
        }
        return annoBody;
    }

    private static void setServiceIdForAnnotation(String europeanaId, WebResource webResource,
        Map<String, Object>[] services, AnnotationBody annoBody) {
        String serviceId = EdmManifestUtils.getServiceId(webResource, europeanaId);
        if (serviceId != null) {
            Service service = new Service(serviceId, ManifestDefinitions.IMAGE_SERVICE_TYPE_3);
            service.setProfile(EdmManifestUtils.lookupServiceDoapImplements(services, serviceId,
                europeanaId));
            annoBody.setService(service);
        }
    }

    private static void setThumbnailIfRequired(WebResource webResource, Canvas c) {
        //EA-3325: check if the webResource has a "svcsHasService"; if not, add a thumbnail
        if (Objects.isNull(webResource.get(EdmManifestUtils.SVCS_HAS_SERVICE))){
            c.setThumbnail(getCanvasThumbnailImageV3(webResource.getId()));
        }
    }

    private static void setRightsForCanvas(WebResource webResource, Canvas c) {
        LinkedHashMap<String, ArrayList<String>> license = (LinkedHashMap<String, ArrayList<String>>) webResource.get("webResourceEdmRights");
        if (license != null && !license.values().isEmpty()) {
            c.setRights(new Rights(license.values().iterator().next().get(0)));
        }
    }

    private static void setRequiredStatementForCanvas(WebResource webResource, Canvas c) {
        String attributionText = (String) webResource.get(EdmManifestUtils.HTML_ATTRIB_SNIPPET);
        if (!StringUtils.isEmpty(attributionText)){
            c.setRequiredStatement(createRequiredStatementMap(attributionText));
        }
    }

    private static void setDurationForCanvas(WebResource webResource, Canvas c) {
        String durationText = (String) webResource.get(EdmManifestUtils.EBUCORE_DURATION);
        if (durationText != null) {
            Long durationInMs = Long.valueOf(durationText);
            c.setDuration(durationInMs / 1000D);
        }
    }

    private static void setHeightAndWidthForCanvas(WebResource webResource, Canvas c) {
        Object obj = webResource.get(EdmManifestUtils.EBUCORE_HEIGHT);
        if (obj instanceof Integer val){
            c.setHeight(val);
        }
        obj = webResource.get(EdmManifestUtils.EBUCORE_WIDTH);
        if (obj instanceof Integer val){
            c.setWidth(val);
        }
    }

    /**
     * If media type is not either Browser or Rendered
     *
     * NOTE - This is will not happen with the media categories configured for now.
     * As if media type is present it will either be browser or rendered.
     * But for future if something else is added in the XML file
     * the code should be resilient to handle that
     * @param mediaType
     * @return
     */
    private static boolean ifMediaTypeIsNotBrowserOrRendered(MediaType mediaType) {
        return mediaType != null && !(mediaType.isRendered() || mediaType.isBrowserSupported());
    }

    /**
     * Adds the technical metadata in the annotation body of the canvas
     * @param canvas
     * @param body
     */
    private static void addTechnicalMetadata(Canvas canvas, AnnotationBody body) {
        body.setHeight(canvas.getHeight());
        body.setWidth(canvas.getWidth());
        if(!EdmManifestUtils.IMAGE.equals(body.getType().orElse(null))) {
            body.setDuration(canvas.getDuration());
        }
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
