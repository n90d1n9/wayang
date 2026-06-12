package tech.kayys.wayang.agent.api;

import java.util.List;
import java.util.Map;

/**
 * Structured operator-facing attention item emitted by Hermes operational APIs.
 */
public record HermesOperationalAttention(
        String source,
        String severity,
        int priority,
        String message,
        String action,
        boolean retryable,
        Map<String, Object> metadata) {

    public HermesOperationalAttention {
        source = HermesResponseMetadata.text(source, "hermes");
        severity = HermesResponseMetadata.text(severity, "info");
        priority = Math.max(priority, 0);
        message = HermesResponseMetadata.text(message, "");
        action = HermesResponseMetadata.text(action, "");
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public static HermesOperationalAttention of(
            String source,
            String severity,
            int priority,
            String message) {
        return new HermesOperationalAttention(
                source,
                severity,
                priority,
                message,
                "",
                false,
                Map.of());
    }

    static List<HermesOperationalAttention> fromMessages(
            String source,
            String severity,
            int priority,
            List<String> messages) {
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }
        return messages.stream()
                .map(message -> HermesResponseMetadata.text(message, ""))
                .filter(message -> !message.isEmpty())
                .distinct()
                .map(message -> of(source, severity, priority, message))
                .toList();
    }
}
