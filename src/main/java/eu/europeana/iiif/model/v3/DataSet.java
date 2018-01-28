package eu.europeana.iiif.model.v3;

import java.io.Serializable;

/**
 * Source data of the resource, see http://prezi3.iiif.io/api/presentation/3.0/#technical-properties.
 * Not to be confused with a Dataset in Europeana.
 *
 * @author Patrick Ehlert
 * Created on 24-01-2018
 */
public class DataSet extends IdType implements Serializable {

    private static final long serialVersionUID = 3664766425503896401L;

    public String format;
    public String profile;

    public DataSet(String id) {
        super("Dataset");
        this.id = id;
    }

}
