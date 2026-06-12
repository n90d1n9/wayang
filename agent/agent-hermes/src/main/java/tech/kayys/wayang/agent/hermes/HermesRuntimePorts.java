package tech.kayys.wayang.agent.hermes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Configurable runtime adapter bundle for Hermes directives.
 */
public record HermesRuntimePorts(
        HermesExecutionPort executionPort,
        HermesGatewayPort gatewayPort,
        HermesAutomationPort automationPort,
        HermesDelegationPort delegationPort,
        HermesProviderRoutingPort providerRoutingPort,
        HermesMemoryReflectionPort memoryReflectionPort,
        HermesTrajectoryExportPort trajectoryExportPort,
        HermesSkillPersistencePort skillPersistencePort,
        HermesRuntimeJournalPort runtimeJournalPort,
        HermesLearningAuditPort learningAuditPort,
        HermesSkillLineagePort skillLineagePort) {

    public HermesRuntimePorts {
        executionPort = executionPort == null ? HermesExecutionPort.noop() : executionPort;
        gatewayPort = gatewayPort == null ? HermesGatewayPort.noop() : gatewayPort;
        automationPort = automationPort == null ? HermesAutomationPort.noop() : automationPort;
        delegationPort = delegationPort == null ? HermesDelegationPort.noop() : delegationPort;
        providerRoutingPort = providerRoutingPort == null ? HermesProviderRoutingPort.noop() : providerRoutingPort;
        memoryReflectionPort = memoryReflectionPort == null ? HermesMemoryReflectionPort.noop() : memoryReflectionPort;
        trajectoryExportPort = trajectoryExportPort == null ? HermesTrajectoryExportPort.noop() : trajectoryExportPort;
        skillPersistencePort = skillPersistencePort == null ? HermesSkillPersistencePort.noop() : skillPersistencePort;
        runtimeJournalPort = runtimeJournalPort == null ? HermesRuntimeJournalPort.noop() : runtimeJournalPort;
        learningAuditPort = learningAuditPort == null ? HermesLearningAuditPort.noop() : learningAuditPort;
        skillLineagePort = skillLineagePort == null ? HermesSkillLineagePort.noop() : skillLineagePort;
    }

    public HermesRuntimePorts(
            HermesExecutionPort executionPort,
            HermesGatewayPort gatewayPort,
            HermesAutomationPort automationPort,
            HermesDelegationPort delegationPort,
            HermesProviderRoutingPort providerRoutingPort,
            HermesMemoryReflectionPort memoryReflectionPort,
            HermesTrajectoryExportPort trajectoryExportPort,
            HermesSkillPersistencePort skillPersistencePort,
            HermesRuntimeJournalPort runtimeJournalPort) {
        this(
                executionPort,
                gatewayPort,
                automationPort,
                delegationPort,
                providerRoutingPort,
                memoryReflectionPort,
                trajectoryExportPort,
                skillPersistencePort,
                runtimeJournalPort,
                null,
                null);
    }

    public HermesRuntimePorts(
            HermesExecutionPort executionPort,
            HermesGatewayPort gatewayPort,
            HermesAutomationPort automationPort,
            HermesDelegationPort delegationPort,
            HermesProviderRoutingPort providerRoutingPort,
            HermesMemoryReflectionPort memoryReflectionPort,
            HermesTrajectoryExportPort trajectoryExportPort,
            HermesSkillPersistencePort skillPersistencePort,
            HermesRuntimeJournalPort runtimeJournalPort,
            HermesSkillLineagePort skillLineagePort) {
        this(
                executionPort,
                gatewayPort,
                automationPort,
                delegationPort,
                providerRoutingPort,
                memoryReflectionPort,
                trajectoryExportPort,
                skillPersistencePort,
                runtimeJournalPort,
                null,
                skillLineagePort);
    }

    public HermesRuntimePorts(
            HermesExecutionPort executionPort,
            HermesGatewayPort gatewayPort,
            HermesAutomationPort automationPort,
            HermesDelegationPort delegationPort,
            HermesProviderRoutingPort providerRoutingPort,
            HermesMemoryReflectionPort memoryReflectionPort,
            HermesTrajectoryExportPort trajectoryExportPort,
            HermesSkillPersistencePort skillPersistencePort) {
        this(
                executionPort,
                gatewayPort,
                automationPort,
                delegationPort,
                providerRoutingPort,
                memoryReflectionPort,
                trajectoryExportPort,
                skillPersistencePort,
                null,
                null,
                null);
    }

    public static HermesRuntimePorts noop() {
        return new HermesRuntimePorts(null, null, null, null, null, null, null, null, null, null, null);
    }

    public List<HermesRuntimePortDescriptor> descriptors() {
        return List.of(
                executionPort.descriptor(),
                gatewayPort.descriptor(),
                automationPort.descriptor(),
                delegationPort.descriptor(),
                providerRoutingPort.descriptor(),
                memoryReflectionPort.descriptor(),
                trajectoryExportPort.descriptor(),
                skillPersistencePort.descriptor(),
                runtimeJournalPort.descriptor(),
                learningAuditPort.descriptor(),
                skillLineagePort.descriptor());
    }

    public Map<String, Object> toMetadata() {
        List<HermesRuntimePortDescriptor> values = descriptors();
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("configuredCount", values.stream().filter(HermesRuntimePortDescriptor::configured).count());
        metadata.put("readyCount", values.stream().filter(HermesRuntimePortDescriptor::ready).count());
        metadata.put("noopCount", values.stream().filter(HermesRuntimePortDescriptor::noop).count());
        metadata.put("ports", values.stream()
                .map(HermesRuntimePortDescriptor::toMetadata)
                .toList());
        return Map.copyOf(metadata);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private HermesExecutionPort executionPort;
        private HermesGatewayPort gatewayPort;
        private HermesAutomationPort automationPort;
        private HermesDelegationPort delegationPort;
        private HermesProviderRoutingPort providerRoutingPort;
        private HermesMemoryReflectionPort memoryReflectionPort;
        private HermesTrajectoryExportPort trajectoryExportPort;
        private HermesSkillPersistencePort skillPersistencePort;
        private HermesRuntimeJournalPort runtimeJournalPort;
        private HermesLearningAuditPort learningAuditPort;
        private HermesSkillLineagePort skillLineagePort;

        public Builder executionPort(HermesExecutionPort value) {
            this.executionPort = value;
            return this;
        }

        public Builder gatewayPort(HermesGatewayPort value) {
            this.gatewayPort = value;
            return this;
        }

        public Builder automationPort(HermesAutomationPort value) {
            this.automationPort = value;
            return this;
        }

        public Builder delegationPort(HermesDelegationPort value) {
            this.delegationPort = value;
            return this;
        }

        public Builder providerRoutingPort(HermesProviderRoutingPort value) {
            this.providerRoutingPort = value;
            return this;
        }

        public Builder memoryReflectionPort(HermesMemoryReflectionPort value) {
            this.memoryReflectionPort = value;
            return this;
        }

        public Builder trajectoryExportPort(HermesTrajectoryExportPort value) {
            this.trajectoryExportPort = value;
            return this;
        }

        public Builder skillPersistencePort(HermesSkillPersistencePort value) {
            this.skillPersistencePort = value;
            return this;
        }

        public Builder runtimeJournalPort(HermesRuntimeJournalPort value) {
            this.runtimeJournalPort = value;
            return this;
        }

        public Builder learningAuditPort(HermesLearningAuditPort value) {
            this.learningAuditPort = value;
            return this;
        }

        public Builder skillLineagePort(HermesSkillLineagePort value) {
            this.skillLineagePort = value;
            return this;
        }

        public HermesRuntimePorts build() {
            return new HermesRuntimePorts(
                    executionPort,
                    gatewayPort,
                    automationPort,
                    delegationPort,
                    providerRoutingPort,
                    memoryReflectionPort,
                    trajectoryExportPort,
                    skillPersistencePort,
                    runtimeJournalPort,
                    learningAuditPort,
                    skillLineagePort);
        }
    }
}
