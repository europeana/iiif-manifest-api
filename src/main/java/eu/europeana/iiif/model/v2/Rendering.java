package eu.europeana.iiif.model.v2;

/**
 * @author Srishti Singh
 * Created on 25-05-2023
 */
public class Rendering extends JsonLdId {

    private String format;
    private String label;

    public Rendering(String id, String format, String label) {
        super(id);
        this.format = format;
        this.label = label;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
