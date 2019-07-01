package eu.europeana.iiif.model.v3;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * @author Patrick Ehlert
 * Created on 24-01-2018
 */
public class Canvas extends JsonLdIdType {

    private static final long serialVersionUID = 3925574023427671991L;

    @JsonIgnore
    private int pageNr; // for internal use

    private LanguageMap label;
    private Integer height;
    private Integer width;
    private Double duration;
    private LanguageMap attribution;
    private Rights rights;
    private AnnotationPage[] items;

    /**
     * Create a new canvas object
     * @param id
     * @param pageNr
     * @param height
     * @param width
     */
    public Canvas(String id, int pageNr, Integer height, Integer width) {
        super(id, "Canvas");
        this.pageNr = pageNr;
        this.height = height;
        this.width = width;
    }

    public int getPageNr() {
        return pageNr;
    }

    public LanguageMap getLabel() {
        return label;
    }

    public void setLabel(LanguageMap label) {
        this.label = label;
    }

    public Integer getHeight() {
        return height;
    }

    public Integer getWidth() {
        return width;
    }

    public Double getDuration() {
        return duration;
    }

    public void setDuration(Double duration) {
        this.duration = duration;
    }

    public LanguageMap getAttribution() {
        return attribution;
    }

    public void setAttribution(LanguageMap attribution) {
        this.attribution = attribution;
    }

    public Rights getRights() {
        return rights;
    }

    public void setRights(Rights rights) {
        this.rights = rights;
    }

    public AnnotationPage[] getItems() {
        return items;
    }

    public void setItems(AnnotationPage[] items) {
        this.items = items;
    }
}
