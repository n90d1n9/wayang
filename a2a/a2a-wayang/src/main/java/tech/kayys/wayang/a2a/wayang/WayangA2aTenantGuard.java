package tech.kayys.wayang.a2a.wayang;

import tech.kayys.wayang.a2a.core.A2aAgentCard;
import tech.kayys.wayang.a2a.core.A2aAgentInterface;
import tech.kayys.wayang.a2a.core.A2aSendMessageRequest;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Validates explicit tenant hints against Agent Card interfaces.
 */
final class WayangA2aTenantGuard {

    private final List<String> supportedTenants;

    private WayangA2aTenantGuard(List<String> supportedTenants) {
        this.supportedTenants = copyDistinct(supportedTenants);
    }

    static WayangA2aTenantGuard fromAgentCard(A2aAgentCard agentCard) {
        A2aAgentCard resolved = Objects.requireNonNull(agentCard, "agentCard");
        return new WayangA2aTenantGuard(resolved.supportedInterfaces().stream()
                .map(A2aAgentInterface::tenant)
                .filter(tenant -> tenant != null && !tenant.isBlank())
                .toList());
    }

    Optional<WayangA2aHttpResponse> validateHttp(WayangA2aHttpRequest request) {
        return WayangA2aTenantHints.fromHttpRequest(request)
                .filter(this::unsupported)
                .map(this::tenantNotSupportedHttp);
    }

    Optional<WayangA2aJsonRpcError> validateJsonRpc(WayangA2aJsonRpcRequest request) {
        return WayangA2aTenantHints.fromMap(request.params())
                .filter(this::unsupported)
                .map(this::tenantNotSupportedJsonRpc);
    }

    Optional<WayangA2aJsonRpcError> validateJsonRpc(A2aSendMessageRequest request) {
        return WayangA2aTenantHints.fromSendMessageRequest(request)
                .filter(this::unsupported)
                .map(this::tenantNotSupportedJsonRpc);
    }

    private WayangA2aHttpResponse tenantNotSupportedHttp(String tenant) {
        return WayangA2aHttpResponse.error(
                400,
                "tenant_not_supported",
                "A2A tenant is not advertised by Agent Card: " + tenant + ".",
                tenantMetadata(tenant));
    }

    private WayangA2aJsonRpcError tenantNotSupportedJsonRpc(String tenant) {
        return WayangA2aJsonRpcError.invalidParams(
                "A2A tenant is not advertised by Agent Card: " + tenant + ".");
    }

    private boolean unsupported(String tenant) {
        return tenant != null && !supportedTenants.isEmpty() && !supportedTenants.contains(tenant);
    }

    private Map<String, Object> tenantMetadata(String tenant) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("tenant", tenant);
        metadata.put("supportedTenants", supportedTenants);
        return WayangA2aMaps.copyMap(metadata);
    }

    private static List<String> copyDistinct(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        Set<String> copy = new LinkedHashSet<>();
        for (String value : values) {
            String normalized = WayangA2aMaps.optional(value);
            if (normalized != null) {
                copy.add(normalized);
            }
        }
        return List.copyOf(copy);
    }
}
