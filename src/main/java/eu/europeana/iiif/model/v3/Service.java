package eu.europeana.iiif.model.v3;

import java.io.Serializable;

/**
 * @author Patrick Ehlert
 * Created on 24-01-2018
 */
public class Service extends IdType implements Serializable {

    private static final long serialVersionUID = -4279624215412828621L;
    
    public String profile;

    public Service(String id) {
        super("ImageService3");
        this.id = id;
    }
}
