package eu.europeana.iiif.service;

import com.jayway.jsonpath.Configuration;
import eu.europeana.iiif.config.ManifestSettings;
import eu.europeana.iiif.model.v2.*;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.junit.Assert.*;

/**
 * Tests the EDM-IIIF Manifest v2 mapping
 * @author Patrick Ehlert
 * Created on 13-02-2018
 */

@TestPropertySource("classpath:iiif-test.properties")
@SpringBootTest(classes = {EdmManifestMappingV2.class, ManifestSettings.class})
public class EdmManifestV2MappingTest {

    // Initialize the manifest service, because that will setup our default Jackson mapper configuration used in the tests
    private static final ManifestService ms = new ManifestService(new ManifestSettings());

    @Autowired
    private ManifestSettings settings;

    @Test
    public void testId() {
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(EdmManifestData.TEST_ID);
        Assertions.assertEquals("id", EdmManifestUtils.getEuropeanaId(document));
    }

    // EA-3325
//    @Test
//    public void testWithin() {
//        Object document = Configuration.defaultConfiguration().jsonProvider().parse(EdmManifestData.TEST_WITHIN);
//        Assertions.assertEquals("https://data.theeuropeanlibrary.org/someurl", EdmManifestMappingV2.getWithinV2(document));
//    }

//    @Test
//    public void testWithinEmpty() {
//        Object document = Configuration.defaultConfiguration().jsonProvider().parse(EdmManifestData.TEST_EMPTY);
//        Assertions.assertNull(EdmManifestMappingV2.getWithinV2(document));
//    }

     /**
     * Test if we get a title as a label, even when descriptions are present
     */
    @Test
    public void testLabelIsTitle() {
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(EdmManifestData.TEST_TITLE);
        LanguageObject[] labels = EdmManifestMappingV2.getLabelsV2(document);
        Assertions.assertNotNull(labels);
        Assertions.assertTrue(labels.length > 0);
        Assertions.assertNotNull(labels[0]);
        Assertions.assertEquals("en", labels[0].getLanguage());
        Assertions.assertEquals("Title", labels[0].getValue());
    }

    /**
     * Test if we get a description as a label, if no title is present
     */
    @Test
    public void testLabelIsDescription() {
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(EdmManifestData.TEST_DESCRIPTION);
        LanguageObject[] labels = EdmManifestMappingV2.getLabelsV2(document);
        Assertions.assertNotNull(labels);
        Assertions.assertTrue(labels.length > 0);
        Assertions.assertNotNull(labels[0]);
        Assertions.assertNull(labels[0].getLanguage());
        Assertions.assertEquals("Description", labels[0].getValue());
    }

    /**
     * Test if we handle missing title and description (so no label) properly
     */
    @Test
    public void testLabelEmpty() {
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(EdmManifestData.TEST_EMPTY);
        Assertions.assertNull(EdmManifestMappingV2.getLabelsV2(document));
    }

    /**
     * Test if we get a proper description, if there are a title and description defined (not necessarily in 1 proxy)
     */
    @Test
    public void testDescription() {
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(EdmManifestData.TEST_TITLE_DESCRIPTION);
        LanguageObject[] descriptions = EdmManifestMappingV2.getDescriptionV2(document);
        Assertions.assertNotNull(descriptions);
        Assertions.assertTrue(descriptions.length > 0);
        Assertions.assertNotNull(descriptions[0]);
        Assertions.assertNull(descriptions[0].getLanguage());
        Assertions.assertEquals("Description", descriptions[0].getValue());
    }

    /**
     * Test if they description is left out, if it's already used as a title (i.e. if there are no titles), or if there
     * are not descriptions at all.
     */
    @Test
    public void testDescriptionEmpty() {
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(EdmManifestData.TEST_DESCRIPTION);
        Assertions.assertNull(EdmManifestMappingV2.getDescriptionV2(document));

        document = Configuration.defaultConfiguration().jsonProvider().parse(EdmManifestData.TEST_TITLE); // no description
        Assertions.assertNull(EdmManifestMappingV2.getDescriptionV2(document));
    }

