package eu.europeana.iiif.model.v3;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
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
    public final String[] put(String key, String value[]) {
        return super.put(key, value);
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
