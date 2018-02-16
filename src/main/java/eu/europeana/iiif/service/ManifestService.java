package eu.europeana.iiif.service;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider;
import com.jayway.jsonpath.spi.json.JsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import com.jayway.jsonpath.spi.mapper.MappingProvider;
import eu.europeana.iiif.model.v2.ManifestV2;
import eu.europeana.iiif.model.v3.ManifestV3;
import eu.europeana.iiif.service.exception.IIIFException;
import eu.europeana.iiif.service.exception.InvalidApiKeyException;
import eu.europeana.iiif.service.exception.RecordNotFoundException;
import eu.europeana.iiif.service.exception.RecordParseException;
import eu.europeana.iiif.service.exception.RecordRetrieveException;
import ioinformarics.oss.jackson.module.jsonld.JsonldModule;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Service that loads record data, uses that to generate a Manifest object and serializes the manifest in JSON-LD
 *
  * @author Patrick Ehlert
 *  Created on 06-12-2017
 */
@Service
public class ManifestService {

    private static final Logger LOG = LogManager.getLogger(ManifestService.class);

    // create a single objectMapper for efficiency purposes (see https://github.com/FasterXML/jackson-docs/wiki/Presentation:-Jackson-Performance)
    private static ObjectMapper mapper = new ObjectMapper();

    //TODO fix proper loading from .properties file (doesn't work right now)


    @Value("${record-api.url:bla}")
    private String recordApiUrl = "http://www.europeana.eu/api/v2/record";

    @Value("${suppress-parse-exception:true}")
    private String suppressParseException = "false";

    private CloseableHttpClient httpClient = HttpClients.createDefault();

    public ManifestService() {
        LOG.debug("init manifestService, recordUrl = {} ", recordApiUrl);

        // configure jsonpath: we use jsonpath in combination with Jackson because otherwise we do not always know what
        // type of objects are returned (see also https://stackoverflow.com/a/40963445)
        Configuration.setDefaults(new Configuration.Defaults() {

            private final JsonProvider jsonProvider = new JacksonJsonNodeJsonProvider();
            private final MappingProvider mappingProvider = new JacksonMappingProvider();

            @Override
            public JsonProvider jsonProvider() {
                return jsonProvider;
            }

            @Override
            public MappingProvider mappingProvider() {
                return mappingProvider;
            }

            @Override
            public Set<Option> options() {
                if (Boolean.parseBoolean(suppressParseException)) {
                    // we want to be fault tolerant in production, but for testing we may want to disable this option
                    return EnumSet.of(Option.SUPPRESS_EXCEPTIONS);
                } else {
                    return EnumSet.noneOf(Option.class);
                }
            }
        });

        // configure Jackson serialization
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        mapper.registerModule(new JsonldModule(loadJsonContext()));
    }

    protected ObjectMapper getJsonMapper() {
        return mapper;
    }

    /**
     * Load the json context from file
     * @return
     */
    private Map<String, Object> loadJsonContext() {
        try (InputStream in = this.getClass().getResourceAsStream("/jsonld/context.jsonld")) {
            return mapper.readValue(in, Map.class);
        } catch (IOException e) {
            LOG.fatal("Error reading context.jsonld", e);
        }
        return null;
    }

    /**
     * Return record information in Json format.
     * @param recordId Europeana record id in the form of "/datasetid/recordid" (so with leading slash and without trailing slash)
     * @return record information in json format
     * @throws IIIFException (InvalidApiKeyException if the provide key is not valid,
     *      RecordNotFoundException if there was a 404, RecordRetrieveException on all other problems)
     */
    public String getRecordJson(String recordId, String wskey) throws IIIFException {
        String result= null;

        StringBuilder url = new StringBuilder(recordApiUrl);
        url.append(recordId);
        url.append(".json?wskey=");
        url.append(wskey);

        try {
            try (CloseableHttpResponse response = httpClient.execute(new HttpGet(url.toString()))) {
                int responseCode = response.getStatusLine().getStatusCode();
                LOG.debug("Record request: {}, status code = {}", recordId, responseCode);
                if (responseCode == HttpStatus.SC_UNAUTHORIZED) {
                    throw new InvalidApiKeyException("API key is not valid");
                } else if (responseCode == HttpStatus.SC_NOT_FOUND) {
                    throw new RecordNotFoundException("Record with id '"+recordId+"' not found");
                } else if (responseCode != HttpStatus.SC_OK) {
                    throw new RecordRetrieveException("Error retrieving record: "+response.getStatusLine().getReasonPhrase());
                }

                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    result = EntityUtils.toString(entity);
                    LOG.debug("Record request: {}, response = {}", recordId, result);
                    EntityUtils.consume(entity); // make sure entity is consumed fully so connection can be reused
                } else {
                    LOG.warn("Request entity = null");
                }
            }
        } catch (IOException e) {
            throw new RecordRetrieveException("Error retrieving record", e);
        }


