package eu.europeana.iiif.model.v2;

import ioinformarics.oss.jackson.module.jsonld.annotation.JsonldType;

import java.io.Serializable;

/**
 * @author Patrick Ehlert
 * Created on 06-02-2018
 */
@JsonldType("sc:Sequence")
public class Sequence extends JsonLdId implements Serializable {

    private static final long serialVersionUID = -5249256267287170116L;

    private String label = "Current Page Order";
    private String startCanvas;
    private Canvas[] canvases;

    public Sequence(String id) {
        super(id);
    }

    public String getLabel() {
        return label;
    }

    public String getStartCanvas() {
        return startCanvas;
    }

    public void setStartCanvas(String startCanvas) {
        this.startCanvas = startCanvas;
    }

    public Canvas[] getCanvases() {
        return canvases;
    }

    public void setCanvases(Canvas[] canvases) {
        this.canvases = canvases;
    }
}
