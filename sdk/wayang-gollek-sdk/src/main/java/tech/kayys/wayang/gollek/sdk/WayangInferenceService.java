package tech.kayys.wayang.gollek.sdk;

import tech.kayys.gollek.sdk.core.ChatParams;
import tech.kayys.gollek.sdk.core.GollekChatService;
import tech.kayys.gollek.sdk.core.GollekSdk;
import tech.kayys.gollek.sdk.exception.SdkException;
import tech.kayys.gollek.spi.Message;
import tech.kayys.gollek.spi.inference.InferenceResponse;
import tech.kayys.gollek.spi.inference.StreamingInferenceChunk;
import tech.kayys.gollek.spi.tool.ToolDefinition;
import io.smallrye.mutiny.Multi;

import java.io.InputStreamReader;
import java.util.List;
import java.util.UUID;

/**
 * Wayang inference service that wraps the Gollek SDK for in-process inference.
 * <p>
 * <b>Architecture:</b><br>
 * When {@code gollekSdk} is available (non-null), all inference is handled in-process
 * via the Gollek SDK. The subprocess fallback ({@code gollek run}) is strictly a
 * last-resort path used only when the SDK itself cannot be initialised.
 * This design ensures that Wayang Code never proxies user prompts to an external
 * shell process as long as the SDK is reachable.
 */
public class WayangInferenceService {

    private final GollekSdk gollekSdk;
    private final String modelId;
    private boolean useSubprocessFallback = false;

    public WayangInferenceService(GollekSdk gollekSdk, String systemPrompt, String modelId) {
        this.gollekSdk = gollekSdk;
        this.modelId = modelId;
        
        // If SDK is null, use subprocess fallback
        if (gollekSdk == null) {
            this.useSubprocessFallback = true;
        }
    }

    public GollekSdk getSdk() {
        return gollekSdk;
    }

    /**
     * Streaming inference with token chunks.
     * <p>
     * When {@code gollekSdk} is available, inference is handled in-process via the
     * Gollek SDK. The subprocess fallback ({@code gollek run}) is strictly reserved for
     * the case where the SDK itself could not be initialised ({@code gollekSdk == null}).
     * This ensures that Wayang never proxies user prompts to an external shell process
     * as long as the SDK is reachable.
     */
    public Multi<StreamingInferenceChunk> inferenceStreaming(
            String modelId,
            String systemPrompt,
            List<Message> history,
            List<ToolDefinition> tools,
            ChatParams params) {
        
        if (useSubprocessFallback || gollekSdk == null) {
            // SDK could not be initialised — last-resort subprocess path.
            System.err.println("[WayangInferenceService] SDK unavailable; using subprocess fallback for model=" + modelId);
            String lastUserText = lastUserText(history);
            return streamViaSubprocess(modelId, systemPrompt, lastUserText, params);
        }
        
        // Use the Gollek SDK in-process. Any failure (including SDK_NO_PROVIDERS when
        // no native GGUF/Safetensor provider is registered) propagates as a stream error
        // so the TUI can surface a clear message rather than spawning a shell process.
        GollekChatService svc = new GollekChatService();
        return svc.streamChat(gollekSdk, modelId, systemPrompt, history, tools, params);
    }

    private String lastUserText(List<Message> history) {
        for (int i = history.size() - 1; i >= 0; i--) {
            Message msg = history.get(i);
            if (msg.getRole() == Message.Role.USER) {
                return msg.getContent() != null ? msg.getContent() : "";
            }
        }
        return "";
    }

