package eu.europeana.iiif.model.v3;

import java.io.Serializable;

/**
 * @author Patrick Ehlert
 * Created on 24-01-2018
 */
public class AnnotationPage extends JsonLdIdType implements Serializable {

    private static final long serialVersionUID = -259542823420543924L;

    private Annotation[] items;

    public AnnotationPage(String id) {
        super(id, "AnnotationPage");
    }

    public Annotation[] getItems() {
        return items;
    }

    public void setItems(Annotation[] items) {
        this.items = items;
    }
}
