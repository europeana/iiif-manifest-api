package eu.europeana.iiif.model.v3;

import eu.europeana.iiif.service.LanguageMapUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;

/**
 * Test if creating new language maps works fine. If no language or 'def' is specified strings should be filled under
 * the special '@none' key
 * @author Patrick Ehlert
 * Created on 19-06-2019
 */
public class LanguageMapTest {

    @Test
    public void createLanguageMap() {
        String test1 = "some test string";
        LanguageMap l = new LanguageMap(test1);
        Assertions.assertEquals(1, l.size());
        Assertions.assertEquals(1, l.get(LanguageMap.NO_LANGUAGE_KEY).length);
        Assertions.assertEquals("({@none=[some test string]})", l.toString());

        String test2 = "yet another test string";
        l.put(null, new String[]{test2});
        Assertions.assertEquals(1, l.size());
        Assertions.assertEquals(2, l.get(LanguageMap.NO_LANGUAGE_KEY).length);
        Assertions.assertEquals("({@none=[some test string, yet another test string]})", l.toString());

        String test3 = "third one";
        l.put("def", new String[]{test3});
        Assertions.assertEquals(1, l.size());
        Assertions.assertEquals(3, l.get(LanguageMap.NO_LANGUAGE_KEY).length);
        Assertions.assertEquals("({@none=[some test string, yet another test string, third one]})", l.toString());

        String test4 = "are we done now?";
        l.put("en", new String[]{test4});
        Assertions.assertEquals(2, l.size());
        Assertions.assertEquals(1, l.get("en").length);
        Assertions.assertEquals("({@none=[some test string, yet another test string, third one]}, {en=[are we done now?]})", l.toString());
    }

    @Test
    public void mergeLanguageMap() {
        LanguageMap l1 = new LanguageMap(LanguageMap.DEFAULT_METADATA_KEY, new String[]{"A string"});
        l1.put("duplicateKey", new String[]{"value1", "value2"});
        LanguageMap l2 = new LanguageMap("ru", new String[]{"Another string"});
        l2.put("duplicateKey", new String[]{"value3", "value4"});

        LanguageMap merged = LanguageMapUtils.mergeLanguageMaps(new LanguageMap[]{l1, l2});
        Assertions.assertEquals(3, merged.size());
        Assertions.assertEquals(3, merged.values().size());
        Assertions.assertEquals("({en=[A string]}, {duplicateKey=[value1, value2, value3, value4]}, {ru=[Another string]})", merged.toString());
    }
}
