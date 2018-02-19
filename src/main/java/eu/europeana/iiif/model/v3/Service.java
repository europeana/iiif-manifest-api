package eu.europeana.iiif.model.v3;

import java.io.Serializable;

/**
 * @author Patrick Ehlert
 * Created on 24-01-2018
 */
public class Service extends JsonLdIdType implements Serializable {

    private static final long serialVersionUID = -4279624215412828621L;
    
    private String profile;

    public Service(String id) {
        super(id, "ImageService3");
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }
}
