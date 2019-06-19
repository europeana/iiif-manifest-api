package eu.europeana.iiif.model.v3;

import java.io.Serializable;

/**
 * @author Patrick Ehlert
 * Created on 24-01-2018
 */
public class Annotation extends JsonLdIdType implements Serializable {

    private static final long serialVersionUID = -2420858050000556844L;

    private String motivation = "painting";
    private String timeMode;
    private AnnotationBody body;
    private String target;


    public Annotation(String id) {
        super(id, "Annotation");
    }

    public String getMotivation() {
        return motivation;
    }

    public String getTimeMode() {
        return timeMode;
    }

    public void setTimeMode(String timeMode) {
        this.timeMode = timeMode;
    }

    public AnnotationBody getBody() {
        return body;
    }

    public void setBody(AnnotationBody body) {
        this.body = body;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }
}
