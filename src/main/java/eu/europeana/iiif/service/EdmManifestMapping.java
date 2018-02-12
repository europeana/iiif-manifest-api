package eu.europeana.iiif.service;

import com.jayway.jsonpath.JsonPath;
import eu.europeana.iiif.model.Definitions;
import eu.europeana.iiif.model.v2.DataSet;
import eu.europeana.iiif.model.v2.LanguageObject;
import eu.europeana.iiif.model.v2.ManifestV2;
import eu.europeana.iiif.model.v3.Collection;
import eu.europeana.iiif.model.v3.LanguageMap;
import eu.europeana.iiif.model.v3.ManifestV3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * This class contains all the methods for mapping EDM record data to IIIF Manifest data for both IIIF v2 and v3
 *
 * @author Patrick Ehlert
 * Created on 08-02-2018
 */
public class EdmManifestMapping {

    private static final Logger LOG = LogManager.getLogger(EdmManifestMapping.class);

    private EdmManifestMapping() {
        // private constructor to prevent initialization
    }

    /**
     * Generates a IIIF v2 manifest based on the provided (parsed) json document
     * @param jsonDoc
     * @return IIIF Manifest v2 object
     */
    public static ManifestV2 getManifestV2(Object jsonDoc) {
        String europeanaId = getEuropeanaId(jsonDoc);
        ManifestV2 manifest = new ManifestV2(getManifestId(europeanaId));
        manifest.setWithin(getWithinV2(jsonDoc));
        manifest.setLabel(getLabelsV2(jsonDoc));
        manifest.setDescription(getDescriptionV2(jsonDoc));
        manifest.setMetadata(getMetaDataV2(jsonDoc));
        manifest.setThumbnail(getThumbnailImage(europeanaId, jsonDoc));
        manifest.setNavDate(getNavDate(europeanaId, jsonDoc));
        manifest.setAttribution(getAttribution(europeanaId, jsonDoc));
        manifest.setLicense(getLicense(europeanaId, jsonDoc));
        manifest.setSeeAlso(getDataSets(europeanaId));
        manifest.setSequences(getSequencesV2(europeanaId, jsonDoc));
        return manifest;
    }

    /**
     * Generates a IIIF v3 manifest based on the provided (parsed) json document
     * @param jsonDoc
     * @return IIIF Manifest v3 object
     */
    public static ManifestV3 getManifestV3(Object jsonDoc) {
        String europeanaId = getEuropeanaId(jsonDoc);
        ManifestV3 manifest = new ManifestV3(getManifestId(europeanaId));
        manifest.setWithin(EdmManifestMapping.getWithinV3(jsonDoc));
//        manifest.setLabel(EdmManifestMapping.getLabelsV3(jsonDoc));
//        manifest.setDescription(EdmManifestMapping.getDescriptionV3(jsonDoc));
        return manifest;
    }

    /**
     * Extract the Europeana object ID from the 'about' field.
     * @param jsonDoc
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
        return Definitions.SEQUENCE_ID.replace(Definitions.ID_PLACEHOLDER, europeanaId).concat(Integer.valueOf(order).toString());
    }

    /**
     * Create a canvas ID
     * @param europeanaId consisting of dataset ID and record ID separated by a slash (string should have a leading slash and not trailing slash)
     * @param order number
     * @return String containing the canvas ID
     */
    public static String getCanvasId(String europeanaId, int order) {
        return Definitions.CANVAS_ID.replace(Definitions.ID_PLACEHOLDER, europeanaId).concat(Integer.valueOf(order).toString());
    }

    /**
     * Create an annotation ID
     * @param europeanaId consisting of dataset ID and record ID separated by a slash (string should have a leading slash and not trailing slash)
     * @param order number
     * @return String containing the annotation ID
     */
    public static String getAnnotationId(String europeanaId, int order) {
        return Definitions.ANNOTATION_ID.replace(Definitions.ID_PLACEHOLDER, europeanaId).concat(Integer.valueOf(order).toString());
    }

