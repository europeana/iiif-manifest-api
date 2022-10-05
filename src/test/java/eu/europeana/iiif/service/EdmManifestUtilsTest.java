package eu.europeana.iiif.service;

import com.jayway.jsonpath.Configuration;
import eu.europeana.iiif.model.v3.LanguageMap;
import eu.europeana.iiif.model.v3.Text;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = {EdmManifestUtils.class})
public class EdmManifestUtilsTest {

    private static EdmManifestUtils edmManifestUtils;

    @Test
    public void isUrlTest() {
        Assertions.assertTrue(edmManifestUtils.isUrl("https://testing"));
        Assertions.assertTrue(edmManifestUtils.isUrl("http://testing"));
        Assertions.assertTrue(edmManifestUtils.isUrl("ftp://testing"));
        Assertions.assertTrue(edmManifestUtils.isUrl("file://testing"));
        Assertions.assertFalse(edmManifestUtils.isUrl("test://testing"));
        Assertions.assertFalse(edmManifestUtils.isUrl("123://testing"));
    }

    @Test
    public void getThumbnailIdTest() {
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(EdmManifestData.TEST_THUMBNAIL);
        String thumbnailId = edmManifestUtils.getThumbnailId(null, document);
        Assertions.assertEquals(EdmManifestData.TEST_THUMBNAIL_ID, thumbnailId);
    }

    /**
     * Test if we output a date in the proper format
     */
    @Test
    public void testNavDate() {
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(EdmManifestData.TEST_NAVDATE);
        String navDate = EdmManifestUtils.getNavDate("test", document);
        Assertions.assertNotNull(navDate);
        Assertions.assertEquals("1922-03-15T00:00:00Z", navDate);
    }

    /**
     * Test if we handle non-existing navDates properly
     */
    @Test
    public void testNavDateEmpty() {
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(EdmManifestData.TEST_EMPTY);
        Assertions.assertNull(EdmManifestUtils.getNavDate("test", document));
    }


    /**
     * Test if we retrieve and set a homepage field properly
     */
    @Test
    public void testHomepage() {
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(EdmManifestData.TEST_HOMEPAGE);
        Text[] text = edmManifestUtils.getHomePage("test", document);
        Assertions.assertNotNull(text);
        Assertions.assertEquals(1, text.length);
        Assertions.assertEquals(EdmManifestData.TEST_HOMEPAGE_ID, text[0].getId());
        EdmManifestV3MappingTest.testLanguageMap(LanguageMap.DEFAULT_METADATA_KEY, new String[]{"Europeana"}, text[0].getLabel());
        Assertions.assertEquals("Text", text[0].getType().get());
        Assertions.assertEquals("text/html", text[0].getFormat());
    }

    /**
     * Test if we handle non-existing landingPages properly
     */
    @Test
    public void testHomepageEmpty() {
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(EdmManifestData.TEST_EMPTY);
        Assertions.assertNull(edmManifestUtils.getHomePage("test", document));
    }

    @Test
    public void getValueFromDataProviderAggregationTest(){
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(EdmManifestData.TEST_SEQUENCE_3CANVAS_1SERVICE);
        String edmIsShownBy = edmManifestUtils.getValueFromDataProviderAggregation(document, null, "edmIsShownBy");
        Assertions.assertNotNull(edmIsShownBy);
        Assertions.assertEquals("wr3Id", edmIsShownBy);

        edmIsShownBy = edmManifestUtils.getValueFromDataProviderAggregation(document, null, "edmIsShownAt");
        Assertions.assertNull(edmIsShownBy);

        String about = edmManifestUtils.getValueFromDataProviderAggregation(document, null, "about");
        Assertions.assertNotNull(about);
        Assertions.assertEquals("/aggregation/provider/testing", about);
    }

    @Test
    public void getValueFromDataProviderAggregation_MultipleProxyAggTest(){
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(EdmManifestData.TEST_SEQUENCE_MULTIPLE_PROXY_AGG);
        String edmIsShownBy = edmManifestUtils.getValueFromDataProviderAggregation(document, null, "edmIsShownBy");
        System.out.println(edmIsShownBy);
        Assertions.assertNotNull(edmIsShownBy);
        Assertions.assertEquals("provider_edmIsShownBy", edmIsShownBy);

        edmIsShownBy = edmManifestUtils.getValueFromDataProviderAggregation(document, null, "edmIsShownAt");
        Assertions.assertNull(edmIsShownBy);

        String about = edmManifestUtils.getValueFromDataProviderAggregation(document, null, "about");
        Assertions.assertNotNull(about);
        Assertions.assertEquals("/aggregation/provider/1/", about);
    }

    @Test
    public void getDataProviderFromProxyWithOutLineageTest(){
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(EdmManifestData.TEST_SEQUENCE_3CANVAS_1SERVICE);
        Assertions.assertEquals("/aggregation/provider/testing", edmManifestUtils.getDataProviderFromProxyWithOutLineage(document, null));
    }

    @Test
    public void getDataProviderFromProxyWithOutLineage_MultipleProxyAggTest(){
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(EdmManifestData.TEST_SEQUENCE_MULTIPLE_PROXY_AGG);
        Assertions.assertEquals("/aggregation/provider/1/", edmManifestUtils.getDataProviderFromProxyWithOutLineage(document, null));
    }

    @Test
    public void getDataProviderFromProxyWithOutLineage_MultipleProxyInTest(){
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(EdmManifestData.MULTIPLE_PROXY_IN);
        Assertions.assertEquals("/first/provider/test", edmManifestUtils.getDataProviderFromProxyWithOutLineage(document, null));
    }
}
