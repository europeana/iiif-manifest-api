package eu.europeana.iiif.model.v2;

import eu.europeana.iiif.model.IdType;

import java.io.Serializable;

/**
 * @author Patrick Ehlert
 * Created on 24-01-2018
 */
public class AnnotationBody extends IdType implements Serializable {

    private static final long serialVersionUID = 2703342049366188602L;

    private String format;
    private Service service;

    public AnnotationBody(String id) {
        super(id, "dcTypes:Image");
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public void setService(Service service) {
        this.service = service;
    }
}
