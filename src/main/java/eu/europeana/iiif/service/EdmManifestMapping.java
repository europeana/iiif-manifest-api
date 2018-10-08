package eu.europeana.iiif.service;

import com.jayway.jsonpath.JsonPath;
import eu.europeana.iiif.model.Definitions;
import eu.europeana.iiif.model.EdmDateUtils;
import eu.europeana.iiif.model.WebResource;
import eu.europeana.iiif.model.WebResourceSorter;
import eu.europeana.iiif.model.v2.LanguageObject;
import eu.europeana.iiif.model.v2.ManifestV2;
import eu.europeana.iiif.model.v3.Collection;
import eu.europeana.iiif.model.v3.LanguageMap;
import eu.europeana.iiif.model.v3.ManifestV3;
import eu.europeana.iiif.service.exception.DataInconsistentException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * This class contains all the methods for mapping EDM record data to IIIF Manifest data for both IIIF v2 and v3
 *
 * @author Patrick Ehlert
 * Created on 08-02-2018
 */
@SuppressWarnings("squid:S1168") // ignore sonarqube rule: we return null on purpose in this class
public final class EdmManifestMapping {

    private static final Logger LOG = LogManager.getLogger(EdmManifestMapping.class);

    private EdmManifestMapping() {
        // private constructor to prevent initialization
    }

    /**
     * Generates a IIIF v2 manifest based on the provided (parsed) json document
     * @param settings manifest settings object loaded from properties file
     * @param jsonDoc parsed json document
     * @return IIIF Manifest v2 object
     */
    public static ManifestV2 getManifestV2(ManifestSettings settings, Object jsonDoc) {
        String europeanaId = getEuropeanaId(jsonDoc);
        ManifestV2 manifest = new ManifestV2(europeanaId, getManifestId(europeanaId));
        manifest.setWithin(getWithinV2(jsonDoc));
        manifest.setLabel(getLabelsV2(jsonDoc));
        manifest.setDescription(getDescriptionV2(jsonDoc));
        manifest.setMetadata(getMetaDataV2(jsonDoc));
        manifest.setThumbnail(getThumbnailImageV2(settings, europeanaId, jsonDoc));
        manifest.setNavDate(getNavDate(europeanaId, jsonDoc));
        manifest.setAttribution(getAttributionV2(europeanaId, jsonDoc));
        manifest.setLicense(getLicense(europeanaId, jsonDoc));
        manifest.setSeeAlso(getDataSetsV2(europeanaId));
        manifest.setSequences(getSequencesV2(settings, europeanaId, jsonDoc));
        return manifest;
    }

    /**
     * Generates a IIIF v3 manifest based on the provided (parsed) json document
     * @param settings manifest settings object loaded from properties file
     * @param jsonDoc parsed json document
     * @return IIIF Manifest v3 object
     */
    public static ManifestV3 getManifestV3(ManifestSettings settings, Object jsonDoc) {
        String europeanaId = getEuropeanaId(jsonDoc);
        ManifestV3 manifest = new ManifestV3(europeanaId, getManifestId(europeanaId));
        manifest.setWithin(EdmManifestMapping.getWithinV3(jsonDoc));
        manifest.setLabel(EdmManifestMapping.getLabelsV3(jsonDoc));
        manifest.setDescription(EdmManifestMapping.getDescriptionV3(jsonDoc));
        // TODO implement rest of v3 that is currently commented out
        //manifest.setMetaData(EdmManifestMapping.getMetaDataV3());
        //manifest.setThumbnail(getThumbnailImageV3(settings, europeanaId, jsonDoc));
        manifest.setNavDate(getNavDate(europeanaId, jsonDoc));
        //manifest.setAttributionV3(getAttributionV3(europeanaId, jsonDoc));
        //manifest.setRights(getRights(europeanaId, jsonDoc));
        manifest.setSeeAlso(getDataSetsV3(europeanaId));
        //manifest.setSequences(getSequencesV3(settings, europeanaId, jsonDoc));
        return manifest;
    }

