package eu.europeana.iiif.service;

import com.jayway.jsonpath.Filter;
import com.jayway.jsonpath.JsonPath;
import eu.europeana.iiif.model.Definitions;
import eu.europeana.iiif.model.WebResource;
import eu.europeana.iiif.model.WebResourceSorter;
import eu.europeana.iiif.model.v2.LanguageObject;
import eu.europeana.iiif.model.v2.ManifestV2;
import eu.europeana.iiif.model.v3.Collection;
import eu.europeana.iiif.model.v3.*;
import eu.europeana.iiif.service.exception.DataInconsistentException;
import eu.europeana.metis.mediaprocessing.extraction.ResourceType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.jayway.jsonpath.Criteria.where;
import static com.jayway.jsonpath.Filter.filter;

/**
 * This class contains all the methods for mapping EDM record data to IIIF Manifest data for both IIIF v2 and v3
 *
 * @author Patrick Ehlert
 * Created on 08-02-2018
 */
// ignore sonarqube rule: we return null on purpose in this class
// ignore pmd rule:  we want to make a clear which objects are v2 and which v3
@SuppressWarnings({"squid:S1168", "pmd:UnnecessaryFullyQualifiedName"})
public final class EdmManifestMapping {

    private static final Logger LOG = LogManager.getLogger(EdmManifestMapping.class);

    private static final String ABOUT = "about";
    private static final String TEXT_ATTRIB_SNIPPET = "textAttributionSnippet";
    private static final String HTML_ATTRIB_SNIPPET = "htmlAttributionSnippet";
    private static final String EBUCORE_HEIGHT = "ebucoreHeight";
    private static final String EBUCORE_WIDTH = "ebucoreWidth";

    private EdmManifestMapping() {
        // private constructor to prevent initialization
    }

    /**
     * Generates a IIIF v2 manifest based on the provided (parsed) json document
     * @param jsonDoc parsed json document
     * @return IIIF Manifest v2 object
     */
    static ManifestV2 getManifestV2(Object jsonDoc) {
        String europeanaId = getEuropeanaId(jsonDoc);
        String isShownBy = getIsShownBy(europeanaId, jsonDoc);
        ManifestV2 manifest = new ManifestV2(europeanaId, Definitions.getManifestId(europeanaId), isShownBy);
        manifest.setWithin(getWithinV2(jsonDoc));
        manifest.setLabel(getLabelsV2(jsonDoc));
        manifest.setDescription(getDescriptionV2(jsonDoc));
        manifest.setMetadata(getMetaDataV2(jsonDoc));
        manifest.setThumbnail(getThumbnailImageV2(europeanaId, jsonDoc));
        manifest.setNavDate(getNavDate(europeanaId, jsonDoc));
        manifest.setAttribution(getAttributionV2(europeanaId, isShownBy, jsonDoc));
        manifest.setLicense(getLicense(europeanaId, jsonDoc));
        manifest.setSeeAlso(getDataSetsV2(europeanaId));
        manifest.setSequences(getSequencesV2(europeanaId, isShownBy, jsonDoc));
        if (manifest.getSequences() != null) {
            manifest.setStartCanvasPageNr(getStartCanvasV2(manifest.getSequences()[0].getCanvases(), isShownBy));
        }
        return manifest;
    }

