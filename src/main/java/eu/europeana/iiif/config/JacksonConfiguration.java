package eu.europeana.iiif.config;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * We are getting org.springframework.http.converter.HttpMessageConversionException
 *  Nested exception -  com.fasterxml.jackson.databind.exc.InvalidDefinitionException: Java 8 date/time type java.time.OffsetDateTime not supported by default:
 *  add Module "com.fasterxml.jackson.datatype:jackson-datatype-jsr310" to enable handling
 *  (through reference chain: eu.europeana.api.commons.error.EuropeanaApiErrorResponse["timestamp"])
 *
 *  Adding jsr module is not enough, we need to create a bean overriding the spring default Object mapper with Java Time Module
 * @author shweta
 */
@Configuration
public class JacksonConfiguration {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }
}