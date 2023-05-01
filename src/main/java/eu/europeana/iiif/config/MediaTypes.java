package eu.europeana.iiif.config;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import eu.europeana.iiif.model.MediaType;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author srishti singh
 * @since 18 April 2023
 */
@JacksonXmlRootElement(localName = "config")
public class MediaTypes {

        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "format")
        public List<MediaType> mediaTypeCategories;

        private Map<String, MediaType> map = new HashMap<>();

         /**
         * Map contains all the suppoerted media types except EU Screen entries
         * @return
         */
         public Map<String, MediaType> getMap() {
             return this.map;
         }


        /**
         * Checks if a media Type is configured for the given mime Type
         *
         * @param mimeType mime type to match
         * @return true if a Media Type match is configured, false otherwise.
         */
        public boolean hasMediaType(String mimeType) {
            return map.containsKey(mimeType);
        }

        /**
         * Gets the configured media Type for the given entity mime type
         *
         * @param mimetype entity ID
         * @return Matching media Type, or empty Optional if none found
         */
        public Optional<MediaType> getMediaType(String mimetype) {
            if (StringUtils.isNotEmpty(mimetype)) {
                return Optional.ofNullable(map.get(mimetype));
            }
            return Optional.empty();
        }

        public Optional<MediaType> getEUScreenType(String edmType) {
            return mediaTypeCategories.stream().filter(s -> s.isEuScreen() && s.getType().equalsIgnoreCase(edmType)).findFirst();
        }

}