    /**
     * Generates a IIIF v3 manifest based on the provided (parsed) json document
     * @param jsonDoc parsed json document
     * @return IIIF Manifest v3 object
     */
    static ManifestV3 getManifestV3(Object jsonDoc) {
        String europeanaId = getEuropeanaId(jsonDoc);
        String isShownBy = getIsShownBy(europeanaId, jsonDoc);

        // EA-1973 + EA-2002 temporary(?) workaround for EUScreen; use isShownAt and use edmType instead of ebucoreMimetype
        ResourceType euScreenTypeHack = null;
        if (StringUtils.isEmpty(isShownBy)) {
            String edmType = (String) getFirstValueArray("edmType", europeanaId,
                    JsonPath.parse(jsonDoc).read("$.object.proxies[?(@.europeanaProxy == true)].edmType", String[].class));
            String isShownAt = (String) getFirstValueArray("edmIsShownAt", europeanaId,
                    JsonPath.parse(jsonDoc).read("$.object.aggregations[*].edmIsShownAt", String[].class));
            LOG.debug("edmType = {}, isShownAt = {}", edmType, isShownAt);
            if (isShownAt != null && ("VIDEO".equalsIgnoreCase(edmType) || "SOUND".equalsIgnoreCase(edmType))  &&
                    (isShownAt.startsWith("http://www.euscreen.eu/item.html") ||
                     isShownAt.startsWith("https://www.euscreen.eu/item.html")) ){
                LOG.debug("Using isShownAt because item is EUScreen video or audio");
                isShownBy = isShownAt;
                if ("SOUND".equalsIgnoreCase(edmType)) {
                    euScreenTypeHack = ResourceType.AUDIO;
                } else {
                    euScreenTypeHack = ResourceType.VIDEO;
                }
            }
        }

        ManifestV3 manifest = new ManifestV3(europeanaId, Definitions.getManifestId(europeanaId), isShownBy);
        manifest.setWithin(EdmManifestMapping.getWithinV3(jsonDoc));
        manifest.setLabel(EdmManifestMapping.getLabelsV3(jsonDoc));
        manifest.setSummary(EdmManifestMapping.getDescriptionV3(jsonDoc));
        manifest.setMetaData(EdmManifestMapping.getMetaDataV3(jsonDoc));
        manifest.setThumbnail(getThumbnailImageV3(europeanaId, jsonDoc));
        manifest.setNavDate(getNavDate(europeanaId, jsonDoc));
        manifest.setHomePage(getHomePage(europeanaId, jsonDoc));
        manifest.setRequiredStatement(getAttributionV3(europeanaId, isShownBy, jsonDoc));
        manifest.setRights(getRights(europeanaId, jsonDoc));
        manifest.setSeeAlso(getDataSetsV3(europeanaId));
        manifest.setItems(getItems(europeanaId, isShownBy, jsonDoc, euScreenTypeHack));
        manifest.setStart(getStartCanvasV3(manifest.getItems(), isShownBy));
        return manifest;
    }

    /**
     * Extract the Europeana object ID from the 'about' field.
     * @param jsonDoc parsed json document
     * @return string containing the Europeana ID of the object (dataset ID and record ID separated by a slash)
     */
    static String getEuropeanaId(Object jsonDoc) {
        return JsonPath.parse(jsonDoc).read("$.object.about", String.class);
    }

    static String getIsShownBy(String europeanaId, Object jsonDoc) {
        return (String) getFirstValueArray("edmIsShownBy", europeanaId,
                JsonPath.parse(jsonDoc).read("$.object.aggregations[*].edmIsShownBy", String[].class));
    }

    /**
     * Return first proxy.dctermsIsPartOf that starts with "http://data.theeuropeanlibrary.org/ that we can find
     * @param jsonDoc parsed json document
     * @return
     */
    static String getWithinV2(Object jsonDoc) {
        List<String> result = getEuropeanaLibraryCollections(jsonDoc);
        if (result.isEmpty()) {
            return null;
        }
        return result.get(0);
    }

    /**
     * Create a collection for all proxy.dctermsIsPartOf that start with "http://data.theeuropeanlibrary.org/
     * @param jsonDoc parsed json document
     * @return
     */
    static Collection[] getWithinV3(Object jsonDoc) {
        List<String> collections = getEuropeanaLibraryCollections(jsonDoc);
        if (collections.isEmpty()) {
            return null;
        }
        List<Collection> result = new ArrayList<>(collections.size());
        for (String collection : collections) {
            result.add(new Collection(collection));
        }
        return result.toArray(new Collection[0]);
    }

