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
    private LanguageMap requiredStatement; // attribution
    private Rights rights;
    private AnnotationPage[] items;
    private AnnotationPage[] annotations; // full text identifiers

    /**
     * Create a new canvas object
     * @param id
     * @param pageNr
     */
    public Canvas(String id, int pageNr) {
        super(id, "Canvas");
        this.pageNr = pageNr;
    }

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

    public void setHeight(Integer height) {
        this.height = height;
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

    public LanguageMap getRequiredStatement() {
        return requiredStatement;
    }

    public void setRequiredStatement(LanguageMap requiredStatement) {
        this.requiredStatement = requiredStatement;
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

    /**
     * If fulltext is available they this will contain AnnotationPages with only the full text identifier
     * @return array of {@link AnnotationPage} with full text identifiers (if fulltext is available)
     */
    public AnnotationPage[] getAnnotations() {
        return annotations;
    }

    public void setAnnotations(AnnotationPage[] annotations) {
        this.annotations = annotations;
    }
}
