package com.backbase.oss.boat.transformers.bundler;

import com.backbase.oss.boat.transformers.TransformerException;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.examples.Example;

public class BoatDeserializationUtils {

    private BoatDeserializationUtils(){
        throw new AssertionError("Private constructor");
    }

    public static <T> T deserialize(Object contents, String fileOrHost, Class<T> expectedType) {
        T result;

        boolean isJson = false;

        if (contents instanceof String && isJson((String) contents)) {
            isJson = true;
        }

        if (expectedType.equals(Example.class)) {
            Example example = new Example();
            example.setValue(contents);
            return (T) example;
        }

        try {
            if (contents instanceof String) {
                if (isJson) {
                    result = Json.mapper().readValue((String) contents, expectedType);
                } else {
                    result = Yaml.mapper().readValue((String) contents, expectedType);
                }
            } else {
                result = Json.mapper().convertValue(contents, expectedType);
            }
        } catch (Exception e) {
            throw new TransformerException("An exception was thrown while trying to deserialize the contents of " + fileOrHost + " into type " + expectedType, e);
        }

        return result;
    }

    private static boolean isJson(String contents) {
        return contents.trim().startsWith("{");
    }

    private static boolean isXml(String contents) {
        return contents.trim().startsWith("<");
    }

}
