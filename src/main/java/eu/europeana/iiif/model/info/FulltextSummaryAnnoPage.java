package eu.europeana.iiif.model.info;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import eu.europeana.iiif.model.v3.JsonLdIdType;

import static eu.europeana.iiif.model.ManifestDefinitions.INFO_ANNOPAGE_TYPE;

/**
 * Created by luthien on 15/04/2021.
 */
public class FulltextSummaryAnnoPage extends JsonLdIdType {

    private static final long serialVersionUID = -670619785903826924L;

    @JsonProperty("language")
    private String language;

    @JsonIgnore
    private boolean orig;

    public FulltextSummaryAnnoPage(){}

    /**
     * This object serves as a placeholder for either an original or translated AnnoPage
     * It is used in the summary info endpoint only
     *
     * @param id    String containing identifying URL of the FulltextSummaryAnnoPage
     * @param language  String containing language of the FulltextSummaryAnnoPage
     * @param orig  boolean is this the original language true / false
     */
    public FulltextSummaryAnnoPage(String id, String language, boolean orig){
        super(id, INFO_ANNOPAGE_TYPE);
        this.language = language;
        this.orig = orig;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }


}
