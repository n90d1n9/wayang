package tech.kayys.wayang.agent.spi;

import java.util.Map;

/**
 * Coordination Configuration
 */
public record CoordinationConfig(
        long timeoutMs,
        int minParticipants,
        Map<String, Object> params) {
}
