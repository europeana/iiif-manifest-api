package eu.europeana.iiif.model.v2;

import eu.europeana.iiif.model.IdType;

import java.io.Serializable;

/**
 * @author Patrick Ehlert
 * Created on 06-02-2018
 */
public class Annotation extends IdType implements Serializable {

    private static final long serialVersionUID = -7091618924397220872L;

    private String motivation = "sc:painting";
    private AnnotationBody resource;
    private String on;

    public Annotation(String id) {
        super(id, "oa:Annotation");
    }

    public void setResource(AnnotationBody resource) {
        this.resource = resource;
    }

    public void setOn(String on) {
        this.on = on;
    }
}
