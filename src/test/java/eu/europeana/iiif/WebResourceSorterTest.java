package eu.europeana.iiif;

import eu.europeana.iiif.model.WebResource;
import eu.europeana.iiif.model.WebResourceSorter;
import eu.europeana.iiif.service.exception.DataInconsistentException;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Patrick Ehlert
 * Created on 08-03-2018
 */
public class WebResourceSorterTest {

    private static final WebResource[] SEQUENCE1 = new WebResource[]{ new WebResource("1", "2"),
                                                                      new WebResource("2", null) };
    private static final WebResource[] SEQUENCE2 = new WebResource[]{ new WebResource("3", "4"),
                                                                      new WebResource("4", "5"),
                                                                      new WebResource("5", null) };
    private static final WebResource ISOLATED1 = new WebResource("iso1", null);
    private static final WebResource ISOLATED2 = new WebResource("iso2", null);
    private static final WebResource ISOLATED3 = new WebResource("iso3", null);
    private static final WebResource ISOLATED4 = new WebResource("iso4", null);
    private static final WebResource ISOLATED5 = new WebResource("iso5", "6");

    private static final List<String> orderViews1 = new ArrayList<>(Arrays.asList("1", "2", "3", "4", "5", "iso2", "iso1", "iso3", "iso4", "iso5"));
    private static final List<String> orderViews2 = new ArrayList<>(Arrays.asList("5", "2", "3", "4", "1", "iso2", "iso1", "iso5", "iso4", "iso3"));
    private static final List<String> orderViews3 = new ArrayList<>(Arrays.asList("iso4", "2", "3", "4", "1", "iso1", "iso2", "iso3", "5", "iso5"));


    /**
     * @param webResources list of webresources to check
     * @return true if the webresource with wrId1 is found after the webresource with wrId2 in the webResources array
     */
    private boolean isAfter(List<WebResource> webResources, String wrId1, String wrId2) {
        int pos1 = -1;
        int pos2 = -1;
        for (int i = 0; i < webResources.size() && (pos1 == -1 || pos2 == -1); i++) {
            WebResource wr = webResources.get(i);
            if (wrId1.equals(wr.getId())) {
                pos1 = i;
            }
            if (wrId2.equals(wr.getId())) {
                pos2 = i;
            }
            //LogManager.getLogger(WebResourceSorterTest.class).debug("i = {}, pos wrId1 {}, wrId2 {}, result = {}", i, wrId1, wrId2, (pos1 > pos2));
        }
        if (pos1 == -1) {
            throw new IllegalArgumentException("Webresource "+wrId1+" was not found!");
        }
        if (pos2 == -1) {
            throw new IllegalArgumentException("Webresource "+wrId2+" was not found!");
        }
        return pos1 > pos2;
    }

    /**
     * Test if we get the expected order for a 'normal' test case
     * @throws DataInconsistentException when the data is inconsistent
     */
    @Test
    public void sortNormal() throws DataInconsistentException {
        // 2 sequences and 2 isolated nodes
        List<WebResource> test = new ArrayList<>();
        test.add(ISOLATED1);
        test.addAll(Arrays.asList(SEQUENCE1));
        test.add(ISOLATED2);
        test.addAll(Arrays.asList(SEQUENCE2));
        List<WebResource> wrs = WebResourceSorter.sort(test, orderViews1);
        assertTrue(isAfter(wrs, "1", "2"));
        assertTrue(isAfter(wrs, "3", "4"));
        assertTrue(isAfter(wrs, "4", "5"));
        assertTrue(isAfter(wrs, "iso1", "2"));
        assertTrue(isAfter(wrs, "iso1", "5"));
        assertTrue(isAfter(wrs, "iso2", "2"));
        assertTrue(isAfter(wrs, "iso2", "5"));
        assertTrue(isAfter(wrs, "iso1", "iso2"));
    }

    /**
     * Check if we can handle a list of webresources that have no sequences properly
     * @throws DataInconsistentException when the data is inconsistent
     */
    @Test
    public void sortOnlyIsolated() throws DataInconsistentException {
        WebResource[] isolated = new WebResource[]{ ISOLATED1, ISOLATED2, ISOLATED3, ISOLATED4};
        List<WebResource> wrs = WebResourceSorter.sort(Arrays.asList(isolated), orderViews1);
        assertNotNull(wrs);
        assertTrue(isAfter(wrs, "iso1", "iso2"));
        assertTrue(isAfter(wrs, "iso4", "iso3"));
        assertTrue(isAfter(wrs, "iso3", "iso1"));
        assertTrue(isAfter(wrs, "iso4", "iso2"));
    }