    /**
     * Extract the Europeana object ID from the 'about' field.
     * @param jsonDoc parsed json document
     * @return string containing the Europeana ID of the object (dataset ID and record ID separated by a slash)
     */
    public static String getEuropeanaId(Object jsonDoc) {
        return JsonPath.parse(jsonDoc).read("$.object.about", String.class);
    }

    /**
     * Create the IIIF manifest ID
     * @param europeanaId consisting of dataset ID and record ID separated by a slash (string should have a leading slash and not trailing slash)
     * @return string containing the IIIF manifest ID
     */
    public static String getManifestId(String europeanaId) {
        return Definitions.MANIFEST_ID.replace(Definitions.ID_PLACEHOLDER, europeanaId);
    }

    /**
     * Create a sequence ID
     * @param europeanaId consisting of dataset ID and record ID separated by a slash (string should have a leading slash and not trailing slash)
     * @param order number
     * @return string containing the sequence ID
     */
    public static String getSequenceId(String europeanaId, int order) {
        return Definitions.SEQUENCE_ID.replace(Definitions.ID_PLACEHOLDER, europeanaId).concat(Integer.toString(order));
    }

    /**
     * Create a canvas ID
     * @param europeanaId consisting of dataset ID and record ID separated by a slash (string should have a leading slash and not trailing slash)
     * @param order number
     * @return String containing the canvas ID
     */
    public static String getCanvasId(String europeanaId, int order) {
        return Definitions.CANVAS_ID.replace(Definitions.ID_PLACEHOLDER, europeanaId).concat(Integer.toString(order));
    }

    /**
     * Create an annotation ID
     * @param europeanaId consisting of dataset ID and record ID separated by a slash (string should have a leading slash and not trailing slash)
     * @param order number
     * @return String containing the annotation ID
     */
    public static String getAnnotationId(String europeanaId, int order) {
        return Definitions.ANNOTATION_ID.replace(Definitions.ID_PLACEHOLDER, europeanaId).concat(Integer.toString(order));
    }

    /**
     * Create a dataset ID (datasets information are part of the manifest)
     * @param europeanaId consisting of dataset ID and record ID separated by a slash (string should have a leading slash and not trailing slash)
     * @return string containing the dataset ID consisting of a base url, Europeana ID and postfix (rdf/xml, json or json-ld)
     */
    public static String getDatasetId(String europeanaId, String postFix) {
        return Definitions.DATASET_ID_BASE_URL + europeanaId + postFix;
    }

    /**
     *
     * @param jsonDoc parsed json document
     * @return
     */
    public static String getWithinV2(Object jsonDoc) {
        // TODO wait until V2 implementation for 'within' is clear
        return null;
    }

    /**
     * @param jsonDoc parsed json document
     * @return
     */
    public static Collection[] getWithinV3(Object jsonDoc) {
        List<Collection> result = new ArrayList<>();
        // TODO I think we can incorporate the startsWith into the JsonPath read
        List<String> collections = JsonPath.parse(jsonDoc).read("$.object.proxies[*].dctermsIsPartOf.def[*]", List.class);
        for (String collection : collections) {
            if (collection.toLowerCase(Locale.getDefault()).startsWith("http://data.theeuropeanlibrary.org")
                    || collection.toLowerCase(Locale.getDefault()).startsWith("https://data.theeuropeanlibrary.org")) {
                result.add(new Collection(collection));
            }
        }
        return result.toArray(new Collection[result.size()]);
    }

    /**
     * We first check all proxies for a title. If there are no titles, then we check the description fields
     * @param jsonDoc parsed json document
     * @return
     */
    public static LanguageMap getLabelsV3(Object jsonDoc)  {
        LanguageMap[] maps = JsonPath.parse(jsonDoc).read("$.object.proxies[*].dcTitle", LanguageMap[].class);
        if (maps == null || maps.length == 0) {
            maps = JsonPath.parse(jsonDoc).read("$.object.proxies[*].dcDescription", LanguageMap[].class);
        }
        return mergeLanguageMaps(maps);
    }

