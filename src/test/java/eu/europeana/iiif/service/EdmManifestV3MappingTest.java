package eu.europeana.iiif.service;

import static eu.europeana.iiif.model.ManifestDefinitions.ATTRIBUTION_STRING;

import com.jayway.jsonpath.Configuration;
import eu.europeana.iiif.config.AppConfig;
import eu.europeana.iiif.config.ManifestSettings;
import eu.europeana.iiif.config.MediaTypes;
import eu.europeana.iiif.config.SerializationConfig;
import eu.europeana.iiif.model.v3.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Tests the EDM-IIIF Manifest v3 mapping
 * @author Patrick Ehlert
 * Created on 19-06-2019
 */

@TestPropertySource("classpath:iiif-test.properties")
@SpringBootTest(classes = {EdmManifestMappingV3.class, ManifestSettings.class, AppConfig.class, SerializationConfig.class})
public class EdmManifestV3MappingTest {

    private static final Logger LOG = LogManager.getLogger(EdmManifestV3MappingTest.class);

    // Initialize the manifest service, because that will setup our default Jackson mapper configuration used in the tests
    private static final ManifestService ms = new ManifestService(new ManifestSettings(), new MediaTypes());

    @Autowired
    private ManifestSettings settings;

    @Autowired
    private MediaTypes mediaTypes;

    // we don't test some fields because this is already done in v2, for example 'id' and 'navdate'

    @Test
    public void testWithinV3() {
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(EdmManifestData.TEST_WITHIN);
        Collection[] col = EdmManifestMappingV3.getWithinV3(document);
        Assertions.assertNotNull(col);
        Assertions.assertTrue(col.length > 0);
        Assertions.assertEquals("https://data.theeuropeanlibrary.org/someurl", col[0].getId());
        Assertions.assertEquals("Collection", col[0].getType().get());
    }

    @Test
    public void testWithinV3Empty() {
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(EdmManifestData.TEST_EMPTY);
        Assertions.assertNull(EdmManifestMappingV3.getWithinV3(document));
    }

    /**
     * Test if we get a title as a label, even when descriptions are present
     */
    @Test
    public void testLabelIsTitle() {
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(EdmManifestData.TEST_TITLE);
        LanguageMap labels = EdmManifestMappingV3.getLabelsV3(document);
        testLanguageMap("en", new String[]{"Title"}, labels);
    }

    /**
     * Test if we get a description as a label, if no title is present
     */
    @Test
    public void testLabelIsDescription() {
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(EdmManifestData.TEST_DESCRIPTION);
        LanguageMap labels = EdmManifestMappingV3.getLabelsV3(document);
        testLanguageMap(LanguageMap.NO_LANGUAGE_KEY, new String[]{"Description"}, labels);
    }

    /**
     * Test if we handle missing title and description (so no label) properly
     */
    @Test
    public void testLabelEmpty() {
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(EdmManifestData.TEST_EMPTY);
        Assertions.assertNull(EdmManifestMappingV3.getLabelsV3(document));
    }

    /**
     * Test if we get a proper description, if there are a title and description defined (not necessarily in 1 proxy)
     */
    @Test
    public void testDescription() {
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(EdmManifestData.TEST_TITLE_DESCRIPTION);
        LanguageMap descriptions = EdmManifestMappingV3.getDescriptionV3(document);
        testLanguageMap(LanguageMap.NO_LANGUAGE_KEY, new String[]{"Description"}, descriptions);
    }

    /**
     * Test if they description is left out, if it's already used as a title (i.e. if there are no titles), or if there
     * are not descriptions at all.
     */
    @Test
    public void testDescriptionEmpty() {
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(EdmManifestData.TEST_DESCRIPTION);
        Assertions.assertNull(EdmManifestMappingV3.getDescriptionV3(document));

        document = Configuration.defaultConfiguration().jsonProvider().parse(EdmManifestData.TEST_TITLE); // no description
        Assertions.assertNull(EdmManifestMappingV3.getDescriptionV3(document));
    }

