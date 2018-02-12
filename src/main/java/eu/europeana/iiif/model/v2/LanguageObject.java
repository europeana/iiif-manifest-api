package eu.europeana.iiif.model.v2;

import java.io.Serializable;

/**
 * @author Patrick Ehlert
 * Created on 06-02-2018
 */
public class LanguageObject implements Serializable {

    private static final long serialVersionUID = -6226879210853038163L;

    private String language;
    private String value;

    public LanguageObject(String language, String value) {
        this.language = language;
        this.value = value;
    }

}
