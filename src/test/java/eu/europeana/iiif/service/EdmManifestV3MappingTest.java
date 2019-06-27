package eu.europeana.iiif.service;

import com.jayway.jsonpath.Configuration;
import eu.europeana.iiif.config.ManifestSettings;
import eu.europeana.iiif.model.v3.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.*;

/**
 * Tests the EDM-IIIF Manifest v3 mapping
 * @author Patrick Ehlert
 * Created on 19-06-2019
 */

@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource("classpath:iiif-test.properties")
@SpringBootTest
public class EdmManifestV3MappingTest {

    // Initialize the manifest service, because that will setup our default Jackson mapper configuration used in the tests
    private static final ManifestService ms = new ManifestService(new ManifestSettings());

    // we don't test some fields because this is already done in v2, for example 'id' and 'navdate'

    @Test
    public void testWithinV3() {
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(EdmManifestData.TEST_WITHIN);
        Collection[] col = EdmManifestMapping.getWithinV3(document);
        assertNotNull(col);
        assertTrue(col.length > 0);
        assertEquals("https://data.theeuropeanlibrary.org/someurl", col[0].getId());
        assertEquals("Collection", col[0].getType());
    }

    @Test
    public void testWithinV3Empty() {
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(EdmManifestData.TEST_EMPTY);
        assertNull(EdmManifestMapping.getWithinV3(document));
    }

    /**
     * Test if we get a title as a label, even when descriptions are present
     */
    @Test
    public void testLabelIsTitle() {
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(EdmManifestData.TEST_TITLE);
        LanguageMap labels = EdmManifestMapping.getLabelsV3(document);
        testLanguageMap("en", new String[]{"Title"}, labels);
    }

    /**
     * Test if we get a description as a label, if no title is present
     */
    @Test
    public void testLabelIsDescription() {
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(EdmManifestData.TEST_DESCRIPTION);
        LanguageMap labels = EdmManifestMapping.getLabelsV3(document);
        testLanguageMap(LanguageMap.NO_LANGUAGE_KEY, new String[]{"Description"}, labels);
    }

    /**
     * Test if we handle missing title and description (so no label) properly
     */
    @Test
    public void testLabelEmpty() {
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(EdmManifestData.TEST_EMPTY);
        assertNull(EdmManifestMapping.getLabelsV3(document));
    }

    /**
     * Test if we get a proper description, if there are a title and description defined (not necessarily in 1 proxy)
     */
    @Test
    public void testDescription() {
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(EdmManifestData.TEST_TITLE_DESCRIPTION);
        LanguageMap descriptions = EdmManifestMapping.getDescriptionV3(document);
        testLanguageMap(LanguageMap.NO_LANGUAGE_KEY, new String[]{"Description"}, descriptions);
    }

    /**
     * Test if they description is left out, if it's already used as a title (i.e. if there are no titles), or if there
     * are not descriptions at all.
     */
    @Test
    public void testDescriptionEmpty() {
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(EdmManifestData.TEST_DESCRIPTION);
        assertNull(EdmManifestMapping.getDescriptionV3(document));

        document = Configuration.defaultConfiguration().jsonProvider().parse(EdmManifestData.TEST_TITLE); // no description
        assertNull(EdmManifestMapping.getDescriptionV3(document));
    }

    /**
     * Test if we construct a metadata object properly
     */
    @Test
    public void testMetaDataSimple() {
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(EdmManifestData.TEST_METADATA_SIMPLE);
        MetaData[] metaData = EdmManifestMapping.getMetaDataV3(document);
        assertNotNull(metaData);
        assertEquals(3, metaData.length);

        assertNotNull(metaData[0]);
        LanguageMap label1 = metaData[0].getLabel();
        LanguageMap value1 = metaData[0].getValue();
        testLanguageMap(LanguageMap.DEFAULT_METADATA_KEY, new String[]{"format"}, label1);
        testLanguageMap(LanguageMap.NO_LANGUAGE_KEY, new String[]{"SomeFormat"}, value1);

        assertNotNull(metaData[1]);
        LanguageMap label2 = metaData[1].getLabel();
        LanguageMap value2 = metaData[1].getValue();
        testLanguageMap(LanguageMap.DEFAULT_METADATA_KEY, new String[]{"type"}, label2);
        testLanguageMap("nl", new String[]{"Precies mijn type"}, value2);

        assertNotNull(metaData[2]);
        LanguageMap label3 = metaData[2].getLabel();
        LanguageMap value3 = metaData[2].getValue();
        testLanguageMap(LanguageMap.DEFAULT_METADATA_KEY, new String[]{"type"}, label3);
        testLanguageMap("en", new String[]{"Exactly my type as well"}, value3);
    }

