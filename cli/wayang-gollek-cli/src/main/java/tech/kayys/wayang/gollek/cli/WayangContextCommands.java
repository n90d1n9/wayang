package tech.kayys.wayang.gollek.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;
import tech.kayys.wayang.gollek.sdk.HarnessPlan;
import tech.kayys.wayang.gollek.sdk.HarnessPlanRequest;
import tech.kayys.wayang.gollek.sdk.WayangContextApi;
import tech.kayys.wayang.gollek.sdk.WorkspaceInspectionRequest;
import tech.kayys.wayang.gollek.sdk.WorkspaceSnapshot;

import java.util.concurrent.Callable;

final class WayangContextCommands {

    private WayangContextCommands() {
    }

    @Command(name = "workspace", aliases = "inspect", description = "Inspect workspace context for coding-agent runs.")
    static final class WorkspaceCommand implements Callable<Integer> {
        @ParentCommand
        WayangGollekCli parent;

        @Option(names = {"-p", "--path"}, description = "Workspace path.", defaultValue = ".")
        String path;

        @Option(names = "--max-entries", description = "Maximum top-level paths to show.", defaultValue = "80")
        int maxEntries;

        @Option(names = "--include-hidden", description = "Include hidden top-level paths.")
        boolean includeHidden;

        @Option(names = "--json", description = "Render workspace as compact JSON.")
        boolean json;

        @Override
        public Integer call() {
            try {
                WayangCliContext context = parent.context();
                WayangContextApi contexts = context.client().contexts();
                WorkspaceSnapshot snapshot = contexts.workspace(new WorkspaceInspectionRequest(
                        path,
                        maxEntries,
                        includeHidden));
                WayangCliRender.jsonOrText(
                        context.out(),
                        json,
                        () -> contexts.workspaceJson(snapshot),
                        () -> WayangWorkspaceTextFormat.text(snapshot));
                return 0;
            } catch (RuntimeException e) {
                return parent.context().commandFailure(e);
            }
        }
    }

    @Command(name = "harness", aliases = "checks", description = "Plan workspace verification checks for agentic changes.")
    static final class HarnessCommand implements Callable<Integer> {
        @ParentCommand
        WayangGollekCli parent;

        @Option(names = {"-p", "--path"}, description = "Workspace path.", defaultValue = ".")
        String path;

        @Option(names = "--max-checks", description = "Maximum planned checks to show.", defaultValue = "8")
        int maxChecks;

        @Option(names = "--required-only", description = "Hide optional checks whose project scripts may not exist.")
        boolean requiredOnly;

        @Option(names = "--json", description = "Render planned checks as compact JSON.")
        boolean json;

        @Override
        public Integer call() {
            try {
                WayangCliContext context = parent.context();
                WayangContextApi contexts = context.client().contexts();
                HarnessPlan plan = contexts.harness(new HarnessPlanRequest(
                        path,
                        maxChecks,
                        !requiredOnly));
                WayangCliRender.jsonOrText(
                        context.out(),
                        json,
                        () -> contexts.harnessJson(plan),
                        () -> WayangHarnessTextFormat.text(plan));
                return 0;
            } catch (RuntimeException e) {
                return parent.context().commandFailure(e);
            }
        }
    }
}
