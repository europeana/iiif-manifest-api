package eu.europeana.iiif.model.v3;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

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
    // EA-3324
    private RequiredStatementMap requiredStatement; // attribution
    private Rights rights;
    private AnnotationPage[] items;
    private Rendering rendering; // EA-3413
    @JsonProperty("annotations")
    private AnnotationPage[] ftSummaryAnnoPages; // full text identifiers
    private Image[]          thumbnail;     // EA-3325

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

    // EA-3324 changed type of RequiredStatement from LanguageMap to RequiredStatementMap
    public RequiredStatementMap getRequiredStatement() {
        return requiredStatement;
    }

    public void setRequiredStatement(RequiredStatementMap requiredStatement) {
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

    public Rendering getRendering() {
        return rendering;
    }

    public void setRendering(Rendering rendering) {
        this.rendering = rendering;
    }

    @JsonIgnore
    public Annotation getStartCanvasAnnotation() {
        if (items == null || items.length == 0) {
            return null;
        }
        return items[0].getItems()[0];
    }

    public void setItems(AnnotationPage[] items) {
        this.items = items;
    }

    /**
     * If fulltext is available they this will contain AnnotationPages with only the full text identifier
     * @return array of {@link AnnotationPage} with full text identifiers (if fulltext is available)
     */
    public AnnotationPage[] getFtSummaryAnnoPages() {
        return ftSummaryAnnoPages;
    }

    public void setFtSummaryAnnoPages(AnnotationPage[] ftSummaryAnnoPages) {
        this.ftSummaryAnnoPages = ftSummaryAnnoPages;
    }

    public Image[] getThumbnail() {
        // EA-3325
        return thumbnail;
    }

    public void setThumbnail(Image[] thumbnail) {
        // EA-3325
        this.thumbnail = thumbnail;
    }
}
