package eu.europeana.iiif.model.v3;

import ioinformarics.oss.jackson.module.jsonld.annotation.JsonldId;
import ioinformarics.oss.jackson.module.jsonld.annotation.JsonldProperty;
import ioinformarics.oss.jackson.module.jsonld.annotation.JsonldType;

import java.io.Serializable;

/**
 * Id and Type are common fields for most IIIF v3 types.
 *
 * @author Patrick Ehlert
 * Created on 24-01-2018
 */
public class IdType implements Serializable{

    private static final long serialVersionUID = -2716881573824312952L;

    @JsonldId
    private String id;
    private String type;

    public IdType() {
        // empty constructor to make it also deserializable (see SonarQube squid:S2055)
    }

    public IdType(String id, String type) {
        this.id = id;
        this.type = type;
    }

    public IdType(String type) {
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }
}