    /**
     * We first check all proxies for a title. If there are no titles, then we check the description fields
     * @param jsonDoc parsed json document
     * @return array of LanguageObject
     */
    public static LanguageObject[] getLabelsV2(Object jsonDoc) {
        // we read everything in as LanguageMap[] because that best matches the EDM implementation, then we convert to LanguageObjects[]
        LanguageMap labelsV3 = getLabelsV3(jsonDoc);
        if (labelsV3 == null) {
            return null;
        }
        return EdmManifestMapping.langMapsToObjects(labelsV3);
    }

    /**
     * This converts a LanguageMap array (v3) to a LanguageObject array (v2).
     */
    private static LanguageObject[] langMapsToObjects(LinkedHashMap<String, String[]> map) {
        if (map == null) {
            return null;
        }
        List<LanguageObject> result = new ArrayList<>();
            for (Map.Entry<String, String[]> entry : map.entrySet()) {
                String language = entry.getKey();
                String[] values = entry.getValue();
                for (String value: values) {
                    result.add(new LanguageObject(language, value));
                }
            }
        if (result.isEmpty()) {
            return null;
        }
        return result.toArray(new LanguageObject[result.size()]);
    }

    /**
     * This merges an array of languagemaps into a single languagemap. We also check for empty maps and return null if
     * the provided array is empty
     */
    private static LanguageMap mergeLanguageMaps(LanguageMap[] maps) {
        if (maps == null || maps.length == 0) {
            return null;
        } else if (maps.length == 1) {
            return maps[0];
        }
        LanguageMap result = new LanguageMap();
        for (LanguageMap map : maps) {
            // we should not have duplicate keys in our data, but we check for that if debug is enabled
            if (LOG.isDebugEnabled()) {
                for(String key : map.keySet()) {
                    if (result.keySet().contains(key)) {
                        LOG.warn("Duplicate key found when merging language maps: key = {}", key);
                    }
                }
            }
            result.putAll(map);
        }
        return result;
    }

    /**
     * Returns the values from the proxy.dcDescription fields, but only if they aren't used as a label yet.
     * @param jsonDoc parsed json document
     * @return
     */
    public static LanguageMap getDescriptionV3(Object jsonDoc) {
        if (JsonPath.parse(jsonDoc).read("$.object.proxies[*].dcTitle", LanguageMap[].class).length > 0) {
            return mergeLanguageMaps(JsonPath.parse(jsonDoc).read("$.object.proxies[*].dcDescription", LanguageMap[].class));
        }
        return null;
    }

    /**
     * Returns the values from the proxy.dcDescription fields, but only if they aren't used as a label yet.
     * @param jsonDoc parsed json document
     * @return
     */
    public static LanguageObject[] getDescriptionV2(Object jsonDoc) {
        // we read everything in as LanguageMap[] because that best matches the EDM implementation, then we convert to LanguageObjects[]
        LanguageMap descriptionsV3 = getDescriptionV3(jsonDoc);
        if (descriptionsV3 == null) {
            return null;
        }
        return EdmManifestMapping.langMapsToObjects(getDescriptionV3(jsonDoc));
    }

    /**
     * Reads the dcDate, dcFormat, dcRelation, dcType, dcLanguage and dcSource values from all proxies and puts them in a
     * map with the appropriate label
     * @param jsonDoc parsed json document
     * @return
     */
    public static eu.europeana.iiif.model.v2.MetaData[] getMetaDataV2(Object jsonDoc) {
        Map<String, List<LanguageObject>> data = new LinkedHashMap<>();
        addMetaData(data, JsonPath.parse(jsonDoc).read("$.object.proxies[*].dcDate", LanguageMap[].class), "date");
        addMetaData(data, JsonPath.parse(jsonDoc).read("$.object.proxies[*].dcFormat", LanguageMap[].class), "format");
        addMetaData(data, JsonPath.parse(jsonDoc).read("$.object.proxies[*].dcRelation", LanguageMap[].class), "relation");
        addMetaData(data, JsonPath.parse(jsonDoc).read("$.object.proxies[*].dcType", LanguageMap[].class), "type");
        addMetaData(data, JsonPath.parse(jsonDoc).read("$.object.proxies[*].dcLanguage", LanguageMap[].class), "language");
        addMetaData(data, JsonPath.parse(jsonDoc).read("$.object.proxies[*].dcSource", LanguageMap[].class), "source");

        List<eu.europeana.iiif.model.v2.MetaData> result = new LinkedList<>();
        for (Map.Entry<String, List<LanguageObject>> entry : data.entrySet()) {
            String label = entry.getKey();
            List<LanguageObject> values = entry.getValue();
            result.add(new eu.europeana.iiif.model.v2.MetaData(label, values.toArray(new LanguageObject[values.size()])));
        }

        if (result.isEmpty()) {
            return null;
        }
        return result.toArray(new eu.europeana.iiif.model.v2.MetaData[result.size()]);
    }

