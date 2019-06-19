package eu.europeana.iiif.model.v3;

import java.io.Serializable;

/**
 * @author Patrick Ehlert
 * Created on 24-01-2018
 */
public class Image extends JsonLdIdType implements Serializable {

    private static final long serialVersionUID = 6964375170132552570L;

    public Image(String id) {
        super(id, "Image");
    }

}
