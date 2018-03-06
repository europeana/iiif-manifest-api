package eu.europeana.iiif.model.v3;

import java.io.Serializable;

/**
 * @author Patrick Ehlert
 * Created on 24-01-2018
 */
public class Rights extends JsonLdIdType implements Serializable {

    private static final long serialVersionUID = -3102243950444778135L;

    private String format = "text/html";

    public Rights(String id) {
        super(id, "Text");
    }

    public String getFormat() {
        return format;
    }

}