    /**
     * Last-resort subprocess fallback: stream inference via {@code gollek run}.
     * Only invoked when {@code gollekSdk == null} (SDK failed to initialise).
     *
     * <p>Reads {@code gollek run} stdout line-by-line and filters out the
     * telemetry/progress lines that gollek writes to stdout (e.g. "Resolved local
     * model index entry", "Performance Metrics:", separator lines, etc.).
     * Only the actual model response text is emitted as streaming chunks.
     */
    private Multi<StreamingInferenceChunk> streamViaSubprocess(String modelId, String systemPrompt, String userPrompt, ChatParams params) {
        return Multi.createFrom().emitter(emitter -> {
            try {
                String effectiveModelId = extractModelId(modelId);

                ProcessBuilder pb = new ProcessBuilder(
                    "gollek", "run", "--no-banner",
                    "--model", effectiveModelId,
                    "--prompt", userPrompt,
                    "--max-tokens", String.valueOf(params.maxTokens()),
                    "--temperature", String.valueOf(params.temperature())
                );
                pb.redirectErrorStream(false);
                pb.redirectError(ProcessBuilder.Redirect.DISCARD);
                pb.environment().put("NO_COLOR", "1");

                Process process = pb.start();

                java.io.BufferedReader reader = new java.io.BufferedReader(
                        new InputStreamReader(process.getInputStream()));
                int chunkIndex = 0;
                boolean inModelOutput = false;
                String line;

                while ((line = reader.readLine()) != null) {
                    // Detect start of model output region: after the "---" separator
                    if (!inModelOutput) {
                        if (line.startsWith("---")) {
                            inModelOutput = true;
                        }
                        // Skip all pre-separator telemetry lines
                        continue;
                    }

                    // Skip telemetry lines that appear inside the model output region
                    if (isGollekTelemetry(line)) {
                        continue;
                    }

                    // Stop emitting when we hit post-output telemetry markers
                    if (line.startsWith("[Fast GGUF") || line.startsWith("[GGUF")
                            || line.startsWith("Performance Metrics:")) {
                        break;
                    }

                    // Emit this line as model output (re-add newline stripped by readLine)
                    String text = line + "\n";
                    emitter.emit(StreamingInferenceChunk.textDelta(
                            UUID.randomUUID().toString(),
                            chunkIndex++,
                            text));
                }

                // Drain remaining stdout silently
                while (reader.readLine() != null) { }

                int exitCode = process.waitFor();
                if (exitCode == 0) {
                    emitter.complete();
                } else {
                    emitter.fail(new RuntimeException("gollek subprocess failed with exit code " + exitCode));
                }
            } catch (Exception e) {
                System.err.println("[WayangInferenceService] Subprocess streaming failed: " + e.getMessage());
                emitter.fail(e);
            }
        });
    }

    /**
     * Extract model ID from a string which may be a ModelInfo object's toString() representation
     * or a plain model ID string.
     */
    private String extractModelId(String modelIdStr) {
        if (modelIdStr == null || modelIdStr.isBlank()) {
            return "unknown-model";
        }
        
        // If it looks like a ModelInfo object, extract the modelId value
        if (modelIdStr.startsWith("ModelInfo{") && modelIdStr.contains("modelId='")) {
            int start = modelIdStr.indexOf("modelId='") + "modelId='".length();
            int end = modelIdStr.indexOf("'", start);
            if (end > start) {
                return modelIdStr.substring(start, end);
            }
        }
        
        return modelIdStr;
    }

    /**
     * Health check for the inference service
     */
    public boolean healthCheck() {
        return gollekSdk != null || useSubprocessFallback;
    }

    /**
     * Returns true if the given line is a known gollek run telemetry/progress line
     * that should be filtered from the model response stream.
     */
    private static boolean isGollekTelemetry(String line) {
        if (line == null) return false;
        return line.startsWith("Using llama.cpp")
                || line.startsWith("Using safetensor")
                || line.startsWith("Resolved local model")
                || line.startsWith("Model: ")
                || line.startsWith("Provider: ")
                || line.startsWith("Execution route:")
                || line.startsWith("[Fast GGUF")
                || line.startsWith("[GGUF")
                || line.startsWith("Performance Metrics:")
                || line.startsWith("  open time")
                || line.startsWith("  generate call")
                || line.startsWith("  generation")
                || line.startsWith("  token latency")
                || line.startsWith("  native ")
                || line.startsWith("Duration:")
                || line.startsWith("Speed:");
    }
}
