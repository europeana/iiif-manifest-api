package eu.europeana.iiif.model.v3;

import java.io.Serializable;

/**
 * MetaData object that holds various proxy data such as dc:date, dc:format, dc:relation, dc:type, dc:language
 * and dc:source.
 * @author Patrick Ehlert
 * Created on 24-01-2018
 */
public class MetaData implements Serializable {

    private static final long serialVersionUID = -8198825949445182532L;

    private LanguageMap label;
    private LanguageMap value;

    /**
     * Create a new MetaData object
     * @param label languagemap containing metadata field label information in various languages
     * @param value languagemap containing metadata field value information in various lanuages
     */
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

    /**
     * @return textual representation of the contents of the metadata object (for debugging purposes)
     */
    @Override
    public String toString() {
        return "label: " + label + " value: " + value;
    }

}
