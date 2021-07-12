package eu.europeana.iiif.model.info;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.europeana.iiif.model.v3.JsonLdIdType;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static eu.europeana.iiif.model.Definitions.INFO_CANVAS_TYPE;

/**
 * Created by luthien on 15/04/2021.
 */
public class FulltextSummaryCanvas extends JsonLdIdType {

    private static final long serialVersionUID = 7066577659030844718L;

    private static final Pattern PAGENUMBERPATTERN = Pattern.compile("/canvas/(\\d+)$");

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

    public String getPageNumber(){
        Matcher m = PAGENUMBERPATTERN.matcher(getId());
        if (m.find( )) {
            return m.group(1);
        } else {
            return "x";
        }
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
}