    /**
     * Test if we construct a metadata object properly
     */
    @Test
    public void testMetaData() {
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(EdmManifestData.TEST_METADATA_SIMPLE);
        MetaData[] metaData = EdmManifestMappingV2.getMetaDataV2(document);
        Assertions.assertNotNull(metaData);
        Assertions.assertEquals(2, metaData.length);

        MetaData meta1 = metaData[0];
        Assertions.assertNotNull(meta1);
        Assertions.assertEquals("format", meta1.getLabel());
        Assertions.assertEquals(1, meta1.getValue().length);

        LanguageObject meta1Value1 = meta1.getValue()[0];
        Assertions.assertNotNull(meta1Value1);
        Assertions.assertNull(meta1Value1.getLanguage());
        Assertions.assertEquals("SomeFormat", meta1Value1.getValue());

        MetaData meta2 = metaData[1];
        Assertions.assertNotNull(meta2);
        Assertions.assertEquals("type", meta2.getLabel());
        Assertions.assertEquals(2, meta2.getValue().length);

        LanguageObject meta2Value1 = meta2.getValue()[0];
        Assertions.assertNotNull(meta2Value1);
        Assertions.assertEquals("nl",meta2Value1.getLanguage());
        Assertions.assertEquals("Precies mijn type", meta2Value1.getValue());

        LanguageObject meta2Value2 = meta2.getValue()[1];
        Assertions.assertNotNull(meta2Value2);
        Assertions.assertEquals("en", meta2Value2.getLanguage());
        Assertions.assertEquals("Exactly my type as well", meta2Value2.getValue());
    }

    /**
     * Test if we handle non-existing metadata properly
     */
    @Test
    public void testMetaDataEmpty() {
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(EdmManifestData.TEST_EMPTY);
        Assertions.assertNull(EdmManifestMappingV2.getMetaDataV2(document));
    }

    /**
     * Test if we retrieve thumbnail image information properly
     */
    @Test
    public void testThumbnail() {
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(EdmManifestData.TEST_THUMBNAIL);
        Image image = EdmManifestMappingV2.getThumbnailImageV2("test", document);
        Assertions.assertNotNull(image);
        Assertions.assertEquals(EdmManifestData.TEST_THUMBNAIL_ID, image.getId());
    }

    /**
     * Test if we handle non-existing thumbnails properly
     */
    @Test
    public void testThumbnailEmpty() {
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(EdmManifestData.TEST_EMPTY);
        Assertions.assertNull(EdmManifestMappingV2.getThumbnailImageV2( "test", document));
    }

    /**
     * Test if we retrieve attribution properly
     */
    @Test
    public void testAttribution() {
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(EdmManifestData.TEST_ATTRIBUTION);
        String attribution = EdmManifestMappingV2.getAttributionV2("test", EdmManifestData.TEST_IS_SHOWN_BY, document);
        Assertions.assertNotNull(attribution);
        Assertions.assertEquals(EdmManifestData.TEST_ATTRIBUTION_TEXT_V2, attribution);
    }

    /**
     * Test if we handle non-existing attributions properly
     */
    @Test
    public void testAttributionEmpty() {
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(EdmManifestData.TEST_EMPTY);
        Assertions.assertNull(EdmManifestMappingV2.getAttributionV2("test", EdmManifestData.TEST_IS_SHOWN_BY, document));
    }

    /**
     * Test if we retrieve license text from europeana aggregation
     */
    @Test
    public void testLicenseFromEuropeanaAggregation() {
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(EdmManifestData.TEST_LICENSE_EUROPEANAAGGREGATION);
        String license = EdmManifestMappingV2.getLicense("test", document);
        Assertions.assertNotNull(license);
        Assertions.assertEquals("licenseTextEuropeana", license);
    }

    /**
     * Test if we retrieve license text from other aggregations (if there isn't any in the europeanaAggregation)
     */
    @Test
    public void testLicenseFromOtherAggregations() {
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(EdmManifestData.TEST_LICENSE_OTHERAGGREGATION);
        String license = EdmManifestMappingV2.getLicense("test", document);
        Assertions.assertNotNull(license);
        Assertions.assertEquals("licenseTextAggregation", license);
    }

    /**
     * Test if we retrieve license text from other aggregations (multiple aggregations) (if there isn't any in the europeanaAggregation)
     */
    @Test
    public void testLicenseFromOtherAggregations_MultipleProxyAgg() {
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(EdmManifestData.TEST_LICENSE_MULTIPLE_PROXY_AGGREGATION);
        String license = EdmManifestMappingV2.getLicense("test", document);
        Assertions.assertNotNull(license);
        Assertions.assertEquals("http://test.org/test/", license);
    }

    /**
     * Test if we handle non-existing license properly
     */
    @Test
    public void testLicenseEmpty() {
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(EdmManifestData.TEST_EMPTY);
        Assertions.assertNull(EdmManifestMappingV2.getLicense("test", document));
    }

    @Test
    public void isShownByMultipleProxyAgg() {
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(EdmManifestData.TEST_SEQUENCE_MULTIPLE_PROXY_AGG);
        String isShownBy = EdmManifestUtils.getValueFromDataProviderAggregation(document, null, "edmIsShownBy");
        Assertions.assertNotNull(isShownBy);
    }

    /**
     * Test if we generate the 3 seeAlso datasets with the correct ID
     */
    @Test
    public void testSeeAlso() {
        DataSet[] datasets = EdmManifestMappingV2.getDataSetsV2(settings, "TEST-ID");
        Assertions.assertNotNull(datasets);
        Assertions.assertEquals(3, datasets.length);
        for (DataSet dataset : datasets) {
            Assertions.assertTrue(dataset.getId().contains("TEST-ID"));
        }
    }

