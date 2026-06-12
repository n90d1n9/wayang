package tech.kayys.gamelan.cli;

import jakarta.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Option;
import tech.kayys.gamelan.agent.SdkProvider;
import tech.kayys.gamelan.util.AnsiPrinter;
import tech.kayys.gollek.sdk.core.GollekSdk;
import tech.kayys.gollek.sdk.exception.SdkException;
import tech.kayys.gollek.sdk.model.ModelInfo;

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

    @Override
    public void run() {
        new picocli.CommandLine(this).usage(System.out);
    }

    @Command(name = "list", description = "List locally available models", aliases = {"ls"})
    static class ListCmd implements Runnable {

        @Inject SdkProvider sdkProvider;

        @Option(names = {"-f", "--format"}, description = "Filter by format (GGUF, SAFETENSORS, ...)")
        String format;

        @Override
        public void run() {
            AnsiPrinter printer = new AnsiPrinter(true);
            GollekSdk sdk = sdkProvider.sdk();
            try {
                List<ModelInfo> models = format != null
                        ? sdk.listModelsByFormat(
                            tech.kayys.gollek.spi.model.ModelFormat.valueOf(format.toUpperCase()))
                        : sdk.listModels();

                if (models.isEmpty()) {
                    printer.warn("No models found. Run: gamelan models pull <model>");
                    return;
                }
                printer.sectionHeader("Local Models (" + models.size() + ")");
                for (ModelInfo m : models) {
                    printer.listItem(m.id(),
                            "[" + m.format() + "]"
                            + (m.contextWindow() > 0 ? " ctx:" + m.contextWindow() : ""));
                }
            } catch (SdkException e) {
                printer.error("Cannot list models: " + e.getMessage());
                System.exit(1);
            }
        }
    }

    @Command(name = "pull", description = "Pull a model (local name or hf:<org>/<repo>)")
    static class PullCmd implements Runnable {

        @Inject SdkProvider sdkProvider;

        @Parameters(index = "0", description = "Model spec (e.g. llama3 or hf:Qwen/Qwen2.5-0.5B)")
        String modelSpec;

        @Option(names = {"--force-gguf"}, description = "Convert to GGUF after download")
        boolean forceGguf;

        @Option(names = {"-q", "--quant"}, description = "Quantization type (Q4_K_M, Q8_0, etc.)",
                defaultValue = "Q4_K_M")
        String quant;

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
                    sdk.prepareModel(modelSpec, true, p -> printer.info("  " + p.getStatus()));
                }

                printer.success("Done! Use: gamelan --model " + modelSpec);
            } catch (SdkException e) {
                System.out.println();
                printer.error("Pull failed: " + e.getMessage());
                System.exit(1);
            }
        }
    }

    @Command(name = "rm", description = "Delete a local model", aliases = {"remove", "delete"})
    static class RemoveCmd implements Runnable {

        @Inject SdkProvider sdkProvider;

        @Parameters(index = "0", description = "Model ID")
        String modelId;

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

    @Command(name = "info", description = "Show model details")
    static class InfoCmd implements Runnable {

        @Inject SdkProvider sdkProvider;

        @Parameters(index = "0", description = "Model ID")
        String modelId;

        @Override
        public void run() {
            AnsiPrinter printer = new AnsiPrinter(true);
            GollekSdk sdk = sdkProvider.sdk();
            try {
                sdk.getModelInfo(modelId).ifPresentOrElse(info -> {
                    printer.sectionHeader("Model: " + info.id());
                    printer.println("  Format      : " + info.format());
                    printer.println("  Context     : " + info.contextWindow());
                    printer.println("  Streaming   : " + info.supportsStreaming());
                    printer.println("  Tools       : " + info.supportsTools());
                    if (info.capabilities() != null && !info.capabilities().isEmpty()) {
                        printer.println("  Capabilities:");
                        info.capabilities().forEach((k, v) -> printer.println("    " + k + ": " + v));
                    }
                }, () -> printer.error("Model not found: " + modelId));
            } catch (SdkException e) {
                printer.error("Cannot fetch model info: " + e.getMessage());
                System.exit(1);
            }
        }
    }
}
