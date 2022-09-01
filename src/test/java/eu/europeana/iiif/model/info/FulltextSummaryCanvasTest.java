package eu.europeana.iiif.model.info;


import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class FulltextSummaryCanvasTest {

    @Test
    public void testPageId() {
        String idNewStyle = "https://fulltext-acceptance.eanadev.org/presentation/9200396/BibliographicResource_3000118436165/canvas/fde35?lang=fr";
        FulltextSummaryCanvas test = new FulltextSummaryCanvas(idNewStyle);
        assertEquals("fde35", test.getPageNumber());

        String idOldStyle = "https://fulltext-acceptance.eanadev.org/presentation/9200396/BibliographicResource_3000118436165/canvas/100";
        FulltextSummaryCanvas test2 = new FulltextSummaryCanvas(idOldStyle);
        assertEquals("100", test2.getPageNumber());

        String idIncorrect = "https://fulltext-acceptance.eanadev.org/presentation/9200396/BibliographicResource_3000118436165/100";
        FulltextSummaryCanvas test3 = new FulltextSummaryCanvas(idIncorrect);
        assertNull(test3.getPageNumber());
    }
}
