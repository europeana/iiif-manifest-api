package eu.europeana.iiif.service;

import com.jayway.jsonpath.Filter;
import com.jayway.jsonpath.JsonPath;
import eu.europeana.iiif.AcceptUtils;
import eu.europeana.iiif.config.ManifestSettings;
import eu.europeana.iiif.model.ManifestDefinitions;
import eu.europeana.iiif.model.WebResource;
import eu.europeana.iiif.model.WebResourceSorter;
import eu.europeana.iiif.model.v3.Collection;
import eu.europeana.iiif.model.v3.*;
import eu.europeana.metis.schema.model.MediaType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.util.StringUtils;

import java.util.*;

import static com.jayway.jsonpath.Criteria.where;
import static com.jayway.jsonpath.Filter.filter;

/**
 * This class contains all the methods for mapping EDM record data to IIIF Manifest data for IIIF v3
 *
 * @author Srishti Singh
 * Created on 25-03-2020
 */
// ignore sonarqube rule: we return null on purpose in this class
// ignore pmd rule:  we want to make a clear which objects are v2 and which v3
public final class EdmManifestMappingV3 {

    // EA-3324 hack
    private static final String AUDIO = "audio";
    private static final String SOUND = "sound";
    private static final Logger LOG = LogManager.getLogger(EdmManifestMappingV3.class);

    private EdmManifestMappingV3() {
        // private constructor to prevent initialization
    }

    /**
     * Generates a IIIF v3 manifest based on the provided (parsed) json document
     * @param jsonDoc parsed json document
     * @return IIIF Manifest v3 object
     */
    static ManifestV3 getManifestV3(ManifestSettings ms, Object jsonDoc) {
        String europeanaId = EdmManifestUtils.getEuropeanaId(jsonDoc);
        String isShownBy = EdmManifestUtils.getValueFromDataProviderAggregation(jsonDoc, europeanaId, "edmIsShownBy");
        // EA-1973 + EA-2002 temporary(?) workaround for EUScreen; use isShownAt and use edmType instead of ebucoreMimetype
        MediaType euScreenTypeHack = null;
        if (StringUtils.isEmpty(isShownBy)) {
            LOG.debug("isShownBy is empty");
            // find edmType (try first Europeana Proxy, use other proxies as fallback)
            String edmType = (String) EdmManifestUtils.getFirstValueArray("edmType", europeanaId,
                    JsonPath.parse(jsonDoc).read("$.object.proxies[?(@.europeanaProxy == true)].edmType", String[].class));
            if (StringUtils.isEmpty(edmType)) {
                edmType = (String) EdmManifestUtils.getFirstValueArray("edmType", europeanaId,
                        JsonPath.parse(jsonDoc).read("$.object.proxies[?(!@.lineage && @.europeanaProxy != true )].edmType", String[].class));
            }
            // find isShownAt
            String isShownAt = EdmManifestUtils.getValueFromDataProviderAggregation(jsonDoc, europeanaId, "edmIsShownAt");
            LOG.debug("edmType = {}, isShownAt = {}", edmType, isShownAt);
            if (isShownAt != null && ("VIDEO".equalsIgnoreCase(edmType) || "SOUND".equalsIgnoreCase(edmType))  &&
                    (isShownAt.startsWith("http://www.euscreen.eu/item.html") ||
                            isShownAt.startsWith("https://www.euscreen.eu/item.html")) ){
                LOG.debug("Using isShownAt because item is EUScreen video or audio");
                isShownBy = isShownAt;
                if ("SOUND".equalsIgnoreCase(edmType)) {
                    euScreenTypeHack = MediaType.AUDIO;
                } else {
                    euScreenTypeHack = MediaType.VIDEO;
                }
            }
        } else {
            LOG.debug("isShownBy = {}", isShownBy);
        }

        ManifestV3 manifest = new ManifestV3(europeanaId, ManifestDefinitions.getManifestId(europeanaId), isShownBy);
        manifest.setService(getServiceDescriptionV3(ms.getFullTextApiBaseUrl(), europeanaId));
        // EA-3325
//        manifest.setPartOf(getWithinV3(jsonDoc));
        manifest.setLabel(getLabelsV3(jsonDoc));
        manifest.setSummary(getDescriptionV3(jsonDoc));
        manifest.setMetadata(getMetaDataV3(jsonDoc));
        manifest.setThumbnail(getThumbnailImageV3(europeanaId, jsonDoc));
        manifest.setNavDate(EdmManifestUtils.getNavDate(europeanaId, jsonDoc));
        manifest.setHomePage(EdmManifestUtils.getHomePage(europeanaId, jsonDoc));
        manifest.setRequiredStatement(getAttributionV3(europeanaId, isShownBy, jsonDoc));
        manifest.setRights(getRights(europeanaId, jsonDoc));
        manifest.setSeeAlso(getDataSetsV3(europeanaId));
        manifest.setItems(getItems(europeanaId, isShownBy, jsonDoc, euScreenTypeHack));
        manifest.setStart(getStartCanvasV3(manifest.getItems(), isShownBy));
        return manifest;
    }

