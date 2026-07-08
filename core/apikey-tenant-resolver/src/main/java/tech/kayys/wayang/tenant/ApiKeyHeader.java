package tech.kayys.wayang.tenant;

import jakarta.ws.rs.container.ContainerRequestContext;

/**
 * API key header utilities for tenant resolution.
 */
public final class ApiKeyHeader {

    public static final String HEADER_API_KEY = "X-API-Key";
    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String AUTHORIZATION_SCHEME = "ApiKey";
    public static final String COMMUNITY_API_KEY = "community";

    private ApiKeyHeader() {
    }

    public static String extractApiKey(ContainerRequestContext requestContext) {
        if (requestContext == null) {
            return null;
        }

        String apiKey = requestContext.getHeaderString(HEADER_API_KEY);
        if (apiKey != null && !apiKey.isBlank()) {
            return apiKey.trim();
        }

        String authorization = requestContext.getHeaderString(HEADER_AUTHORIZATION);
        if (authorization == null || authorization.isBlank()) {
            return null;
        }

        String[] parts = authorization.trim().split("\\s+", 2);
        if (parts.length == 2 && AUTHORIZATION_SCHEME.equalsIgnoreCase(parts[0])) {
            return parts[1].trim();
        }

        return null;
    }
}