    /**
     * Test that we do not create a sequence if there are no webresources
     */
    @Test
    public void testSequenceEmpty() {
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(EdmManifestData.TEST_EMPTY);
        Assertions.assertNull(EdmManifestMappingV2.getSequencesV2(settings, "test", null, document));
    }

    /**
     * Test that we do not create a sequence if the webresource is not an edmIsShownAtField (or hasView)
     */
    @Test
    public void testSequenceMissingIsShownAtHasView() {
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(EdmManifestData.TEST_SEQUENCE_2CANVAS_NOISSHOWNBY);
        Assertions.assertNull(EdmManifestMappingV2.getSequencesV2(settings, "test", null, document));
    }

    /**
     * Test if we generate sequences (and it's containing objects) properly
     */
    @Test
    public void testSequence() {
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(EdmManifestData.TEST_SEQUENCE_3CANVAS_1SERVICE);
        String edmIsShownBy = EdmManifestUtils.getValueFromDataProviderAggregation(document, null, "edmIsShownBy");
        Sequence[] sequence = EdmManifestMappingV2.getSequencesV2(settings, "/test-id", edmIsShownBy, document);
        Assertions.assertNotNull(sequence);
        Assertions.assertEquals(1, sequence.length); // there should always be only 1 sequence

        // test canvas part
        Assertions.assertTrue(sequence[0].getStartCanvas().endsWith("/test-id" + "/canvas/p1"));
        Assertions.assertNotNull(sequence[0].getCanvases());
        // note that one of the 3 canvases is not edmIsShownBy or hasView so it's not included
        Assertions.assertEquals(2, sequence[0].getCanvases().length);

        // we only check the first canvas
        ExpectedCanvasValues ecv = new ExpectedCanvasValues();
        ecv.id = sequence[0].getStartCanvas();
        ecv.label = "p. 1";
        ecv.attribution = "wr3Attribution";
        ecv.license = "wr3License";
        ecv.annotationAndBody = new ExpectedAnnotationAndBodyValues();
        ecv.annotationAndBody.idEndsWith = "/test-id/annotation/p1";
        ecv.annotationAndBody.onId = ecv.id;
        ecv.annotationAndBody.bodyId = "wr3Id";
        ecv.annotationAndBody.format = "video/mp4";
        ecv.annotationAndBody.service = new ExpectedServiceValues();
        ecv.annotationAndBody.service.id = "service3Id";
        ecv.annotationAndBody.service.profile = "serviceProfile";

        checkCanvas(ecv, sequence[0].getCanvases()[0]);
    }

    /**
     * Test if we generate a canvas object (and it's containing objects) properly
     */
    public void checkCanvas(ExpectedCanvasValues ecv, Canvas c) {
        Assertions.assertEquals(ecv.id, c.getId());
        Assertions.assertEquals(ecv.label, c.getLabel());
        // TODO read height, width from configuration and check it
        Assertions.assertEquals(ecv.attribution, c.getAttribution());
        Assertions.assertEquals(ecv.license, c.getLicense());

        // test image/annotation part (can be at most 1)
        if (ecv.annotationAndBody == null) {
            Assertions.assertNull(c.getAttribution());
        } else {
            Assertions.assertNotNull(c.getAttribution());
            Assertions.assertEquals(1, c.getImages().length);
            checkAnnotationAndBody(ecv.annotationAndBody, c.getImages()[0]);
        }
    }

    /**
     * Test if we generate an annotation and annotation body object (and containing service object) properly
     */
    public void checkAnnotationAndBody(ExpectedAnnotationAndBodyValues eabv, Annotation ann) {
        Assertions.assertEquals(eabv.onId, ann.getOn());

        // test annotationBody part
        AnnotationBody body = ann.getResource();
        Assertions.assertNotNull(body);
        Assertions.assertEquals(eabv.bodyId, body.getId());
        Assertions.assertEquals(eabv.format, body.getFormat());

        // test service part
        if (eabv.service == null) {
            Assertions.assertNull(body.getService());
        } else {
            Assertions.assertNotNull(body.getService());
            checkService(eabv.service, body.getService());
        }
    }

    public void checkService(ExpectedServiceValues esv, Service s) {
        Assertions.assertEquals(esv.id, s.getId());
        Assertions.assertEquals(esv.profile, s.getProfile());
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

    @Test
    public void testRetrieveRecordUpdate() {
        Assertions.assertEquals(LocalDateTime.of(2017, 6, 6, 19, 40, 18, 82000000).atZone(ZoneOffset.UTC),
                EdmManifestUtils.getRecordTimestampUpdate(
                "{\"object\":{\"timestamp_update\":\"2017-06-06T19:40:18.082Z\"}}"));
    }

}
