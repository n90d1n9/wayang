package tech.kayys.wayang.gollek.cli;

import picocli.CommandLine.Option;
import tech.kayys.wayang.gollek.sdk.AgentRunEventsQuery;

final class WayangRunEventQueryOptions {

    @Option(names = "--state", description = "Filter lifecycle events by state, for example completed.")
    String state;

    @Option(names = "--type", description = "Filter lifecycle events by type, for example run.completed.")
    String type;

    @Option(names = "--after-sequence", description = "Return events with sequence greater than this value.")
    Long afterSequence;

    @Option(names = "--limit", description = "Maximum latest matching events to render.")
    Integer limit;

    AgentRunEventsQuery toQuery() {
        return AgentRunEventsQuery.of(state, type, afterSequence, limit);
    }
}