    /**
     * Test if we construct a metadata object properly
     */
    @Test
    public void testMetaDataSimple() {
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(EdmManifestData.TEST_METADATA_SIMPLE);
        MetaData[] metaData = EdmManifestMappingV3.getMetaDataV3(document);
        Assertions.assertNotNull(metaData);
        Assertions.assertEquals(3, metaData.length);

        Assertions.assertNotNull(metaData[0]);
        LanguageMap label1 = metaData[0].getLabel();
        LanguageMap value1 = metaData[0].getValue();
        testLanguageMap(LanguageMap.DEFAULT_METADATA_KEY, new String[]{"format"}, label1);
        testLanguageMap(LanguageMap.NO_LANGUAGE_KEY, new String[]{"SomeFormat"}, value1);

        Assertions.assertNotNull(metaData[1]);
        LanguageMap label2 = metaData[1].getLabel();
        LanguageMap value2 = metaData[1].getValue();
        testLanguageMap(LanguageMap.DEFAULT_METADATA_KEY, new String[]{"type"}, label2);
        testLanguageMap("nl", new String[]{"Precies mijn type"}, value2);

        Assertions.assertNotNull(metaData[2]);
        LanguageMap label3 = metaData[2].getLabel();
        LanguageMap value3 = metaData[2].getValue();
        testLanguageMap(LanguageMap.DEFAULT_METADATA_KEY, new String[]{"type"}, label3);
        testLanguageMap("en", new String[]{"Exactly my type as well"}, value3);
    }

    @Test
    public void testMetaDataComplicated() {
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(EdmManifestData.TEST_METADATA_COMPLICATED);
        MetaData[] metaData = EdmManifestMappingV3.getMetaDataV3(document);
        Assertions.assertNotNull(metaData);
        Assertions.assertEquals(3, metaData.length);

        LOG.info("metaData1 = "+metaData[0]);
        Assertions.assertNotNull(metaData[0]);
        LanguageMap label1 = metaData[0].getLabel();
        LanguageMap value1 = metaData[0].getValue();
        testLanguageMap(LanguageMap.DEFAULT_METADATA_KEY, new String[]{"format"}, label1);
        testLanguageMap("en", new String[]{"SomeFormat"}, value1);
        Assertions.assertEquals("label: ({en=[format]}) value: ({en=[SomeFormat]})", metaData[0].toString());

        LOG.info("metaData2 = "+metaData[1]);
        Assertions.assertNotNull(metaData[1]);
        LanguageMap label2 = metaData[1].getLabel();
        LanguageMap value2 = metaData[1].getValue();
        testLanguageMap(LanguageMap.DEFAULT_METADATA_KEY, new String[]{"source"}, label2);
        testLanguageMap(LanguageMap.NO_LANGUAGE_KEY, new String[]{"<a href='http://data.europeana.eu/place/base/203206'>http://data.europeana.eu/place/base/203206</a>"}, value2);
        testLanguageMap("be", new String[]{"Bierbeek"}, value2);
        testLanguageMap("bg", new String[]{"Бийрбек"}, value2);
        testLanguageMap("zh", new String[]{"比尔贝克"}, value2);
        Assertions.assertEquals("label: ({en=[source]}) value: ({@none=[<a href='http://data.europeana.eu/place/base/203206'>http://data.europeana.eu/place/base/203206</a>]}, {be=[Bierbeek]}, {bg=[Бийрбек]}, {zh=[比尔贝克]})", metaData[1].toString());

        LOG.info("metaData3 = "+metaData[2]);
        Assertions.assertNotNull(metaData[2]);
        LanguageMap label3 = metaData[2].getLabel();
        LanguageMap value3 = metaData[2].getValue();
        testLanguageMap(LanguageMap.DEFAULT_METADATA_KEY, new String[]{"source"}, label3);
        testLanguageMap(LanguageMap.NO_LANGUAGE_KEY, new String[]{"May the source be with you", "<a href='https://some.url'>https://some.url</a>"}, value3);
        testLanguageMap("en", new String[]{"Just a test"}, value3);
        Assertions.assertEquals("label: ({en=[source]}) value: ({@none=[May the source be with you, <a href='https://some.url'>https://some.url</a>]}, {en=[Just a test]})", metaData[2].toString());
    }

