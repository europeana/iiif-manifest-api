package eu.europeana.iiif.model.v3;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.Optional;

/**
 * @author Patrick Ehlert
 * Created on 24-01-2018
 */
@JsonPropertyOrder({"context", "id", "profile"})
public class Service extends JsonLdIdType {

    private static final long serialVersionUID = -4279624215412828621L;
    
    private String profile;

    @JsonProperty("@context")
    private String context;

    public Service(String id, String type) {
        super(id, type);
    }

    public Service(String id, String type, String context, String profile){
        super(id, type);
        this.context = context;
        this.profile = profile;
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    /**
     * Gets the context for this service.
     * This property is nullable as it is not required for V3 services.
     */
    public Optional<String> getContext() {
        return Optional.ofNullable(context);
    }
}
