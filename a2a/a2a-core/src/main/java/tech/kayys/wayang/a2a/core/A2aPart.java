package tech.kayys.wayang.a2a.core;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A2A message or artifact part. Exactly one payload field is present.
 */
public record A2aPart(
        String text,
        String raw,
        String url,
        Object data,
        boolean dataPresent,
        String filename,
        String mediaType,
        Map<String, Object> metadata) {

    public A2aPart {
        raw = A2aValues.optional(raw);
        url = A2aValues.optional(url);
        filename = A2aValues.optional(filename);
        mediaType = A2aValues.optional(mediaType);
        data = A2aValues.copyValue(data);
        metadata = A2aValues.copyMap(metadata);

        int payloads = (text == null ? 0 : 1)
                + (raw == null ? 0 : 1)
                + (url == null ? 0 : 1)
                + (dataPresent ? 1 : 0);
        if (payloads != 1) {
            throw new IllegalArgumentException("A2A part requires exactly one payload field");
        }
    }

    public static A2aPart text(String text) {
        if (text == null) {
            throw new IllegalArgumentException("text must not be null");
        }
        return new A2aPart(text, null, null, null, false, null, null, Map.of());
    }

    public static A2aPart data(Object data) {
        return new A2aPart(null, null, null, data, true, null, null, Map.of());
    }

    public static A2aPart fileWithRaw(String raw, String filename, String mediaType) {
        return new A2aPart(null, A2aValues.required(raw, "raw"), null, null, false,
                filename, mediaType, Map.of());
    }

    public static A2aPart fileWithUrl(String url, String filename, String mediaType) {
        return new A2aPart(null, null, A2aValues.required(url, "url"), null, false,
                filename, mediaType, Map.of());
    }

    public Map<String, Object> toMap() {
        Map<String, Object> payload = new LinkedHashMap<>();
        if (text != null) {
            payload.put("text", text);
        }
        if (raw != null) {
            payload.put("raw", raw);
        }
        if (url != null) {
            payload.put("url", url);
        }
        if (dataPresent) {
            payload.put("data", data);
        }
        A2aValues.putOptional(payload, "filename", filename);
        A2aValues.putOptional(payload, "mediaType", mediaType);
        A2aValues.putOptional(payload, "metadata", metadata);
        return A2aValues.copyMap(payload);
    }

    public static A2aPart fromMap(Map<?, ?> payload) {
        Map<String, Object> source = A2aValues.copyMap(payload);
        return new A2aPart(
                source.containsKey("text") ? String.valueOf(source.get("text")) : null,
                A2aValues.optionalString(source, "raw"),
                A2aValues.optionalString(source, "url"),
                source.get("data"),
                source.containsKey("data"),
                A2aValues.optionalString(source, "filename"),
                A2aValues.optionalString(source, "mediaType"),
                A2aValues.objectOrEmpty(source, "metadata"));
    }
}
