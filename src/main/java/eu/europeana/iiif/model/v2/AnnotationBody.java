package eu.europeana.iiif.model.v2;

import ioinformarics.oss.jackson.module.jsonld.annotation.JsonldType;

/**
 * @author Patrick Ehlert
 * Created on 24-01-2018
 */
@JsonldType("dctypes:Image")
public class AnnotationBody extends JsonLdId {

    private static final long serialVersionUID = 2703342049366188602L;

    private String format;
    private Service service;

    public AnnotationBody(String id) {
        super(id);
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public Service getService() {
        return service;
    }

    public void setService(Service service) {
        this.service = service;
    }
}
