package tech.kayys.wayang.a2a.wayang;

import tech.kayys.wayang.a2a.core.A2aSendMessageRequest;
import tech.kayys.wayang.a2a.core.A2aTask;

import java.util.Map;
import java.util.Optional;

/**
 * Shared tenant extraction rules for A2A request and task scoping.
 */
final class WayangA2aTenantHints {

    private WayangA2aTenantHints() {
    }

    static Optional<String> fromHttpRequest(WayangA2aHttpRequest request) {
        if (request == null) {
            return Optional.empty();
        }
        Optional<String> attributeTenant = fromMap(request.attributes());
        if (attributeTenant.isPresent()) {
            return attributeTenant;
        }
        Optional<String> cachedTenant = fromCachedSendMessageRequest(request);
        if (cachedTenant.isPresent()) {
            return cachedTenant;
        }
        try {
            return fromMap(WayangA2aHttpJson.read(request.body()));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    static Optional<String> fromSendMessageRequest(A2aSendMessageRequest request) {
        if (request == null) {
            return Optional.empty();
        }
        String directTenant = WayangA2aMaps.optional(request.tenant());
        if (directTenant != null) {
            return Optional.of(directTenant);
        }
        return fromMap(request.metadata());
    }

    static Optional<String> fromMap(Map<String, ?> values) {
        Optional<String> directTenant = WayangA2aMaps.firstString(values, "tenant", "tenantId");
        if (directTenant.isPresent()) {
            return directTenant;
        }
        if (values != null && values.get("metadata") instanceof Map<?, ?> metadata) {
            return WayangA2aMaps.firstString(WayangA2aMaps.copyMap(metadata), "tenant", "tenantId");
        }
        return Optional.empty();
    }

    static Optional<String> fromTask(A2aTask task) {
        if (task == null) {
            return Optional.empty();
        }
        return fromMap(task.metadata());
    }

    private static Optional<String> fromCachedSendMessageRequest(WayangA2aHttpRequest request) {
        Object cached = request.attributes().get(WayangA2a.SEND_MESSAGE_REQUEST_ATTRIBUTE);
        if (!(cached instanceof A2aSendMessageRequest sendMessageRequest)) {
            return Optional.empty();
        }
        return fromSendMessageRequest(sendMessageRequest);
    }
}
