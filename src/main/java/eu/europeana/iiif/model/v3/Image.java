package eu.europeana.iiif.model.v3;

import java.io.Serializable;

/**
 * @author Patrick Ehlert
 * Created on 24-01-2018
 */
public class Image extends IdType implements Serializable {

    private static final long serialVersionUID = 1636104373070277504L;

    public Image(String id) {
        super(id, "sc:Image");
    }
}
