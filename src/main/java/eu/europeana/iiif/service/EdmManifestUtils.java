package eu.europeana.iiif.service;

import com.jayway.jsonpath.JsonPath;
import eu.europeana.iiif.model.MediaType;
import eu.europeana.iiif.model.WebResource;
import eu.europeana.iiif.model.WebResourceSorter;
import eu.europeana.iiif.model.v3.LanguageMap;
import eu.europeana.iiif.model.v3.Text;
import eu.europeana.iiif.exception.DataInconsistentException;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.commons.lang3.StringUtils;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This class contains all the common methods for mapping EDM record data to IIIF Manifest data for IIIF v2 and v3
 *
 * @author Srishti Singh
 * Created on 25-03-2020
 */
public final class EdmManifestUtils {

    private static final Logger LOG = LogManager.getLogger(EdmManifestUtils.class);

    public static final String ABOUT = "about";
    public static final String TEXT_ATTRIB_SNIPPET = "textAttributionSnippet";
    public static final String HTML_ATTRIB_SNIPPET = "htmlAttributionSnippet";
    public static final String EBUCORE_HEIGHT = "ebucoreHeight";
    public static final String EBUCORE_WIDTH = "ebucoreWidth";
    public static final String SVCS_HAS_SERVICE = "svcsHasService";
    public static final String WEB_RESOURCE_EDM_RIGHTS = "webResourceEdmRights";
    public static final String EBUCORE_DURATION = "ebucoreDuration";
    public static final String EBUCORE_HAS_MIMETYPE = "ebucoreHasMimeType";
    public static final String LINGUISTIC = "zxx";
    public static final String SERVICE = "Service";

    public static final String IMAGE = "Image";

    private EdmManifestUtils() {
        // private constructor to prevent initialization
    }

    public static List<String> getEuropeanaLibraryCollections(Object jsonDoc) {
        return JsonPath.parse(jsonDoc).
                read("$.object.proxies[*].dctermsIsPartOf.def[?(@ =~ /http(s)?:\\/\\/data.theeuropeanlibrary.org.*/i)]", List.class);
    }

    /**
     * Extract the Europeana object ID from the 'about' field.
     * @param jsonDoc parsed json document
     * @return string containing the Europeana ID of the object (dataset ID and record ID separated by a slash)
     */
    static String getEuropeanaId(Object jsonDoc) {
        return JsonPath.parse(jsonDoc).read("$.object.about", String.class);
    }

    /**
     * Naive check if the provide string is an url
     * @param s the url to check
     * @return true if we consider it an url, otherwise false
     */
    public static boolean isUrl(String s) {
        return StringUtils.startsWithIgnoreCase(s, "http://")
                || StringUtils.startsWithIgnoreCase(s, "https://")
                || StringUtils.startsWithIgnoreCase(s, "ftp://")
                || StringUtils.startsWithIgnoreCase(s, "file://");
    }

