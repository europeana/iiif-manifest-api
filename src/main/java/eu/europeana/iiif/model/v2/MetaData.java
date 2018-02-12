package eu.europeana.iiif.model.v2;

import java.io.Serializable;

/**
 * @author Patrick Ehlert
 * Created on 06-02-2018
 */
public class MetaData implements Serializable {

    private static final long serialVersionUID = -4938773181802761161L;

    public String label;
    public LanguageObject[] value;

    public MetaData (String label, LanguageObject[] value) {
        this.label = label;
        this.value = value;
    }
}
