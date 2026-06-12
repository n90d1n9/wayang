package tech.kayys.wayang.gollek.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.Parameters;
import tech.kayys.wayang.gollek.sdk.AgentRunEvents;
import tech.kayys.wayang.gollek.sdk.AgentRunEventsFollowOptions;
import tech.kayys.wayang.gollek.sdk.AgentRunEventsFollowResult;
import tech.kayys.wayang.gollek.sdk.WayangRunApi;

import java.util.concurrent.Callable;
import java.util.function.Consumer;

final class WayangRunEventCommands {

    private WayangRunEventCommands() {
    }

    @Command(
            name = "events",
            description = "Show a lifecycle event timeline for a run id.",
            mixinStandardHelpOptions = true)
    static final class EventsCommand implements Callable<Integer> {
        @ParentCommand
        WayangRunCommands.RunCommand parent;

        @Parameters(index = "0", description = "Run id to inspect.")
        String runId;

        @Option(names = "--json", description = "Render run events as compact JSON.")
        boolean json;

        @Option(names = "--stats", description = "Render cursor and summary envelopes without event rows.")
        boolean stats;

        @Option(names = "--follow", description = "Poll for new lifecycle events until a terminal event or max polls.")
        boolean follow;

        @Option(names = "--follow-result", description = "Render a final follow result envelope after streamed event windows.")
        boolean followResult;

        @Option(names = "--follow-result-only", description = "Render only the final follow result envelope.")
        boolean followResultOnly;

        @Option(names = "--poll-millis", description = "Milliseconds between --follow polls.", defaultValue = "1000")
        Long pollMillis;

        @Option(
                names = "--max-polls",
                description = "Maximum --follow polls before returning non-zero.",
                defaultValue = "60")
        Integer maxPolls;

        @Mixin
        WayangRunEventQueryOptions query = new WayangRunEventQueryOptions();

        @Override
        public Integer call() {
            try {
                if (followResult && !follow) {
                    throw new IllegalArgumentException("--follow-result is only supported with --follow.");
                }
                if (followResultOnly && !follow) {
                    throw new IllegalArgumentException("--follow-result-only is only supported with --follow.");
                }
                if (follow) {
                    return followEvents();
                }
                WayangRunApi runs = parent.context().client().runs();
                AgentRunEvents events = runs.events(runId, query.toQuery());
                render(runs, events);
                return events.empty() ? 1 : 0;
            } catch (RuntimeException e) {
                return parent.context().commandFailure(e);
            }
        }

        private Integer followEvents() {
            WayangCliContext context = parent.context();
            WayangRunApi runs = context.client().runs();
            Consumer<AgentRunEvents> eventConsumer = followResultOnly ? null : events -> render(runs, events);
            AgentRunEventsFollowResult result = runs.followEvents(
                    runId,
                    AgentRunEventsFollowOptions.of(query.toQuery(), maxPolls, pollMillis),
                    eventConsumer);
            if (followResult || followResultOnly) {
                WayangCliRender.jsonOrText(
                        context.out(),
                        json,
                        () -> runs.followResultJson(result, stats),
                        () -> WayangRunEventFollowTextFormat.text(result));
            }
            return result.successful() ? 0 : 1;
        }

        private void render(WayangRunApi runs, AgentRunEvents events) {
            WayangCliRender.jsonOrText(
                    parent.context().out(),
                    json,
                    () -> stats
                            ? runs.eventsStatsJson(events)
                            : runs.eventsJson(events),
                    () -> stats
                            ? WayangRunEventTextFormat.eventsStatsText(events)
                            : WayangRunEventTextFormat.eventsText(events));
        }
    }
}
