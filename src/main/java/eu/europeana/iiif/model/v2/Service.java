package eu.europeana.iiif.model.v2;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * @author Patrick Ehlert
 * Created on 06-02-2018
 */
@JsonPropertyOrder({"context", "id", "profile"})
public class Service extends JsonLdId {

    private static final long serialVersionUID = -7367911509714923855L;

    @JsonProperty("@context")
    private String context;
    private String profile;

    public Service(String id, String context) {
        super(id);
        this.context = context;
    }

    public Service(String id, String context, String profile) {
        this(id, context);
        this.profile = profile;
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
