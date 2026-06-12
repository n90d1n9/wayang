package tech.kayys.wayang.a2a.wayang;

import tech.kayys.wayang.a2a.core.A2aAgentCapabilities;
import tech.kayys.wayang.a2a.core.A2aAgentInterface;
import tech.kayys.wayang.a2a.core.A2aAgentProvider;
import tech.kayys.wayang.a2a.core.A2aProtocol;

import java.util.List;
import java.util.Map;

/**
 * Runtime profile used to render a Wayang Agent Card.
 */
public record WayangA2aAgentProfile(
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
        String iconUrl) {

    public WayangA2aAgentProfile {
        name = WayangA2aMaps.required(name, "name");
        description = WayangA2aMaps.required(description, "description");
        supportedInterfaces = supportedInterfaces == null || supportedInterfaces.isEmpty()
                ? List.of()
                : List.copyOf(supportedInterfaces);
        if (supportedInterfaces.isEmpty()) {
            throw new IllegalArgumentException("supportedInterfaces must not be empty");
        }
        version = version == null || version.isBlank() ? WayangA2a.DEFAULT_AGENT_VERSION : version.trim();
        documentationUrl = WayangA2aMaps.optional(documentationUrl);
        capabilities = capabilities == null ? A2aAgentCapabilities.basic() : capabilities;
        securitySchemes = WayangA2aMaps.copyMap(securitySchemes);
        securityRequirements = securityRequirements == null || securityRequirements.isEmpty()
                ? List.of()
                : securityRequirements.stream().map(WayangA2aMaps::copyMap).toList();
        defaultInputModes = normalizedModes(defaultInputModes);
        defaultOutputModes = normalizedModes(defaultOutputModes);
        iconUrl = WayangA2aMaps.optional(iconUrl);
    }

    public static WayangA2aAgentProfile httpJson(String name, String description, String endpointUrl) {
        return new WayangA2aAgentProfile(
                name,
                description,
                List.of(new A2aAgentInterface(endpointUrl, A2aProtocol.BINDING_HTTP_JSON, null, A2aProtocol.VERSION)),
                null,
                WayangA2a.DEFAULT_AGENT_VERSION,
                null,
                A2aAgentCapabilities.basic(),
                Map.of(),
                List.of(),
                List.of(WayangA2a.DEFAULT_TEXT_MEDIA_TYPE),
                List.of(WayangA2a.DEFAULT_TEXT_MEDIA_TYPE),
                null);
    }

    private static List<String> normalizedModes(List<String> modes) {
        List<String> normalized = WayangA2aMaps.stringList(modes);
        return normalized.isEmpty() ? List.of(WayangA2a.DEFAULT_TEXT_MEDIA_TYPE) : normalized;
    }
}
