package tech.kayys.wayang.gollek.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.Parameters;
import tech.kayys.wayang.gollek.sdk.AgentRunInspection;
import tech.kayys.wayang.gollek.sdk.AgentRunStatus;
import tech.kayys.wayang.gollek.sdk.WayangRunApi;

import java.util.concurrent.Callable;

final class WayangRunInspectionCommands {

    private WayangRunInspectionCommands() {
    }

    @Command(
            name = "status",
            description = "Show a lifecycle status snapshot for a run id.",
            mixinStandardHelpOptions = true)
    static final class StatusCommand implements Callable<Integer> {
        @ParentCommand
        WayangRunCommands.RunCommand parent;

        @Parameters(index = "0", description = "Run id to inspect.")
        String runId;

        @Option(names = "--json", description = "Render run status as compact JSON.")
        boolean json;

        @Override
        public Integer call() {
            try {
                WayangCliContext context = parent.context();
                WayangRunApi runs = context.client().runs();
                AgentRunStatus status = runs.status(runId);
                WayangCliRender.jsonOrText(
                        context.out(),
                        json,
                        () -> runs.statusJson(status),
                        () -> WayangRunInspectionTextFormat.statusText(status));
                return status.known() ? 0 : 1;
            } catch (RuntimeException e) {
                return parent.context().commandFailure(e);
            }
        }
    }

    @Command(
            name = "inspect",
            description = "Show run status and lifecycle events for a run id.",
            mixinStandardHelpOptions = true)
    static final class InspectCommand implements Callable<Integer> {
        @ParentCommand
        WayangRunCommands.RunCommand parent;

        @Parameters(index = "0", description = "Run id to inspect.")
        String runId;

        @Option(names = "--json", description = "Render run inspection as compact JSON.")
        boolean json;

        @Mixin
        WayangRunEventQueryOptions query = new WayangRunEventQueryOptions();

        @Override
        public Integer call() {
            try {
                WayangCliContext context = parent.context();
                WayangRunApi runs = context.client().runs();
                AgentRunInspection inspection = runs.inspect(runId, query.toQuery());
                WayangCliRender.jsonOrText(
                        context.out(),
                        json,
                        () -> runs.inspectionJson(inspection),
                        () -> WayangRunInspectionTextFormat.inspectionText(inspection));
                return inspection.empty() ? 1 : 0;
            } catch (RuntimeException e) {
                return parent.context().commandFailure(e);
            }
        }
    }
}