    /**
     * Test if we handle non-existing metadata properly
     */
    @Test
    public void testMetaDataV3Empty() {
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(EdmManifestData.TEST_EMPTY);
        Assertions.assertNull(EdmManifestMappingV3.getMetaDataV3(document));
    }

    /**
     * Test if we retrieve thumbnail image information properly
     */
    @Test
    public void testThumbnail() {
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(EdmManifestData.TEST_THUMBNAIL);
        Image[] images = EdmManifestMappingV3.getThumbnailImageV3("test", document);
        Assertions.assertNotNull(images);
        Assertions.assertEquals(1, images.length);
        Assertions.assertEquals(EdmManifestData.TEST_THUMBNAIL_ID, images[0].getId());
        Assertions.assertEquals("Image", images[0].getType().get());
    }

    /**
     * Test if we handle non-existing thumbnails properly
     */
    @Test
    public void testThumbnailEmpty() {
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(EdmManifestData.TEST_EMPTY);
        Assertions.assertNull(EdmManifestMappingV3.getThumbnailImageV3( "test", document));
    }

    /**
     * Tests if thumbnails are added for non-IIIF resources (ie. without a "svcsHasService" field)
     * JSON TEST_SEQUENCE_3CANVAS_1SERVICE has a "svcsHasService" field and shouldn't have a thumbnail added
     * JSON TEST_SEQUENCE_MULTIPLE_PROXY_AGG doesn't have a "svcsHasService" field and shouldn't have a thumbnail
     */
//    @Test
//    public void testThumbnailOrSVCS() {
//        Object hasNoThumbnail = Configuration.defaultConfiguration().jsonProvider().parse(EdmManifestData.TEST_SEQUENCE_3CANVAS_1SERVICE);
//        Assertions.assertNull(EdmManifestMappingV3.getCanvasThumbnailImageV3(( "test", hasNoThumbnail));
//        Object hasThumbnail = Configuration.defaultConfiguration().jsonProvider().parse(EdmManifestData.TEST_SEQUENCE_MULTIPLE_PROXY_AGG);
//        Assertions.assertNotNull(EdmManifestMappingV3.getThumbnailImageV3( "test", hasThumbnail));
//    }

    /**
     * Test if we retrieve attribution properly
     * NB in EA-3324 the attribution was extended RequiredStatement =  <Map><String, LanguageMap>
     */
    @Test
    public void testAttribution() {
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(EdmManifestData.TEST_ATTRIBUTION);
        RequiredStatementMap requiredStatementMap = EdmManifestMappingV3.getAttributionV3Root("test", EdmManifestData.TEST_IS_SHOWN_BY, document);
        testRequiredStatementMap(LanguageMap.DEFAULT_METADATA_KEY, new String[]{EdmManifestData.TEST_ATTRIBUTION_TEXT_V3}, requiredStatementMap);
    }

    /**
     * Test if we handle non-existing attributions properly
     */
    @Test
    public void testAttributionEmpty() {
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(EdmManifestData.TEST_EMPTY);
        Assertions.assertNull(EdmManifestMappingV3.getAttributionV3Root("test", EdmManifestData.TEST_IS_SHOWN_BY, document));
    }

    /**
     * Test if we retrieve license text from europeana aggregation
     */
    @Test
    public void testRightsFromEuropeanaAggregation() {
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(EdmManifestData.TEST_LICENSE_EUROPEANAAGGREGATION);
        Rights rights = EdmManifestMappingV3.getRights("test", document);
        Assertions.assertNotNull(rights);
        Assertions.assertEquals("licenseTextEuropeana", rights.getId());
        Assertions.assertEquals("Text", rights.getType().get());
        Assertions.assertEquals("text/html", rights.getFormat());
    }

