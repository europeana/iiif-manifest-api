package eu.europeana.iiif.service;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider;
import com.jayway.jsonpath.spi.json.JsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import com.jayway.jsonpath.spi.mapper.MappingProvider;
import eu.europeana.iiif.model.Definitions;
import eu.europeana.iiif.model.v3.Collection;
import eu.europeana.iiif.model.v3.LanguageMap;
import eu.europeana.iiif.model.v3.Manifest;
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
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
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
                return EnumSet.noneOf(Option.class);
            }
        });

        // configure Jackson serialization
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        mapper.registerModule(new JsonldModule(loadJsonContext()));
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
     * Generats a manifest object filled with data that is extracted from te provided JSON
     * @param json record data in JSON format
     * @return Manifest object
     */
    public Manifest generateManifest (String json)  {
        Manifest result;

        long start = System.currentTimeMillis();
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(json);
        result = new Manifest(getIdUrl(getId(json)));
        result.within = getWithin(document);
        result.label = getLabel(document);

        LOG.debug("Generated in "+(System.currentTimeMillis()-start));

        return result;
    }

    protected String getId(String json) {
        return JsonPath.parse(json).read("$.object.about", String.class);
    }

    protected String getIdUrl(String id) {
        return Definitions.IIIF_MANIFESTID_BASE_UIRL +id+ Definitions.IIIF_MANIFESTID_POSTFIX;
    }

    protected Collection[] getWithin(Object json) {
        List<Collection> result = new ArrayList<>();
        // TODO I think we can incorporate the startsWith into the JsonPath read
        List<String> collections = JsonPath.parse(json).read("$.object.proxies[*].dctermsIsPartOf.def[*]", List.class);
        for (String collection : collections) {
            if (collection.toLowerCase().startsWith("http://data.theeuropeanlibrary.org") || collection.toLowerCase().startsWith("https://data.theeuropeanlibrary.org")) {
                result.add(new Collection(collection));
            }
        }
        return result.toArray(new Collection[result.size()]);
    }

    protected LanguageMap getLabel(Object json)  {
        LanguageMap[] maps = JsonPath.parse(json).read("$.object.proxies[*].dcTitle", LanguageMap[].class);
        if (maps.length == 0) {
            LOG.debug("No title, checking description");
            maps = JsonPath.parse(json).read("$.object.proxies[*].dcDescription", LanguageMap[].class);
        }
        // TODO what if there is no title and no description
        return maps[0];
    }

    // TODO insert context when serializing!
    // perhaps we can use https://github.com/io-informatics/jackson-jsonld or
    // https://github.com/kbss-cvut/jb4jsonld-jackson

    /**
     * Serialize manifest to JSON-LD
     * @param m manifest
     * @return JSON-LD string
     * @throws RecordParseException when there is a problem parsing
     */
    public String serializeManifest(Manifest m) throws RecordParseException {
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
     * Temporary main method for testing
     * @param args
     * @throws IIIFException
     */
    public static void main(String[] args) throws IIIFException {
        ManifestService s = new ManifestService();
        //String json = s.getRecordJson("/9200356/BibliographicResource_3000118390149");
        String json = "{\"apikey\":\"api2demo\",\"success\":true,\"statsDuration\":240,\"requestNumber\":999,\"object\":{\"edmDatasetName\":[\"9200356_Ag_EU_TEL_a0616_Newspapers_Estonia\"],\"title\":[\"Edasi - 1922-03-15\"],\"providedCHOs\":[{\"about\":\"/item/9200356/BibliographicResource_3000118390149\"}],\"aggregations\":[{\"about\":\"/aggregation/provider/9200356/BibliographicResource_3000118390149\",\"edmDataProvider\":{\"def\":[\"National Library of Estonia\"]},\"edmIsShownBy\":\"http://www.theeuropeanlibrary.org/tel4/newspapers/issue/fullscreen/3000118390149\",\"edmIsShownAt\":\"http://www.europeana.eu/api/api2demo/redirect?shownAt=http%3A%2F%2Fwww.theeuropeanlibrary.org%2Ftel4%2Fnewspapers%2Fissue%2F3000118390149&provider=The+European+Library&id=http%3A%2F%2Fwww.europeana.eu%2Fresolve%2Frecord%2F9200356%2FBibliographicResource_3000118390149&profile=full\",\"edmObject\":\"http://port2.theeuropeanlibrary.org/fcgi-bin/iipsrv2.fcgi?FIF=node-3/image/NLE/Edasi/1922/03/15/1/19220315_1-0001.jp2&wid=200&cvt=jpg\",\"edmProvider\":{\"en\":[\"The European Library\"]},\"edmRights\":{\"def\":[\"http://creativecommons.org/publicdomain/mark/1.0/\"]},\"edmUgc\":\"false\",\"aggregatedCHO\":\"/item/9200356/BibliographicResource_3000118390149\",\"webResources\":[{\"webResourceEdmRights\":{\"def\":[\"http://creativecommons.org/publicdomain/mark/1.0/\"]},\"about\":\"http://www.theeuropeanlibrary.org/tel4/newspapers/issue/3000118390149\",\"textAttributionSnippet\":\"Edasi - 1922-03-15 - http://europeana.eu/portal/record/9200356/BibliographicResource_3000118390149.html. National Library of Estonia - http://www.theeuropeanlibrary.org/tel4/newspapers/issue/3000118390149. Public Domain - http://creativecommons.org/publicdomain/mark/1.0/\",\"htmlAttributionSnippet\":\"<span about='http://data.europeana.eu/item/9200356/BibliographicResource_3000118390149'><a href='http://europeana.eu/portal/record/9200356/BibliographicResource_3000118390149.html'><span property='dc:title'>Edasi - 1922-03-15</span></a>. <a href='http://www.theeuropeanlibrary.org/tel4/newspapers/issue/3000118390149'>National Library of Estonia</a>. <a href='http://creativecommons.org/publicdomain/mark/1.0/' rel='xhv:license http://www.europeana.eu/schemas/edm/rights'>Public Domain</a><span rel='cc:useGuidelines' resource='http://www.europeana.eu/rights/pd-usage-guide/'>.</span></span>\"},{\"webResourceEdmRights\":{\"def\":[\"http://creativecommons.org/publicdomain/mark/1.0/\"]},\"about\":\"http://www.theeuropeanlibrary.org/tel4/newspapers/issue/fullscreen/3000118390149\",\"textAttributionSnippet\":\"Edasi - 1922-03-15 - http://europeana.eu/portal/record/9200356/BibliographicResource_3000118390149.html. National Library of Estonia - http://www.theeuropeanlibrary.org/tel4/newspapers/issue/3000118390149. Public Domain - http://creativecommons.org/publicdomain/mark/1.0/\",\"htmlAttributionSnippet\":\"<span about='http://data.europeana.eu/item/9200356/BibliographicResource_3000118390149'><a href='http://europeana.eu/portal/record/9200356/BibliographicResource_3000118390149.html'><span property='dc:title'>Edasi - 1922-03-15</span></a>. <a href='http://www.theeuropeanlibrary.org/tel4/newspapers/issue/3000118390149'>National Library of Estonia</a>. <a href='http://creativecommons.org/publicdomain/mark/1.0/' rel='xhv:license http://www.europeana.eu/schemas/edm/rights'>Public Domain</a><span rel='cc:useGuidelines' resource='http://www.europeana.eu/rights/pd-usage-guide/'>.</span></span>\"},{\"webResourceEdmRights\":{\"def\":[\"http://creativecommons.org/publicdomain/mark/1.0/\"]},\"about\":\"http://port2.theeuropeanlibrary.org/fcgi-bin/iipsrv2.fcgi?FIF=node-3/image/NLE/Edasi/1922/03/15/1/19220315_1-0001.jp2&wid=200&cvt=jpg\",\"textAttributionSnippet\":\"Edasi - 1922-03-15 - http://europeana.eu/portal/record/9200356/BibliographicResource_3000118390149.html. National Library of Estonia - http://www.theeuropeanlibrary.org/tel4/newspapers/issue/3000118390149. Public Domain - http://creativecommons.org/publicdomain/mark/1.0/\",\"htmlAttributionSnippet\":\"<span about='http://data.europeana.eu/item/9200356/BibliographicResource_3000118390149'><a href='http://europeana.eu/portal/record/9200356/BibliographicResource_3000118390149.html'><span property='dc:title'>Edasi - 1922-03-15</span></a>. <a href='http://www.theeuropeanlibrary.org/tel4/newspapers/issue/3000118390149'>National Library of Estonia</a>. <a href='http://creativecommons.org/publicdomain/mark/1.0/' rel='xhv:license http://www.europeana.eu/schemas/edm/rights'>Public Domain</a><span rel='cc:useGuidelines' resource='http://www.europeana.eu/rights/pd-usage-guide/'>.</span></span>\"}],\"edmPreviewNoDistribute\":false}],\"about\":\"/9200356/BibliographicResource_3000118390149\",\"europeanaAggregation\":{\"about\":\"/aggregation/europeana/9200356/BibliographicResource_3000118390149\",\"aggregatedCHO\":\"/item/9200356/BibliographicResource_3000118390149\",\"edmLandingPage\":\"http://europeana.eu/portal/record/9200356/BibliographicResource_3000118390149.html\",\"edmCountry\":{\"def\":[\"estonia\"]},\"edmLanguage\":{\"def\":[\"et\"]},\"edmRights\":{\"def\":[\"http://creativecommons.org/publicdomain/mark/1.0/\"]},\"edmPreview\":\"https://www.europeana.eu/api/v2/thumbnail-by-url.json?uri=http%3A%2F%2Fport2.theeuropeanlibrary.org%2Ffcgi-bin%2Fiipsrv2.fcgi%3FFIF%3Dnode-3%2Fimage%2FNLE%2FEdasi%2F1922%2F03%2F15%2F1%2F19220315_1-0001.jp2%26wid%3D200%26cvt%3Djpg&size=LARGE&type=TEXT\"},\"proxies\":[{\"about\":\"/proxy/provider/9200356/BibliographicResource_3000118390149\",\"dcDate\":{\"def\":[\"1922\"]},\"dcIdentifier\":{\"def\":[\"http://data.theeuropeanlibrary.org/BibliographicResource/3000118390149\"]},\"dcLanguage\":{\"def\":[\"et\"]},\"dcTitle\":{\"def\":[\"Edasi - 1922-03-15\"]},\"dcType\":{\"def\":[\"http://vocab.getty.edu/aat/300026656\",\"Newspaper Issue\"],\"en\":[\"Analytic serial\"]},\"dctermsExtent\":{\"en\":[\"Pages: 4\"]},\"dctermsIsPartOf\":{\"def\":[\"http://data.theeuropeanlibrary.org/BibliographicResource/3000100340004\",\"http://data.theeuropeanlibrary.org/Collection/a0616\"],\"en\":[\"Europeana Newspapers\"]},\"dctermsIssued\":{\"def\":[\"1922-03-15\"]},\"edmIsNextInSequence\":[\"http://data.theeuropeanlibrary.org/BibliographicResource/3000118390042\"],\"proxyIn\":[\"/aggregation/provider/9200356/BibliographicResource_3000118390149\"],\"proxyFor\":\"/item/9200356/BibliographicResource_3000118390149\",\"edmType\":\"TEXT\",\"europeanaProxy\":false},{\"about\":\"/proxy/europeana/9200356/BibliographicResource_3000118390149\",\"dcDate\":{\"def\":[\"http://semium.org/time/19xx_1_third\",\"http://semium.org/time/1922\"]},\"dctermsTemporal\":{\"def\":[\"http://semium.org/time/19xx\"]},\"proxyIn\":[\"/aggregation/europeana/9200356/BibliographicResource_3000118390149\"],\"proxyFor\":\"/item/9200356/BibliographicResource_3000118390149\",\"edmType\":\"TEXT\",\"year\":{\"def\":[\"1922\"]},\"europeanaProxy\":true}],\"language\":[\"et\"],\"timespans\":[{\"about\":\"http://semium.org/time/19xx_1_third\",\"prefLabel\":{\"ru\":[\"Начало 20-го века\"],\"en\":[\"Early 20th century\"]},\"begin\":{\"def\":[\"Tue Jan 01 00:19:32 CET 1901\"]},\"end\":{\"def\":[\"Sun Dec 31 00:19:32 CET 1933\"]},\"isPartOf\":{\"def\":[\"http://semium.org/time/19xx\"]}},{\"about\":\"http://semium.org/time/1922\",\"prefLabel\":{\"def\":[\"1922\"]},\"begin\":{\"def\":[\"Sun Jan 01 00:19:32 CET 1922\"]},\"end\":{\"def\":[\"Sun Dec 31 00:19:32 CET 1922\"]},\"isPartOf\":{\"def\":[\"http://semium.org/time/19xx_1_third\"]}},{\"about\":\"http://semium.org/time/AD2xxx\",\"prefLabel\":{\"en\":[\"Second millenium AD\",\"Second millenium AD, years 1001-2000\"],\"fr\":[\"2e millénaire après J.-C.\"]},\"isPartOf\":{\"def\":[\"http://semium.org/time/ChronologicalPeriod\"]}},{\"about\":\"http://semium.org/time/ChronologicalPeriod\",\"prefLabel\":{\"en\":[\"Chronological period\"]},\"isPartOf\":{\"def\":[\"http://semium.org/time/Time\"]}},{\"about\":\"http://semium.org/time/19xx\",\"prefLabel\":{\"ru\":[\"20й век\"],\"def\":[\"20..\",\"20??\",\"20e\"],\"en\":[\"20-th\",\"20th\",\"20th century\"],\"fr\":[\"20e siècle\"],\"nl\":[\"20de eeuw\"]},\"begin\":{\"def\":[\"Tue Jan 01 00:19:32 CET 1901\"]},\"end\":{\"def\":[\"Sun Dec 31 01:00:00 CET 2000\"]},\"isPartOf\":{\"def\":[\"http://semium.org/time/AD2xxx\"]}},{\"about\":\"http://semium.org/time/Time\",\"prefLabel\":{\"en\":[\"Time\"]}}],\"europeanaCompleteness\":5,\"europeanaCollectionName\":[\"9200356_Ag_EU_TEL_a0616_Newspapers_Estonia\"],\"concepts\":[{\"about\":\"http://vocab.getty.edu/aat/300026656\",\"prefLabel\":{\"de\":[\"Zeitung\"],\"en\":[\"newspapers\"],\"es\":[\"diarios (noticias)\"],\"nl\":[\"kranten\"]},\"altLabel\":{\"de\":[\"Tageblätter\"],\"sv\":[\"tidning\"],\"en\":[\"newspaper\"],\"it\":[\"giornale (newspaper)\"],\"fr\":[\"journal (newspaper)\"],\"nl\":[\"nieuwsbladen\"],\"es\":[\"diario (noticias)\"]},\"note\":{\"def\":[\"http://vocab.getty.edu/aat/rev/5002531933\"]},\"broader\":[\"http://vocab.getty.edu/aat/300026642\"],\"narrower\":[\"http://vocab.getty.edu/aat/300404424\"]}],\"year\":[\"1922\"],\"type\":\"TEXT\",\"timestamp_created_epoch\":1422220146248,\"timestamp_update_epoch\":1462984526654,\"timestamp_update\":\"2016-05-11T16:35:26.654Z\",\"timestamp_created\":\"2015-01-25T21:09:06.248Z\"}}\n";
        Manifest m = s.generateManifest(json);
        LOG.debug("jsonld = \n{}", s.serializeManifest(m));

    }
}
