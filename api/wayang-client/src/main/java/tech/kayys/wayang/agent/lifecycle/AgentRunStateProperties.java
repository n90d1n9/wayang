package tech.kayys.wayang.agent.lifecycle;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import tech.kayys.wayang.client.WayangJsonSchemaDocuments;
import tech.kayys.wayang.contract.WayangContractDescriptor;

/**
 * Schema property definitions for state-specific agent run lifecycle envelopes.
 * Handles result, status, inspection, wait, cancel, forget, and run-store related schemas.
 */
final class AgentRunStateProperties {

    private AgentRunStateProperties() {
    }

    // Required properties lists

    public static List<String> resultRequired() {
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

    public static List<String> statusEnvelopeRequired() {
        return AgentRunEventProperties.withContract(statusRequired());
    }

    public static List<String> inspectionRequired() {
        return List.of("contract", "runId", "outcome", "known", "empty", "message", "status", "events");
    }

    public static List<String> waitRequired() {
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

    public static List<String> cancelRequired() {
        return List.of("contract", "runId", "cancelled", "outcome", "handle", "message", "metadata");
    }

    public static List<String> forgetRequired() {
        return List.of("contract", "runId", "forgotten", "outcome", "message", "metadata");
    }

    public static List<String> runStoreRequired() {
        return AgentRunEventProperties.withContract(runStoreDiagnosticsRequired());
    }

    public static List<String> runStoreVerificationRequired() {
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

    public static List<String> runStoreCompactionPreviewRequired() {
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

    public static List<String> runStoreCompactionResultRequired() {
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

    // Property maps

    public static Map<String, Object> resultProperties(WayangContractDescriptor contract) {
        Map<String, Object> properties = AgentRunSchemaHandler.contractOnlyProperties(contract);
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

    public static Map<String, Object> statusEnvelopeProperties(WayangContractDescriptor contract) {
        Map<String, Object> properties = AgentRunSchemaHandler.contractOnlyProperties(contract);
        properties.putAll(statusProperties());
        return properties;
    }

    public static Map<String, Object> inspectionProperties(WayangContractDescriptor contract) {
        Map<String, Object> properties = AgentRunSchemaHandler.contractOnlyProperties(contract);
        properties.put("runId", WayangJsonSchemaDocuments.stringProperty());
        properties.put("outcome", WayangJsonSchemaDocuments.stringProperty());
        properties.put("known", WayangJsonSchemaDocuments.booleanProperty());
        properties.put("empty", WayangJsonSchemaDocuments.booleanProperty());
        properties.put("message", WayangJsonSchemaDocuments.stringProperty());
        properties.put("status", statusProperty());
        properties.put("events", AgentRunEventProperties.eventsProperty(lifecycleContract(AgentRunLifecycleContract.RUN_EVENTS), true));
        return properties;
    }

    public static Map<String, Object> waitProperties(WayangContractDescriptor contract) {
        Map<String, Object> properties = AgentRunSchemaHandler.contractOnlyProperties(contract);
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

    public static Map<String, Object> cancelProperties(WayangContractDescriptor contract) {
        Map<String, Object> properties = AgentRunSchemaHandler.contractOnlyProperties(contract);
        properties.put("runId", WayangJsonSchemaDocuments.stringProperty());
        properties.put("cancelled", WayangJsonSchemaDocuments.booleanProperty());
        properties.put("outcome", WayangJsonSchemaDocuments.stringProperty());
        properties.put("handle", handleProperty());
        properties.put("message", WayangJsonSchemaDocuments.stringProperty());
        properties.put("metadata", WayangJsonSchemaDocuments.openObjectProperty());
        return properties;
    }

    public static Map<String, Object> forgetProperties(WayangContractDescriptor contract) {
        Map<String, Object> properties = AgentRunSchemaHandler.contractOnlyProperties(contract);
        properties.put("runId", WayangJsonSchemaDocuments.stringProperty());
        properties.put("forgotten", WayangJsonSchemaDocuments.booleanProperty());
        properties.put("outcome", WayangJsonSchemaDocuments.stringProperty());
        properties.put("message", WayangJsonSchemaDocuments.stringProperty());
        properties.put("metadata", WayangJsonSchemaDocuments.openObjectProperty());
        return properties;
    }

    public static Map<String, Object> runStoreProperties(WayangContractDescriptor contract) {
        Map<String, Object> properties = AgentRunSchemaHandler.contractOnlyProperties(contract);
        properties.putAll(runStoreDiagnosticsProperties());
        return properties;
    }

    public static Map<String, Object> runStoreVerificationProperties(WayangContractDescriptor contract) {
        Map<String, Object> properties = AgentRunSchemaHandler.contractOnlyProperties(contract);
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

    public static Map<String, Object> runStoreCompactionPreviewProperties(WayangContractDescriptor contract) {
        Map<String, Object> properties = AgentRunSchemaHandler.contractOnlyProperties(contract);
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

    public static Map<String, Object> runStoreCompactionResultProperties(WayangContractDescriptor contract) {
        Map<String, Object> properties = AgentRunSchemaHandler.contractOnlyProperties(contract);
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

    // Private state helpers

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

    // RunStore property helpers

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

    private static WayangContractDescriptor lifecycleContract(String envelope) {
        return WayangContractDescriptors.lifecycle(envelope);
    }
}