    /**
     * Test if we retrieve license text from other aggregations (if there isn't any in the europeanaAggregation)
     */
    @Test
    public void testRightsFromOtherAggregations() {
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(EdmManifestData.TEST_LICENSE_OTHERAGGREGATION);
        Rights rights = EdmManifestMappingV3.getRights("test", document);
        Assertions.assertNotNull(rights);
        Assertions.assertEquals("licenseTextAggregation", rights.getId());
    }

    /**
     * Test if we handle non-existing license properly
     */
    @Test
    public void testRightsEmpty() {
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(EdmManifestData.TEST_EMPTY);
        Assertions.assertNull(EdmManifestMappingV3.getRights("test", document));
    }

    /**
     * Test if we generate the 3 seeAlso datasets with the correct ID
     */
    @Test
    public void testSeeAlso() {
        DataSet[] datasets = EdmManifestMappingV3.getDataSetsV3(settings, "TEST-ID");
        Assertions.assertNotNull(datasets);
        Assertions.assertEquals(3, datasets.length);
        for (DataSet dataset : datasets) {
            Assertions.assertTrue(dataset.getId().contains("TEST-ID"));
        }
    }

    /**
     * Test if we set a proper start canvas
     */
    @Test
    public void testStartCanvas() {
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(EdmManifestData.TEST_SEQUENCE_3CANVAS_1SERVICE);
        String edmIsShownBy = EdmManifestUtils.getValueFromDataProviderAggregation(document, null, "edmIsShownBy");
        Canvas[] canvases = EdmManifestMappingV3.getItems(settings, mediaTypes, "/test-id", edmIsShownBy, document, null);
        Canvas start = EdmManifestMappingV3.getStartCanvasV3(canvases, edmIsShownBy);

        // test if only a few fields are set and the rest is null
        ExpectedCanvasAndAnnotationPageValues expectedCanvas = new ExpectedCanvasAndAnnotationPageValues();
        expectedCanvas.idEndsWith = "/test-id/canvas/p1";
        expectedCanvas.type = "Canvas";
        checkCanvas(expectedCanvas, start);
    }

    /**
     * Test if it works fine with multiple proxy aggregation object
     */
    @Test
    public void testStartCanvasWithMultipleProxyAggregation() {
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(EdmManifestData.TEST_SEQUENCE_MULTIPLE_PROXY_AGG);
        String proxyIn = EdmManifestUtils.getDataProviderFromProxyWithOutLineage(document, null);
        Assertions.assertNotNull(proxyIn);

        String edmIsShownBy = EdmManifestUtils.getValueFromDataProviderAggregation(document, null, "edmIsShownBy");
        String isShownAt = EdmManifestUtils.getValueFromDataProviderAggregation(document, null, "edmIsShownAt");

        Assertions.assertNotNull(edmIsShownBy);
        Assertions.assertNull(isShownAt);

        Canvas[] canvases = EdmManifestMappingV3.getItems(settings, mediaTypes, "/test-id", edmIsShownBy, document, null);
        Canvas start = EdmManifestMappingV3.getStartCanvasV3(canvases, edmIsShownBy);

        ExpectedCanvasAndAnnotationPageValues expectedCanvas = new ExpectedCanvasAndAnnotationPageValues();
        expectedCanvas.idEndsWith = "/test-id/canvas/p1";
        expectedCanvas.type = "Canvas";
        checkCanvas(expectedCanvas, start);
    }

