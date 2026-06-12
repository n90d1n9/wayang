package tech.kayys.wayang.a2a.wayang;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Optional access boundary for authenticated A2A extended Agent Cards.
 */
@FunctionalInterface
public interface WayangA2aExtendedAgentCardAuthorizer {

    String BEARER_REALM = "a2a-extended-agent-card";

    Optional<WayangA2aHttpResponse> authorize(WayangA2aHttpRequest request);

    static WayangA2aExtendedAgentCardAuthorizer allowAll() {
        return request -> Optional.empty();
    }

    static WayangA2aExtendedAgentCardAuthorizer requireAuthorizationHeader(String expectedValue) {
        String expected = WayangA2aMaps.required(expectedValue, "expectedValue");
        return request -> request.header(WayangA2aHttpResponse.HEADER_AUTHORIZATION)
                .filter(expected::equals)
                .map(ignored -> Optional.<WayangA2aHttpResponse>empty())
                .orElseGet(WayangA2aExtendedAgentCardAuthorizer::unauthorized);
    }

    static WayangA2aExtendedAgentCardAuthorizer requireBearerToken(String token) {
        String expectedToken = WayangA2aMaps.required(token, "token");
        return request -> request.header(WayangA2aHttpResponse.HEADER_AUTHORIZATION)
                .filter(value -> bearerTokenMatches(value, expectedToken))
                .map(ignored -> Optional.<WayangA2aHttpResponse>empty())
                .orElseGet(WayangA2aExtendedAgentCardAuthorizer::unauthorized);
    }

    static Optional<WayangA2aHttpResponse> unauthorized() {
        return Optional.of(WayangA2aHttpResponse.error(
                401,
                "extended_agent_card_unauthorized",
                "A2A extended Agent Card requires authorization.",
                authenticationMetadata())
                .withHeaders(authenticationHeaders()));
    }

    private static boolean bearerTokenMatches(String authorization, String expectedToken) {
        if (authorization == null || authorization.isBlank()) {
            return false;
        }
        String[] parts = authorization.trim().split("\\s+", 2);
        return parts.length == 2
                && "Bearer".equalsIgnoreCase(parts[0])
                && expectedToken.equals(parts[1].trim());
    }

    private static Map<String, Object> authenticationMetadata() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("scheme", "Bearer");
        return WayangA2aMaps.copyMap(metadata);
    }

    private static Map<String, Object> authenticationHeaders() {
        Map<String, Object> headers = new LinkedHashMap<>();
        headers.put(
                WayangA2aHttpResponse.HEADER_WWW_AUTHENTICATE,
                "Bearer realm=\"" + BEARER_REALM + "\"");
        return WayangA2aMaps.copyMap(headers);
    }
}
