package eu.europeana.iiif.model.v3;

import org.junit.Test;

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
        assertEquals(1, l.size());
        assertEquals(1, l.get(LanguageMap.NO_LANGUAGE_KEY).length);
        assertEquals("({@none=[some test string]})", l.toString());

        String test2 = "yet another test string";
        l.put(null, new String[]{test2});
        assertEquals(1, l.size());
        assertEquals(2, l.get(LanguageMap.NO_LANGUAGE_KEY).length);
        assertEquals("({@none=[some test string, yet another test string]})", l.toString());

        String test3 = "third one";
        l.put("def", new String[]{test3});
        assertEquals(1, l.size());
        assertEquals(3, l.get(LanguageMap.NO_LANGUAGE_KEY).length);
        assertEquals("({@none=[some test string, yet another test string, third one]})", l.toString());

        String test4 = "are we done now?";
        l.put("en", new String[]{test4});
        assertEquals(2, l.size());
        assertEquals(1, l.get("en").length);
        assertEquals("({@none=[some test string, yet another test string, third one]}, {en=[are we done now?]})", l.toString());
    }
}
