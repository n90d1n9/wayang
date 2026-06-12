package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.spi.AgentType;
import tech.kayys.wayang.agent.spi.OrchestrationStrategy;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HermesAgentModeConfigTest {

    @Test
    void descriptorExposesHermesAsFirstClassMode() {
        HermesAgentModeDescriptor descriptor = HermesAgentMode.descriptor();

        assertThat(descriptor.id()).isEqualTo("hermes-agent");
        assertThat(descriptor.agentType()).isEqualTo(AgentType.HERMES);
        assertThat(descriptor.strategy()).isEqualTo(OrchestrationStrategy.HERMES_AGENT);
        assertThat(descriptor.features())
                .contains("persistent-memory", "autonomous-skill-creation", "parallel-subagents");
    }

    @Test
    void runtimeCapabilitiesExposeEffectiveModeContract() {
        HermesAgentModeConfig config = HermesAgentModeConfig.builder()
                .persistentMemoryEnabled(false)
                .mcpEnabled(false)
                .gatewayEnabled(false)
                .trajectoryExportEnabled(true)
                .defaultToolsets(List.of("skills", "memory", "mcp", "rag"))
                .gatewayPlatforms(List.of("cli", "slack"))
                .build();

        HermesRuntimeCapabilities capabilities = config.runtimeCapabilities();

        assertThat(capabilities.enabledFeatures())
                .contains("autonomous-skill-creation", "trajectory-export")
                .doesNotContain("persistent-memory", "mcp-tooling", "gateway-continuity");
        assertThat(capabilities.disabledFeatures())
                .contains("persistent-memory", "mcp-tooling", "gateway-continuity");
        assertThat(capabilities.defaultToolsets()).containsExactly("skills", "rag");
        assertThat(capabilities.gatewayPlatforms()).isEmpty();
        assertThat(capabilities.supportsGatewayPlatform("slack")).isFalse();
        assertThat(capabilities.supportsExecutionBackend("DOCKER")).isTrue();
        assertThat(capabilities.skillPersistenceStrategy().hasFileFallback()).isTrue();
        assertThat(capabilities.toMetadata())
                .containsEntry("supportsPersistentMemory", false)
                .containsEntry("supportsTrajectoryExport", true);
    }

    @Test
    void configDefaultsCaptureHermesPosture() {
        HermesAgentModeConfig config = HermesAgentModeConfig.defaults();

        assertThat(config.persistentMemoryEnabled()).isTrue();
        assertThat(config.skillLearningEnabled()).isTrue();
        assertThat(config.mcpEnabled()).isTrue();
        assertThat(config.defaultToolsets()).contains("skills", "memory", "mcp", "rag");
        assertThat(config.executionBackends()).contains("local", "docker", "ssh", "modal");
        assertThat(config.runtimeAdapterProfile()).isEqualTo(HermesRuntimeAdapterRegistry.DEFAULT_PROFILE);
        assertThat(config.runtimeEventJournalEnabled()).isFalse();
        assertThat(config.runtimeEventJournalStore()).isEqualTo("file-system");
        assertThat(config.runtimeEventJournalPath()).isEqualTo("var/hermes/runtime-events.jsonl");
        assertThat(config.runtimeEventJournalObjectPrefix())
                .isEqualTo(ObjectStorageHermesRuntimeEventSink.DEFAULT_PREFIX);
        assertThat(config.runtimeEventJournalJdbcTableName())
                .isEqualTo(DatabaseHermesRuntimeEventSink.DEFAULT_TABLE_NAME);
        assertThat(config.runtimeEventJournalJdbcInitializeSchema()).isTrue();
        assertThat(config.runtimeEventJournalFormat()).isEqualTo("jsonl");
        assertThat(config.runtimeEventJournalMaxEvents())
                .isEqualTo(FileSystemHermesRuntimeEventSink.DEFAULT_MAX_EVENTS);
        assertThat(config.skillLineageRemediationPolicy().dryRunOnly()).isTrue();
        assertThat(config.skillLineageRemediationPolicy().mutationAllowed()).isFalse();
        assertThat(config.skillLineageRepairBackends())
                .containsExactly("database", "file-system", "object-storage");
        assertThat(config.skillLineageRepairMutationBackends()).isEmpty();
        assertThat(config.skillLineageRepairBackendRegistry().backendIds())
                .containsExactly("database", "file-system", "object-storage");
        assertThat(config.skillLineageRepairApprovalStore()).isEqualTo("noop");
        assertThat(config.skillLineageRepairApprovalPath())
                .isEqualTo(FileSystemHermesSkillLineageRepairApprovalStore.DEFAULT_PATH);
        assertThat(config.skillLineageRepairApprovalObjectPrefix())
                .isEqualTo(ObjectStorageHermesSkillLineageRepairApprovalStore.DEFAULT_PREFIX);
        assertThat(config.skillLineageRepairApprovalJdbcTableName())
                .isEqualTo(DatabaseHermesSkillLineageRepairApprovalStore.DEFAULT_TABLE_NAME);
        assertThat(config.skillLineageRepairApprovalJdbcInitializeSchema()).isTrue();
        assertThat(config.skillLineageRepairDispatchLedgerStore()).isEqualTo("noop");
        assertThat(config.skillLineageRepairDispatchLedgerPath())
                .isEqualTo(HermesSkillLineageRepairAdapterDispatchLedgerResolver.DEFAULT_FILE_SYSTEM_PATH);
        assertThat(config.skillLineageRepairDispatchLedgerObjectPrefix())
                .isEqualTo(ObjectStorageHermesSkillLineageRepairAdapterDispatchLedger.DEFAULT_PREFIX);
        assertThat(config.skillLineageRepairDispatchLedgerJdbcTableName())
                .isEqualTo(DatabaseHermesSkillLineageRepairAdapterDispatchLedger.DEFAULT_TABLE_NAME);
        assertThat(config.skillLineageRepairDispatchLedgerJdbcInitializeSchema()).isTrue();
        assertThat(config.skillLineageRepairDispatchLedgerMaxRecords())
                .isEqualTo(FileSystemHermesSkillLineageRepairAdapterDispatchLedger.DEFAULT_MAX_RECORDS);
        assertThat(config.maxSubAgents()).isEqualTo(4);
        assertThat(config.persistenceHints()).containsEntry("fallback", "file-system");
        assertThat(config.skillPersistenceStrategy().hasFileFallback()).isTrue();
        assertThat(config.toMetadata())
                .containsKey("skillLineageRemediationPolicy")
                .containsEntry("skillLineageRepairApprovalStore", "noop")
                .containsEntry("skillLineageRepairDispatchLedgerStore", "noop");
    }

    @Test
    void builderAllowsRuntimeConfiguration() {
        HermesAgentModeConfig config = HermesAgentModeConfig.builder()
                .skillLearningEnabled(false)
                .preferLocalProviders(true)
                .defaultToolsets(List.of("skills"))
                .executionBackends(List.of("docker"))
                .runtimeAdapterProfile("vps-live")
                .runtimeEventJournalEnabled(true)
                .runtimeEventJournalStore("object_storage")
                .runtimeEventJournalPath("/tmp/hermes/events.jsonl")
                .runtimeEventJournalObjectPrefix("tenant-a/hermes-events")
                .runtimeEventJournalJdbcTableName("tenant_a_hermes_runtime_events")
                .runtimeEventJournalJdbcInitializeSchema(false)
                .runtimeEventJournalMaxEvents(250)
                .skillLineageRemediationPolicy(HermesSkillLineageRemediationPolicy.manual(
                        2,
                        List.of("repair-orphaned-lineage-root"),
                        List.of("lineage-root")))
                .skillLineageRepairBackends(List.of("database", "s3", "RustFS"))
                .skillLineageRepairMutationBackends(List.of("rustfs"))
                .skillLineageRepairApprovalStore("hybrid")
                .skillLineageRepairApprovalPath("/tmp/hermes/repair-approvals.jsonl")
                .skillLineageRepairApprovalObjectPrefix("tenant-a/hermes/repair-approvals")
                .skillLineageRepairApprovalJdbcTableName("tenant_a_hermes_repair_approvals")
                .skillLineageRepairApprovalJdbcInitializeSchema(false)
                .skillLineageRepairDispatchLedgerStore("hybrid")
                .skillLineageRepairDispatchLedgerPath("/tmp/hermes/repair-dispatch.jsonl")
                .skillLineageRepairDispatchLedgerObjectPrefix("tenant-a/hermes/repair-dispatch")
                .skillLineageRepairDispatchLedgerJdbcTableName("tenant_a_hermes_repair_dispatch_ledger")
                .skillLineageRepairDispatchLedgerJdbcInitializeSchema(false)
                .skillLineageRepairDispatchLedgerMaxRecords(128)
                .minStepsToLearn(5)
                .maxSubAgents(6)
                .build();

        assertThat(config.skillLearningEnabled()).isFalse();
        assertThat(config.preferLocalProviders()).isTrue();
        assertThat(config.defaultToolsets()).containsExactly("skills");
        assertThat(config.executionBackends()).containsExactly("docker");
        assertThat(config.runtimeAdapterProfile()).isEqualTo("vps-live");
        assertThat(config.runtimeEventJournalEnabled()).isTrue();
        assertThat(config.runtimeEventJournalStore()).isEqualTo("object-storage");
        assertThat(config.runtimeEventJournalPath()).isEqualTo("/tmp/hermes/events.jsonl");
        assertThat(config.runtimeEventJournalObjectPrefix()).isEqualTo("tenant-a/hermes-events");
        assertThat(config.runtimeEventJournalJdbcTableName())
                .isEqualTo("tenant_a_hermes_runtime_events");
        assertThat(config.runtimeEventJournalJdbcInitializeSchema()).isFalse();
        assertThat(config.runtimeEventJournalMaxEvents()).isEqualTo(250);
        assertThat(config.skillLineageRemediationPolicy().mode()).isEqualTo("manual");
        assertThat(config.skillLineageRemediationPolicy().maxActionsPerRun()).isEqualTo(2);
        assertThat(config.skillLineageRemediationPolicy().allowedActions())
                .containsExactly("repair-orphaned-lineage-root");
        assertThat(config.skillLineageRemediationPolicy().allowedTargetTypes()).containsExactly("lineage-root");
        assertThat(config.skillLineageRepairBackends()).containsExactly("database", "s3", "rustfs");
        assertThat(config.skillLineageRepairMutationBackends()).containsExactly("rustfs");
        assertThat(config.skillLineageRepairBackendRegistry().mutationBackendIds()).containsExactly("rustfs");
        assertThat(config.skillLineageRepairApprovalStore()).isEqualTo("hybrid");
        assertThat(config.skillLineageRepairApprovalPath()).isEqualTo("/tmp/hermes/repair-approvals.jsonl");
        assertThat(config.skillLineageRepairApprovalObjectPrefix())
                .isEqualTo("tenant-a/hermes/repair-approvals");
        assertThat(config.skillLineageRepairApprovalJdbcTableName())
                .isEqualTo("tenant_a_hermes_repair_approvals");
        assertThat(config.skillLineageRepairApprovalJdbcInitializeSchema()).isFalse();
        assertThat(config.skillLineageRepairDispatchLedgerStore()).isEqualTo("hybrid");
        assertThat(config.skillLineageRepairDispatchLedgerPath()).isEqualTo("/tmp/hermes/repair-dispatch.jsonl");
        assertThat(config.skillLineageRepairDispatchLedgerObjectPrefix())
                .isEqualTo("tenant-a/hermes/repair-dispatch");
        assertThat(config.skillLineageRepairDispatchLedgerJdbcTableName())
                .isEqualTo("tenant_a_hermes_repair_dispatch_ledger");
        assertThat(config.skillLineageRepairDispatchLedgerJdbcInitializeSchema()).isFalse();
        assertThat(config.skillLineageRepairDispatchLedgerMaxRecords()).isEqualTo(128);
        assertThat(config.minStepsToLearn()).isEqualTo(5);
        assertThat(config.maxSubAgents()).isEqualTo(6);
    }

    @Test
    void propertiesConfigureHermesMode() {
        Properties properties = new Properties();
        properties.setProperty("wayang.agent.hermes.skill-learning-enabled", "false");
        properties.setProperty("wayang.agent.hermes.prefer-local-providers", "yes");
        properties.setProperty("wayang.agent.hermes.min-steps-to-learn", "7");
        properties.setProperty("wayang.agent.hermes.max-sub-agents", "5");
        properties.setProperty("wayang.agent.hermes.preferred-provider", "ollama");
        properties.setProperty("wayang.agent.hermes.default-toolsets", "skills,memory,rag");
        properties.setProperty("wayang.agent.hermes.runtime-adapter-profile", "live-vps");
        properties.setProperty("wayang.agent.hermes.runtime-event-journal-enabled", "true");
        properties.setProperty("wayang.agent.hermes.runtime-event-journal-store", "jdbc");
        properties.setProperty("wayang.agent.hermes.runtime-event-journal-path", "/var/log/wayang/hermes.jsonl");
        properties.setProperty("wayang.agent.hermes.runtime-event-journal-object-prefix", "cloud/hermes/events");
        properties.setProperty(
                "wayang.agent.hermes.runtime-event-journal-jdbc-table-name",
                "wayang_runtime_events");
        properties.setProperty(
                "wayang.agent.hermes.runtime-event-journal-jdbc-initialize-schema",
                "false");
        properties.setProperty("wayang.agent.hermes.runtime-event-journal-max-events", "500");
        properties.setProperty("wayang.agent.hermes.skill-lineage-remediation-mode", "manual");
        properties.setProperty("wayang.agent.hermes.skill-lineage-remediation-max-actions", "3");
        properties.setProperty("wayang.agent.hermes.skill-lineage-remediation-allowed-actions",
                "repair-orphaned-lineage-root,inspect-learned-skill-storage-consistency");
        properties.setProperty("wayang.agent.hermes.skill-lineage-remediation-allowed-target-types",
                "lineage-root,catalog");
        properties.setProperty("wayang.agent.hermes.skill-lineage-repair-backends",
                "database,file-system,s3,rustfs");
        properties.setProperty("wayang.agent.hermes.skill-lineage-repair-mutation-backends", "rustfs");
        properties.setProperty("wayang.agent.hermes.skill-lineage-repair-approval-store", "database");
        properties.setProperty(
                "wayang.agent.hermes.skill-lineage-repair-approval-path",
                "/var/lib/wayang/hermes/repair-approvals.jsonl");
        properties.setProperty(
                "wayang.agent.hermes.skill-lineage-repair-approval-object-prefix",
                "cloud/hermes/repair-approvals");
        properties.setProperty(
                "wayang.agent.hermes.skill-lineage-repair-approval-jdbc-table-name",
                "wayang_approval_grants");
        properties.setProperty(
                "wayang.agent.hermes.skill-lineage-repair-approval-jdbc-initialize-schema",
                "false");
        properties.setProperty("wayang.agent.hermes.skill-lineage-repair-dispatch-ledger-store", "jdbc");
        properties.setProperty(
                "wayang.agent.hermes.skill-lineage-repair-dispatch-ledger-path",
                "/var/lib/wayang/hermes/repair-dispatch.jsonl");
        properties.setProperty(
                "wayang.agent.hermes.skill-lineage-repair-dispatch-ledger-object-prefix",
                "cloud/hermes/repair-dispatch");
        properties.setProperty(
                "wayang.agent.hermes.skill-lineage-repair-dispatch-ledger-jdbc-table-name",
                "wayang_repair_dispatch_ledger");
        properties.setProperty(
                "wayang.agent.hermes.skill-lineage-repair-dispatch-ledger-jdbc-initialize-schema",
                "false");
        properties.setProperty("wayang.agent.hermes.skill-lineage-repair-dispatch-ledger-max-records", "250");
        properties.setProperty("wayang.agent.hermes.learning-promotion-receipt-ledger-store", "jdbc");
        properties.setProperty(
                "wayang.agent.hermes.learning-promotion-receipt-ledger-path",
                "/var/lib/wayang/hermes/promotion-receipts.jsonl");
        properties.setProperty(
                "wayang.agent.hermes.learning-promotion-receipt-ledger-object-prefix",
                "cloud/hermes/promotion-receipts");
        properties.setProperty(
                "wayang.agent.hermes.learning-promotion-receipt-ledger-jdbc-table-name",
                "wayang_promotion_receipts");
        properties.setProperty(
                "wayang.agent.hermes.learning-promotion-receipt-ledger-jdbc-initialize-schema",
                "false");
        properties.setProperty("wayang.agent.hermes.learning-promotion-receipt-ledger-max-records", "125");
        properties.setProperty("wayang.agent.hermes.persistence-hints.definitions", "database");
        properties.setProperty("wayang.agent.hermes.persistence-hints.artifacts", "s3");

        HermesAgentModeConfig config = HermesAgentModeConfigs.fromProperties(properties);

        assertThat(config.skillLearningEnabled()).isFalse();
        assertThat(config.preferLocalProviders()).isTrue();
        assertThat(config.minStepsToLearn()).isEqualTo(7);
        assertThat(config.maxSubAgents()).isEqualTo(5);
        assertThat(config.preferredProvider()).isEqualTo("ollama");
        assertThat(config.defaultToolsets()).containsExactly("skills", "memory", "rag");
        assertThat(config.runtimeAdapterProfile()).isEqualTo("live-vps");
        assertThat(config.runtimeEventJournalEnabled()).isTrue();
        assertThat(config.runtimeEventJournalStore()).isEqualTo("database");
        assertThat(config.runtimeEventJournalPath()).isEqualTo("/var/log/wayang/hermes.jsonl");
        assertThat(config.runtimeEventJournalObjectPrefix()).isEqualTo("cloud/hermes/events");
        assertThat(config.runtimeEventJournalJdbcTableName()).isEqualTo("wayang_runtime_events");
        assertThat(config.runtimeEventJournalJdbcInitializeSchema()).isFalse();
        assertThat(config.runtimeEventJournalMaxEvents()).isEqualTo(500);
        assertThat(config.skillLineageRemediationPolicy().mode()).isEqualTo("manual");
        assertThat(config.skillLineageRemediationPolicy().approvalRequired()).isTrue();
        assertThat(config.skillLineageRemediationPolicy().maxActionsPerRun()).isEqualTo(3);
        assertThat(config.skillLineageRemediationPolicy().allowedActions())
                .containsExactly(
                        "repair-orphaned-lineage-root",
                        "inspect-learned-skill-storage-consistency");
        assertThat(config.skillLineageRemediationPolicy().allowedTargetTypes())
                .containsExactly("lineage-root", "catalog");
        assertThat(config.skillLineageRepairBackends())
                .containsExactly("database", "file-system", "s3", "rustfs");
        assertThat(config.skillLineageRepairMutationBackends()).containsExactly("rustfs");
        assertThat(config.skillLineageRepairBackendRegistry().backendCount()).isEqualTo(4);
        assertThat(config.skillLineageRepairBackendRegistry().mutationBackendIds()).containsExactly("rustfs");
        assertThat(config.skillLineageRepairApprovalStore()).isEqualTo("database");
        assertThat(config.skillLineageRepairApprovalPath())
                .isEqualTo("/var/lib/wayang/hermes/repair-approvals.jsonl");
        assertThat(config.skillLineageRepairApprovalObjectPrefix())
                .isEqualTo("cloud/hermes/repair-approvals");
        assertThat(config.skillLineageRepairApprovalJdbcTableName()).isEqualTo("wayang_approval_grants");
        assertThat(config.skillLineageRepairApprovalJdbcInitializeSchema()).isFalse();
        assertThat(config.skillLineageRepairDispatchLedgerStore()).isEqualTo("database");
        assertThat(config.skillLineageRepairDispatchLedgerPath())
                .isEqualTo("/var/lib/wayang/hermes/repair-dispatch.jsonl");
        assertThat(config.skillLineageRepairDispatchLedgerObjectPrefix())
                .isEqualTo("cloud/hermes/repair-dispatch");
        assertThat(config.skillLineageRepairDispatchLedgerJdbcTableName())
                .isEqualTo("wayang_repair_dispatch_ledger");
        assertThat(config.skillLineageRepairDispatchLedgerJdbcInitializeSchema()).isFalse();
        assertThat(config.skillLineageRepairDispatchLedgerMaxRecords()).isEqualTo(250);
        assertThat(config.persistenceHints())
                .containsEntry("definitions", "database")
                .containsEntry("artifacts", "s3")
                .containsEntry("learningPromotionReceiptLedgerStore", "jdbc")
                .containsEntry(
                        "learningPromotionReceiptLedgerPath",
                        "/var/lib/wayang/hermes/promotion-receipts.jsonl")
                .containsEntry(
                        "learningPromotionReceiptLedgerObjectPrefix",
                        "cloud/hermes/promotion-receipts")
                .containsEntry("learningPromotionReceiptLedgerJdbcTableName", "wayang_promotion_receipts")
                .containsEntry("learningPromotionReceiptLedgerJdbcInitializeSchema", "false")
                .containsEntry("learningPromotionReceiptLedgerMaxRecords", "125")
                .containsEntry("fallback", "file-system");
        assertThat(HermesLearningPromotionReceiptLedgerResolver.metadata(config))
                .containsEntry("ledgerStore", "database")
                .containsEntry("ledgerPath", "/var/lib/wayang/hermes/promotion-receipts.jsonl")
                .containsEntry("ledgerObjectPrefix", "cloud/hermes/promotion-receipts")
                .containsEntry("ledgerJdbcTableName", "wayang_promotion_receipts")
                .containsEntry("ledgerJdbcInitializeSchema", false)
                .containsEntry("maxRecords", 125);
        assertThat(config.skillPersistenceStrategy().usesDatabaseDefinitions()).isTrue();
        assertThat(config.skillPersistenceStrategy().cloudStores()).containsExactly("s3");
    }

    @Test
    void environmentConfigUsesWayangHermesPrefix() {
        HermesAgentModeConfig config = HermesAgentModeConfigs.fromEnvironment(Map.ofEntries(
                Map.entry("WAYANG_AGENT_HERMES_REQUIRE_TOOL_CALLING", "off"),
                Map.entry("WAYANG_AGENT_HERMES_MEMORY_ENTRY_LIMIT", "3"),
                Map.entry("WAYANG_AGENT_HERMES_MAX_SUB_AGENTS", "2"),
                Map.entry("WAYANG_AGENT_HERMES_GATEWAY_PLATFORMS", "cli;slack"),
                Map.entry("WAYANG_AGENT_HERMES_RUNTIME_ADAPTER_PROFILE", "home-server"),
                Map.entry("WAYANG_AGENT_HERMES_RUNTIME_EVENT_JOURNAL_ENABLED", "true"),
                Map.entry("WAYANG_AGENT_HERMES_RUNTIME_EVENT_JOURNAL_STORE", "object-storage"),
                Map.entry("WAYANG_AGENT_HERMES_RUNTIME_EVENT_JOURNAL_JDBC_TABLE_NAME", "env_runtime_events"),
                Map.entry("WAYANG_AGENT_HERMES_RUNTIME_EVENT_JOURNAL_MAX_EVENTS", "25"),
                Map.entry("WAYANG_AGENT_HERMES_SKILL_LINEAGE_REMEDIATION_MODE", "manual"),
                Map.entry("WAYANG_AGENT_HERMES_SKILL_LINEAGE_REMEDIATION_MAX_ACTIONS", "1"),
                Map.entry("WAYANG_AGENT_HERMES_SKILL_LINEAGE_REMEDIATION_ALLOWED_ACTIONS", "all"),
                Map.entry("WAYANG_AGENT_HERMES_SKILL_LINEAGE_REMEDIATION_ALLOWED_TARGET_TYPES", "catalog"),
                Map.entry("WAYANG_AGENT_HERMES_SKILL_LINEAGE_REPAIR_BACKENDS", "object-storage;rustfs"),
                Map.entry("WAYANG_AGENT_HERMES_SKILL_LINEAGE_REPAIR_MUTATION_BACKENDS", "rustfs"),
                Map.entry("WAYANG_AGENT_HERMES_SKILL_LINEAGE_REPAIR_APPROVAL_STORE", "jdbc"),
                Map.entry(
                        "WAYANG_AGENT_HERMES_SKILL_LINEAGE_REPAIR_APPROVAL_OBJECT_PREFIX",
                        "env/hermes/repair-approvals"),
                Map.entry(
                        "WAYANG_AGENT_HERMES_SKILL_LINEAGE_REPAIR_APPROVAL_JDBC_TABLE_NAME",
                        "env_approval_grants"),
                Map.entry("WAYANG_AGENT_HERMES_SKILL_LINEAGE_REPAIR_DISPATCH_LEDGER_STORE", "db"),
                Map.entry(
                        "WAYANG_AGENT_HERMES_SKILL_LINEAGE_REPAIR_DISPATCH_LEDGER_OBJECT_PREFIX",
                        "env/hermes/repair-dispatch"),
                Map.entry(
                        "WAYANG_AGENT_HERMES_SKILL_LINEAGE_REPAIR_DISPATCH_LEDGER_JDBC_TABLE_NAME",
                        "env_repair_dispatch_ledger"),
                Map.entry("WAYANG_AGENT_HERMES_SKILL_LINEAGE_REPAIR_DISPATCH_LEDGER_MAX_RECORDS", "30"),
                Map.entry("WAYANG_AGENT_HERMES_PROMOTION_RECEIPT_LEDGER_STORE", "file-system"),
                Map.entry("WAYANG_AGENT_HERMES_PERSISTENCE_HINTS_ARTIFACTS", "rustfs")));

        assertThat(config.requireToolCalling()).isFalse();
        assertThat(config.memoryEntryLimit()).isEqualTo(3);
        assertThat(config.maxSubAgents()).isEqualTo(2);
        assertThat(config.gatewayPlatforms()).containsExactly("cli", "slack");
        assertThat(config.runtimeAdapterProfile()).isEqualTo("home-server");
        assertThat(config.runtimeEventJournalEnabled()).isTrue();
        assertThat(config.runtimeEventJournalStore()).isEqualTo("object-storage");
        assertThat(config.runtimeEventJournalJdbcTableName()).isEqualTo("env_runtime_events");
        assertThat(config.runtimeEventJournalMaxEvents()).isEqualTo(25);
        assertThat(config.skillLineageRemediationPolicy().mode()).isEqualTo("manual");
        assertThat(config.skillLineageRemediationPolicy().maxActionsPerRun()).isEqualTo(1);
        assertThat(config.skillLineageRemediationPolicy().allowedActions()).containsExactly("all");
        assertThat(config.skillLineageRemediationPolicy().allowedTargetTypes()).containsExactly("catalog");
        assertThat(config.skillLineageRepairBackends()).containsExactly("object-storage", "rustfs");
        assertThat(config.skillLineageRepairMutationBackends()).containsExactly("rustfs");
        assertThat(config.skillLineageRepairApprovalStore()).isEqualTo("database");
        assertThat(config.skillLineageRepairApprovalObjectPrefix())
                .isEqualTo("env/hermes/repair-approvals");
        assertThat(config.skillLineageRepairApprovalJdbcTableName()).isEqualTo("env_approval_grants");
        assertThat(config.skillLineageRepairDispatchLedgerStore()).isEqualTo("database");
        assertThat(config.skillLineageRepairDispatchLedgerObjectPrefix())
                .isEqualTo("env/hermes/repair-dispatch");
        assertThat(config.skillLineageRepairDispatchLedgerJdbcTableName())
                .isEqualTo("env_repair_dispatch_ledger");
        assertThat(config.skillLineageRepairDispatchLedgerMaxRecords()).isEqualTo(30);
        assertThat(config.persistenceHints())
                .containsEntry("artifacts", "rustfs")
                .containsEntry("learningPromotionReceiptLedgerStore", "file-system")
                .containsEntry("fallback", "file-system");
    }

    @Test
    void mapConfigSupportsNestedRuntimeShape() {
        HermesAgentModeConfig config = HermesAgentModeConfigs.fromMap(Map.of(
                "wayang", Map.of(
                        "agent", Map.of(
                                "hermes", Map.ofEntries(
                                        Map.entry("mcp-enabled", "false"),
                                        Map.entry("execution-backends", "local,docker,ssh"),
                                        Map.entry("runtime-adapter-profile", "hybrid-lab"),
                                        Map.entry("runtime-event-journal-store", "hybrid"),
                                        Map.entry("runtime-event-journal-path", "logs/hermes.jsonl"),
                                        Map.entry("runtime-event-journal-object-prefix", "object/hermes/events"),
                                        Map.entry("runtime-event-journal-jdbc-table-name", "nested_runtime_events"),
                                        Map.entry("runtime-event-journal-jdbc-initialize-schema", "off"),
                                        Map.entry("runtime-event-journal-max-events", "75"),
                                        Map.entry("skill-lineage-remediation-mode", "manual"),
                                        Map.entry("skill-lineage-remediation-max-actions", "2"),
                                        Map.entry("skill-lineage-remediation-allowed-actions", "all"),
                                        Map.entry(
                                                "skill-lineage-remediation-allowed-target-types",
                                                "lineage-root,catalog"),
                                        Map.entry("lineage-repair-approval-store", "hybrid"),
                                        Map.entry(
                                                "lineage-repair-approval-path",
                                                "logs/hermes-repair-approvals.jsonl"),
                                        Map.entry(
                                                "lineage-repair-approval-object-prefix",
                                                "objects/hermes-repair-approvals"),
                                        Map.entry(
                                                "lineage-repair-approval-jdbc-table-name",
                                                "nested_approval_grants"),
                                        Map.entry(
                                                "lineage-repair-approval-jdbc-initialize-schema",
                                                "off"),
                                        Map.entry("lineage-repair-dispatch-ledger-store", "file-system"),
                                        Map.entry(
                                                "lineage-repair-dispatch-ledger-path",
                                                "logs/hermes-repair-dispatch.jsonl"),
                                        Map.entry(
                                                "lineage-repair-dispatch-ledger-object-prefix",
                                                "objects/hermes-repair-dispatch"),
                                        Map.entry(
                                                "lineage-repair-dispatch-ledger-jdbc-table-name",
                                                "nested_repair_dispatch_ledger"),
                                        Map.entry(
                                                "lineage-repair-dispatch-ledger-jdbc-initialize-schema",
                                                "no"),
                                        Map.entry("lineage-repair-dispatch-ledger-max-records", "33"),
                                        Map.entry("receipt-ledger-store", "hybrid"),
                                        Map.entry("receipt-ledger-path", "logs/promotion-receipts.jsonl"),
                                        Map.entry(
                                                "receipt-ledger-object-prefix",
                                                "objects/hermes-promotion-receipts"),
                                        Map.entry("receipt-ledger-database-table", "nested_promotion_receipts"),
                                        Map.entry("receipt-ledger-database-initialize-schema", "off"),
                                        Map.entry("receipt-ledger-max-records", "42"),
                                        Map.entry(
                                                "persistence-hints",
                                                "definitions=hybrid,artifacts=object-storage"))))));

        assertThat(config.mcpEnabled()).isFalse();
        assertThat(config.executionBackends()).containsExactly("local", "docker", "ssh");
        assertThat(config.runtimeAdapterProfile()).isEqualTo("hybrid-lab");
        assertThat(config.runtimeEventJournalStore()).isEqualTo("hybrid");
        assertThat(config.runtimeEventJournalPath()).isEqualTo("logs/hermes.jsonl");
        assertThat(config.runtimeEventJournalObjectPrefix()).isEqualTo("object/hermes/events");
        assertThat(config.runtimeEventJournalJdbcTableName()).isEqualTo("nested_runtime_events");
        assertThat(config.runtimeEventJournalJdbcInitializeSchema()).isFalse();
        assertThat(config.runtimeEventJournalMaxEvents()).isEqualTo(75);
        assertThat(config.skillLineageRemediationPolicy().mode()).isEqualTo("manual");
        assertThat(config.skillLineageRemediationPolicy().allowedActions()).containsExactly("all");
        assertThat(config.skillLineageRemediationPolicy().allowedTargetTypes())
                .containsExactly("lineage-root", "catalog");
        assertThat(config.skillLineageRepairApprovalStore()).isEqualTo("hybrid");
        assertThat(config.skillLineageRepairApprovalPath())
                .isEqualTo("logs/hermes-repair-approvals.jsonl");
        assertThat(config.skillLineageRepairApprovalObjectPrefix())
                .isEqualTo("objects/hermes-repair-approvals");
        assertThat(config.skillLineageRepairApprovalJdbcTableName()).isEqualTo("nested_approval_grants");
        assertThat(config.skillLineageRepairApprovalJdbcInitializeSchema()).isFalse();
        assertThat(config.skillLineageRepairDispatchLedgerStore()).isEqualTo("file-system");
        assertThat(config.skillLineageRepairDispatchLedgerPath())
                .isEqualTo("logs/hermes-repair-dispatch.jsonl");
        assertThat(config.skillLineageRepairDispatchLedgerObjectPrefix())
                .isEqualTo("objects/hermes-repair-dispatch");
        assertThat(config.skillLineageRepairDispatchLedgerJdbcTableName())
                .isEqualTo("nested_repair_dispatch_ledger");
        assertThat(config.skillLineageRepairDispatchLedgerJdbcInitializeSchema()).isFalse();
        assertThat(config.skillLineageRepairDispatchLedgerMaxRecords()).isEqualTo(33);
        assertThat(config.persistenceHints())
                .containsEntry("definitions", "hybrid")
                .containsEntry("artifacts", "object-storage")
                .containsEntry("learningPromotionReceiptLedgerStore", "hybrid")
                .containsEntry("learningPromotionReceiptLedgerPath", "logs/promotion-receipts.jsonl")
                .containsEntry(
                        "learningPromotionReceiptLedgerObjectPrefix",
                        "objects/hermes-promotion-receipts")
                .containsEntry("learningPromotionReceiptLedgerJdbcTableName", "nested_promotion_receipts")
                .containsEntry("learningPromotionReceiptLedgerJdbcInitializeSchema", "off")
                .containsEntry("learningPromotionReceiptLedgerMaxRecords", "42");
    }

    @Test
    void mapConfigSupportsRuntimeEventAliases() {
        HermesAgentModeConfig config = HermesAgentModeConfigs.fromMap(Map.ofEntries(
                Map.entry("wayang.agent.hermes.runtime-events-enabled", "enabled"),
                Map.entry("wayang.agent.hermes.runtime-events-store", "database"),
                Map.entry("wayang.agent.hermes.runtime-events-path", "alt/events.jsonl"),
                Map.entry("wayang.agent.hermes.runtime-events-object-prefix", "alt/events"),
                Map.entry("wayang.agent.hermes.runtime-events-jdbc-table-name", "alt_runtime_events"),
                Map.entry("wayang.agent.hermes.runtime-events-jdbc-initialize-schema", "no"),
                Map.entry("wayang.agent.hermes.runtime-events-format", "jsonl"),
                Map.entry("wayang.agent.hermes.runtime-events-max-events", "64")));

        assertThat(config.runtimeEventJournalEnabled()).isTrue();
        assertThat(config.runtimeEventJournalStore()).isEqualTo("database");
        assertThat(config.runtimeEventJournalPath()).isEqualTo("alt/events.jsonl");
        assertThat(config.runtimeEventJournalObjectPrefix()).isEqualTo("alt/events");
        assertThat(config.runtimeEventJournalJdbcTableName()).isEqualTo("alt_runtime_events");
        assertThat(config.runtimeEventJournalJdbcInitializeSchema()).isFalse();
        assertThat(config.runtimeEventJournalFormat()).isEqualTo("jsonl");
        assertThat(config.runtimeEventJournalMaxEvents()).isEqualTo(64);
    }

    @Test
    void mapConfigSupportsRuntimeEventCamelAliases() {
        HermesAgentModeConfig config = HermesAgentModeConfigs.fromMap(Map.ofEntries(
                Map.entry("wayang.agent.hermes.runtimeEventsEnabled", "on"),
                Map.entry("wayang.agent.hermes.runtimeEventsStore", "object-storage"),
                Map.entry("wayang.agent.hermes.runtimeEventsPath", "camel/events.jsonl"),
                Map.entry("wayang.agent.hermes.runtimeEventsObjectPrefix", "camel/events"),
                Map.entry("wayang.agent.hermes.runtimeEventsJdbcTableName", "camel_runtime_events"),
                Map.entry("wayang.agent.hermes.runtimeEventsJdbcInitializeSchema", "disabled"),
                Map.entry("wayang.agent.hermes.runtimeEventsMaxEvents", "71")));

        assertThat(config.runtimeEventJournalEnabled()).isTrue();
        assertThat(config.runtimeEventJournalStore()).isEqualTo("object-storage");
        assertThat(config.runtimeEventJournalPath()).isEqualTo("camel/events.jsonl");
        assertThat(config.runtimeEventJournalObjectPrefix()).isEqualTo("camel/events");
        assertThat(config.runtimeEventJournalJdbcTableName()).isEqualTo("camel_runtime_events");
        assertThat(config.runtimeEventJournalJdbcInitializeSchema()).isFalse();
        assertThat(config.runtimeEventJournalMaxEvents()).isEqualTo(71);
    }

    @Test
    void mapConfigSupportsLegacyRuntimeEventDatabaseAliases() {
        HermesAgentModeConfig config = HermesAgentModeConfigs.fromMap(Map.of(
                "wayang.agent.hermes.runtime-event-journal-database-table", "legacy_runtime_events",
                "wayang.agent.hermes.runtime-event-journal-database-initialize-schema", "off"));

        assertThat(config.runtimeEventJournalJdbcTableName()).isEqualTo("legacy_runtime_events");
        assertThat(config.runtimeEventJournalJdbcInitializeSchema()).isFalse();
    }

    @Test
    void mapConfigSupportsShortRepairAliases() {
        HermesAgentModeConfig config = HermesAgentModeConfigs.fromMap(Map.ofEntries(
                Map.entry("wayang.agent.hermes.repairApprovalStore", "database"),
                Map.entry("wayang.agent.hermes.repairApprovalPath", "short/approval.jsonl"),
                Map.entry("wayang.agent.hermes.repairApprovalObjectPrefix", "short/approval-objects"),
                Map.entry("wayang.agent.hermes.repairApprovalJdbcTableName", "short_approval"),
                Map.entry("wayang.agent.hermes.repairApprovalJdbcInitializeSchema", "false"),
                Map.entry("wayang.agent.hermes.repairDispatchLedgerStore", "object-storage"),
                Map.entry("wayang.agent.hermes.repairDispatchLedgerPath", "short/dispatch.jsonl"),
                Map.entry("wayang.agent.hermes.repairDispatchLedgerObjectPrefix", "short/dispatch-objects"),
                Map.entry("wayang.agent.hermes.repairDispatchLedgerJdbcTableName", "short_dispatch"),
                Map.entry("wayang.agent.hermes.repairDispatchLedgerJdbcInitializeSchema", "off"),
                Map.entry("wayang.agent.hermes.repairDispatchLedgerMaxRecords", "44")));

        assertThat(config.skillLineageRepairApprovalStore()).isEqualTo("database");
        assertThat(config.skillLineageRepairApprovalPath()).isEqualTo("short/approval.jsonl");
        assertThat(config.skillLineageRepairApprovalObjectPrefix()).isEqualTo("short/approval-objects");
        assertThat(config.skillLineageRepairApprovalJdbcTableName()).isEqualTo("short_approval");
        assertThat(config.skillLineageRepairApprovalJdbcInitializeSchema()).isFalse();
        assertThat(config.skillLineageRepairDispatchLedgerStore()).isEqualTo("object-storage");
        assertThat(config.skillLineageRepairDispatchLedgerPath()).isEqualTo("short/dispatch.jsonl");
        assertThat(config.skillLineageRepairDispatchLedgerObjectPrefix()).isEqualTo("short/dispatch-objects");
        assertThat(config.skillLineageRepairDispatchLedgerJdbcTableName()).isEqualTo("short_dispatch");
        assertThat(config.skillLineageRepairDispatchLedgerJdbcInitializeSchema()).isFalse();
        assertThat(config.skillLineageRepairDispatchLedgerMaxRecords()).isEqualTo(44);
    }

    @Test
    void mapConfigSupportsLegacyRepairDatabaseAliases() {
        HermesAgentModeConfig config = HermesAgentModeConfigs.fromMap(Map.of(
                "wayang.agent.hermes.skill-lineage-repair-approval-database-table", "legacy_approval",
                "wayang.agent.hermes.skill-lineage-repair-approval-database-initialize-schema", "false",
                "wayang.agent.hermes.skill-lineage-repair-dispatch-ledger-database-table", "legacy_dispatch",
                "wayang.agent.hermes.skill-lineage-repair-dispatch-ledger-database-initialize-schema", "no"));

        assertThat(config.skillLineageRepairApprovalJdbcTableName()).isEqualTo("legacy_approval");
        assertThat(config.skillLineageRepairApprovalJdbcInitializeSchema()).isFalse();
        assertThat(config.skillLineageRepairDispatchLedgerJdbcTableName()).isEqualTo("legacy_dispatch");
        assertThat(config.skillLineageRepairDispatchLedgerJdbcInitializeSchema()).isFalse();
    }

    @Test
    void rejectsInvalidLearningThreshold() {
        assertThatThrownBy(() -> HermesAgentModeConfig.builder().minStepsToLearn(0).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("minStepsToLearn");
        assertThatThrownBy(() -> HermesAgentModeConfig.builder().maxSubAgents(0).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxSubAgents");
        assertThatThrownBy(() -> HermesAgentModeConfig.builder().runtimeEventJournalFormat("properties").build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("runtimeEventJournalFormat");
        assertThatThrownBy(() -> HermesAgentModeConfig.builder().runtimeEventJournalMaxEvents(0).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("runtimeEventJournalMaxEvents");
        assertThatThrownBy(() -> HermesAgentModeConfig.builder().runtimeEventJournalStore("queue").build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("runtimeEventJournalStore");
        assertThatThrownBy(() -> HermesAgentModeConfig.builder()
                .runtimeEventJournalJdbcTableName("runtime-events;drop")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("runtime event JDBC table");
        assertThatThrownBy(() -> HermesAgentModeConfig.builder()
                .skillLineageRepairDispatchLedgerStore("queue")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("skillLineageRepairDispatchLedgerStore");
        assertThatThrownBy(() -> HermesAgentModeConfig.builder()
                .skillLineageRepairApprovalStore("queue")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("skillLineageRepairApprovalStore");
        assertThatThrownBy(() -> HermesAgentModeConfig.builder()
                .skillLineageRepairApprovalJdbcTableName("approval-grants;drop")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("approval JDBC table");
        assertThatThrownBy(() -> HermesAgentModeConfig.builder()
                .skillLineageRepairDispatchLedgerJdbcTableName("dispatch-ledger;drop")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dispatch ledger JDBC table");
        assertThatThrownBy(() -> HermesAgentModeConfig.builder()
                .skillLineageRepairDispatchLedgerMaxRecords(0)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("skillLineageRepairDispatchLedgerMaxRecords");
        assertThatThrownBy(() -> HermesAgentModeConfig.builder()
                .skillLineageRemediationPolicy(new HermesSkillLineageRemediationPolicy(
                        "eager",
                        1,
                        List.of("all"),
                        List.of("catalog")))
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("skillLineageRemediationPolicy");
    }
}
