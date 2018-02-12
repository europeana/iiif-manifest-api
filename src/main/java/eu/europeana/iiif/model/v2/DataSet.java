package eu.europeana.iiif.model.v2;

import eu.europeana.iiif.model.IdType;

import java.io.Serializable;

/**
 * Source data of the resource, see http://prezi3.iiif.io/api/presentation/3.0/#technical-properties.
 * Not to be confused with a Dataset in Europeana.
 *
 * @author Patrick Ehlert
 * Created on 06-02-2018
 */
public class DataSet extends IdType implements Serializable {

    private static final long serialVersionUID = 8476756746789079580L;

    public String format;
    public String profile = "http://www.europeana.eu/schemas/edm/";

    public DataSet(String id, String format) {
        super(id, "sc:Dataset");
        this.format = format;
    }

}
