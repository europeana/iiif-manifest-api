package eu.europeana.iiif.config;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import eu.europeana.iiif.model.MediaType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

/**
 * @author srishti singh
 * @since 18 April 2023
 */
@Configuration
public class AppConfig {

    private static final Logger LOG = LogManager.getLogger(AppConfig.class);

    @Resource
    private ManifestSettings manifestSettings;

    @Resource(name = "msXmlMapper")
    private XmlMapper xmlMapper;


    @Bean(name = "msMediaTypes")
    public MediaTypes getMediaTypes() throws IOException {
        String mediaTypeXMLConfigFile = manifestSettings.getMediaXMLConfig();

        MediaTypes mediaTypes;
        try (InputStream inputStream = getClass().getResourceAsStream(mediaTypeXMLConfigFile)) {
            assert inputStream != null;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                String contents = reader.lines().collect(Collectors.joining(System.lineSeparator()));
                mediaTypes = xmlMapper.readValue(contents, MediaTypes.class);
            }
        }

        if (!mediaTypes.mediaTypeCategories.isEmpty()) {
            mediaTypes.getMap().putAll(mediaTypes.mediaTypeCategories.stream().filter(media -> !media.isEuScreen()).collect(Collectors.toMap(MediaType::getMimeType, e-> e)));
        } else {
            LOG.error("media Categories not configured at startup. mediacategories.xml file not added or is empty");
        }
        return mediaTypes;
    }

}
