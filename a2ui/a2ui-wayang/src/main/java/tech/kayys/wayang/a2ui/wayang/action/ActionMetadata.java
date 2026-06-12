package tech.kayys.wayang.a2ui.wayang.action;

import tech.kayys.wayang.a2ui.wayang.transport.TransportMaps;

import tech.kayys.wayang.gollek.sdk.AgentRunCancelResult;
import tech.kayys.wayang.gollek.sdk.AgentRunEvents;
import tech.kayys.wayang.gollek.sdk.AgentRunHistory;
import tech.kayys.wayang.gollek.sdk.AgentRunInspection;
import tech.kayys.wayang.gollek.sdk.AgentRunWaitResult;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Ordered metadata projections for routed A2UI action results.
 */
public final class ActionMetadata {

    private ActionMetadata() {
    }

    public static Map<String, Object> inspection(AgentRunInspection inspection) {
        AgentRunInspection resolved = Objects.requireNonNull(inspection, "inspection");
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("known", resolved.known());
        values.put("empty", resolved.empty());
        return TransportMaps.freeze(values);
    }

    public static Map<String, Object> history(AgentRunHistory history) {
        AgentRunHistory resolved = Objects.requireNonNull(history, "history");
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("totalRuns", resolved.totalRuns());
        values.put("returnedRuns", resolved.returnedRuns());
        values.put("offset", resolved.offset());
        values.put("hasMore", resolved.hasMore());
        values.put("filtered", resolved.query().filtered());
        return TransportMaps.freeze(values);
    }

    public static Map<String, Object> events(AgentRunEvents events) {
        AgentRunEvents resolved = Objects.requireNonNull(events, "events");
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("totalEvents", resolved.totalEvents());
        values.put("returnedEvents", resolved.returnedEvents());
        values.put("afterSequence", resolved.cursor().afterSequence());
        values.put("nextAfterSequence", resolved.nextAfterSequence());
        values.put("filtered", resolved.query().filtered());
        values.put("truncated", resolved.truncated());
        return TransportMaps.freeze(values);
    }

    public static Map<String, Object> waitResult(AgentRunWaitResult result) {
        AgentRunWaitResult resolved = Objects.requireNonNull(result, "result");
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("terminal", resolved.terminal());
        values.put("timedOut", resolved.timedOut());
        values.put("attempts", resolved.attempts());
        values.put("elapsedMillis", resolved.elapsedMillis());
        return TransportMaps.freeze(values);
    }

    public static Map<String, Object> cancelResult(AgentRunCancelResult result) {
        AgentRunCancelResult resolved = Objects.requireNonNull(result, "result");
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("cancelled", resolved.cancelled());
        return TransportMaps.freeze(values);
    }
}
