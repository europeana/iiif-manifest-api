package eu.europeana.iiif;

import eu.europeana.iiif.model.WebResource;
import eu.europeana.iiif.model.WebResourceSorter;
import eu.europeana.iiif.service.exception.DataInconsistentException;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

    /**
     * @param webResources
     * @return true if the webresource with wrId1 is found after the webresource with wrId2 in the webResources array
     */
    public boolean isAfter(WebResource[] webResources, String wrId1, String wrId2) {
        int pos1 = -1;
        int pos2 = -1;
        for (int i = 0; i < webResources.length && (pos1 == -1 || pos2 == -1); i++) {
            WebResource wr = webResources[i];
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
     * @throws DataInconsistentException
     */
    @Test
    public void sortNormalTest() throws DataInconsistentException {
        // 2 sequences and 2 isolated nodes
        List<WebResource> test = new ArrayList<>();
        test.add(ISOLATED1);
        test.addAll(Arrays.asList(SEQUENCE1));
        test.add(ISOLATED2);
        test.addAll(Arrays.asList(SEQUENCE2));

        WebResource[] wrs = WebResourceSorter.sort(test.toArray(new WebResource[test.size()]));
        assertTrue(isAfter(wrs, "1", "2"));
        assertTrue(isAfter(wrs, "3", "4"));
        assertTrue(isAfter(wrs, "4", "5"));
        assertTrue(isAfter(wrs, "iso1", "2"));
        assertTrue(isAfter(wrs, "iso1", "5"));
        assertTrue(isAfter(wrs, "iso2", "2"));
        assertTrue(isAfter(wrs, "iso2", "5"));
    }

    /**
     * Test if an error is thrown for data that has infinite loops
     * @throws DataInconsistentException*
     */
    @Test(expected = DataInconsistentException.class)
    public void sortInfiniteLoopTest() throws DataInconsistentException {
        WebResource[] infiniteLoop = new WebResource[]{
                new WebResource("1", "2"),
                new WebResource("2", "3"),
                new WebResource("3", "1")};
        WebResource[] wrs = WebResourceSorter.sort(infiniteLoop);
    }

    /**
     * Test if an error is thrown if a nextInSequence webresource doesn't exist
     * @throws DataInconsistentException
     */
    @Test(expected = DataInconsistentException.class)
    public void sortIncompleteSequence() throws DataInconsistentException {
        WebResource[] incomplete = new WebResource[]{
                new WebResource("1", "2"),
                new WebResource("2", "3"),
                new WebResource("3", "4")};
        WebResource[] wrs = WebResourceSorter.sort(incomplete);
    }

    /**
     * Test if an error is thrown if there are two intertwined sequences
     * @throws DataInconsistentException
     */
    @Test(expected = DataInconsistentException.class)
    public void sortIntertwinedSequence() throws DataInconsistentException {
        WebResource[] intertwined = new WebResource[]{
                new WebResource("1", "2"),
                new WebResource("2", "3"),
                new WebResource("5", "4"),
                new WebResource("4", "3")};
        WebResource[] wrs = WebResourceSorter.sort(intertwined);
    }

}
