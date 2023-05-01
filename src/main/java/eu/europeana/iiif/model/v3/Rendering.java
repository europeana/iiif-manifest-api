package eu.europeana.iiif.model.v3;

/**
 * @author Srishti Singh
 * Created on 25-05-2023
 */
public class Rendering extends JsonLdIdType{


    private String format;
    private LanguageMap label;

    public Rendering(String id, String type) {
        super(id, type);
    }

    public Rendering(String id, String type, String format, LanguageMap label) {
        super(id, type);
        this.format = format;
        this.label = label;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public LanguageMap getLabel() {
        return label;
    }

    public void setLabel(LanguageMap label) {
        this.label = label;
    }
}
