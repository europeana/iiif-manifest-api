package eu.europeana.iiif.model.v2;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.europeana.iiif.model.v3.LanguageMap;

import java.io.Serializable;

/**
 * @author Patrick Ehlert
 * Created on 06-02-2018
 */
public class LanguageObject implements Serializable {

    private static final long serialVersionUID = -6226879210853038163L;

    @JsonProperty("@language")
    private String language;
    @JsonProperty("@value")
    private String value;

    /**
     * Construct a new language object. Note that if language is 'def' it will not be set.
     * @param language
     * @param value
     */
    public LanguageObject(String language, String value) {
        // if no specific language is defined (def or @none), then don't set language at all
        if (!"def".equalsIgnoreCase(language) && !LanguageMap.NO_LANGUAGE_KEY.equalsIgnoreCase(language)) {
            this.language = language;
        }
        this.value = value;
    }

    public String getLanguage() {
        return language;
    }

    public String getValue() {
        return value;
    }
}
