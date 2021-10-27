package eu.europeana.iiif.model.v3;

/**
 * @author Patrick Ehlert
 * Created on 24-01-2018
 */
public class AnnotationPage extends JsonLdIdType {

    private static final long serialVersionUID = -259542823420543924L;

    private Annotation[] items;
    private String language;

    public AnnotationPage(String id) {
        super(id, "AnnotationPage");
    }

    public AnnotationPage(String id, String language) {
        super(id, "AnnotationPage");
        this.language = language;
    }

    public String getLanguage() {
        return language;
    }

    public Annotation[] getItems() {
        return items;
    }

    public void setItems(Annotation[] items) {
        this.items = items;
    }
}
