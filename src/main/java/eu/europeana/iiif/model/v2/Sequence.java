package eu.europeana.iiif.model.v2;

import eu.europeana.iiif.model.Definitions;
import eu.europeana.iiif.model.IdType;

import java.io.Serializable;

/**
 * @author Patrick Ehlert
 * Created on 06-02-2018
 */
public class Sequence extends IdType implements Serializable {

    private static final long serialVersionUID = -5249256267287170116L;

    private String label = "Current Page Order";
    private String startCanvas;
    private Canvas[] canvases;

    public Sequence(String id) {
        super(id, "sc:Sequence");
    }

    public void setStartCanvas(String startCanvas) {
        this.startCanvas = startCanvas;
    }

    public void setCanvases(Canvas[] canvases) {
        this.canvases = canvases;
    }
}
