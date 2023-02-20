package eu.europeana.iiif.model.v3;

import java.util.LinkedHashMap;

/**
 * Created by luthien on 15/02/2023.
 */
public class RequiredStatementMap extends LinkedHashMap<String, LanguageMap> {

    private static final long serialVersionUID = 905716545704976007L;

    public static final String LABEL = "label";
    public static final String VALUE = "value";

    public RequiredStatementMap() {
        super();
        // empty constructor to allow deserializing
    }

    /**
     * Creates a Map<String, LanguageMap> as required for requiredStatement in EA-3324
     */
    public RequiredStatementMap(LanguageMap labelMap, LanguageMap valueMap){
        super();
        this.put(LABEL, labelMap);
        this.put(VALUE, valueMap);

    }

    // just in case we want to do checking, similar as in LanguageMap
    @Override
    public final LanguageMap put(final String key, final LanguageMap valueMap) {
        String storeKey = key;
        LanguageMap storeValue = valueMap;
        return super.put(storeKey, storeValue);
    }


}
