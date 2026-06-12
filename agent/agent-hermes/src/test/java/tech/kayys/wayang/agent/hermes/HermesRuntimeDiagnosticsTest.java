package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class HermesRuntimeDiagnosticsTest {

    @Test
    void reportsDefaultRuntimeReadyWithProvidedSkillManagement() {
        HermesRuntimeDiagnostics diagnostics = HermesRuntimeDiagnostics.from(
                HermesAgentModeConfig.defaults(),
                HermesRuntimePorts.noop());

        assertThat(diagnostics.ready()).isTrue();
        assertThat(diagnostics.runtimePortsReady()).isTrue();
        assertThat(diagnostics.skillPersistenceReady()).isTrue();
        assertThat(diagnostics.learningAuditConfigured()).isFalse();
        assertThat(diagnostics.learningAuditReady()).isFalse();
        assertThat(diagnostics.configuredPortCount()).isZero();
        assertThat(diagnostics.readyPortCount()).isEqualTo(11);
        assertThat(diagnostics.noopPortCount()).isEqualTo(11);
        assertThat(diagnostics.toMetadata())
                .containsEntry("ready", true)
                .containsEntry("runtimePortsReady", true)
                .containsEntry("skillPersistenceReady", true)
                .containsEntry("learningAuditConfigured", false)
                .containsEntry("learningAuditReady", false)
                .containsKey("assembly")
                .containsKey("runtimePorts")
                .containsKey("lifecycle")
                .containsKey("skillPersistencePreflight")
                .containsKey("learningAudit");
        assertThat(diagnostics.lifecycle().phase()).isEqualTo("ready");
    }

    @Test
    void reportsConfiguredLearningAuditPort() {
        HermesRuntimePorts ports = HermesRuntimePorts.builder()
                .learningAuditPort(HermesLearningAuditPort.service(HermesLearningPromotionReceiptLedger.inMemory()))
                .build();

        HermesRuntimeDiagnostics diagnostics = HermesRuntimeDiagnostics.from(
                HermesAgentModeConfig.defaults(),
                ports);

        assertThat(diagnostics.learningAuditConfigured()).isTrue();
        assertThat(diagnostics.learningAuditReady()).isTrue();
        assertThat(diagnostics.learningAuditMetadata())
                .containsEntry("configured", true)
                .containsEntry("ready", true)
                .containsEntry("noop", false)
                .containsEntry("status", "ready")
                .containsKey("port");
    }

    @Test
    void surfacesLearningAuditRetentionAttention() {
        HermesRuntimePorts ports = HermesRuntimePorts.builder()
                .learningAuditPort(HermesLearningAuditPort.service(retentionLedger("file-system", 4, 5)))
                .build();

        HermesRuntimeDiagnostics diagnostics = HermesRuntimeDiagnostics.from(
                HermesAgentModeConfig.defaults(),
                ports);

        assertThat(diagnostics.ready()).isTrue();
        assertThat(diagnostics.attention())
                .contains("Learning-audit receipt ledger is at 80% of retention capacity.");
        assertThat(diagnostics.learningAuditMetadata())
                .containsEntry("retentionRequiresAttention", true)
                .containsEntry("retentionSeverity", "warning")
                .containsEntry("retentionPriority", 2)
                .containsKey("retentionRecommendedActions");
        @SuppressWarnings("unchecked")
        Map<String, Object> retentionStatus =
                (Map<String, Object>) diagnostics.learningAuditMetadata().get("retentionStatus");
        assertThat(retentionStatus)
                .containsEntry("ledgerType", "file-system")
                .containsEntry("status", "near-capacity")
                .containsEntry("severity", "warning")
                .containsEntry("priority", 2)
                .containsEntry("requiresAttention", true);
    }

    @Test
    void reportsMissingResourcesForDurableSkillPersistenceTargets() {
        HermesAgentModeConfig config = HermesAgentModeConfig.builder()
                .persistenceHints(Map.of(
                        HermesSkillPersistenceHintKeys.DEFINITIONS, "database",
                        HermesSkillPersistenceHintKeys.ARTIFACTS, "s3",
                        HermesSkillPersistenceHintKeys.FALLBACK, "file-system"))
                .build();

        HermesRuntimeDiagnostics diagnostics = HermesRuntimeDiagnostics.from(
                config,
                HermesRuntimePorts.noop(),
                Optional.empty(),
                Optional.empty());

        assertThat(diagnostics.ready()).isFalse();
        assertThat(diagnostics.runtimePortsReady()).isTrue();
        assertThat(diagnostics.skillPersistenceReady()).isFalse();
        assertThat(diagnostics.lifecycle().phase()).isEqualTo("degraded");
        assertThat(diagnostics.assemblyReport()).isNotNull();
        assertThat(diagnostics.attention())
                .contains("Missing learned-skill persistence resources: DataSource, ObjectStorageService");
        @SuppressWarnings("unchecked")
        Map<String, Object> preflight = (Map<String, Object>) diagnostics.toMetadata()
                .get("skillPersistencePreflight");
        assertThat(preflight)
                .containsEntry("ready", false)
                .containsEntry("dataSourceRequired", true)
                .containsEntry("objectStorageRequired", true);
    }

    @Test
    void reportsUnavailableRuntimePorts() {
        HermesRuntimePorts ports = HermesRuntimePorts.builder()
                .executionPort(new HermesExecutionPort() {
                    @Override
                    public HermesPortDispatchResult dispatch(HermesExecutionDirective directive) {
                        return HermesPortDispatchResult.unavailable(
                                "execution",
                                directive.operation(),
                                directive.backend(),
                                "execution backend unavailable",
                                directive.toMetadata());
                    }

                    @Override
                    public HermesRuntimePortDescriptor descriptor() {
                        return new HermesRuntimePortDescriptor(
                                "execution",
                                "test-execution",
                                "custom",
                                true,
                                false,
                                false,
                                "unavailable",
                                "execution backend unavailable",
                                Map.of());
                    }
                })
                .build();

        HermesRuntimeDiagnostics diagnostics = HermesRuntimeDiagnostics.from(
                HermesAgentModeConfig.defaults(),
                ports);

        assertThat(diagnostics.ready()).isFalse();
        assertThat(diagnostics.runtimePortsReady()).isFalse();
        assertThat(diagnostics.skillPersistenceReady()).isTrue();
        assertThat(diagnostics.configuredPortCount()).isEqualTo(1);
        assertThat(diagnostics.readyPortCount()).isEqualTo(10);
        assertThat(diagnostics.attention())
                .contains("Hermes runtime port unavailable: execution (unavailable)");
    }

    private HermesLearningPromotionReceiptLedger retentionLedger(
            String ledgerType,
            int recordCount,
            int maxRecords) {
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
                        "ledgerType", ledgerType,
                        "recordCount", recordCount,
                        "retentionPolicy", Map.of(
                                "retentionMode", "max-entries",
                                "maxEntries", maxRecords));
            }
        };
    }
}
