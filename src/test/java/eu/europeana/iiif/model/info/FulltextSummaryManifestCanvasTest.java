package eu.europeana.iiif.model.info;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class FulltextSummaryManifestCanvasTest {

    @Test
    public void testPageId() {
        String idNewStyle = "https://fulltext-acceptance.eanadev.org/presentation/9200396/BibliographicResource_3000118436165/canvas/fde35?lang=fr";
        FulltextSummaryCanvas test = new FulltextSummaryCanvas(idNewStyle);
        Assertions.assertEquals("fde35", test.getPageNumber());

        String idOldStyle = "https://fulltext-acceptance.eanadev.org/presentation/9200396/BibliographicResource_3000118436165/canvas/100";
        FulltextSummaryCanvas test2 = new FulltextSummaryCanvas(idOldStyle);
        Assertions.assertEquals("100", test2.getPageNumber());

        String idIncorrect = "https://fulltext-acceptance.eanadev.org/presentation/9200396/BibliographicResource_3000118436165/100";
        FulltextSummaryCanvas test3 = new FulltextSummaryCanvas(idIncorrect);
        Assertions.assertNull(test3.getPageNumber());
    }
}
