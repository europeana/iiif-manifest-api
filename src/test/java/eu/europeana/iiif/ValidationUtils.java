package eu.europeana.iiif;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;

public class ValidationUtils {

    private static final String JSON_V4_SCHEMA_IDENTIFIER = "http://json-schema.org/draft-04/schema#";
    private static final String JSON_SCHEMA_IDENTIFIER_ELEMENT = "$schema";

    public static String validateJson(File schemaFile, File jsonFile) throws IOException, ProcessingException {
        final JsonSchema schemaNode = getSchemaNode(schemaFile);
        final JsonNode jsonNode = getJsonNode(jsonFile);
        return validateJson(schemaNode, jsonNode);
    }

    public static boolean isJsonValid(File schemaFile, File jsonFile) throws ProcessingException, IOException {
        final JsonSchema schemaNode = getSchemaNode(schemaFile);
        final JsonNode jsonNode = getJsonNode(jsonFile);
        return isJsonValid(schemaNode, jsonNode);
    }

    private static String validateJson(JsonSchema jsonSchemaNode, JsonNode jsonNode) throws ProcessingException {
        ProcessingReport report = jsonSchemaNode.validate(jsonNode);
        if (!report.isSuccess()) {
            for (ProcessingMessage processingMessage : report) {
                return JsonSchemaValidation.FAIL + "\n" + new ProcessingException(processingMessage);
            }
        }
        return JsonSchemaValidation.SUCCESS;
    }

    private static boolean isJsonValid(JsonSchema jsonSchemaNode, JsonNode jsonNode) throws ProcessingException {
        ProcessingReport report = jsonSchemaNode.validate(jsonNode);
        return report.isSuccess();
    }

    private static JsonNode getJsonNode(File jsonFile) throws IOException {
        return JsonLoader.fromFile(jsonFile);
    }

    private static JsonSchema getSchemaNode(File schemaFile) throws IOException, ProcessingException {
        final JsonNode schemaNode = getJsonNode(schemaFile);
        return _getSchemaNode(schemaNode);
    }

    private static JsonSchema _getSchemaNode(JsonNode jsonNode) throws ProcessingException {
        final JsonNode schemaIdentifier = jsonNode.get(JSON_SCHEMA_IDENTIFIER_ELEMENT);
        if (null == schemaIdentifier) {
            ((ObjectNode) jsonNode).put(JSON_SCHEMA_IDENTIFIER_ELEMENT, JSON_V4_SCHEMA_IDENTIFIER);
        }
        final JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
        return factory.getJsonSchema(jsonNode);
    }
}