    /**
     * Generates Service descriptions for the manifest
     */
    private static Service[] getServiceDescriptionV3(String fulltextBaseUrl, String europeanaId) {
        return new Service[]{new Service(EdmManifestUtils.getFullTextSearchUrl(fulltextBaseUrl, europeanaId),
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
     * Return an with the id of the thumbnail as defined in 'europeanaAggregation.edmPreview'
     * @param jsonDoc parsed json document
     * @return Image object, or null if no edmPreview was found
     */
    static eu.europeana.iiif.model.v3.Image[] getThumbnailImageV3(String europeanaId, Object jsonDoc) {
        String thumbnailId = EdmManifestUtils.getThumbnailId(europeanaId, jsonDoc);
        if (StringUtils.isEmpty(thumbnailId)) {
            return null;
        }
        return new eu.europeana.iiif.model.v3.Image[] {new eu.europeana.iiif.model.v3.Image(thumbnailId)};
    }


    /**
     * Return attribution text as a String
     * We look for the webResource that corresponds to our edmIsShownBy and return the attribution snippet for that.
     * @param europeanaId consisting of dataset ID and record ID separated by a slash (string should have a leading slash and not trailing slash)
     * @param isShownBy edmIsShownBy value
     * @param jsonDoc parsed json document
     * @return
     */
    static LanguageMap getAttributionV3(String europeanaId, String isShownBy, Object jsonDoc) {
        Filter isShownByFilter = filter(where(EdmManifestUtils.ABOUT).is(isShownBy));
        String[] attributions = JsonPath.parse(jsonDoc).
                read("$.object.aggregations[*].webResources[?]."+ EdmManifestUtils.HTML_ATTRIB_SNIPPET, String[].class, isShownByFilter);
        String attribution = (String) EdmManifestUtils.getFirstValueArray(EdmManifestUtils.HTML_ATTRIB_SNIPPET, europeanaId, attributions);
        if (StringUtils.isEmpty(attribution)) {
            return null;
        }
        return new LanguageMap(LanguageMap.DEFAULT_METADATA_KEY, attribution);
    }


    /**
     * Generates 3 datasets with the appropriate ID and format (one for rdf/xml, one for json and one for json-ld)
     * @param europeanaId consisting of dataset ID and record ID separated by a slash (string should have a leading slash and not trailing slash)
     * @return array of 3 datasets
     */
    static eu.europeana.iiif.model.v3.DataSet[] getDataSetsV3(String europeanaId) {
        eu.europeana.iiif.model.v3.DataSet[] result = new eu.europeana.iiif.model.v3.DataSet[3];
        result[0] = new eu.europeana.iiif.model.v3.DataSet(ManifestDefinitions.getDatasetId(europeanaId, ".json-ld"),
                AcceptUtils.MEDIA_TYPE_JSONLD);
        result[1] = new eu.europeana.iiif.model.v3.DataSet(ManifestDefinitions.getDatasetId(europeanaId, ".json"),
                org.springframework.http.MediaType.APPLICATION_JSON_VALUE);
        result[2] = new eu.europeana.iiif.model.v3.DataSet(ManifestDefinitions.getDatasetId(europeanaId, ".rdf"),
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
    static eu.europeana.iiif.model.v3.Canvas[] getItems(String europeanaId, String isShownBy, Object jsonDoc, MediaType euScreenTypeHack) {
        // generate canvases in a same order as the web resources
        List<WebResource> sortedResources = EdmManifestUtils.getSortedWebResources(europeanaId, isShownBy, jsonDoc);
        if (sortedResources.isEmpty()) {
            return null;
        }

        int order = 1;
        Map<String, Object>[] services = JsonPath.parse(jsonDoc).read("$.object[?(@.services)].services[*]", Map[].class);
        List<eu.europeana.iiif.model.v3.Canvas> canvases = new ArrayList<>(sortedResources.size());
        for (WebResource webResource: sortedResources) {
            canvases.add(getCanvasV3(europeanaId, order, webResource, services, euScreenTypeHack, jsonDoc));
            order++;
        }
        return canvases.toArray(new eu.europeana.iiif.model.v3.Canvas[0]);
    }


    /**
     * Generates a new canvas, but note that we do not fill the otherContent (Full-Text) here. That's done later.
     */
    private static eu.europeana.iiif.model.v3.Canvas getCanvasV3(String europeanaId, int order, WebResource webResource,
                                                                 Map<String, Object>[] services, MediaType euScreenTypeHack,
                                                                 Object jsonDoc) {
        eu.europeana.iiif.model.v3.Canvas c =
                new eu.europeana.iiif.model.v3.Canvas(ManifestDefinitions.getCanvasId(europeanaId, order), order);

        c.setLabel(new LanguageMap(null, "p. "+order));

        Object obj = webResource.get(EdmManifestUtils.EBUCORE_HEIGHT);
        if (obj instanceof Integer){
            c.setHeight((Integer) obj);
        }

        obj = webResource.get(EdmManifestUtils.EBUCORE_WIDTH);
        if (obj instanceof Integer){
            c.setWidth((Integer) obj);
        }

        String durationText = (String) webResource.get("ebucoreDuration");
        if (durationText != null) {
            Long durationInMs = Long.valueOf(durationText);
            c.setDuration(durationInMs / 1000D);
        }

        String attributionText = (String) webResource.get(EdmManifestUtils.HTML_ATTRIB_SNIPPET);
        if (!StringUtils.isEmpty(attributionText)){
            c.setRequiredStatement(new LanguageMap(LanguageMap.DEFAULT_METADATA_KEY, attributionText));
        }

        LinkedHashMap<String, ArrayList<String>> license = (LinkedHashMap<String, ArrayList<String>>) webResource.get("webResourceEdmRights");
        if (license != null && !license.values().isEmpty()) {
            c.setRights(new Rights(license.values().iterator().next().get(0)));
        }

        // a canvas has 1 annotation page by default (an extra annotation page is added later if there is a full text available)
        AnnotationPage annoPage = new AnnotationPage(null); // id is not really necessary in this case
        c.setItems(new AnnotationPage[] {annoPage});

        // annotation page has 1 annotation
        Annotation anno = new Annotation(null);
        annoPage.setItems(new Annotation[] { anno });

        // we use Metis to determine if it's an image, video, audio or text based on mimetype
        String ebucoreMimeType = (String) webResource.get("ebucoreHasMimeType");
        MediaType mediaType = MediaType.getMediaType(ebucoreMimeType);

        // EA-1973 + EA-2002 temporary(?) workaround for EUScreen; use isShownAt and use edmType instead of ebucoreMimetype
        if (euScreenTypeHack != null) {
            LOG.debug("Override mediaType {} with {} because of EUScreen hack", mediaType, euScreenTypeHack);
            mediaType = euScreenTypeHack;
            ebucoreMimeType = null;
        }

        if (mediaType == MediaType.AUDIO || mediaType == MediaType.VIDEO) {
            anno.setTimeMode("trim");
        }
        anno.setTarget(c.getId());

        //EA-3325: check if the webResource has a "svcsHasService"; if not, add a thumbnail
        if (Objects.isNull(webResource.get(EdmManifestUtils.SVCS_HAS_SERVICE))){
            c.setThumbnail(getThumbnailImageV3(europeanaId, jsonDoc));
        }

        // annotation has 1 annotationBody
        eu.europeana.iiif.model.v3.AnnotationBody annoBody = new AnnotationBody(
                  // EA-3324 Temporary hack awaiting Metis to fix this
//                (String) webResource.get(EdmManifestUtils.ABOUT),  StringUtils.capitalize(mediaType.toString().toLowerCase(Locale.GERMANY)));
                (String) webResource.get(EdmManifestUtils.ABOUT),  StringUtils.capitalize(StringUtils.replace(mediaType.toString().toLowerCase(Locale.GERMANY), AUDIO, SOUND)));
        anno.setBody(annoBody);

        if (!StringUtils.isEmpty(ebucoreMimeType)) {
            annoBody.setFormat(ebucoreMimeType);
        }

        // body can have a service
        String serviceId = EdmManifestUtils.getServiceId(webResource, europeanaId);
        if (serviceId != null) {
            eu.europeana.iiif.model.v3.Service service = new eu.europeana.iiif.model.v3.Service(serviceId, ManifestDefinitions.IMAGE_SERVICE_TYPE_3);
            service.setProfile(EdmManifestUtils.lookupServiceDoapImplements(services, serviceId, europeanaId));
            annoBody.setService(service);
        }
        return c;
    }
}
