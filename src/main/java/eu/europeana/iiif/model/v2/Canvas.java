package eu.europeana.iiif.model.v2;

import com.fasterxml.jackson.annotation.JsonIgnore;
import ioinformarics.oss.jackson.module.jsonld.annotation.JsonldType;

/**
 * @author Patrick Ehlert
 * Created on 06-02-2018
 */
@JsonldType("sc:Canvas")
public class Canvas extends JsonLdId {

    private static final long serialVersionUID = 6160907015595073905L;

    @JsonIgnore
    private int pageNr; // for internal use

    private String label;
    private Integer height;
    private Integer width;
    private String attribution;
    private String license;
    private Annotation[] images;
    private Rendering rendering;     // EA-3413
    private Image thumbnail;  // EA-3325
    private String [] otherContent;  // only 1 value is expected (or null)

    /**
     * Create a new canvas object
     * @param id
     * @param pageNr
     * @param height
     * @param width
     */
    public Canvas(String id, int pageNr, Integer height, Integer width) {
        super(id);
        this.pageNr = pageNr;
        this.height = height;
        this.width = width;
    }

    /**
     * Create a new canvas object
     * @param id
     * @param pageNr
     */
    public Canvas(String id, int pageNr) {
        super(id);
        this.pageNr = pageNr;
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

    public void setHeight(Integer height) {
        this.height = height;
    }

    public void setWidth(Integer width) {
        this.width = width;
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

    public Image getThumbnail() {
        // EA-3325
        return thumbnail;
    }

    public void setThumbnail(Image thumbnail) {
        // EA-3325
        this.thumbnail = thumbnail;
    }

    @JsonIgnore
    public Annotation getStartImageAnnotation() {
        if (images == null || images.length == 0) {
            return null;
        }
        return images[0];
    }

    public Rendering getRendering() {
        return rendering;
    }

    public void setRendering(Rendering rendering) {
        this.rendering = rendering;
    }

    public String[] getOtherContent() {
        return otherContent;
    }

    public void setOtherContent(String[] otherContent) {
        this.otherContent = otherContent;
    }
}