        return result;
    }

    /**
     * Generates a manifest object for IIIF v2 filled with data that is extracted from the provided JSON
     * @param json record data in JSON format
     * @return Manifest v2 object
     */
    public ManifestV2 generateManifestV2 (String json)  {
        long start = System.currentTimeMillis();
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(json);
        ManifestV2 result = EdmManifestMapping.getManifestV2(document);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Generated in {} ms", (System.currentTimeMillis() - start));
        }
        return result;
    }

    /**
     * Generates a manifest object for IIIF v3 filled with data that is extracted from the provided JSON
     * @param json record data in JSON format
     * @return Manifest v3 object
     */
    public ManifestV3 generateManifestV3 (String json)  {
        long start = System.currentTimeMillis();
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(json);
        ManifestV3 result = EdmManifestMapping.getManifestV3(document);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Generated in {} ms ", (System.currentTimeMillis() - start));
        }
        return result;
    }


    // TODO insert context when serializing!? (or do we redefine it?)
    // perhaps we can use https://github.com/io-informatics/jackson-jsonld or
    // https://github.com/kbss-cvut/jb4jsonld-jackson

   /**
     * Serialize manifest to JSON-LD
     * @param m manifest
     * @return JSON-LD string
     * @throws RecordParseException when there is a problem parsing
     */
    public String serializeManifest(Object m) throws RecordParseException {
        try {
            return mapper.
                    writerWithDefaultPrettyPrinter().
                    writeValueAsString(m);
        }
        catch (IOException e) {
            throw new RecordParseException("Error serializing data: "+e.getMessage(), e);
        }

    }

    /**
     * Main method for testing/debugging purposes only
     * @param args
     */
    public static void main(String[] args) {
        ManifestService s = new ManifestService();
        //String json = s.getRecordJson("/9200356/BibliographicResource_3000118390149");
        String json = "{\"apikey\":\"api2demo\",\"success\":true,\"statsDuration\":295,\"requestNumber\":999,\"object\":{\"title\":[\"Edasi - 1922-03-15\"],\"edmDatasetName\":[\"9200356_Ag_EU_TEL_a0616_Newspapers_Estonia\"],\"aggregations\":[{\"about\":\"/aggregation/provider/9200356/BibliographicResource_3000118390149\",\"edmDataProvider\":{\"def\":[\"National Library of Estonia\"]},\"edmIsShownBy\":\"http://iiif.europeana.eu/records/GGDNOQYY5N35KNXL7PZBCNRWDJN6RCWLCKN6XXPRD5632RSEEQIA/representations/presentation_images/versions/c7aaa970-fd11-11e5-bc8a-fa163e60dd72/files/node-3/image/NLE/Edasi/1922/03/15/1/19220315_1-0001/full/full/0/default.jpg\",\"edmObject\":\"http://iiif.europeana.eu/records/GGDNOQYY5N35KNXL7PZBCNRWDJN6RCWLCKN6XXPRD5632RSEEQIA/representations/presentation_images/versions/c7aaa970-fd11-11e5-bc8a-fa163e60dd72/files/node-3/image/NLE/Edasi/1922/03/15/1/19220315_1-0001/full/full/0/default.jpg\",\"edmProvider\":{\"en\":[\"The European Library\"]},\"edmRights\":{\"def\":[\"http://creativecommons.org/publicdomain/mark/1.0/\"]},\"hasView\":[\"http://iiif.europeana.eu/records/GGDNOQYY5N35KNXL7PZBCNRWDJN6RCWLCKN6XXPRD5632RSEEQIA/representations/presentation_images/versions/c7aaa970-fd11-11e5-bc8a-fa163e60dd72/files/node-3/image/NLE/Edasi/1922/03/15/1/19220315_1-0001/full/full/0/default.jpg\",\"http://iiif.europeana.eu/records/GGDNOQYY5N35KNXL7PZBCNRWDJN6RCWLCKN6XXPRD5632RSEEQIA/representations/presentation_images/versions/c7aaa970-fd11-11e5-bc8a-fa163e60dd72/files/node-3/image/NLE/Edasi/1922/03/15/1/19220315_1-0002/full/full/0/default.jpg\",\"http://iiif.europeana.eu/records/GGDNOQYY5N35KNXL7PZBCNRWDJN6RCWLCKN6XXPRD5632RSEEQIA/representations/presentation_images/versions/c7aaa970-fd11-11e5-bc8a-fa163e60dd72/files/node-3/image/NLE/Edasi/1922/03/15/1/19220315_1-0003/full/full/0/default.jpg\",\"http://iiif.europeana.eu/records/GGDNOQYY5N35KNXL7PZBCNRWDJN6RCWLCKN6XXPRD5632RSEEQIA/representations/presentation_images/versions/c7aaa970-fd11-11e5-bc8a-fa163e60dd72/files/node-3/image/NLE/Edasi/1922/03/15/1/19220315_1-0004/full/full/0/default.jpg\"],\"aggregatedCHO\":\"/item/9200356/BibliographicResource_3000118390149\",\"webResources\":[{\"about\":\"http://iiif.europeana.eu/records/GGDNOQYY5N35KNXL7PZBCNRWDJN6RCWLCKN6XXPRD5632RSEEQIA/representations/presentation_images/versions/c7aaa970-fd11-11e5-bc8a-fa163e60dd72/files/node-3/image/NLE/Edasi/1922/03/15/1/19220315_1-0001/full/full/0/default.jpg\",\"textAttributionSnippet\":\"Edasi - 1922-03-15 - http://europeana.eu/portal/record/9200356/BibliographicResource_3000118390149.html. National Library of Estonia. Public Domain - http://creativecommons.org/publicdomain/mark/1.0/\",\"htmlAttributionSnippet\":\"<span about='http://data.europeana.eu/item/9200356/BibliographicResource_3000118390149'><a href='http://europeana.eu/portal/record/9200356/BibliographicResource_3000118390149.html'><span property='dc:title'>Edasi - 1922-03-15</span></a>. National Library of Estonia. <a href='http://creativecommons.org/publicdomain/mark/1.0/' rel='xhv:license http://www.europeana.eu/schemas/edm/rights'>Public Domain</a><span rel='cc:useGuidelines' resource='http://www.europeana.eu/rights/pd-usage-guide/'>.</span></span>\",\"svcsHasService\":[\"http://iiif.europeana.eu/records/GGDNOQYY5N35KNXL7PZBCNRWDJN6RCWLCKN6XXPRD5632RSEEQIA/representations/presentation_images/versions/c7aaa970-fd11-11e5-bc8a-fa163e60dd72/files/node-3/image/NLE/Edasi/1922/03/15/1/19220315_1-0001\"]},{\"about\":\"http://iiif.europeana.eu/records/GGDNOQYY5N35KNXL7PZBCNRWDJN6RCWLCKN6XXPRD5632RSEEQIA/representations/presentation_images/versions/c7aaa970-fd11-11e5-bc8a-fa163e60dd72/files/node-3/image/NLE/Edasi/1922/03/15/1/19220315_1-0002/full/full/0/default.jpg\",\"isNextInSequence\":\"http://iiif.europeana.eu/records/GGDNOQYY5N35KNXL7PZBCNRWDJN6RCWLCKN6XXPRD5632RSEEQIA/representations/presentation_images/versions/c7aaa970-fd11-11e5-bc8a-fa163e60dd72/files/node-3/image/NLE/Edasi/1922/03/15/1/19220315_1-0001/full/full/0/default.jpg\",\"textAttributionSnippet\":\"Edasi - 1922-03-15 - http://europeana.eu/portal/record/9200356/BibliographicResource_3000118390149.html. National Library of Estonia. Public Domain - http://creativecommons.org/publicdomain/mark/1.0/\",\"htmlAttributionSnippet\":\"<span about='http://data.europeana.eu/item/9200356/BibliographicResource_3000118390149'><a href='http://europeana.eu/portal/record/9200356/BibliographicResource_3000118390149.html'><span property='dc:title'>Edasi - 1922-03-15</span></a>. National Library of Estonia. <a href='http://creativecommons.org/publicdomain/mark/1.0/' rel='xhv:license http://www.europeana.eu/schemas/edm/rights'>Public Domain</a><span rel='cc:useGuidelines' resource='http://www.europeana.eu/rights/pd-usage-guide/'>.</span></span>\",\"svcsHasService\":[\"http://iiif.europeana.eu/records/GGDNOQYY5N35KNXL7PZBCNRWDJN6RCWLCKN6XXPRD5632RSEEQIA/representations/presentation_images/versions/c7aaa970-fd11-11e5-bc8a-fa163e60dd72/files/node-3/image/NLE/Edasi/1922/03/15/1/19220315_1-0002\"]},{\"about\":\"http://iiif.europeana.eu/records/GGDNOQYY5N35KNXL7PZBCNRWDJN6RCWLCKN6XXPRD5632RSEEQIA/representations/presentation_images/versions/c7aaa970-fd11-11e5-bc8a-fa163e60dd72/files/node-3/image/NLE/Edasi/1922/03/15/1/19220315_1-0003/full/full/0/default.jpg\",\"isNextInSequence\":\"http://iiif.europeana.eu/records/GGDNOQYY5N35KNXL7PZBCNRWDJN6RCWLCKN6XXPRD5632RSEEQIA/representations/presentation_images/versions/c7aaa970-fd11-11e5-bc8a-fa163e60dd72/files/node-3/image/NLE/Edasi/1922/03/15/1/19220315_1-0002/full/full/0/default.jpg\",\"textAttributionSnippet\":\"Edasi - 1922-03-15 - http://europeana.eu/portal/record/9200356/BibliographicResource_3000118390149.html. National Library of Estonia. Public Domain - http://creativecommons.org/publicdomain/mark/1.0/\",\"htmlAttributionSnippet\":\"<span about='http://data.europeana.eu/item/9200356/BibliographicResource_3000118390149'><a href='http://europeana.eu/portal/record/9200356/BibliographicResource_3000118390149.html'><span property='dc:title'>Edasi - 1922-03-15</span></a>. National Library of Estonia. <a href='http://creativecommons.org/publicdomain/mark/1.0/' rel='xhv:license http://www.europeana.eu/schemas/edm/rights'>Public Domain</a><span rel='cc:useGuidelines' resource='http://www.europeana.eu/rights/pd-usage-guide/'>.</span></span>\",\"svcsHasService\":[\"http://iiif.europeana.eu/records/GGDNOQYY5N35KNXL7PZBCNRWDJN6RCWLCKN6XXPRD5632RSEEQIA/representations/presentation_images/versions/c7aaa970-fd11-11e5-bc8a-fa163e60dd72/files/node-3/image/NLE/Edasi/1922/03/15/1/19220315_1-0003\"]},{\"about\":\"http://iiif.europeana.eu/records/GGDNOQYY5N35KNXL7PZBCNRWDJN6RCWLCKN6XXPRD5632RSEEQIA/representations/presentation_images/versions/c7aaa970-fd11-11e5-bc8a-fa163e60dd72/files/node-3/image/NLE/Edasi/1922/03/15/1/19220315_1-0004/full/full/0/default.jpg\",\"isNextInSequence\":\"http://iiif.europeana.eu/records/GGDNOQYY5N35KNXL7PZBCNRWDJN6RCWLCKN6XXPRD5632RSEEQIA/representations/presentation_images/versions/c7aaa970-fd11-11e5-bc8a-fa163e60dd72/files/node-3/image/NLE/Edasi/1922/03/15/1/19220315_1-0003/full/full/0/default.jpg\",\"textAttributionSnippet\":\"Edasi - 1922-03-15 - http://europeana.eu/portal/record/9200356/BibliographicResource_3000118390149.html. National Library of Estonia. Public Domain - http://creativecommons.org/publicdomain/mark/1.0/\",\"htmlAttributionSnippet\":\"<span about='http://data.europeana.eu/item/9200356/BibliographicResource_3000118390149'><a href='http://europeana.eu/portal/record/9200356/BibliographicResource_3000118390149.html'><span property='dc:title'>Edasi - 1922-03-15</span></a>. National Library of Estonia. <a href='http://creativecommons.org/publicdomain/mark/1.0/' rel='xhv:license http://www.europeana.eu/schemas/edm/rights'>Public Domain</a><span rel='cc:useGuidelines' resource='http://www.europeana.eu/rights/pd-usage-guide/'>.</span></span>\",\"svcsHasService\":[\"http://iiif.europeana.eu/records/GGDNOQYY5N35KNXL7PZBCNRWDJN6RCWLCKN6XXPRD5632RSEEQIA/representations/presentation_images/versions/c7aaa970-fd11-11e5-bc8a-fa163e60dd72/files/node-3/image/NLE/Edasi/1922/03/15/1/19220315_1-0004\"]}],\"edmPreviewNoDistribute\":false}],\"about\":\"/9200356/BibliographicResource_3000118390149\",\"europeanaAggregation\":{\"about\":\"/aggregation/europeana/9200356/BibliographicResource_3000118390149\",\"aggregatedCHO\":\"/item/9200356/BibliographicResource_3000118390149\",\"edmLandingPage\":\"http://europeana.eu/portal/record/9200356/BibliographicResource_3000118390149.html\",\"edmCountry\":{\"def\":[\"estonia\"]},\"edmLanguage\":{\"def\":[\"et\"]},\"edmPreview\":\"http://europeanastatic.eu/api/image?uri=http%3A%2F%2Fiiif.europeana.eu%2Frecords%2FGGDNOQYY5N35KNXL7PZBCNRWDJN6RCWLCKN6XXPRD5632RSEEQIA%2Frepresentations%2Fpresentation_images%2Fversions%2Fc7aaa970-fd11-11e5-bc8a-fa163e60dd72%2Ffiles%2Fnode-3%2Fimage%2FNLE%2FEdasi%2F1922%2F03%2F15%2F1%2F19220315_1-0001%2Ffull%2Ffull%2F0%2Fdefault.jpg&size=LARGE&type=TEXT\"},\"proxies\":[{\"about\":\"/proxy/provider/9200356/BibliographicResource_3000118390149\",\"dcIdentifier\":{\"def\":[\"http://data.theeuropeanlibrary.org/BibliographicResource/3000118390149\"]},\"dcLanguage\":{\"def\":[\"et\"]},\"dcTitle\":{\"def\":[\"Edasi - 1922-03-15\"]},\"dcType\":{\"def\":[\"http://schema.org/PublicationIssue\"]},\"dctermsExtent\":{\"en\":[\"Pages: 4\"]},\"dctermsIsPartOf\":{\"def\":[\"http://data.theeuropeanLibrary.org/BibliographicResource/3000100340004\",\"http://data.theeuropeanlibrary.org/Collection/a0616\"],\"en\":[\"Europeana Newspapers\"]},\"dctermsIssued\":{\"def\":[\"1922-03-15\"]},\"edmIsNextInSequence\":[\"http://data.theeuropeanLibrary.org/BibliographicResource/3000118390042\"],\"proxyIn\":[\"/aggregation/provider/9200356/BibliographicResource_3000118390149\"],\"proxyFor\":\"/item/9200356/BibliographicResource_3000118390149\",\"edmType\":\"TEXT\",\"europeanaProxy\":false},{\"about\":\"/proxy/europeana/9200356/BibliographicResource_3000118390149\",\"proxyIn\":[\"/aggregation/europeana/9200356/BibliographicResource_3000118390149\"],\"proxyFor\":\"/item/9200356/BibliographicResource_3000118390149\",\"edmType\":\"TEXT\",\"europeanaProxy\":true}],\"language\":[\"et\"],\"europeanaCompleteness\":5,\"providedCHOs\":[{\"about\":\"/item/9200356/BibliographicResource_3000118390149\"}],\"europeanaCollectionName\":[\"9200356_Ag_EU_TEL_a0616_Newspapers_Estonia\"],\"services\":[{\"about\":\"http://iiif.europeana.eu/records/GGDNOQYY5N35KNXL7PZBCNRWDJN6RCWLCKN6XXPRD5632RSEEQIA/representations/presentation_images/versions/c7aaa970-fd11-11e5-bc8a-fa163e60dd72/files/node-3/image/NLE/Edasi/1922/03/15/1/19220315_1-0001\",\"id\":{\"timestamp\":1512120749,\"machineIdentifier\":14987444,\"processIdentifier\":-15654,\"counter\":7962431,\"timeSecond\":1512120749,\"time\":1512120749000,\"date\":1512120749000},\"dctermsConformsTo\":[\"http://iiif.io/api/image\"],\"doapImplements\":[\"http://iiif.io/api/image/2/level1.json\"]},{\"about\":\"http://iiif.europeana.eu/records/GGDNOQYY5N35KNXL7PZBCNRWDJN6RCWLCKN6XXPRD5632RSEEQIA/representations/presentation_images/versions/c7aaa970-fd11-11e5-bc8a-fa163e60dd72/files/node-3/image/NLE/Edasi/1922/03/15/1/19220315_1-0002\",\"id\":{\"timestamp\":1512120749,\"machineIdentifier\":14987444,\"processIdentifier\":-15654,\"counter\":7962432,\"timeSecond\":1512120749,\"time\":1512120749000,\"date\":1512120749000},\"dctermsConformsTo\":[\"http://iiif.io/api/image\"],\"doapImplements\":[\"http://iiif.io/api/image/2/level1.json\"]},{\"about\":\"http://iiif.europeana.eu/records/GGDNOQYY5N35KNXL7PZBCNRWDJN6RCWLCKN6XXPRD5632RSEEQIA/representations/presentation_images/versions/c7aaa970-fd11-11e5-bc8a-fa163e60dd72/files/node-3/image/NLE/Edasi/1922/03/15/1/19220315_1-0003\",\"id\":{\"timestamp\":1512120749,\"machineIdentifier\":14987444,\"processIdentifier\":-15654,\"counter\":7962433,\"timeSecond\":1512120749,\"time\":1512120749000,\"date\":1512120749000},\"dctermsConformsTo\":[\"http://iiif.io/api/image\"],\"doapImplements\":[\"http://iiif.io/api/image/2/level1.json\"]},{\"about\":\"http://iiif.europeana.eu/records/GGDNOQYY5N35KNXL7PZBCNRWDJN6RCWLCKN6XXPRD5632RSEEQIA/representations/presentation_images/versions/c7aaa970-fd11-11e5-bc8a-fa163e60dd72/files/node-3/image/NLE/Edasi/1922/03/15/1/19220315_1-0004\",\"id\":{\"timestamp\":1512120749,\"machineIdentifier\":14987444,\"processIdentifier\":-15654,\"counter\":7962434,\"timeSecond\":1512120749,\"time\":1512120749000,\"date\":1512120749000},\"dctermsConformsTo\":[\"http://iiif.io/api/image\"],\"doapImplements\":[\"http://iiif.io/api/image/2/level1.json\"]}],\"type\":\"TEXT\",\"timestamp_created_epoch\":1422220146248,\"timestamp_update_epoch\":1512120749767,\"timestamp_created\":\"2015-01-25T21:09:06.248Z\",\"timestamp_update\":\"2017-12-01T09:32:29.767Z\"}}";

        try {
            ManifestV2 m2 = s.generateManifestV2(json);
            LOG.debug("jsonld V2 = \n{}", s.serializeManifest(m2));

            //ManifestV3 m3 = s.generateManifestV3(json);
            //LOG.debug("jsonld V3 = \n{}", s.serializeManifest(m3));
        } catch (RecordParseException e) {
            LOG.error("Error generating manifest", e);
        }
    }
}
