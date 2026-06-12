package tech.kayys.wayang.gollek.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;
import tech.kayys.wayang.gollek.sdk.AgentRunPreview;
import tech.kayys.wayang.gollek.sdk.AgentRunReadiness;
import tech.kayys.wayang.gollek.sdk.AgentRunRequest;
import tech.kayys.wayang.gollek.sdk.AgentRunResult;
import tech.kayys.wayang.gollek.sdk.WayangRunApi;
import tech.kayys.wayang.gollek.sdk.WayangRunSpec;
import tech.kayys.wayang.gollek.sdk.WayangSpecApi;

import java.util.concurrent.Callable;

/**
 * CLI command module for Wayang run lifecycle operations.
 *
 * <p>The module translates command-line options into SDK facade calls and keeps
 * run lifecycle behavior in {@link WayangRunApi} and run-spec behavior in
 * {@link WayangSpecApi}.</p>
 */
final class WayangRunCommands {

    private WayangRunCommands() {
    }

    @Command(
            name = "run",
            description = "Prepare an agentic run through Wayang boundaries.",
            mixinStandardHelpOptions = true,
            subcommands = {
                    WayangRunControlCommands.CancelCommand.class,
                    WayangRunEventCommands.EventsCommand.class,
                    WayangRunControlCommands.ForgetCommand.class,
                    WayangRunInspectionCommands.InspectCommand.class,
                    WayangRunHistoryCommands.ListCommand.class,
                    WayangRunHistoryCommands.StatsCommand.class,
                    WayangRunStoreCommands.StoreCommand.class,
                    WayangRunInspectionCommands.StatusCommand.class,
                    WayangRunControlCommands.WaitCommand.class
            })
    static final class RunCommand implements Callable<Integer> {
        @ParentCommand
        WayangGollekCli parent;

        @Mixin
        WayangRunRequestOptions requestOptions = new WayangRunRequestOptions();

        @Option(names = "--json", description = "Render prepared run as compact JSON.")
        boolean json;

        @Option(names = "--preflight", description = "Assess surface policy and skill readiness without preparing a run.")
        boolean preflight;

        @Option(names = "--dry-run", description = "Render the normalized core run preview without submitting a run.")
        boolean dryRun;

        @Option(names = "--require-ready", description = "Refuse to submit unless surface policy and skill readiness pass.")
        boolean requireReady;

        @Option(names = "--print-spec", description = "Print the resolved run request as a Java properties spec without submitting a run.")
        boolean printSpec;

        @Option(names = "--output", paramLabel = "<path>", description = "Write --print-spec output to a UTF-8 file.")
        String specOutputPath;

        @Option(names = "--force", description = "Allow --output to overwrite an existing file.")
        boolean forceOutput;

        @Override
        public Integer call() {
            try {
                WayangCliContext context = context();
                WayangSpecApi specs = context.client().specs();
                WayangCliOutputTarget output = WayangCliOutputTarget.of(specOutputPath, forceOutput);
                output.ensureSupported(printSpec, "--output is only supported with --print-spec.");
                WayangRunSpec sourceSpec = requestOptions.readSpecOrDefault(specs);
                AgentRunRequest request = requestOptions.toRequest(sourceSpec.request(), context.in());
                boolean resolvedRequireReady = requireReady || sourceSpec.requireReady();
                WayangRunApi runs = context.client().runs();
                if (printSpec) {
                    String specText = specs.format(WayangRunSpec.of(
                            sourceSpec.profileId(),
                            request,
                            resolvedRequireReady));
                    output.writeOrPrint(context.out(), specs::writeProperties, "Wayang run spec", specText);
                    return 0;
                }
                if (preflight) {
                    AgentRunReadiness readiness = runs.preflight(request);
                    WayangCliRender.jsonOrText(
                            context.out(),
                            json,
                            () -> runs.preflightJson(readiness),
                            () -> WayangRunPreflightTextFormat.text(readiness));
                    return readiness.ready() ? 0 : 1;
                }
                if (dryRun) {
                    AgentRunPreview preview = runs.preview(request);
                    WayangCliRender.jsonOrText(
                            context.out(),
                            json,
                            () -> runs.previewJson(preview),
                            () -> WayangRunPreviewTextFormat.text(preview));
                    return preview.ready() ? 0 : 1;
                }
                if (resolvedRequireReady) {
                    AgentRunReadiness readiness = runs.preflight(request);
                    if (!readiness.ready()) {
                        WayangCliRender.jsonOrText(
                                context.out(),
                                json,
                                () -> runs.preflightJson(readiness),
                                () -> WayangRunPreflightTextFormat.text(readiness));
                        return 1;
                    }
                }
                AgentRunResult result = runs.run(request);
                WayangCliRender.jsonOrText(
                        context.out(),
                        json,
                        () -> runs.resultJson(result),
                        () -> WayangRunResultTextFormat.text(result));
                return result.successful() ? 0 : 1;
            } catch (RuntimeException e) {
                return context().commandFailure(e);
            }
        }

        WayangCliContext context() {
            return parent.context();
        }
    }
}
