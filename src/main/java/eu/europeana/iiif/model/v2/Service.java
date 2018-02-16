package eu.europeana.iiif.model.v2;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
 * @author Patrick Ehlert
 * Created on 06-02-2018
 */
public class Service extends JsonLdId implements Serializable {

    private static final long serialVersionUID = -7367911509714923855L;

    @JsonProperty("@context")
    private String context = "http://iiif.io/api/image/2/context.json";
    private String profile;

    public Service(String id) {
        super(id);
    }

    public String getContext() {
        return context;
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }
}
