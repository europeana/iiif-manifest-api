package eu.europeana.iiif.model.v3;

/**
 * @author Patrick Ehlert
 * Created on 24-01-2018
 */
public class AnnotationPage extends JsonLdIdType {

    private static final long serialVersionUID = -259542823420543924L;

    private Annotation[] items;

    private String language;

    private String[] textGranularity;

    private String source;

    public AnnotationPage(String id) {
        super(id, "AnnotationPage");
    }

    public AnnotationPage(String id, String language) {
        super(id, "AnnotationPage");
        this.language = language;
    }

    // This used for setting id, lang and source in annotations field in canvas
    public AnnotationPage(String id, String language, String source) {
        super(id, "AnnotationPage");
        this.language = language;
        this.source = source;
    }

    // Used for creating Annotationpages with FullText summary "annotations" (actually, FTAnnoPages)
    public AnnotationPage(String id, String language, String[] textGranularity, String source) {
        super(id, "AnnotationPage");
        this.language = language;
        this.textGranularity = textGranularity;
        this.source = source;
    }

    public String getLanguage() {
        return language;
    }

    public String[] getTextGranularity() {
        return textGranularity;
    }

    public void setTextGranularity(String[] textGranularity) {
        this.textGranularity = textGranularity;
    }

    public Annotation[] getItems() {
        return items;
    }

    public void setItems(Annotation[] items) {
        this.items = items;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
