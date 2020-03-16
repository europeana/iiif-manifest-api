package eu.europeana.iiif.model.v3;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Manifest v3 root document
 *
 * @author Patrick Ehlert
 * Created on 24-01-2018
 * Modified to latest v3 developments on March 2020
 */
@JsonPropertyOrder({"context", "id", "type"})
public class ManifestV3 extends JsonLdIdType {

    private static final long serialVersionUID = -4087877560219592406L;

    private static final String[] context = {"http://www.w3.org/ns/anno.jsonld",
                                             "http://iiif.io/api/presentation/3/context.json"};
    private static final Agent[] provider = new Agent[]{ new Agent() };

    private Collection[] within;
    private LanguageMap label; // edm:title
    private LanguageMap summary; // edm:description
    private MetaData[] metaData;
    private Image[] thumbnail;
    private Text[] homepage;
    private String navDate;
    private LanguageMap requiredStatement; // edm:attribution
    private Rights rights;
    private DataSet[] seeAlso;
    private Canvas start;
    private Canvas[] items;

    @JsonIgnore
    private String europeanaId; // for internal use only
    @JsonIgnore
    private String isShownBy; // for internal use only

    /**
     * Create a new empty manifest (only id, context and logo ar filled in)
     * @param europeanaId
     * @param manifestId
     */
    public ManifestV3(String europeanaId, String manifestId, String isShownBy) {
        super(manifestId, "Manifest");
        this.europeanaId = europeanaId;
        this.isShownBy = isShownBy;
    }

    public String getEuropeanaId() {
        return europeanaId;
    }

    public String getIsShownBy() {
        return isShownBy;
    }

    @JsonProperty("@context")
    public String[] getContext() {
        return ManifestV3.context;
    }

    public Collection[] getWithin() {
        return within;
    }

    public void setWithin(Collection[] within) {
        this.within = within;
    }

    /**
     * @return {@link LanguageMap} containing proxy/title, or if empty proxy/description
     */
    public LanguageMap getLabel() {
        return label;
    }

    public void setLabel(LanguageMap label) {
        this.label = label;
    }

    /**
     * @return {@link LanguageMap} containing proxy/description (but only if proxy/title exists)
     */
    public LanguageMap getSummary() {
        return summary;
    }

    public void setSummary(LanguageMap summary) {
        this.summary = summary;
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

    /**
     * @return array of {@link Text} containing only 1 reference to the Europeana homepage (in English)
     */
    public Text[] getHomepage() {
        return homepage;
    }

    public void setHomePage(Text[] homepage) {
        this.homepage = homepage;
    }

    /**
     * @return {@link LanguageMap} containing only an English attribution snippet (in html format)
     */
    public LanguageMap getRequiredStatement() {
        return requiredStatement;
    }

    public void setRequiredStatement(LanguageMap requiredStatement) {
        this.requiredStatement = requiredStatement;
    }

    public Rights getRights() {
        return rights;
    }

    public void setRights(Rights rights) {
        this.rights = rights;
    }

    public DataSet[] getSeeAlso() {
        return seeAlso;
    }

    /**
     * @return array of {@link Agent} containing only 1 "Europeana" agent (with Europeana homepage)
     */
    public Agent[] getProvider() {
        return ManifestV3.provider;
    }

    public void setSeeAlso(DataSet[] seeAlso) {
        this.seeAlso = seeAlso;
    }

    /**
     * @return {@link Canvas} only containing an id and type. The id refers to the Canvas/page with edm:isShownBy.
     * If that doesn't exists we return the first canvas (p1)
     */
    public Canvas getStart() {
        return start;
    }

    public void setStart(Canvas startCanvas) {
        this.start = startCanvas;
    }

    public Canvas[] getItems() {
        return items;
    }

    public void setItems(Canvas[] items) {
        this.items = items;
    }
}
