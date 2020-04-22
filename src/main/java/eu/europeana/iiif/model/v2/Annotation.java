package eu.europeana.iiif.model.v2;

import ioinformarics.oss.jackson.module.jsonld.annotation.JsonldType;

import java.io.Serializable;

/**
 * @author Patrick Ehlert
 * Created on 06-02-2018
 */
@JsonldType(value = "oa:Annotation")
public class Annotation implements Serializable {

    private static final long serialVersionUID = -7091618924397220872L;

    private String motivation = "sc:painting";
    private AnnotationBody resource;
    private String on;

    public String getMotivation() {
        return motivation;
    }

    public AnnotationBody getResource() {
        return resource;
    }

    public void setResource(AnnotationBody resource) {
        this.resource = resource;
    }

    public String getOn() {
        return on;
    }

    public void setOn(String on) {
        this.on = on;
    }
}
