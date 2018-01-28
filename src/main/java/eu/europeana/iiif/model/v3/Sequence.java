package eu.europeana.iiif.model.v3;

import java.io.Serializable;

/**
 * @author Patrick Ehlert
 * Created on 24-01-2018
 */
public class Sequence extends IdType implements Serializable {

    private static final long serialVersionUID = 9146997370971854498L;

    public LanguageMap label;
    public String startCanvas;
    public Canvas[] items;

    public Sequence(String id) {
        super("Sequence");
        this.id = id;
    }
}
