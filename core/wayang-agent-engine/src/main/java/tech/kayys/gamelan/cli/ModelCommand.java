package tech.kayys.gamelan.cli;

import jakarta.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Option;
import tech.kayys.gamelan.agent.SdkProvider;
import tech.kayys.gamelan.util.AnsiPrinter;
import tech.kayys.gollek.sdk.core.GollekSdk;
import tech.kayys.gollek.sdk.exception.SdkException;
import tech.kayys.gollek.spi.model.ModelInfo;

import java.util.List;

/**
 * Model management subcommand. Wraps the Gollek SDK model operations.
 *
 * <pre>
 * Usage:
 *   gamelan models              # List locally available models
 *   gamelan models pull qwen2   # Pull a model (supports HuggingFace: hf:org/repo)
 *   gamelan models rm qwen2     # Delete a local model
 *   gamelan models info qwen2   # Show model details
 * </pre>
 */
@Command(
    name = "models",
    description = "Manage local LLM models via the Gollek engine",
    mixinStandardHelpOptions = true,
    subcommands = {
        ModelCommand.ListCmd.class,
        ModelCommand.PullCmd.class,
        ModelCommand.RemoveCmd.class,
        ModelCommand.InfoCmd.class
    }
)
public class ModelCommand implements Runnable {

    private String formatSize(Long bytes) {
        if (bytes == null) return "Unknown";
        double m = bytes / 1048576.0;
        if (m > 1024) return String.format("%.2f GB", m / 1024.0);
        return String.format("%.2f MB", m);
    }

    @Override
        public void run() {
        new picocli.CommandLine(this).usage(System.out);
    }

    // ── models list ────────────────────────────────────────────────────────

    @Command(name = "list", description = "List locally available models", aliases = {"ls"})
    static class ListCmd implements Runnable {

        @Inject
        SdkProvider sdkProvider;

        @Option(names = {"-f", "--format"}, description = "Filter by format (GGUF, SAFETENSORS, ...)")
        String format;

        private String formatSize(Long bytes) {
        if (bytes == null) return "Unknown";
        double m = bytes / 1048576.0;
        if (m > 1024) return String.format("%.2f GB", m / 1024.0);
        return String.format("%.2f MB", m);
    }

    @Override
        public void run() {
            AnsiPrinter printer = new AnsiPrinter(true);
            GollekSdk sdk = sdkProvider.sdk();
            try {
                List<ModelInfo> models = sdk.listModels();
                if (format != null) {
                    models = models.stream()
                            .filter(m -> format.equalsIgnoreCase(m.getFormat()))
                            .toList();
                }

                if (models.isEmpty()) {
                    printer.warn("No models found. Run: gamelan models pull <model>");
                    return;
                }
                printer.sectionHeader("Local Models (" + models.size() + ")");
                for (ModelInfo m : models) {
                    printer.listItem(m.getModelId(),
                            "[" + m.getFormat() + "] " + formatSize(m.getSizeBytes())
                            + (m.getQuantization() != null ? " " + m.getQuantization() : ""));
                }
            } catch (SdkException e) {
                printer.error("Cannot list models: " + e.getMessage());
                System.exit(1);
            }
        }
    }

    // ── models pull ────────────────────────────────────────────────────────

    @Command(name = "pull", description = "Pull a model (local name or hf:<org>/<repo>)")
    static class PullCmd implements Runnable {

        @Inject
        SdkProvider sdkProvider;

        @Parameters(index = "0", description = "Model spec (e.g. llama3 or hf:Qwen/Qwen2.5-0.5B)")
        String modelSpec;

        @Option(names = {"--force-gguf"}, description = "Convert to GGUF after download")
        boolean forceGguf;

        @Option(names = {"-q", "--quant"}, description = "Quantization type (Q4_K_M, Q8_0, etc.)",
                defaultValue = "Q4_K_M")
        String quant;

        private String formatSize(Long bytes) {
        if (bytes == null) return "Unknown";
        double m = bytes / 1048576.0;
        if (m > 1024) return String.format("%.2f GB", m / 1024.0);
        return String.format("%.2f MB", m);
    }

    @Override
        public void run() {
            AnsiPrinter printer = new AnsiPrinter(true);
            GollekSdk sdk = sdkProvider.sdk();
            printer.info("Pulling model: " + modelSpec);

            try {
                sdk.pullModel(modelSpec, progress -> {
                    String bar = progress.getProgressBar(30);
                    System.out.printf("\r  [%s] %3d%% — %s",
                            bar, progress.getPercentComplete(), progress.getStatus());
                    System.out.flush();
                });
                System.out.println();

                if (forceGguf) {
                    printer.info("Converting to GGUF [" + quant + "]...");
                    sdk.prepareModel(modelSpec, true, quant, p ->
                            printer.info("  " + p.getStatus()));
                }

                printer.success("Done! Use: gamelan --model " + modelSpec);
            } catch (SdkException e) {
                System.out.println();
                printer.error("Pull failed: " + e.getMessage());
                System.exit(1);
            }
        }
    }

    // ── models rm ─────────────────────────────────────────────────────────

    @Command(name = "rm", description = "Delete a local model", aliases = {"remove", "delete"})
    static class RemoveCmd implements Runnable {

        @Inject
        SdkProvider sdkProvider;

        @Parameters(index = "0", description = "Model ID")
        String modelId;

        private String formatSize(Long bytes) {
        if (bytes == null) return "Unknown";
        double m = bytes / 1048576.0;
        if (m > 1024) return String.format("%.2f GB", m / 1024.0);
        return String.format("%.2f MB", m);
    }

    @Override
        public void run() {
            AnsiPrinter printer = new AnsiPrinter(true);
            GollekSdk sdk = sdkProvider.sdk();
            try {
                sdk.deleteModel(modelId);
                printer.success("Deleted: " + modelId);
            } catch (SdkException e) {
                printer.error("Delete failed: " + e.getMessage());
                System.exit(1);
            }
        }
    }

    // ── models info ────────────────────────────────────────────────────────

    @Command(name = "info", description = "Show model details")
    static class InfoCmd implements Runnable {

        @Inject
        SdkProvider sdkProvider;

        @Parameters(index = "0", description = "Model ID")
        String modelId;

        private String formatSize(Long bytes) {
        if (bytes == null) return "Unknown";
        double m = bytes / 1048576.0;
        if (m > 1024) return String.format("%.2f GB", m / 1024.0);
        return String.format("%.2f MB", m);
    }

    @Override
        public void run() {
            AnsiPrinter printer = new AnsiPrinter(true);
            GollekSdk sdk = sdkProvider.sdk();
            try {
                sdk.getModelInfo(modelId).ifPresentOrElse(info -> {
                    printer.sectionHeader("Model: " + info.getModelId());
                    printer.println("  Name        : " + info.getName());
                    printer.println("  Format      : " + info.getFormat());
                    printer.println("  Size        : " + info.getSizeFormatted());
                    printer.println("  Quantization: " + info.getQuantization());
                    printer.println("  Created     : " + info.getCreatedAt());
                    printer.println("  Updated     : " + info.getUpdatedAt());
                    if (info.getMetadata() != null && !info.getMetadata().isEmpty()) {
                        printer.println("  Metadata:");
                        info.getMetadata().forEach((k, v) ->
                                printer.println("    " + k + ": " + v));
                    }
                }, () -> printer.error("Model not found: " + modelId));
            } catch (SdkException e) {
                printer.error("Cannot fetch model info: " + e.getMessage());
                System.exit(1);
            }
        }
    }
}
