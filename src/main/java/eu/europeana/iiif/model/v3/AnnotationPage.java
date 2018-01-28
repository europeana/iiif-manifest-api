package eu.europeana.iiif.model.v3;

import java.io.Serializable;

/**
 * @author Patrick Ehlert
 * Created on 24-01-2018
 */
public class AnnotationPage extends IdType implements Serializable {

    private static final long serialVersionUID = -259542823420543924L;

    public Annotation[] items;

    public AnnotationPage(String id) {
        super("AnnotationPage");
        this.id = id;
    }
}
