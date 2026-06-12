package tech.kayys.wayang.gollek.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;
import tech.kayys.wayang.gollek.sdk.AgentRunHistory;
import tech.kayys.wayang.gollek.sdk.WayangRunApi;

import java.util.concurrent.Callable;

final class WayangRunHistoryCommands {

    private WayangRunHistoryCommands() {
    }

    @Command(
            name = "list",
            description = "List lifecycle status snapshots recorded by this SDK session.",
            mixinStandardHelpOptions = true)
    static final class ListCommand implements Callable<Integer> {
        @ParentCommand
        WayangRunCommands.RunCommand parent;

        @Option(names = "--json", description = "Render run history as compact JSON.")
        boolean json;

        @Mixin
        WayangRunHistoryQueryOptions query = new WayangRunHistoryQueryOptions();

        @Override
        public Integer call() {
            try {
                WayangCliContext context = parent.context();
                WayangRunApi runs = context.client().runs();
                AgentRunHistory history = runs.history(query.toQuery());
                WayangCliRender.jsonOrText(
                        context.out(),
                        json,
                        () -> runs.historyJson(history),
                        () -> WayangRunHistoryTextFormat.historyText(history));
                return 0;
            } catch (RuntimeException e) {
                return parent.context().commandFailure(e);
            }
        }
    }

    @Command(
            name = "stats",
            description = "Summarize lifecycle status snapshots recorded by this SDK session.",
            mixinStandardHelpOptions = true)
    static final class StatsCommand implements Callable<Integer> {
        @ParentCommand
        WayangRunCommands.RunCommand parent;

        @Option(names = "--json", description = "Render run history statistics as compact JSON.")
        boolean json;

        @Mixin
        WayangRunHistoryQueryOptions query = new WayangRunHistoryQueryOptions();

        @Override
        public Integer call() {
            try {
                WayangCliContext context = parent.context();
                WayangRunApi runs = context.client().runs();
                AgentRunHistory history = runs.history(query.toQuery());
                WayangCliRender.jsonOrText(
                        context.out(),
                        json,
                        () -> runs.historyStatsJson(history),
                        () -> WayangRunHistoryTextFormat.historyStatsText(history));
                return 0;
            } catch (RuntimeException e) {
                return parent.context().commandFailure(e);
            }
        }
    }
}
