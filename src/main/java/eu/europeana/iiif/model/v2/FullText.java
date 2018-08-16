package eu.europeana.iiif.model.v2;

import java.io.Serializable;

/**
 * @author Patrick Ehlert
 * Created on 06-02-2018
 */
public class FullText extends JsonLdId implements Serializable {

    private static final long serialVersionUID = 1939876036877859496L;

    public FullText(String id) {
        super(id);
    }
}
