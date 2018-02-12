package eu.europeana.iiif.model.v2;

import eu.europeana.iiif.model.IdType;

import java.io.Serializable;

/**
 * @author Patrick Ehlert
 * Created on 06-02-2018
 */
public class Canvas extends IdType implements Serializable {

    private static final long serialVersionUID = 6160907015595073905L;

    // TODO load height and width from configuration file

    public String label;
    public Integer height = 1024;
    public Integer width = 686;
    public LanguageObject attribution;
    public String license;
    public Annotation[] images;
    public FullText[] otherContent;

    public Canvas(String id) {
        super(id, "sc:Canvas");

    }
}