    @Test
    public void startCanvasNoIsShownByShouldNotThrow() {
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(EdmManifestData.TEST_SEQUENCE_3CANVAS_NOISSHOWNBY);
        String edmIsShownBy = EdmManifestUtils.getValueFromDataProviderAggregation(document, null, "edmIsShownBy");

        System.out.println(mediaTypes.mediaTypeCategories.size());
        Canvas[] canvases = EdmManifestMappingV3.getItems(settings, mediaTypes, "/test-id", edmIsShownBy, document, null);
        Canvas start = EdmManifestMappingV3.getStartCanvasV3(canvases, edmIsShownBy);

        // test if only a few fields are set and the rest is null
        ExpectedCanvasAndAnnotationPageValues expectedCanvas = new ExpectedCanvasAndAnnotationPageValues();
        expectedCanvas.idEndsWith = "/test-id/canvas/p1";
        expectedCanvas.type = "Canvas";
        checkCanvas(expectedCanvas, start);
    }

    @Test
    public void testStartCanvasEmpty() {
       Assertions.assertNull(EdmManifestMappingV3.getStartCanvasV3(null, null));
    }

    /**
     * Test that we do not create canvases if there are no webresources
     */
    @Test
    public void testCanvasEmpty() {
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(EdmManifestData.TEST_EMPTY);
        Assertions.assertNull(EdmManifestMappingV3.getItems(settings, mediaTypes, "test", null, document, null));
    }

    /**
     * Test that we do not create a canvas if the webresource is not an edmIsShownAtField (or hasView)
     */
    @Test
    public void testCanvasMissingIsShownAtHasView() {
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(EdmManifestData.TEST_SEQUENCE_2CANVAS_NOISSHOWNBY);
        Assertions.assertNull(EdmManifestMappingV3.getItems(settings, mediaTypes, "test", null, document, null));
    }

    /**
     * Test if we generate canvases (and their containing objects) properly
     */
    @Test
    public void testCanvases() {
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(EdmManifestData.TEST_SEQUENCE_3CANVAS_1SERVICE);
        String edmIsShownBy = EdmManifestUtils.getValueFromDataProviderAggregation(document, null, "edmIsShownBy");
        Canvas[] canvases = EdmManifestMappingV3.getItems(settings, mediaTypes, "/test-id", edmIsShownBy, document, null);
        Assertions.assertNotNull(canvases);
        // note that the 3rd canvas is not edmIsShownBy or hasView so not included
        Assertions.assertEquals(2, canvases.length);

        // CANVAS 1
        Canvas canvas1 = canvases[0];
        ExpectedCanvasAndAnnotationPageValues expectedCanvas = new ExpectedCanvasAndAnnotationPageValues();
        expectedCanvas.idEndsWith = "/test-id/canvas/p1";
        expectedCanvas.type = "Canvas";
        expectedCanvas.label = new LanguageMap(LanguageMap.NO_LANGUAGE_KEY, "p. 1");
        expectedCanvas.duration = 98.765;
        expectedCanvas.requiredStatementMap = new RequiredStatementMap(
                new LanguageMap(LanguageMap.DEFAULT_METADATA_KEY, ATTRIBUTION_STRING),
                new LanguageMap(LanguageMap.DEFAULT_METADATA_KEY, "<span>wr3Attribution</span>"));
        expectedCanvas.rightsId = "wr3License";
        expectedCanvas.annoPageid = null; // we only set if for fulltext annopages
        expectedCanvas.annoPageType = "AnnotationPage";

        ExpectedAnnotationAndBodyValues expectedAnnotation = new ExpectedAnnotationAndBodyValues();
        expectedCanvas.annoPageAnnotationAndBody = new ExpectedAnnotationAndBodyValues[] {expectedAnnotation};
        expectedAnnotation.id = null;
        expectedAnnotation.type = "Annotation";
        expectedAnnotation.motivation = "painting";
        expectedAnnotation.timeMode = null; // as it's not AV no time mode should be set
        expectedAnnotation.target = "https://iiif.europeana.eu/presentation/test-id/canvas/p1";
        expectedAnnotation.bodyId = "wr3Id";
        expectedAnnotation.bodyType = "Image";
        expectedAnnotation.bodyFormat = "image/webp";
        expectedAnnotation.hasService = true;
        expectedAnnotation.bodyServiceId = "service3Id";
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
        expectedCanvas2.requiredStatementMap = new RequiredStatementMap(
                new LanguageMap(LanguageMap.DEFAULT_METADATA_KEY, ATTRIBUTION_STRING),
                new LanguageMap(LanguageMap.DEFAULT_METADATA_KEY, "<span>wr2Attribution</span>"));
        expectedCanvas2.rightsId = "wr2License";
        expectedCanvas2.annoPageid = null; // we only set if for fulltext annopages
        expectedCanvas2.annoPageType = "AnnotationPage";

        ExpectedAnnotationAndBodyValues expectedAnnotation2 = new ExpectedAnnotationAndBodyValues();
        expectedCanvas2.annoPageAnnotationAndBody = new ExpectedAnnotationAndBodyValues[] {expectedAnnotation2};
        expectedAnnotation2.id = null;
        expectedAnnotation2.type = "Annotation";
        expectedAnnotation2.motivation = "painting";
        expectedAnnotation2.timeMode = "trim"; // as it's AV
        expectedAnnotation2.target = "https://iiif.europeana.eu/presentation/test-id/canvas/p2";
        expectedAnnotation2.bodyId = "wr2Id";
        expectedAnnotation2.bodyType = "Sound";
        expectedAnnotation2.bodyFormat = "audio/mp4";
        expectedAnnotation2.hasService = false;
        checkCanvas(expectedCanvas2, canvas2);
    }

