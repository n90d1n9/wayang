package tech.kayys.wayang.gollek.sdk;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AgentRunLifecycleContractTest {

    @Test
    void normalizesLifecycleContractDescriptors() {
        AgentRunLifecycleContract contract = AgentRunLifecycleContract.of(" run-events ");
        AgentRunLifecycleContract fallback = new AgentRunLifecycleContract("", 0, null);

        assertThat(contract.schema()).isEqualTo("wayang.run.lifecycle");
        assertThat(contract.version()).isEqualTo(1);
        assertThat(contract.envelope()).isEqualTo(AgentRunLifecycleContract.RUN_EVENTS);
        assertThat(fallback.schema()).isEqualTo("wayang.run.lifecycle");
        assertThat(fallback.version()).isEqualTo(1);
        assertThat(fallback.envelope()).isEmpty();
        assertThat(AgentRunLifecycleContract.runResult().envelope())
                .isEqualTo(AgentRunLifecycleContract.RUN_RESULT);
        assertThat(AgentRunLifecycleContract.runEventsFollow().envelope())
                .isEqualTo(AgentRunLifecycleContract.RUN_EVENTS_FOLLOW);
        assertThat(AgentRunLifecycleContract.runForget().envelope())
                .isEqualTo(AgentRunLifecycleContract.RUN_FORGET);
        assertThat(AgentRunLifecycleContract.runStore().envelope())
                .isEqualTo(AgentRunLifecycleContract.RUN_STORE);
        assertThat(AgentRunLifecycleContract.runStoreVerification().envelope())
                .isEqualTo(AgentRunLifecycleContract.RUN_STORE_VERIFICATION);
        assertThat(AgentRunLifecycleContract.runStoreCompactionPreview().envelope())
                .isEqualTo(AgentRunLifecycleContract.RUN_STORE_COMPACTION_PREVIEW);
        assertThat(AgentRunLifecycleContract.runStoreCompaction().envelope())
                .isEqualTo(AgentRunLifecycleContract.RUN_STORE_COMPACTION);
    }

    @Test
    void exposesConcreteStatusJsonSchema() {
        WayangContractDescriptor descriptor = WayangContractCatalog.discover(
                        WayangContractQuery.of(AgentRunLifecycleContract.SCHEMA, "run-status"))
                .contracts()
                .get(0);

        WayangContractJsonSchema schema = WayangGollekSdk.local().contractJsonSchema(descriptor);

        assertThat(schema.id()).isEqualTo("urn:wayang:contract:wayang.run.lifecycle:v1:run-status");
        assertThat(schema.document())
                .containsEntry("x-wayang-schema", AgentRunLifecycleContract.SCHEMA)
                .containsEntry("x-wayang-envelope", AgentRunLifecycleContract.RUN_STATUS)
                .containsEntry("x-wayang-domain", "lifecycle");
        List<String> required = required(schema);
        Map<String, Object> properties = properties(schema);
        assertThat(required)
                .containsExactly("contract", "handle", "outcome", "known", "message", "metadata");
        assertThat(properties)
                .containsKeys("contract", "handle", "outcome", "known", "message", "metadata");
        assertThat(required((Map<String, Object>) properties.get("handle")))
                .containsExactly("runId", "state", "strategy", "terminal");
    }

    @Test
    void exposesConcreteEventFollowJsonSchema() {
        WayangContractDescriptor descriptor = WayangContractCatalog.discover(
                        WayangContractQuery.of(AgentRunLifecycleContract.SCHEMA, "run-events-follow"))
                .contracts()
                .get(0);

        WayangContractJsonSchema schema = WayangGollekSdk.local().contractJsonSchema(descriptor);

        assertThat(schema.id()).isEqualTo("urn:wayang:contract:wayang.run.lifecycle:v1:run-events-follow");
        List<String> required = required(schema);
        Map<String, Object> properties = properties(schema);
        assertThat(required)
                .contains(
                        "contract",
                        "runId",
                        "successful",
                        "terminalState",
                        "terminalEventType",
                        "initialQuery",
                        "nextQuery",
                        "lastEvents");
        assertThat(properties)
                .containsKeys(
                        "contract",
                        "terminalSequence",
                        "maxPollsReached",
                        "elapsedMillis",
                        "initialQuery",
                        "nextQuery",
                        "lastEvents");
        assertThat(required((Map<String, Object>) properties.get("initialQuery")))
                .containsExactly("state", "type", "afterSequence", "limit", "filtered");
        assertThat((List<?>) ((Map<String, Object>) properties.get("lastEvents")).get("oneOf"))
                .hasSize(2);
    }

    @Test
    void exposesConcreteHistoryJsonSchema() {
        WayangContractDescriptor descriptor = WayangContractCatalog.discover(
                        WayangContractQuery.of(AgentRunLifecycleContract.SCHEMA, "run-list"))
                .contracts()
                .get(0);

        WayangContractJsonSchema schema = WayangGollekSdk.local().contractJsonSchema(descriptor);

        Map<String, Object> properties = properties(schema);
        assertThat(required(schema))
                .contains(
                        "contract",
                        "query",
                        "page",
                        "summary",
                        "stateCounts",
                        "surfaceCounts",
                        "strategySummaries",
                        "runs");
        assertThat(properties)
                .containsKeys("query", "page", "summary", "totalRuns", "returnedRuns", "runs");
        assertThat(required((Map<String, Object>) properties.get("page")))
                .containsExactly(
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
                        "empty");
    }

    @Test
    void exposesConcreteRunStoreJsonSchema() {
        WayangContractDescriptor descriptor = WayangContractCatalog.discover(
                        WayangContractQuery.of(AgentRunLifecycleContract.SCHEMA, "run-store"))
                .contracts()
                .get(0);

        WayangContractJsonSchema schema = WayangGollekSdk.local().contractJsonSchema(descriptor);

        Map<String, Object> properties = properties(schema);
        assertThat(schema.id()).isEqualTo("urn:wayang:contract:wayang.run.lifecycle:v1:run-store");
        assertThat(required(schema))
                .contains(
                        "contract",
                        "backend",
                        "persistent",
                        "retentionPolicy",
                        "retentionAssessment");
        assertThat(properties)
                .containsKeys(
                        "contract",
                        "backend",
                        "path",
                        "lockPath",
                        "runCount",
                        "retentionPolicy",
                        "retentionAssessment");
        assertThat(required((Map<String, Object>) properties.get("retentionPolicy")))
                .containsExactly(
                        "mode",
                        "maxRuns",
                        "maxEventsPerRun",
                        "runsBounded",
                        "eventsPerRunBounded",
                        "bounded",
                        "unlimited");
        assertThat(required((Map<String, Object>) properties.get("retentionAssessment")))
                .contains("policy", "pruned", "totalRuns", "retainedRunIds", "prunedEventsByRun");
    }

    @Test
    void exposesConcreteRunStoreVerificationJsonSchema() {
        WayangContractDescriptor descriptor = WayangContractCatalog.discover(
                        WayangContractQuery.of(AgentRunLifecycleContract.SCHEMA, "run-store-verification"))
                .contracts()
                .get(0);

        WayangContractJsonSchema schema = WayangGollekSdk.local().contractJsonSchema(descriptor);

        Map<String, Object> properties = properties(schema);
        assertThat(schema.id()).isEqualTo("urn:wayang:contract:wayang.run.lifecycle:v1:run-store-verification");
        assertThat(required(schema))
                .containsExactly(
                        "contract",
                        "passed",
                        "exitCode",
                        "issueCount",
                        "errorCount",
                        "warningCount",
                        "policy",
                        "issues",
                        "diagnostics");
        assertThat(properties).containsKeys("contract", "policy", "issues", "diagnostics");
        assertThat(required((Map<String, Object>) properties.get("policy")))
                .containsExactly("mode", "failOnWarnings");
        assertThat(required((Map<String, Object>) properties.get("diagnostics")))
                .contains("backend", "persistent", "retentionPolicy", "retentionAssessment");
    }

    @Test
    void exposesConcreteRunStoreCompactionPreviewJsonSchema() {
        WayangContractDescriptor descriptor = WayangContractCatalog.discover(
                        WayangContractQuery.of(AgentRunLifecycleContract.SCHEMA, "run-store-compaction-preview"))
                .contracts()
                .get(0);

        WayangContractJsonSchema schema = WayangGollekSdk.local().contractJsonSchema(descriptor);

        Map<String, Object> properties = properties(schema);
        assertThat(schema.id()).isEqualTo(
                "urn:wayang:contract:wayang.run.lifecycle:v1:run-store-compaction-preview");
        assertThat(required(schema))
                .containsExactly(
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
        assertThat(properties)
                .containsKeys(
                        "contract",
                        "dryRun",
                        "retentionAssessment",
                        "backupRetention",
                        "issues",
                        "diagnostics");
        assertThat(required((Map<String, Object>) properties.get("retentionAssessment")))
                .contains("policy", "pruned", "prunedEvents", "prunedRunIds");
        assertThat(required((Map<String, Object>) properties.get("backupRetention")))
                .contains(
                        "policy",
                        "retainedBackupCount",
                        "prunedBackupCount",
                        "failedBackupPruneCount",
                        "failedBackupPrunePaths");
    }

    @Test
    void exposesConcreteRunStoreCompactionResultJsonSchema() {
        WayangContractDescriptor descriptor = WayangContractCatalog.discover(
                        WayangContractQuery.of(AgentRunLifecycleContract.SCHEMA, "run-store-compaction"))
                .contracts()
                .get(0);

        WayangContractJsonSchema schema = WayangGollekSdk.local().contractJsonSchema(descriptor);

        Map<String, Object> properties = properties(schema);
        assertThat(schema.id()).isEqualTo("urn:wayang:contract:wayang.run.lifecycle:v1:run-store-compaction");
        assertThat(required(schema))
                .containsExactly(
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
        assertThat(properties)
                .containsKeys(
                        "contract",
                        "applied",
                        "compacted",
                        "backupCreated",
                        "backupPath",
                        "backupRetention",
                        "beforeDiagnostics",
                        "afterDiagnostics");
        assertThat(required((Map<String, Object>) properties.get("backupRetention")))
                .contains(
                        "policy",
                        "retainedBackupCount",
                        "prunedBackupCount",
                        "failedBackupPruneCount",
                        "failedBackupPrunePaths");
        assertThat(required((Map<String, Object>) properties.get("afterDiagnostics")))
                .contains("backend", "persistent", "retentionPolicy", "retentionAssessment");
    }

    @SuppressWarnings("unchecked")
    private static List<String> required(WayangContractJsonSchema schema) {
        return (List<String>) schema.document().get("required");
    }

    @SuppressWarnings("unchecked")
    private static List<String> required(Map<String, Object> schemaProperty) {
        return (List<String>) schemaProperty.get("required");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> properties(WayangContractJsonSchema schema) {
        return (Map<String, Object>) schema.document().get("properties");
    }
}
