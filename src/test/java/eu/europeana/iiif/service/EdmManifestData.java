package eu.europeana.iiif.service;

/**
 * Made up record data used for testing EdmManifestMapping class
 * @author Patrick Ehlert
 * Created on 19-06-2019
 */
public class EdmManifestData {

    public static final String TEST_EMPTY = "{\"object\": { \"proxies\":[], \"europeanaAggregation\":{}, \"aggregations\":[] }}";
    
    public static final String TEST_ID = "{\"object\": {\"about\":\"id\"}}";
    
    public static final String TEST_WITHIN = "{\"object\": { \"proxies\":[{\"dctermsIsPartOf\":" +
            "{\"def\":[\"http://data.europeana.eu\", \"https://data.theeuropeanlibrary.org/someurl\"], " +
            " \"en\":[\"Europeana Newspapers\"]}" +
            "}]}}";
    
    public static final String TEST_TITLE = "{\"object\": { \"proxies\":[{}, { \"dcTitle\":{\"en\":[\"Title\"]} }, {} ] }}";
    public static final String TEST_DESCRIPTION = "{\"object\": { \"proxies\":[{}, { \"dcDescription\":{\"def\":[\"Description\"]} }, {}] }}";
    public static final String TEST_TITLE_DESCRIPTION = "{\"object\": { \"proxies\":[ {\"dcTitle\":{\"en\":[\"Title\"]} }, { \"dcDescription\":{\"def\":[\"Description\"]} }, {}] }}";
    
    public static final String TEST_METADATA_SIMPLE = "{\"object\": { \"proxies\":[{ \"dcType\":{\"nl\":[\"Precies mijn type\"]}}, { \"dcFormat\":{\"def\":[\"SomeFormat\"]} }, {}," +
                                                                                  "{ \"dcType\":{\"en\":[\"Exactly my type as well\"]}} ] }}";

    public static final String TEST_METADATA_COMPLICATED = "{\"object\": { \"proxies\":[{ \"dcSource\":{\"def\":[\"http://data.europeana.eu/place/base/203206\"]}}, { \"dcFormat\":{\"en\":[\"SomeFormat\"]} }, {}," +
                                                                                       "{ \"dcSource\":{\"def\" :[\"May the source be with you\", \"https://some.url\"]}} ], " +
                                                                          "\"concepts\":  [{ \"about\":\"http://data.europeana.eu/place/base/203206\", " +
                                                                                            "\"prefLabel\":{\"be\":[\"Bierbeek\"], \"bg\": [\"Бийрбек\"], \"zh\":[\"比尔贝克\"] }}]," +
                                                                          "\"timespans\": [{ \"about\":\"https://some.url\", " +
                                                                                            "\"prefLabel\":{\"en\":[\"Just a test\"] }}]" + "}}";
    
    public static final String TEST_THUMBNAIL_ID = "https://www.europeana.eu/api/v2/thumbnail-by-url.json?uri=test&size=LARGE&type=IMAGE";
    public static final String TEST_THUMBNAIL = "{\"object\": {\"europeanaAggregation\" :{ \"edmPreview\":\""+TEST_THUMBNAIL_ID+"\"}}}";

    public static final String TEST_CANVAS_THUMBNAIL_ID = "https://www.museumap.hu/media-provider-webapp/rest/file/preview/solr/oai-aggregated-bib9568907?mediaId=323995&size=masterview&tenant_id=Museumap&defaultImage=true";
    
    public static final String TEST_NAVDATE = "{\"object\": {\"proxies\":[{}, {\"dctermsIssued\":{\"en\":[\"NOT A REAL DATE\"]}}, {\"dctermsIssued\":{\"def\":[\"1922-03-15\"]}} ]}}";

    public static final String TEST_HOMEPAGE_ID = "https://www.europeana.eu/record/x/y.html";
    public static final String TEST_HOMEPAGE = "{\"object\": {\"europeanaAggregation\" :{ \"edmLandingPage\":\""+TEST_HOMEPAGE_ID+"\"}}}";

    public static final String TEST_IS_SHOWN_BY = "https://test.europeana.eu/test.jpg";

