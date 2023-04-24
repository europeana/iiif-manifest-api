package eu.europeana.iiif.config;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import eu.europeana.iiif.model.MediaType;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Optional;

/**
 * @author srishti singh
 * @since 18 April 2023
 */
@JacksonXmlRootElement(localName = "config")
public class MediaTypes {

        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "source")
        private List<MediaType> mediaTypes;

        public List<MediaType> getMediaTypes() {
            return mediaTypes;
        }

        /**
         * Checks if a media Type is configured for the given mime Type
         *
         * @param mimeType mime type to match
         * @return true if a Media Type match is configured, false otherwise.
         */
        public boolean hasMediaType(String mimeType) {
            return mediaTypes.stream().anyMatch(s -> mimeType.contains(s.getMimeType()));
        }

        /**
         * Gets the configured media Type for the given entity mime type
         *
         * @param mimetype entity ID
         * @return Matching media Type, or empty Optional if none found
         */
        public Optional<MediaType> getMediaType(String mimetype) {
            if (StringUtils.isNotEmpty(mimetype)) {
                return mediaTypes.stream().filter(s -> mimetype.contains(s.getMimeType())).findFirst();
            }
            return Optional.empty();
        }

        public Optional<MediaType> getEUScreenType(String edmType) {
            return mediaTypes.stream().filter(s -> s.isEuScreen() && s.getType().equalsIgnoreCase(edmType)).findFirst();
        }

}
