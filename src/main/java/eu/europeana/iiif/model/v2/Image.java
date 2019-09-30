package eu.europeana.iiif.model.v2;

import ioinformarics.oss.jackson.module.jsonld.annotation.JsonldType;

/**
 * @author Patrick Ehlert
 * Created on 24-01-2018
 */
@JsonldType(value = "dctypes:Image")
public class Image extends JsonLdId {

    private static final long serialVersionUID = 1636104373070277504L;

    private Integer height;
    private Integer width;

    public Image(String id, Integer height, Integer width) {
        super(id);
        this.height = height;
        this.width = width;
    }

    public Integer getHeight() {
        return height;
    }

    public Integer getWidth() {
        return width;
    }
}