    public static final String TEST_ATTRIBUTION_TEXT_V2 = "some attribution text";
    public static final String TEST_ATTRIBUTION_TEXT_V3 = "<span>some attribution text</span>";
    public static final String TEST_ATTRIBUTION = "{\"object\": {\"aggregations\":[{ \"edmIsShownBy\":\""+TEST_IS_SHOWN_BY+"\"}, "+
            " {\"webResources\":[{\"about\":\"http://dont.pick/me.jpg\", \"textAttributionSnippet\":\"attributionTextFalse\", " +
                                                                        "\"htmlAttributionSnippet\":\"<span>attributionTextFalse</span>\"}," +
            "{\"about\":\""+TEST_IS_SHOWN_BY+"\", \"textAttributionSnippet\":\""+TEST_ATTRIBUTION_TEXT_V2+"\", " +
                                                 "\"htmlAttributionSnippet\":\""+TEST_ATTRIBUTION_TEXT_V3+"\"}" +"]}]}}";
    
    public static final String TEST_LICENSE_EUROPEANAAGGREGATION = "{\"object\": { \"aggregations\": [{\"edmRights\": { \"en\": [\"licenseTextAggregation\"]}}], \"europeanaAggregation\" : {\"edmRights\": { \"en\": [\"licenseTextEuropeana\"]}}}}";
    public static final String TEST_LICENSE_OTHERAGGREGATION = "{\"object\": {\"proxies\":[{\"about\":\"/proxy/provider/testing\",\"proxyIn\":[\"/aggregation/provider/testing\"]}], \"europeanaAggregation\" : {\"edmRights\":{}}, \"aggregations\": [{}, {\"about\":\"/aggregation/provider/testing\",\"edmRights\": { \"en\": [\"licenseTextAggregation\"]}}] }}";
    public static final String TEST_LICENSE_MULTIPLE_PROXY_AGGREGATION =  "{\"object\": { \"europeanaAggregation\" : {\"edmRights\":{}}, \"aggregations\": [{\"about\": \"/aggregation/provider/test\",\"edmIsShownBy\": \"http://repos.europeanafashion.eu/gdcdpp/images/00000004N.jpg\",\"edmRights\": {\"def\": [\"http://test.org/test/\"]}},{\"about\": \"/aggregation/aggregator/1/\",\"edmIsShownBy\": \"http://repos.europeanafashion.eu/gdcdpp/images/00000004N.jpg\"}],\"proxies\": [{\"about\": \"/proxy/europeana/1/\",\"proxyIn\": [\"/aggregation/europeana/1/\"],\"lineage\": [\"/proxy/provider/1/\",\"/proxy/aggregator/1/\"],\"europeanaProxy\": true},{\"about\": \"/proxy/aggregator/1/\",\"proxyIn\": [\"/aggregation/aggregator/1/\"],\"lineage\": [\"/proxy/provider/1/\"],\"edmType\": \"IMAGE\",\"europeanaProxy\": false},{\"about\": \"/proxy/provider/1/\",\"proxyIn\": [\"/aggregation/provider/test\"],\"proxyFor\": \"/item/1/\",\"edmType\": \"IMAGE\",\"europeanaProxy\": false}]}}";

    public static final String TEST_SEQUENCE_3CANVAS_1SERVICE = "{\"object\": { \"proxies\":[{\"about\":\"/proxy/provider/testing\",\"proxyIn\":[\"/aggregation/provider/testing\"]}]," +
            "\"aggregations\": [ {\"about\":\"/aggregation/provider/testing\",\"edmIsShownBy\": \"wr3Id\", \"hasView\": [\"wr2Id\"], \"webResources\": [ "+
            "{\"about\": \"wr1Id\", \"textAttributionSnippet\": \"wr1Attribution\" , \"webResourceEdmRights\":"+
            "{\"def\":[\"wr1License\"]}, \"ebucoreHasMimeType\": \"image/jpeg\" },"+
            "{\"about\": \"wr2Id\", \"textAttributionSnippet\": \"wr2Attribution\", \"htmlAttributionSnippet\": \"<span>wr2Attribution</span>\", \"webResourceEdmRights\":"+
            "{\"def\":[\"wr2License\"]}, \"ebucoreHasMimeType\": \"audio/mp4\" },"+
            "{\"about\": \"wr3Id\", \"textAttributionSnippet\": \"wr3Attribution\", \"htmlAttributionSnippet\": \"<span>wr3Attribution</span>\", \"webResourceEdmRights\":"+
            "{\"def\":[\"wr3License\"]}, \"ebucoreHasMimeType\": \"image/webp\", \"svcsHasService\": [\"service3Id\"], \"ebucoreDuration\": \"98765\"  }"+
            "] } ], \"services\": [{\"about\": \"service3Id\", \"doapImplements\": [\"serviceProfile\"]}] } }";

