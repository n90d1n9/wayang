package tech.kayys.wayang.rag.slo;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import tech.kayys.wayang.rag.runtime.RagRuntimeConfig;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@ApplicationScoped
public class RagSloConfigAdminService {

    @Inject
    RagRuntimeConfig config;

    public RagSloConfigStatus status() {
        return new RagSloConfigStatus(currentThresholds(), Instant.now());
    }

    public RagSloConfigStatus update(RagSloConfigUpdate update) {
        if (update == null) {
            return status();
        }
        if (update.embeddingLatencyP95Ms() != null) {
            config.setSloEmbeddingLatencyP95Ms(update.embeddingLatencyP95Ms());
        }
        if (update.searchLatencyP95Ms() != null) {
            config.setSloSearchLatencyP95Ms(update.searchLatencyP95Ms());
        }
        if (update.ingestLatencyP95Ms() != null) {
            config.setSloIngestLatencyP95Ms(update.ingestLatencyP95Ms());
        }
        if (update.embeddingFailureRate() != null) {
            config.setSloEmbeddingFailureRate(update.embeddingFailureRate());
        }
        if (update.searchFailureRate() != null) {
            config.setSloSearchFailureRate(update.searchFailureRate());
        }
        if (update.indexLagMs() != null) {
            config.setSloIndexLagMs(update.indexLagMs());
        }
        if (update.compactionFailureRate() != null) {
            config.setSloCompactionFailureRate(update.compactionFailureRate());
        }
        if (update.compactionCycleStalenessMs() != null) {
            config.setSloCompactionCycleStalenessMs(update.compactionCycleStalenessMs());
        }
        if (update.severityWarningMultiplier() != null) {
            config.setSloSeverityWarningMultiplier(update.severityWarningMultiplier());
        }
        if (update.severityCriticalMultiplier() != null) {
            config.setSloSeverityCriticalMultiplier(update.severityCriticalMultiplier());
        }
        if (update.severityWarningByMetric() != null) {
            config.setSloSeverityWarningByMetric(update.severityWarningByMetric());
        }
        if (update.severityCriticalByMetric() != null) {
            config.setSloSeverityCriticalByMetric(update.severityCriticalByMetric());
        }
        if (update.alertEnabled() != null) {
            config.setSloAlertEnabled(update.alertEnabled());
        }
        if (update.alertMinSeverity() != null) {
            config.setSloAlertMinSeverity(update.alertMinSeverity());
        }
        if (update.alertCooldownMs() != null) {
            config.setSloAlertCooldownMs(update.alertCooldownMs());
        }
        return status();
    }

    public RagSloConfigStatus reload() {
        Config mp = ConfigProvider.getConfig();
        config.setSloEmbeddingLatencyP95Ms(readDouble(mp, "wayang.rag.slo.embedding-latency-p95-ms",
                config.getSloEmbeddingLatencyP95Ms()));
        config.setSloSearchLatencyP95Ms(readDouble(mp, "wayang.rag.slo.search-latency-p95-ms",
                config.getSloSearchLatencyP95Ms()));
        config.setSloIngestLatencyP95Ms(readDouble(mp, "wayang.rag.slo.ingest-latency-p95-ms",
                config.getSloIngestLatencyP95Ms()));
        config.setSloEmbeddingFailureRate(readDouble(mp, "wayang.rag.slo.embedding-failure-rate",
                config.getSloEmbeddingFailureRate()));
        config.setSloSearchFailureRate(readDouble(mp, "wayang.rag.slo.search-failure-rate",
                config.getSloSearchFailureRate()));
        config.setSloIndexLagMs(readLong(mp, "wayang.rag.slo.index-lag-ms", config.getSloIndexLagMs()));
        config.setSloCompactionFailureRate(readDouble(
                mp,
                "wayang.rag.slo.compaction-failure-rate",
                config.getSloCompactionFailureRate()));
        config.setSloCompactionCycleStalenessMs(readLong(
                mp,
                "wayang.rag.slo.compaction-cycle-staleness-ms",
                config.getSloCompactionCycleStalenessMs()));
        config.setSloSeverityWarningMultiplier(readDouble(
                mp,
                "wayang.rag.slo.severity.warning-multiplier",
                config.getSloSeverityWarningMultiplier()));
        config.setSloSeverityCriticalMultiplier(readDouble(
                mp,
                "wayang.rag.slo.severity.critical-multiplier",
                config.getSloSeverityCriticalMultiplier()));
        config.setSloSeverityWarningByMetric(readMetricMultiplierMap(
                mp,
                "wayang.rag.slo.severity.warning-by-metric",
                config.getSloSeverityWarningByMetric()));
        config.setSloSeverityCriticalByMetric(readMetricMultiplierMap(
                mp,
                "wayang.rag.slo.severity.critical-by-metric",
                config.getSloSeverityCriticalByMetric()));
        config.setSloAlertEnabled(readBoolean(
                mp,
                "wayang.rag.slo.alert.enabled",
                config.isSloAlertEnabled()));
        config.setSloAlertMinSeverity(readString(
                mp,
                "wayang.rag.slo.alert.min-severity",
                config.getSloAlertMinSeverity()));
        config.setSloAlertCooldownMs(readLong(
                mp,
                "wayang.rag.slo.alert.cooldown-ms",
                config.getSloAlertCooldownMs()));
        return status();
    }