    /**
     * We read in metadata as a LanguageMap[], but we need to convert it to Map consisting of labels and List<LanguageObjects>
     * Also if the key is 'def' we should leave that out (for v2)
     */
    private static void addMetaData(Map<String, List<LanguageObject>> metaData, LanguageMap[] dataToAdd, String fieldName) {
        for (LanguageMap map : dataToAdd) {
            for (Map.Entry<String, String[]> entry : map.entrySet()) {
                String language = entry.getKey();
                String[] values = entry.getValue();
                for (String value: values) {
                    List<LanguageObject> langObjects;
                    if (!metaData.keySet().contains(fieldName)) {
                        langObjects = new ArrayList<>();
                        metaData.put(fieldName, langObjects);
                    } else {
                        langObjects = metaData.get(fieldName);
                    }
                    langObjects.add(new LanguageObject(language, value));
                }
            }
        }
    }

    /**
     * Return an with the id of the thumbnail as defined in 'europeanaAggregation.edmPreview'
     * @param settings manifest settings object loaded from properties file
     * @param jsonDoc parsed json document
     * @return Image object, or null if no edmPreview was found
     */
    public static eu.europeana.iiif.model.v2.Image getThumbnailImageV2(ManifestSettings settings, String europeanaId, Object jsonDoc) {
        String[] thumbnailIds = JsonPath.parse(jsonDoc).read("$.object.europeanaAggregation[?(@.edmPreview)].edmPreview", String[].class);
        String thumbnailId = (String) getFirstValueArray("thumbnail ids", europeanaId, thumbnailIds);
        if (StringUtils.isEmpty(thumbnailId)) {
            return null;
        }
        // TODO ?? implement width based on image = large/medium
        return new eu.europeana.iiif.model.v2.Image(thumbnailId, null, null);
    }

