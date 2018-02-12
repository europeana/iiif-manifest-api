package eu.europeana.iiif.model.v2;

import eu.europeana.iiif.model.IdType;

import java.io.Serializable;

/**
 * @author Patrick Ehlert
 * Created on 06-02-2018
 */
public class Service extends IdType implements Serializable {

    private static final long serialVersionUID = -7367911509714923855L;

    private String context = "http://iiif.io/api/image/2/context.json";
    private String profile;

    public Service(String id) {
        super(id, "sc:ImageService2");
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }
}