    private RagSloThresholds currentThresholds() {
        return new RagSloThresholds(
                config.getSloEmbeddingLatencyP95Ms(),
                config.getSloSearchLatencyP95Ms(),
                config.getSloIngestLatencyP95Ms(),
                config.getSloEmbeddingFailureRate(),
                config.getSloSearchFailureRate(),
                config.getSloIndexLagMs(),
                config.getSloCompactionFailureRate(),
                config.getSloCompactionCycleStalenessMs(),
                config.getSloSeverityWarningMultiplier(),
                config.getSloSeverityCriticalMultiplier(),
                config.getSloSeverityWarningByMetric(),
                config.getSloSeverityCriticalByMetric(),
                config.isSloAlertEnabled(),
                config.getSloAlertMinSeverity(),
                config.getSloAlertCooldownMs());
    }

    private static double readDouble(Config config, String key, double fallback) {
        return config.getOptionalValue(key, String.class)
                .map(String::trim)
                .filter(v -> !v.isBlank())
                .map(v -> parseDouble(v, fallback))
                .orElse(fallback);
    }

    private static long readLong(Config config, String key, long fallback) {
        return config.getOptionalValue(key, String.class)
                .map(String::trim)
                .filter(v -> !v.isBlank())
                .map(v -> parseLong(v, fallback))
                .orElse(fallback);
    }

    private static boolean readBoolean(Config config, String key, boolean fallback) {
        return config.getOptionalValue(key, String.class)
                .map(String::trim)
                .filter(v -> !v.isBlank())
                .map(v -> parseBoolean(v, fallback))
                .orElse(fallback);
    }

    private static String readString(Config config, String key, String fallback) {
        return config.getOptionalValue(key, String.class)
                .map(String::trim)
                .filter(v -> !v.isBlank())
                .orElse(fallback);
    }

    private static double parseDouble(String value, double fallback) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static long parseLong(String value, long fallback) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static boolean parseBoolean(String value, boolean fallback) {
        String normalized = value == null ? "" : value.trim().toLowerCase();
        if ("true".equals(normalized)) {
            return true;
        }
        if ("false".equals(normalized)) {
            return false;
        }
        return fallback;
    }

    private static Map<String, Double> readMetricMultiplierMap(Config config, String key,
            Map<String, Double> fallback) {
        return config.getOptionalValue(key, String.class)
                .map(String::trim)
                .filter(v -> !v.isBlank())
                .map(RagSloConfigAdminService::parseMetricMultiplierMap)
                .orElse(fallback == null ? Map.of() : fallback);
    }

    static Map<String, Double> parseMetricMultiplierMap(String value) {
        if (value == null || value.isBlank()) {
            return Map.of();
        }
        Map<String, Double> parsed = new LinkedHashMap<>();
        for (String token : value.split(",")) {
            String entry = token == null ? "" : token.trim();
            if (entry.isEmpty()) {
                continue;
            }
            int delimiter = entry.indexOf('=');
            if (delimiter <= 0 || delimiter == entry.length() - 1) {
                continue;
            }
            String metric = entry.substring(0, delimiter).trim();
            String raw = entry.substring(delimiter + 1).trim();
            if (metric.isEmpty() || raw.isEmpty()) {
                continue;
            }
            try {
                parsed.put(metric, Double.parseDouble(raw));
            } catch (NumberFormatException ignored) {
            }
        }
        return Map.copyOf(parsed);
    }
}
