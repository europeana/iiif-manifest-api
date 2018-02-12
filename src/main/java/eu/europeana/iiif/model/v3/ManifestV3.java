package eu.europeana.iiif.model.v3;

import eu.europeana.iiif.model.v2.DataSet;
import eu.europeana.iiif.model.v2.Image;
import eu.europeana.iiif.model.v2.ManifestV2;
import eu.europeana.iiif.model.v2.Sequence;

import java.io.Serializable;

/**
 * @author Patrick Ehlert
 * Created on 24-01-2018
 */
public class ManifestV3 extends ManifestV2 implements Serializable {

    private static final long serialVersionUID = -4087877560219592406L;

    // note that we extend v2.Manifest so we only list the types here that are different in v3
    private Collection[] within;
    private LanguageMap label;
    private LanguageMap description;
    private LanguageMap attribution;
    private Rights rights;
    private Image[] logo;
    private DataSet[] seeAlso;

    public Sequence[] items;

    public ManifestV3(String id) {
        super(id);
    }

    public void setWithin(Collection[] within) {
        this.within = within;
    }

    public void setLabel(LanguageMap label) {
        this.label = label;
    }

    public void setDescription(LanguageMap description) {
        this.description = description;
    }

    public void setAttribution(LanguageMap attribution) {
        this.attribution = attribution;
    }

    public void setRights(Rights rights) {
        this.rights = rights;
    }

    public void setLogo(Image[] logo) {
        this.logo = logo;
    }

    @Override
    public void setSeeAlso(DataSet[] seeAlso) {
        this.seeAlso = seeAlso;
    }

    public void setItems(Sequence[] items) {
        this.items = items;
    }
}
