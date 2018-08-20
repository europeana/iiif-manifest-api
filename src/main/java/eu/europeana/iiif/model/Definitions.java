package eu.europeana.iiif.model;

/**
 * @author Patrick Ehlert
 * Created on 26-01-2018
 */
public final class Definitions {

    /**
     * Place holder for the dataset and record part of an ID. This is used in various places in the manifest
     */
    public static final String ID_PLACEHOLDER = "{DATASET_ID}/{RECORD_ID}";

    /**
     * Base URL used for generation the various types of IDs
     */
    public static final String IIIF_PRESENTATION_BASE_URL = "https://iiif.europeana.eu/presentation" + ID_PLACEHOLDER;

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
     * Url for annotation IDs but with placeholder for the actual dataset and record ID
     * Note that there the order number is not included here (so first canvas should be /annotation/p1)
     */
    public static final String ANNOTATION_ID = IIIF_PRESENTATION_BASE_URL + "/annotation/p";

    /**
     * Base url of a dataset ID (seeAlso part of manifest)
     */
    public static final String DATASET_ID_BASE_URL = "https://www.europeana.eu/api/v2/record";

    /**
     * Media type for json-ld
     */
    public static final String MEDIA_TYPE_JSONLD = "application/ld+json";

    /**
     * Media type for IIIF version 2
     */
    public static final String MEDIA_TYPE_IIIF_V2 = "http://iiif.io/api/presentation/2/context.json";

    /**
     * Media type for IIIF version 3
     */
    public static final String MEDIA_TYPE_IIIF_V3 = "http://iiif.io/api/presentation/3/context.json";

    /**
     * Default Content-type returned on manifest requests for version 3
     */
    public static final String MEDIA_TYPE_IIIF_JSONLD_V3 = MEDIA_TYPE_JSONLD + ";profile=\""+MEDIA_TYPE_IIIF_V3+"\"";

    /**
     * Default Content-type returned on manifest requests for version 2
     */
    public static final String MEDIA_TYPE_IIIF_JSONLD_V2 = MEDIA_TYPE_JSONLD + ";profile=\""+MEDIA_TYPE_IIIF_V2+"\"";

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

    private Definitions() {
        // empty constructor to avoid initializationRE
    }

}
