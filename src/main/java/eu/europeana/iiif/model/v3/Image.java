package eu.europeana.iiif.model.v3;

import java.io.Serializable;

/**
 * @author Patrick Ehlert
 * Created on 24-01-2018
 */
public class Image extends JsonLdIdType implements Serializable {

    private static final long serialVersionUID = 6964375170132552570L;

    private Integer height;
    private Integer width;

    public Image(String id, Integer height, Integer width) {
        super(id, "Image");
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
