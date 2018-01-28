package eu.europeana.iiif.model.v3;

import ioinformarics.oss.jackson.module.jsonld.annotation.JsonldId;

import java.io.Serializable;

/**
 * @author Patrick Ehlert
 * Created on 24-01-2018
 */
public class IdType implements Serializable{
    
    private static final long serialVersionUID = -4566241702990679641L;

    //@JsonldId
    String id;
    String type;

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