    private void checkCanvas(ExpectedCanvasAndAnnotationPageValues expected, Canvas canvas) {
        Assertions.assertNotNull(canvas);
        Assertions.assertTrue(canvas.getId().endsWith(expected.idEndsWith),
                "Expected id ending with " + expected.idEndsWith + " but id is "+canvas.getId());
        Assertions.assertEquals(expected.type, canvas.getType().get());
        testLanguageMap(expected.label, canvas.getLabel());
        Assertions.assertEquals(expected.duration, canvas.getDuration());
        testRequiredStatementMap(expected.requiredStatementMap, canvas.getRequiredStatement());
        if (expected.rightsId == null) {
            Assertions.assertNull(canvas.getRights());
        } else {
            Assertions.assertEquals(expected.rightsId, canvas.getRights().getId());
        }
        if (expected.annoPageAnnotationAndBody == null) {
            Assertions.assertNull(canvas.getItems());
        } else {
            Assertions.assertNotNull(canvas.getItems());
            AnnotationPage ap = canvas.getItems()[0];
            Assertions.assertNotNull(ap);
            Assertions.assertEquals(expected.annoPageid, ap.getId());
            Assertions.assertEquals(expected.annoPageType, ap.getType().get());
            checkAnnotationAndBodyAndServiceValues(expected.annoPageAnnotationAndBody, ap.getItems());
        }
    }

    private void checkAnnotationAndBodyAndServiceValues(ExpectedAnnotationAndBodyValues[] expectedAnnotations, Annotation[] annotations) {
        Assertions.assertNotNull(annotations);
        Assertions.assertNotNull(expectedAnnotations);
        for (int i = 0; i < expectedAnnotations.length; i++) {
            ExpectedAnnotationAndBodyValues expected = expectedAnnotations[i];
            Annotation annotation = annotations[i];
            Assertions.assertEquals(expected.id, annotation.getId(), "Annotation id");
            Assertions.assertEquals(expected.type, annotation.getType().get(), "Annotation type");
            Assertions.assertEquals(expected.motivation, annotation.getMotivation(), "Annotation motivation");
            Assertions.assertEquals(expected.timeMode, annotation.getTimeMode(), "Annotation timeMode");
            Assertions.assertEquals(expected.target, annotation.getTarget(), "Annotation target");
            AnnotationBody body = annotation.getBody();
            Assertions.assertNotNull(body); // body should always be present
            Assertions.assertEquals(expected.bodyId, body.getId(), "AnnotationBody id");
            Assertions.assertEquals(expected.bodyType, body.getType().get(), "AnnotationBody type");
            Assertions.assertEquals(expected.bodyFormat, body.getFormat(), "AnnotationBody format");
            if (expected.hasService) {
                Service service = body.getService();
                Assertions.assertNotNull(service);
                Assertions.assertEquals(expected.bodyServiceId, service.getId(), "Service id");
                Assertions.assertEquals(expected.bodyServiceType, service.getType().get(), "Service type");
                Assertions.assertEquals(expected.bodyServiceProfile, service.getProfile(), "Service profile");
            } else {
                Assertions.assertNull (annotation.getBody().getService());
            }

        }
    }

