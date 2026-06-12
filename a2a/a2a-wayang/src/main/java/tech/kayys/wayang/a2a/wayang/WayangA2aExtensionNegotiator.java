package tech.kayys.wayang.a2a.wayang;

import tech.kayys.wayang.a2a.core.A2aAgentCard;
import tech.kayys.wayang.a2a.core.A2aAgentExtension;
import tech.kayys.wayang.a2a.core.A2aProtocol;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Validates A2A extension opt-ins advertised by an Agent Card.
 */
final class WayangA2aExtensionNegotiator {

    private final List<String> requiredExtensions;

    private WayangA2aExtensionNegotiator(List<String> requiredExtensions) {
        this.requiredExtensions = copyDistinct(requiredExtensions);
    }

    static WayangA2aExtensionNegotiator fromAgentCard(A2aAgentCard agentCard) {
        A2aAgentCard resolved = Objects.requireNonNull(agentCard, "agentCard");
        return new WayangA2aExtensionNegotiator(resolved.capabilities().extensions().stream()
                .filter(A2aAgentExtension::required)
                .map(A2aAgentExtension::uri)
                .filter(uri -> uri != null && !uri.isBlank())
                .toList());
    }

    List<String> requiredExtensions() {
        return requiredExtensions;
    }

    String requiredExtensionsHeader() {
        return String.join(", ", requiredExtensions);
    }

    List<String> requestedExtensions(WayangA2aHttpRequest request) {
        return request.header(A2aProtocol.HEADER_EXTENSIONS)
                .map(WayangA2aExtensionNegotiator::splitHeader)
                .orElse(List.of());
    }

    List<String> missingRequiredExtensions(WayangA2aHttpRequest request) {
        if (requiredExtensions.isEmpty()) {
            return List.of();
        }
        Set<String> requested = new LinkedHashSet<>(requestedExtensions(request));
        return requiredExtensions.stream()
                .filter(extension -> !requested.contains(extension))
                .toList();
    }

    Optional<WayangA2aHttpResponse> validateHttp(WayangA2aHttpRequest request) {
        List<String> missing = missingRequiredExtensions(request);
        if (missing.isEmpty()) {
            return Optional.empty();
        }
        List<String> requested = requestedExtensions(request);
        return Optional.of(WayangA2aHttpResponse.error(
                400,
                "extension_support_required",
                "A2A request requires extension support: " + String.join(", ", missing) + ".",
                extensionMetadata(missing, requested))
                .withHeaders(requiredExtensionHeaders()));
    }

    private static List<String> splitHeader(String header) {
        if (header == null || header.isBlank()) {
            return List.of();
        }
        Set<String> values = new LinkedHashSet<>();
        for (String token : header.split(",")) {
            String extension = token.trim();
            if (!extension.isBlank()) {
                values.add(extension);
            }
        }
        return List.copyOf(values);
    }

    private static List<String> copyDistinct(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        Set<String> copy = new LinkedHashSet<>();
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                copy.add(value.trim());
            }
        }
        return List.copyOf(copy);
    }

    private Map<String, Object> extensionMetadata(List<String> missing, List<String> requested) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("missingExtensions", missing);
        metadata.put("requiredExtensions", requiredExtensions);
        metadata.put("providedExtensions", requested);
        return WayangA2aMaps.copyMap(metadata);
    }

    private Map<String, Object> requiredExtensionHeaders() {
        Map<String, Object> headers = new LinkedHashMap<>();
        headers.put(WayangA2aHttpResponse.HEADER_A2A_EXTENSIONS, requiredExtensionsHeader());
        return WayangA2aMaps.copyMap(headers);
    }
}
