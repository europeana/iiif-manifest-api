package eu.europeana.iiif.model.v3;

import java.util.*;

/**
 * Hash map that stores languages as key and text strings in that language as values
 * The map supports automatically merging existing key and values using the 'put' method
 * @author Patrick Ehlert
 * Created on 24-01-2018
 */
public class LanguageMap extends LinkedHashMap<String, String[]> {

    private static final long serialVersionUID = -7678917507346373456L;

    public static final String NO_LANGUAGE_KEY = "@none";
    public static final String DEFAULT_METADATA_KEY = "en";

    public LanguageMap() {
        super();
        // empty constructor to allow deserializing
    }

    public LanguageMap(String language, String[] value) {
        super();
        this.put(checkKey(language), value);
    }

    public LanguageMap(String language, String value) {
        super();
        String[] values = new String[]{value};
        this.put(checkKey(language), values);
    }

    public LanguageMap(String[] values) {
        this(NO_LANGUAGE_KEY, values);
    }

    public LanguageMap(String value) {
        this(NO_LANGUAGE_KEY, value);
    }

    /**
     * Can we use the provided key, or should we use @none?
     * @return key that should be used for storing values
     */
    private String checkKey(String key) {
        if (key == null || key.isEmpty() || "def".equalsIgnoreCase(key)) {
            return NO_LANGUAGE_KEY;
        }
        return key;
    }

    /**
     * If the provided key isn't in the map yet, then this works as a normal map 'put' operation, otherwise the
     * existing and new values for the key are merged.
     * Also if the key is null or empty or then we use the default '@None' key instead
     * @param key key to insert
     * @param values values to insert (or merge with existing values if the key already exists)
     */
    @Override
    // Note that we need to override the base put-method because we need to make sure we check the key and modify it
    // if necessary when deserializing record json data
    public final String[] put(final String key, final String[] values) {
        String storeKey = checkKey(key);
        String[] storeValues = values;
        // check if key already exists, if so we have to re-insert the values with the new value(s) added
        // TODO this doesn't take duplicate values into account, but for now we ignore that
        if (this.containsKey(storeKey)) {
            List<String> newValues = new ArrayList<>();
            newValues.addAll(Arrays.asList(this.get(storeKey)));
            newValues.addAll(Arrays.asList(values));
            storeValues = newValues.toArray(new String[0]);
        }
        return super.put(storeKey, storeValues);
    }

    /**
     * Merges al key-value pairs from the provided map into this map
     * @param map languagemap to merge into this map
     */
    public void put(LanguageMap map) {
        for (Map.Entry<String, String[]> entryToAdd : map.entrySet()) {
            this.put(entryToAdd.getKey(), entryToAdd.getValue());
        }
    }

    /**
     * @return textual representation of the contents of the language map (for debugging purposes)
     */
    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append('(');
        for (Map.Entry<String, String[]> entry : this.entrySet()) {
            if (s.length() > 1) {
                s.append(", ");
            }
            String language = entry.getKey();
            String[] values = entry.getValue();
            s.append('{').append(language).append('=').append(Arrays.toString(values)).append('}');
        }
        s.append(')');
        return s.toString();
    }
}
