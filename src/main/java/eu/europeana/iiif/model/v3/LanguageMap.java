package eu.europeana.iiif.model.v3;

import java.io.Serializable;
import java.util.*;

/**
 * @author Patrick Ehlert
 * Created on 24-01-2018
 */
public class LanguageMap extends LinkedHashMap<String, String[]> implements Serializable {

    private static final long serialVersionUID = -7678917507346373456L;

    public static final String NO_LANGUAGE_KEY = "@none";
    public static final String DEFAULT_METADATA_KEY = "en";

    public LanguageMap() {
        super();
        // empty constructor to allow deserializing
    }

    public LanguageMap(String language, String[] value) {
        super();
        if (language == null || language.isEmpty() || "def".equalsIgnoreCase(language)) {
            this.put(NO_LANGUAGE_KEY, value);
        } else {
            this.put(language, value);
        }
    }

    public LanguageMap(String language, String value) {
        super();
        String[] values = new String[1];
        values[0] = value;
        if (language == null || language.isEmpty() || "def".equalsIgnoreCase(language)) {
            this.put(NO_LANGUAGE_KEY, values);
        } else {
            this.put(language, values);
        }
    }

    public LanguageMap(String[] values) {
        super();
        this.put(NO_LANGUAGE_KEY, values);
    }

    public LanguageMap(String value) {
        super();
        String[] values = new String[1];
        values[0] = value;
        this.put(NO_LANGUAGE_KEY, values);
    }

    /* Added put as final because we use it in our constructor and constructors shouldn't call overridable methods, see squid:S1699 */
    @Override
    public final String[] put(final String key, final String[] values) {
        String storeKey = key;
        // can we use the current language key, or should we set @none?
        if (storeKey == null || storeKey.isEmpty() || "def".equalsIgnoreCase(storeKey)) {
            storeKey = NO_LANGUAGE_KEY;
        }
        // check if key already exists, if so we have to re-insert the values with the new value(s) added
        String[] storeValues = values;
        if (this.containsKey(storeKey)) {
            List<String> newValues = new ArrayList<>();
            newValues.addAll(Arrays.asList(this.get(storeKey)));
            newValues.addAll(Arrays.asList(values));
            storeValues = newValues.toArray(new String[0]);
        }
        return super.put(storeKey, storeValues);
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
