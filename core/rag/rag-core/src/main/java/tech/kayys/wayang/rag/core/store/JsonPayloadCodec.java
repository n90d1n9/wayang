package tech.kayys.wayang.rag.core.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Objects;

public class JsonPayloadCodec<T> implements PayloadCodec<T> {

    private final ObjectMapper objectMapper;
    private final Class<T> payloadType;

    public JsonPayloadCodec(ObjectMapper objectMapper, Class<T> payloadType) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.payloadType = Objects.requireNonNull(payloadType, "payloadType must not be null");
    }

    @Override
    public String serialize(T payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize payload", e);
        }
    }

    @Override
    public T deserialize(String payload) {
        try {
            return objectMapper.readValue(payload, payloadType);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to deserialize payload", e);
        }
    }
}
