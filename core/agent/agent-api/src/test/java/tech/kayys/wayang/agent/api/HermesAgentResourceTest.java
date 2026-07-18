package tech.kayys.wayang.agent.api;

import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.hermes.HermesAgentModeConfig;
import tech.kayys.wayang.agent.hermes.HermesLearningAuditPort;
import tech.kayys.wayang.agent.hermes.HermesLearningAuditService;
import tech.kayys.wayang.agent.hermes.HermesLearningPromotion;
import tech.kayys.wayang.agent.hermes.HermesLearningPromotionReceipt;
import tech.kayys.wayang.agent.hermes.HermesLearningPromotionReceiptLedger;
import tech.kayys.wayang.agent.hermes.HermesLearningPromotionReceiptLedgerEntry;
import tech.kayys.wayang.agent.hermes.HermesLearningPromotionReceiptPage;
import tech.kayys.wayang.agent.hermes.HermesLearningPromotionReceiptQuery;
import tech.kayys.wayang.agent.hermes.HermesRuntimeDiagnostics;
import tech.kayys.wayang.agent.hermes.HermesRuntimeDiagnosticsPort;
import tech.kayys.wayang.agent.hermes.HermesRuntimeEvent;
import tech.kayys.wayang.agent.hermes.HermesRuntimeEventPage;
import tech.kayys.wayang.agent.hermes.HermesRuntimeEventQuery;
import tech.kayys.wayang.agent.hermes.HermesRuntimeJournalPort;
import tech.kayys.wayang.agent.hermes.HermesRuntimeJournalService;
import tech.kayys.wayang.agent.hermes.HermesRuntimePorts;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class HermesAgentResourceTest {

    @Test
    void statusReturnsReadySummaryWhenOperationalPortsAreConfigured() {
        HermesAgentResource resource = new HermesAgentResource();
        HermesLearningAuditPort learningAuditPort = learningAuditPort();
        resource.runtimeDiagnosticsPort = HermesRuntimeDiagnosticsPort.service(
                HermesRuntimeDiagnostics.from(
                        HermesAgentModeConfig.defaults(),
                        HermesRuntimePorts.builder().learningAuditPort(learningAuditPort).build()));
        resource.runtimeJournalPort = HermesRuntimeJournalPort.service(
                new HermesRuntimeJournalService(query -> runtimeEventPage(List.of(), query)));
        resource.learningAuditPort = learningAuditPort;

        Response response = resource.status();

        assertThat(response.getStatus()).isEqualTo(200);
        HermesStatusResponse body = (HermesStatusResponse) response.getEntity();
        assertThat(body)
                .extracting(
                        HermesStatusResponse::status,
                        HermesStatusResponse::ready,
                        HermesStatusResponse::diagnosticsConfigured,
                        HermesStatusResponse::diagnosticsReady,
                        HermesStatusResponse::journalConfigured,
                        HermesStatusResponse::learningAuditConfigured,
                        HermesStatusResponse::learningAuditReady)
                .containsExactly(HermesStatusResponse.STATUS_UP, true, true, true, true, true, true);
        assertThat(body.attention())
                .containsExactly("Learned-skill persistence will use the provided SkillManagementService");
        assertThat(body.diagnostics())
                .containsEntry("ready", true)
                .containsEntry("runtimePortsReady", true)
                .containsEntry("skillPersistenceReady", true)
                .containsEntry("learningAuditConfigured", true)
                .containsEntry("learningAuditReady", true)
                .containsKey("diagnostics");
    }

    @Test
    void statusReturnsUnavailableWhenDiagnosticsPortIsMissing() {
        HermesAgentResource resource = new HermesAgentResource();
        resource.runtimeJournalPort = HermesRuntimeJournalPort.service(
                new HermesRuntimeJournalService(query -> runtimeEventPage(List.of(), query)));
        resource.learningAuditPort = learningAuditPort();

        Response response = resource.status();

        assertThat(response.getStatus()).isEqualTo(503);
        HermesStatusResponse body = (HermesStatusResponse) response.getEntity();
        assertThat(body)
                .extracting(
                        HermesStatusResponse::status,
                        HermesStatusResponse::ready,
                        HermesStatusResponse::diagnosticsConfigured,
                        HermesStatusResponse::diagnosticsReady,
                        HermesStatusResponse::journalConfigured,
                        HermesStatusResponse::learningAuditConfigured,
                        HermesStatusResponse::learningAuditReady)
                .containsExactly(HermesStatusResponse.STATUS_UNAVAILABLE, false, false, false, true, true, true);
        assertThat(body.attention())
                .containsExactly(HermesOperationalMessages.MISSING_DIAGNOSTICS_PORT);
        assertThat(body.diagnostics()).isEmpty();
    }

    @Test
    void statusReturnsDegradedWhenJournalPortIsMissing() {
        HermesAgentResource resource = new HermesAgentResource();
        HermesLearningAuditPort learningAuditPort = learningAuditPort();
        resource.runtimeDiagnosticsPort = HermesRuntimeDiagnosticsPort.service(
                HermesRuntimeDiagnostics.from(
                        HermesAgentModeConfig.defaults(),
                        HermesRuntimePorts.builder().learningAuditPort(learningAuditPort).build()));
        resource.learningAuditPort = learningAuditPort;

        Response response = resource.status();

        assertThat(response.getStatus()).isEqualTo(503);
        HermesStatusResponse body = (HermesStatusResponse) response.getEntity();
        assertThat(body)
                .extracting(
                        HermesStatusResponse::status,
                        HermesStatusResponse::ready,
                        HermesStatusResponse::diagnosticsConfigured,
                        HermesStatusResponse::diagnosticsReady,
                        HermesStatusResponse::journalConfigured,
                        HermesStatusResponse::learningAuditConfigured,
                        HermesStatusResponse::learningAuditReady)
                .containsExactly(HermesStatusResponse.STATUS_DEGRADED, false, true, true, false, true, true);
        assertThat(body.attention())
                .containsExactly(
                        "Learned-skill persistence will use the provided SkillManagementService",
                        HermesOperationalMessages.MISSING_JOURNAL_PORT);
    }

    @Test
    void diagnosticsReturnsSelectedPortView() {
        HermesAgentResource resource = new HermesAgentResource();
        resource.runtimeDiagnosticsPort = HermesRuntimeDiagnosticsPort.service(
                HermesRuntimeDiagnostics.from(HermesAgentModeConfig.defaults(), HermesRuntimePorts.noop()));

        Response response = resource.diagnostics(new HermesDiagnosticsRequest("skill-persistence"));

        assertThat(response.getStatus()).isEqualTo(200);
        HermesDiagnosticsResponse body = (HermesDiagnosticsResponse) response.getEntity();
        assertThat(body)
                .extracting(
                        HermesDiagnosticsResponse::port,
                        HermesDiagnosticsResponse::operation,
                        HermesDiagnosticsResponse::target,
                        HermesDiagnosticsResponse::successful,
                        HermesDiagnosticsResponse::status,
                        HermesDiagnosticsResponse::view)
                .containsExactly(
                        "runtime-diagnostics",
                        "inspect",
                        "runtime-diagnostics:skill-persistence",
                        true,
                        "inspected",
                        "skill-persistence");
        Map<String, Object> metadata = body.metadata();
        assertThat(metadata)
                .containsEntry("view", "skill-persistence")
                .containsEntry("ready", true)
                .containsKey("diagnostics");
        assertThat(body.diagnostics())
                .containsEntry("adapterResolution", "provided-skill-management")
                .containsEntry("ready", true);
    }

    @Test
    void capabilitiesReturnsDedicatedDiagnosticsView() {
        HermesAgentResource resource = new HermesAgentResource();
        resource.runtimeDiagnosticsPort = HermesRuntimeDiagnosticsPort.service(
                HermesRuntimeDiagnostics.from(HermesAgentModeConfig.defaults(), HermesRuntimePorts.noop()));

        Response response = resource.capabilities();

        assertThat(response.getStatus()).isEqualTo(200);
        HermesDiagnosticsResponse body = (HermesDiagnosticsResponse) response.getEntity();
        assertThat(body.metadata())
                .containsEntry("view", "capabilities")
                .containsKey("diagnostics");
        assertThat(body.view()).isEqualTo("capabilities");
        assertThat(body.diagnostics())
                .containsEntry("supportsSkillLearning", true)
                .containsKey("skillPersistenceStrategy");
    }

    @Test
    void lifecycleReturnsDedicatedDiagnosticsView() {
        HermesAgentResource resource = new HermesAgentResource();
        resource.runtimeDiagnosticsPort = HermesRuntimeDiagnosticsPort.service(
                HermesRuntimeDiagnostics.from(HermesAgentModeConfig.defaults(), HermesRuntimePorts.noop()));

        Response response = resource.lifecycle();

        assertThat(response.getStatus()).isEqualTo(200);
        HermesDiagnosticsResponse body = (HermesDiagnosticsResponse) response.getEntity();
        assertThat(body.metadata())
                .containsEntry("view", "lifecycle")
                .containsKey("diagnostics");
        assertThat(body.view()).isEqualTo("lifecycle");
        assertThat(body.diagnostics())
                .containsEntry("phase", "ready")
                .containsEntry("backgroundWorkEnabled", true)
                .containsEntry("sessionContinuityEnabled", true);
    }

    @Test
    void runtimePortsReturnsDedicatedDiagnosticsView() {
        HermesAgentResource resource = new HermesAgentResource();
        resource.runtimeDiagnosticsPort = HermesRuntimeDiagnosticsPort.service(
                HermesRuntimeDiagnostics.from(HermesAgentModeConfig.defaults(), HermesRuntimePorts.noop()));

        Response response = resource.runtimePorts();

        assertThat(response.getStatus()).isEqualTo(200);
        HermesDiagnosticsResponse body = (HermesDiagnosticsResponse) response.getEntity();
        assertThat(body.metadata())
                .containsEntry("view", "runtime-ports")
                .containsKey("diagnostics");
        assertThat(body.view()).isEqualTo("runtime-ports");
        assertThat(body.diagnostics())
                .containsEntry("configuredCount", 0L)
                .containsEntry("noopCount", 11L)
                .containsKey("ports");
    }

    @Test
    void skillPersistenceReturnsDedicatedDiagnosticsView() {
        HermesAgentResource resource = new HermesAgentResource();
        resource.runtimeDiagnosticsPort = HermesRuntimeDiagnosticsPort.service(
                HermesRuntimeDiagnostics.from(HermesAgentModeConfig.defaults(), HermesRuntimePorts.noop()));

        Response response = resource.skillPersistence();

        assertThat(response.getStatus()).isEqualTo(200);
        HermesDiagnosticsResponse body = (HermesDiagnosticsResponse) response.getEntity();
        assertThat(body.metadata())
                .containsEntry("view", "skill-persistence")
                .containsKey("diagnostics");
        assertThat(body.view()).isEqualTo("skill-persistence");
        assertThat(body.diagnostics())
                .containsEntry("adapterResolution", "provided-skill-management")
                .containsEntry("ready", true);
    }

    @Test
    void learningAuditDiagnosticsReturnsDedicatedDiagnosticsView() {
        HermesAgentResource resource = new HermesAgentResource();
        HermesLearningAuditPort learningAuditPort = learningAuditPort();
        resource.runtimeDiagnosticsPort = HermesRuntimeDiagnosticsPort.service(
                HermesRuntimeDiagnostics.from(
                        HermesAgentModeConfig.defaults(),
                        HermesRuntimePorts.builder().learningAuditPort(learningAuditPort).build()));

        Response response = resource.learningAuditDiagnostics();

        assertThat(response.getStatus()).isEqualTo(200);
        HermesDiagnosticsResponse body = (HermesDiagnosticsResponse) response.getEntity();
        assertThat(body.view()).isEqualTo("learning-audit");
        assertThat(body.learningAuditConfigured()).isTrue();
        assertThat(body.learningAuditReady()).isTrue();
        assertThat(body.diagnostics())
                .containsEntry("configured", true)
                .containsEntry("ready", true)
                .containsEntry("noop", false)
                .containsKey("port");
        @SuppressWarnings("unchecked")
        Map<String, Object> port = (Map<String, Object>) body.diagnostics().get("port");
        @SuppressWarnings("unchecked")
        Map<String, Object> portMetadata = (Map<String, Object>) port.get("metadata");
        @SuppressWarnings("unchecked")
        Map<String, Object> retentionStatus = (Map<String, Object>) portMetadata.get("retentionStatus");
        assertThat(retentionStatus)
                .containsEntry("ledgerType", "in-memory")
                .containsEntry("status", "unbounded")
                .containsEntry("requiresAttention", false)
                .containsKey("recommendedActions");
    }

    @Test
    void learningAuditRetentionReturnsCompactRetentionStatus() {
        HermesAgentResource resource = new HermesAgentResource();
        resource.learningAuditPort = learningAuditPort();

        Response response = resource.learningAuditRetention();

        assertThat(response.getStatus()).isEqualTo(200);
        HermesLearningAuditRetentionResponse body =
                (HermesLearningAuditRetentionResponse) response.getEntity();
        assertThat(body)
                .extracting(
                        HermesLearningAuditRetentionResponse::port,
                        HermesLearningAuditRetentionResponse::configured,
                        HermesLearningAuditRetentionResponse::ready,
                        HermesLearningAuditRetentionResponse::ledgerType,
                        HermesLearningAuditRetentionResponse::retentionStatus,
                        HermesLearningAuditRetentionResponse::retentionRequiresAttention)
                .containsExactly("learning-audit", true, true, "in-memory", "unbounded", false);
        assertThat(body.learningAuditRetentionStatus())
                .containsEntry("ledgerType", "in-memory")
                .containsEntry("status", "unbounded")
                .containsKey("recommendedActions");
    }

    @Test
    void diagnosticsReturnsNotFoundWhenPortIsMissing() {
        HermesAgentResource resource = new HermesAgentResource();

        Response response = resource.diagnostics(new HermesDiagnosticsRequest());

        assertThat(response.getStatus()).isEqualTo(404);
        assertThat(response.getEntity())
                .isEqualTo(new ApiErrorResponse(HermesOperationalMessages.MISSING_DIAGNOSTICS_PORT));
    }

    @Test
    void journalReturnsFilteredRuntimeEvents() {
        HermesAgentResource resource = new HermesAgentResource();
        HermesRuntimeEvent matchingEvent = runtimeEvent(
                HermesRuntimeEvent.TYPE_RESPONSE_COMPLETED,
                "req-1",
                "tenant-a",
                "session-a",
                "user-a",
                "successful",
                Instant.parse("2026-06-03T00:00:00Z"));
        HermesRuntimeEvent otherEvent = runtimeEvent(
                HermesRuntimeEvent.TYPE_RESPONSE_COMPLETED,
                "req-2",
                "tenant-a",
                "session-b",
                "user-a",
                "successful",
                Instant.parse("2026-06-03T01:00:00Z"));
        List<HermesRuntimeEvent> events = List.of(matchingEvent, otherEvent);
        resource.runtimeJournalPort = HermesRuntimeJournalPort.service(
                new HermesRuntimeJournalService(query -> runtimeEventPage(events, query)));

        Response response = resource.journal(journalRequest(
                null,
                null,
                "tenant-a",
                "session-a",
                null,
                "successful",
                null,
                null,
                5));

        assertThat(response.getStatus()).isEqualTo(200);
        HermesJournalResponse body = (HermesJournalResponse) response.getEntity();
        assertThat(body)
                .extracting(
                        HermesJournalResponse::port,
                        HermesJournalResponse::operation,
                        HermesJournalResponse::target,
                        HermesJournalResponse::successful,
                        HermesJournalResponse::status,
                        HermesJournalResponse::matchedEvents,
                        HermesJournalResponse::returnedEvents,
                        HermesJournalResponse::truncated,
                        HermesJournalResponse::journalStatus,
                        HermesJournalResponse::resumable,
                        HermesJournalResponse::requiresAttention)
                .containsExactly(
                        "runtime-journal",
                        "inspect",
                        "session:session-a",
                        true,
                        "inspected",
                        1,
                        1,
                        false,
                        "completed",
                        false,
                        false);
        Map<String, Object> metadata = body.metadata();
        assertThat(metadata)
                .containsEntry("matchedEvents", 1)
                .containsEntry("returnedEvents", 1)
                .containsEntry("truncated", false)
                .containsEntry("status", "completed")
                .containsEntry("resumable", false)
                .containsEntry("requiresAttention", false)
                .containsKeys("journalView", "sessionSnapshot", "query");
        assertThat(body.query())
                .containsEntry("tenantId", "tenant-a")
                .containsEntry("sessionId", "session-a")
                .containsEntry("outcome", "successful")
                .containsEntry("limit", 5);
        assertThat(body.sessionSnapshot())
                .containsEntry("latestRequestId", "req-1")
                .containsEntry("status", "completed");
    }

    @Test
    void latestJournalReturnsPresetRuntimeEvents() {
        HermesAgentResource resource = new HermesAgentResource();
        List<HermesRuntimeEvent> events = List.of(
                runtimeEvent(
                        HermesRuntimeEvent.TYPE_RESPONSE_COMPLETED,
                        "req-1",
                        "tenant-a",
                        "session-a",
                        "user-a",
                        "successful",
                        Instant.parse("2026-06-03T00:00:00Z")),
                runtimeEvent(
                        HermesRuntimeEvent.TYPE_RESPONSE_FAILED,
                        "req-2",
                        "tenant-a",
                        "session-b",
                        "user-a",
                        "failed",
                        Instant.parse("2026-06-03T01:00:00Z")));
        resource.runtimeJournalPort = HermesRuntimeJournalPort.service(
                new HermesRuntimeJournalService(query -> runtimeEventPage(events, query)));

        Response response = resource.latestJournal(new HermesJournalPresetRequest(1));

        assertThat(response.getStatus()).isEqualTo(200);
        HermesJournalResponse body = (HermesJournalResponse) response.getEntity();
        assertThat(body.target()).isEqualTo("latest");
        assertThat(body.matchedEvents()).isEqualTo(2);
        assertThat(body.returnedEvents()).isEqualTo(1);
        assertThat(body.truncated()).isTrue();
        assertThat(body.query())
                .containsEntry("outcome", "")
                .containsEntry("limit", 1);
    }

    @Test
    void journalReturnsCursorQueryMetadata() {
        HermesAgentResource resource = new HermesAgentResource();
        List<HermesRuntimeEvent> events = List.of(runtimeEvent(
                HermesRuntimeEvent.TYPE_RESPONSE_COMPLETED,
                "req-1",
                "tenant-a",
                "session-a",
                "user-a",
                "successful",
                Instant.parse("2026-06-03T00:00:00Z")));
        resource.runtimeJournalPort = HermesRuntimeJournalPort.service(
                new HermesRuntimeJournalService(query -> runtimeEventPage(events, query)));

        Response response = resource.journal(journalRequestWithCursor("evt-before", null, 5));

        assertThat(response.getStatus()).isEqualTo(200);
        HermesJournalResponse body = (HermesJournalResponse) response.getEntity();
        assertThat(body.target()).isEqualTo("before-event:evt-before");
        assertThat(body.query())
                .containsEntry("beforeEventId", "evt-before")
                .containsEntry("afterEventId", "")
                .containsEntry("limit", 5);
    }

    @Test
    void failedJournalReturnsPresetRuntimeEvents() {
        HermesAgentResource resource = new HermesAgentResource();
        List<HermesRuntimeEvent> events = List.of(
                runtimeEvent(
                        HermesRuntimeEvent.TYPE_RESPONSE_COMPLETED,
                        "req-1",
                        "tenant-a",
                        "session-a",
                        "user-a",
                        "successful",
                        Instant.parse("2026-06-03T00:00:00Z")),
                runtimeEvent(
                        HermesRuntimeEvent.TYPE_RESPONSE_FAILED,
                        "req-2",
                        "tenant-a",
                        "session-b",
                        "user-a",
                        "failed",
                        Instant.parse("2026-06-03T01:00:00Z")));
        resource.runtimeJournalPort = HermesRuntimeJournalPort.service(
                new HermesRuntimeJournalService(query -> runtimeEventPage(events, query)));

        Response response = resource.failedJournal(new HermesJournalPresetRequest(5));

        assertThat(response.getStatus()).isEqualTo(200);
        HermesJournalResponse body = (HermesJournalResponse) response.getEntity();
        assertThat(body.target()).isEqualTo("outcome:failed");
        assertThat(body.matchedEvents()).isEqualTo(1);
        assertThat(body.returnedEvents()).isEqualTo(1);
        assertThat(body.query())
                .containsEntry("outcome", "failed")
                .containsEntry("limit", 5);
        assertThat(body.sessionSnapshot())
                .containsEntry("latestRequestId", "req-2")
                .containsEntry("status", "needs-attention");
    }

    @Test
    void learningJournalReturnsPresetRuntimeEvents() {
        HermesAgentResource resource = new HermesAgentResource();
        List<HermesRuntimeEvent> events = List.of(
                runtimeEvent(
                        HermesRuntimeEvent.TYPE_RESPONSE_COMPLETED,
                        "req-1",
                        "tenant-a",
                        "session-a",
                        "user-a",
                        "successful",
                        Instant.parse("2026-06-03T00:00:00Z")),
                runtimeEvent(
                        HermesRuntimeEvent.TYPE_SKILL_LEARNING_COMPLETED,
                        "req-2",
                        "tenant-a",
                        "session-a",
                        "user-a",
                        "created",
                        Instant.parse("2026-06-03T01:00:00Z")),
                runtimeEvent(
                        HermesRuntimeEvent.TYPE_SKILL_LEARNING_FAILED,
                        "req-3",
                        "tenant-a",
                        "session-b",
                        "user-a",
                        "failed",
                        Instant.parse("2026-06-03T02:00:00Z")));
        resource.runtimeJournalPort = HermesRuntimeJournalPort.service(
                new HermesRuntimeJournalService(query -> runtimeEventPage(events, query)));

        Response response = resource.learningJournal(new HermesJournalPresetRequest(5));

        assertThat(response.getStatus()).isEqualTo(200);
        HermesJournalResponse body = (HermesJournalResponse) response.getEntity();
        assertThat(body.target()).isEqualTo("type-prefix:skill.learning");
        assertThat(body.matchedEvents()).isEqualTo(2);
        assertThat(body.returnedEvents()).isEqualTo(2);
        assertThat(body.query())
                .containsEntry("typePrefix", "skill.learning")
                .containsEntry("limit", 5);
        assertThat(body.journalView())
                .containsEntry("matchedEvents", 2)
                .containsEntry("returnedEvents", 2);
        assertThat(body.sessionSnapshot())
                .containsEntry("latestRequestId", "req-3")
                .containsEntry("status", "needs-attention");
    }

    @Test
    void learningAuditRetentionEventsReturnsRetentionRuntimeEvents() {
        HermesAgentResource resource = new HermesAgentResource();
        List<HermesRuntimeEvent> events = List.of(
                runtimeEvent(
                        HermesRuntimeEvent.TYPE_SKILL_LEARNING_COMPLETED,
                        "req-1",
                        "tenant-a",
                        "session-a",
                        "user-a",
                        "warning",
                        Instant.parse("2026-06-03T00:00:00Z")),
                runtimeEvent(
                        HermesRuntimeEvent.TYPE_LEARNING_AUDIT_RETENTION_ATTENTION,
                        "req-2",
                        "tenant-a",
                        "session-a",
                        "user-a",
                        "suppressed",
                        Instant.parse("2026-06-03T01:00:00Z")),
                runtimeEvent(
                        HermesRuntimeEvent.TYPE_LEARNING_AUDIT_RETENTION_ATTENTION,
                        "req-3",
                        "tenant-a",
                        "session-b",
                        "user-a",
                        "warning",
                        Instant.parse("2026-06-03T02:00:00Z")));
        resource.runtimeJournalPort = HermesRuntimeJournalPort.service(
                new HermesRuntimeJournalService(query -> runtimeEventPage(events, query)));

        Response response = resource.learningAuditRetentionEvents(journalRequest(
                HermesRuntimeEvent.TYPE_SKILL_LEARNING_COMPLETED,
                null,
                null,
                null,
                null,
                "warning",
                null,
                null,
                5));

        assertThat(response.getStatus()).isEqualTo(200);
        HermesJournalResponse body = (HermesJournalResponse) response.getEntity();
        assertThat(body.target()).isEqualTo(
                "type:" + HermesRuntimeEvent.TYPE_LEARNING_AUDIT_RETENTION_ATTENTION);
        assertThat(body.matchedEvents()).isEqualTo(1);
        assertThat(body.returnedEvents()).isEqualTo(1);
        assertThat(body.query())
                .containsEntry("type", HermesRuntimeEvent.TYPE_LEARNING_AUDIT_RETENTION_ATTENTION)
                .containsEntry("outcome", "warning")
                .containsEntry("limit", 5);
        assertThat(body.journalView())
                .containsEntry("matchedEvents", 1)
                .containsEntry("returnedEvents", 1);
        assertThat(body.learningAuditRetentionEvents()).hasSize(1);
        assertThat(body.learningAuditRetentionSummary().totalEvents()).isEqualTo(1);
        assertThat(body.learningAuditRetentionSummary().retentionStatusCounts())
                .containsEntry("unknown", 1L);
        assertThat(body.sessionSnapshot())
                .containsEntry("latestRequestId", "req-3")
                .containsEntry("status", "active")
                .containsEntry("pendingActionCount", 1);
    }

    @Test
    void learningAuditReturnsFilteredPromotionReceipts() {
        HermesAgentResource resource = new HermesAgentResource();
        resource.learningAuditPort = learningAuditPort();

        Response response = resource.learningAudit(new HermesLearningAuditRequest(
                "skill-a",
                null,
                null,
                null,
                false,
                5));

        assertThat(response.getStatus()).isEqualTo(200);
        HermesLearningAuditResponse body = (HermesLearningAuditResponse) response.getEntity();
        assertThat(body)
                .extracting(
                        HermesLearningAuditResponse::port,
                        HermesLearningAuditResponse::operation,
                        HermesLearningAuditResponse::target,
                        HermesLearningAuditResponse::successful,
                        HermesLearningAuditResponse::status,
                        HermesLearningAuditResponse::matchedReceipts,
                        HermesLearningAuditResponse::returnedReceipts,
                        HermesLearningAuditResponse::truncated,
                        HermesLearningAuditResponse::persistedReceipts,
                        HermesLearningAuditResponse::latestSkillId,
                        HermesLearningAuditResponse::latestOutcome)
                .containsExactly(
                        "learning-audit",
                        "inspect",
                        "skill:skill-a",
                        true,
                        "inspected",
                        1,
                        1,
                        false,
                        1L,
                        "skill-a",
                        HermesLearningPromotionReceipt.OUTCOME_PERSISTED);
        assertThat(body.query())
                .containsEntry("skillId", "skill-a")
                .containsEntry("limit", 5);
        assertThat(body.learningAuditView())
                .containsEntry("matchedReceipts", 1)
                .containsEntry("returnedReceipts", 1);
        assertThat(body.learningAuditSummary())
                .containsEntry("latestSkillId", "skill-a")
                .containsEntry("persistedReceipts", 1L);
    }

    @Test
    void learningAuditReturnsCursorWindowMetadata() {
        HermesAgentResource resource = new HermesAgentResource();
        resource.learningAuditPort = learningAuditPortWithEntries(List.of(
                ledgerEntry("2026-01-04T00:00:00Z", "key-4", "skill-4"),
                ledgerEntry("2026-01-01T00:00:00Z", "key-1", "skill-1"),
                ledgerEntry("2026-01-03T00:00:00Z", "key-3", "skill-3"),
                ledgerEntry("2026-01-02T00:00:00Z", "key-2", "skill-2")));

        Response response = resource.learningAudit(new HermesLearningAuditRequest(
                null,
                null,
                null,
                null,
                false,
                null,
                "key-3",
                1));

        assertThat(response.getStatus()).isEqualTo(200);
        HermesLearningAuditResponse body = (HermesLearningAuditResponse) response.getEntity();
        assertThat(body)
                .extracting(
                        HermesLearningAuditResponse::target,
                        HermesLearningAuditResponse::matchedReceipts,
                        HermesLearningAuditResponse::totalMatchedReceipts,
                        HermesLearningAuditResponse::returnedReceipts,
                        HermesLearningAuditResponse::previousCursor,
                        HermesLearningAuditResponse::nextCursor,
                        HermesLearningAuditResponse::firstCursor,
                        HermesLearningAuditResponse::lastCursor,
                        HermesLearningAuditResponse::hasPreviousPage,
                        HermesLearningAuditResponse::hasNextPage,
                        HermesLearningAuditResponse::cursorResolved)
                .containsExactly(
                        "after-receipt:key-3",
                        2,
                        4,
                        1,
                        "key-2",
                        "key-2",
                        "key-2",
                        "key-2",
                        true,
                        true,
                        true);
        assertThat(body.query())
                .containsEntry("afterReceiptId", "key-3")
                .containsEntry("limit", 1);
        assertThat(body.learningAuditView())
                .containsEntry("totalMatchedReceipts", 4)
                .containsEntry("cursorResolved", true);
    }

    @Test
    void learningAuditPresetRoutesReturnPromotionReceipts() {
        HermesAgentResource resource = new HermesAgentResource();
        resource.learningAuditPort = learningAuditPort();

        HermesLearningAuditResponse latest = (HermesLearningAuditResponse) resource.latestLearningAudit(
                new HermesLearningAuditPresetRequest(2)).getEntity();
        HermesLearningAuditResponse persisted = (HermesLearningAuditResponse) resource.persistedLearningAudit(
                new HermesLearningAuditPresetRequest(3)).getEntity();
        HermesLearningAuditResponse skill = (HermesLearningAuditResponse) resource.skillLearningAudit(
                "skill-a",
                new HermesLearningAuditPresetRequest(4)).getEntity();
        HermesLearningAuditResponse status = (HermesLearningAuditResponse) resource.statusLearningAudit(
                HermesLearningPromotion.STATUS_REJECTED,
                new HermesLearningAuditPresetRequest(5)).getEntity();
        HermesLearningAuditResponse outcome = (HermesLearningAuditResponse) resource.outcomeLearningAudit(
                HermesLearningPromotionReceipt.OUTCOME_REJECTED,
                new HermesLearningAuditPresetRequest(6)).getEntity();
        HermesLearningAuditResponse receipt = (HermesLearningAuditResponse) resource.receiptLearningAudit(
                "key-skill-a",
                new HermesLearningAuditPresetRequest(7)).getEntity();

        assertThat(latest.target()).isEqualTo("latest");
        assertThat(latest.query()).containsEntry("limit", 2);
        assertThat(persisted.target()).isEqualTo("persisted");
        assertThat(persisted.query()).containsEntry("persistedOnly", true).containsEntry("limit", 3);
        assertThat(skill.target()).isEqualTo("skill:skill-a");
        assertThat(skill.matchedReceipts()).isEqualTo(1);
        assertThat(status.target()).isEqualTo("status:rejected");
        assertThat(status.query()).containsEntry("status", HermesLearningPromotion.STATUS_REJECTED);
        assertThat(outcome.target()).isEqualTo("outcome:rejected");
        assertThat(outcome.query()).containsEntry("outcome", HermesLearningPromotionReceipt.OUTCOME_REJECTED);
        assertThat(receipt.target()).isEqualTo("receipt:key-skill-a");
        assertThat(receipt.query()).containsEntry("idempotencyKey", "key-skill-a");
    }

    @Test
    void identityJournalPresetsReturnFilteredRuntimeEvents() {
        HermesAgentResource resource = new HermesAgentResource();
        List<HermesRuntimeEvent> events = List.of(
                runtimeEvent(
                        HermesRuntimeEvent.TYPE_RESPONSE_COMPLETED,
                        "req-1",
                        "tenant-a",
                        "session-a",
                        "user-a",
                        "successful",
                        Instant.parse("2026-06-03T00:00:00Z")),
                runtimeEvent(
                        HermesRuntimeEvent.TYPE_RESPONSE_COMPLETED,
                        "req-2",
                        "tenant-b",
                        "session-b",
                        "user-b",
                        "successful",
                        Instant.parse("2026-06-03T01:00:00Z")));
        resource.runtimeJournalPort = HermesRuntimeJournalPort.service(
                new HermesRuntimeJournalService(query -> runtimeEventPage(events, query)));

        HermesJournalResponse request = (HermesJournalResponse) resource.requestJournal(
                "req-1",
                new HermesJournalPresetRequest(5)).getEntity();
        HermesJournalResponse session = (HermesJournalResponse) resource.sessionJournal(
                "session-b",
                new HermesJournalPresetRequest(5)).getEntity();
        HermesJournalResponse user = (HermesJournalResponse) resource.userJournal(
                "user-a",
                new HermesJournalPresetRequest(5)).getEntity();
        HermesJournalResponse tenant = (HermesJournalResponse) resource.tenantJournal(
                "tenant-b",
                new HermesJournalPresetRequest(5)).getEntity();

        assertThat(request.target()).isEqualTo("request:req-1");
        assertThat(request.matchedEvents()).isEqualTo(1);
        assertThat(request.query()).containsEntry("requestId", "req-1");
        assertThat(session.target()).isEqualTo("session:session-b");
        assertThat(session.sessionSnapshot()).containsEntry("latestRequestId", "req-2");
        assertThat(user.target()).isEqualTo("user:user-a");
        assertThat(user.sessionSnapshot()).containsEntry("latestRequestId", "req-1");
        assertThat(tenant.target()).isEqualTo("tenant:tenant-b");
        assertThat(tenant.sessionSnapshot()).containsEntry("latestRequestId", "req-2");
    }

    @Test
    void identityJournalPresetsRejectBlankRouteValues() {
        HermesAgentResource resource = new HermesAgentResource();
        resource.runtimeJournalPort = HermesRuntimeJournalPort.noop();

        assertBadRequest(
                resource.requestJournal(" ", new HermesJournalPresetRequest(5)),
                "requestId is required");
        assertBadRequest(
                resource.sessionJournal(" ", new HermesJournalPresetRequest(5)),
                "sessionId is required");
        assertBadRequest(
                resource.userJournal(" ", new HermesJournalPresetRequest(5)),
                "userId is required");
        assertBadRequest(
                resource.tenantJournal(" ", new HermesJournalPresetRequest(5)),
                "tenantId is required");
    }

    @Test
    void learningAuditPresetRoutesRejectBlankRouteValues() {
        HermesAgentResource resource = new HermesAgentResource();
        resource.learningAuditPort = HermesLearningAuditPort.noop();

        assertBadRequest(
                resource.skillLearningAudit(" ", new HermesLearningAuditPresetRequest(5)),
                "skillId is required");
        assertBadRequest(
                resource.statusLearningAudit(" ", new HermesLearningAuditPresetRequest(5)),
                "status is required");
        assertBadRequest(
                resource.outcomeLearningAudit(" ", new HermesLearningAuditPresetRequest(5)),
                "outcome is required");
        assertBadRequest(
                resource.receiptLearningAudit(" ", new HermesLearningAuditPresetRequest(5)),
                "idempotencyKey is required");
    }

    @Test
    void journalReturnsNotFoundWhenPortIsMissing() {
        HermesAgentResource resource = new HermesAgentResource();

        Response response = resource.journal(new HermesJournalRequest());

        assertThat(response.getStatus()).isEqualTo(404);
        assertThat(response.getEntity())
                .isEqualTo(new ApiErrorResponse(HermesOperationalMessages.MISSING_JOURNAL_PORT));
    }

    @Test
    void learningAuditRetentionEventsReturnsNotFoundWhenJournalPortIsMissing() {
        HermesAgentResource resource = new HermesAgentResource();

        Response response = resource.learningAuditRetentionEvents(new HermesJournalRequest());

        assertThat(response.getStatus()).isEqualTo(404);
        assertThat(response.getEntity())
                .isEqualTo(new ApiErrorResponse(HermesOperationalMessages.MISSING_JOURNAL_PORT));
    }

    @Test
    void learningAuditReturnsNotFoundWhenPortIsMissing() {
        HermesAgentResource resource = new HermesAgentResource();

        Response response = resource.learningAudit(new HermesLearningAuditRequest());

        assertThat(response.getStatus()).isEqualTo(404);
        assertThat(response.getEntity())
                .isEqualTo(new ApiErrorResponse(HermesOperationalMessages.MISSING_LEARNING_AUDIT_PORT));
    }

    @Test
    void learningAuditRetentionReturnsNotFoundWhenPortIsMissing() {
        HermesAgentResource resource = new HermesAgentResource();

        Response response = resource.learningAuditRetention();

        assertThat(response.getStatus()).isEqualTo(404);
        assertThat(response.getEntity())
                .isEqualTo(new ApiErrorResponse(HermesOperationalMessages.MISSING_LEARNING_AUDIT_PORT));
    }

    @Test
    void journalRejectsInvalidInstantQuery() {
        HermesAgentResource resource = new HermesAgentResource();
        resource.runtimeJournalPort = HermesRuntimeJournalPort.noop();

        Response response = resource.journal(journalRequest(null, null, null, null, null, null, "nope", null, 0));

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getEntity())
                .isEqualTo(new ApiErrorResponse("occurredFrom must be an ISO-8601 instant"));
    }

    @Test
    void journalRejectsConflictingCursorQuery() {
        HermesAgentResource resource = new HermesAgentResource();
        resource.runtimeJournalPort = HermesRuntimeJournalPort.noop();

        Response response = resource.journal(journalRequestWithCursor("evt-before", "evt-after", 5));

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getEntity())
                .isEqualTo(new ApiErrorResponse("beforeEventId and afterEventId cannot both be set"));
    }

    @Test
    void learningAuditRejectsConflictingCursorQuery() {
        HermesAgentResource resource = new HermesAgentResource();
        resource.learningAuditPort = HermesLearningAuditPort.noop();

        Response response = resource.learningAudit(new HermesLearningAuditRequest(
                null,
                null,
                null,
                null,
                false,
                "key-before",
                "key-after",
                5));

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getEntity())
                .isEqualTo(new ApiErrorResponse("beforeReceiptId and afterReceiptId cannot both be set"));
    }

    private void assertBadRequest(Response response, String message) {
        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getEntity()).isEqualTo(new ApiErrorResponse(message));
    }

    private HermesLearningAuditPort learningAuditPort() {
        HermesLearningPromotionReceiptLedger ledger = HermesLearningPromotionReceiptLedger.inMemory();
        ledger.record(receipt(
                "skill-a",
                "key-skill-a",
                HermesLearningPromotion.STATUS_APPROVED,
                HermesLearningPromotionReceipt.OUTCOME_PERSISTED,
                true));
        ledger.record(receipt(
                "skill-b",
                "key-skill-b",
                HermesLearningPromotion.STATUS_REJECTED,
                HermesLearningPromotionReceipt.OUTCOME_REJECTED,
                false));
        return HermesLearningAuditPort.service(new HermesLearningAuditService(ledger));
    }

    private HermesLearningAuditPort learningAuditPortWithEntries(
            List<HermesLearningPromotionReceiptLedgerEntry> entries) {
        HermesLearningPromotionReceiptLedger ledger = new HermesLearningPromotionReceiptLedger() {
            @Override
            public Optional<HermesLearningPromotionReceipt> find(String idempotencyKey) {
                return entries.stream()
                        .map(HermesLearningPromotionReceiptLedgerEntry::receipt)
                        .filter(receipt -> receipt.idempotencyKey().equals(idempotencyKey))
                        .findFirst();
            }

            @Override
            public HermesLearningPromotionReceipt record(HermesLearningPromotionReceipt receipt) {
                return receipt;
            }

            @Override
            public HermesLearningPromotionReceiptPage query(HermesLearningPromotionReceiptQuery query) {
                return HermesLearningPromotionReceiptPage.fromEntries(entries, query);
            }

            @Override
            public int recordCount() {
                return entries.size();
            }

            @Override
            public Map<String, Object> toMetadata() {
                return Map.of("ledgerType", "test", "recordCount", entries.size());
            }
        };
        return HermesLearningAuditPort.service(new HermesLearningAuditService(ledger));
    }

    private HermesLearningPromotionReceiptLedgerEntry ledgerEntry(
            String recordedAt,
            String key,
            String skillId) {
        return new HermesLearningPromotionReceiptLedgerEntry(
                recordedAt,
                receipt(
                        skillId,
                        key,
                        HermesLearningPromotion.STATUS_APPROVED,
                        HermesLearningPromotionReceipt.OUTCOME_PERSISTED,
                        true));
    }

    private HermesLearningPromotionReceipt receipt(
            String skillId,
            String key,
            String status,
            String outcome,
            boolean persisted) {
        return new HermesLearningPromotionReceipt(
                "promotion-" + skillId,
                key,
                status,
                outcome,
                skillId,
                persisted,
                "test receipt",
                "in-memory",
                "definitions=in-memory,artifacts=in-memory",
                Map.of("adapterId", "in-memory"));
    }

    private HermesRuntimeEvent runtimeEvent(
            String type,
            String requestId,
            String tenantId,
            String sessionId,
            String userId,
            String outcome,
            Instant occurredAt) {
        return new HermesRuntimeEvent(
                "",
                type,
                requestId,
                tenantId,
                sessionId,
                userId,
                outcome,
                occurredAt,
                Map.of("test", true));
    }

    private HermesJournalRequest journalRequest(
            String type,
            String requestId,
            String tenantId,
            String sessionId,
            String userId,
            String outcome,
            String occurredFrom,
            String occurredUntil,
            int limit) {
        return new HermesJournalRequest(
                type,
                requestId,
                tenantId,
                sessionId,
                userId,
                outcome,
                occurredFrom,
                occurredUntil,
                limit);
    }

    private HermesJournalRequest journalRequestWithCursor(
            String beforeEventId,
            String afterEventId,
            int limit) {
        return new HermesJournalRequest(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                beforeEventId,
                afterEventId,
                limit);
    }

    private HermesRuntimeEventPage runtimeEventPage(List<HermesRuntimeEvent> events, HermesRuntimeEventQuery query) {
        List<HermesRuntimeEvent> matched = events.stream()
                .filter(query::matches)
                .toList();
        return new HermesRuntimeEventPage(
                matched.stream().limit(query.limit()).toList(),
                matched.size());
    }
}
