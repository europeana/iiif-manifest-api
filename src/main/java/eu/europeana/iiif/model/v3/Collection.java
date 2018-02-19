package eu.europeana.iiif.model.v3;

import java.io.Serializable;

/**
 * @author Patrick Ehlert
 * Created on 24-01-2018
 */
public class Collection extends JsonLdIdType implements Serializable {

    private static final long serialVersionUID = 2809134550779212300L;

    public Collection (String id) {
        super(id, "Collection");
    }
}
