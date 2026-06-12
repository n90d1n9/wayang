package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HermesRuntimeDiagnosticsPortTest {

    @Test
    void directiveBuildsStableTargetsAndMetadata() {
        HermesRuntimeDiagnosticsDirective summary = HermesRuntimeDiagnosticsDirective.summary();
        HermesRuntimeDiagnosticsDirective runtimePorts = HermesRuntimeDiagnosticsDirective.runtimePorts();
        HermesRuntimeDiagnosticsDirective lifecycle = HermesRuntimeDiagnosticsDirective.lifecycle();
        HermesRuntimeDiagnosticsDirective skillPersistence = HermesRuntimeDiagnosticsDirective.skillPersistence();
        HermesRuntimeDiagnosticsDirective learningAudit = HermesRuntimeDiagnosticsDirective.learningAudit();
        HermesRuntimeDiagnosticsDirective inactive = HermesRuntimeDiagnosticsDirective.none();

        assertThat(summary.target()).isEqualTo("runtime-diagnostics:summary");
        assertThat(summary.toMetadata())
                .containsEntry("active", true)
                .containsEntry("operation", "inspect")
                .containsEntry("target", "runtime-diagnostics:summary")
                .containsEntry("view", "summary");
        assertThat(runtimePorts.target()).isEqualTo("runtime-diagnostics:runtime-ports");
        assertThat(lifecycle.target()).isEqualTo("runtime-diagnostics:lifecycle");
        assertThat(skillPersistence.target()).isEqualTo("runtime-diagnostics:skill-persistence");
        assertThat(learningAudit.target()).isEqualTo("runtime-diagnostics:learning-audit");
        assertThat(inactive.active()).isFalse();
        assertThat(inactive.operation()).isEqualTo("none");
        assertThat(HermesRuntimeDiagnosticsDirective.inspect("unknown").view()).isEqualTo("full");
    }

    @Test
    void serviceBackedPortReturnsSummaryView() {
        HermesRuntimeDiagnosticsPort port = HermesRuntimeDiagnosticsPort.service(
                HermesRuntimeDiagnostics.from(HermesAgentModeConfig.defaults(), HermesRuntimePorts.noop()));

        HermesPortDispatchResult result = port.inspect(HermesRuntimeDiagnosticsDirective.summary());

        assertThat(result.port()).isEqualTo("runtime-diagnostics");
        assertThat(result.status()).isEqualTo("inspected");
        assertThat(result.successful()).isTrue();
        assertThat(result.metadata())
                .containsEntry("ready", true)
                .containsEntry("runtimePortsReady", true)
                .containsEntry("skillPersistenceReady", true)
                .containsEntry("learningAuditConfigured", false)
                .containsEntry("learningAuditReady", false)
                .containsEntry("view", "summary")
                .containsKey("diagnostics");
        assertThat(metadataMap(result.metadata(), "diagnostics"))
                .containsEntry("ready", true)
                .containsEntry("configuredPortCount", 0L)
                .containsEntry("readyPortCount", 11L)
                .containsEntry("noopPortCount", 11L)
                .containsEntry("learningAuditConfigured", false)
                .containsEntry("learningAuditReady", false)
                .containsKey("assembly");
    }

    @Test
    void serviceBackedPortCanReturnLifecycleView() {
        HermesRuntimeDiagnosticsPort port = HermesRuntimeDiagnosticsPort.service(
                HermesRuntimeDiagnostics.from(HermesAgentModeConfig.defaults(), HermesRuntimePorts.noop()));

        HermesPortDispatchResult result = port.inspect(HermesRuntimeDiagnosticsDirective.lifecycle());

        assertThat(result.target()).isEqualTo("runtime-diagnostics:lifecycle");
        assertThat(metadataMap(result.metadata(), "diagnostics"))
                .containsEntry("phase", "ready")
                .containsEntry("backgroundWorkEnabled", true)
                .containsEntry("sessionContinuityEnabled", true);
    }

    @Test
    void serviceBackedPortCanReturnSkillPersistenceView() {
        HermesRuntimeDiagnosticsPort port = HermesRuntimeDiagnosticsPort.service(
                HermesRuntimeDiagnostics.from(HermesAgentModeConfig.defaults(), HermesRuntimePorts.noop()));

        HermesPortDispatchResult result = port.inspect(HermesRuntimeDiagnosticsDirective.skillPersistence());

        assertThat(result.target()).isEqualTo("runtime-diagnostics:skill-persistence");
        assertThat(metadataMap(result.metadata(), "diagnostics"))
                .containsEntry("ready", true)
                .containsEntry("adapterResolution", "provided-skill-management");
    }

    @Test
    void serviceBackedPortCanReturnLearningAuditView() {
        HermesLearningAuditPort learningAuditPort = HermesLearningAuditPort.service(
                HermesLearningPromotionReceiptLedger.inMemory());
        HermesRuntimeDiagnosticsPort port = HermesRuntimeDiagnosticsPort.service(
                HermesRuntimeDiagnostics.from(
                        HermesAgentModeConfig.defaults(),
                        HermesRuntimePorts.builder().learningAuditPort(learningAuditPort).build()));

        HermesPortDispatchResult result = port.inspect(HermesRuntimeDiagnosticsDirective.learningAudit());

        assertThat(result.target()).isEqualTo("runtime-diagnostics:learning-audit");
        assertThat(result.metadata())
                .containsEntry("learningAuditConfigured", true)
                .containsEntry("learningAuditReady", true);
        assertThat(metadataMap(result.metadata(), "diagnostics"))
                .containsEntry("configured", true)
                .containsEntry("ready", true)
                .containsEntry("noop", false)
                .containsKey("port");
    }

    @Test
    void serviceBackedPortMergesLiveMetadataOverlayIntoViews() {
        HermesRuntimeDiagnosticsPort port = HermesRuntimeDiagnosticsPort.service(
                HermesRuntimeDiagnostics.from(HermesAgentModeConfig.defaults(), HermesRuntimePorts.noop()),
                () -> Map.of(
                        HermesMetadataKeys.METADATA_LEARNING_AUDIT_RETENTION_OBSERVATION,
                        Map.of(
                                "outcome", "suppressed",
                                "reason", "duplicate-state")));

        HermesPortDispatchResult summary = port.inspect(HermesRuntimeDiagnosticsDirective.summary());
        HermesPortDispatchResult full = port.inspect(HermesRuntimeDiagnosticsDirective.full());

        assertThat(metadataMap(summary.metadata(), HermesMetadataKeys.METADATA_LEARNING_AUDIT_RETENTION_OBSERVATION))
                .containsEntry("outcome", "suppressed")
                .containsEntry("reason", "duplicate-state");
        assertThat(metadataMap(
                metadataMap(summary.metadata(), "diagnostics"),
                HermesMetadataKeys.METADATA_LEARNING_AUDIT_RETENTION_OBSERVATION))
                .containsEntry("outcome", "suppressed");
        assertThat(metadataMap(full.metadata(), "diagnostics"))
                .containsKey(HermesMetadataKeys.METADATA_LEARNING_AUDIT_RETENTION_OBSERVATION);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> metadataMap(Map<String, Object> metadata, String key) {
        return (Map<String, Object>) metadata.get(key);
    }
}
