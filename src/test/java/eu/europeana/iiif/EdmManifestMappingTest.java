package eu.europeana.iiif;

import com.jayway.jsonpath.Configuration;
import eu.europeana.iiif.model.v2.Annotation;
import eu.europeana.iiif.model.v2.AnnotationBody;
import eu.europeana.iiif.model.v2.Canvas;
import eu.europeana.iiif.model.v2.DataSet;
import eu.europeana.iiif.model.v2.Image;
import eu.europeana.iiif.model.v2.LanguageObject;
import eu.europeana.iiif.model.v2.MetaData;
import eu.europeana.iiif.model.v2.Sequence;
import eu.europeana.iiif.model.v2.Service;
import eu.europeana.iiif.service.EdmManifestMapping;
import eu.europeana.iiif.service.ManifestService;
import eu.europeana.iiif.service.ManifestSettings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests the EDM-IIIF Manifest mapping
 * @author Patrick Ehlert
 * Created on 13-02-2018
 */

public class EdmManifestMappingTest {

    private static final Logger LOG = LogManager.getLogger(EdmManifestMappingTest.class);

    private static final String TEST_EMPTY = "{\"object\": { \"proxies\":[], \"europeanaAggregation\":{}, \"aggregations\":[] }}";
    private static final String TEST_EMPTY_SUPPRESS_EXCEPTIONS = ""; // this will only work if suppress exceptions for the parser is enabled
    private static final String TEST_ID = "{\"object\": {\"about\":\"id\"}}";
    private static final String TEST_TITLE = "{\"object\": { \"proxies\":[{}, { \"dcTitle\":{\"en\":[\"Title\"]} }, {} ] }}";
    private static final String TEST_DESCRIPTION = "{\"object\": { \"proxies\":[{}, { \"dcDescription\":{\"def\":[\"Description\"]} }, {}] }}";
    private static final String TEST_TITLE_DESCRIPTION = "{\"object\": { \"proxies\":[ {\"dcTitle\":{\"en\":[\"Title\"]} }, { \"dcDescription\":{\"def\":[\"Description\"]} }, {}] }}";
    private static final String TEST_METADATA = "{\"object\": { \"proxies\":[{ \"dcType\":{\"en\":[\"Type\"]}}, { \"dcFormat\":{\"def\":[\"Format\"]} }, {}] }}";
    private static final String TEST_THUMBNAIL_ID = "https://www.europeana.eu/api/v2/thumbnail-by-url.json?uri=test&size=LARGE&type=IMAGE";
    private static final String TEST_THUMBNAIL = "{\"object\": {\"europeanaAggregation\" :{ \"edmPreview\":\""+TEST_THUMBNAIL_ID+"\"}}}";
    private static final String TEST_NAVDATE = "{\"object\": {\"proxies\":[{}, {\"dctermsIssued\":{\"en\":[\"NOT A REAL DATE\"]}}, {\"dctermsIssued\":{\"def\":[\"1922-03-15\"]}} ]}}";
    private static final String TEST_ATTRIBUTION = "{\"object\": {\"aggregations\":[{}, {\"webResources\":[{}, {\"textAttributionSnippet\":\"attributionText\"}]}]}}";
    private static final String TEST_LICENSE_EUROPEANAAGGREGATION = "{\"object\": { \"aggregations\": [{\"edmRights\": { \"en\": [\"licenseTextAggregation\"]}}], \"europeanaAggregation\" : {\"edmRights\": { \"en\": [\"licenseTextEuropeana\"]}}}}";
    private static final String TEST_LICENSE_OTHERAGGREGATION = "{\"object\": { \"europeanaAggregation\" : {\"edmRights\":{}}, \"aggregations\": [{}, {\"edmRights\": { \"en\": [\"licenseTextAggregation\"]}}] }}";
    private static final String TEST_SEQUENCE_1CANVAS_1SERVICE = "{\"object\": { \"aggregations\": [ {\"webResources\": [ {\"about\": \"wr1Id\", \"textAttributionSnippet\": \"wr1Attribution\", \"webResourceEdmRights\": {\"def\":[\"wr1License\"]}, \"ebuCoreHasMimeType\": \"wr1MimeType\", \"svcsHasService\": [\"service1Id\"]  } ] } ], \"services\": [{\"about\": \"service1Id\", \"doapImplements\": [\"serviceProfile\"]}] } }";
    private static final String TEST_SEQUENCE_4CANVASES_NOSERVICE = "{\"object\": { \"europeanaAggregation\" : {\"edmRights\":{}}, \"aggregations\": [{}, {\"edmRights\": { \"en\": [\"licenseTextAggregation\"]}}] }}";

    // Initialize the manifestservice, because that will setup our default Jackson mapper configuration used in the tests
    private static final ManifestService ms = new ManifestService(new ManifestSettings());


//    @Test
//    public void testEmpty() {
//        EdmManifestMapping.getManifestV2(TEST_EMPTY);
//        EdmManifestMapping.getManifestV3(TEST_EMPTY);
//    }

    @Test
    public void testId() {
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(TEST_ID);
        assertEquals("id", EdmManifestMapping.getEuropeanaId(document));
    }

