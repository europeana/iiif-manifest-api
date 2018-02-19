package eu.europeana.iiif.model.v3;

import java.io.Serializable;

/**
 * @author Patrick Ehlert
 * Created on 24-01-2018
 */
public class Sequence extends JsonLdIdType implements Serializable {

    private static final long serialVersionUID = 9146997370971854498L;

    private LanguageMap label = new LanguageMap("en", "Current page order");
    private String startCanvas;
    private Canvas[] items;

    public Sequence(String id) {
        super(id, "Sequence");
    }

    public LanguageMap getLabel() {
        return label;
    }

    public String getStartCanvas() {
        return startCanvas;
    }

    public void setStartCanvas(String startCanvas) {
        this.startCanvas = startCanvas;
    }

    public Canvas[] getItems() {
        return items;
    }

    public void setItems(Canvas[] items) {
        this.items = items;
    }
}
