package tech.kayys.wayang.agent.lifecycle;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import tech.kayys.wayang.client.WayangJsonSchemaDocuments;
import tech.kayys.wayang.contract.WayangContractDescriptor;
import tech.kayys.wayang.contract.WayangContractDescriptors;

/**
 * Schema property definitions for event-related agent run lifecycle envelopes.
 * Handles events, events-follow, and history/list related schemas.
 */
final class AgentRunEventProperties {

    private AgentRunEventProperties() {
    }

    // Required properties lists

    public static List<String> eventsRequired(boolean includeEvents) {
        List<String> required = List.of(
                "contract",
                "runId",
                "outcome",
                "query",
                "cursor",
                "summary",
                "totalEvents",
                "returnedEvents",
                "firstSequence",
                "lastSequence",
                "nextAfterSequence",
                "truncated",
                "stateCounts",
                "typeCounts",
                "stateSummaries",
                "typeSummaries",
                "empty",
                "message");
        return includeEvents ? append(required, "events") : required;
    }

    public static List<String> eventsFollowRequired() {
        return List.of(
                "contract",
                "runId",
                "successful",
                "outcome",
                "terminal",
                "terminalState",
                "terminalEventType",
                "terminalSequence",
                "maxPollsReached",
                "polls",
                "elapsedMillis",
                "initialQuery",
                "nextQuery",
                "nextAfterSequence",
                "empty",
                "message",
                "metadata",
                "lastEvents");
    }

    public static List<String> historyRequired(boolean includeRuns) {
        List<String> required = includeRuns
                ? List.of(
                        "contract",
                        "outcome",
                        "query",
                        "page",
                        "summary",
                        "totalRuns",
                        "returnedRuns",
                        "pageSize",
                        "offset",
                        "windowStart",
                        "windowEnd",
                        "previousOffset",
                        "hasPrevious",
                        "nextOffset",
                        "hasMore",
                        "truncated",
                        "stateCounts",
                        "surfaceCounts",
                        "profileCounts",
                        "strategyCounts",
                        "stateSummaries",
                        "surfaceSummaries",
                        "profileSummaries",
                        "strategySummaries",
                        "empty",
                        "message")
                : List.of(
                        "contract",
                        "outcome",
                        "query",
                        "page",
                        "summary",
                        "totalRuns",
                        "returnedRuns",
                        "stateCounts",
                        "surfaceCounts",
                        "profileCounts",
                        "strategyCounts",
                        "stateSummaries",
                        "surfaceSummaries",
                        "profileSummaries",
                        "strategySummaries",
                        "empty",
                        "message");
        return includeRuns ? append(required, "runs") : required;
    }

    // Property maps

