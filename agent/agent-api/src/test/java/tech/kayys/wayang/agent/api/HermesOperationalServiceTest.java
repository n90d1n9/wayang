package tech.kayys.wayang.agent.api;

import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.hermes.HermesAgentModeConfig;
import tech.kayys.wayang.agent.hermes.HermesLearningAuditPort;
import tech.kayys.wayang.agent.hermes.HermesLearningAuditService;
import tech.kayys.wayang.agent.hermes.HermesLearningPromotion;
import tech.kayys.wayang.agent.hermes.HermesLearningPromotionReceipt;
import tech.kayys.wayang.agent.hermes.HermesLearningPromotionReceiptLedger;
import tech.kayys.wayang.agent.hermes.HermesRuntimeDiagnostics;
import tech.kayys.wayang.agent.hermes.HermesRuntimeDiagnosticsPort;
import tech.kayys.wayang.agent.hermes.HermesRuntimeEvent;
import tech.kayys.wayang.agent.hermes.HermesRuntimeEventPage;
import tech.kayys.wayang.agent.hermes.HermesRuntimeJournalPort;
import tech.kayys.wayang.agent.hermes.HermesRuntimeJournalService;
import tech.kayys.wayang.agent.hermes.HermesRuntimePorts;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HermesOperationalServiceTest {

    private final HermesOperationalService service = new HermesOperationalService();

    @Test
    void statusCoordinatesResolvedOperationalPorts() {
        Response response = service.status(new HermesOperationalPortSources(
                diagnosticsPort(),
                null,
                journalPort(),
                null,
                learningAuditPort(),
                null));

        assertThat(response.getStatus()).isEqualTo(200);
        HermesStatusResponse body = (HermesStatusResponse) response.getEntity();
        assertThat(body.ready()).isTrue();
        assertThat(body.diagnosticsConfigured()).isTrue();
        assertThat(body.journalConfigured()).isTrue();
        assertThat(body.learningAuditConfigured()).isTrue();
        assertThat(body.learningAuditReady()).isTrue();
    }

    @Test
    void diagnosticsCoordinatesDirectiveAndResponseMapping() {
        Response response = service.diagnostics(
                new HermesOperationalPortSources(diagnosticsPort(), null, null, null),
                new HermesDiagnosticsRequest("runtime-ports"));

        assertThat(response.getStatus()).isEqualTo(200);
        HermesDiagnosticsResponse body = (HermesDiagnosticsResponse) response.getEntity();
        assertThat(body.view()).isEqualTo("runtime-ports");
        assertThat(body.diagnostics()).containsKey("ports");
    }

    @Test
    void lifecycleCoordinatesDedicatedDiagnosticsView() {
        Response response = service.lifecycle(
                new HermesOperationalPortSources(diagnosticsPort(), null, null, null));

        assertThat(response.getStatus()).isEqualTo(200);
        HermesDiagnosticsResponse body = (HermesDiagnosticsResponse) response.getEntity();
        assertThat(body.view()).isEqualTo("lifecycle");
        assertThat(body.diagnostics())
                .containsEntry("phase", "ready")
                .containsEntry("backgroundWorkEnabled", true);
    }

    @Test
    void learningAuditDiagnosticsCoordinatesDedicatedDiagnosticsView() {
        HermesLearningAuditPort learningAuditPort = learningAuditPort();
        Response response = service.learningAuditDiagnostics(
                new HermesOperationalPortSources(
                        diagnosticsPort(HermesRuntimePorts.builder()
                                .learningAuditPort(learningAuditPort)
                                .build()),
                        null,
                        null,
                        null,
                        learningAuditPort,
                        null));

        assertThat(response.getStatus()).isEqualTo(200);
        HermesDiagnosticsResponse body = (HermesDiagnosticsResponse) response.getEntity();
        assertThat(body.view()).isEqualTo("learning-audit");
        assertThat(body.learningAuditConfigured()).isTrue();
        assertThat(body.learningAuditReady()).isTrue();
        assertThat(body.diagnostics())
                .containsEntry("configured", true)
                .containsEntry("ready", true)
                .containsEntry("noop", false);
    }

    @Test
    void learningAuditRetentionCoordinatesPortDescriptor() {
        Response response = service.learningAuditRetention(new HermesOperationalPortSources(
                null,
                null,
                null,
                null,
                learningAuditPort(),
                null));

        assertThat(response.getStatus()).isEqualTo(200);
        HermesLearningAuditRetentionResponse body =
                (HermesLearningAuditRetentionResponse) response.getEntity();
        assertThat(body.port()).isEqualTo("learning-audit");
        assertThat(body.configured()).isTrue();
        assertThat(body.ready()).isTrue();
        assertThat(body.ledgerType()).isEqualTo("in-memory");
        assertThat(body.retentionStatus()).isEqualTo("unbounded");
        assertThat(body.retentionRequiresAttention()).isFalse();
        assertThat(body.learningAuditRetentionStatus())
                .containsEntry("ledgerType", "in-memory")
                .containsEntry("status", "unbounded");
    }

    @Test
    void journalCoordinatesDeferredValidationAndResponseMapping() {
        Response response = service.journal(
                new HermesOperationalPortSources(null, null, journalPort(), null),
                new HermesJournalRequest());

        assertThat(response.getStatus()).isEqualTo(200);
        HermesJournalResponse body = (HermesJournalResponse) response.getEntity();
        assertThat(body.port()).isEqualTo("runtime-journal");
        assertThat(body.query()).containsEntry("limit", 100);
    }

    @Test
    void latestJournalCoordinatesPresetDirective() {
        Response response = service.latestJournal(
                new HermesOperationalPortSources(null, null, journalPort(), null),
                new HermesJournalPresetRequest(3));

        assertThat(response.getStatus()).isEqualTo(200);
        HermesJournalResponse body = (HermesJournalResponse) response.getEntity();
        assertThat(body.target()).isEqualTo("latest");
        assertThat(body.query())
                .containsEntry("outcome", "")
                .containsEntry("limit", 3);
    }

    @Test
    void failedJournalCoordinatesPresetDirective() {
        Response response = service.failedJournal(
                new HermesOperationalPortSources(null, null, journalPort(), null),
                new HermesJournalPresetRequest(4));

        assertThat(response.getStatus()).isEqualTo(200);
        HermesJournalResponse body = (HermesJournalResponse) response.getEntity();
        assertThat(body.target()).isEqualTo("outcome:failed");
        assertThat(body.query())
                .containsEntry("outcome", "failed")
                .containsEntry("limit", 4);
    }

    @Test
    void learningJournalCoordinatesPresetDirective() {
        Response response = service.learningJournal(
                new HermesOperationalPortSources(null, null, journalPort(), null),
                new HermesJournalPresetRequest(6));

        assertThat(response.getStatus()).isEqualTo(200);
        HermesJournalResponse body = (HermesJournalResponse) response.getEntity();
        assertThat(body.target()).isEqualTo("type-prefix:skill.learning");
        assertThat(body.query())
                .containsEntry("typePrefix", "skill.learning")
                .containsEntry("limit", 6);
    }

    @Test
    void learningAuditRetentionEventsCoordinatesFilteredDirective() {
        Response response = service.learningAuditRetentionEvents(
                new HermesOperationalPortSources(null, null, journalPort(), null),
                new HermesJournalRequest(
                        HermesRuntimeEvent.TYPE_RESPONSE_COMPLETED,
                        null,
                        null,
                        null,
                        null,
                        "warning",
                        null,
                        null,
                        "evt-before",
                        null,
                        8));

        assertThat(response.getStatus()).isEqualTo(200);
        HermesJournalResponse body = (HermesJournalResponse) response.getEntity();
        assertThat(body.target()).isEqualTo(
                "type:" + HermesRuntimeEvent.TYPE_LEARNING_AUDIT_RETENTION_ATTENTION);
        assertThat(body.query())
                .containsEntry("type", HermesRuntimeEvent.TYPE_LEARNING_AUDIT_RETENTION_ATTENTION)
                .containsEntry("outcome", "warning")
                .containsEntry("beforeEventId", "evt-before")
                .containsEntry("limit", 8);
    }

    @Test
    void learningAuditCoordinatesDirectiveAndResponseMapping() {
        HermesOperationalPortSources ports = new HermesOperationalPortSources(
                null,
                null,
                null,
                null,
                learningAuditPort(),
                null);

        Response response = service.learningAudit(
                ports,
                new HermesLearningAuditRequest(null, null, null, null, true, 4));

        assertThat(response.getStatus()).isEqualTo(200);
        HermesLearningAuditResponse body = (HermesLearningAuditResponse) response.getEntity();
        assertThat(body.port()).isEqualTo("learning-audit");
        assertThat(body.target()).isEqualTo("persisted");
        assertThat(body.query())
                .containsEntry("persistedOnly", true)
                .containsEntry("limit", 4);
        assertThat(body.matchedReceipts()).isEqualTo(1);
        assertThat(body.persistedReceipts()).isEqualTo(1L);
        assertThat(body.latestSkillId()).isEqualTo("skill-a");
    }

    @Test
    void learningAuditPresetsCoordinatePresetDirectives() {
        HermesOperationalPortSources ports = new HermesOperationalPortSources(
                null,
                null,
                null,
                null,
                learningAuditPort(),
                null);

        HermesLearningAuditResponse latest = (HermesLearningAuditResponse) service.latestLearningAudit(
                ports,
                new HermesLearningAuditPresetRequest(2)).getEntity();
        HermesLearningAuditResponse persisted = (HermesLearningAuditResponse) service.persistedLearningAudit(
                ports,
                new HermesLearningAuditPresetRequest(3)).getEntity();
        HermesLearningAuditResponse skill = (HermesLearningAuditResponse) service.skillLearningAudit(
                ports,
                "skill-a",
                new HermesLearningAuditPresetRequest(4)).getEntity();
        HermesLearningAuditResponse status = (HermesLearningAuditResponse) service.statusLearningAudit(
                ports,
                HermesLearningPromotion.STATUS_REJECTED,
                new HermesLearningAuditPresetRequest(5)).getEntity();
        HermesLearningAuditResponse outcome = (HermesLearningAuditResponse) service.outcomeLearningAudit(
                ports,
                HermesLearningPromotionReceipt.OUTCOME_REJECTED,
                new HermesLearningAuditPresetRequest(6)).getEntity();
        HermesLearningAuditResponse receipt = (HermesLearningAuditResponse) service.receiptLearningAudit(
                ports,
                "key-skill-a",
                new HermesLearningAuditPresetRequest(7)).getEntity();

        assertThat(latest.target()).isEqualTo("latest");
        assertThat(latest.query()).containsEntry("limit", 2);
        assertThat(persisted.target()).isEqualTo("persisted");
        assertThat(persisted.query()).containsEntry("persistedOnly", true).containsEntry("limit", 3);
        assertThat(skill.target()).isEqualTo("skill:skill-a");
        assertThat(skill.query()).containsEntry("skillId", "skill-a").containsEntry("limit", 4);
        assertThat(status.target()).isEqualTo("status:rejected");
        assertThat(status.query()).containsEntry("status", HermesLearningPromotion.STATUS_REJECTED);
        assertThat(outcome.target()).isEqualTo("outcome:rejected");
        assertThat(outcome.query()).containsEntry("outcome", HermesLearningPromotionReceipt.OUTCOME_REJECTED);
        assertThat(receipt.target()).isEqualTo("receipt:key-skill-a");
        assertThat(receipt.query()).containsEntry("idempotencyKey", "key-skill-a");
    }

    @Test
    void learningAuditIdentityPresetsRejectBlankIdentityValues() {
        HermesOperationalPortSources ports = new HermesOperationalPortSources(
                null,
                null,
                null,
                null,
                HermesLearningAuditPort.noop(),
                null);

        assertBadRequest(
                service.skillLearningAudit(ports, " ", new HermesLearningAuditPresetRequest(2)),
                "skillId is required");
        assertBadRequest(
                service.statusLearningAudit(ports, " ", new HermesLearningAuditPresetRequest(3)),
                "status is required");
        assertBadRequest(
                service.outcomeLearningAudit(ports, " ", new HermesLearningAuditPresetRequest(4)),
                "outcome is required");
        assertBadRequest(
                service.receiptLearningAudit(ports, " ", new HermesLearningAuditPresetRequest(5)),
                "idempotencyKey is required");
    }

    @Test
    void identityJournalPresetsCoordinatePresetDirectives() {
        HermesOperationalPortSources ports = new HermesOperationalPortSources(null, null, journalPort(), null);

        HermesJournalResponse request = (HermesJournalResponse) service.requestJournal(
                ports,
                "req-1",
                new HermesJournalPresetRequest(2)).getEntity();
        HermesJournalResponse session = (HermesJournalResponse) service.sessionJournal(
                ports,
                "session-a",
                new HermesJournalPresetRequest(3)).getEntity();
        HermesJournalResponse user = (HermesJournalResponse) service.userJournal(
                ports,
                "user-a",
                new HermesJournalPresetRequest(4)).getEntity();
        HermesJournalResponse tenant = (HermesJournalResponse) service.tenantJournal(
                ports,
                "tenant-a",
                new HermesJournalPresetRequest(5)).getEntity();

        assertThat(request.target()).isEqualTo("request:req-1");
        assertThat(request.query()).containsEntry("requestId", "req-1").containsEntry("limit", 2);
        assertThat(session.target()).isEqualTo("session:session-a");
        assertThat(session.query()).containsEntry("sessionId", "session-a").containsEntry("limit", 3);
        assertThat(user.target()).isEqualTo("user:user-a");
        assertThat(user.query()).containsEntry("userId", "user-a").containsEntry("limit", 4);
        assertThat(tenant.target()).isEqualTo("tenant:tenant-a");
        assertThat(tenant.query()).containsEntry("tenantId", "tenant-a").containsEntry("limit", 5);
    }

    @Test
    void identityJournalPresetsRejectBlankIdentityValues() {
        HermesOperationalPortSources ports = new HermesOperationalPortSources(null, null, journalPort(), null);

        assertBadRequest(
                service.requestJournal(ports, " ", new HermesJournalPresetRequest(2)),
                "requestId is required");
        assertBadRequest(
                service.sessionJournal(ports, " ", new HermesJournalPresetRequest(3)),
                "sessionId is required");
        assertBadRequest(
                service.userJournal(ports, " ", new HermesJournalPresetRequest(4)),
                "userId is required");
        assertBadRequest(
                service.tenantJournal(ports, " ", new HermesJournalPresetRequest(5)),
                "tenantId is required");
    }

    @Test
    void missingPortsUseOperationalErrors() {
        HermesOperationalPortSources ports = new HermesOperationalPortSources(null, null, null, null);

        Response diagnostics = service.capabilities(ports);
        Response journal = service.journal(ports, new HermesJournalRequest());

        assertThat(diagnostics.getStatus()).isEqualTo(404);
        assertThat(diagnostics.getEntity())
                .isEqualTo(new ApiErrorResponse(HermesOperationalMessages.MISSING_DIAGNOSTICS_PORT));
        assertThat(journal.getStatus()).isEqualTo(404);
        assertThat(journal.getEntity())
                .isEqualTo(new ApiErrorResponse(HermesOperationalMessages.MISSING_JOURNAL_PORT));
        assertThat(service.lifecycle(ports).getStatus()).isEqualTo(404);
        assertThat(service.learningAuditDiagnostics(ports).getStatus()).isEqualTo(404);
        assertThat(service.learningAuditRetention(ports).getStatus()).isEqualTo(404);
        assertThat(service.latestJournal(ports, new HermesJournalPresetRequest()).getStatus()).isEqualTo(404);
        assertThat(service.failedJournal(ports, new HermesJournalPresetRequest()).getStatus()).isEqualTo(404);
        assertThat(service.learningJournal(ports, new HermesJournalPresetRequest()).getStatus()).isEqualTo(404);
        assertThat(service.learningAuditRetentionEvents(
                ports,
                new HermesJournalRequest()).getStatus()).isEqualTo(404);
        assertThat(service.learningAudit(ports, new HermesLearningAuditRequest()).getStatus()).isEqualTo(404);
        assertThat(service.latestLearningAudit(
                ports,
                new HermesLearningAuditPresetRequest()).getStatus()).isEqualTo(404);
        assertThat(service.persistedLearningAudit(
                ports,
                new HermesLearningAuditPresetRequest()).getStatus()).isEqualTo(404);
        assertThat(service.skillLearningAudit(
                ports,
                "skill-a",
                new HermesLearningAuditPresetRequest()).getStatus()).isEqualTo(404);
        assertThat(service.statusLearningAudit(
                ports,
                "approved",
                new HermesLearningAuditPresetRequest()).getStatus()).isEqualTo(404);
        assertThat(service.outcomeLearningAudit(
                ports,
                "persisted",
                new HermesLearningAuditPresetRequest()).getStatus()).isEqualTo(404);
        assertThat(service.receiptLearningAudit(
                ports,
                "key-skill-a",
                new HermesLearningAuditPresetRequest()).getStatus()).isEqualTo(404);
        assertThat(service.requestJournal(ports, "req-1", new HermesJournalPresetRequest()).getStatus()).isEqualTo(404);
        assertThat(service.sessionJournal(ports, "session-a", new HermesJournalPresetRequest()).getStatus()).isEqualTo(404);
        assertThat(service.userJournal(ports, "user-a", new HermesJournalPresetRequest()).getStatus()).isEqualTo(404);
        assertThat(service.tenantJournal(ports, "tenant-a", new HermesJournalPresetRequest()).getStatus()).isEqualTo(404);
    }

    private void assertBadRequest(Response response, String message) {
        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getEntity()).isEqualTo(new ApiErrorResponse(message));
    }

    private HermesRuntimeDiagnosticsPort diagnosticsPort() {
        return diagnosticsPort(HermesRuntimePorts.noop());
    }

    private HermesRuntimeDiagnosticsPort diagnosticsPort(HermesRuntimePorts runtimePorts) {
        return HermesRuntimeDiagnosticsPort.service(
                HermesRuntimeDiagnostics.from(HermesAgentModeConfig.defaults(), runtimePorts));
    }

    private HermesRuntimeJournalPort journalPort() {
        return HermesRuntimeJournalPort.service(
                new HermesRuntimeJournalService(query -> new HermesRuntimeEventPage(List.of(), 0)));
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
}
