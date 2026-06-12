package tech.kayys.wayang.a2ui.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Encodes A2UI messages as compact JSON and JSON Lines streams.
 */
public final class A2uiJsonlCodec {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    public A2uiJsonlCodec() {
        this(new ObjectMapper());
    }

    public A2uiJsonlCodec(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    public String line(A2uiServerMessage message) {
        return json(Objects.requireNonNull(message, "message").toPayload());
    }

    public String line(A2uiClientMessage message) {
        return json(Objects.requireNonNull(message, "message").toPayload());
    }

    public String dataPart(A2uiServerMessage message) {
        return json(A2uiDataPart.of(message).toPayload());
    }

    public String dataPart(A2uiClientMessage message) {
        return json(A2uiDataPart.of(message).toPayload());
    }

    public String stream(List<? extends A2uiServerMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return "";
        }
        return messages.stream()
                .map(this::line)
                .reduce((left, right) -> left + "\n" + right)
                .orElse("") + "\n";
    }

    public A2uiClientMessage clientMessage(String line) {
        return clientPayload(readObject(line));
    }

    public A2uiClientMessage clientMessage(Map<?, ?> payload) {
        return clientPayload(A2uiValues.copyMap(payload));
    }

    public List<A2uiClientMessage> clientStream(String jsonl) {
        if (jsonl == null || jsonl.isBlank()) {
            return List.of();
        }
        List<A2uiClientMessage> messages = new ArrayList<>();
        for (String line : jsonl.split("\\R")) {
            if (!line.isBlank()) {
                messages.add(clientMessage(line));
            }
        }
        return List.copyOf(messages);
    }

    public A2uiClientMessage clientDataPart(String dataPart) {
        return clientDataPart(readObject(dataPart));
    }

    public A2uiClientMessage clientDataPart(Map<?, ?> payload) {
        Map<String, Object> dataPart = A2uiValues.copyMap(payload);
        Object metadata = dataPart.get("metadata");
        if (metadata instanceof Map<?, ?> metadataMap
                && !A2uiProtocol.MIME_TYPE.equals(metadataMap.get("mimeType"))) {
            throw new IllegalArgumentException("A2UI data part has unsupported mimeType: " + metadataMap.get("mimeType"));
        }
        Object data = dataPart.get("data");
        if (!(data instanceof Map<?, ?> dataMap)) {
            throw new IllegalArgumentException("A2UI data part must contain an object data payload");
        }
        return clientPayload(A2uiValues.copyMap(dataMap));
    }

    private String json(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Unable to encode A2UI payload", e);
        }
    }

    private Map<String, Object> readObject(String line) {
        if (line == null || line.isBlank()) {
            throw new IllegalArgumentException("A2UI JSON line must not be blank");
        }
        try {
            return objectMapper.readValue(line, MAP_TYPE);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Unable to decode A2UI payload", e);
        }
    }

    private A2uiClientMessage clientPayload(Map<String, Object> payload) {
        if (payload.containsKey("userAction")) {
            Map<String, Object> body = objectBody(payload, "userAction");
            return new A2uiUserAction(
                    string(body, "name"),
                    string(body, "surfaceId"),
                    string(body, "sourceComponentId"),
                    instant(body.get("timestamp")),
                    contextBodyOrEmpty(body, "context"));
        }
        if (payload.containsKey("error")) {
            Map<String, Object> body = objectBody(payload, "error");
            return new A2uiClientError(
                    optionalString(body, "surfaceId"),
                    string(body, "message"),
                    objectBodyOrEmpty(body, "details"));
        }
        throw new IllegalArgumentException("Unsupported A2UI client message: " + payload.keySet());
    }

    private static Map<String, Object> objectBody(Map<String, Object> payload, String key) {
        Object raw = payload.get(key);
        if (raw instanceof Map<?, ?> map) {
            return A2uiValues.copyMap(map);
        }
        throw new IllegalArgumentException("A2UI field must be an object: " + key);
    }

    private static Map<String, Object> objectBodyOrEmpty(Map<String, Object> payload, String key) {
        Object raw = payload.get(key);
        if (raw == null) {
            return Map.of();
        }
        if (raw instanceof Map<?, ?> map) {
            return A2uiValues.copyMap(map);
        }
        throw new IllegalArgumentException("A2UI field must be an object: " + key);
    }

    private static Map<String, Object> contextBodyOrEmpty(Map<String, Object> payload, String key) {
        Object raw = payload.get(key);
        if (raw == null) {
            return Map.of();
        }
        if (raw instanceof Map<?, ?> map) {
            return A2uiValues.copyMap(map);
        }
        if (raw instanceof List<?> list) {
            return contextEntries(list);
        }
        throw new IllegalArgumentException("A2UI userAction context must be an object or entry list");
    }

    private static Map<String, Object> contextEntries(List<?> entries) {
        if (entries.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> context = new LinkedHashMap<>();
        for (Object rawEntry : entries) {
            if (!(rawEntry instanceof Map<?, ?> entryMap)) {
                throw new IllegalArgumentException("A2UI action context entry must be an object");
            }
            Map<String, Object> entry = A2uiValues.copyMap(entryMap);
            String key = optionalString(entry, "key");
            if (key == null || key.isBlank()) {
                throw new IllegalArgumentException("A2UI action context entry requires key");
            }
            if (!entry.containsKey("value")) {
                throw new IllegalArgumentException("A2UI action context entry requires value");
            }
            context.put(key.trim(), contextValue(entry.get("value")));
        }
        return Map.copyOf(context);
    }

    private static Object contextValue(Object raw) {
        if (raw instanceof Map<?, ?> map) {
            Map<String, Object> value = A2uiValues.copyMap(map);
            if (value.containsKey("literalString")) {
                return value.get("literalString");
            }
            if (value.containsKey("literalNumber")) {
                return value.get("literalNumber");
            }
            if (value.containsKey("literalBoolean")) {
                return value.get("literalBoolean");
            }
            return value;
        }
        return A2uiValues.copyValue(raw);
    }

    private static String string(Map<String, Object> payload, String key) {
        String value = optionalString(payload, key);
        if (value == null) {
            throw new IllegalArgumentException("A2UI field is required: " + key);
        }
        return value;
    }

    private static String optionalString(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private static Instant instant(Object raw) {
        if (raw == null) {
            return null;
        }
        try {
            return Instant.parse(String.valueOf(raw));
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("A2UI timestamp must be an ISO-8601 instant: " + raw, e);
        }
    }
}
