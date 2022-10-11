package eu.europeana.iiif.model;

import eu.europeana.iiif.IIIFDefinitions;

/**
 * Definitions specifically for IIIF Manifest. For definitions shared between IIIF Manifest and Fulltext API
 * @see IIIFDefinitions class
 * @author Patrick Ehlert
 * Created on 26-01-2018
 */
public final class ManifestDefinitions {

    /**
     * Place holder for the dataset and record part of an ID. This is used in various places in the manifest
     */
    public static final String ID_PLACEHOLDER = "{DATASET_ID}/{RECORD_ID}";

    /**
     * Base URL used for generation the various types of IDs
     */
    public static final String IIIF_PRESENTATION_BASE_URL =
            IIIFDefinitions.IIIF_EUROPENA_BASE_URL + IIIFDefinitions.PRESENTATION_PATH + ID_PLACEHOLDER;

    /**
     * Url of all manifest IDs but with placeholder for the actual dataset and record ID
     */
    public static final String MANIFEST_ID = IIIF_PRESENTATION_BASE_URL + "/manifest";

    /**
     * Url of a sequence IDs but with placeholder for the actual dataset and record ID.
     * Note that there the order number is not included here (so first sequence should be /sequence/s1)
     */
    public static final String SEQUENCE_ID = IIIF_PRESENTATION_BASE_URL + "/sequence/s";

    /**
     * Url for canvas IDs but with placeholder for the actual dataset and record ID
     * Note that there the order number is not included here (so first canvas should be /canvas/p1)
     */
    public static final String CANVAS_ID = IIIF_PRESENTATION_BASE_URL + "/canvas/p";

    /**
     * Base url of a dataset ID (seeAlso part of manifest)
     */
    public static final String DATASET_ID_BASE_URL = "https://www.europeana.eu/api/v2/record";

    /**
     * Media type for rdf
     */
    public static final String MEDIA_TYPE_RDF = "application/rdf+xml";

    /**
     * Location of the europeana logo (for v2 manifest)
     */
    public static final String EUROPEANA_LOGO_URL = "https://style.europeana.eu/images/europeana-logo-default.png";

    /**
     * Location of the EDM schema definition;
     */
    public static final String EDM_SCHEMA_URL = "http://www.europeana.eu/schemas/edm/";


    /**
     * Context value for search service description
     */
    public static final String SEARCH_CONTEXT_VALUE = "http://iiif.io/api/search/1/context.json";

    /**
     * Profile value for search service description
     */
    public static final String SEARCH_PROFILE_VALUE = "http://iiif.io/api/search/1/search";

    /**
     * Context value for image service description
     */
    public static final String IMAGE_CONTEXT_VALUE = "http://iiif.io/api/image/2/context.json";

    public static final String IMAGE_SERVICE_TYPE_3 = "ImageService3";

    /**
     * Titles of Fulltext summary types
     */
    public static final  String INFO_CANVAS_TYPE          = "FulltextSummaryCanvas";
    public static final  String INFO_ANNOPAGE_TYPE        = "AnnotationPage";

    private ManifestDefinitions() {
        // empty constructor to avoid initializationRE
    }

    public static String getFulltextSummaryPath(String europeanaId) {
        return IIIFDefinitions.PRESENTATION_PATH + europeanaId + IIIFDefinitions.FULLTEXT_SUMMARY_PATH + "/"; // for now trailing slash is needed
    }

    public static String getFulltextSearchPath(String europeanaId) {
        return IIIFDefinitions.PRESENTATION_PATH + europeanaId + IIIFDefinitions.FULLTEXT_SEARCH_PATH;
    }

    /**
     * Create the IIIF manifest ID
     *
     * @param europeanaId consisting of dataset ID and record ID separated by a slash (string should have a leading slash and not trailing slash)
     * @return string containing the IIIF manifest ID
     */
    public static String getManifestId(String europeanaId) {
        return ManifestDefinitions.MANIFEST_ID.replace(ManifestDefinitions.ID_PLACEHOLDER, europeanaId);
    }

    /**
     * Create a canvas ID
     *
     * @param europeanaId consisting of dataset ID and record ID separated by a slash (string should have a leading slash and not trailing slash)
     * @param order       number
     * @return String containing the canvas ID
     */
    public static String getCanvasId(String europeanaId, int order) {
        return ManifestDefinitions.CANVAS_ID.replace(ManifestDefinitions.ID_PLACEHOLDER, europeanaId).concat(Integer.toString(order));
    }

    /**
     * Create a dataset ID (datasets information are part of the manifest)
     *
     * @param europeanaId consisting of dataset ID and record ID separated by a slash (string should have a leading slash and not trailing slash)
     * @return string containing the dataset ID consisting of a base url, Europeana ID and postfix (rdf/xml, json or json-ld)
     */
    public static String getDatasetId(String europeanaId, String postFix) {
        return ManifestDefinitions.DATASET_ID_BASE_URL + europeanaId + postFix;
    }

}
