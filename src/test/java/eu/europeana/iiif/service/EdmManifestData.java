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
                                                                                       "{ \"dcSource\":{\"def\" :[\"May the source be with you\"]}} ], " +
                                                                          "\"concepts\": [{ \"about\":\"http://data.europeana.eu/place/base/203206\", " +
                                                                                           "\"prefLabel\":{\"be\":[\"Bierbeek\"], \"bg\": [\"Бийрбек\"], \"zh\":[\"比尔贝克\"] }}]" + "}}";
    
    public static final String TEST_THUMBNAIL_ID = "https://www.europeana.eu/api/v2/thumbnail-by-url.json?uri=test&size=LARGE&type=IMAGE";
    public static final String TEST_THUMBNAIL = "{\"object\": {\"europeanaAggregation\" :{ \"edmPreview\":\""+TEST_THUMBNAIL_ID+"\"}}}";
    
    public static final String TEST_NAVDATE = "{\"object\": {\"proxies\":[{}, {\"dctermsIssued\":{\"en\":[\"NOT A REAL DATE\"]}}, {\"dctermsIssued\":{\"def\":[\"1922-03-15\"]}} ]}}";
    
    public static final String TEST_IS_SHOWN_BY = "https://test.europeana.eu/test.jpg";
    
    public static final String TEST_ATTRIBUTION = "{\"object\": {\"aggregations\":[{ \"edmIsShownBy\":\""+TEST_IS_SHOWN_BY+"\"}, "+
            " {\"webResources\":[{\"about\":\"http://dont.pick/me.jpg\", \"textAttributionSnippet\":\"attributionTextFalse\"}," +
            "{\"about\":\""+TEST_IS_SHOWN_BY+"\", \"textAttributionSnippet\":\"attributionTextOk\"}" +"]}]}}";
    
    public static final String TEST_LICENSE_EUROPEANAAGGREGATION = "{\"object\": { \"aggregations\": [{\"edmRights\": { \"en\": [\"licenseTextAggregation\"]}}], \"europeanaAggregation\" : {\"edmRights\": { \"en\": [\"licenseTextEuropeana\"]}}}}";
    public static final String TEST_LICENSE_OTHERAGGREGATION = "{\"object\": { \"europeanaAggregation\" : {\"edmRights\":{}}, \"aggregations\": [{}, {\"edmRights\": { \"en\": [\"licenseTextAggregation\"]}}] }}";
    
    public static final String TEST_SEQUENCE_2CANVAS_1SERVICE = "{\"object\": { \"aggregations\": [ {\"edmIsShownBy\": \"wr1Id\", \"hasView\": [\"wr2Id\"], \"webResources\": [ "+
            "{\"about\": \"wr1Id\", \"textAttributionSnippet\": \"wr1Attribution\", \"webResourceEdmRights\":"+
            "{\"def\":[\"wr1License\"]}, \"ebucoreHasMimeType\": \"video/mp4\", \"svcsHasService\": [\"service1Id\"], \"ebucoreDuration\": \"98765\"  },"+
            "{\"about\": \"wr2Id\", \"textAttributionSnippet\": \"wr2Attribution\", \"webResourceEdmRights\":"+
            "{\"def\":[\"wr2License\"]}, \"ebucoreHasMimeType\": \"wr2MimeType\" }"+
            "] } ], \"services\": [{\"about\": \"service1Id\", \"doapImplements\": [\"serviceProfile\"]}] } }";
    public static final String TEST_SEQUENCE_2CANVAS_NOISSHOWNAT = "{\"object\": { \"aggregations\": [ { \"webResources\": [ "+
            "{\"about\": \"wr1Id\", \"textAttributionSnippet\": \"wr1Attribution\", \"webResourceEdmRights\":"+
            "{\"def\":[\"wr1License\"]}, \"ebucoreHasMimeType\": \"wr1MimeType\", \"svcsHasService\": [\"service1Id\"]  },"+
            "{\"about\": \"wr2Id\", \"textAttributionSnippet\": \"wr2Attribution\", \"webResourceEdmRights\":"+
            "{\"def\":[\"wr2License\"]}, \"ebucoreHasMimeType\": \"wr2MimeType\", \"svcsHasService\": [\"service2Id\"]  }"+
            "] } ], \"services\": [{\"about\": \"service1Id\", \"doapImplements\": [\"serviceProfile\"]}] } }";
}
