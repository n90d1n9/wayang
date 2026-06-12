package tech.kayys.wayang.a2a.core;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Public or authenticated A2A Agent Card.
 */
public record A2aAgentCard(
        String name,
        String description,
        List<A2aAgentInterface> supportedInterfaces,
        A2aAgentProvider provider,
        String version,
        String documentationUrl,
        A2aAgentCapabilities capabilities,
        Map<String, Object> securitySchemes,
        List<Map<String, Object>> securityRequirements,
        List<String> defaultInputModes,
        List<String> defaultOutputModes,
        List<A2aAgentSkill> skills,
        List<A2aAgentCardSignature> signatures,
        String iconUrl) {

    public A2aAgentCard {
        name = A2aValues.required(name, "name");
        description = A2aValues.required(description, "description");
        supportedInterfaces = A2aValues.copyRecords(supportedInterfaces);
        if (supportedInterfaces.isEmpty()) {
            throw new IllegalArgumentException("A2A Agent Card requires at least one supported interface");
        }
        version = A2aValues.required(version, "version");
        documentationUrl = A2aValues.optional(documentationUrl);
        capabilities = capabilities == null ? A2aAgentCapabilities.basic() : capabilities;
        securitySchemes = A2aValues.copyMap(securitySchemes);
        securityRequirements = securityRequirements == null || securityRequirements.isEmpty()
                ? List.of()
                : securityRequirements.stream().map(A2aValues::copyMap).toList();
        defaultInputModes = A2aValues.stringList(defaultInputModes, "defaultInputModes");
        if (defaultInputModes.isEmpty()) {
            throw new IllegalArgumentException("A2A Agent Card requires default input modes");
        }
        defaultOutputModes = A2aValues.stringList(defaultOutputModes, "defaultOutputModes");
        if (defaultOutputModes.isEmpty()) {
            throw new IllegalArgumentException("A2A Agent Card requires default output modes");
        }
        skills = A2aValues.copyRecords(skills);
        if (skills.isEmpty()) {
            throw new IllegalArgumentException("A2A Agent Card requires at least one skill");
        }
        signatures = A2aValues.copyRecords(signatures);
        iconUrl = A2aValues.optional(iconUrl);
    }

    public static A2aAgentCard minimal(
            String name,
            String description,
            String endpointUrl,
            List<A2aAgentSkill> skills) {
        return new A2aAgentCard(
                name,
                description,
                List.of(A2aAgentInterface.httpJson(endpointUrl)),
                null,
                "1.0.0",
                null,
                A2aAgentCapabilities.basic(),
                Map.of(),
                List.of(),
                List.of("text/plain"),
                List.of("text/plain"),
                skills,
                List.of(),
                null);
    }

    public A2aAgentInterface preferredInterface() {
        return supportedInterfaces.getFirst();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("name", name);
        payload.put("description", description);
        payload.put("supportedInterfaces", supportedInterfaces.stream()
                .map(A2aAgentInterface::toMap)
                .toList());
        if (provider != null) {
            payload.put("provider", provider.toMap());
        }
        payload.put("version", version);
        A2aValues.putOptional(payload, "documentationUrl", documentationUrl);
        payload.put("capabilities", capabilities.toMap());
        A2aValues.putOptional(payload, "securitySchemes", securitySchemes);
        A2aValues.putOptional(payload, "securityRequirements", securityRequirements);
        payload.put("defaultInputModes", defaultInputModes);
        payload.put("defaultOutputModes", defaultOutputModes);
        payload.put("skills", skills.stream().map(A2aAgentSkill::toMap).toList());
        if (!signatures.isEmpty()) {
            payload.put("signatures", signatures.stream().map(A2aAgentCardSignature::toMap).toList());
        }
        A2aValues.putOptional(payload, "iconUrl", iconUrl);
        return A2aValues.copyMap(payload);
    }

    public String toJson() {
        return A2aJson.write(toMap(), "agent card");
    }

    public static A2aAgentCard fromJson(String json) {
        return fromMap(A2aJson.read(json, "agent card"));
    }

    public static A2aAgentCard fromMap(Map<?, ?> payload) {
        Map<String, Object> source = A2aValues.copyMap(payload);
        A2aAgentProvider provider = source.get("provider") instanceof Map<?, ?> providerMap
                ? A2aAgentProvider.fromMap(providerMap)
                : null;
        return new A2aAgentCard(
                A2aValues.string(source, "name"),
                A2aValues.string(source, "description"),
                A2aValues.objectList(source.get("supportedInterfaces"), "supportedInterfaces").stream()
                        .map(A2aAgentInterface::fromMap)
                        .toList(),
                provider,
                A2aValues.string(source, "version"),
                A2aValues.optionalString(source, "documentationUrl"),
                A2aAgentCapabilities.fromMap(A2aValues.objectRequired(source, "capabilities")),
                A2aValues.objectOrEmpty(source, "securitySchemes"),
                A2aValues.objectList(source.get("securityRequirements"), "securityRequirements"),
                A2aValues.stringList(source.get("defaultInputModes"), "defaultInputModes"),
                A2aValues.stringList(source.get("defaultOutputModes"), "defaultOutputModes"),
                A2aValues.objectList(source.get("skills"), "skills").stream()
                        .map(A2aAgentSkill::fromMap)
                        .toList(),
                A2aValues.objectList(source.get("signatures"), "signatures").stream()
                        .map(A2aAgentCardSignature::fromMap)
                        .toList(),
                A2aValues.optionalString(source, "iconUrl"));
    }
}
