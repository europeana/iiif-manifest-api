package eu.europeana.iiif.model.v3;

import java.io.Serializable;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Patrick Ehlert
 * Created on 24-01-2018
 */
public class LanguageMap extends LinkedHashMap<String, String[]> implements Serializable {

    private static final long serialVersionUID = -7678917507346373456L;

    private static final String NO_LANGUAGE_KEY = "@none";

    public LanguageMap() {
        // empty constructor to allow deserializing
    }

    public LanguageMap(String language, String[] value) {
        if (language == null || language.isEmpty() || "def".equalsIgnoreCase(language)) {
            this.put(NO_LANGUAGE_KEY, value);
        } else {
            this.put(language, value);
        }
    }

    public LanguageMap(String language, String value) {
        String[] values = new String[0];
        values[0] = value;
        if (language == null || language.isEmpty() || "def".equalsIgnoreCase(language)) {
            this.put(NO_LANGUAGE_KEY, values);
        } else {
            this.put(language, values);
        }
    }

    public LanguageMap(String[] values) {
        this.put(NO_LANGUAGE_KEY, values);
    }

    public LanguageMap(String value) {
        String[] values = new String[0];
        values[0] = value;
        this.put(NO_LANGUAGE_KEY, values);
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
