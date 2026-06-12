package tech.kayys.wayang.a2a.wayang;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Shared media type matching for A2A input/output mode negotiation.
 */
final class WayangA2aMediaTypes {

    private WayangA2aMediaTypes() {
    }

    static List<String> copyDistinct(List<String> values) {
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

    static boolean supports(List<String> supportedMediaTypes, String requestedMediaType) {
        for (String supportedMediaType : supportedMediaTypes) {
            if (matches(supportedMediaType, requestedMediaType)) {
                return true;
            }
        }
        return false;
    }

    static boolean intersects(List<String> supportedMediaTypes, List<String> requestedMediaTypes) {
        if (requestedMediaTypes == null || requestedMediaTypes.isEmpty()) {
            return true;
        }
        for (String requestedMediaType : requestedMediaTypes) {
            if (supports(supportedMediaTypes, requestedMediaType)) {
                return true;
            }
        }
        return false;
    }

    static boolean matches(String supported, String requested) {
        String supportedMode = WayangA2aMaps.optional(supported);
        String requestedMode = WayangA2aMaps.optional(requested);
        if (supportedMode == null || requestedMode == null) {
            return false;
        }
        if (supportedMode.equals(requestedMode)
                || "*/*".equals(supportedMode)
                || "*/*".equals(requestedMode)) {
            return true;
        }
        return wildcardMatches(supportedMode, requestedMode)
                || wildcardMatches(requestedMode, supportedMode);
    }

    private static boolean wildcardMatches(String wildcard, String candidate) {
        int slash = wildcard.indexOf('/');
        return slash > 0
                && wildcard.endsWith("/*")
                && candidate.startsWith(wildcard.substring(0, slash + 1));
    }
}
