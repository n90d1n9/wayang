package tech.kayys.wayang.a2a.wayang;

import tech.kayys.wayang.a2a.core.A2aAgentCapabilities;
import tech.kayys.wayang.a2a.core.A2aAgentCard;
import tech.kayys.wayang.a2a.core.A2aAgentCardSignature;
import tech.kayys.wayang.a2a.core.A2aAgentExtension;
import tech.kayys.wayang.a2a.core.A2aAgentInterface;
import tech.kayys.wayang.a2a.core.A2aAgentProvider;
import tech.kayys.wayang.a2a.core.A2aAgentSkill;
import tech.kayys.wayang.a2a.core.A2aProtocol;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Agent Card payload expectations used by the A2A spec-alignment report.
 */
final class WayangA2aAgentCardSpecAlignment {

    private WayangA2aAgentCardSpecAlignment() {
    }

    static List<WayangA2aSpecAlignmentRequirement> requirements() {
        return List.of(
                topLevelFieldRequirement(),
                componentFieldRequirement(),
                bindingDefaultRequirement());
    }

    private static WayangA2aSpecAlignmentRequirement topLevelFieldRequirement() {
        Map<String, Object> expected = Map.of("fields", List.of(
                "name",
                "description",
                "supportedInterfaces",
                "provider",
                "version",
                "documentationUrl",
                "capabilities",
                "securitySchemes",
                "securityRequirements",
                "defaultInputModes",
                "defaultOutputModes",
                "skills",
                "signatures",
                "iconUrl"));
        Map<String, Object> actual = Map.of("fields", fieldNames(fullAgentCard().toMap()));
        return WayangA2aSpecAlignmentRequirementFactory.compare(
                "agent_card.top_level_fields",
                "agent_card",
                "A2A Agent Card top-level fields",
                expected,
                actual,
                "A2A Agent Card top-level field names do not match the pinned v1.0 snapshot.");
    }

    private static WayangA2aSpecAlignmentRequirement componentFieldRequirement() {
        Map<String, Object> expected = Map.of(
                "interfaceFields", List.of("url", "protocolBinding", "tenant", "protocolVersion"),
                "providerFields", List.of("url", "organization"),
                "capabilityFields", List.of("streaming", "pushNotifications", "extensions", "extendedAgentCard"),
                "extensionFields", List.of("uri", "description", "required", "params"),
                "skillFields", List.of(
                        "id",
                        "name",
                        "description",
                        "tags",
                        "examples",
                        "inputModes",
                        "outputModes",
                        "securityRequirements"),
                "signatureFields", List.of("protected", "signature", "header"));
        Map<String, Object> actual = Map.of(
                "interfaceFields", fieldNames(fullAgentInterface().toMap()),
                "providerFields", fieldNames(fullProvider().toMap()),
                "capabilityFields", fieldNames(fullCapabilities().toMap()),
                "extensionFields", fieldNames(fullExtension().toMap()),
                "skillFields", fieldNames(fullSkill().toMap()),
                "signatureFields", fieldNames(fullSignature().toMap()));
        return WayangA2aSpecAlignmentRequirementFactory.compare(
                "agent_card.component_fields",
                "agent_card",
                "A2A Agent Card component fields",
                expected,
                actual,
                "A2A Agent Card component field names do not match the pinned v1.0 snapshot.");
    }

    private static WayangA2aSpecAlignmentRequirement bindingDefaultRequirement() {
        A2aAgentCard minimal = A2aAgentCard.minimal(
                "Wayang",
                "Default Wayang A2A endpoint",
                "https://wayang.test/a2a",
                List.of(A2aAgentSkill.of("chat", "Chat", "General chat", List.of("chat"))));
        Map<String, Object> expected = Map.of(
                "preferredProtocolBinding", A2aProtocol.BINDING_HTTP_JSON,
                "preferredProtocolVersion", A2aProtocol.VERSION,
                "defaultInputModes", List.of("text/plain"),
                "defaultOutputModes", List.of("text/plain"));
        Map<String, Object> actual = Map.of(
                "preferredProtocolBinding", minimal.preferredInterface().protocolBinding(),
                "preferredProtocolVersion", minimal.preferredInterface().protocolVersion(),
                "defaultInputModes", minimal.defaultInputModes(),
                "defaultOutputModes", minimal.defaultOutputModes());
        return WayangA2aSpecAlignmentRequirementFactory.compare(
                "agent_card.binding_defaults",
                "agent_card",
                "A2A Agent Card binding defaults",
                expected,
                actual,
                "A2A Agent Card default binding or mode values do not match the pinned v1.0 snapshot.");
    }

    private static A2aAgentCard fullAgentCard() {
        return new A2aAgentCard(
                "Wayang Agent",
                "Agentic core endpoint",
                List.of(fullAgentInterface()),
                fullProvider(),
                "1.2.3",
                "https://wayang.test/docs",
                fullCapabilities(),
                Map.of("bearer", Map.of("httpAuthSecurityScheme", Map.of("scheme", "Bearer"))),
                List.of(Map.of("bearer", List.of())),
                List.of("text/plain"),
                List.of("text/plain", "application/json"),
                List.of(fullSkill()),
                List.of(fullSignature()),
                "https://wayang.test/icon.png");
    }

    private static A2aAgentInterface fullAgentInterface() {
        return new A2aAgentInterface(
                "https://wayang.test/a2a/jsonrpc",
                A2aProtocol.BINDING_JSONRPC,
                "tenant-a",
                A2aProtocol.VERSION);
    }

    private static A2aAgentProvider fullProvider() {
        return new A2aAgentProvider("https://kayys.tech", "Kayys");
    }

    private static A2aAgentCapabilities fullCapabilities() {
        return new A2aAgentCapabilities(true, true, List.of(fullExtension()), true);
    }

    private static A2aAgentExtension fullExtension() {
        return new A2aAgentExtension(
                "https://a2ui.org/a2a-extension/a2ui/v0.8",
                "A2UI surfaces",
                true,
                Map.of("catalog", "standard"));
    }

    private static A2aAgentSkill fullSkill() {
        return new A2aAgentSkill(
                "rag",
                "RAG",
                "Answer with retrieved context",
                List.of("rag", "search"),
                List.of("Find answer with citations"),
                List.of("text/plain"),
                List.of("text/plain", "application/json"),
                List.of(Map.of("bearer", List.of())));
    }

    private static A2aAgentCardSignature fullSignature() {
        return new A2aAgentCardSignature(
                "eyJhbGciOiJFUzI1NiJ9",
                "signature",
                Map.of("kid", "agent-card-key-1"));
    }

    private static List<String> fieldNames(Map<String, Object> payload) {
        return payload.keySet().stream().toList();
    }
}