    /**
     * Return the first dctermsIssued date we can find in a proxy
     * Note that we assume that the desired value is in a mapping with a 'def' key
     * @param europeanaId consisting of dataset ID and record ID separated by a slash (string should have a leading slash and not trailing slash)
     * @param jsonDoc parsed json document
     * @return date string in xsd:datetime format (i.e. YYYY-MM-DDThh:mm:ssZ)
     */
    public static String getNavDate(String europeanaId, Object jsonDoc) {
        LocalDate navDate = null;
        LanguageMap[] proxiesLangDates = JsonPath.parse(jsonDoc).read("$.object.proxies[*].dctermsIssued", LanguageMap[].class);
        for (LanguageMap langDates : proxiesLangDates) {
            for (String[] dates : langDates.values()) {
                // we assume there is only 1 value here
                String date = (String) getFirstValueArray("navDate", europeanaId, dates);
                // TODO make EdmDateStringToDate handle more different date strings (see EA-990)
                navDate = EdmDateUtils.dateStringToDate(date);
                if (navDate != null) {
                    break;
                }
            }
            if (navDate != null) {
                break;
            }
        }

        if (navDate == null) {
            return null;
        }
        ZonedDateTime zdt = Timestamp.valueOf(navDate.atStartOfDay()).toLocalDateTime().atZone(ZoneOffset.UTC);
        return zdt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    /**
     * Return attribution text as a String
     * We take the value from the first 'textAttributionSnippet' field of a webResource we find in any aggregation.
     * @param europeanaId consisting of dataset ID and record ID separated by a slash (string should have a leading slash and not trailing slash)
     * @param jsonDoc parsed json document
     * @return
     */
    public static String getAttributionV2(String europeanaId, Object jsonDoc) {
        String[] attributions = JsonPath.parse(jsonDoc).read("$.object.aggregations[*].webResources[*].textAttributionSnippet", String[].class);
        // since there are usually many webresources we simply just pick the first one and ignore if there are others.
        String attribution = (String) getFirstValueArray(null, europeanaId, attributions);
        if (!StringUtils.isEmpty(attribution)) {
            return attribution;
        }
        return null;
    }
    
    /**
     * Return the first license description we find in any 'aggregation.edmRights' field. Note that we first try the europeanaAggregation and if
     * that doesn't contain an edmRights, we check the other aggregations
     * @param europeanaId consisting of dataset ID and record ID separated by a slash (string should have a leading slash and not trailing slash)
     * @param jsonDoc parsed json document
     * @return
     */
    public static String getLicense(String europeanaId, Object jsonDoc) {
        // edmRights may not exists in the europeanaAggregation
        LanguageMap[] licenseMaps = JsonPath.parse(jsonDoc).read("$.object.europeanaAggregation[?(@.edmRights)].edmRights", LanguageMap[].class);
        LanguageMap licenseMap = (LanguageMap) getFirstValueArray("licenseMap", europeanaId, licenseMaps);
        if (licenseMap == null || licenseMap.values().isEmpty()) {
            LanguageMap[] licenses = JsonPath.parse(jsonDoc).read("$.object.aggregations[*].edmRights", LanguageMap[].class);
            licenseMap = (LanguageMap) getFirstValueArray("license", europeanaId, licenses);
        }

        if (licenseMap != null && !licenseMap.values().isEmpty()) {
            return (String) getFirstValueArray("license text", europeanaId, licenseMap.values().iterator().next());
        }
        return null;
    }

    /**
     * Generates 3 datasets with the appropriate ID and format (one for rdf/xml, one for json and one for json-ld)
     * @param europeanaId consisting of dataset ID and record ID separated by a slash (string should have a leading slash and not trailing slash)
     * @return array of 3 datasets
     */
    public static eu.europeana.iiif.model.v2.DataSet[] getDataSetsV2(String europeanaId) {
        eu.europeana.iiif.model.v2.DataSet[] result = new eu.europeana.iiif.model.v2.DataSet[3];
        result[0] = new eu.europeana.iiif.model.v2.DataSet(getDatasetId(europeanaId, ".json-ld"), Definitions.MEDIA_TYPE_JSONLD);
        result[1] = new eu.europeana.iiif.model.v2.DataSet(getDatasetId(europeanaId, ".json"), MediaType.APPLICATION_JSON_VALUE);
        result[2] = new eu.europeana.iiif.model.v2.DataSet(getDatasetId(europeanaId, ".rdf"), Definitions.MEDIA_TYPE_RDF);
        return result;
    }

    /**
     * Generates 3 datasets with the appropriate ID and format (one for rdf/xml, one for json and one for json-ld)
     * @param europeanaId consisting of dataset ID and record ID separated by a slash (string should have a leading slash and not trailing slash)
     * @return array of 3 datasets
     */
    public static eu.europeana.iiif.model.v3.DataSet[] getDataSetsV3(String europeanaId) {
        eu.europeana.iiif.model.v3.DataSet[] result = new eu.europeana.iiif.model.v3.DataSet[3];
        result[0] = new eu.europeana.iiif.model.v3.DataSet(getDatasetId(europeanaId, ".json-ld"), Definitions.MEDIA_TYPE_JSONLD);
        result[1] = new eu.europeana.iiif.model.v3.DataSet(getDatasetId(europeanaId, ".json"), MediaType.APPLICATION_JSON_VALUE);
        result[2] = new eu.europeana.iiif.model.v3.DataSet(getDatasetId(europeanaId, ".rdf"), Definitions.MEDIA_TYPE_RDF);
        return result;
    }

    /**
     *
     * @param europeanaId consisting of dataset ID and record ID separated by a slash (string should have a leading slash and not trailing slash)
     * @param settings manifest settings object loaded from properties file*
     * @param jsonDoc parsed json document
     * @return
     */
    public static eu.europeana.iiif.model.v2.Sequence[] getSequencesV2(ManifestSettings settings, String europeanaId, Object jsonDoc) {
        String edmIsShownBy = (String) getFirstValueArray("edmIsShownBy", europeanaId,
                JsonPath.parse(jsonDoc).read("$.object.aggregations[*].edmIsShownBy", String[].class));
        List<WebResource> webResources = getWebResources(edmIsShownBy, jsonDoc);
        Map<String, Object>[] services = JsonPath.parse(jsonDoc).read("$.object[?(@.services)].services[*]", Map[].class);

        // create canvases in a particular order
        List<WebResource> sorted;
        try {
            sorted = WebResourceSorter.sort(webResources);
        } catch (DataInconsistentException e) {
            LOG.error("Error trying to sort webresources for {}. Cause: {}", europeanaId, e);
            sorted = webResources;
        }
        int order = 1;
        List<eu.europeana.iiif.model.v2.Canvas> canvases = new LinkedList<>();
        for (WebResource webResource: sorted) {
            canvases.add(getCanvas(settings, europeanaId, order, webResource, services));
            order++;
        }

        if (!canvases.isEmpty()) {
            // there should be only 1 sequence, so order number is always 1
            eu.europeana.iiif.model.v2.Sequence[] result = new eu.europeana.iiif.model.v2.Sequence[1];
            result[0] = new eu.europeana.iiif.model.v2.Sequence(getSequenceId(europeanaId, 1), edmIsShownBy);
            result[0].setStartCanvas(getCanvasId(europeanaId, 1));
            result[0].setCanvases(canvases.toArray(new eu.europeana.iiif.model.v2.Canvas[canvases.size()]));
            return result;
        }
        return null;
    }

    /**
     * We should only generate a canvas for webresources that are either in the edmIsShownBy or in the hasViews
     * @param edmIsShownBy
     * @param jsonDoc
     * @return list of webresourcs that are either edmIsShownBy or hasView
     */
    private static List<WebResource> getWebResources(String edmIsShownBy, Object jsonDoc) {

        String[][] hasViews = JsonPath.parse(jsonDoc).read("$.object.aggregations[*].hasView", String[][].class);

        List<String> validWebResources = new ArrayList<>();
        validWebResources.add(edmIsShownBy);
        for (String[] hasView : hasViews) {
            validWebResources.addAll(Arrays.asList(hasView));
        }

        // get all webresources and check if they are edmIsShownBy or hasView
        WebResource[] webResources = JsonPath.parse(jsonDoc).read("$.object.aggregations[*].webResources[*]", WebResource[].class);
        List<WebResource> result = new ArrayList<>();
        for (WebResource wr : webResources) {
            if (validWebResources.contains(wr.getId())) {
                result.add(wr);
                LOG.debug("Valid webresource {} ", wr.getId());
            } else {
                LOG.debug("Skipping webresource {}", wr.getId());
            }
        }
        return result;
    }

    /**
     * Generates a new canvas, but note that we do not fill the otherContent (Full-Text) here. That is done later
     */
    private static eu.europeana.iiif.model.v2.Canvas getCanvas(ManifestSettings settings,
                                                               String europeanaId,
                                                               int order,
                                                               WebResource webResource,
                                                               Map<String, Object>[] services) {
        eu.europeana.iiif.model.v2.Canvas c = new eu.europeana.iiif.model.v2.Canvas(settings, getCanvasId(europeanaId, order), order);

        c.setLabel("p. "+order);

        String attributionText = (String) webResource.get("textAttributionSnippet");
        if (!StringUtils.isEmpty(attributionText)){
            c.setAttribution(attributionText);
        }

        LinkedHashMap<String, ArrayList<String>> license = (LinkedHashMap<String, ArrayList<String>>) webResource.get("webResourceEdmRights");
        if (license != null && !license.values().isEmpty()) {
            c.setLicense(license.values().iterator().next().get(0));
        }

        c.setImages(new eu.europeana.iiif.model.v2.Annotation[1]);
        c.getImages()[0] = new eu.europeana.iiif.model.v2.Annotation(getAnnotationId(europeanaId, order));
        c.getImages()[0].setOn(c.getId());

        eu.europeana.iiif.model.v2.AnnotationBody body = new eu.europeana.iiif.model.v2.AnnotationBody((String) webResource.get("about"));
        String ebuCoreMimeType = (String) webResource.get("ebuCoreHasMimeType");
        if (!StringUtils.isEmpty(ebuCoreMimeType)) {
            body.setFormat(ebuCoreMimeType);
        }

        List<String> serviceIds = (List<String>) webResource.get("svcsHasService");
        if (serviceIds != null && !serviceIds.isEmpty()) {
            String serviceId = (String) getFirstValueArray("service", europeanaId, serviceIds.toArray());
            LOG.debug("WebResource {} has serviceId {}", webResource.getId(), serviceId);
            eu.europeana.iiif.model.v2.Service service = new eu.europeana.iiif.model.v2.Service(serviceId);
            service.setProfile(lookupServiceDoapImplements(services, serviceId, europeanaId));
            body.setService(service);
        } else {
            LOG.debug("No serviceId for webresource {}", webResource.getId());
        }
        c.getImages()[0].setResource(body);
        return c;
    }

    /**
     * Check if the array of services contains a service with the provided serviceId. If so we retrieve the doapImplements
     * field from that service;
     */
    private static String lookupServiceDoapImplements(Map<String, Object>[] services, String serviceId, String europeanaId) {
        String result = null;
        for (Map<String, Object> s : services) {
            String sId = (String) s.get("about");
            if (sId != null && sId.equalsIgnoreCase(serviceId)) {
                // Note: there is a problem with cardinality of the doapImplements field. It should be a String, but at the moment
                // it is defined in EDM as a String[]. So we need to check what we get here.
                Object doapImplements = s.get("doapImplements");
                if (doapImplements == null) {
                    LOG.warn("Record {} has service {} with no doapImplements field", europeanaId, serviceId);
                } else if (doapImplements instanceof List) {
                    result = ((List<String>) doapImplements).get(0);
                } else {
                    result = (String) doapImplements;
                }
                break;
            }
        }
        if (result == null) {
            LOG.warn("Record {} defined service {} in webresource, but no such service is defined (with a doapImplements field)", europeanaId, serviceId);
        }
        return result;
    }

    /**
     * In many cases we assume there will be only 1 proxy or aggregation with the provided value, so this method helps
     * to retrieve the first value object while providing a warning if there are more values than expected.
     * @param fieldName optional, if not null we log a warning if there is more than 1 expected value
     * @param europeanaId
     * @param values
     * @return first value object from the array of values
     */
    private static Object getFirstValueArray(String fieldName, String europeanaId, Object[] values) {
        if (values.length >= 1) {
            if (!StringUtils.isEmpty(fieldName) && values.length > 1) {
                LOG.warn("Multiple {} values found for record {}, returning first", fieldName, europeanaId);
            }
            return values[0];
        }
        return null;
    }

    /**
     * Parses record information in json format and returns the record's 'timestamp_update' value
     * @param json
     * @return LocalDateTime object with the record's 'timestamp_update' value (UTC)
     */
    public static ZonedDateTime getRecordTimestampUpdate(String json) {
       String date = JsonPath.parse(json).read("$.object.timestamp_update", String.class);
       if (StringUtils.isEmpty(date)) {
           return null;
       }
       return EdmDateUtils.recordTimestampToDateTime(date);
    }
}
