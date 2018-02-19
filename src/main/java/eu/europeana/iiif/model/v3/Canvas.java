package eu.europeana.iiif.model.v3;

import eu.europeana.iiif.service.ManifestSettings;

import java.io.Serializable;

/**
 * @author Patrick Ehlert
 * Created on 24-01-2018
 */
public class Canvas extends JsonLdIdType implements Serializable {

    private static final long serialVersionUID = 3925574023427671991L;

    private LanguageMap label;
    private Integer height;
    private Integer width;
    private LanguageMap attribution;
    private Rights rights;
    private AnnotationPage[] items;

    public Canvas(ManifestSettings settings, String id) {
        super(id, "Canvas");
        this.height = settings.getCanvasHeight();
        this.width = settings.getCanvasWidth();
    }

    public LanguageMap getLabel() {
        return label;
    }

    public void setLabel(LanguageMap label) {
        this.label = label;
    }

    public Integer getHeight() {
        return height;
    }

    public Integer getWidth() {
        return width;
    }

    public LanguageMap getAttribution() {
        return attribution;
    }

    public void setAttribution(LanguageMap attribution) {
        this.attribution = attribution;
    }

    public Rights getRights() {
        return rights;
    }

    public void setRights(Rights rights) {
        this.rights = rights;
    }

    public AnnotationPage[] getItems() {
        return items;
    }

    public void setItems(AnnotationPage[] items) {
        this.items = items;
    }
}
