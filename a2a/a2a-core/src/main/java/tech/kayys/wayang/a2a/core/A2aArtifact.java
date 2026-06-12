package tech.kayys.wayang.a2a.core;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Output artifact produced by an A2A task.
 */
public record A2aArtifact(
        String artifactId,
        String name,
        String description,
        List<A2aPart> parts,
        Map<String, Object> metadata,
        List<String> extensions) {

    public A2aArtifact {
        artifactId = A2aValues.required(artifactId, "artifactId");
        name = A2aValues.optional(name);
        description = A2aValues.optional(description);
        parts = A2aValues.copyRecords(parts);
        if (parts.isEmpty()) {
            throw new IllegalArgumentException("A2A artifact requires at least one part");
        }
        metadata = A2aValues.copyMap(metadata);
        extensions = A2aValues.stringList(extensions, "extensions");
    }

    public Map<String, Object> toMap() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("artifactId", artifactId);
        A2aValues.putOptional(payload, "name", name);
        A2aValues.putOptional(payload, "description", description);
        payload.put("parts", parts.stream().map(A2aPart::toMap).toList());
        A2aValues.putOptional(payload, "metadata", metadata);
        A2aValues.putOptional(payload, "extensions", extensions);
        return A2aValues.copyMap(payload);
    }

    public static A2aArtifact fromMap(Map<?, ?> payload) {
        Map<String, Object> source = A2aValues.copyMap(payload);
        return new A2aArtifact(
                A2aValues.string(source, "artifactId"),
                A2aValues.optionalString(source, "name"),
                A2aValues.optionalString(source, "description"),
                A2aValues.objectList(source.get("parts"), "parts").stream()
                        .map(A2aPart::fromMap)
                        .toList(),
                A2aValues.objectOrEmpty(source, "metadata"),
                A2aValues.stringList(source.get("extensions"), "extensions"));
    }
}