    @Test
    public void testMetaDataComplicated() {
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(EdmManifestData.TEST_METADATA_COMPLICATED);
        System.err.println("testdata = "+EdmManifestData.TEST_METADATA_COMPLICATED);
        MetaData[] metaData = EdmManifestMapping.getMetaDataV3(document);
        assertNotNull(metaData);
        assertEquals(3, metaData.length);

        assertNotNull(metaData[0]);
        LanguageMap label1 = metaData[0].getLabel();
        LanguageMap value1 = metaData[0].getValue();
        testLanguageMap(LanguageMap.DEFAULT_METADATA_KEY, new String[]{"format"}, label1);
        testLanguageMap("en", new String[]{"SomeFormat"}, value1);

        assertNotNull(metaData[1]);
        LanguageMap label2 = metaData[1].getLabel();
        LanguageMap value2 = metaData[1].getValue();
        testLanguageMap(LanguageMap.DEFAULT_METADATA_KEY, new String[]{"source"}, label2);
        testLanguageMap(LanguageMap.NO_LANGUAGE_KEY, new String[]{"<a href='URI'>http://data.europeana.eu/place/base/203206</a>"}, value2);
        testLanguageMap("be", new String[]{"Bierbeek"}, value2);
        testLanguageMap("bg", new String[]{"Бийрбек"}, value2);
        testLanguageMap("zh", new String[]{"比尔贝克"}, value2);

        assertNotNull(metaData[2]);
        LanguageMap label3 = metaData[2].getLabel();
        LanguageMap value3 = metaData[2].getValue();
        testLanguageMap(LanguageMap.DEFAULT_METADATA_KEY, new String[]{"source"}, label3);
        testLanguageMap(LanguageMap.NO_LANGUAGE_KEY, new String[]{"May the source be with you"}, value3);
    }

    /**
     * Test if we handle non-existing metadata properly
     */
    @Test
    public void testMetaDataV3Empty() {
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(EdmManifestData.TEST_EMPTY);
        assertNull(EdmManifestMapping.getMetaDataV3(document));
    }

    /**
     * Test if we retrieve thumbnail image information properly
     */
    @Test
    public void testThumbnail() {
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(EdmManifestData.TEST_THUMBNAIL);
        Image[] images = EdmManifestMapping.getThumbnailImageV3("test", document);
        assertNotNull(images);
        assertEquals(1, images.length);
        assertEquals(EdmManifestData.TEST_THUMBNAIL_ID, images[0].getId());
        assertEquals("Image", images[0].getType());
    }

    /**
     * Test if we handle non-existing thumbnails properly
     */
    @Test
    public void testThumbnailEmpty() {
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(EdmManifestData.TEST_EMPTY);
        assertNull(EdmManifestMapping.getThumbnailImageV3( "test", document));
    }

    /**
     * Test if we retrieve attribution properly
     */
    @Test
    public void testAttribution() {
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(EdmManifestData.TEST_ATTRIBUTION);
        LanguageMap attribution = EdmManifestMapping.getAttributionV3("test", EdmManifestData.TEST_IS_SHOWN_BY, document);
        testLanguageMap(LanguageMap.DEFAULT_METADATA_KEY, new String[]{"attributionTextOk"}, attribution);
    }

    /**
     * Test if we handle non-existing attributions properly
     */
    @Test
    public void testAttributionEmpty() {
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(EdmManifestData.TEST_EMPTY);
        assertNull(EdmManifestMapping.getAttributionV3("test", EdmManifestData.TEST_IS_SHOWN_BY, document));
    }

    /**
     * Test if we retrieve license text from europeana aggregation
     */
    @Test
    public void testRightsFromEuropeanaAggregation() {
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(EdmManifestData.TEST_LICENSE_EUROPEANAAGGREGATION);
        Rights rights = EdmManifestMapping.getRights("test", document);
        assertNotNull(rights);
        assertEquals("licenseTextEuropeana", rights.getId());
        assertEquals("Text", rights.getType());
        assertEquals("text/html", rights.getFormat());
    }

    /**
     * Test if we retrieve license text from other aggregations (if there isn't any in the europeanaAggregation)
     */
    @Test
    public void testRightsFromOtherAggregations() {
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(EdmManifestData.TEST_LICENSE_OTHERAGGREGATION);
        Rights rights = EdmManifestMapping.getRights("test", document);
        assertNotNull(rights);
        assertEquals("licenseTextAggregation", rights.getId());
    }

    /**
     * Test if we handle non-existing license properly
     */
    @Test
    public void testRightsEmpty() {
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(EdmManifestData.TEST_EMPTY);
        assertNull(EdmManifestMapping.getRights("test", document));
    }

