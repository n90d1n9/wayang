package tech.kayys.wayang.a2ui.wayang.session;

import java.util.List;
import java.util.Map;

/**
 * Builds and validates a JSON-backed session config source from a normalized source specification.
 */
@FunctionalInterface
public interface SessionConfigSourceProvider {

    SessionConfigSource source(Map<String, Object> values);

    default SessionConfigSourceCapability capability(String type) {
        return SessionConfigSourceCapability.generic(type);
    }

    default List<String> validationErrors(Map<String, Object> values) {
        return List.of();
    }
}
