package tech.kayys.wayang.rag.core;

import java.util.List;
import java.util.Map;

/**
 * Standard generation configuration for RAG operations.
 * This class serves as the single source of truth for RAG generation
 * parameters.
 */
public class GenerationConfig {
    private final String provider;
    private final String model;
    private final float temperature;
    private final int maxTokens;
    private final float topP;
    private final float frequencyPenalty;
    private final float presencePenalty;
    private final List<String> stopSequences;
    private final String systemPrompt;
    private final Map<String, Object> additionalParams;
    private final boolean enableCitations;
    private final boolean enableGrounding;
    private final CitationStyle citationStyle;
    private final boolean enableFactualityChecks;
    private final boolean enableBiasDetection;
    private final Map<String, Object> safetySettings;

    public GenerationConfig(String provider, String model, float temperature, int maxTokens,
            float topP, float frequencyPenalty, float presencePenalty,
            List<String> stopSequences, String systemPrompt,
            Map<String, Object> additionalParams, boolean enableCitations,
            boolean enableGrounding, CitationStyle citationStyle,
            boolean enableFactualityChecks, boolean enableBiasDetection,
            Map<String, Object> safetySettings) {
        this.provider = provider;
        this.model = model;
        this.temperature = temperature;
        this.maxTokens = maxTokens;
        this.topP = topP;
        this.frequencyPenalty = frequencyPenalty;
        this.presencePenalty = presencePenalty;
        this.stopSequences = stopSequences != null ? stopSequences : List.of();
        this.systemPrompt = systemPrompt;
        this.additionalParams = additionalParams != null ? additionalParams : Map.of();
        this.enableCitations = enableCitations;
        this.enableGrounding = enableGrounding;
        this.citationStyle = citationStyle;
        this.enableFactualityChecks = enableFactualityChecks;
        this.enableBiasDetection = enableBiasDetection;
        this.safetySettings = safetySettings != null ? safetySettings : Map.of();
    }

    public static GenerationConfig defaults() {
        return new GenerationConfig("openai", "gpt-4", 0.7f, 1024, 1.0f, 0.0f, 0.0f,
                List.of(), "You are a helpful assistant.",
                Map.of(), false, false, CitationStyle.INLINE_NUMBERED,
                false, false, Map.of());
    }

    public String provider() {
        return provider;
    }

    public String model() {
        return model;
    }

    public float temperature() {
        return temperature;
    }

    public int maxTokens() {
        return maxTokens;
    }

    public float topP() {
        return topP;
    }

    public float frequencyPenalty() {
        return frequencyPenalty;
    }

    public float presencePenalty() {
        return presencePenalty;
    }

    public List<String> stopSequences() {
        return stopSequences;
    }

    public String systemPrompt() {
        return systemPrompt;
    }

    public Map<String, Object> additionalParams() {
        return additionalParams;
    }

    public boolean enableCitations() {
        return enableCitations;
    }

    public boolean enableGrounding() {
        return enableGrounding;
    }

    public CitationStyle citationStyle() {
        return citationStyle;
    }

    public boolean enableFactualityChecks() {
        return enableFactualityChecks;
    }

    public boolean enableBiasDetection() {
        return enableBiasDetection;
    }

    public Map<String, Object> safetySettings() {
        return safetySettings;
    }
}
