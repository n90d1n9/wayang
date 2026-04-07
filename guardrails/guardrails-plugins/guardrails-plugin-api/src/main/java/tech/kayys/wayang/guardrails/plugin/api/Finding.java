package tech.kayys.wayang.guardrails.plugin.api;

/**
 * A specific finding within a detection check.
 */
public record Finding(
        String type,
        String value,
        int start,
        int end,
        double confidence) {
}
