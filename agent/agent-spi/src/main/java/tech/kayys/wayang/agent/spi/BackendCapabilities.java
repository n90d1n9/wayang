package tech.kayys.wayang.agent.spi;

import java.util.List;

/**
 * Backend capabilities record.
 * Used to query what features a backend supports.
 *
 * @param streaming         Supports streaming responses
 * @param toolCalling       Supports native tool calling (function calling)
 * @param multimodal        Supports multimodal inputs (images, audio)
 * @param structuredOutput  Supports structured outputs (JSON schema validation)
 * @param parallelTools     Supports parallel tool calls
 * @param vision            Supports vision/image understanding
 * @param audio             Supports audio/speech processing
 * @param embedding         Supports embedding generation
 * @param maxConcurrentRuns Maximum concurrent inference runs
 * @param supportedModels   List of supported model identifiers
 */
public record BackendCapabilities(
        boolean streaming,
        boolean toolCalling,
        boolean multimodal,
        boolean structuredOutput,
        boolean parallelTools,
        boolean vision,
        boolean audio,
        boolean embedding,
        int maxConcurrentRuns,
        List<String> supportedModels) {

    public static BackendCapabilities none() {
        return new BackendCapabilities(
            false, false, false, false, false,
            false, false, false, 0, List.of()
        );
    }

    public static BackendCapabilities full() {
        return new BackendCapabilities(
            true, true, true, true, true,
            true, true, true,
            Integer.MAX_VALUE,
            List.of("*")
        );
    }

    /**
     * Check if all requested capabilities are supported.
     */
    public boolean supportsAll(boolean... caps) {
        boolean[] all = {streaming, toolCalling, multimodal, structuredOutput,
                        parallelTools, vision, audio, embedding};
        for (boolean cap : caps) {
            if (!cap) return false;
        }
        return true;
    }
}
