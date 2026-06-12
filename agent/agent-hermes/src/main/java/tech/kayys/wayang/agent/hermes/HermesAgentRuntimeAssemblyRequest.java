package tech.kayys.wayang.agent.hermes;

import tech.kayys.wayang.agent.skills.management.SkillManagementService;
import tech.kayys.wayang.agent.spi.InferenceBackend;

import java.util.Objects;
import java.util.Optional;

/**
 * Immutable request describing the dependencies needed to assemble Hermes mode.
 */
record HermesAgentRuntimeAssemblyRequest(
        InferenceBackend inferenceBackend,
        SkillManagementService skillManagementService,
        HermesMemorySnapshotProvider memorySnapshotProvider,
        HermesAgentModeConfig config,
        HermesPersistenceResources resources,
        HermesRuntimePortContributions portContributions,
        Optional<HermesRuntimeEventSink> runtimeEventSink) {

    HermesAgentRuntimeAssemblyRequest {
        inferenceBackend = Objects.requireNonNull(inferenceBackend, "inferenceBackend");
        skillManagementService = Objects.requireNonNull(skillManagementService, "skillManagementService");
        memorySnapshotProvider = memorySnapshotProvider == null
                ? HermesMemorySnapshotProvider.none()
                : memorySnapshotProvider;
        config = config == null ? HermesAgentModeConfig.defaults() : config;
        resources = resources == null ? HermesPersistenceResources.empty() : resources;
        portContributions = portContributions == null
                ? HermesRuntimePortContributions.empty()
                : portContributions;
        runtimeEventSink = HermesOptionals.orEmpty(runtimeEventSink);
    }

    static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent builder for composing Hermes runtime assembly requests.
     */
    static final class Builder {
        private InferenceBackend inferenceBackend;
        private SkillManagementService skillManagementService;
        private HermesMemorySnapshotProvider memorySnapshotProvider;
        private HermesAgentModeConfig config;
        private HermesPersistenceResources resources;
        private HermesRuntimePortContributions portContributions;
        private Optional<HermesRuntimeEventSink> runtimeEventSink = Optional.empty();

        private Builder() {
        }

        Builder inferenceBackend(InferenceBackend inferenceBackend) {
            this.inferenceBackend = inferenceBackend;
            return this;
        }

        Builder skillManagementService(SkillManagementService skillManagementService) {
            this.skillManagementService = skillManagementService;
            return this;
        }

        Builder memorySnapshotProvider(HermesMemorySnapshotProvider memorySnapshotProvider) {
            this.memorySnapshotProvider = memorySnapshotProvider;
            return this;
        }

        Builder config(HermesAgentModeConfig config) {
            this.config = config;
            return this;
        }

        Builder resources(HermesPersistenceResources resources) {
            this.resources = resources;
            return this;
        }

        Builder portContributions(HermesRuntimePortContributions portContributions) {
            this.portContributions = portContributions;
            return this;
        }

        Builder runtimeEventSink(Optional<HermesRuntimeEventSink> runtimeEventSink) {
            this.runtimeEventSink = HermesOptionals.orEmpty(runtimeEventSink);
            return this;
        }

        HermesAgentRuntimeAssemblyRequest build() {
            return new HermesAgentRuntimeAssemblyRequest(
                    inferenceBackend,
                    skillManagementService,
                    memorySnapshotProvider,
                    config,
                    resources,
                    portContributions,
                    runtimeEventSink);
        }
    }
}