    /**
     * Constuct the Dataset ID (datasets information are part of the manifest)
     * @param europeanaId consisting of dataset ID and record ID separated by a slash (string should have a leading slash and not trailing slash)
     * @return string containing the dataset ID consisting of a base url, Europeana ID and postfix (rdf/xml, json or json-ld)
     */
    public static String getDatasetId(String europeanaId, String postFix) {
        return Definitions.DATASET_ID_BASE_URL + europeanaId + postFix;
    }

    public static String getWithinV2(Object jsonDoc) {
        // TODO wait until V2 implementation for within is clear
        return null;
    }


    public static Collection[] getWithinV3(Object jsonDoc) {
        List<Collection> result = new ArrayList<>();
        // TODO I think we can incorporate the startsWith into the JsonPath read
        List<String> collections = JsonPath.parse(jsonDoc).read("$.object.proxies[*].dctermsIsPartOf.def[*]", List.class);
        for (String collection : collections) {
            if (collection.toLowerCase().startsWith("http://data.theeuropeanlibrary.org") || collection.toLowerCase().startsWith("https://data.theeuropeanlibrary.org")) {
                result.add(new Collection(collection));
            }
        }
        return result.toArray(new Collection[result.size()]);
    }

    public static LanguageMap[] getLabelsV3(Object jsonDoc)  {
        LanguageMap[] maps = JsonPath.parse(jsonDoc).read("$.object.proxies[*].dcTitle", LanguageMap[].class);
        if (maps.length == 0) {
            LOG.debug("No title, checking description");
            maps = JsonPath.parse(jsonDoc).read("$.object.proxies[*].dcDescription", LanguageMap[].class);
        }
        // TODO what if there is no title and no description
        return maps;
    }

    public static LanguageObject[] getLabelsV2(Object jsonDoc) {
        return EdmManifestMapping.langMapsToObjects(getLabelsV3(jsonDoc));
    }

    /**
     * Returns values of all proxy/description in EDM, but only if
     * @param jsonDoc
     * @return
     */
    public static LanguageMap[] getDescriptionV3(Object jsonDoc) {
        if (JsonPath.parse(jsonDoc).read("$.object.proxies[*].dcTitle", LanguageMap[].class).length > 0) {
            return JsonPath.parse(jsonDoc).read("$.object.proxies[*].dcDescription", LanguageMap[].class);
        }
        return null;
    }

    public static LanguageObject[] getDescriptionV2(Object jsonDoc) {
        return EdmManifestMapping.langMapsToObjects(getDescriptionV3(jsonDoc));
    }

    public static eu.europeana.iiif.model.v2.MetaData[] getMetaDataV2(Object jsonDoc) {
        // we add to a map first to avoid duplicate data (or perhaps combine data if this is necessary)
        // TODO think if we can construct MetaData objects right away
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

        return result.toArray(new eu.europeana.iiif.model.v2.MetaData[result.size()]);
    }

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
     * Return an with the id of the thumbnail as defined in 'ore:Aggregation/edm:preview'
     * Note that we assume there is only 1 aggregation with an edmPreview field that contains a single thumbnail url
     * @param jsonDoc
     * @return Image object, or null if no edmPreview was found
     */
    public static eu.europeana.iiif.model.v2.Image getThumbnailImage(String europeanaId, Object jsonDoc) {
        String[] thumbnails = JsonPath.parse(jsonDoc).read("$.object.aggregations[*].edmPreview", String[].class);
        String thumbnailId = (String) getFirstValueArray("thumbnail", europeanaId, thumbnails);
        if (!StringUtils.isEmpty(thumbnailId)) {
            return new eu.europeana.iiif.model.v2.Image(thumbnailId);
        }
        // TODO load thumbnail and set width and height? What if it's not available

        return null;
    }

