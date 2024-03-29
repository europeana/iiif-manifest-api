package eu.europeana.iiif;

import eu.europeana.iiif.config.ManifestSettings;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Tests if the ManifestSettings getAppVersion method works properly
 * @author Patrick Ehlert
 * Created on 27-06-2019
 */
@TestPropertySource(locations = "classpath:iiif-test.properties")
@SpringBootTest(classes = {ManifestSettings.class})
public class ManifestSettingsTest {

    @Autowired
    private ManifestSettings ms;

    /**
     * Test if the getAppVersion method always returns a value and doesn't throw an error
     */
    @Test
    public void testManifest() {
        Assertions.assertEquals("${project.version}", ms.getAppVersion());
    }

}
