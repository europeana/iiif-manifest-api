package eu.europeana.iiif.model.v3;

import java.io.Serializable;

/**
 * @author Patrick Ehlert
 * Created on 24-01-2018
 */
public class MetaData implements Serializable {

    private static final long serialVersionUID = -8198825949445182532L;

    private LanguageMap label;
    private LanguageMap value;

    public MetaData(LanguageMap label, LanguageMap value) {
        this.label = label;
        this.value = value;
    }

    public LanguageMap getLabel() {
        return label;
    }

    public LanguageMap getValue() {
        return value;
    }

}
