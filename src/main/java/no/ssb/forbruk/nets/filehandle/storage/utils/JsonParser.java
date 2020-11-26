package no.ssb.forbruk.nets.filehandle.storage.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class JsonParser {
    static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ObjectMapper mapper;

    public JsonParser() {
        mapper = OBJECT_MAPPER;
    }

    public JsonParser(ObjectMapper objectMapper) {
        mapper = objectMapper;
    }

    public static JsonParser createJsonParser() {
        return new JsonParser(OBJECT_MAPPER);
    }

    public ObjectMapper mapper() {
        return mapper;
    }

    public ObjectNode createObjectNode() {
        return mapper.createObjectNode();
    }

    public ArrayNode createArrayNode() {
        return mapper.createArrayNode();
    }

    public <T> T fromJson(InputStream source, Class<T> clazz) {
        try {
            String json = new String(source.readAllBytes(), StandardCharsets.UTF_8);
            return mapper.readValue(json, clazz);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public <T> T fromJson(String source, Class<T> clazz) {
        try {
            return mapper.readValue(source, clazz);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String toJSON(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public String toPrettyJSON(Object value) {
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
