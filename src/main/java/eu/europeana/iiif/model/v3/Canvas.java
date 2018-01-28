package eu.europeana.iiif.model.v3;

import java.io.Serializable;

/**
 * @author Patrick Ehlert
 * Created on 24-01-2018
 */
public class Canvas extends IdType implements Serializable {

    private static final long serialVersionUID = 3925574023427671991L;

    public LanguageMap label;
    public Integer height;
    public Integer width;
    public LanguageMap attribution;
    public Rights rights;
    public AnnotationPage[] items;

    public Canvas(String id) {
        super("Canvas");
        this.id = id;
    }
}
