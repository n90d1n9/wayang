package tech.kayys.wayang.rag.runtime;

import tech.kayys.wayang.rag.core.GenerationConfig;

import java.util.Map;

/**
 * Native RAG answer rendering mode.
 */
public enum NativeGenerationMode {
    CONTEXT,
    EXTRACTIVE;

    public static final String PARAM_NATIVE_GENERATION_MODE = "nativeGenerationMode";
    public static final String PARAM_GENERATION_MODE = "generationMode";

    static NativeGenerationMode from(GenerationConfig generationConfig) {
        if (generationConfig == null) {
            return CONTEXT;
        }

        NativeGenerationMode mode = fromParams(generationConfig.additionalParams());
        if (mode != null) {
            return mode;
        }

        return fromLegacyProvider(generationConfig.provider());
    }

    private static NativeGenerationMode fromParams(Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return null;
        }
        NativeGenerationMode mode = parse(params.get(PARAM_NATIVE_GENERATION_MODE));
        if (mode != null) {
            return mode;
        }
        return parse(params.get(PARAM_GENERATION_MODE));
    }

    private static NativeGenerationMode fromLegacyProvider(String provider) {
        String normalized = normalize(provider);
        if (normalized.contains("extract")) {
            return EXTRACTIVE;
        }
        return CONTEXT;
    }

    private static NativeGenerationMode parse(Object value) {
        String normalized = normalize(value == null ? null : value.toString());
        return switch (normalized) {
            case "extract", "extractive", "native-extractive" -> EXTRACTIVE;
            case "context", "concat", "native-context", "contextual" -> CONTEXT;
            default -> null;
        };
    }

    private static String normalize(String value) {
        return RagRuntimeText.trimToLowerEmpty(value);
    }
}