    public static String getThumbnailId(String europeanaId, Object jsonDoc) {
        String[] thumbnailIds = JsonPath.parse(jsonDoc).read("$.object.europeanaAggregation[?(@.edmPreview)].edmPreview", String[].class);
        return (String) EdmManifestUtils.getFirstValueArray("thumbnail ids", europeanaId, thumbnailIds);
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
                String date = (String) EdmManifestUtils.getFirstValueArray("navDate", europeanaId, dates);
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
    public static Text[] getHomePage(String europeanaId, Object jsonDoc) {
        String[] landingPages = JsonPath.parse(jsonDoc).read("$.object.europeanaAggregation[?(@.edmLandingPage)].edmLandingPage", String[].class);
        String landingPage = (String) EdmManifestUtils.getFirstValueArray("landingPage", europeanaId, landingPages);
        if (landingPage == null) {
            return null;
        }
        return new Text[]{new Text(landingPage, new LanguageMap(LanguageMap.DEFAULT_METADATA_KEY, "Europeana"))};
    }


    public static String getLicenseText(String europeanaId, Object jsonDoc) {
        // first try europeanaAggregation.edmRights field (but for now this will almost never be set)
        LanguageMap[] licenseMaps = JsonPath.parse(jsonDoc).read("$.object.europeanaAggregation[?(@.edmRights)].edmRights", LanguageMap[].class);
        LanguageMap licenseMap = (LanguageMap) EdmManifestUtils.getFirstValueArray("licenseMap", europeanaId, licenseMaps);
        if (licenseMap == null || licenseMap.values().isEmpty()) {
            // as a back-up try the data provider aggregation.edmRights
            String proxyIn = EdmManifestUtils.getDataProviderFromProxyWithOutLineage(jsonDoc, europeanaId);
            LanguageMap[] licenses = JsonPath.parse(jsonDoc).read("$.object.aggregations[?(@.about == '" + proxyIn + "')].edmRights", LanguageMap[].class);
            licenseMap = (LanguageMap) EdmManifestUtils.getFirstValueArray("license", europeanaId, licenses);
        }

        if (licenseMap != null && !licenseMap.values().isEmpty()) {
            return (String) EdmManifestUtils.getFirstValueArray("license text", europeanaId, licenseMap.values().iterator().next());
        }
        return null;
    }

    /**
     * We should only generate a canvas for web resources that are either in the edmIsShownBy or in the hasViews
     * @return sorted list of web resources that are either edmIsShownBy or hasView
     */
    public static List<WebResource> getSortedWebResources(String europeanaId, String edmIsShownBy, Object jsonDoc) {
        String[][] hasViews = JsonPath.parse(jsonDoc).read("$.object.aggregations[*].hasView", String[][].class);

        List<String> validWebResources = new ArrayList<>();
        validWebResources.add(edmIsShownBy);
        LOG.trace("edmIsShownBy = {}", edmIsShownBy);
        for (String[] hasView : hasViews) {
            for (String view: hasView) {
                // check for duplicates
                if (!validWebResources.contains(view)) {
                    validWebResources.add(view);
                }
            }
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
            sorted = WebResourceSorter.sort(unsorted, validWebResources);
        } catch (DataInconsistentException e) {
            LOG.error("Error trying to sort webresources for {}. Cause: {}", europeanaId, e);
            sorted = unsorted;
        }
        return sorted;
    }

    /**
     * Check if the array of services contains a service with the provided serviceId. If so we retrieve the doapImplements
     * field from that service;
     */
    public static String lookupServiceDoapImplements(Map<String, Object>[] services, String serviceId, String europeanaId) {
        String result = null;
        for (Map<String, Object> s : services) {
            String sId = (String) s.get(ABOUT);
            if (sId != null && sId.equalsIgnoreCase(serviceId)) {
                // Note: there is a problem with cardinality of the doapImplements field. It should be a String, but at the moment
                // it is defined in EDM as a String[]. Here we get an Instance of List.
                Object doapImplements = s.get("doapImplements");
                    if (doapImplements != null && doapImplements instanceof List) {
                        // check for empty list as there are many cases where we do get an empty list. See : EA-3227
                        if (((List<String>) doapImplements).isEmpty()) {
                            LOG.warn("Record {} has service {} with empty doapImplements field value", europeanaId, serviceId);
                        } else {
                            result = ((List<String>) doapImplements).get(0);
                        }
                    }
                    break;
            }
        }
        if (result == null) {
            LOG.warn("Record {} defined service {} in webresource, but no such service is defined (with a doapImplements field)", europeanaId, serviceId);
        }
        return result;
    }

    public static String getServiceId(WebResource wr, String europeanaId) {
        List<String> serviceIds = (List<String>) wr.get(SVCS_HAS_SERVICE);
        if (serviceIds != null && !serviceIds.isEmpty()) {
            String serviceId = (String) EdmManifestUtils.getFirstValueArray("service", europeanaId, serviceIds.toArray());
            LOG.trace("WebResource {} has serviceId {}", wr.getId(), serviceId);
            return serviceId;
        }
        LOG.debug("No serviceId for webresource {}", wr.getId());
        return null;
    }

    /**
     * Generates a url for a fulltext search
     *
     * @param europeanaId identifier to include in the path
     */
//    public static String getFullTextSearchUrl(String fulltextBaseUrl, String europeanaId) {
//        return fulltextBaseUrl + getFulltextSearchPath(europeanaId);
//    }

    /**
     * Returns the data provider Aggregation
     * There must be only one data provider aggregation
     * @param jsonDoc
     */
    public static String getValueFromDataProviderAggregation(Object jsonDoc, String europeanaId, String fieldName) {
        String proxyIn = getDataProviderFromProxyWithOutLineage(jsonDoc, europeanaId);
        if (!StringUtils.isEmpty(proxyIn)) {
            String[] dataProviderAggregation = JsonPath.parse(jsonDoc)
                    .read("$.object.aggregations[?(@.about == '" + proxyIn + "')]."+fieldName, String[].class);
            return (String) getFirstValueArray(fieldName, europeanaId, dataProviderAggregation);
        }
        return null;
    }

    /**
     * get the proxyIn value from the main proxy
     *
     * @param jsonDoc
     * @param europeanaId
     * @return
     */
    public static String getDataProviderFromProxyWithOutLineage(Object jsonDoc, String europeanaId) {
        String[] proxyIn = JsonPath.parse(jsonDoc).read("$.object.proxies[?(!@.lineage && @.europeanaProxy != true )].proxyIn[0]", String[].class);
        if (proxyIn.length >= 1) {
            if (proxyIn.length > 1) {
                LOG.warn("Multiple proxyIn values found in proxy w/o lineage for record {}, returning first", europeanaId);
            }
            return proxyIn[0];
        }
        return "";
    }

    /**
     * In many cases we assume there will be only 1 proxy or aggregation with the provided value, so this method helps
     * to retrieve the first value object while providing a warning if there are more values than expected.
     * @param fieldName optional, if not null we log a warning if there is more than 1 expected value
     * @param europeanaId
     * @param values
     * @return first value object from the array of values
     */
    static Object getFirstValueArray(String fieldName, String europeanaId, Object[] values) {
        if (values.length >= 1) {
            if (!StringUtils.isEmpty(fieldName) && values.length > 1) {
                // This happens actually quite often in production, so we lowered log severity from WARN to DEBUG
                LOG.debug("Multiple {} values found for record {}, returning first", fieldName, europeanaId);
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

    /**EA-3745  Generate ID for annotation based on associated webResource.
     * For specialized formats i.e. the ones which are rendered the thumbnail URL is used as id.
     * @param annotationID
     * @param mediaType
     * @param thumbnailURL
     * @return String
     */
    public static String getIdForAnnotation(String annotationID, MediaType mediaType, String thumbnailURL) {
        return thumbnailURL + annotationID +
            Optional.ofNullable(mediaType.getType()).map(String::toUpperCase)
                .map(type -> "&type=" + type).orElse("");

    }
}