    /**
     * Return the default dctermsIssued date
     * Note that we assume that the desired value is in a mapping with a 'def' key
     * @param jsonDoc
     * @return
     */
    public static String getNavDate(String europeanaId, Object jsonDoc) {
        // TODO check: Can we always take value from first proxy Also should I  always take def value?
        // TODO check: specs say (xsd: dateTime), does this mean dates are not always in format YYYY-MM-DD and this needs to be checked?
        String[][] proxiesDates = JsonPath.parse(jsonDoc).read("$.object.proxies[*].dctermsIssued.def", String[][].class);
        String[] navDates = (String[]) getFirstValueArray(" proxy navDates", europeanaId, proxiesDates);
        String navDate = (String) getFirstValueArray("navDate", europeanaId, navDates);
        if (!StringUtils.isEmpty(navDate)) {
            return navDate;
        }
        return null;
    }

    /**
     * Return attribution text as a languageObject
     * We take the attribution from he first webResource we find that has a 'textAttributionSnippet' field
     * @param jsonDoc
     * @return
     */
    public static String getAttribution(String europeanaId, Object jsonDoc) {
        // TODO check: can we assume there is only 1 aggregation?
        String[] attributions = JsonPath.parse(jsonDoc).read("$.object.aggregations[0].webResources[*].textAttributionSnippet", String[].class);
        String attribution = (String) getFirstValueArray("attribution", europeanaId, attributions);
        if (!StringUtils.isEmpty(attribution)) {
            return attribution;
        }
        return null;
    }

    /**
     * Return the license description as defined in 'ore:Aggregation/edm:rights'
     * Note that we assume there is only 1 aggregation with an edmRights mapping with a 'def' key that contains a single license text value
     * // TODO check: aggregations until we find one with edmRights?
     * @param jsonDoc
     * @return
     */
    public static String getLicense(String europeanaId, Object jsonDoc) {
        String[][] licenses = JsonPath.parse(jsonDoc).read("$.object.aggregations[*].edmRights.def", String[][].class);
        String[] licenseTexts = (String[]) getFirstValueArray("license", europeanaId, licenses);
        return (String) getFirstValueArray("license text", europeanaId, licenseTexts);
    }

    /**
     * Generates 3 datasets with the appropriate Id and format (one for rdf/xml, one for json and one for json-ld)
     * @param europeanaId consisting of 'dataset-id/record-id'
     * @return array of 3 datasets
     */
    public static DataSet[] getDataSets(String europeanaId) {
        DataSet[] result = new DataSet[3];
        // TODO check: record API says json-ld is of type 'application/json;charset=UTF-8'. Do we still hang on to "application/ld+json";
        // TODO check: add charset=UTF-8 to mediatypes?
        result[0] = new DataSet(getDatasetId(europeanaId, ".json-ld"), Definitions.MEDIA_TYPE_JSONLD);
        result[1] = new DataSet(getDatasetId(europeanaId, ".json"), MediaType.APPLICATION_JSON_VALUE);
        result[2] = new DataSet(getDatasetId(europeanaId, ".rdf"), Definitions.MEDIA_TYPE_RDF);
        return result;
    }

    public static eu.europeana.iiif.model.v2.Sequence[] getSequencesV2(String europeanaId, Object jsonDoc) {

        // TODO check: make sure we only need to check nextInSequence of webresources and have nothing to do withnot edmNextInSequence of proxy

        // TODO check: what if webresource doesn't have edmNextInSequence? or if there are no webresources. Do we still create a sequence without canvases, or not?

        // TODO check: can I assume there is always only 1 aggregation with webresources?
        Map<String, Object>[] webResources = JsonPath.parse(jsonDoc).read("$.object.aggregations[0].webResources[*]", Map[].class);

        // need to check in reverse order
        int order = 1;
        List<eu.europeana.iiif.model.v2.Canvas> canvases = new LinkedList<>();
        for (int i = webResources.length - 1; i >= 0; i--) {
            Map<String, Object> webResource = webResources[i];
            if (webResource.keySet().contains("isNextInSequence")) {
                canvases.add(getCanvas(europeanaId, order, webResource));
                order++;
            }
        }

        // TODO check: I assume that we do not have to make a sequence if there are no webresources with edmNextInSequence
        if (canvases.size() > 0) {
            // there should be only 1 sequence, so order number is always 1
            eu.europeana.iiif.model.v2.Sequence[] result = new eu.europeana.iiif.model.v2.Sequence[1];
            result[0] = new eu.europeana.iiif.model.v2.Sequence(getSequenceId(europeanaId, 1));
            result[0].setStartCanvas(getCanvasId(europeanaId, 1));
            result[0].setCanvases(canvases.toArray(new eu.europeana.iiif.model.v2.Canvas[canvases.size()]));
            return result;
        }
        return null;
    }

