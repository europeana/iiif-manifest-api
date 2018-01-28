package eu.europeana.iiif.model.v3;

import java.io.Serializable;

/**
 * @author Patrick Ehlert
 * Created on 24-01-2018
 */
public class Manifest extends IdType implements Serializable {

    private static final long serialVersionUID = -4087877560219592406L;

    public Collection[] within;
    public LanguageMap label;
    public LanguageMap description;
    public MetaData metadata;
    public Image[] thumbnail;
    public String navDate;
    public LanguageMap attribution;
    public Rights rights;
    public Image[] logo;
    public DataSet[] seeAlso;
    public Sequence[] items;

    public Manifest(String id) {
        super("Manifest");
        this.id = id;
    }

}
