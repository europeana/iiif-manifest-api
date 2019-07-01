package eu.europeana.iiif.service;

import eu.europeana.iiif.model.v2.LanguageObject;
import eu.europeana.iiif.model.v3.LanguageMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Utility class for processing language maps or language objects;
 * @author Patrick Ehlert
 * Created on 27-06-2019
 */
public final class LanguageMapUtils {

    private LanguageMapUtils() {
        // empty constructor to prevent initialization
    }

    /**
     * This merges an array of languagemaps into a single languagemap. We also check for empty maps and return null if
     * the provided array is empty
     */
    public static LanguageMap mergeLanguageMaps(LanguageMap[] maps) {
        if (maps == null || maps.length == 0) {
            return null;
        } else if (maps.length == 1) {
            return maps[0];
        }
        LanguageMap result = new LanguageMap();
        for (LanguageMap map : maps) {
            result.put(map);
        }
        return result;
    }

    /**
     * This converts a LanguageMap array (v3) to a LanguageObject array (v2).
     * @param map language map to change into language object
     * @return array of language objects
     */
    public static LanguageObject[] langMapToObjects(LanguageMap map) {
        if (map == null) {
            return new LanguageObject[0];
        }
        List<LanguageObject> result = new ArrayList<>();
        for (Map.Entry<String, String[]> entry : map.entrySet()) {
            String language = entry.getKey();
            String[] values = entry.getValue();
            for (String value: values) {
                result.add(new LanguageObject(language, value));
            }
        }
        if (result.isEmpty()) {
            return new LanguageObject[0];
        }
        return result.toArray(new LanguageObject[0]);
    }
}
