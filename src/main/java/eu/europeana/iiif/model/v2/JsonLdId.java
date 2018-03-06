package eu.europeana.iiif.model.v2;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import ioinformarics.oss.jackson.module.jsonld.annotation.JsonldId;

import java.io.Serializable;

/**
 * Id for IIIF v2 types
 *
 * @author Patrick Ehlert
 * Created on 24-01-2018
 */
@JsonPropertyOrder({"id"}) // make sure id always comes first, instead of last
public class JsonLdId implements Serializable{
    
    private static final long serialVersionUID = -4566241702990679641L;

    @JsonldId
    private String id;

    public JsonLdId() {
        // empty constructor to make it also deserializable (see SonarQube squid:S2055)
    }

    public JsonLdId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

}
