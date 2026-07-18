package tech.kayys.wayang.gollek.cli;

import picocli.CommandLine.Option;
import tech.kayys.wayang.gollek.sdk.AgentRunHistoryQuery;

final class WayangRunHistoryQueryOptions {

    @Option(names = "--state", description = "Filter by lifecycle state, for example completed or failed.")
    String state;

    @Option(names = "--limit", description = "Maximum matching run statuses to include in the returned window.")
    Integer limit;

    @Option(names = "--offset", description = "Zero-based run history offset for paged reads.")
    Integer offset;

    @Option(names = "--tenant", description = "Filter by tenant id.")
    String tenantId;

    @Option(names = "--session", description = "Filter by session id.")
    String sessionId;

    @Option(names = "--surface", description = "Filter by product surface id.")
    String surfaceId;

    @Option(names = "--profile", description = "Filter by product profile id.")
    String profileId;

    AgentRunHistoryQuery toQuery() {
        return AgentRunHistoryQuery.of(state, limit, tenantId, sessionId, surfaceId, profileId, offset);
    }
}
