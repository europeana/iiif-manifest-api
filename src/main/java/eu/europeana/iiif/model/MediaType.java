package eu.europeana.iiif.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * @author srishti singh
 * @since 18 April 2023
 */
@JacksonXmlRootElement(localName = "format")
public class MediaType {

    private static final String BROWSER = "Browser";
    private static final String RENDERED = "Rendered";
    private static final String EU_SCREEN = "EUScreen";

    public static final String  VIDEO    = "Video";
    public static final String  SOUND    = "Sound";

    @JacksonXmlProperty(localName =  "mediaType", isAttribute = true)
    private String mimeType;

    @JacksonXmlProperty(isAttribute = true)
    private String label;

    @JacksonXmlProperty(isAttribute = true)
    private String type;

    @JacksonXmlProperty(isAttribute = true)
    private String support;

    public String getMimeType() {
        return mimeType;
    }

    public String getLabel() {
        return label;
    }

    public String getType() {
        return type;
    }

    public String getSupport() {
        return support;
    }

    public boolean isRendered() {
        return RENDERED.equals(getSupport());
    }

    public boolean isBrowserSupported() {
        return BROWSER.equals(getSupport());
    }

    public boolean isVideoOrSound() {
        return ( VIDEO.equals(getType()) || SOUND.equals(getType()) ) ;
    }

    public boolean isEuScreen() {
        return EU_SCREEN.equals(getSupport());
    }
}


