package eu.europeana.iiif.model;

import java.io.Serializable;

/**
 * This class functions a base class for all manifests (ease of reference)
 *
 * @author Patrick Ehlert
 * Created on 07-02-2018
 */
public class AbstractManifest extends IdType implements Serializable {

    private static final long serialVersionUID = 7093070186267688349L;

    public AbstractManifest(String id) {
        super(id, "Manifest");
    }
}