    public static Map<String, Object> eventsProperties(WayangContractDescriptor contract, boolean includeEvents) {
        Map<String, Object> properties = AgentRunSchemaHandler.contractOnlyProperties(contract);
        properties.put("runId", WayangJsonSchemaDocuments.stringProperty());
        properties.put("outcome", WayangJsonSchemaDocuments.stringProperty());
        properties.put("query", eventQueryProperty());
        properties.put("cursor", eventCursorProperty());
        properties.put("summary", eventSummaryProperty());
        properties.put("totalEvents", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("returnedEvents", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("firstSequence", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("lastSequence", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("nextAfterSequence", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("truncated", WayangJsonSchemaDocuments.booleanProperty());
        properties.put("stateCounts", WayangJsonSchemaDocuments.countMapProperty());
        properties.put("typeCounts", WayangJsonSchemaDocuments.countMapProperty());
        properties.put("stateSummaries", WayangJsonSchemaDocuments.arrayProperty(eventFacetSummaryProperty()));
        properties.put("typeSummaries", WayangJsonSchemaDocuments.arrayProperty(eventFacetSummaryProperty()));
        properties.put("empty", WayangJsonSchemaDocuments.booleanProperty());
        properties.put("message", WayangJsonSchemaDocuments.stringProperty());
        if (includeEvents) {
            properties.put("events", WayangJsonSchemaDocuments.arrayProperty(eventProperty()));
        }
        return properties;
    }

    public static Map<String, Object> eventsFollowProperties(WayangContractDescriptor contract) {
        Map<String, Object> properties = AgentRunSchemaHandler.contractOnlyProperties(contract);
        properties.put("runId", WayangJsonSchemaDocuments.stringProperty());
        properties.put("successful", WayangJsonSchemaDocuments.booleanProperty());
        properties.put("outcome", WayangJsonSchemaDocuments.stringProperty());
        properties.put("terminal", WayangJsonSchemaDocuments.booleanProperty());
        properties.put("terminalState", WayangJsonSchemaDocuments.nullableStringProperty());
        properties.put("terminalEventType", WayangJsonSchemaDocuments.nullableStringProperty());
        properties.put("terminalSequence", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("maxPollsReached", WayangJsonSchemaDocuments.booleanProperty());
        properties.put("polls", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("elapsedMillis", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("initialQuery", eventQueryProperty());
        properties.put("nextQuery", eventQueryProperty());
        properties.put("nextAfterSequence", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("empty", WayangJsonSchemaDocuments.booleanProperty());
        properties.put("message", WayangJsonSchemaDocuments.stringProperty());
        properties.put("metadata", WayangJsonSchemaDocuments.openObjectProperty());
        properties.put("lastEvents", lastEventsProperty());
        return properties;
    }

    public static Map<String, Object> historyProperties(WayangContractDescriptor contract, boolean includeRuns) {
        Map<String, Object> properties = AgentRunSchemaHandler.contractOnlyProperties(contract);
        properties.put("outcome", WayangJsonSchemaDocuments.stringProperty());
        properties.put("query", historyQueryProperty());
        properties.put("page", historyPageProperty());
        properties.put("summary", historySummaryProperty());
        properties.put("totalRuns", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("returnedRuns", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        if (includeRuns) {
            properties.put("pageSize", WayangJsonSchemaDocuments.positiveIntegerProperty());
            properties.put("offset", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
            properties.put("windowStart", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
            properties.put("windowEnd", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
            properties.put("previousOffset", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
            properties.put("hasPrevious", WayangJsonSchemaDocuments.booleanProperty());
            properties.put("nextOffset", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
            properties.put("hasMore", WayangJsonSchemaDocuments.booleanProperty());
            properties.put("truncated", WayangJsonSchemaDocuments.booleanProperty());
        }
        properties.put("stateCounts", WayangJsonSchemaDocuments.countMapProperty());
        properties.put("surfaceCounts", WayangJsonSchemaDocuments.countMapProperty());
        properties.put("profileCounts", WayangJsonSchemaDocuments.countMapProperty());
        properties.put("strategyCounts", WayangJsonSchemaDocuments.countMapProperty());
        properties.put("stateSummaries", WayangJsonSchemaDocuments.arrayProperty(historyFacetSummaryProperty()));
        properties.put("surfaceSummaries", WayangJsonSchemaDocuments.arrayProperty(historyFacetSummaryProperty()));
        properties.put("profileSummaries", WayangJsonSchemaDocuments.arrayProperty(historyFacetSummaryProperty()));
        properties.put("strategySummaries", WayangJsonSchemaDocuments.arrayProperty(historyFacetSummaryProperty()));
        properties.put("empty", WayangJsonSchemaDocuments.booleanProperty());
        properties.put("message", WayangJsonSchemaDocuments.stringProperty());
        if (includeRuns) {
            properties.put("runs", WayangJsonSchemaDocuments.arrayProperty(AgentRunStateProperties.statusProperty()));
        }
        return properties;
    }

    // Private event helpers

    private static Map<String, Object> eventQueryProperty() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("state", WayangJsonSchemaDocuments.nullableStringProperty());
        properties.put("type", WayangJsonSchemaDocuments.nullableStringProperty());
        properties.put("afterSequence", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("limit", WayangJsonSchemaDocuments.positiveIntegerProperty());
        properties.put("filtered", WayangJsonSchemaDocuments.booleanProperty());
        return WayangJsonSchemaDocuments.objectProperty(
                List.of("state", "type", "afterSequence", "limit", "filtered"),
                true,
                properties);
    }

    private static Map<String, Object> eventCursorProperty() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("afterSequence", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("firstSequence", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("lastSequence", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("nextAfterSequence", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("limit", WayangJsonSchemaDocuments.positiveIntegerProperty());
        properties.put("totalEvents", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("returnedEvents", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("remainingEvents", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("advanced", WayangJsonSchemaDocuments.booleanProperty());
        properties.put("truncated", WayangJsonSchemaDocuments.booleanProperty());
        properties.put("empty", WayangJsonSchemaDocuments.booleanProperty());
        return WayangJsonSchemaDocuments.objectProperty(
                List.of(
                        "afterSequence",
                        "firstSequence",
                        "lastSequence",
                        "nextAfterSequence",
                        "limit",
                        "totalEvents",
                        "returnedEvents",
                        "remainingEvents",
                        "advanced",
                        "truncated",
                        "empty"),
                true,
                properties);
    }

    private static Map<String, Object> eventSummaryProperty() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("totalEvents", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("returnedEvents", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("stateCounts", WayangJsonSchemaDocuments.countMapProperty());
        properties.put("typeCounts", WayangJsonSchemaDocuments.countMapProperty());
        properties.put("stateSummaries", WayangJsonSchemaDocuments.arrayProperty(eventFacetSummaryProperty()));
        properties.put("typeSummaries", WayangJsonSchemaDocuments.arrayProperty(eventFacetSummaryProperty()));
        properties.put("empty", WayangJsonSchemaDocuments.booleanProperty());
        return WayangJsonSchemaDocuments.objectProperty(
                List.of(
                        "totalEvents",
                        "returnedEvents",
                        "stateCounts",
                        "typeCounts",
                        "stateSummaries",
                        "typeSummaries",
                        "empty"),
                true,
                properties);
    }

    private static Map<String, Object> eventFacetSummaryProperty() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("name", WayangJsonSchemaDocuments.stringProperty());
        properties.put("count", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        return WayangJsonSchemaDocuments.objectProperty(List.of("name", "count"), true, properties);
    }

    private static Map<String, Object> eventProperty() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("runId", WayangJsonSchemaDocuments.stringProperty());
        properties.put("sequence", WayangJsonSchemaDocuments.positiveIntegerProperty());
        properties.put("type", WayangJsonSchemaDocuments.stringProperty());
        properties.put("state", WayangJsonSchemaDocuments.stringProperty());
        properties.put("message", WayangJsonSchemaDocuments.stringProperty());
        properties.put("metadata", WayangJsonSchemaDocuments.openObjectProperty());
        return WayangJsonSchemaDocuments.objectProperty(
                List.of("runId", "sequence", "type", "state", "message", "metadata"),
                true,
                properties);
    }

    static Map<String, Object> eventsProperty(WayangContractDescriptor contract, boolean includeEvents) {
        return WayangJsonSchemaDocuments.objectProperty(
                eventsRequired(includeEvents),
                true,
                eventsProperties(contract, includeEvents));
    }

    private static Map<String, Object> lastEventsProperty() {
        return WayangJsonSchemaDocuments.oneOfProperty(List.of(
                eventsProperty(lifecycleContract(AgentRunLifecycleContract.RUN_EVENTS), true),
                eventsProperty(lifecycleContract(AgentRunLifecycleContract.RUN_EVENTS_STATS), false)));
    }

    // Private history helpers

    private static Map<String, Object> historyQueryProperty() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("state", WayangJsonSchemaDocuments.nullableStringProperty());
        properties.put("limit", WayangJsonSchemaDocuments.positiveIntegerProperty());
        properties.put("offset", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("tenantId", WayangJsonSchemaDocuments.nullableStringProperty());
        properties.put("sessionId", WayangJsonSchemaDocuments.nullableStringProperty());
        properties.put("surfaceId", WayangJsonSchemaDocuments.nullableStringProperty());
        properties.put("profileId", WayangJsonSchemaDocuments.nullableStringProperty());
        properties.put("filtered", WayangJsonSchemaDocuments.booleanProperty());
        return WayangJsonSchemaDocuments.objectProperty(
                List.of("state", "limit", "offset", "tenantId", "sessionId", "surfaceId", "profileId", "filtered"),
                true,
                properties);
    }

    private static Map<String, Object> historyPageProperty() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("totalRuns", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("returnedRuns", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("pageSize", WayangJsonSchemaDocuments.positiveIntegerProperty());
        properties.put("offset", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("windowStart", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("windowEnd", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("previousOffset", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("hasPrevious", WayangJsonSchemaDocuments.booleanProperty());
        properties.put("nextOffset", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("hasMore", WayangJsonSchemaDocuments.booleanProperty());
        properties.put("truncated", WayangJsonSchemaDocuments.booleanProperty());
        properties.put("empty", WayangJsonSchemaDocuments.booleanProperty());
        return WayangJsonSchemaDocuments.objectProperty(
                List.of(
                        "totalRuns",
                        "returnedRuns",
                        "pageSize",
                        "offset",
                        "windowStart",
                        "windowEnd",
                        "previousOffset",
                        "hasPrevious",
                        "nextOffset",
                        "hasMore",
                        "truncated",
                        "empty"),
                true,
                properties);
    }

    private static Map<String, Object> historySummaryProperty() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("totalRuns", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("returnedRuns", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("stateCounts", WayangJsonSchemaDocuments.countMapProperty());
        properties.put("surfaceCounts", WayangJsonSchemaDocuments.countMapProperty());
        properties.put("profileCounts", WayangJsonSchemaDocuments.countMapProperty());
        properties.put("strategyCounts", WayangJsonSchemaDocuments.countMapProperty());
        properties.put("stateSummaries", WayangJsonSchemaDocuments.arrayProperty(historyFacetSummaryProperty()));
        properties.put("surfaceSummaries", WayangJsonSchemaDocuments.arrayProperty(historyFacetSummaryProperty()));
        properties.put("profileSummaries", WayangJsonSchemaDocuments.arrayProperty(historyFacetSummaryProperty()));
        properties.put("strategySummaries", WayangJsonSchemaDocuments.arrayProperty(historyFacetSummaryProperty()));
        properties.put("empty", WayangJsonSchemaDocuments.booleanProperty());
        return WayangJsonSchemaDocuments.objectProperty(
                List.of(
                        "totalRuns",
                        "returnedRuns",
                        "stateCounts",
                        "surfaceCounts",
                        "profileCounts",
                        "strategyCounts",
                        "stateSummaries",
                        "surfaceSummaries",
                        "profileSummaries",
                        "strategySummaries",
                        "empty"),
                true,
                properties);
    }

    private static Map<String, Object> historyFacetSummaryProperty() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("name", WayangJsonSchemaDocuments.stringProperty());
        properties.put("count", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        return WayangJsonSchemaDocuments.objectProperty(List.of("name", "count"), true, properties);
    }

    // Utility methods

    static List<String> withContract(List<String> values) {
        ArrayList<String> copy = new ArrayList<>();
        copy.add("contract");
        copy.addAll(values);
        return List.copyOf(copy);
    }

    private static List<String> append(List<String> values, String value) {
        ArrayList<String> copy = new ArrayList<>(values);
        copy.add(value);
        return List.copyOf(copy);
    }

    private static WayangContractDescriptor lifecycleContract(String envelope) {
        return WayangContractDescriptors.lifecycle(envelope);
    }
}