    /**
     * Test if we generate the 3 seeAlso datasets with the correct ID
     */
    @Test
    public void testSeeAlso() {
        DataSet[] datasets = EdmManifestMapping.getDataSetsV3("TEST-ID");
        assertNotNull(datasets);
        assertEquals(3, datasets.length);
        for (DataSet dataset : datasets) {
            assertTrue(dataset.getId().contains("TEST-ID"));
        }
    }

    /**
     * Test that we do not create canvases if there are no webresources
     */
    @Test
    public void testCanvasEmpty() {
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(EdmManifestData.TEST_EMPTY);
        assertNull(EdmManifestMapping.getItems(ms.getSettings(), "test", null, document));
    }

    /**
     * Test that we do not create a canvas if the webresource is not an edmIsShownAtField (or hasView)
     */
    @Test
    public void testCanvasMissingIsShownAtHasView() {
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(EdmManifestData.TEST_SEQUENCE_2CANVAS_NOISSHOWNAT);
        assertNull(EdmManifestMapping.getItems(ms.getSettings(), "test", null, document));
    }

    /**
     * Test if we generate canvases (and their containing objects) properly
     */
    @Test
    public void testCanvases() {
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(EdmManifestData.TEST_SEQUENCE_2CANVAS_1SERVICE);
        String edmIsShownBy = EdmManifestMapping.getIsShownBy(null, document);
        Canvas[] canvases = EdmManifestMapping.getItems(ms.getSettings(), "/test-id", edmIsShownBy, document);
        assertNotNull(canvases);
        assertEquals(2, canvases.length);

        // CANVAS 1
        Canvas canvas1 = canvases[0];
        ExpectedCanvasAndAnnotationPageValues expectedCanvas = new ExpectedCanvasAndAnnotationPageValues();
        expectedCanvas.idEndsWith = "/test-id/canvas/p1";
        expectedCanvas.type = "Canvas";
        expectedCanvas.label = new LanguageMap(LanguageMap.NO_LANGUAGE_KEY, "p. 1");
        expectedCanvas.duration = 98.765;
        expectedCanvas.attribution = new LanguageMap(LanguageMap.DEFAULT_METADATA_KEY, "wr1Attribution");
        expectedCanvas.rightsId = "wr1License";
        expectedCanvas.annoPageid = null; // we only set if for fulltext annopages
        expectedCanvas.annoPageType = "AnnotationPage";

        ExpectedAnnotationAndBodyValues expectedAnnotation = new ExpectedAnnotationAndBodyValues();
        expectedCanvas.annoPageAnnotationAndBody = new ExpectedAnnotationAndBodyValues[] {expectedAnnotation};
        expectedAnnotation.id = null;
        expectedAnnotation.type = "Annotation";
        expectedAnnotation.motivation = "painting";
        expectedAnnotation.timeMode = "trim";
        expectedAnnotation.target = "https://iiif.europeana.eu/presentation/test-id/canvas/p1";
        expectedAnnotation.bodyId = "wr1Id";
        expectedAnnotation.bodyType = "Video";
        expectedAnnotation.bodyFormat = "video/mp4";
        expectedAnnotation.hasService = true;
        expectedAnnotation.bodyServiceId = "service1Id";
        expectedAnnotation.bodyServiceProfile = "serviceProfile";
        expectedAnnotation.bodyServiceType = "ImageService3";
        checkCanvas(expectedCanvas, canvas1);

        // CANVAS 2
        Canvas canvas2 = canvases[1];
        ExpectedCanvasAndAnnotationPageValues expectedCanvas2 = new ExpectedCanvasAndAnnotationPageValues();
        expectedCanvas2.idEndsWith = "/test-id/canvas/p2";
        expectedCanvas2.type = "Canvas";
        expectedCanvas2.label = new LanguageMap(LanguageMap.NO_LANGUAGE_KEY, "p. 2");
        expectedCanvas2.duration = null;
        expectedCanvas2.attribution = new LanguageMap(LanguageMap.DEFAULT_METADATA_KEY, "wr2Attribution");
        expectedCanvas2.rightsId = "wr2License";
        expectedCanvas2.annoPageid = null; // we only set if for fulltext annopages
        expectedCanvas2.annoPageType = "AnnotationPage";

        ExpectedAnnotationAndBodyValues expectedAnnotation2 = new ExpectedAnnotationAndBodyValues();
        expectedCanvas2.annoPageAnnotationAndBody = new ExpectedAnnotationAndBodyValues[] {expectedAnnotation2};
        expectedAnnotation2.id = null;
        expectedAnnotation2.type = "Annotation";
        expectedAnnotation2.motivation = "painting";
        expectedAnnotation2.timeMode = null;
        expectedAnnotation2.target = "https://iiif.europeana.eu/presentation/test-id/canvas/p2";
        expectedAnnotation2.bodyId = "wr2Id";
        expectedAnnotation2.bodyType = "Unknown";
        expectedAnnotation2.bodyFormat = "wr2MimeType";
        expectedAnnotation2.hasService = false;
        checkCanvas(expectedCanvas2, canvas2);
    }

