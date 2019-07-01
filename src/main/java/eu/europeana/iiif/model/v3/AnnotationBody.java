package eu.europeana.iiif.model.v3;

/**
 * @author Patrick Ehlert
 * Created on 24-01-2018
 */
public class AnnotationBody extends JsonLdIdType {

    private static final long serialVersionUID = 7359225934920121361L;

    private String format;
    private Service service;

    public AnnotationBody(String id, String type) {
        super(id, type);
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