    public static final String TEST_SEQUENCE_1CANVAS_THUMB = "{\"object\": { \"proxies\":[{\"about\":\"/proxy/provider/testing\",\"proxyIn\":[\"/aggregation/provider/testing\"]}]," +
            "\"aggregations\": [ {\"about\":\"/aggregation/provider/testing\",\"edmIsShownBy\": \"wr2Id\", " +
            "\"hasView\": [\"" + TEST_CANVAS_THUMBNAIL_ID + "\"], " +
            "\"webResources\": [ "+
            "{\"about\": \"" + TEST_CANVAS_THUMBNAIL_ID + "\", " +
            "\"textAttributionSnippet\": \"wr1Attribution\" , \"webResourceEdmRights\":"+
            "{\"def\":[\"wr1License\"]}, \"ebucoreHasMimeType\": \"image/jpeg\" },"+
            "{\"about\": \"wr2Id\", \"textAttributionSnippet\": \"wr2Attribution\", \"htmlAttributionSnippet\": \"<span>wr2Attribution</span>\", \"webResourceEdmRights\":"+
            "{\"def\":[\"wr2License\"]}, \"ebucoreHasMimeType\": \"image/webp\", \"svcsHasService\": [\"service2Id\"], \"ebucoreDuration\": \"98765\"  }"+
            "] } ], \"services\": [{\"about\": \"service2Id\", \"doapImplements\": [\"serviceProfile\"]}] } }";


    public static final String TEST_SEQUENCE_MULTIPLE_PROXY_AGG = "{ \"object\" : { \"aggregations\": [" +
            "{\"about\": \"/aggregation/provider/1/\", \"edmIsShownBy\": \"provider_edmIsShownBy\", \"hasView\": [\"wr2Id\"]," +
            "\"webResources\": [{\"about\": \"wr1Id\", \"textAttributionSnippet\": \"wr1Attribution\" , \"webResourceEdmRights\":{\"def\":[\"wr1License\"]}, \"ebucoreHasMimeType\": \"image/webp\"}," +
            "{\"about\": \"wr2Id\", \"textAttributionSnippet\": \"wr2Attribution\", \"htmlAttributionSnippet\": \"<span>wr2Attribution</span>\", \"webResourceEdmRights\": {\"def\":[\"wr2License\"]}, \"ebucoreHasMimeType\": \"image/webp\" }," +
            "{\"about\": \"wr3Id\", \"textAttributionSnippet\": \"wr3Attribution\", \"htmlAttributionSnippet\": \"<span>wr3Attribution</span>\", \"webResourceEdmRights\":{\"def\":[\"wr3License\"]}, \"ebucoreHasMimeType\": \"video/mp4\", \"svcsHasService\": [\"service3Id\"], \"ebucoreDuration\": \"98765\"  }" +
            "] }," +
            "{\"about\": \"/aggregation/aggregator/1/europeana_fashion_00000004\",\"edmIsShownBy\": \"SecondAggregation_edmIsShowByTest\"}]," +
            "\"proxies\": [{\"about\": \"/proxy/europeana/1/\",\"proxyIn\": [\"/aggregation/europeana/1/europeana_fashion_00000004\"],\"lineage\": [\"/proxy/provider/1/\",\"/proxy/aggregator/1/\"],\"europeanaProxy\": true}," +
            "{\"about\": \"/proxy/aggregator/1/\",\"proxyIn\": [\"/aggregation/aggregator/1/europeana_fashion_00000004\"],\"lineage\": [\"/proxy/provider/1/europeana_fashion_00000004\"],\"edmType\": \"IMAGE\",\"europeanaProxy\": false}," +
            "{\"about\": \"/proxy/provider/1/\",\"proxyIn\": [\"/aggregation/provider/1/\"],\"proxyFor\": \"/item/1/europeana_fashion_00000004\",\"edmType\": \"IMAGE\",\"europeanaProxy\": false}] }}";