    private void checkCanvas(ExpectedCanvasAndAnnotationPageValues expected, Canvas canvas) {
        assertNotNull(canvas);
        assertTrue("Expected id ending with " + expected.idEndsWith + " but id is "+canvas.getId(),
                canvas.getId().endsWith(expected.idEndsWith));
        assertEquals(expected.type, canvas.getType());
        testLanguageMap(expected.label, canvas.getLabel());
        assertEquals(expected.duration, canvas.getDuration());
        testLanguageMap(expected.attribution, canvas.getAttribution());
        assertEquals(expected.rightsId, canvas.getRights().getId());

        assertNotNull(canvas.getItems());
        AnnotationPage ap = canvas.getItems()[0];
        assertNotNull(ap);
        assertEquals(expected.annoPageid, ap.getId());
        assertEquals(expected.annoPageType, ap.getType());
        checkAnnotationAndBodyAndServiceValues(expected.annoPageAnnotationAndBody, ap.getItems());
    }

    private void checkAnnotationAndBodyAndServiceValues(ExpectedAnnotationAndBodyValues[] expectedAnnotations, Annotation[] annotations) {
        assertNotNull(annotations);
        assertNotNull(expectedAnnotations);
        for (int i = 0; i < expectedAnnotations.length; i++) {
            ExpectedAnnotationAndBodyValues expected = expectedAnnotations[i];
            Annotation annotation = annotations[i];
            assertEquals("Annotation id", expected.id, annotation.getId());
            assertEquals("Annotation type", expected.type, annotation.getType());
            assertEquals("Annotation motivation", expected.motivation, annotation.getMotivation());
            assertEquals("Annotation timeMode", expected.timeMode, annotation.getTimeMode());
            assertEquals("Annotation target", expected.target, annotation.getTarget());
            AnnotationBody body = annotation.getBody();
            assertNotNull(body); // body should always be present
            assertEquals("AnnotationBody id", expected.bodyId, body.getId());
            assertEquals("AnnotationBody type",expected.bodyType, body.getType());
            assertEquals("AnnotationBody format", expected.bodyFormat, body.getFormat());
            if (expected.hasService) {
                Service service = body.getService();
                assertNotNull(service);
                assertEquals("Service id", expected.bodyServiceId, service.getId());
                assertEquals("Service type", expected.bodyServiceType, service.getType());
                assertEquals("Service profile", expected.bodyServiceProfile, service.getProfile());
            } else {
                assertNull (annotation.getBody().getService());
            }

        }
    }

    private static class ExpectedCanvasAndAnnotationPageValues {
        String idEndsWith;
        String type;
        LanguageMap label;
        Double duration;
        LanguageMap attribution;
        String rightsId;
        // in these tests there can be only 1 annotationPage (because full-text availability check is done elsewhere)
        String annoPageid;
        String annoPageType;
        ExpectedAnnotationAndBodyValues[] annoPageAnnotationAndBody;
    }

    private static class ExpectedAnnotationAndBodyValues {
        String id;
        String type;
        String motivation;
        String timeMode;
        String target;
        String bodyId;
        String bodyType;
        String bodyFormat;
        boolean hasService;
        String bodyServiceId;
        String bodyServiceType;
        String bodyServiceProfile;
    }

    /**
     * Test if the provided languageMap contains a particular key and set of values. Ordering of values is also checked
     */
    private void testLanguageMap(String expectedKey, String[] expectedValues, LanguageMap map) {
        assertNotNull(map);
        if (expectedKey == null) {
            assertEquals(0, map.size());
            assertNull("Expected value cannot be set when there is no key!", expectedValues);
        } else {
            assertTrue("Key '" + expectedKey + "' not found!", map.containsKey(expectedKey));
            String[] values = map.get(expectedKey);
            if (expectedValues == null) {
                assertNull(values);
            } else {
                for (int i = 0; i < expectedValues.length; i++) {
                    assertEquals(values[i], expectedValues[i]);
                }
            }
        }
    }

    private void testLanguageMap(LanguageMap expectedMap, LanguageMap map) {
        if (expectedMap == null) {
            assertNull(map);
        } else {
            for (String expectedKey : expectedMap.keySet()) {
                testLanguageMap(expectedKey, expectedMap.get(expectedKey), map);
            }
        }
    }
}
