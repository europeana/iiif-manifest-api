package eu.europeana.iiif.model.v3;

import java.io.Serializable;

/**
 * @author Patrick Ehlert
 * Created on 24-01-2018
 */
public class Rights extends IdType implements Serializable {

    private static final long serialVersionUID = -3102243950444778135L;

    public String format;

    public Rights(String id) {
        super("Text");
        this.id = id;
    }

}
