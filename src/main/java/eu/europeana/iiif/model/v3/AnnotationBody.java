package eu.europeana.iiif.model.v3;

import java.io.Serializable;

/**
 * @author Patrick Ehlert
 * Created on 24-01-2018
 */
public class AnnotationBody extends IdType implements Serializable {

    private static final long serialVersionUID = 7359225934920121361L;

    public String format;
    public Service service;

    public AnnotationBody(String id) {
        super("Image");
        this.id = id;
    }
}