    private static class ExpectedCanvasAndAnnotationPageValues {
        String idEndsWith;
        String type;
        LanguageMap label;
        Double duration;
//        LanguageMap attribution;
        RequiredStatementMap requiredStatementMap;
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
     * Test if the provided RequiredStatementMap contains particular keys and sets of values
     */
    protected void testRequiredStatementMap(String expectedKey, String[] expectedValues, RequiredStatementMap requiredStatementMap) {

        Assertions.assertNotNull(requiredStatementMap);
        if (expectedKey == null) {
            Assertions.assertEquals(0, requiredStatementMap.size());
            Assertions.assertNull(expectedValues, "Expected value cannot be set when there is no key!");
        } else {
            Assertions.assertTrue(requiredStatementMap.containsKey("label"), "Key 'label' not found!");
            Assertions.assertTrue(requiredStatementMap.containsKey("value"), "Key 'value' not found!");
            LanguageMap labelMap = requiredStatementMap.get("label");
            Assertions.assertTrue(labelMap.containsKey(expectedKey), "Key '" + expectedKey + "' not found in label of Requiredstatement!");
            Assertions.assertEquals(labelMap.get(expectedKey)[0], ATTRIBUTION_STRING);

            LanguageMap valueMap = requiredStatementMap.get("value");
            Assertions.assertTrue(valueMap.containsKey(expectedKey), "Key '" + expectedKey + "' not found in label of Requiredstatement!");

            for (int i = 0; i < expectedValues.length; i++) {
                Assertions.assertEquals(valueMap.get(expectedKey)[i], expectedValues[i]);
            }
        }
    }

    /**
     * Test if the provided RequiredStatementMaps contains the same two LanguageMaps
     */
    protected void testRequiredStatementMap(RequiredStatementMap expectedMap, RequiredStatementMap map) {
        if (expectedMap == null) {
            Assertions.assertNull(map);
        } else {
            Assertions.assertEquals(2, map.size());
            for (String expectedKey : expectedMap.keySet()) {
                testLanguageMap(expectedMap.get(expectedKey), map.get(expectedKey));
            }
        }
    }

    /**
     * Test if the provided languageMap contains a particular key and set of values. Ordering of values is also checked
     */
    protected static void testLanguageMap(String expectedKey, String[] expectedValues, LanguageMap map) {
        Assertions.assertNotNull(map);
        if (expectedKey == null) {
            Assertions.assertEquals(0, map.size());
            Assertions.assertNull(expectedValues, "Expected value cannot be set when there is no key!");
        } else {
            Assertions.assertTrue(map.containsKey(expectedKey), "Key '" + expectedKey + "' not found!");
            String[] values = map.get(expectedKey);
            if (expectedValues == null) {
                Assertions.assertNull(values);
            } else {
                for (int i = 0; i < expectedValues.length; i++) {
                    Assertions.assertEquals(values[i], expectedValues[i]);
                }
            }
        }
    }

    private void testLanguageMap(LanguageMap expectedMap, LanguageMap map) {
        if (expectedMap == null) {
            Assertions.assertNull(map);
        } else {
            for (String expectedKey : expectedMap.keySet()) {
                testLanguageMap(expectedKey, expectedMap.get(expectedKey), map);
            }
        }
    }
}
