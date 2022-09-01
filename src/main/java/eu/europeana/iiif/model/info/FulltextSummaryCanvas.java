package eu.europeana.iiif.model.info;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.europeana.iiif.model.v3.JsonLdIdType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static eu.europeana.iiif.model.Definitions.INFO_CANVAS_TYPE;

/**
 * Created by luthien on 15/04/2021.
 */
public class FulltextSummaryCanvas extends JsonLdIdType {

    private static final long serialVersionUID = 7066577659030844718L;

    private static final String PAGE_ID_START = "/canvas/";

    private String originalLanguage;

    @JsonProperty("annotations")
    private List<FulltextSummaryAnnoPage> annotations;

    public FulltextSummaryCanvas(){}

    /**
     * This is not a true IIIF FulltextSummaryCanvas object but merely a container object to group original and
     * translated Annopages
     *
     * @param id String containing identifying URL of the FulltextSummaryCanvas
     */
    public FulltextSummaryCanvas(String id) {
        super(id, INFO_CANVAS_TYPE);
        annotations = new ArrayList<>();
    }

    /**
     * Adds an annotation - actually: an FulltextSummaryAnnoPage (AnnoPage for a specific language) to the FulltextSummaryCanvas
     * @param alPage FulltextSummaryAnnoPage object to be added to the annotations List
     */
    public void addAnnotation(FulltextSummaryAnnoPage alPage){
        annotations.add(alPage);
    }

    public List<FulltextSummaryAnnoPage> getAnnotations() {
        return new ArrayList<>(annotations);
    }

    /**
     * Return the page id of this fulltext annopage
     * @return fulltext id or null if it cannot be found
     */
    public String getPageNumber(){
        int start = getId().indexOf(PAGE_ID_START);
        if (start == -1) {
            return null;
        }
        String result = getId().substring(start + PAGE_ID_START.length());
        int end = result.indexOf('?');
        if (end != -1) {
            result = result.substring(0, end);
        }
        return result;
    }

    public String getOriginalLanguage() {
        return originalLanguage;
    }

    /*
     * Used by Jackson deserializing data from Fulltext API
     */
    public void setOriginalLanguage(String originalLanguage) {
        this.originalLanguage = originalLanguage;
    }

    public List<String> getAnnoPageIDs(){
        List<String> annoPageIDs  = new ArrayList<>();
        for (FulltextSummaryAnnoPage sap : annotations) {
            annoPageIDs.add(sap.getId());
        }
        return annoPageIDs;
    }

    public Map<String, String> getAnnoPageIDLang(){
        Map<String, String> annoPageLangs  = new HashMap<>();
        for (FulltextSummaryAnnoPage sap : annotations) {
            annoPageLangs.put(sap.getId(), sap.getLanguage());
        }
        return annoPageLangs;
    }
}