    /**
     * Test if we get a title as a label, even when descriptions are present
     */
    @Test
    public void testLabelIsTitle() {
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(TEST_TITLE);
        LanguageObject[] labels = EdmManifestMapping.getLabelsV2(document);
        assertNotNull(labels[0]);
        assertEquals("en", labels[0].getLanguage());
        assertEquals("Title", labels[0].getValue());
    }

    /**
     * Test if we get a description as a label, if no title is present
     */
    @Test
    public void testLabelIsDescription() {
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(TEST_DESCRIPTION);
        LanguageObject[] labels = EdmManifestMapping.getLabelsV2(document);
        assertNotNull(labels[0]);
        assertNull(labels[0].getLanguage());
        assertEquals("Description", labels[0].getValue());
    }

    /**
     * Test if we handle missing title and description (so no label) properly
     */
    @Test
    public void testLabelEmpty() {
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(TEST_EMPTY);
        assertNull(EdmManifestMapping.getLabelsV2(document));
    }

    /**
     * Test if we get a proper description, if there are a title and description defined (not necessarily in 1 proxy)
     */
    @Test
    public void testDescription() {
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(TEST_TITLE_DESCRIPTION);
        LanguageObject[] descriptions = EdmManifestMapping.getDescriptionV2(document);
        assertNotNull(descriptions[0]);
        assertNull(descriptions[0].getLanguage());
        assertEquals("Description", descriptions[0].getValue());
    }

    /**
     * Test if they description is left out, if it's already used as a title (i.e. if there are no titles), or if there
     * are not descriptions at all.
     */
    @Test
    public void testDescriptionEmpty() {
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(TEST_DESCRIPTION);
        assertNull(EdmManifestMapping.getDescriptionV2(document));

        document = Configuration.defaultConfiguration().jsonProvider().parse(TEST_TITLE); // no description
        assertNull(EdmManifestMapping.getDescriptionV2(document));
    }

    /**
     * Test if we construct a metadata object properly
     */
    @Test
    public void testMetaDataV2() {
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(TEST_METADATA);
        MetaData[] metaData = EdmManifestMapping.getMetaDataV2(document);
        assertNotNull(metaData);
        assertEquals(2, metaData.length);

        assertNotNull(metaData[0]);
        assertEquals("format", metaData[0].getLabel());
        assertEquals(1, metaData[0].getValue().length);
        assertNull(metaData[0].getValue()[0].getLanguage());
        assertEquals("Format", metaData[0].getValue()[0].getValue());

        assertNotNull(metaData[1]);
        assertEquals("type", metaData[1].getLabel());
        assertEquals(1, metaData[1].getValue().length);
        assertEquals("en", metaData[1].getValue()[0].getLanguage());
        assertEquals("Type", metaData[1].getValue()[0].getValue());
    }

    /**
     * Test if we handle non-existing metadata properly
     */
    @Test
    public void testMetaDataV2Empty() {
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(TEST_EMPTY);
        assertNull(EdmManifestMapping.getMetaDataV2(document));
    }

    /**
     * Test if we retrieve thumbnail image information properly
     */
    @Test
    public void testThumbnail() {
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(TEST_THUMBNAIL);
        Image image = EdmManifestMapping.getThumbnailImageV2(ms.getSettings(), "test", document);
        assertNotNull(image);
        assertEquals(TEST_THUMBNAIL_ID, image.getId());
    }

    /**
     * Test if we handle non-existing thumbnails properly
     */
    @Test
    public void testThumbnailEmpty() {
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(TEST_EMPTY);
        assertNull(EdmManifestMapping.getThumbnailImageV2(ms.getSettings(), "test", document));
    }

    /**
     * Test if we output a date in the proper format
     */
    @Test
    public void testNavDate() {
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(TEST_NAVDATE);
        String navDate = EdmManifestMapping.getNavDate("test", document);
        assertNotNull(navDate);
        assertEquals("1922-03-15T00:00:00Z", navDate);
    }

    /**
     * Test if we handle non-existing navDates properly
     */
    @Test
    public void testNavDateEmpty() {
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(TEST_EMPTY);
        assertNull(EdmManifestMapping.getNavDate("test", document));
    }

    /**
     * Test if we retrieve attribution properly
     */
    @Test
    public void testAttribution() {
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(TEST_ATTRIBUTION);
        String attribution = EdmManifestMapping.getAttributionV2("test", document);
        assertNotNull(attribution);
        assertEquals("attributionText", attribution);
    }

    /**
     * Test if we handle non-existing attributions properly
     */
    @Test
    public void testAttributionEmpty() {
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(TEST_EMPTY);
        assertNull(EdmManifestMapping.getAttributionV2("test", document));
    }

    /**
     * Test if we retrieve license text from europeana aggregation
     */
    @Test
    public void testLicenseFromEuropeanaAggregation() {
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(TEST_LICENSE_EUROPEANAAGGREGATION);
        String license = EdmManifestMapping.getLicense("test", document);
        assertNotNull(license);
        assertEquals("licenseTextEuropeana", license);
    }

