package eu.europeana.iiif.model.v3;

import eu.europeana.iiif.model.IdType;

import java.io.Serializable;

/**
 * @author Patrick Ehlert
 * Created on 24-01-2018
 */
public class Annotation extends IdType implements Serializable {

    private static final long serialVersionUID = -2420858050000556844L;

    public String motivation = "painting";
    public AnnotationBody body;
    public String target;

    public Annotation(String id) {
        super(id, "Annotation");
    }

}
