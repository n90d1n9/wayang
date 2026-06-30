package tech.kayys.wayang.gollek.sdk.provider;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import tech.kayys.gollek.spi.embedding.EmbeddingRequest;
import tech.kayys.gollek.spi.embedding.EmbeddingResponse;
import tech.kayys.gollek.spi.inference.InferenceRequest;
import tech.kayys.gollek.spi.inference.InferenceResponse;
import tech.kayys.gollek.spi.inference.StreamingInferenceChunk;
import tech.kayys.gollek.spi.provider.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Gollek SDK inference provider that delegates to {@code gollek run} subprocess.
 *
 * <p>This provider has a public no-arg constructor and is registered via Java SPI
 * ({@code META-INF/services/tech.kayys.gollek.spi.provider.LLMProvider}), so it
 * is discovered by {@link java.util.ServiceLoader} in the standalone/manual-init
 * path (i.e., when CDI is unavailable in the shaded CLI JAR).
 *
 * <p>It acts as a catch-all: {@link #supports} returns {@code true} for any model,
 * and inference is performed via {@code gollek run --no-banner}. Gollek telemetry
 * lines are filtered from stdout so that only the model's text response is returned.
 */
public class GollekSubprocessProvider implements StreamingProvider {

    private static final String PROVIDER_ID = "gollek-subprocess";

    /** No-arg constructor required for {@link java.util.ServiceLoader} discovery. */
    public GollekSubprocessProvider() {}

    // ── LLMProvider ──────────────────────────────────────────────────────────

    @Override
    public String id() {
        return PROVIDER_ID;
    }

    @Override
    public String name() {
        return "Gollek Subprocess Provider (gollek run)";
    }

    @Override
    public String version() {
        return "1.0.0";
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean supports(String modelId, ProviderRequest request) {
        // Accept any model; gollek itself validates whether the model exists.
        return true;
    }

    @Override
    public ProviderMetadata metadata() {
        return ProviderMetadata.builder()
                .providerId(PROVIDER_ID)
                .name(name())
                .version(version())
                .build();
    }

    @Override
    public ProviderCapabilities capabilities() {
        return ProviderCapabilities.builder()
                .streaming(true)
                .build();
    }

    @Override
    public Uni<ProviderHealth> health() {
        return Uni.createFrom().item(ProviderHealth.healthy());
    }

    @Override
    public Optional<ProviderMetrics> metrics() {
        return Optional.of(new ProviderMetrics());
    }

    @Override
    public void shutdown() {
        // No-op for subprocess provider
    }

    @Override
    public void initialize(ProviderConfig config) {
        // No-op for subprocess provider
    }

    @Override
    public Uni<InferenceResponse> infer(ProviderRequest request) {
        return Uni.createFrom().emitter(emitter -> {
            try {
                StringBuilder sb = new StringBuilder();
                runGollek(request, line -> sb.append(line).append('\n'));
                emitter.complete(InferenceResponse.builder()
                        .requestId(UUID.randomUUID().toString())
                        .content(sb.toString().trim())
                        .build());
            } catch (Exception e) {
                emitter.fail(e);
            }
        });
    }

    @Override
    public Multi<StreamingInferenceChunk> inferStream(ProviderRequest request) {
        return Multi.createFrom().emitter(emitter -> {
            try {
                int[] idx = {0};
                runGollek(request, line -> emitter.emit(StreamingInferenceChunk.textDelta(
                        UUID.randomUUID().toString(), idx[0]++, line + "\n")));
                emitter.complete();
            } catch (Exception e) {
                emitter.fail(e);
            }
        });
    }

    @Override
    public Uni<EmbeddingResponse> embed(EmbeddingRequest request) {
        return Uni.createFrom().failure(
                new UnsupportedOperationException("Embeddings not supported via gollek run"));
    }

    // ── Internal ─────────────────────────────────────────────────────────────

    /** Runs {@code gollek run} and passes filtered model-output lines to {@code sink}. */
    private void runGollek(ProviderRequest request, java.util.function.Consumer<String> sink)
            throws Exception {
        String modelId = request.getModel();
        String prompt = extractPrompt(request);
        int maxTokens = getIntParam(request, "max_tokens", 4096);
        double temperature = getDoubleParam(request, "temperature", 0.7);

        ProcessBuilder pb = new ProcessBuilder(
                "gollek", "run", "--no-banner",
                "--model", modelId,
                "--prompt", prompt,
                "--max-tokens", String.valueOf(maxTokens),
                "--temperature", String.valueOf(temperature)
        );
        pb.redirectErrorStream(false);
        pb.redirectError(ProcessBuilder.Redirect.DISCARD);
        pb.environment().put("NO_COLOR", "1");

        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {

            boolean inModelOutput = false;
            String line;

            while ((line = reader.readLine()) != null) {
                if (!inModelOutput) {
                    // The "---" separator marks the start of model output
                    if (line.startsWith("---")) {
                        inModelOutput = true;
                    }
                    continue; // skip all pre-separator telemetry
                }

                // Skip known telemetry lines inside the output region
                if (isGollekTelemetry(line)) {
                    continue;
                }

                // Post-output telemetry — stop
                if (line.startsWith("[Fast GGUF") || line.startsWith("[GGUF")
                        || line.startsWith("Performance Metrics:")) {
                    break;
                }

                sink.accept(line);
            }

            // Drain remaining stdout silently
            while (reader.readLine() != null) { }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("gollek run exited with code " + exitCode);
        }
    }

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

    private static String extractPrompt(ProviderRequest request) {
        if (request.getMessages() == null || request.getMessages().isEmpty()) {
            return "";
        }
        // Concatenate all user messages
        StringBuilder sb = new StringBuilder();
        for (tech.kayys.gollek.spi.Message msg : request.getMessages()) {
            if (msg.getRole() == tech.kayys.gollek.spi.Message.Role.USER
                    && msg.getContent() != null) {
                if (sb.length() > 0) sb.append('\n');
                sb.append(msg.getContent());
            }
        }
        return sb.toString();
    }

    private static int getIntParam(ProviderRequest r, String key, int def) {
        Object v = r.getParameters() != null ? r.getParameters().get(key) : null;
        return v instanceof Number ? ((Number) v).intValue() : def;
    }

    private static double getDoubleParam(ProviderRequest r, String key, double def) {
        Object v = r.getParameters() != null ? r.getParameters().get(key) : null;
        return v instanceof Number ? ((Number) v).doubleValue() : def;
    }
}
