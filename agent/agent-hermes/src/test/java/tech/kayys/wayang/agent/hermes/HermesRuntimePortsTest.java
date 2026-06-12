package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class HermesRuntimePortsTest {

    @Test
    void builderKeepsConfiguredPortsAndDefaultsMissingPortsToNoop() {
        HermesAutomationPort automationPort = directive -> result(
                "custom-automation",
                directive.operation(),
                directive.taskId(),
                directive.toMetadata());

        HermesRuntimePorts ports = HermesRuntimePorts.builder()
                .automationPort(automationPort)
                .build();

        assertThat(ports.automationPort().register(automationDirective()).port())
                .isEqualTo("custom-automation");
        HermesPortDispatchResult execution = ports.executionPort().dispatch(executionDirective());
        assertThat(execution.port()).isEqualTo("execution");
        assertThat(execution.status()).isEqualTo("noop");
        assertThat(ports.descriptors()).hasSize(11);
        assertThat(ports.toMetadata())
                .containsEntry("configuredCount", 1L)
                .containsEntry("readyCount", 11L)
                .containsEntry("noopCount", 10L);

        HermesRuntimePortDescriptor automationDescriptor = ports.automationPort().descriptor();
        assertThat(automationDescriptor.port()).isEqualTo("automation");
        assertThat(automationDescriptor.configured()).isTrue();
        assertThat(automationDescriptor.noop()).isFalse();
        assertThat(automationDescriptor.status()).isEqualTo("ready");

        HermesRuntimePortDescriptor executionDescriptor = ports.executionPort().descriptor();
        assertThat(executionDescriptor.port()).isEqualTo("execution");
        assertThat(executionDescriptor.configured()).isFalse();
        assertThat(executionDescriptor.noop()).isTrue();
        assertThat(executionDescriptor.status()).isEqualTo("noop");
        assertThat(executionDescriptor.toMetadata())
                .containsEntry("adapterType", "noop")
                .containsEntry("ready", true);
    }

    @Test
    void builderKeepsRuntimeJournalPort() {
        HermesRuntimeJournalPort journalPort = directive -> result(
                "custom-runtime-journal",
                directive.operation(),
                directive.target(),
                directive.toMetadata());

        HermesRuntimePorts ports = HermesRuntimePorts.builder()
                .runtimeJournalPort(journalPort)
                .build();

        HermesPortDispatchResult result = ports.runtimeJournalPort()
                .inspect(HermesRuntimeJournalDirective.latest(3));

        assertThat(result.port()).isEqualTo("custom-runtime-journal");
        assertThat(result.target()).isEqualTo("latest");
        assertThat(ports.runtimeJournalPort().descriptor().port()).isEqualTo("runtime-journal");
        assertThat(ports.toMetadata())
                .containsEntry("configuredCount", 1L)
                .containsEntry("readyCount", 11L)
                .containsEntry("noopCount", 10L);
    }

    @Test
    void builderKeepsLearningAuditPort() {
        HermesLearningAuditPort auditPort = directive -> result(
                "custom-learning-audit",
                directive.operation(),
                directive.target(),
                directive.toMetadata());

        HermesRuntimePorts ports = HermesRuntimePorts.builder()
                .learningAuditPort(auditPort)
                .build();

        HermesPortDispatchResult result = ports.learningAuditPort()
                .inspect(HermesLearningAuditDirective.skill("hermes-audit", 3));

        assertThat(result.port()).isEqualTo("custom-learning-audit");
        assertThat(result.target()).isEqualTo("skill:hermes-audit");
        assertThat(ports.learningAuditPort().descriptor().port()).isEqualTo("learning-audit");
        assertThat(ports.toMetadata())
                .containsEntry("configuredCount", 1L)
                .containsEntry("readyCount", 11L)
                .containsEntry("noopCount", 10L);
    }

    @Test
    void builderKeepsSkillLineagePort() {
        HermesSkillLineagePort lineagePort = directive -> result(
                "custom-skill-lineage",
                directive.operation(),
                directive.target(),
                directive.toMetadata());

        HermesRuntimePorts ports = HermesRuntimePorts.builder()
                .skillLineagePort(lineagePort)
                .build();

        HermesPortDispatchResult result = ports.skillLineagePort()
                .inspect(HermesSkillLineageDirective.inspect("hermes-audit"));

        assertThat(result.port()).isEqualTo("custom-skill-lineage");
        assertThat(result.target()).isEqualTo("skill:hermes-audit");
        assertThat(ports.skillLineagePort().descriptor().port()).isEqualTo("skill-lineage");
        assertThat(ports.toMetadata())
                .containsEntry("configuredCount", 1L)
                .containsEntry("readyCount", 11L)
                .containsEntry("noopCount", 10L);
    }

    @Test
    void composeRuntimePortsPrefersExplicitBundle() {
        HermesRuntimePorts bundled = HermesRuntimePorts.builder()
                .executionPort(directive -> result(
                        "bundled-execution",
                        directive.operation(),
                        directive.backend(),
                        directive.toMetadata()))
                .build();
        HermesExecutionPort individual = directive -> result(
                "individual-execution",
                directive.operation(),
                directive.backend(),
                directive.toMetadata());
        HermesRuntimeAdapterRegistry registry = HermesRuntimeAdapterRegistry.builder()
                .register(HermesRuntimeAdapterRegistry.DEFAULT_PROFILE, HermesRuntimePorts.builder()
                        .executionPort(directive -> result(
                                "registry-execution",
                                directive.operation(),
                                directive.backend(),
                                directive.toMetadata()))
                        .build())
                .build();

        HermesRuntimePorts ports = HermesRuntimePortsFactory.compose(
                Optional.of(bundled),
                Optional.of(registry),
                HermesAgentModeConfig.defaults(),
                Optional.of(individual),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty());

        assertThat(ports).isSameAs(bundled);
        assertThat(ports.executionPort().dispatch(executionDirective()).port())
                .isEqualTo("bundled-execution");
    }

    @Test
    void composeRuntimePortsUsesIndividualPortsWhenNoBundleExists() {
        HermesGatewayPort gatewayPort = directive -> result(
                "custom-gateway",
                directive.operation(),
                directive.destinationId(),
                directive.toMetadata());

        HermesRuntimePorts ports = HermesRuntimePortsFactory.compose(
                Optional.empty(),
                Optional.empty(),
                Optional.of(gatewayPort),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty());

        assertThat(ports.gatewayPort().deliver(gatewayDirective()).port())
                .isEqualTo("custom-gateway");
        assertThat(ports.automationPort().register(automationDirective()).status())
                .isEqualTo("noop");
    }

    @Test
    void composeRuntimePortsUsesConfiguredRegistryProfile() {
        HermesRuntimePorts livePorts = HermesRuntimePorts.builder()
                .gatewayPort(directive -> result(
                        "registry-gateway",
                        directive.operation(),
                        directive.destinationId(),
                        directive.toMetadata()))
                .build();
        HermesRuntimeAdapterRegistry registry = HermesRuntimeAdapterRegistry.builder()
                .register(HermesRuntimeAdapterRegistry.DEFAULT_PROFILE, HermesRuntimePorts.noop())
                .register("Live-VPS", livePorts)
                .build();
        HermesAgentModeConfig config = HermesAgentModeConfig.builder()
                .runtimeAdapterProfile("live-vps")
                .build();

        HermesRuntimePorts ports = HermesRuntimePortsFactory.compose(
                Optional.empty(),
                Optional.of(registry),
                config,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty());

        assertThat(ports).isSameAs(livePorts);
        assertThat(ports.gatewayPort().deliver(gatewayDirective()).port())
                .isEqualTo("registry-gateway");
        assertThat(registry.contains("LIVE-VPS")).isTrue();
        assertThat(registry.toMetadata())
                .containsEntry("profileCount", 2)
                .containsEntry("defaultProfileAvailable", true);
        assertThat(registry.profiles()).containsExactly("default", "live-vps");
    }

    @Test
    void composeRuntimePortsFallsBackToIndividualPortsWhenRegistryIsEmpty() {
        HermesGatewayPort gatewayPort = directive -> result(
                "fallback-gateway",
                directive.operation(),
                directive.destinationId(),
                directive.toMetadata());

        HermesRuntimePorts ports = HermesRuntimePortsFactory.compose(
                Optional.empty(),
                Optional.of(HermesRuntimeAdapterRegistry.builder().build()),
                HermesAgentModeConfig.builder().runtimeAdapterProfile("missing").build(),
                Optional.empty(),
                Optional.of(gatewayPort),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty());

        assertThat(ports.gatewayPort().deliver(gatewayDirective()).port())
                .isEqualTo("fallback-gateway");
    }

    @Test
    void factoryAddsDefaultSkillLineagePortWhenNoExplicitPortExists() {
        HermesRuntimePorts ports = HermesRuntimePortsFactory.create(
                Optional.empty(),
                Optional.empty(),
                HermesAgentModeConfig.defaults(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                null,
                List.of(),
                Optional.empty(),
                Optional.empty());

        HermesPortDispatchResult result = ports.skillLineagePort()
                .inspect(HermesSkillLineageDirective.catalog());

        assertThat(result.port()).isEqualTo("skill-lineage");
        assertThat(result.status()).isEqualTo("noop");
        assertThat(result.reason()).isEqualTo("skill lineage adapter not configured");
    }

    @Test
    void factoryAddsConfiguredLearningAuditPortWhenReceiptLedgerIsConfigured(@TempDir Path tempDir) {
        Path ledgerPath = tempDir.resolve("learning/promotion-receipts.jsonl");
        HermesAgentModeConfig config = HermesAgentModeConfig.builder()
                .persistenceHints(Map.of(
                        "learningPromotionReceiptLedgerStore", "file-system",
                        "learningPromotionReceiptLedgerPath", ledgerPath.toString()))
                .build();
        HermesLearningPromotionReceiptLedgerResolver.resolve(config).record(new HermesLearningPromotionReceipt(
                "promotion-audit",
                "key-audit",
                HermesLearningPromotion.STATUS_APPROVED,
                HermesLearningPromotionReceipt.OUTCOME_PERSISTED,
                "skill-audit",
                true,
                "persisted",
                "file-system",
                "definitions=file-system,artifacts=file-system",
                Map.of("adapterId", "file-system")));

        HermesRuntimePorts ports = HermesRuntimePortsFactory.create(
                Optional.empty(),
                Optional.empty(),
                config,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                null,
                List.of(),
                Optional.empty(),
                Optional.empty());

        HermesPortDispatchResult result = ports.learningAuditPort()
                .inspect(HermesLearningAuditDirective.skill("skill-audit", 10));

        assertThat(result.status()).isEqualTo("inspected");
        assertThat(result.metadata())
                .containsEntry("matchedReceipts", 1)
                .containsEntry("latestSkillId", "skill-audit");
        assertThat(ports.learningAuditPort().descriptor().configured()).isTrue();
    }

    @Test
    void repairAdapterRegistryUsesConfiguredLedgerAndAdapters(@TempDir Path tempDir) {
        Path ledgerPath = tempDir.resolve("repair-dispatch-ledger.jsonl");
        HermesAgentModeConfig config = HermesAgentModeConfig.builder()
                .skillLineageRepairDispatchLedgerStore("file-system")
                .skillLineageRepairDispatchLedgerPath(ledgerPath.toString())
                .skillLineageRepairDispatchLedgerMaxRecords(5)
                .build();

        HermesSkillLineageRepairAdapterRegistry registry =
                HermesRuntimePortsFactory.repairAdapterRegistry(
                        config,
                        List.of(HermesSkillLineageRepairAdapter.previewOnly(
                                "database-repair",
                                "database",
                                "database")),
                        Optional.empty(),
                        Optional.empty());

        assertThat(registry.toMetadata())
                .containsEntry("adapterCount", 1)
                .containsKey("dispatchLedger");
        assertThat(metadataMap(registry.toMetadata(), "dispatchLedger"))
                .containsEntry("ledgerType", "file-system")
                .containsEntry("ledgerPath", ledgerPath.toString())
                .containsEntry("maxRecords", 5)
                .containsEntry("replaySupported", true);
    }

    private static HermesPortDispatchResult result(
            String port,
            String operation,
            String target,
            Map<String, Object> metadata) {
        return new HermesPortDispatchResult(
                port,
                operation,
                target,
                true,
                true,
                true,
                "captured",
                "captured by test",
                metadata);
    }

    private static HermesExecutionDirective executionDirective() {
        return new HermesExecutionDirective(
                true,
                true,
                true,
                "dispatch",
                "local",
                "",
                "local-terminal",
                "standard",
                false,
                false,
                false,
                "req-a",
                "tenant-a",
                "session-a",
                "user-a",
                "execution requested");
    }

    private static HermesGatewayDirective gatewayDirective() {
        return new HermesGatewayDirective(
                true,
                true,
                true,
                "deliver",
                "telegram",
                "conversation",
                "conversation-a",
                "telegram:conversation-a",
                "channel-a",
                "",
                "conversation-a",
                "message-a",
                "tenant-a",
                "session-a",
                "user-a",
                "user-a",
                "corr-a",
                "gateway delivery requested");
    }

    private static HermesAutomationDirective automationDirective() {
        return new HermesAutomationDirective(
                true,
                true,
                true,
                "register",
                "hermes-automation-req-a",
                "Run daily report",
                "daily",
                "natural-language",
                "UTC",
                true,
                "req-a",
                "tenant-a",
                "session-a",
                "user-a",
                "explicit",
                "automation schedule requested");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> metadataMap(Map<String, Object> metadata, String key) {
        return (Map<String, Object>) metadata.get(key);
    }
}
