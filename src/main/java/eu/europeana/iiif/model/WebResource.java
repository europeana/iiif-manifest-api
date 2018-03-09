package eu.europeana.iiif.model;

import org.springframework.util.StringUtils;

import java.util.HashMap;

/**
 * Class to help retrieve and sort web resources received from record JSON data
 * @author Patrick Ehlert
 * Created on 07-03-2018
 */
public class WebResource extends HashMap<String, Object> {

    private static final String EDM_ID = "about";
    private static final String EDM_NEXT_IN_SEQUENCE = "isNextInSequence";

    public WebResource() {
        // default constructor
    }

    /**
     * Create new webresource (for testing)
     * @param id
     * @param isNextInSequence
     */
    public WebResource(String id, String isNextInSequence) {
        this.put(EDM_ID, id);
        this.put(EDM_NEXT_IN_SEQUENCE, isNextInSequence);
    }

    /**
     * @return the id of the webresource (in edm 'about' value)
     */
    public String getId() {
        return (String) this.get(EDM_ID);
    }

    /**
     * @return true if the webresource has a isNextInSequence key with a non-empty value
     */
    public boolean hasNextInSequence() {
        return !StringUtils.isEmpty(this.getNextInSequence());
    }

    /**
     * @return the value of the isNextInSequence key, or null if there is no key
     */
    public String getNextInSequence() {
        return (String) this.get(EDM_NEXT_IN_SEQUENCE);
    }

}
