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
    // EA-3436
    private Integer height;
    private Integer width;
    private Double duration;

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

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Double getDuration() {
        return duration;
    }

    public void setDuration(Double duration) {
        this.duration = duration;
    }
}
