package eu.europeana.iiif;


import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import org.apache.commons.lang3.StringUtils;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;


public class JsonSchemaValidation {

    protected static final String FAIL = "Failed";
    protected static final String SUCCESS = "Success";

    private static final String JSON_SCHEMA_MANIFEST_V2 = "manifest.IIIFv2.jschema";

    @Test
    public void validateJsonAgainstSchemaV2Success() throws IOException, ProcessingException {
        File schemaFile = loadFile(JSON_SCHEMA_MANIFEST_V2);
        File jsonFile   = loadFile("manifestIIIFv2_ValidResponse.json");

        String result = ValidationUtils.validateJson(schemaFile, jsonFile);
        Assert.assertTrue(StringUtils.equals(result, SUCCESS));
    }

    @Test
    public void validateJsonAgainstSchemaV2Failed() throws IOException, ProcessingException {
        File schemaFile = loadFile(JSON_SCHEMA_MANIFEST_V2);
        File jsonFile   = loadFile("manifestIIIFv2_InvalidResponse.json");

        String result = ValidationUtils.validateJson(schemaFile, jsonFile);
        Assert.assertTrue(StringUtils.startsWith(result, FAIL));
        System.out.println(result);

    }

    private File loadFile(String fileName) throws IOException {
        File jsonFile   = new ClassPathResource(fileName).getFile();
        if (jsonFile != null) {
            return jsonFile;
        }
        throw new FileNotFoundException(fileName);
    }
}