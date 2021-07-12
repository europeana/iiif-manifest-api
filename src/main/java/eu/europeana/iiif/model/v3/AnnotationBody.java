package eu.europeana.iiif.model.v3;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Patrick Ehlert
 * Created on 24-01-2018
 */
public class AnnotationBody extends JsonLdIdType {

    private static final long serialVersionUID = 7359225934920121361L;

    private String format;
    private Service service;

    @JsonProperty("language")
    private String originalLanguage;

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

    public String getOriginalLanguage() {
        return originalLanguage;
    }

    public void setOriginalLanguage(String originalLanguage) {
        this.originalLanguage = originalLanguage;
    }
}
