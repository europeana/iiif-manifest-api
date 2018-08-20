package eu.europeana.iiif.model.v3;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import eu.europeana.iiif.model.Definitions;
import eu.europeana.iiif.service.EdmManifestMapping;

import java.io.Serializable;

/**
 * @author Patrick Ehlert
 * Created on 24-01-2018
 */
public class ManifestV3 extends JsonLdIdType implements Serializable {

    private static final long serialVersionUID = -4087877560219592406L;

    @JsonIgnore
    private String europeanaId; // for internal use only

    @JsonProperty("@context")
    private String[] context = {"http://www.w3.org/ns/anno.jsonld", "http://iiif.io/api/presentation/3/context.json"};
    private Collection[] within;
    private LanguageMap label;
    private LanguageMap description;
    private MetaData[] metaData;
    private Image[] thumbnail;
    private String navDate;
    private LanguageMap attribution;
    private Rights rights;
    private Image[] logo;
    private DataSet[] seeAlso;
    private Sequence[] items;

    /**
     * Create a new empty manifest (only id, context and logo ar filled in)
     * @param europeanaId
     * @param manifestId
     */
    public ManifestV3(String europeanaId, String manifestId) {
        super(manifestId, "Manifest");
        this.europeanaId = europeanaId;
        logo = new Image[1];
        // TODO set height & width?
        logo[0] = new Image(Definitions.EUROPEANA_LOGO_URL, null, null);
    }


    public String getEuropeanaId() {
        return europeanaId;
    }

    public String[] getContext() {
        return context;
    }

    public Collection[] getWithin() {
        return within;
    }

    public void setWithin(Collection[] within) {
        this.within = within;
    }

    public LanguageMap getLabel() {
        return label;
    }

    public void setLabel(LanguageMap label) {
        this.label = label;
    }

    public LanguageMap getDescription() {
        return description;
    }

    public void setDescription(LanguageMap description) {
        this.description = description;
    }

    public MetaData[] getMetaData() {
        return metaData;
    }

    public void setMetaData(MetaData[] metaData) {
        this.metaData = metaData;
    }

    public Image[] getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(Image[] thumbnail) {
        this.thumbnail = thumbnail;
    }

    public String getNavDate() {
        return navDate;
    }

    public void setNavDate(String navDate) {
        this.navDate = navDate;
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

    public Image[] getLogo() {
        return logo;
    }

    public void setLogo(Image[] logo) {
        this.logo = logo;
    }

    public DataSet[] getSeeAlso() {
        return seeAlso;
    }

    public void setSeeAlso(DataSet[] seeAlso) {
        this.seeAlso = seeAlso;
    }

    public Sequence[] getItems() {
        return items;
    }

    public void setItems(Sequence[] items) {
        this.items = items;
    }
}
