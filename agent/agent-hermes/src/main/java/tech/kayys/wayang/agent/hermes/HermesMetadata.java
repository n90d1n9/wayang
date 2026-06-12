package tech.kayys.wayang.agent.hermes;

import java.util.Map;

final class HermesMetadata {

    private HermesMetadata() {
    }

    static Map<String, Object> copy(Map<String, Object> metadata) {
        return metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
