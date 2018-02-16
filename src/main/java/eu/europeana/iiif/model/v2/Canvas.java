package eu.europeana.iiif.model.v2;

import ioinformarics.oss.jackson.module.jsonld.annotation.JsonldType;

import java.io.Serializable;

/**
 * @author Patrick Ehlert
 * Created on 06-02-2018
 */
@JsonldType("sc:Canvas")
public class Canvas extends JsonLdId implements Serializable {

    private static final long serialVersionUID = 6160907015595073905L;

    // TODO load height and width from configuration file

    private String label;
    private Integer height = 1024;
    private Integer width = 686;
    private String attribution;
    private String license;
    private Annotation[] images;
    private FullText[] otherContent;

    public Canvas(String id) {
        super(id);
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Integer getHeight() {
        return height;
    }

    public Integer getWidth() {
        return width;
    }

    public String getAttribution() {
        return attribution;
    }

    public void setAttribution(String attribution) {
        this.attribution = attribution;
    }

    public String getLicense() {
        return license;
    }

    public void setLicense(String license) {
        this.license = license;
    }

    public Annotation[] getImages() {
        return images;
    }

    public void setImages(Annotation[] images) {
        this.images = images;
    }

    public FullText[] getOtherContent() {
        return otherContent;
    }

    public void setOtherContent(FullText[] otherContent) {
        this.otherContent = otherContent;
    }
}
