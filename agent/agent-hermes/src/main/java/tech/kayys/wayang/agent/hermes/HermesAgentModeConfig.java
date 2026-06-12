package tech.kayys.wayang.agent.hermes;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Configurable Hermes mode switches. Store selection stays outside this module
 * and is provided through skill-management factories.
 */
public record HermesAgentModeConfig(
        boolean persistentMemoryEnabled,
        boolean skillLearningEnabled,
        boolean skillSelfImprovementEnabled,
        boolean mcpEnabled,
        boolean gatewayEnabled,
        boolean cronEnabled,
        boolean subAgentsEnabled,
        boolean trajectoryExportEnabled,
        boolean preferLocalProviders,
        boolean requireToolCalling,
        int minStepsToLearn,
        int maxSkillProcedureSteps,
        int maxSubAgents,
        int memoryEntryLimit,
        String preferredProvider,
        String fallbackProvider,
        List<String> defaultToolsets,
        List<String> gatewayPlatforms,
        List<String> executionBackends,
        String runtimeAdapterProfile,
        boolean runtimeEventJournalEnabled,
        String runtimeEventJournalStore,
        String runtimeEventJournalPath,
        String runtimeEventJournalObjectPrefix,
        String runtimeEventJournalJdbcTableName,
        boolean runtimeEventJournalJdbcInitializeSchema,
        String runtimeEventJournalFormat,
        int runtimeEventJournalMaxEvents,
        HermesSkillLineageRemediationPolicy skillLineageRemediationPolicy,
        List<String> skillLineageRepairBackends,
        List<String> skillLineageRepairMutationBackends,
        String skillLineageRepairApprovalStore,
        String skillLineageRepairApprovalPath,
        String skillLineageRepairApprovalObjectPrefix,
        String skillLineageRepairApprovalJdbcTableName,
        boolean skillLineageRepairApprovalJdbcInitializeSchema,
        String skillLineageRepairDispatchLedgerStore,
        String skillLineageRepairDispatchLedgerPath,
        String skillLineageRepairDispatchLedgerObjectPrefix,
        String skillLineageRepairDispatchLedgerJdbcTableName,
        boolean skillLineageRepairDispatchLedgerJdbcInitializeSchema,
        int skillLineageRepairDispatchLedgerMaxRecords,
        Map<String, String> persistenceHints) {

    public HermesAgentModeConfig {
        if (minStepsToLearn < 1) {
            throw new IllegalArgumentException("minStepsToLearn must be positive");
        }
        if (maxSkillProcedureSteps < 1) {
            throw new IllegalArgumentException("maxSkillProcedureSteps must be positive");
        }
        if (maxSubAgents < 1) {
            throw new IllegalArgumentException("maxSubAgents must be positive");
        }
        if (memoryEntryLimit < 0) {
            throw new IllegalArgumentException("memoryEntryLimit cannot be negative");
        }
        preferredProvider = normalizeText(preferredProvider, "auto");
        fallbackProvider = normalizeText(fallbackProvider, "auto");
        defaultToolsets = copy(defaultToolsets);
        gatewayPlatforms = copy(gatewayPlatforms);
        executionBackends = copy(executionBackends);
        runtimeAdapterProfile = normalizeText(runtimeAdapterProfile, HermesRuntimeAdapterRegistry.DEFAULT_PROFILE);
        runtimeEventJournalStore = normalizeRuntimeEventJournalStore(runtimeEventJournalStore);
        runtimeEventJournalPath = normalizeText(runtimeEventJournalPath, "var/hermes/runtime-events.jsonl");
        runtimeEventJournalObjectPrefix = normalizeText(
                runtimeEventJournalObjectPrefix,
                ObjectStorageHermesRuntimeEventSink.DEFAULT_PREFIX);
        runtimeEventJournalJdbcTableName =
                DatabaseHermesRuntimeEventSink.normalizeTableName(runtimeEventJournalJdbcTableName);
        runtimeEventJournalFormat = normalizeText(runtimeEventJournalFormat, "jsonl").toLowerCase(Locale.ROOT);
        if (!runtimeEventJournalFormat.equals("jsonl")) {
            throw new IllegalArgumentException("runtimeEventJournalFormat must be jsonl");
        }
        if (runtimeEventJournalMaxEvents < 1) {
            throw new IllegalArgumentException("runtimeEventJournalMaxEvents must be positive");
        }
        skillLineageRemediationPolicy = skillLineageRemediationPolicy == null
                ? HermesSkillLineageRemediationPolicy.dryRun()
                : skillLineageRemediationPolicy;
        skillLineageRepairBackends = normalizeBackendList(
                skillLineageRepairBackends == null || skillLineageRepairBackends.isEmpty()
                        ? HermesSkillLineageRepairBackendRegistry.DEFAULT_BACKENDS
                        : skillLineageRepairBackends);
        skillLineageRepairMutationBackends = normalizeBackendList(skillLineageRepairMutationBackends);
        if (!skillLineageRepairMutationBackends.isEmpty()) {
            LinkedHashSet<String> values = new LinkedHashSet<>(skillLineageRepairBackends);
            values.addAll(skillLineageRepairMutationBackends);
            skillLineageRepairBackends = List.copyOf(values);
        }
        skillLineageRepairApprovalStore =
                normalizeSkillLineageRepairApprovalStore(skillLineageRepairApprovalStore);
        skillLineageRepairApprovalPath = normalizeText(
                skillLineageRepairApprovalPath,
                FileSystemHermesSkillLineageRepairApprovalStore.DEFAULT_PATH);
        skillLineageRepairApprovalObjectPrefix = normalizeText(
                skillLineageRepairApprovalObjectPrefix,
                ObjectStorageHermesSkillLineageRepairApprovalStore.DEFAULT_PREFIX);
        skillLineageRepairApprovalJdbcTableName =
                DatabaseHermesSkillLineageRepairApprovalStore.normalizeTableName(
                        skillLineageRepairApprovalJdbcTableName);
        skillLineageRepairDispatchLedgerStore =
                normalizeSkillLineageRepairDispatchLedgerStore(skillLineageRepairDispatchLedgerStore);
        skillLineageRepairDispatchLedgerPath = normalizeText(
                skillLineageRepairDispatchLedgerPath,
                HermesSkillLineageRepairAdapterDispatchLedgerResolver.DEFAULT_FILE_SYSTEM_PATH);
        skillLineageRepairDispatchLedgerObjectPrefix = normalizeText(
                skillLineageRepairDispatchLedgerObjectPrefix,
                ObjectStorageHermesSkillLineageRepairAdapterDispatchLedger.DEFAULT_PREFIX);
        skillLineageRepairDispatchLedgerJdbcTableName =
                DatabaseHermesSkillLineageRepairAdapterDispatchLedger.normalizeTableName(
                        skillLineageRepairDispatchLedgerJdbcTableName);
        if (skillLineageRepairDispatchLedgerMaxRecords < 1) {
            throw new IllegalArgumentException("skillLineageRepairDispatchLedgerMaxRecords must be positive");
        }
        persistenceHints = persistenceHints == null ? Map.of() : Map.copyOf(persistenceHints);
    }

    public static HermesAgentModeConfig defaults() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public Map<String, Object> toMetadata() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("persistentMemoryEnabled", persistentMemoryEnabled);
        metadata.put("skillLearningEnabled", skillLearningEnabled);
        metadata.put("skillSelfImprovementEnabled", skillSelfImprovementEnabled);
        metadata.put("mcpEnabled", mcpEnabled);
        metadata.put("gatewayEnabled", gatewayEnabled);
        metadata.put("cronEnabled", cronEnabled);
        metadata.put("subAgentsEnabled", subAgentsEnabled);
        metadata.put("trajectoryExportEnabled", trajectoryExportEnabled);
        metadata.put("preferLocalProviders", preferLocalProviders);
        metadata.put("requireToolCalling", requireToolCalling);
        metadata.put("minStepsToLearn", minStepsToLearn);
        metadata.put("maxSkillProcedureSteps", maxSkillProcedureSteps);
        metadata.put("maxSubAgents", maxSubAgents);
        metadata.put("memoryEntryLimit", memoryEntryLimit);
        metadata.put("preferredProvider", preferredProvider);
        metadata.put("fallbackProvider", fallbackProvider);
        metadata.put("defaultToolsets", defaultToolsets);
        metadata.put("gatewayPlatforms", gatewayPlatforms);
        metadata.put("executionBackends", executionBackends);
        metadata.put("runtimeAdapterProfile", runtimeAdapterProfile);
        metadata.put("runtimeEventJournalEnabled", runtimeEventJournalEnabled);
        metadata.put("runtimeEventJournalStore", runtimeEventJournalStore);
        metadata.put("runtimeEventJournalPath", runtimeEventJournalPath);
        metadata.put("runtimeEventJournalObjectPrefix", runtimeEventJournalObjectPrefix);
        metadata.put("runtimeEventJournalJdbcTableName", runtimeEventJournalJdbcTableName);
        metadata.put("runtimeEventJournalJdbcInitializeSchema", runtimeEventJournalJdbcInitializeSchema);
        metadata.put("runtimeEventJournalFormat", runtimeEventJournalFormat);
        metadata.put("runtimeEventJournalMaxEvents", runtimeEventJournalMaxEvents);
        metadata.put("runtimeEventJournal", HermesRuntimeEventSinkResolver.metadata(this));
        metadata.put("skillLineageRemediationPolicy", skillLineageRemediationPolicy.toMetadata());
        metadata.put("skillLineageRepairBackends", skillLineageRepairBackends);
        metadata.put("skillLineageRepairMutationBackends", skillLineageRepairMutationBackends);
        metadata.put("skillLineageRepairBackendRegistry", skillLineageRepairBackendRegistry().toMetadata());
        metadata.put("skillLineageRepairApprovalStore", skillLineageRepairApprovalStore);
        metadata.put("skillLineageRepairApprovalPath", skillLineageRepairApprovalPath);
        metadata.put("skillLineageRepairApprovalObjectPrefix", skillLineageRepairApprovalObjectPrefix);
        metadata.put("skillLineageRepairApprovalJdbcTableName", skillLineageRepairApprovalJdbcTableName);
        metadata.put(
                "skillLineageRepairApprovalJdbcInitializeSchema",
                skillLineageRepairApprovalJdbcInitializeSchema);
        metadata.put(
                "skillLineageRepairApproval",
                HermesSkillLineageRepairApprovalStoreResolver.metadata(this));
        metadata.put("skillLineageRepairDispatchLedgerStore", skillLineageRepairDispatchLedgerStore);
        metadata.put("skillLineageRepairDispatchLedgerPath", skillLineageRepairDispatchLedgerPath);
        metadata.put("skillLineageRepairDispatchLedgerObjectPrefix", skillLineageRepairDispatchLedgerObjectPrefix);
        metadata.put("skillLineageRepairDispatchLedgerJdbcTableName", skillLineageRepairDispatchLedgerJdbcTableName);
        metadata.put(
                "skillLineageRepairDispatchLedgerJdbcInitializeSchema",
                skillLineageRepairDispatchLedgerJdbcInitializeSchema);
        metadata.put("skillLineageRepairDispatchLedgerMaxRecords", skillLineageRepairDispatchLedgerMaxRecords);
        metadata.put(
                "skillLineageRepairDispatchLedger",
                HermesSkillLineageRepairAdapterDispatchLedgerResolver.metadata(this));
        metadata.put(
                "learningPromotionReceiptLedger",
                HermesLearningPromotionReceiptLedgerResolver.metadata(this));
        metadata.put("persistenceHints", persistenceHints);
        metadata.put("skillPersistenceStrategy", skillPersistenceStrategy().toMetadata());
        metadata.put("capabilities", runtimeCapabilities().toMetadata());
        return Map.copyOf(metadata);
    }

    public HermesRuntimeCapabilities runtimeCapabilities() {
        return HermesRuntimeCapabilities.from(this);
    }

    public HermesSkillPersistenceStrategy skillPersistenceStrategy() {
        return HermesSkillPersistenceStrategy.fromHints(persistenceHints);
    }

    public HermesSkillLineageRepairBackendRegistry skillLineageRepairBackendRegistry() {
        return HermesSkillLineageRepairBackendRegistry.from(
                skillLineageRepairBackends,
                skillLineageRepairMutationBackends);
    }

    private static List<String> copy(List<String> values) {
        return HermesText.trimmedList(values);
    }

    private static String normalizeText(String value, String fallback) {
        return HermesText.trimOr(value, fallback);
    }

    private static List<String> normalizeBackendList(List<String> values) {
        return values == null ? List.of() : List.copyOf(values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(HermesSkillLineageRepairBackend::normalizeBackendId)
                .distinct()
                .toList());
    }

    private static String normalizeRuntimeEventJournalStore(String value) {
        return HermesPersistenceStoreKind.runtimeEventJournal(value).configValue();
    }

    private static String normalizeSkillLineageRepairDispatchLedgerStore(String value) {
        return HermesPersistenceStoreKind.repairStore(
                value,
                "skillLineageRepairDispatchLedgerStore").configValue();
    }

    private static String normalizeSkillLineageRepairApprovalStore(String value) {
        return HermesPersistenceStoreKind.repairStore(
                value,
                "skillLineageRepairApprovalStore").configValue();
    }

    public static final class Builder {
        private boolean persistentMemoryEnabled = true;
        private boolean skillLearningEnabled = true;
        private boolean skillSelfImprovementEnabled = true;
        private boolean mcpEnabled = true;
        private boolean gatewayEnabled = true;
        private boolean cronEnabled = true;
        private boolean subAgentsEnabled = true;
        private boolean trajectoryExportEnabled = false;
        private boolean preferLocalProviders = false;
        private boolean requireToolCalling = true;
        private int minStepsToLearn = 3;
        private int maxSkillProcedureSteps = 8;
        private int maxSubAgents = 4;
        private int memoryEntryLimit = 8;
        private String preferredProvider = "auto";
        private String fallbackProvider = "auto";
        private List<String> defaultToolsets = new ArrayList<>(List.of(
                "terminal", "skills", "memory", "mcp", "rag"));
        private List<String> gatewayPlatforms = new ArrayList<>(List.of(
                "cli", "telegram", "discord", "slack", "whatsapp", "signal"));
        private List<String> executionBackends = new ArrayList<>(List.of(
                "local", "docker", "ssh", "daytona", "modal", "singularity"));
        private String runtimeAdapterProfile = HermesRuntimeAdapterRegistry.DEFAULT_PROFILE;
        private boolean runtimeEventJournalEnabled = false;
        private String runtimeEventJournalStore = "file-system";
        private String runtimeEventJournalPath = "var/hermes/runtime-events.jsonl";
        private String runtimeEventJournalObjectPrefix = ObjectStorageHermesRuntimeEventSink.DEFAULT_PREFIX;
        private String runtimeEventJournalJdbcTableName = DatabaseHermesRuntimeEventSink.DEFAULT_TABLE_NAME;
        private boolean runtimeEventJournalJdbcInitializeSchema = true;
        private String runtimeEventJournalFormat = "jsonl";
        private int runtimeEventJournalMaxEvents = FileSystemHermesRuntimeEventSink.DEFAULT_MAX_EVENTS;
        private HermesSkillLineageRemediationPolicy skillLineageRemediationPolicy =
                HermesSkillLineageRemediationPolicy.dryRun();
        private List<String> skillLineageRepairBackends =
                new ArrayList<>(HermesSkillLineageRepairBackendRegistry.DEFAULT_BACKENDS);
        private List<String> skillLineageRepairMutationBackends = new ArrayList<>();
        private String skillLineageRepairApprovalStore = "noop";
        private String skillLineageRepairApprovalPath =
                FileSystemHermesSkillLineageRepairApprovalStore.DEFAULT_PATH;
        private String skillLineageRepairApprovalObjectPrefix =
                ObjectStorageHermesSkillLineageRepairApprovalStore.DEFAULT_PREFIX;
        private String skillLineageRepairApprovalJdbcTableName =
                DatabaseHermesSkillLineageRepairApprovalStore.DEFAULT_TABLE_NAME;
        private boolean skillLineageRepairApprovalJdbcInitializeSchema = true;
        private String skillLineageRepairDispatchLedgerStore = "noop";
        private String skillLineageRepairDispatchLedgerPath =
                HermesSkillLineageRepairAdapterDispatchLedgerResolver.DEFAULT_FILE_SYSTEM_PATH;
        private String skillLineageRepairDispatchLedgerObjectPrefix =
                ObjectStorageHermesSkillLineageRepairAdapterDispatchLedger.DEFAULT_PREFIX;
        private String skillLineageRepairDispatchLedgerJdbcTableName =
                DatabaseHermesSkillLineageRepairAdapterDispatchLedger.DEFAULT_TABLE_NAME;
        private boolean skillLineageRepairDispatchLedgerJdbcInitializeSchema = true;
        private int skillLineageRepairDispatchLedgerMaxRecords =
                FileSystemHermesSkillLineageRepairAdapterDispatchLedger.DEFAULT_MAX_RECORDS;
        private Map<String, String> persistenceHints =
                new LinkedHashMap<>(HermesSkillPersistenceStrategy.defaultHints());

        public Builder persistentMemoryEnabled(boolean value) {
            this.persistentMemoryEnabled = value;
            return this;
        }

        public Builder skillLearningEnabled(boolean value) {
            this.skillLearningEnabled = value;
            return this;
        }

        public Builder skillSelfImprovementEnabled(boolean value) {
            this.skillSelfImprovementEnabled = value;
            return this;
        }

        public Builder mcpEnabled(boolean value) {
            this.mcpEnabled = value;
            return this;
        }

        public Builder gatewayEnabled(boolean value) {
            this.gatewayEnabled = value;
            return this;
        }

        public Builder cronEnabled(boolean value) {
            this.cronEnabled = value;
            return this;
        }

        public Builder subAgentsEnabled(boolean value) {
            this.subAgentsEnabled = value;
            return this;
        }

        public Builder trajectoryExportEnabled(boolean value) {
            this.trajectoryExportEnabled = value;
            return this;
        }

        public Builder preferLocalProviders(boolean value) {
            this.preferLocalProviders = value;
            return this;
        }

        public Builder requireToolCalling(boolean value) {
            this.requireToolCalling = value;
            return this;
        }

        public Builder minStepsToLearn(int value) {
            this.minStepsToLearn = value;
            return this;
        }

        public Builder maxSkillProcedureSteps(int value) {
            this.maxSkillProcedureSteps = value;
            return this;
        }

        public Builder maxSubAgents(int value) {
            this.maxSubAgents = value;
            return this;
        }

        public Builder memoryEntryLimit(int value) {
            this.memoryEntryLimit = value;
            return this;
        }

        public Builder preferredProvider(String value) {
            this.preferredProvider = value;
            return this;
        }

        public Builder fallbackProvider(String value) {
            this.fallbackProvider = value;
            return this;
        }

        public Builder defaultToolsets(List<String> values) {
            this.defaultToolsets = values == null ? new ArrayList<>() : new ArrayList<>(values);
            return this;
        }

        public Builder gatewayPlatforms(List<String> values) {
            this.gatewayPlatforms = values == null ? new ArrayList<>() : new ArrayList<>(values);
            return this;
        }

        public Builder executionBackends(List<String> values) {
            this.executionBackends = values == null ? new ArrayList<>() : new ArrayList<>(values);
            return this;
        }

        public Builder runtimeAdapterProfile(String value) {
            this.runtimeAdapterProfile = value;
            return this;
        }

        public Builder runtimeEventJournalEnabled(boolean value) {
            this.runtimeEventJournalEnabled = value;
            return this;
        }

        public Builder runtimeEventJournalStore(String value) {
            this.runtimeEventJournalStore = value;
            return this;
        }

        public Builder runtimeEventJournalPath(String value) {
            this.runtimeEventJournalPath = value;
            return this;
        }

        public Builder runtimeEventJournalObjectPrefix(String value) {
            this.runtimeEventJournalObjectPrefix = value;
            return this;
        }

        public Builder runtimeEventJournalJdbcTableName(String value) {
            this.runtimeEventJournalJdbcTableName = value;
            return this;
        }

        public Builder runtimeEventJournalJdbcInitializeSchema(boolean value) {
            this.runtimeEventJournalJdbcInitializeSchema = value;
            return this;
        }

        public Builder runtimeEventJournalFormat(String value) {
            this.runtimeEventJournalFormat = value;
            return this;
        }

        public Builder runtimeEventJournalMaxEvents(int value) {
            this.runtimeEventJournalMaxEvents = value;
            return this;
        }

        public Builder skillLineageRemediationPolicy(HermesSkillLineageRemediationPolicy value) {
            this.skillLineageRemediationPolicy = value == null
                    ? HermesSkillLineageRemediationPolicy.dryRun()
                    : value;
            return this;
        }

        public Builder skillLineageRepairBackends(List<String> values) {
            this.skillLineageRepairBackends = values == null ? new ArrayList<>() : new ArrayList<>(values);
            return this;
        }

        public Builder skillLineageRepairMutationBackends(List<String> values) {
            this.skillLineageRepairMutationBackends = values == null ? new ArrayList<>() : new ArrayList<>(values);
            return this;
        }

        public Builder skillLineageRepairApprovalStore(String value) {
            this.skillLineageRepairApprovalStore = value;
            return this;
        }

        public Builder skillLineageRepairApprovalPath(String value) {
            this.skillLineageRepairApprovalPath = value;
            return this;
        }

        public Builder skillLineageRepairApprovalObjectPrefix(String value) {
            this.skillLineageRepairApprovalObjectPrefix = value;
            return this;
        }

        public Builder skillLineageRepairApprovalJdbcTableName(String value) {
            this.skillLineageRepairApprovalJdbcTableName = value;
            return this;
        }

        public Builder skillLineageRepairApprovalJdbcInitializeSchema(boolean value) {
            this.skillLineageRepairApprovalJdbcInitializeSchema = value;
            return this;
        }

        public Builder skillLineageRepairDispatchLedgerStore(String value) {
            this.skillLineageRepairDispatchLedgerStore = value;
            return this;
        }

        public Builder skillLineageRepairDispatchLedgerPath(String value) {
            this.skillLineageRepairDispatchLedgerPath = value;
            return this;
        }

        public Builder skillLineageRepairDispatchLedgerObjectPrefix(String value) {
            this.skillLineageRepairDispatchLedgerObjectPrefix = value;
            return this;
        }

        public Builder skillLineageRepairDispatchLedgerJdbcTableName(String value) {
            this.skillLineageRepairDispatchLedgerJdbcTableName = value;
            return this;
        }

        public Builder skillLineageRepairDispatchLedgerJdbcInitializeSchema(boolean value) {
            this.skillLineageRepairDispatchLedgerJdbcInitializeSchema = value;
            return this;
        }

        public Builder skillLineageRepairDispatchLedgerMaxRecords(int value) {
            this.skillLineageRepairDispatchLedgerMaxRecords = value;
            return this;
        }

        public Builder persistenceHints(Map<String, String> values) {
            this.persistenceHints = values == null ? new LinkedHashMap<>() : new LinkedHashMap<>(values);
            return this;
        }

        public HermesAgentModeConfig build() {
            return new HermesAgentModeConfig(
                    persistentMemoryEnabled,
                    skillLearningEnabled,
                    skillSelfImprovementEnabled,
                    mcpEnabled,
                    gatewayEnabled,
                    cronEnabled,
                    subAgentsEnabled,
                    trajectoryExportEnabled,
                    preferLocalProviders,
                    requireToolCalling,
                    minStepsToLearn,
                    maxSkillProcedureSteps,
                    maxSubAgents,
                    memoryEntryLimit,
                    preferredProvider,
                    fallbackProvider,
                    defaultToolsets,
                    gatewayPlatforms,
                    executionBackends,
                    runtimeAdapterProfile,
                    runtimeEventJournalEnabled,
                    runtimeEventJournalStore,
                    runtimeEventJournalPath,
                    runtimeEventJournalObjectPrefix,
                    runtimeEventJournalJdbcTableName,
                    runtimeEventJournalJdbcInitializeSchema,
                    runtimeEventJournalFormat,
                    runtimeEventJournalMaxEvents,
                    skillLineageRemediationPolicy,
                    skillLineageRepairBackends,
                    skillLineageRepairMutationBackends,
                    skillLineageRepairApprovalStore,
                    skillLineageRepairApprovalPath,
                    skillLineageRepairApprovalObjectPrefix,
                    skillLineageRepairApprovalJdbcTableName,
                    skillLineageRepairApprovalJdbcInitializeSchema,
                    skillLineageRepairDispatchLedgerStore,
                    skillLineageRepairDispatchLedgerPath,
                    skillLineageRepairDispatchLedgerObjectPrefix,
                    skillLineageRepairDispatchLedgerJdbcTableName,
                    skillLineageRepairDispatchLedgerJdbcInitializeSchema,
                    skillLineageRepairDispatchLedgerMaxRecords,
                    persistenceHints);
        }
    }
}