    private static List<String> getEuropeanaLibraryCollections(Object jsonDoc) {
        return JsonPath.parse(jsonDoc).
                read("$.object.proxies[*].dctermsIsPartOf.def[?(@ =~ /http(s)?:\\/\\/data.theeuropeanlibrary.org.*/i)]", List.class);
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
     * We first check all proxies for a title. If there are no titles, then we check the description fields
     * @param jsonDoc parsed json document
     * @return array of LanguageObject
     */
    static LanguageObject[] getLabelsV2(Object jsonDoc) {
        // we read everything in as LanguageMap[] because that best matches the EDM implementation, then we convert to LanguageObjects[]
        LanguageMap labelsV3 = getLabelsV3(jsonDoc);
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
    static LanguageMap getDescriptionV3(Object jsonDoc) {
        if (JsonPath.parse(jsonDoc).read("$.object.proxies[*].dcTitle", LanguageMap[].class).length > 0) {
            return LanguageMapUtils.mergeLanguageMaps(JsonPath.parse(jsonDoc).read("$.object.proxies[*].dcDescription", LanguageMap[].class));
        }
        return null;
    }

    /**
     * Returns the values from the proxy.dcDescription fields, but only if they aren't used as a label yet.
     * @param jsonDoc parsed json document
     * @return
     */
    static LanguageObject[] getDescriptionV2(Object jsonDoc) {
        // we read everything in as LanguageMap[] because that best matches the EDM implementation, then we convert to LanguageObjects[]
        LanguageMap descriptionsV3 = getDescriptionV3(jsonDoc);
        if (descriptionsV3 == null) {
            return null;
        }
        return LanguageMapUtils.langMapToObjects(getDescriptionV3(jsonDoc));
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
                    LOG.trace("  processing value {}", value);
                    if (isUrl(value)) {
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

    /**
     * Naive check if the provide string is an url
     * @param s the url to check
     * @return true if we consider it an url, otherwise false
     */
    private static boolean isUrl(String s) {
        return StringUtils.startsWithIgnoreCase(s, "http://")
                || StringUtils.startsWithIgnoreCase(s, "https://")
                || StringUtils.startsWithIgnoreCase(s, "ftp://")
                || StringUtils.startsWithIgnoreCase(s, "file://");
    }

    private static LanguageMap getTimespanAgentConceptOrPlaceLabels(Object jsonDoc, String value) {
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
        Filter aboutFilter = filter(where(ABOUT).is(value));
        LanguageMap[] labels = JsonPath.parse(jsonDoc).
                read("$.object[?(@." + entityName + ")]." + entityName + "[?].prefLabel", LanguageMap[].class, aboutFilter);
        if (labels.length > 0) {
            return labels[0];
        }
        return null;
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
                    List<LanguageObject> langObjects;
                    if (!metaData.containsKey(fieldName)) {
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
     * @param jsonDoc parsed json document
     * @return Image object, or null if no edmPreview was found
     */
    static eu.europeana.iiif.model.v2.Image getThumbnailImageV2(String europeanaId, Object jsonDoc) {
        String thumbnailId = getThumbnailId(europeanaId, jsonDoc);
        if (StringUtils.isEmpty(thumbnailId)) {
            return null;
        }
        return new eu.europeana.iiif.model.v2.Image(getThumbnailId(europeanaId, jsonDoc), null, null);
    }

    /**
     * Return an with the id of the thumbnail as defined in 'europeanaAggregation.edmPreview'
     * @param jsonDoc parsed json document
     * @return Image object, or null if no edmPreview was found
     */
    static eu.europeana.iiif.model.v3.Image[] getThumbnailImageV3(String europeanaId, Object jsonDoc) {
        String thumbnailId = getThumbnailId(europeanaId, jsonDoc);
        if (StringUtils.isEmpty(thumbnailId)) {
            return null;
        }
        return new eu.europeana.iiif.model.v3.Image[] {new eu.europeana.iiif.model.v3.Image(thumbnailId)};
    }

    private static String getThumbnailId(String europeanaId, Object jsonDoc) {
        String[] thumbnailIds = JsonPath.parse(jsonDoc).read("$.object.europeanaAggregation[?(@.edmPreview)].edmPreview", String[].class);
        return (String) getFirstValueArray("thumbnail ids", europeanaId, thumbnailIds);
    }

    /**
     * Return the first dctermsIssued date we can find in a proxy
     * Note that we assume that the desired value is in a mapping with a 'def' key
     * @param europeanaId consisting of dataset ID and record ID separated by a slash (string should have a leading slash and not trailing slash)
     * @param jsonDoc parsed json document
     * @return date string in xsd:datetime format (i.e. YYYY-MM-DDThh:mm:ssZ)
     */
   static String getNavDate(String europeanaId, Object jsonDoc) {
        LocalDate navDate = null;
        LanguageMap[] proxiesLangDates = JsonPath.parse(jsonDoc).read("$.object.proxies[*].dctermsIssued", LanguageMap[].class);
        for (LanguageMap langDates : proxiesLangDates) {
            for (String[] dates : langDates.values()) {
                // we assume there is only 1 value here
                String date = (String) getFirstValueArray("navDate", europeanaId, dates);
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
     * @param europeanaId consisting of dataset ID and record ID separated by a slash
     * @param jsonDoc parsed json document
     * @return {@link Text} containing reference to the landing page of the item on Europeana website
     */
    static Text[] getHomePage(String europeanaId, Object jsonDoc) {
        String[] landingPages = JsonPath.parse(jsonDoc).read("$.object.europeanaAggregation[?(@.edmLandingPage)].edmLandingPage", String[].class);
        String landingPage = (String) getFirstValueArray("landingPage", europeanaId, landingPages);
        if (landingPage == null) {
            return null;
        }
        return new Text[]{new Text(landingPage, new LanguageMap(LanguageMap.DEFAULT_METADATA_KEY, "Europeana"))};
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
        Filter isShownByFilter = filter(where(ABOUT).is(isShownBy));
        String[] attributions = JsonPath.parse(jsonDoc).
                read("$.object.aggregations[*].webResources[?]." + TEXT_ATTRIB_SNIPPET, String[].class, isShownByFilter);
        return (String) getFirstValueArray(TEXT_ATTRIB_SNIPPET, europeanaId, attributions);
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
        Filter isShownByFilter = filter(where(ABOUT).is(isShownBy));
        String[] attributions = JsonPath.parse(jsonDoc).
                read("$.object.aggregations[*].webResources[?]."+ HTML_ATTRIB_SNIPPET, String[].class, isShownByFilter);
        String attribution = (String) getFirstValueArray(HTML_ATTRIB_SNIPPET, europeanaId, attributions);
        if (StringUtils.isEmpty(attribution)) {
            return null;
        }
        return new LanguageMap(LanguageMap.DEFAULT_METADATA_KEY, attribution);
    }

    /**
     * Return the first license description we find in any 'aggregation.edmRights' field. Note that we first try the europeanaAggregation and if
     * that doesn't contain an edmRights, we check the other aggregations
     * @param europeanaId consisting of dataset ID and record ID separated by a slash (string should have a leading slash and not trailing slash)
     * @param jsonDoc parsed json document
     * @return String containing rights information
     */
    static String getLicense(String europeanaId, Object jsonDoc) {
        return getLicenseText(europeanaId, jsonDoc);
    }

    /**
     * Return the first license description we find in any 'aggregation.edmRights' field. Note that we first try the europeanaAggregation and if
     * that doesn't contain an edmRights, we check the other aggregations
     * @param europeanaId consisting of dataset ID and record ID separated by a slash (string should have a leading slash and not trailing slash)
     * @param jsonDoc parsed json document
     * @return Rights object containing rights information
     */
    static Rights getRights(String europeanaId, Object jsonDoc) {
        String licenseText = getLicenseText(europeanaId, jsonDoc);
        if (StringUtils.isEmpty(licenseText)) {
            return null;
        }
        return new Rights(licenseText);
    }

    private static String getLicenseText(String europeanaId, Object jsonDoc) {
        // first try europeanaAggregation.edmRights field (but for now this will almost never be set)
        LanguageMap[] licenseMaps = JsonPath.parse(jsonDoc).read("$.object.europeanaAggregation[?(@.edmRights)].edmRights", LanguageMap[].class);
        LanguageMap licenseMap = (LanguageMap) getFirstValueArray("licenseMap", europeanaId, licenseMaps);
        if (licenseMap == null || licenseMap.values().isEmpty()) {
            // as a back-up try the aggregation.edmRights
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
    static eu.europeana.iiif.model.v2.DataSet[] getDataSetsV2(String europeanaId) {
        eu.europeana.iiif.model.v2.DataSet[] result = new eu.europeana.iiif.model.v2.DataSet[3];
        result[0] = new eu.europeana.iiif.model.v2.DataSet(Definitions.getDatasetId(europeanaId, ".json-ld"), Definitions.MEDIA_TYPE_JSONLD);
        result[1] = new eu.europeana.iiif.model.v2.DataSet(Definitions.getDatasetId(europeanaId, ".json"), MediaType.APPLICATION_JSON_VALUE);
        result[2] = new eu.europeana.iiif.model.v2.DataSet(Definitions.getDatasetId(europeanaId, ".rdf"), Definitions.MEDIA_TYPE_RDF);
        return result;
    }

    /**
     * Generates 3 datasets with the appropriate ID and format (one for rdf/xml, one for json and one for json-ld)
     * @param europeanaId consisting of dataset ID and record ID separated by a slash (string should have a leading slash and not trailing slash)
     * @return array of 3 datasets
     */
    static eu.europeana.iiif.model.v3.DataSet[] getDataSetsV3(String europeanaId) {
        eu.europeana.iiif.model.v3.DataSet[] result = new eu.europeana.iiif.model.v3.DataSet[3];
        result[0] = new eu.europeana.iiif.model.v3.DataSet(Definitions.getDatasetId(europeanaId, ".json-ld"), Definitions.MEDIA_TYPE_JSONLD);
        result[1] = new eu.europeana.iiif.model.v3.DataSet(Definitions.getDatasetId(europeanaId, ".json"), MediaType.APPLICATION_JSON_VALUE);
        result[2] = new eu.europeana.iiif.model.v3.DataSet(Definitions.getDatasetId(europeanaId, ".rdf"), Definitions.MEDIA_TYPE_RDF);
        return result;
    }

    /**
     * @param europeanaId consisting of dataset ID and record ID separated by a slash (string should have a leading slash and not trailing slash)
     * @param isShownBy
     * @param jsonDoc parsed json document
     * @return
     */
    static eu.europeana.iiif.model.v2.Sequence[] getSequencesV2(String europeanaId, String isShownBy, Object jsonDoc) {
        // generate canvases in a same order as the web resources
        List<WebResource> sortedResources = getSortedWebResources(europeanaId, isShownBy, jsonDoc);
        if (sortedResources.isEmpty()) {
            return null;
        }

        int order = 1;
        Map<String, Object>[] services = JsonPath.parse(jsonDoc).read("$.object[?(@.services)].services[*]", Map[].class);
        List<eu.europeana.iiif.model.v2.Canvas> canvases = new ArrayList<>(sortedResources.size());
        for (WebResource webResource: sortedResources) {
            canvases.add(getCanvasV2(europeanaId, order, webResource, services));
            order++;
        }
        // there should be only 1 sequence, so sequence number is always 1
        eu.europeana.iiif.model.v2.Sequence[] result = new eu.europeana.iiif.model.v2.Sequence[1];
        result[0] = new eu.europeana.iiif.model.v2.Sequence();
        result[0].setStartCanvas(Definitions.getCanvasId(europeanaId, 1));
        result[0].setCanvases(canvases.toArray(new eu.europeana.iiif.model.v2.Canvas[0]));
        return result;
    }

    /**
     * Generates an ordered array of {@link Canvas}es referring to edmIsShownBy and hasView {@link WebResource}s.
     * For more information about the ordering @see {@link WebResourceSorter}
     * @param europeanaId
     * @param isShownBy
     * @param jsonDoc
     * @return array of Canvases
     */
    static eu.europeana.iiif.model.v3.Canvas[] getItems(String europeanaId, String isShownBy, Object jsonDoc, ResourceType euScreenTypeHack) {
        // generate canvases in a same order as the web resources
        List<WebResource> sortedResources = getSortedWebResources(europeanaId, isShownBy, jsonDoc);
        if (sortedResources.isEmpty()) {
            return null;
        }

        int order = 1;
        Map<String, Object>[] services = JsonPath.parse(jsonDoc).read("$.object[?(@.services)].services[*]", Map[].class);
        List<eu.europeana.iiif.model.v3.Canvas> canvases = new ArrayList<>(sortedResources.size());
        for (WebResource webResource: getSortedWebResources(europeanaId, isShownBy, jsonDoc)) {
            canvases.add(getCanvasV3(europeanaId, order, webResource, services, euScreenTypeHack));
            order++;
        }
        return canvases.toArray(new eu.europeana.iiif.model.v3.Canvas[0]);
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
            String annotationBodyId = c.getItems()[0].getItems()[0].getBody().getId();
            if (edmIsShownBy.equals(annotationBodyId)) {
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
            String annotationBodyId = c.getImages()[0].getResource().getId();
            if (edmIsShownBy.equals(annotationBodyId)) {
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
     * We should only generate a canvas for web resources that are either in the edmIsShownBy or in the hasViews
     * @return sorted list of web resources that are either edmIsShownBy or hasView
     */
    private static List<WebResource> getSortedWebResources(String europeanaId, String edmIsShownBy, Object jsonDoc) {
        String[][] hasViews = JsonPath.parse(jsonDoc).read("$.object.aggregations[*].hasView", String[][].class);

        List<String> validWebResources = new ArrayList<>();
        validWebResources.add(edmIsShownBy);
        LOG.trace("edmIsShownBy = {}", edmIsShownBy);
        for (String[] hasView : hasViews) {
            validWebResources.addAll(Arrays.asList(hasView));
            for (String hv : hasView) {
                LOG.trace("hasView = {}", hv);
            }
        }

        // get all web resources and check if they are edmIsShownBy or hasView
        WebResource[] webResources = JsonPath.parse(jsonDoc).read("$.object.aggregations[*].webResources[*]", WebResource[].class);
        List<WebResource> unsorted = new ArrayList<>();
        for (WebResource wr : webResources) {
            if (validWebResources.contains(wr.getId())) {
                unsorted.add(wr);
                LOG.trace("Valid webresource {} ", wr.getId());
            } else {
                LOG.debug("Skipping webresource {}", wr.getId());
            }
        }

        List<WebResource> sorted;
        try {
            sorted = WebResourceSorter.sort(unsorted);
        } catch (DataInconsistentException e) {
            LOG.error("Error trying to sort webresources for {}. Cause: {}", europeanaId, e);
            sorted = unsorted;
        }
        return sorted;
    }

    /**
     * Generates a new canvas, but note that we do not fill the otherContent (Full-Text) here. That is done later
     */
    private static eu.europeana.iiif.model.v2.Canvas getCanvasV2(String europeanaId,
                                                                 int order,
                                                                 WebResource webResource,
                                                                 Map<String, Object>[] services) {
        eu.europeana.iiif.model.v2.Canvas c =
                new eu.europeana.iiif.model.v2.Canvas(Definitions.getCanvasId(europeanaId, order), order);

        c.setLabel("p. "+order);

        Object obj = webResource.get(EBUCORE_HEIGHT);
        if (obj instanceof Integer){
            c.setHeight((Integer) obj);
        }

        obj = webResource.get(EBUCORE_WIDTH);
        if (obj instanceof Integer){
            c.setWidth((Integer) obj);
        }

        String attributionText = (String) webResource.get(TEXT_ATTRIB_SNIPPET);
        if (!StringUtils.isEmpty(attributionText)){
            c.setAttribution(attributionText);
        }

        LinkedHashMap<String, ArrayList<String>> license = (LinkedHashMap<String, ArrayList<String>>) webResource.get("webResourceEdmRights");
        if (license != null && !license.values().isEmpty()) {
            c.setLicense(license.values().iterator().next().get(0));
        }

        // canvas has 1 annotation (image field)
        c.setImages(new eu.europeana.iiif.model.v2.Annotation[1]);
        c.getImages()[0] = new eu.europeana.iiif.model.v2.Annotation();
        c.getImages()[0].setOn(c.getId());

        // annotation has 1 annotationBody
        eu.europeana.iiif.model.v2.AnnotationBody annoBody = new eu.europeana.iiif.model.v2.AnnotationBody((String) webResource.get(ABOUT));
        String ebuCoreMimeType = (String) webResource.get("ebucoreHasMimeType");
        if (!StringUtils.isEmpty(ebuCoreMimeType)) {
            annoBody.setFormat(ebuCoreMimeType);
        }

        // body can have a service
        String serviceId = getServiceId(webResource, europeanaId);
        if (serviceId != null) {
            eu.europeana.iiif.model.v2.Service service = new eu.europeana.iiif.model.v2.Service(serviceId);
            service.setProfile(lookupServiceDoapImplements(services, serviceId, europeanaId));
            annoBody.setService(service);
        }
        c.getImages()[0].setResource(annoBody);
        return c;
    }

    /**
     * Generates a new canvas, but note that we do not fill the otherContent (Full-Text) here. That's done later.
     */
    private static eu.europeana.iiif.model.v3.Canvas getCanvasV3(String europeanaId, int order, WebResource webResource,
                        Map<String, Object>[] services, ResourceType euScreenTypeHack) {
        eu.europeana.iiif.model.v3.Canvas c =
                new eu.europeana.iiif.model.v3.Canvas(Definitions.getCanvasId(europeanaId, order), order);

        c.setLabel(new LanguageMap(null, "p. "+order));

        Object obj = webResource.get(EBUCORE_HEIGHT);
        if (obj instanceof Integer){
            c.setHeight((Integer) obj);
        }

        obj = webResource.get(EBUCORE_WIDTH);
        if (obj instanceof Integer){
            c.setWidth((Integer) obj);
        }

        String durationText = (String) webResource.get("ebucoreDuration");
        if (durationText != null) {
            Long durationInMs = Long.valueOf(durationText);
            c.setDuration(durationInMs / 1000D);
        }

        String attributionText = (String) webResource.get(HTML_ATTRIB_SNIPPET);
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
        ResourceType resourceType = ResourceType.getResourceType(ebucoreMimeType);

        // EA-1973 + EA-2002 temporary(?) workaround for EUScreen; use isShownAt and use edmType instead of ebucoreMimetype
        if (euScreenTypeHack != null) {
            resourceType = euScreenTypeHack;
            ebucoreMimeType = null;
        }

        if (resourceType == ResourceType.AUDIO || resourceType == ResourceType.VIDEO) {
            anno.setTimeMode("trim");
        }
        anno.setTarget(c.getId());

        // annotation has 1 annotationBody
        eu.europeana.iiif.model.v3.AnnotationBody annoBody = new AnnotationBody(
                (String) webResource.get(ABOUT),  StringUtils.capitalize(resourceType.toString().toLowerCase(Locale.GERMANY)));
        anno.setBody(annoBody);

        if (!StringUtils.isEmpty(ebucoreMimeType)) {
            annoBody.setFormat(ebucoreMimeType);
        }

        // body can have a service
        String serviceId = getServiceId(webResource, europeanaId);
        if (serviceId != null) {
            eu.europeana.iiif.model.v3.Service service = new eu.europeana.iiif.model.v3.Service(serviceId);
            service.setProfile(lookupServiceDoapImplements(services, serviceId, europeanaId));
            annoBody.setService(service);
        }
        return c;
    }

    private static String getServiceId(WebResource wr, String europeanaId) {
        List<String> serviceIds = (List<String>) wr.get("svcsHasService");
        if (serviceIds != null && !serviceIds.isEmpty()) {
            String serviceId = (String) getFirstValueArray("service", europeanaId, serviceIds.toArray());
            LOG.trace("WebResource {} has serviceId {}", wr.getId(), serviceId);
            return serviceId;
        }
        LOG.debug("No serviceId for webresource {}", wr.getId());
        return null;
    }

    /**
     * Check if the array of services contains a service with the provided serviceId. If so we retrieve the doapImplements
     * field from that service;
     */
    private static String lookupServiceDoapImplements(Map<String, Object>[] services, String serviceId, String europeanaId) {
        String result = null;
        for (Map<String, Object> s : services) {
            String sId = (String) s.get(ABOUT);
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
