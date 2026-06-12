package tech.kayys.wayang.agent.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.hermes.HermesMetadataKeys;
import tech.kayys.wayang.agent.hermes.HermesRuntimeEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HermesApiJsonContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void diagnosticsResponseSerializesStableJsonShape() throws Exception {
        JsonNode json = json(new HermesDiagnosticsResponse(
                "runtime-diagnostics",
                "inspect",
                "runtime-diagnostics:capabilities",
                true,
                true,
                true,
                "inspected",
                "runtime diagnostics inspected",
                Map.of(
                        "view", "capabilities",
                        "learningAuditConfigured", true,
                        "learningAuditReady", true,
                        HermesMetadataKeys.METADATA_LEARNING_AUDIT_RETENTION_OBSERVATION,
                        Map.of("outcome", "suppressed"),
                        "diagnostics", Map.of("supportsSkillLearning", true)),
                "capabilities",
                true,
                true,
                false,
                true,
                true,
                List.of("needs journal"),
                Map.of("supportsSkillLearning", true),
                Map.of("outcome", "suppressed")));

        assertThat(fields(json))
                .containsExactlyInAnyOrder(
                        "port",
                        "operation",
                        "target",
                        "active",
                        "dispatched",
                        "successful",
                        "status",
                        "reason",
                        "metadata",
                        "view",
                        "ready",
                        "runtimePortsReady",
                        "skillPersistenceReady",
                        "learningAuditConfigured",
                        "learningAuditReady",
                        "attention",
                        "attentionItems",
                        "attentionSummary",
                        "diagnostics",
                        "learningAuditRetentionObservation");
        assertThat(json.path("port").asText()).isEqualTo("runtime-diagnostics");
        assertThat(json.path("view").asText()).isEqualTo("capabilities");
        assertThat(json.path("ready").asBoolean()).isTrue();
        assertThat(json.path("diagnostics").path("supportsSkillLearning").asBoolean()).isTrue();
        assertThat(json.path("learningAuditRetentionObservation").path("outcome").asText())
                .isEqualTo("suppressed");
        assertThat(json.path("metadata").path("view").asText()).isEqualTo("capabilities");
        assertThat(json.path("attention").get(0).asText()).isEqualTo("needs journal");
        assertThat(json.path("attentionItems").get(0).path("message").asText()).isEqualTo("needs journal");
        assertThat(json.path("attentionItems").get(0).path("source").asText()).isEqualTo("runtime-diagnostics");
        assertThat(json.path("attentionSummary").path("totalItems").asInt()).isEqualTo(1);
        assertThat(json.path("attentionSummary").path("sourceCounts").path("runtime-diagnostics").asLong())
                .isEqualTo(1L);
    }

    @Test
    void journalResponseSerializesStableJsonShape() throws Exception {
        JsonNode json = json(new HermesJournalResponse(
                "runtime-journal",
                "inspect",
                "session:session-a",
                true,
                true,
                true,
                "inspected",
                "runtime journal inspected",
                Map.of(
                        "matchedEvents", 1,
                        "totalMatchedEvents", 3,
                        "sessionSnapshot", Map.of("latestRequestId", "req-1")),
                1,
                3,
                1,
                false,
                "evt-prev",
                "evt-next",
                "evt-first",
                "evt-last",
                true,
                true,
                true,
                "completed",
                false,
                false,
                new HermesJournalSummaryResponse(
                        1,
                        1,
                        false,
                        0L,
                        1L,
                        1L,
                        "2026-06-03T00:00:00Z",
                        "2026-06-03T00:00:00Z",
                        "evt-1",
                        "req-1",
                        Map.of("response.completed", 1L),
                        Map.of("successful", 1L),
                        Map.of("tenant-a", 1L)),
                List.of(new HermesJournalEventResponse(
                        "evt-1",
                        "response.completed",
                        "req-1",
                        "tenant-a",
                        "session-a",
                        "user-a",
                        "successful",
                        "2026-06-03T00:00:00Z",
                        Map.of("durationMs", 42))),
                List.of(),
                null,
                null,
                null,
                null,
                null,
                Map.of("sessionId", "session-a", "limit", 5),
                Map.of("matchedEvents", 1),
                Map.of("latestRequestId", "req-1", "status", "completed")));

        assertThat(fields(json))
                .containsExactlyInAnyOrder(
                        "port",
                        "operation",
                        "target",
                        "active",
                        "dispatched",
                        "successful",
                        "status",
                        "reason",
                        "metadata",
                        "matchedEvents",
                        "totalMatchedEvents",
                        "returnedEvents",
                        "truncated",
                        "previousCursor",
                        "nextCursor",
                        "firstCursor",
                        "lastCursor",
                        "hasPreviousPage",
                        "hasNextPage",
                        "cursorResolved",
                        "journalStatus",
                        "resumable",
                        "requiresAttention",
                        "summary",
                        "events",
                        "learningAuditRetentionEvents",
                        "learningAuditRetentionSummary",
                        "operationalAttentionItems",
                        "operationalAttentionSummary",
                        "operationalActionItems",
                        "operationalActionSummary",
                        "query",
                        "journalView",
                        "sessionSnapshot");
        assertThat(json.path("port").asText()).isEqualTo("runtime-journal");
        assertThat(json.path("status").asText()).isEqualTo("inspected");
        assertThat(json.path("journalStatus").asText()).isEqualTo("completed");
        assertThat(json.path("matchedEvents").asInt()).isEqualTo(1);
        assertThat(json.path("totalMatchedEvents").asInt()).isEqualTo(3);
        assertThat(json.path("nextCursor").asText()).isEqualTo("evt-next");
        assertThat(json.path("hasNextPage").asBoolean()).isTrue();
        assertThat(json.path("cursorResolved").asBoolean()).isTrue();
        assertThat(json.path("summary").path("latestEventId").asText()).isEqualTo("evt-1");
        assertThat(json.path("summary").path("typeCounts").path("response.completed").asLong()).isEqualTo(1L);
        assertThat(json.path("events").get(0).path("eventId").asText()).isEqualTo("evt-1");
        assertThat(json.path("events").get(0).path("metadata").path("durationMs").asInt()).isEqualTo(42);
        assertThat(json.path("learningAuditRetentionEvents").isArray()).isTrue();
        assertThat(json.path("learningAuditRetentionEvents").size()).isZero();
        assertThat(json.path("learningAuditRetentionSummary").path("totalEvents").asInt()).isZero();
        assertThat(json.path("operationalAttentionItems").isArray()).isTrue();
        assertThat(json.path("operationalAttentionItems").size()).isZero();
        assertThat(json.path("operationalAttentionSummary").path("totalItems").asInt()).isZero();
        assertThat(json.path("operationalActionItems").isArray()).isTrue();
        assertThat(json.path("operationalActionItems").size()).isZero();
        assertThat(json.path("operationalActionSummary").path("totalActions").asInt()).isZero();
        assertThat(json.path("query").path("sessionId").asText()).isEqualTo("session-a");
        assertThat(json.path("sessionSnapshot").path("latestRequestId").asText()).isEqualTo("req-1");
    }

    @Test
    void operationalAttentionSummaryResponseSerializesStableJsonShape() throws Exception {
        JsonNode json = json(HermesOperationalAttentionSummaryResponse.from(List.of(
                new HermesOperationalAttention(
                        "learning-audit-retention",
                        "warning",
                        2,
                        "capacity warning",
                        "monitor-learning-audit-retention",
                        true,
                        Map.of()))));

        assertThat(fields(json))
                .containsExactlyInAnyOrder(
                        "totalItems",
                        "retryableItems",
                        "highestPriority",
                        "requiresAttention",
                        "actions",
                        "sourceCounts",
                        "severityCounts");
        assertThat(json.path("totalItems").asInt()).isEqualTo(1);
        assertThat(json.path("retryableItems").asInt()).isEqualTo(1);
        assertThat(json.path("highestPriority").asInt()).isEqualTo(2);
        assertThat(json.path("requiresAttention").asBoolean()).isTrue();
        assertThat(json.path("actions").get(0).asText()).isEqualTo("monitor-learning-audit-retention");
        assertThat(json.path("sourceCounts").path("learning-audit-retention").asLong()).isEqualTo(1L);
        assertThat(json.path("severityCounts").path("warning").asLong()).isEqualTo(1L);
    }

    @Test
    void operationalActionSummaryResponseSerializesStableJsonShape() throws Exception {
        JsonNode json = json(HermesOperationalActionSummaryResponse.from(HermesOperationalAction.retentionActions(
                "warning",
                2,
                List.of(
                        "monitor-learning-audit-retention",
                        "archive-learning-audit-receipts"))));

        assertThat(fields(json))
                .containsExactlyInAnyOrder(
                        "totalActions",
                        "safeActions",
                        "unsafeActions",
                        "dryRunSupportedActions",
                        "requiresOperatorApproval",
                        "requiresConfiguration",
                        "requiredConfig",
                        "riskLevelCounts");
        assertThat(json.path("totalActions").asInt()).isEqualTo(2);
        assertThat(json.path("safeActions").asInt()).isEqualTo(1);
        assertThat(json.path("unsafeActions").asInt()).isEqualTo(1);
        assertThat(json.path("dryRunSupportedActions").asInt()).isEqualTo(1);
        assertThat(json.path("requiresOperatorApproval").asBoolean()).isTrue();
        assertThat(json.path("requiresConfiguration").asBoolean()).isTrue();
        assertThat(json.path("requiredConfig").get(0).asText())
                .isEqualTo("learning-audit-archive-target");
        assertThat(json.path("riskLevelCounts").path("medium").asLong()).isEqualTo(1L);
    }

    @Test
    void learningAuditRetentionEventSummaryResponseSerializesStableJsonShape() throws Exception {
        Map<String, Object> retentionStatus = Map.ofEntries(
                Map.entry("status", "near-capacity"),
                Map.entry("severity", "warning"),
                Map.entry("priority", 2),
                Map.entry("remainingEntries", 1),
                Map.entry("overflowEntries", 0),
                Map.entry("utilizationPercent", 80),
                Map.entry("nearCapacity", true),
                Map.entry("atCapacity", false),
                Map.entry("requiresAttention", true),
                Map.entry("attention", List.of("capacity warning")),
                Map.entry("recommendedActions", List.of("monitor-learning-audit-retention")));
        JsonNode json = json(HermesLearningAuditRetentionEventSummaryResponse.from(List.of(
                HermesLearningAuditRetentionEventResponse.from(new HermesJournalEventResponse(
                        "evt-retention",
                        HermesRuntimeEvent.TYPE_LEARNING_AUDIT_RETENTION_ATTENTION,
                        "req-1",
                        "tenant-a",
                        "session-a",
                        "user-a",
                        "near-capacity",
                        "2026-06-03T00:00:00Z",
                        Map.of("retentionStatus", retentionStatus))))));

        assertThat(fields(json))
                .containsExactlyInAnyOrder(
                        "totalEvents",
                        "attentionEvents",
                        "criticalEvents",
                        "warningEvents",
                        "highestPriority",
                        "nearCapacityEvents",
                        "atCapacityEvents",
                        "maxUtilizationPercent",
                        "maxOverflowEntries",
                        "minRemainingEntries",
                        "latestUtilizationPercent",
                        "requiresAttention",
                        "latestEventId",
                        "latestOccurredAt",
                        "latestRetentionStatus",
                        "latestRetentionSeverity",
                        "retentionStatusCounts",
                        "retentionSeverityCounts",
                        "retentionRecommendedActionCounts",
                        "retentionAttention",
                        "retentionAttentionItems",
                        "retentionAttentionSummary",
                        "retentionRecommendedActions",
                        "retentionRecommendedActionItems",
                        "retentionRecommendedActionSummary");
        assertThat(json.path("totalEvents").asInt()).isEqualTo(1);
        assertThat(json.path("requiresAttention").asBoolean()).isTrue();
        assertThat(json.path("nearCapacityEvents").asInt()).isEqualTo(1);
        assertThat(json.path("atCapacityEvents").asInt()).isZero();
        assertThat(json.path("maxUtilizationPercent").asInt()).isEqualTo(80);
        assertThat(json.path("minRemainingEntries").asInt()).isEqualTo(1);
        assertThat(json.path("latestUtilizationPercent").asInt()).isEqualTo(80);
        assertThat(json.path("latestEventId").asText()).isEqualTo("evt-retention");
        assertThat(json.path("retentionStatusCounts").path("near-capacity").asLong()).isEqualTo(1L);
        assertThat(json.path("retentionAttentionSummary").path("totalItems").asInt()).isEqualTo(1);
        assertThat(json.path("retentionAttentionSummary").path("severityCounts").path("warning").asLong())
                .isEqualTo(1L);
        assertThat(json.path("retentionRecommendedActionItems").get(0).path("actionId").asText())
                .isEqualTo("monitor-learning-audit-retention");
        assertThat(json.path("retentionRecommendedActionSummary").path("totalActions").asInt()).isEqualTo(1);
        assertThat(json.path("retentionRecommendedActionSummary").path("safeActions").asInt()).isEqualTo(1);
        assertThat(json.path("retentionRecommendedActionSummary").path("requiresOperatorApproval").asBoolean())
                .isFalse();
    }

    @Test
    void learningAuditRetentionEventResponseSerializesStableJsonShape() throws Exception {
        JsonNode json = json(HermesLearningAuditRetentionEventResponse.from(new HermesJournalEventResponse(
                "evt-retention",
                HermesRuntimeEvent.TYPE_LEARNING_AUDIT_RETENTION_ATTENTION,
                "req-1",
                "tenant-a",
                "session-a",
                "user-a",
                "near-capacity",
                "2026-06-03T00:00:00Z",
                Map.of(
                        "source", "learning-audit-retention",
                        "retentionStatus", Map.ofEntries(
                                Map.entry("ledgerType", "file-system"),
                                Map.entry("bounded", true),
                                Map.entry("recordCount", 4),
                                Map.entry("maxEntries", 5),
                                Map.entry("remainingEntries", 1),
                                Map.entry("overflowEntries", 0),
                                Map.entry("utilizationPercent", 80),
                                Map.entry("nearCapacity", true),
                                Map.entry("atCapacity", false),
                                Map.entry("status", "near-capacity"),
                                Map.entry("severity", "warning"),
                                Map.entry("priority", 2),
                                Map.entry("requiresAttention", true),
                                Map.entry("attention", List.of("capacity warning")),
                                Map.entry("recommendedActions", List.of("monitor-learning-audit-retention")),
                                Map.entry("retentionPolicy", Map.of("retentionMode", "max-entries")))))));

        assertThat(fields(json))
                .containsExactlyInAnyOrder(
                        "eventId",
                        "type",
                        "requestId",
                        "tenantId",
                        "sessionId",
                        "userId",
                        "outcome",
                        "occurredAt",
                        "source",
                        "metadata",
                        "learningAuditRetentionStatus",
                        "ledgerType",
                        "bounded",
                        "recordCount",
                        "maxEntries",
                        "remainingEntries",
                        "overflowEntries",
                        "utilizationPercent",
                        "nearCapacity",
                        "atCapacity",
                        "retentionPolicy",
                        "retentionStatus",
                        "retentionSeverity",
                        "retentionPriority",
                        "retentionRequiresAttention",
                        "retentionAttention",
                        "retentionAttentionItems",
                        "retentionRecommendedActions",
                        "retentionRecommendedActionItems");
        assertThat(json.path("eventId").asText()).isEqualTo("evt-retention");
        assertThat(json.path("type").asText()).isEqualTo(HermesRuntimeEvent.TYPE_LEARNING_AUDIT_RETENTION_ATTENTION);
        assertThat(json.path("ledgerType").asText()).isEqualTo("file-system");
        assertThat(json.path("retentionStatus").asText()).isEqualTo("near-capacity");
        assertThat(json.path("retentionAttentionItems").get(0).path("message").asText())
                .isEqualTo("capacity warning");
        assertThat(json.path("retentionRecommendedActionItems").get(0).path("actionId").asText())
                .isEqualTo("monitor-learning-audit-retention");
    }

    @Test
    void learningAuditResponseSerializesStableJsonShape() throws Exception {
        JsonNode json = json(new HermesLearningAuditResponse(
                "learning-audit",
                "inspect",
                "skill:skill-a",
                true,
                true,
                true,
                "inspected",
                "learning audit inspected",
                Map.of(
                        "matchedReceipts", 1,
                        "totalMatchedReceipts", 1,
                        "firstCursor", "key-skill-a",
                        "lastCursor", "key-skill-a",
                        "cursorResolved", true,
                        "learningAuditSummary", Map.of("latestSkillId", "skill-a")),
                1,
                1,
                1,
                false,
                "",
                "",
                "key-skill-a",
                "key-skill-a",
                false,
                false,
                true,
                1L,
                0L,
                0L,
                "skill-a",
                "persisted",
                Map.of("skillId", "skill-a", "limit", 5),
                Map.of("matchedReceipts", 1),
                Map.of("latestSkillId", "skill-a", "persistedReceipts", 1L),
                Map.of("ledgerType", "file-system", "status", "healthy")));

        assertThat(fields(json))
                .containsExactlyInAnyOrder(
                        "port",
                        "operation",
                        "target",
                        "active",
                        "dispatched",
                        "successful",
                        "status",
                        "reason",
                        "metadata",
                        "matchedReceipts",
                        "totalMatchedReceipts",
                        "returnedReceipts",
                        "truncated",
                        "previousCursor",
                        "nextCursor",
                        "firstCursor",
                        "lastCursor",
                        "hasPreviousPage",
                        "hasNextPage",
                        "cursorResolved",
                        "persistedReceipts",
                        "skippedReceipts",
                        "rejectedReceipts",
                        "latestSkillId",
                        "latestOutcome",
                        "query",
                        "learningAuditView",
                        "learningAuditSummary",
                        "learningAuditRetentionStatus");
        assertThat(json.path("port").asText()).isEqualTo("learning-audit");
        assertThat(json.path("status").asText()).isEqualTo("inspected");
        assertThat(json.path("matchedReceipts").asInt()).isEqualTo(1);
        assertThat(json.path("totalMatchedReceipts").asInt()).isEqualTo(1);
        assertThat(json.path("firstCursor").asText()).isEqualTo("key-skill-a");
        assertThat(json.path("cursorResolved").asBoolean()).isTrue();
        assertThat(json.path("persistedReceipts").asLong()).isEqualTo(1L);
        assertThat(json.path("query").path("skillId").asText()).isEqualTo("skill-a");
        assertThat(json.path("learningAuditSummary").path("latestSkillId").asText()).isEqualTo("skill-a");
        assertThat(json.path("learningAuditRetentionStatus").path("status").asText()).isEqualTo("healthy");
    }

    @Test
    void learningAuditRetentionResponseSerializesStableJsonShape() throws Exception {
        Map<String, Object> retentionStatus = Map.ofEntries(
                Map.entry("ledgerType", "file-system"),
                Map.entry("bounded", true),
                Map.entry("recordCount", 4),
                Map.entry("maxEntries", 5),
                Map.entry("remainingEntries", 1),
                Map.entry("overflowEntries", 0),
                Map.entry("utilizationPercent", 80),
                Map.entry("nearCapacity", true),
                Map.entry("atCapacity", false),
                Map.entry("status", "near-capacity"),
                Map.entry("severity", "warning"),
                Map.entry("priority", 2),
                Map.entry("requiresAttention", true),
                Map.entry("attention", List.of("capacity warning")),
                Map.entry("recommendedActions", List.of("monitor-learning-audit-retention")));
        JsonNode json = json(new HermesLearningAuditRetentionResponse(
                "learning-audit",
                true,
                false,
                true,
                "ready",
                "learning audit service configured",
                Map.of("retentionStatus", retentionStatus),
                retentionStatus,
                "file-system",
                true,
                4,
                5,
                1,
                0,
                80,
                true,
                false,
                "near-capacity",
                "warning",
                2,
                true,
                List.of("capacity warning"),
                List.of("monitor-learning-audit-retention")));

        assertThat(fields(json))
                .containsExactlyInAnyOrder(
                        "port",
                        "configured",
                        "noop",
                        "ready",
                        "status",
                        "reason",
                        "metadata",
                        "learningAuditRetentionStatus",
                        "ledgerType",
                        "bounded",
                        "recordCount",
                        "maxEntries",
                        "remainingEntries",
                        "overflowEntries",
                        "utilizationPercent",
                        "nearCapacity",
                        "atCapacity",
                        "retentionStatus",
                        "retentionSeverity",
                        "retentionPriority",
                        "retentionRequiresAttention",
                        "retentionAttention",
                        "retentionAttentionItems",
                        "retentionRecommendedActions",
                        "retentionRecommendedActionItems");
        assertThat(json.path("port").asText()).isEqualTo("learning-audit");
        assertThat(json.path("retentionStatus").asText()).isEqualTo("near-capacity");
        assertThat(json.path("retentionRequiresAttention").asBoolean()).isTrue();
        assertThat(json.path("learningAuditRetentionStatus").path("utilizationPercent").asInt()).isEqualTo(80);
        assertThat(json.path("retentionAttentionItems").get(0).path("severity").asText()).isEqualTo("warning");
        assertThat(json.path("retentionAttentionItems").get(0).path("message").asText()).isEqualTo("capacity warning");
        assertThat(json.path("retentionRecommendedActions").get(0).asText())
                .isEqualTo("monitor-learning-audit-retention");
        assertThat(json.path("retentionRecommendedActionItems").get(0).path("actionId").asText())
                .isEqualTo("monitor-learning-audit-retention");
        assertThat(json.path("retentionRecommendedActionItems").get(0).path("riskLevel").asText())
                .isEqualTo("low");
        assertThat(json.path("retentionRecommendedActionItems").get(0).path("safe").asBoolean())
                .isTrue();
    }

    @Test
    void statusResponseSerializesStableJsonShape() throws Exception {
        JsonNode json = json(new HermesStatusResponse(
                HermesStatusResponse.STATUS_DEGRADED,
                false,
                true,
                true,
                false,
                true,
                true,
                Map.of(
                        "ready", true,
                        "view", "summary",
                        HermesMetadataKeys.METADATA_LEARNING_AUDIT_RETENTION_OBSERVATION,
                        Map.of("outcome", "suppressed")),
                List.of(HermesOperationalMessages.MISSING_JOURNAL_PORT),
                Map.of("outcome", "suppressed")));

        assertThat(fields(json))
                .containsExactlyInAnyOrder(
                        "status",
                        "ready",
                        "diagnosticsConfigured",
                        "diagnosticsReady",
                        "journalConfigured",
                        "learningAuditConfigured",
                        "learningAuditReady",
                        "diagnostics",
                        "attention",
                        "attentionItems",
                        "attentionSummary",
                        "learningAuditRetentionObservation");
        assertThat(json.path("status").asText()).isEqualTo(HermesStatusResponse.STATUS_DEGRADED);
        assertThat(json.path("diagnosticsConfigured").asBoolean()).isTrue();
        assertThat(json.path("journalConfigured").asBoolean()).isFalse();
        assertThat(json.path("learningAuditConfigured").asBoolean()).isTrue();
        assertThat(json.path("learningAuditReady").asBoolean()).isTrue();
        assertThat(json.path("diagnostics").path("view").asText()).isEqualTo("summary");
        assertThat(json.path("learningAuditRetentionObservation").path("outcome").asText())
                .isEqualTo("suppressed");
        assertThat(json.path("attentionItems").get(0).path("message").asText())
                .isEqualTo(HermesOperationalMessages.MISSING_JOURNAL_PORT);
        assertThat(json.path("attentionSummary").path("totalItems").asInt()).isEqualTo(1);
        assertThat(json.path("attentionSummary").path("sourceCounts").path("hermes-status").asLong())
                .isEqualTo(1L);
        assertThat(json.path("attention").get(0).asText())
                .isEqualTo(HermesOperationalMessages.MISSING_JOURNAL_PORT);
    }

    @Test
    void apiErrorResponseSerializesStableJsonShape() throws Exception {
        JsonNode json = json(new ApiErrorResponse("requestId is required"));

        assertThat(fields(json)).containsExactly("error");
        assertThat(json.path("error").asText()).isEqualTo("requestId is required");
    }

    private JsonNode json(Object value) throws Exception {
        return objectMapper.readTree(objectMapper.writeValueAsString(value));
    }

    private List<String> fields(JsonNode node) {
        List<String> names = new ArrayList<>();
        node.fieldNames().forEachRemaining(names::add);
        return names;
    }
}
