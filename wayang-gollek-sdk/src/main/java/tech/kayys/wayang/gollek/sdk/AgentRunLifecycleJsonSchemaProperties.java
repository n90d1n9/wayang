package tech.kayys.wayang.gollek.sdk;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class AgentRunLifecycleJsonSchemaProperties {

    private AgentRunLifecycleJsonSchemaProperties() {
    }

    static List<String> resultRequired() {
        return List.of(
                "contract",
                "runId",
                "answer",
                "successful",
                "outcome",
                "strategy",
                "handle",
                "steps",
                "metadata");
    }

    static List<String> statusEnvelopeRequired() {
        return withContract(statusRequired());
    }

    static List<String> eventsRequired(boolean includeEvents) {
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

    static List<String> eventsFollowRequired() {
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

    static List<String> inspectionRequired() {
        return List.of("contract", "runId", "outcome", "known", "empty", "message", "status", "events");
    }

    static List<String> historyRequired(boolean includeRuns) {
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

    static List<String> waitRequired() {
        return List.of(
                "contract",
                "runId",
                "outcome",
                "terminal",
                "timedOut",
                "attempts",
                "elapsedMillis",
                "status",
                "message",
                "metadata");
    }

    static List<String> cancelRequired() {
        return List.of("contract", "runId", "cancelled", "outcome", "handle", "message", "metadata");
    }

    static List<String> forgetRequired() {
        return List.of("contract", "runId", "forgotten", "outcome", "message", "metadata");
    }

    static List<String> runStoreRequired() {
        return withContract(runStoreDiagnosticsRequired());
    }

    static List<String> runStoreVerificationRequired() {
        return List.of(
                "contract",
                "passed",
                "exitCode",
                "issueCount",
                "errorCount",
                "warningCount",
                "policy",
                "issues",
                "diagnostics");
    }

    static List<String> runStoreCompactionPreviewRequired() {
        return List.of(
                "contract",
                "dryRun",
                "previewable",
                "compactionNeeded",
                "exitCode",
                "issueCount",
                "errorCount",
                "warningCount",
                "issues",
                "message",
                "retentionAssessment",
                "backupRetention",
                "diagnostics");
    }

    static List<String> runStoreCompactionResultRequired() {
        return List.of(
                "contract",
                "applied",
                "compacted",
                "backupCreated",
                "backupPath",
                "backupRetention",
                "successful",
                "compactionNeeded",
                "exitCode",
                "issueCount",
                "errorCount",
                "warningCount",
                "issues",
                "message",
                "retentionAssessment",
                "beforeDiagnostics",
                "afterDiagnostics");
    }

    static Map<String, Object> contractOnlyProperties(WayangContractDescriptor contract) {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("contract", WayangJsonSchemaDocuments.contractProperty(contract));
        return properties;
    }

    static Map<String, Object> resultProperties(WayangContractDescriptor contract) {
        Map<String, Object> properties = contractOnlyProperties(contract);
        properties.put("runId", WayangJsonSchemaDocuments.stringProperty());
        properties.put("answer", WayangJsonSchemaDocuments.stringProperty());
        properties.put("successful", WayangJsonSchemaDocuments.booleanProperty());
        properties.put("outcome", WayangJsonSchemaDocuments.stringProperty());
        properties.put("strategy", WayangJsonSchemaDocuments.stringProperty());
        properties.put("handle", handleProperty());
        properties.put("steps", WayangJsonSchemaDocuments.stringArrayProperty());
        properties.put("metadata", WayangJsonSchemaDocuments.openObjectProperty());
        return properties;
    }

    static Map<String, Object> statusEnvelopeProperties(WayangContractDescriptor contract) {
        Map<String, Object> properties = contractOnlyProperties(contract);
        properties.putAll(statusProperties());
        return properties;
    }

    static Map<String, Object> eventsProperties(WayangContractDescriptor contract, boolean includeEvents) {
        Map<String, Object> properties = contractOnlyProperties(contract);
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

    static Map<String, Object> eventsFollowProperties(WayangContractDescriptor contract) {
        Map<String, Object> properties = contractOnlyProperties(contract);
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

    static Map<String, Object> inspectionProperties(WayangContractDescriptor contract) {
        Map<String, Object> properties = contractOnlyProperties(contract);
        properties.put("runId", WayangJsonSchemaDocuments.stringProperty());
        properties.put("outcome", WayangJsonSchemaDocuments.stringProperty());
        properties.put("known", WayangJsonSchemaDocuments.booleanProperty());
        properties.put("empty", WayangJsonSchemaDocuments.booleanProperty());
        properties.put("message", WayangJsonSchemaDocuments.stringProperty());
        properties.put("status", statusProperty());
        properties.put("events", eventsProperty(lifecycleContract(AgentRunLifecycleContract.RUN_EVENTS), true));
        return properties;
    }

    static Map<String, Object> historyProperties(WayangContractDescriptor contract, boolean includeRuns) {
        Map<String, Object> properties = contractOnlyProperties(contract);
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
            properties.put("runs", WayangJsonSchemaDocuments.arrayProperty(statusProperty()));
        }
        return properties;
    }

    static Map<String, Object> waitProperties(WayangContractDescriptor contract) {
        Map<String, Object> properties = contractOnlyProperties(contract);
        properties.put("runId", WayangJsonSchemaDocuments.stringProperty());
        properties.put("outcome", WayangJsonSchemaDocuments.stringProperty());
        properties.put("terminal", WayangJsonSchemaDocuments.booleanProperty());
        properties.put("timedOut", WayangJsonSchemaDocuments.booleanProperty());
        properties.put("attempts", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("elapsedMillis", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("status", statusProperty());
        properties.put("message", WayangJsonSchemaDocuments.stringProperty());
        properties.put("metadata", WayangJsonSchemaDocuments.openObjectProperty());
        return properties;
    }

    static Map<String, Object> cancelProperties(WayangContractDescriptor contract) {
        Map<String, Object> properties = contractOnlyProperties(contract);
        properties.put("runId", WayangJsonSchemaDocuments.stringProperty());
        properties.put("cancelled", WayangJsonSchemaDocuments.booleanProperty());
        properties.put("outcome", WayangJsonSchemaDocuments.stringProperty());
        properties.put("handle", handleProperty());
        properties.put("message", WayangJsonSchemaDocuments.stringProperty());
        properties.put("metadata", WayangJsonSchemaDocuments.openObjectProperty());
        return properties;
    }

    static Map<String, Object> forgetProperties(WayangContractDescriptor contract) {
        Map<String, Object> properties = contractOnlyProperties(contract);
        properties.put("runId", WayangJsonSchemaDocuments.stringProperty());
        properties.put("forgotten", WayangJsonSchemaDocuments.booleanProperty());
        properties.put("outcome", WayangJsonSchemaDocuments.stringProperty());
        properties.put("message", WayangJsonSchemaDocuments.stringProperty());
        properties.put("metadata", WayangJsonSchemaDocuments.openObjectProperty());
        return properties;
    }

    static Map<String, Object> runStoreProperties(WayangContractDescriptor contract) {
        Map<String, Object> properties = contractOnlyProperties(contract);
        properties.putAll(runStoreDiagnosticsProperties());
        return properties;
    }

    static Map<String, Object> runStoreVerificationProperties(WayangContractDescriptor contract) {
        Map<String, Object> properties = contractOnlyProperties(contract);
        properties.put("passed", WayangJsonSchemaDocuments.booleanProperty());
        properties.put("exitCode", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("issueCount", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("errorCount", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("warningCount", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("policy", runStoreVerificationPolicyProperty());
        properties.put("issues", WayangJsonSchemaDocuments.arrayProperty(runStoreVerificationIssueProperty()));
        properties.put("diagnostics", runStoreDiagnosticsProperty());
        return properties;
    }

    static Map<String, Object> runStoreCompactionPreviewProperties(WayangContractDescriptor contract) {
        Map<String, Object> properties = contractOnlyProperties(contract);
        properties.put("dryRun", WayangJsonSchemaDocuments.booleanProperty());
        properties.put("previewable", WayangJsonSchemaDocuments.booleanProperty());
        properties.put("compactionNeeded", WayangJsonSchemaDocuments.booleanProperty());
        properties.put("exitCode", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("issueCount", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("errorCount", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("warningCount", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("issues", WayangJsonSchemaDocuments.arrayProperty(runStoreVerificationIssueProperty()));
        properties.put("message", WayangJsonSchemaDocuments.stringProperty());
        properties.put("retentionAssessment", runStoreRetentionAssessmentProperty());
        properties.put("backupRetention", runStoreBackupRetentionResultProperty());
        properties.put("diagnostics", runStoreDiagnosticsProperty());
        return properties;
    }

    static Map<String, Object> runStoreCompactionResultProperties(WayangContractDescriptor contract) {
        Map<String, Object> properties = contractOnlyProperties(contract);
        properties.put("applied", WayangJsonSchemaDocuments.booleanProperty());
        properties.put("compacted", WayangJsonSchemaDocuments.booleanProperty());
        properties.put("backupCreated", WayangJsonSchemaDocuments.booleanProperty());
        properties.put("backupPath", WayangJsonSchemaDocuments.stringProperty());
        properties.put("backupRetention", runStoreBackupRetentionResultProperty());
        properties.put("successful", WayangJsonSchemaDocuments.booleanProperty());
        properties.put("compactionNeeded", WayangJsonSchemaDocuments.booleanProperty());
        properties.put("exitCode", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("issueCount", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("errorCount", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("warningCount", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("issues", WayangJsonSchemaDocuments.arrayProperty(runStoreVerificationIssueProperty()));
        properties.put("message", WayangJsonSchemaDocuments.stringProperty());
        properties.put("retentionAssessment", runStoreRetentionAssessmentProperty());
        properties.put("beforeDiagnostics", runStoreDiagnosticsProperty());
        properties.put("afterDiagnostics", runStoreDiagnosticsProperty());
        return properties;
    }

    private static Map<String, Object> runStoreDiagnosticsProperty() {
        return WayangJsonSchemaDocuments.objectProperty(
                runStoreDiagnosticsRequired(),
                true,
                runStoreDiagnosticsProperties());
    }

    private static Map<String, Object> runStoreBackupRetentionResultProperty() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("policy", runStoreBackupRetentionPolicyProperty());
        properties.put("retainedBackupCount", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("prunedBackupCount", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("failedBackupPruneCount", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("retainedBackupPaths", WayangJsonSchemaDocuments.stringArrayProperty());
        properties.put("prunedBackupPaths", WayangJsonSchemaDocuments.stringArrayProperty());
        properties.put("failedBackupPrunePaths", WayangJsonSchemaDocuments.stringArrayProperty());
        return WayangJsonSchemaDocuments.objectProperty(
                List.of(
                        "policy",
                        "retainedBackupCount",
                        "prunedBackupCount",
                        "failedBackupPruneCount",
                        "retainedBackupPaths",
                        "prunedBackupPaths",
                        "failedBackupPrunePaths"),
                true,
                properties);
    }

    private static Map<String, Object> runStoreBackupRetentionPolicyProperty() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("mode", WayangJsonSchemaDocuments.stringProperty());
        properties.put("maxBackups", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("bounded", WayangJsonSchemaDocuments.booleanProperty());
        properties.put("unlimited", WayangJsonSchemaDocuments.booleanProperty());
        return WayangJsonSchemaDocuments.objectProperty(
                List.of("mode", "maxBackups", "bounded", "unlimited"),
                true,
                properties);
    }

    private static List<String> runStoreDiagnosticsRequired() {
        return List.of(
                "backend",
                "persistent",
                "snapshotPresent",
                "lockPresent",
                "unsupportedSnapshotVersion",
                "runCount",
                "statusCount",
                "eventCount",
                "retentionPolicy",
                "retentionAssessment",
                "backupRetentionPolicy",
                "backupInventory");
    }

    private static Map<String, Object> runStoreDiagnosticsProperties() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("backend", WayangJsonSchemaDocuments.stringProperty());
        properties.put("persistent", WayangJsonSchemaDocuments.booleanProperty());
        properties.put("path", WayangJsonSchemaDocuments.stringProperty());
        properties.put("lockPath", WayangJsonSchemaDocuments.stringProperty());
        properties.put("snapshotPresent", WayangJsonSchemaDocuments.booleanProperty());
        properties.put("lockPresent", WayangJsonSchemaDocuments.booleanProperty());
        properties.put("snapshotVersion", WayangJsonSchemaDocuments.stringProperty());
        properties.put("unsupportedSnapshotVersion", WayangJsonSchemaDocuments.booleanProperty());
        properties.put("runCount", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("statusCount", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("eventCount", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("retentionPolicy", runStoreRetentionPolicyProperty());
        properties.put("retentionAssessment", runStoreRetentionAssessmentProperty());
        properties.put("backupRetentionPolicy", runStoreBackupRetentionPolicyProperty());
        properties.put("backupInventory", runStoreBackupInventoryProperty());
        return properties;
    }

    private static Map<String, Object> runStoreBackupInventoryProperty() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("backupCount", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("latestBackupPath", WayangJsonSchemaDocuments.stringProperty());
        properties.put("oldestBackupPath", WayangJsonSchemaDocuments.stringProperty());
        properties.put("backupPaths", WayangJsonSchemaDocuments.stringArrayProperty());
        return WayangJsonSchemaDocuments.objectProperty(
                List.of("backupCount", "backupPaths"),
                true,
                properties);
    }

    private static Map<String, Object> runStoreVerificationIssueProperty() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("severity", WayangJsonSchemaDocuments.stringProperty());
        properties.put("code", WayangJsonSchemaDocuments.stringProperty());
        properties.put("message", WayangJsonSchemaDocuments.stringProperty());
        return WayangJsonSchemaDocuments.objectProperty(
                List.of("severity", "code", "message"),
                true,
                properties);
    }

    private static Map<String, Object> runStoreVerificationPolicyProperty() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("mode", WayangJsonSchemaDocuments.stringProperty());
        properties.put("failOnWarnings", WayangJsonSchemaDocuments.booleanProperty());
        return WayangJsonSchemaDocuments.objectProperty(
                List.of("mode", "failOnWarnings"),
                true,
                properties);
    }

    private static Map<String, Object> handleProperty() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("runId", WayangJsonSchemaDocuments.stringProperty());
        properties.put("state", WayangJsonSchemaDocuments.stringProperty());
        properties.put("strategy", WayangJsonSchemaDocuments.stringProperty());
        properties.put("terminal", WayangJsonSchemaDocuments.booleanProperty());
        return WayangJsonSchemaDocuments.objectProperty(
                List.of("runId", "state", "strategy", "terminal"),
                true,
                properties);
    }

    private static Map<String, Object> runStoreRetentionPolicyProperty() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("mode", WayangJsonSchemaDocuments.stringProperty());
        properties.put("maxRuns", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("maxEventsPerRun", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("runsBounded", WayangJsonSchemaDocuments.booleanProperty());
        properties.put("eventsPerRunBounded", WayangJsonSchemaDocuments.booleanProperty());
        properties.put("bounded", WayangJsonSchemaDocuments.booleanProperty());
        properties.put("unlimited", WayangJsonSchemaDocuments.booleanProperty());
        return WayangJsonSchemaDocuments.objectProperty(
                List.of(
                        "mode",
                        "maxRuns",
                        "maxEventsPerRun",
                        "runsBounded",
                        "eventsPerRunBounded",
                        "bounded",
                        "unlimited"),
                true,
                properties);
    }

    private static Map<String, Object> runStoreRetentionAssessmentProperty() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("policy", runStoreRetentionPolicyProperty());
        properties.put("pruned", WayangJsonSchemaDocuments.booleanProperty());
        properties.put("totalRuns", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("retainedRuns", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("prunedRuns", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("totalStatuses", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("retainedStatuses", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("prunedStatuses", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("totalEvents", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("retainedEvents", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("prunedEvents", WayangJsonSchemaDocuments.nonNegativeIntegerProperty());
        properties.put("retainedRunIds", WayangJsonSchemaDocuments.stringArrayProperty());
        properties.put("prunedRunIds", WayangJsonSchemaDocuments.stringArrayProperty());
        properties.put("prunedEventsByRun", WayangJsonSchemaDocuments.countMapProperty());
        return WayangJsonSchemaDocuments.objectProperty(
                List.of(
                        "policy",
                        "pruned",
                        "totalRuns",
                        "retainedRuns",
                        "prunedRuns",
                        "totalStatuses",
                        "retainedStatuses",
                        "prunedStatuses",
                        "totalEvents",
                        "retainedEvents",
                        "prunedEvents",
                        "retainedRunIds",
                        "prunedRunIds",
                        "prunedEventsByRun"),
                true,
                properties);
    }

    private static Map<String, Object> statusProperty() {
        return WayangJsonSchemaDocuments.objectProperty(
                statusRequired(),
                true,
                statusProperties());
    }

    private static Map<String, Object> statusProperties() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("handle", handleProperty());
        properties.put("outcome", WayangJsonSchemaDocuments.stringProperty());
        properties.put("known", WayangJsonSchemaDocuments.booleanProperty());
        properties.put("message", WayangJsonSchemaDocuments.stringProperty());
        properties.put("metadata", WayangJsonSchemaDocuments.openObjectProperty());
        return properties;
    }

    private static List<String> statusRequired() {
        return List.of("handle", "outcome", "known", "message", "metadata");
    }

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

    private static Map<String, Object> eventsProperty(WayangContractDescriptor contract, boolean includeEvents) {
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

    private static List<String> append(List<String> values, String value) {
        java.util.ArrayList<String> copy = new java.util.ArrayList<>(values);
        copy.add(value);
        return List.copyOf(copy);
    }

    private static List<String> withContract(List<String> values) {
        java.util.ArrayList<String> copy = new java.util.ArrayList<>();
        copy.add("contract");
        copy.addAll(values);
        return List.copyOf(copy);
    }

    private static WayangContractDescriptor lifecycleContract(String envelope) {
        return WayangContractDescriptors.lifecycle(envelope);
    }
}
