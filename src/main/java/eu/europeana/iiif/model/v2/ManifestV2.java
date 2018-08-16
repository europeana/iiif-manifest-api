package eu.europeana.iiif.model.v2;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import eu.europeana.iiif.model.Definitions;
import eu.europeana.iiif.service.EdmManifestMapping;
import ioinformarics.oss.jackson.module.jsonld.annotation.JsonldType;

import java.io.Serializable;

/**
 * IIIF Manifest version 2 (see also http://iiif.io/api/presentation/2.1/#manifest)
 *
 * @author Patrick Ehlert
 * Created on 06-02-2018
 */
@JsonPropertyOrder({"id", "context"})
@JsonldType(value = "sc:Manifest")
public class ManifestV2 extends JsonLdId implements Serializable {

    private static final long serialVersionUID = -2645198128531918309L;

    @JsonIgnore
    private String europeanaId; // for internal use only

    @JsonProperty("@context")
    private String context = "http://iiif.io/api/presentation/2/context.json";
    private String within;
    private LanguageObject[] label;
    private LanguageObject[] description;
    private MetaData[] metadata;
    private Image thumbnail;
    private String navDate;
    private String attribution;
    private String license;
    private String logo = Definitions.EUROPEANA_LOGO_URL;
    private DataSet[] seeAlso;
    private Sequence[] sequences;

    /**
     * Create a new empty manifest (only id is filled)
     * @param europeanaId
     */
    public ManifestV2(String europeanaId) {
        super(EdmManifestMapping.getManifestId(europeanaId));
        this.europeanaId = europeanaId;
    }

    public String getEuropeanaId() {
        return europeanaId;
    }

    public String getContext() {
        return context;
    }

    public String getWithin() {
        return within;
    }

    public void setWithin(String within) {
        this.within = within;
    }

    public LanguageObject[] getLabel() {
        return label;
    }

    public void setLabel(LanguageObject[] label) {
        this.label = label;
    }

    public LanguageObject[] getDescription() {
        return description;
    }

    public void setDescription(LanguageObject[] description) {
        this.description = description;
    }

    public MetaData[] getMetadata() {
        return metadata;
    }

    public void setMetadata(MetaData[] metadata) {
        this.metadata = metadata;
    }

    public Image getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(Image thumbnail) {
        this.thumbnail = thumbnail;
    }

    public String getNavDate() {
        return navDate;
    }

    public void setNavDate(String navDate) {
        this.navDate = navDate;
    }

    public String getAttribution() {
        return attribution;
    }

    public void setAttribution(String attribution) {
        this.attribution = attribution;
    }

    public String getLicense() {
        return license;
    }

    public void setLicense(String license) {
        this.license = license;
    }

    public String getLogo() {
        return logo;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }

    public DataSet[] getSeeAlso() {
        return seeAlso;
    }

    public void setSeeAlso(DataSet[] seeAlso) {
        this.seeAlso = seeAlso;
    }

    public Sequence[] getSequences() {
        return sequences;
    }

    public void setSequences(Sequence[] sequences) {
        this.sequences = sequences;
    }
}
