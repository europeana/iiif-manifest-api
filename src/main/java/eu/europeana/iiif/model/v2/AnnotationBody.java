package eu.europeana.iiif.model.v2;

import com.fasterxml.jackson.annotation.JsonProperty;
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

    // EA-3436
    private Integer height;
    private Integer width;

    @JsonProperty("language")
    private String originalLanguage;

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
}