    public static final String MULTIPLE_PROXY_IN = "{\"object\": { \"proxies\": [{\"about\": \"/proxy/europeana/1/\",\"proxyIn\": [\"/aggregation/europeana/1/europeana_fashion_00000004\"],\"lineage\": [\"/proxy/provider/1/\",\"/proxy/aggregator/1/\"],\"europeanaProxy\": true}," +
            "{\"about\": \"/proxy/aggregator/1/\",\"proxyIn\": [\"/aggregation/aggregator/1/europeana_fashion_00000004\"],\"lineage\": [\"/proxy/provider/1/europeana_fashion_00000004\"],\"edmType\": \"IMAGE\",\"europeanaProxy\": false}," +
            "{\"about\": \"/proxy/provider/1/\",\"proxyIn\": [\"/first/provider/test\",\"second proxyIn value\"],\"proxyFor\": \"/item/1/europeana_fashion_00000004\",\"edmType\": \"IMAGE\",\"europeanaProxy\": false}]}}";

    public static final String TEST_SEQUENCE_3CANVAS_NOISSHOWNBY = "{\"object\": { \"proxies\":[{\"about\":\"/proxy/provider/testing\",\"proxyIn\":[\"/aggregation/provider/testing\"]}]," +
            "\"aggregations\": [ {\"hasView\": [\"wr2Id\"], \"webResources\": [ "+
            "{\"about\": \"wr1Id\", \"textAttributionSnippet\": \"wr1Attribution\" , \"webResourceEdmRights\":"+
            "{\"def\":[\"wr1License\"]}, \"ebucoreHasMimeType\": \"image/jpeg\" },"+
            "{\"about\": \"wr2Id\", \"textAttributionSnippet\": \"wr2Attribution\", \"htmlAttributionSnippet\": \"<span>wr2Attribution</span>\", \"webResourceEdmRights\":"+
            "{\"def\":[\"wr2License\"]}, \"ebucoreHasMimeType\": \"image/jpeg\" },"+
            "{\"about\": \"wr3Id\", \"textAttributionSnippet\": \"wr3Attribution\", \"htmlAttributionSnippet\": \"<span>wr3Attribution</span>\", \"webResourceEdmRights\":"+
            "{\"def\":[\"wr3License\"]}, \"ebucoreHasMimeType\": \"image/jpeg\", \"svcsHasService\": [\"service3Id\"], \"ebucoreDuration\": \"98765\"  }"+
            "] } ], \"services\": [{\"about\": \"service3Id\", \"doapImplements\": [\"serviceProfile\"]}] } }";

    public static final String TEST_SEQUENCE_2CANVAS_NOISSHOWNBY = "{\"object\": { \"aggregations\": [ { \"webResources\": [ "+
            "{\"about\": \"wr1Id\", \"textAttributionSnippet\": \"wr1Attribution\", \"webResourceEdmRights\":"+
            "{\"def\":[\"wr1License\"]}, \"ebucoreHasMimeType\": \"image/jpeg\", \"svcsHasService\": [\"service1Id\"]  },"+
            "{\"about\": \"wr2Id\", \"textAttributionSnippet\": \"wr2Attribution\", \"webResourceEdmRights\":"+
            "{\"def\":[\"wr2License\"]}, \"ebucoreHasMimeType\": \"image/jpeg\", \"svcsHasService\": [\"service2Id\"]  }"+
            "] } ], \"services\": [{\"about\": \"service1Id\", \"doapImplements\": [\"serviceProfile\"]}] } }";

    public static final String CANVAS_THUMBNAIL_ENCODED_URL = "https%3A%2F%2Fwww.museumap.hu%2Fmedia-provider-webapp%2Frest%2Ffile%2Fpreview%2Fsolr%2Foai-aggregated-bib9568907%3FmediaId%3D323995%26size%3Dmasterview%26tenant_id%3DMuseumap%26defaultImage%3Dtrue";

    public static final String CANVAS_THUMBNAIL_DECODED_URL = "https://www.museumap.hu/media-provider-webapp/rest/file/preview/solr/oai-aggregated-bib9568907?mediaId=323992&size=masterview&tenant_id=Museumap&defaultImage=true";
}