    /**
     * Test if we handle an empty list of webresources properly
     * @throws DataInconsistentException when the data is inconsistent
     */
    @Test
    public void sortEmpty() throws DataInconsistentException {
        List<WebResource> emptyList = new ArrayList<>();
        List<WebResource> wrs = WebResourceSorter.sort(emptyList,orderViews1);
        assertNotNull(wrs);
    }

    /**
     * Test if an error is thrown for data that has infinite loops
     * @throws DataInconsistentException when the data is inconsistent
     */
    @Test(expected = DataInconsistentException.class)
    public void sortInfiniteLoopTest() throws DataInconsistentException {
        WebResource[] infiniteLoop = new WebResource[]{
                new WebResource("1", "2"),
                new WebResource("2", "3"),
                new WebResource("3", "1")};
        WebResourceSorter.sort(Arrays.asList(infiniteLoop),orderViews1);
    }

    /**
     * Test if an error is thrown if a nextInSequence webresource doesn't exist
     * @throws DataInconsistentException when the data is inconsistent
     */
    @Test(expected = DataInconsistentException.class)
    public void sortIncompleteSequence() throws DataInconsistentException {
        WebResource[] incomplete = new WebResource[]{
                new WebResource("1", "2"),
                new WebResource("2", "3"),
                new WebResource("3", "4")};
        WebResourceSorter.sort(Arrays.asList(incomplete),orderViews1);
    }

    /**
     * Test if an error is thrown if there are two intertwined sequences
     * @throws DataInconsistentException when the data is inconsistent
     */
    @Test(expected = DataInconsistentException.class)
    public void sortIntertwinedSequence() throws DataInconsistentException {
        WebResource[] intertwined = new WebResource[]{
                new WebResource("1", "2"),
                new WebResource("2", "3"),
                new WebResource("5", "4"),
                new WebResource("4", "3")};
        WebResourceSorter.sort(Arrays.asList(intertwined),orderViews1);
    }

    /**
     * Test if an error is thrown if there isolated sequence contains nextSequenceID
     * @throws DataInconsistentException when the data is inconsistent
     */
    @Test(expected = DataInconsistentException.class)
    public void sortIsolatedSequence() throws DataInconsistentException {
        WebResource[] isolated = new WebResource[]{ ISOLATED1,ISOLATED5};
        WebResourceSorter.sort(Arrays.asList(isolated), orderViews1);
    }

    /**
     * Checks order of multiple sequences with the combination of isNextSequence + edmIsShownBy
     * If a record has 2 or more sequences, show the sequence that contains the edmIsShownBy first
     */
    @Test
    public void sortMultipleSequences() throws DataInconsistentException {
        // 2 sequences and 4 isolated nodes
        List<WebResource> test = new ArrayList<>();
        test.add(ISOLATED1);
        test.add(ISOLATED2);
        test.add(ISOLATED3);
        test.add(ISOLATED4);
        test.addAll(Arrays.asList(SEQUENCE1));
        test.addAll(Arrays.asList(SEQUENCE2));

        // when multilple sequences contains edmIsShownBy value
        List<WebResource> wrs = WebResourceSorter.sort(test, orderViews2);
        System.out.println(wrs);
        assertTrue(isAfter(wrs, "4", "5"));
        assertTrue(isAfter(wrs, "3", "4"));
        assertTrue(isAfter(wrs, "1", "2"));
        assertTrue(isAfter(wrs, "2", "5"));
        assertTrue(isAfter(wrs, "1", "5"));
        assertTrue(isAfter(wrs, "iso1", "2"));
        assertTrue(isAfter(wrs, "iso1", "5"));
        assertTrue(isAfter(wrs, "iso2", "2"));
        assertTrue(isAfter(wrs, "iso2", "5"));
        assertTrue(isAfter(wrs, "iso1", "iso2"));
        assertTrue(isAfter(wrs, "iso4", "iso1"));
        assertTrue(isAfter(wrs, "iso3", "iso4"));

        wrs.clear();

        // when multiple sequences does not contains edmIsShownBy value
        wrs = WebResourceSorter.sort(test, orderViews3);
        System.out.println(wrs);
        assertTrue(isAfter(wrs, "1", "2"));
        assertTrue(isAfter(wrs, "4", "5"));
        assertTrue(isAfter(wrs, "3", "4"));
        assertTrue(isAfter(wrs, "4", "2"));
        assertTrue(isAfter(wrs, "3", "2"));
        assertTrue(isAfter(wrs, "5", "2"));
        assertTrue(isAfter(wrs, "iso4", "2"));
        assertTrue(isAfter(wrs, "iso3", "5"));
        assertTrue(isAfter(wrs, "iso2", "3"));
        assertTrue(isAfter(wrs, "iso1", "1"));
        assertTrue(isAfter(wrs, "iso1", "iso4"));
        assertTrue(isAfter(wrs, "iso2", "iso4"));
        assertTrue(isAfter(wrs, "iso3", "iso4"));

    }
}
