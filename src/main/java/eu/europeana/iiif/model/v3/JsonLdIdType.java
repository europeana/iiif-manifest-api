package eu.europeana.iiif.model.v3;

import java.io.Serializable;
import java.util.Optional;

/**
 * Id and Type are common fields for most IIIF v3 types.
 *
 * @author Patrick Ehlert
 * Created on 24-01-2018
 */
public class JsonLdIdType implements Serializable{

    private static final long serialVersionUID = -2716881573824312952L;

    private String id;
    private String type;

    public JsonLdIdType() {
        // empty constructor to make it also deserializable (see SonarQube squid:S2055)
    }

    public JsonLdIdType(String id, String type) {
        this.id = id;
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public Optional<String> getType() {
        return Optional.ofNullable(type);
    }
}
