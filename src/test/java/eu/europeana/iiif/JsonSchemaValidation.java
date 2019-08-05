package eu.europeana.iiif;


import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;


import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.Test;
import org.apache.commons.io.IOUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class JsonSchemaValidation {

     private static final StringBuffer validationResult =  new StringBuffer("Validation against Json schema failed: \n");
     private static final StringBuffer successResult =     new StringBuffer("Validation against Json Schema is success");
     private static final String       jsonSchemaV2 =      "manifest.IIIFv2.jschema";
     private static final String       jsonSubjectNull =   "Referred response must be injected before validation";
     private static final String       jsonObjectNull =    "Referred schema must be injected before validation";
     private static final String       schemaNull =        "Referred schema must be injected before validation";

    @Test
    public void validateJsonAgainstSchemaV2Success() throws IOException, JSONException {
        JSONObject jsonSubject = new JSONObject(new JSONTokener(IOUtils.toString(loadJsonFile("manifestIIIFv2_ValidResponse.json"))));
        if (jsonSubject == null) {
            throw new IllegalStateException(jsonSubjectNull);
        }
        try {
              loadJsonSchema().validate(jsonSubject);
              System.out.println(successResult);

        } catch (ValidationException ex) { }

    }

    @Test
    public void validateJsonAgainstSchemaV2Failed() throws IOException, JSONException {
        JSONObject jsonSubject = new JSONObject(new JSONTokener(IOUtils.toString(loadJsonFile("manifestIIIFv2_InvalidResponse.json"))));
        if (jsonSubject == null) {
            throw new IllegalStateException(jsonSubjectNull);
        }
        try {
             loadJsonSchema().validate(jsonSubject);
        } catch (ValidationException ex) {
            ex.getAllMessages().stream().peek(e -> validationResult.append("\n")).forEach(validationResult::append);
            throw new AssertionError(validationResult.toString());
        }

    }

    private InputStream loadJsonFile(String fileName) throws IOException {
        InputStream is = JsonSchemaValidation.class.getClassLoader().getResourceAsStream(fileName);

        if (is != null) {
            return is;
        }
        throw new FileNotFoundException(fileName);
    }

    private Schema loadJsonSchema() throws IOException {
        JSONObject jsonSchema = new JSONObject(new JSONTokener(IOUtils.toString(loadJsonFile(jsonSchemaV2))));

        if (jsonSchema == null) {
            throw new IllegalStateException(jsonObjectNull);
        }
        Schema schema = SchemaLoader.load(jsonSchema);
        if (schema == null) {
            throw new IllegalStateException(schemaNull);
        }
        return schema;
    }
}