package eu.europeana.iiif.model;

import eu.europeana.iiif.IIIFDefinitions;

/**
 * Definitions specifically for IIIF Manifest. For definitions shared between IIIF Manifest and Fulltext API
 *
 * @author Patrick Ehlert Created on 26-01-2018
 * @see IIIFDefinitions class
 */
public final class ManifestDefinitions {

    /**
     * Place holder for the dataset and record part of an ID. This is used in various places in the manifest
     */
    public static final String ID_PLACEHOLDER = "{DATASET_ID}/{RECORD_ID}";

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
    public static final String INFO_CANVAS_TYPE   = "FulltextSummaryCanvas";
    public static final String INFO_ANNOPAGE_TYPE = "AnnotationPage";

    public static final String CANVAS_THUMBNAIL_POSTFIX = "&type=TEXT";

    public static final String ATTRIBUTION_STRING = "Attribution";

    private ManifestDefinitions() {
        // empty constructor to avoid initializationRE
    }

    public static String getFulltextSummaryPath(String europeanaId) {
        return IIIFDefinitions.PRESENTATION_PATH + europeanaId + IIIFDefinitions.FULLTEXT_SUMMARY_PATH +
               "/"; // for now trailing slash is needed
    }

}
