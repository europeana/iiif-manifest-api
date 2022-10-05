package eu.europeana.iiif.model.v2;

import eu.europeana.iiif.model.ManifestDefinitions;

/**
 * Source data of the resource, see http://prezi3.iiif.io/api/presentation/3.0/#technical-properties.
 * Not to be confused with a Dataset in Europeana.
 *
 * @author Patrick Ehlert
 * Created on 06-02-2018
 */
public class DataSet extends JsonLdId {

    private static final long serialVersionUID = 8476756746789079580L;

    private String format;
    private String profile = ManifestDefinitions.EDM_SCHEMA_URL;

    public DataSet(String id, String format) {
        super(id);
        this.format = format;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getProfile() {
        return profile;
    }
}
