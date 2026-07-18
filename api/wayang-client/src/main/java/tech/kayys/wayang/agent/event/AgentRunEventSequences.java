package tech.kayys.wayang.agent.event;

import java.util.List;

import tech.kayys.wayang.client.SdkText;

/**
 * Shared sequence policy for generated run lifecycle events.
 */
final public class AgentRunEventSequences {

    private AgentRunEventSequences() {
    }

    public static long nextForRun(List<AgentRunEvent> events, String runId) {
        String normalizedRunId = SdkText.trimToEmpty(runId);
        long maxSequence = 0;
        for (AgentRunEvent event : events == null ? List.<AgentRunEvent>of() : events) {
            if (event != null && event.runId().equals(normalizedRunId)) {
                maxSequence = Math.max(maxSequence, event.sequence());
            }
        }
        return maxSequence == Long.MAX_VALUE ? Long.MAX_VALUE : maxSequence + 1L;
    }
}
