package eu.europeana.iiif.model;

/**
 * @author Patrick Ehlert
 * Created on 26-01-2018
 */
public class Definitions {

    /**
     * Base url of all manifest IDs
     */
    public static final String IIIF_MANIFESTID_BASE_UIRL = "https://iiif.europeana.eu/presentation";

    /**
     * Postfix part of all manifest IDs
     */
    public static final String IIIF_MANIFESTID_POSTFIX = "/manifest";

    /**
     * Default Content-type returned on manifest requests
     */
    public static final String MEDIA_TYPE_JSONLD = "application/ld+json;profile=\"https://iiif.io/api/presentation/3/context.json\";charset=utf-8";

    private Definitions() {
        // empty constructor to avoid initialization
    }
}
