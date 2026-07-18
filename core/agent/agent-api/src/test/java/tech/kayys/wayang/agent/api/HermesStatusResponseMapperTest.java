package tech.kayys.wayang.agent.api;

import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.hermes.HermesAgentModeConfig;
import tech.kayys.wayang.agent.hermes.HermesLearningAuditPort;
import tech.kayys.wayang.agent.hermes.HermesLearningPromotionReceipt;
import tech.kayys.wayang.agent.hermes.HermesLearningPromotionReceiptLedger;
import tech.kayys.wayang.agent.hermes.HermesLearningPromotionReceiptPage;
import tech.kayys.wayang.agent.hermes.HermesLearningPromotionReceiptQuery;
import tech.kayys.wayang.agent.hermes.HermesMetadataKeys;
import tech.kayys.wayang.agent.hermes.HermesRuntimeDiagnostics;
import tech.kayys.wayang.agent.hermes.HermesRuntimeDiagnosticsPort;
import tech.kayys.wayang.agent.hermes.HermesRuntimeEventPage;
import tech.kayys.wayang.agent.hermes.HermesRuntimeJournalPort;
import tech.kayys.wayang.agent.hermes.HermesRuntimeJournalService;
import tech.kayys.wayang.agent.hermes.HermesRuntimePorts;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class HermesStatusResponseMapperTest {

    private final HermesStatusResponseMapper mapper = new HermesStatusResponseMapper();

    @Test
    void mapsReadyPortsToOkStatus() {
        Response response = mapper.status(
                Optional.of(HermesRuntimeDiagnosticsPort.service(
                        HermesRuntimeDiagnostics.from(HermesAgentModeConfig.defaults(), HermesRuntimePorts.noop()))),
                Optional.of(journalPort()),
                Optional.of(learningAuditPort()));

        assertThat(response.getStatus()).isEqualTo(200);
        HermesStatusResponse body = (HermesStatusResponse) response.getEntity();
        assertThat(body)
                .extracting(
                        HermesStatusResponse::status,
                        HermesStatusResponse::ready,
                        HermesStatusResponse::diagnosticsConfigured,
                        HermesStatusResponse::journalConfigured,
                        HermesStatusResponse::learningAuditConfigured,
                        HermesStatusResponse::learningAuditReady)
                .containsExactly(HermesStatusResponse.STATUS_UP, true, true, true, true, true);
    }

    @Test
    void mapsMissingDiagnosticsPortToUnavailableStatus() {
        Response response = mapper.status(
                Optional.empty(),
                Optional.of(journalPort()),
                Optional.of(learningAuditPort()));

        assertThat(response.getStatus()).isEqualTo(503);
        HermesStatusResponse body = (HermesStatusResponse) response.getEntity();
        assertThat(body.status()).isEqualTo(HermesStatusResponse.STATUS_UNAVAILABLE);
        assertThat(body.diagnosticsConfigured()).isFalse();
        assertThat(body.journalConfigured()).isTrue();
        assertThat(body.learningAuditConfigured()).isTrue();
        assertThat(body.attention())
                .containsExactly(HermesOperationalMessages.MISSING_DIAGNOSTICS_PORT);
    }

    @Test
    void mapsNoopDiagnosticsPortToUnavailableStatus() {
        Response response = mapper.status(
                Optional.of(HermesRuntimeDiagnosticsPort.noop()),
                Optional.of(journalPort()),
                Optional.of(learningAuditPort()));

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
    }

    @Test
    void mapsMissingLearningAuditPortToDegradedStatus() {
        Response response = mapper.status(
                Optional.of(HermesRuntimeDiagnosticsPort.service(
                        HermesRuntimeDiagnostics.from(HermesAgentModeConfig.defaults(), HermesRuntimePorts.noop()))),
                Optional.of(journalPort()),
                Optional.empty());

        assertThat(response.getStatus()).isEqualTo(503);
        HermesStatusResponse body = (HermesStatusResponse) response.getEntity();
        assertThat(body.status()).isEqualTo(HermesStatusResponse.STATUS_DEGRADED);
        assertThat(body.ready()).isFalse();
        assertThat(body.learningAuditConfigured()).isFalse();
        assertThat(body.learningAuditReady()).isFalse();
        assertThat(body.attention())
                .contains(HermesOperationalMessages.MISSING_LEARNING_AUDIT_PORT);
    }

    @Test
    void includesLearningAuditRetentionAttentionInReadyStatus() {
        HermesLearningAuditPort auditPort = HermesLearningAuditPort.service(retentionLedger(4, 5));
        HermesRuntimePorts ports = HermesRuntimePorts.builder()
                .learningAuditPort(auditPort)
                .build();

        Response response = mapper.status(
                Optional.of(HermesRuntimeDiagnosticsPort.service(
                        HermesRuntimeDiagnostics.from(HermesAgentModeConfig.defaults(), ports))),
                Optional.of(journalPort()),
                Optional.of(auditPort));

        assertThat(response.getStatus()).isEqualTo(200);
        HermesStatusResponse body = (HermesStatusResponse) response.getEntity();
        assertThat(body.ready()).isTrue();
        assertThat(body.attention())
                .contains("Learning-audit receipt ledger is at 80% of retention capacity.");
        assertThat(body.diagnostics())
                .containsKey("attention");
    }

    @Test
    void includesLearningAuditRetentionObservationInReadyStatus() {
        Map<String, Object> observation = Map.of(
                "outcome", "suppressed",
                "reason", "duplicate-state");

        Response response = mapper.status(
                Optional.of(HermesRuntimeDiagnosticsPort.service(
                        HermesRuntimeDiagnostics.from(HermesAgentModeConfig.defaults(), HermesRuntimePorts.noop()),
                        () -> Map.of(
                                HermesMetadataKeys.METADATA_LEARNING_AUDIT_RETENTION_OBSERVATION,
                                observation))),
                Optional.of(journalPort()),
                Optional.of(learningAuditPort()));

        assertThat(response.getStatus()).isEqualTo(200);
        HermesStatusResponse body = (HermesStatusResponse) response.getEntity();
        assertThat(body.learningAuditRetentionObservation())
                .containsEntry("outcome", "suppressed")
                .containsEntry("reason", "duplicate-state");
        assertThat(body.diagnostics())
                .containsEntry(HermesMetadataKeys.METADATA_LEARNING_AUDIT_RETENTION_OBSERVATION, observation);
    }

    @Test
    void mapsDiagnosticsInspectionFailureToUnavailableStatus() {
        HermesRuntimeDiagnosticsPort failingPort = directive -> {
            throw new IllegalStateException("diagnostics offline");
        };

        Response response = mapper.status(Optional.of(failingPort), Optional.empty(), Optional.empty());

        assertThat(response.getStatus()).isEqualTo(503);
        HermesStatusResponse body = (HermesStatusResponse) response.getEntity();
        assertThat(body.status()).isEqualTo(HermesStatusResponse.STATUS_UNAVAILABLE);
        assertThat(body.ready()).isFalse();
        assertThat(body.attention())
                .containsExactly(
                        "diagnostics offline",
                        HermesOperationalMessages.MISSING_JOURNAL_PORT,
                        HermesOperationalMessages.MISSING_LEARNING_AUDIT_PORT);
    }

    private HermesRuntimeJournalPort journalPort() {
        return HermesRuntimeJournalPort.service(
                new HermesRuntimeJournalService(query -> new HermesRuntimeEventPage(
                        java.util.List.of(),
                        0)));
    }

    private HermesLearningAuditPort learningAuditPort() {
        return HermesLearningAuditPort.service(HermesLearningPromotionReceiptLedger.inMemory());
    }

    private HermesLearningPromotionReceiptLedger retentionLedger(int recordCount, int maxRecords) {
        return new HermesLearningPromotionReceiptLedger() {
            @Override
            public Optional<HermesLearningPromotionReceipt> find(String idempotencyKey) {
                return Optional.empty();
            }

            @Override
            public HermesLearningPromotionReceipt record(HermesLearningPromotionReceipt receipt) {
                return receipt;
            }

            @Override
            public HermesLearningPromotionReceiptPage query(HermesLearningPromotionReceiptQuery query) {
                return HermesLearningPromotionReceiptPage.empty(query);
            }

            @Override
            public int recordCount() {
                return recordCount;
            }

            @Override
            public Map<String, Object> toMetadata() {
                return Map.of(
                        "ledgerType", "file-system",
                        "recordCount", recordCount,
                        "retentionPolicy", Map.of(
                                "retentionMode", "max-entries",
                                "maxEntries", maxRecords));
            }
        };
    }
}
