package eu.europeana.iiif.model.v2;

import com.fasterxml.jackson.annotation.JsonIgnore;
import eu.europeana.iiif.service.ManifestSettings;
import ioinformarics.oss.jackson.module.jsonld.annotation.JsonldType;

import java.io.Serializable;

/**
 * @author Patrick Ehlert
 * Created on 06-02-2018
 */
@JsonldType("sc:Canvas")
public class Canvas extends JsonLdId implements Serializable {

    private static final long serialVersionUID = 6160907015595073905L;

    @JsonIgnore
    private int pageNr; // for internal use

    private String label;
    private Integer height;
    private Integer width;
    private String attribution;
    private String license;
    private Annotation[] images;
    private FullText[] otherContent; // only 1 value is expected (or null)

    public Canvas(ManifestSettings settings, String id, int pageNr) {
        super(id);
        this.pageNr = pageNr;
        this.height = settings.getCanvasHeight();
        this.width = settings.getCanvasWidth();
    }

    public int getPageNr() {
        return pageNr;
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