    /**
     * Test if we retrieve license text from other aggregations (if there isn't any in the europeanaAggregation)
     */
    @Test
    public void testLicenseFromOtherAggregations() {
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(TEST_LICENSE_OTHERAGGREGATION);
        String license = EdmManifestMapping.getLicense("test", document);
        assertNotNull(license);
        assertEquals("licenseTextAggregation", license);
    }

    /**
     * Test if we handle non-existing license properly
     */
    @Test
    public void testLicenseEmpty() {
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(TEST_EMPTY);
        assertNull(EdmManifestMapping.getLicense("test", document));
    }

    /**
     * Test if we generate the 3 seeAlso datasets with the correct ID
     */
    @Test
    public void testSeeAlso() {
        DataSet[] datasets = EdmManifestMapping.getDataSetsV2("TEST-ID");
        assertNotNull(datasets);
        assertEquals(3, datasets.length);
        for (DataSet dataset : datasets) {
            assertTrue(dataset.getId().contains("TEST-ID"));
        }
    }

    /**
     * Test that we do not create a sequence if there are no webresources
     */
    @Test
    public void testSequenceV2Empty() {
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(TEST_EMPTY);
        assertNull(EdmManifestMapping.getSequencesV2(ms.getSettings(), "test", document));
    }

    /**
     * Test if we generate sequences (and it's containing objects) properly
     */
    @Test
    public void testSequenceV2() {
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(TEST_SEQUENCE_1CANVAS_1SERVICE);
        Sequence[] sequence = EdmManifestMapping.getSequencesV2(ms.getSettings(), "/test-id", document);
        assertNotNull(sequence);
        assertEquals(1, sequence.length); // there should be only 1 sequence
        assertTrue(sequence[0].getId().endsWith("/test-id" + "/sequence/s1"));

        // test canvas part
        assertTrue(sequence[0].getStartCanvas().endsWith("/test-id" + "/canvas/p1"));
        assertNotNull(sequence[0].getCanvases());
        assertEquals(1, sequence[0].getCanvases().length);

        ExpectedCanvasValues ecv = new ExpectedCanvasValues();
        ecv.id = sequence[0].getStartCanvas();
        ecv.label = "p. 1";
        ecv.attribution = "wr1Attribution";
        ecv.license = "wr1License";
        ecv.annotationAndBody = new ExpectedAnnotationAndBodyValues();
        ecv.annotationAndBody.idEndsWith = "/test-id/annotation/p1";
        ecv.annotationAndBody.onId = ecv.id;
        ecv.annotationAndBody.bodyId = "wr1Id";
        ecv.annotationAndBody.format = "wr1MimeType";
        ecv.annotationAndBody.service = new ExpectedServiceValues();
        ecv.annotationAndBody.service.id = "service1Id";
        ecv.annotationAndBody.service.profile = "serviceProfile";

        checkCanvasV2(ecv, sequence[0].getCanvases()[0]);
    }

    // TODO test canvas ordering

    /**
     * Test if we generate a canvas object (and it's containing objects) properly
     */
    public void checkCanvasV2(ExpectedCanvasValues ecv, Canvas c) {
        assertEquals(ecv.id, c.getId());
        assertEquals(ecv.label, c.getLabel());
        // TODO read height, width from configuration and check it
        assertEquals(ecv.attribution, c.getAttribution());
        assertEquals(ecv.license, c.getLicense());

        // test image/annotation part (can be at most 1)
        if (ecv.annotationAndBody == null) {
            assertNull(c.getAttribution());
        } else {
            assertNotNull(c.getAttribution());
            assertEquals(1, c.getImages().length);
            checkAnnotationAndBodyV2(ecv.annotationAndBody, c.getImages()[0]);
        }
    }

    /**
     * Test if we generate an annotation and annotation body object (and containing service object) properly
     */
    public void checkAnnotationAndBodyV2(ExpectedAnnotationAndBodyValues eabv, Annotation ann) {
        assertTrue(ann.getId().endsWith(eabv.idEndsWith));
        assertEquals(eabv.onId, ann.getOn());

        // test annotationBody part
        AnnotationBody body = ann.getResource();
        assertNotNull(body);
        assertEquals(eabv.bodyId, body.getId());
        assertEquals(eabv.format, body.getFormat());

        // test service part
        if (eabv.service == null) {
            assertNull(body.getService());
        } else {
            assertNotNull(body.getService());
            checkServiceV2(eabv.service, body.getService());
        }
    }

    public void checkServiceV2(ExpectedServiceValues esv, Service s) {
        assertEquals(esv.id, s.getId());
        assertEquals(esv.profile, s.getProfile());
    }

    private static class ExpectedCanvasValues {
        String id;
        String label;
        String attribution;
        String license;
        ExpectedAnnotationAndBodyValues annotationAndBody;
    }

    private static class ExpectedAnnotationAndBodyValues {
        String idEndsWith;
        String onId;
        String bodyId; // annotationBody
        String format; // annotationbody
        ExpectedServiceValues service; //annotationbody
    }

    private static class ExpectedServiceValues {
        String id;
        String profile;
    }

}
