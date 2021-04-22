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
public class SummaryCanvas extends JsonLdIdType {

    private static final long serialVersionUID = 7066577659030844718L;

    private static final Pattern PAGENUMBERPATTERN = Pattern.compile("/canvas/(\\d+)$");

    @JsonProperty("annotations")
    private List<SummaryAnnoPage> annotations;

    public SummaryCanvas(){}

    /**
     * This is not a true IIIF SummaryCanvas object but merely a container object to group original and
     * translated Annopages
     *
     * @param id String containing identifying URL of the SummaryCanvas
     */
    public SummaryCanvas(String id) {
        super(id, INFO_CANVAS_TYPE);
        annotations = new ArrayList<>();
    }

    /**
     * Adds an annotation - actually: an SummaryAnnoPage (AnnoPage for a specific language) to the SummaryCanvas
     * @param alPage SummaryAnnoPage object to be added to the annotations List
     */
    public void addAnnotation(SummaryAnnoPage alPage){
        annotations.add(alPage);
    }

    public List<SummaryAnnoPage> getAnnotations() {
        return new ArrayList<>(annotations);
    }

    public void setAnnotations(List<SummaryAnnoPage> annotations) {
        this.annotations = new ArrayList<>(annotations);
    }

    public String getPageNumber(){
        Matcher m = PAGENUMBERPATTERN.matcher(getId());
        if (m.find( )) {
            return m.group(1);
        } else {
            return "x";
        }
    }
}
