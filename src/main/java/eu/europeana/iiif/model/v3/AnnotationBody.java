package eu.europeana.iiif.model.v3;

import java.io.Serializable;

/**
 * @author Patrick Ehlert
 * Created on 24-01-2018
 */
public class AnnotationBody extends IdType implements Serializable {

    private static final long serialVersionUID = 7359225934920121361L;

    private String format;
    private Service service;

    public AnnotationBody(String id) {
        super(id, "dctypes:Image");
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public void setService(Service service) {
        this.service = service;
    }
}
