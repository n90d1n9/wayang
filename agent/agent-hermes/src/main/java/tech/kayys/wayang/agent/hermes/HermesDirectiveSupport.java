package tech.kayys.wayang.agent.hermes;

import tech.kayys.wayang.agent.spi.AgentRequest;

import java.util.Locale;
import java.util.Objects;

/**
 * Shared directive helpers for request identity and stable directive ids.
 */
final class HermesDirectiveSupport {

    private HermesDirectiveSupport() {
    }

    static Identity identity(AgentRequest request) {
        return new Identity(
                request == null ? "" : request.requestId(),
                request == null ? "default" : request.tenantId(),
                request == null ? "" : request.sessionId(),
                request == null ? "" : request.userId());
    }

    static String clean(String value, String fallback) {
        return HermesText.trimOr(value, fallback);
    }

    static String prefixedId(String prefix, String base, String fallback) {
        return clean(prefix, "hermes") + "-" + identifier(base, fallback);
    }

    static String hashBase(Object... values) {
        return Integer.toUnsignedString(Objects.hash(values), 36);
    }

    private static String identifier(String value, String fallback) {
        String normalized = value == null ? "" : value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+", "")
                .replaceAll("-+$", "");
        return normalized.isBlank() ? clean(fallback, "id") : normalized;
    }

    record Identity(String requestId, String tenantId, String sessionId, String userId) {

        Identity {
            requestId = HermesDirectiveSupport.clean(requestId, "");
            tenantId = HermesDirectiveSupport.clean(tenantId, "default");
            sessionId = HermesDirectiveSupport.clean(sessionId, "");
            userId = HermesDirectiveSupport.clean(userId, "");
        }
    }
}
