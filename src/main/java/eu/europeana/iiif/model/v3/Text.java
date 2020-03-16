package eu.europeana.iiif.model.v3;

/**
 *  Contains a reference to an item page or the Europeana homepage (in English language)
 *  @author Patrick Ehlert
 *  Created on 9-3-2020
 */
public class Text extends JsonLdIdType {

    private LanguageMap label;

    /**
     * Create a new text that refers to an item page or the Europeana homepage
     * @param id String containing link to Europeana website
     * @param label {@link LanguageMap} with English description of the link
     */
    public Text(String id, LanguageMap label) {
        super(id, "Text");
        this.label = label;
    }

    public LanguageMap getLabel() {
        return label;
    }

    public String getFormat() {
        return "text/html";
    }
}