    private static eu.europeana.iiif.model.v2.Canvas getCanvas(String europeanaId, int order, Map<String, Object> webResource) {
        eu.europeana.iiif.model.v2.Canvas c = new eu.europeana.iiif.model.v2.Canvas(europeanaId);
        c.label = "p. "+order;
        // TODO check: textAttributionSnippet is a string. For now I created a langObject with name 'def'
        String attributionText = (String) webResource.get("textAttributionSnippet");
        if (!StringUtils.isEmpty(attributionText)){
            c.attribution = new LanguageObject("def", attributionText);
        }
        // TODO check if the name is correct, haven't been able to find an example where this is used
        String license = (String) webResource.get("edmRights");
        if (!StringUtils.isEmpty(license)) {
            c.license = license;
        }

        // TODO check if we should really copy order from canvas, or always supply 1 (as there will be ony 1 annotation)
        c.images = new eu.europeana.iiif.model.v2.Annotation[1];
        c.images[0] = new eu.europeana.iiif.model.v2.Annotation(getAnnotationId(europeanaId, order));
        c.images[0].setOn(c.getId());
        // TODO check: what should the id be of the annotation body? the about of the webresource?
        eu.europeana.iiif.model.v2.AnnotationBody body = new eu.europeana.iiif.model.v2.AnnotationBody((String) webResource.get("about"));
        // TODO: I guess i can leave format empty if there is no ebucoreHasMimeType?
        String ebuCoreMimeType = (String) webResource.get("ebuCoreHasMimeType");
        if (!StringUtils.isEmpty(ebuCoreMimeType)) {
            body.setFormat(ebuCoreMimeType);
        }
        // TODO can I assume there is always only 1 service at most/
        List<String> services = (List<String>) webResource.get("svcsHasService");
        if (services.size() > 0) {
            String serviceId = (String) getFirstValueArray("service", europeanaId, services.toArray());
            eu.europeana.iiif.model.v2.Service s = new eu.europeana.iiif.model.v2.Service(serviceId);
            // TODO what value should we set for profile!?!? doapImplements??
            s.setProfile("");
            body.setService(s);
        }
        c.images[0].setResource(body);
        return c;
    }

    /**
     * In many cases we assume there will be only 1 proxy or aggregation with the provided value, so this method helps
     * to retrieve the first value object while providing a warning if there are more values than expected.
     * @param fieldName
     * @param europeanaId
     * @param values
     * @return first value object from the array of values
     */
    private static Object getFirstValueArray(String fieldName, String europeanaId, Object[] values) {
        if (values.length == 0) {
            return null;
        } else if (values.length > 1) {
            LOG.warn("Multiple {} values found for record {}, returning first", fieldName, europeanaId);
        }
        return values[0];
    }

    /**
     * This converts a LanguageMap array (v3) to a LanguageObject array (v2).
     * Note: We often use LanguageMaps because it matches how language is implemented in EDM. So the easiest solution often
     * is to read values into a v3 LanguageMap and then convert it to v2 LanguageObjects using this method
     * @param maps
     * @return array of languageObjects with the same data as the provided languageMap
     */
    private static LanguageObject[] langMapsToObjects(LinkedHashMap<String, String[]>[] maps) {
        List<LanguageObject> result = new ArrayList<>();
        for (LinkedHashMap<String, String[]> map : maps) {
            for (Map.Entry<String, String[]> entry : map.entrySet()) {
                String language = entry.getKey();
                String[] values = entry.getValue();
                for (String value: values) {
                    result.add(new LanguageObject(language, value));
                }
            }
        }
        if (result.size() == 0) {
            return null;
        }
        return result.toArray(new LanguageObject[result.size()]);
    }

}
