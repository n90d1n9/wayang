package tech.kayys.wayang.rag.retrieval;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.rag.runtime.RagRuntimeConfig;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import java.time.Instant;

@ApplicationScoped
public class RagRetrievalEvalGuardrailConfigAdminService {

    @Inject
    RagRuntimeConfig config;

    public RagRetrievalEvalGuardrailConfigStatus status() {
        return new RagRetrievalEvalGuardrailConfigStatus(current(), Instant.now());
    }

    public RagRetrievalEvalGuardrailConfigStatus update(RagRetrievalEvalGuardrailConfigUpdate update) {
        if (update == null) {
            return status();
        }
        if (update.enabled() != null) {
            config.setRetrievalEvalGuardrailEnabled(update.enabled());
        }
        if (update.windowSize() != null) {
            config.setRetrievalEvalGuardrailWindowSize(update.windowSize());
        }
        if (update.recallDropMax() != null) {
            config.setRetrievalEvalGuardrailRecallDropMax(update.recallDropMax());
        }
        if (update.mrrDropMax() != null) {
            config.setRetrievalEvalGuardrailMrrDropMax(update.mrrDropMax());
        }
        if (update.latencyP95IncreaseMaxMs() != null) {
            config.setRetrievalEvalGuardrailLatencyP95IncreaseMaxMs(update.latencyP95IncreaseMaxMs());
        }
        if (update.latencyAvgIncreaseMaxMs() != null) {
            config.setRetrievalEvalGuardrailLatencyAvgIncreaseMaxMs(update.latencyAvgIncreaseMaxMs());
        }
        return status();
    }

    public RagRetrievalEvalGuardrailConfigStatus reload() {
        Config mp = ConfigProvider.getConfig();
        config.setRetrievalEvalGuardrailEnabled(readBoolean(
                mp,
                "rag.runtime.eval.retrieval.guardrail.enabled",
                config.isRetrievalEvalGuardrailEnabled()));
        config.setRetrievalEvalGuardrailWindowSize(readInt(
                mp,
                "rag.runtime.eval.retrieval.guardrail.window-size",
                config.getRetrievalEvalGuardrailWindowSize()));
        config.setRetrievalEvalGuardrailRecallDropMax(readDouble(
                mp,
                "rag.runtime.eval.retrieval.guardrail.recall-drop-max",
                config.getRetrievalEvalGuardrailRecallDropMax()));
        config.setRetrievalEvalGuardrailMrrDropMax(readDouble(
                mp,
                "rag.runtime.eval.retrieval.guardrail.mrr-drop-max",
                config.getRetrievalEvalGuardrailMrrDropMax()));
        config.setRetrievalEvalGuardrailLatencyP95IncreaseMaxMs(readDouble(
                mp,
                "rag.runtime.eval.retrieval.guardrail.latency-p95-increase-max-ms",
                config.getRetrievalEvalGuardrailLatencyP95IncreaseMaxMs()));
        config.setRetrievalEvalGuardrailLatencyAvgIncreaseMaxMs(readDouble(
                mp,
                "rag.runtime.eval.retrieval.guardrail.latency-avg-increase-max-ms",
                config.getRetrievalEvalGuardrailLatencyAvgIncreaseMaxMs()));
        return status();
    }

    private RagRetrievalEvalGuardrailConfig current() {
        return new RagRetrievalEvalGuardrailConfig(
                config.isRetrievalEvalGuardrailEnabled(),
                config.getRetrievalEvalGuardrailWindowSize(),
                config.getRetrievalEvalGuardrailRecallDropMax(),
                config.getRetrievalEvalGuardrailMrrDropMax(),
                config.getRetrievalEvalGuardrailLatencyP95IncreaseMaxMs(),
                config.getRetrievalEvalGuardrailLatencyAvgIncreaseMaxMs());
    }

    private static double readDouble(Config config, String key, double fallback) {
        return config.getOptionalValue(key, String.class)
                .map(String::trim)
                .filter(v -> !v.isBlank())
                .map(v -> parseDouble(v, fallback))
                .orElse(fallback);
    }

    private static int readInt(Config config, String key, int fallback) {
        return config.getOptionalValue(key, String.class)
                .map(String::trim)
                .filter(v -> !v.isBlank())
                .map(v -> parseInt(v, fallback))
                .orElse(fallback);
    }

    private static boolean readBoolean(Config config, String key, boolean fallback) {
        return config.getOptionalValue(key, String.class)
                .map(String::trim)
                .filter(v -> !v.isBlank())
                .map(v -> parseBoolean(v, fallback))
                .orElse(fallback);
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static double parseDouble(String value, double fallback) {
        try {
            return Double.parseDouble(value);
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
}
