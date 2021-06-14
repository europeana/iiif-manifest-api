package eu.europeana.iiif.model.info;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static eu.europeana.iiif.model.Definitions.MEDIA_TYPE_IIIF_V3;
import static eu.europeana.iiif.model.Definitions.MEDIA_TYPE_W3ORG_JSONLD;

/**
 * Created by luthien on 07/04/2021.
 */
public class FulltextSummary implements Serializable {
    private static final long serialVersionUID = -8052995235828716772L;


    @JsonProperty("@context")
    private final String[] context = new String[]{MEDIA_TYPE_W3ORG_JSONLD, MEDIA_TYPE_IIIF_V3};

    private String              dataSetId;
    private String              localId;

    @JsonProperty("items")
    private List<FulltextSummaryCanvas> canvases;

    public FulltextSummary(){}

    /**
     * This is a container object to group "fake" FulltextSummaryCanvas objects containing original and translated AnnoPages
     * for a given Fulltext record / object
     *
     * @param dataSetId String containing the dataset of this Fulltext FulltextSummary
     * @param localId   String containing the localId of this Fulltext FulltextSummary
     */
    public FulltextSummary(String dataSetId, String localId){
        this.dataSetId = dataSetId;
        this.localId = localId;
        canvases = new ArrayList<>();
    }

    public String getDataSetId() {
        return dataSetId;
    }

    public void setDataSetId(String dataSetId) {
        this.dataSetId = dataSetId;
    }

    public String getLocalId() {
        return localId;
    }

    public void setLocalId(String localId) {
        this.localId = localId;
    }


    /**
     * Adds a *fake* FulltextSummaryCanvas containing original and translated versions of an AnnoPage (AnnotationLangPages)
     * @param fulltextSummaryCanvas FulltextSummaryCanvas object to be added to the canvases List
     */
    public void addCanvas(FulltextSummaryCanvas fulltextSummaryCanvas){
        canvases.add(fulltextSummaryCanvas);
    }

    @JsonValue
    public List<FulltextSummaryCanvas> getCanvases() {
        return new ArrayList<>(canvases);
    }

    public void setCanvases(List<FulltextSummaryCanvas> canvases) {
        this.canvases = new ArrayList<>(canvases);
    }

}
