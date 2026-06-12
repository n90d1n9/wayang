package tech.kayys.wayang.a2ui.wayang.surface;

import tech.kayys.wayang.gollek.sdk.AgentRunEvent;
import tech.kayys.wayang.gollek.sdk.AgentRunEvents;
import tech.kayys.wayang.gollek.sdk.AgentRunHistory;
import tech.kayys.wayang.gollek.sdk.AgentRunStatus;

/**
 * Text snippets used by Wayang A2UI surface renderers.
 */
public final class SurfaceText {

    private SurfaceText() {
    }

    public static String historySummary(AgentRunHistory history) {
        return "Showing " + history.windowStart() + "-" + history.windowEnd()
                + " of " + history.totalRuns() + " runs";
    }

    public static String historyMessage(AgentRunHistory history) {
        if (history.message() == null || history.message().isBlank()) {
            return history.empty() ? "No run statuses are recorded." : "Run history loaded.";
        }
        return history.message();
    }

    public static String runLine(AgentRunStatus status) {
        return status.handle().runId()
                + " - "
                + status.handle().state().name()
                + " - "
                + status.message();
    }

    public static String eventsSummary(AgentRunEvents events) {
        return events.returnedEvents()
                + " of "
                + events.totalEvents()
                + " events, next sequence "
                + events.nextAfterSequence();
    }

    public static String eventsMessage(AgentRunEvents events) {
        if (events.message() == null || events.message().isBlank()) {
            return events.empty() ? "No run events are recorded." : "Run events loaded.";
        }
        return events.message();
    }

    public static String eventLine(AgentRunEvent event) {
        return "#"
                + event.sequence()
                + " - "
                + event.type()
                + " - "
                + event.state().name()
                + " - "
                + event.message();
    }
}
