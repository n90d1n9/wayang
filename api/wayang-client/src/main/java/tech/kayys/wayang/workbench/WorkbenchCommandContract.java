package tech.kayys.wayang.workbench;

import tech.kayys.wayang.agent.lifecycle.AgentRunLifecycleContract;
import tech.kayys.wayang.agent.planner.AgentRunPlanningContract;
import tech.kayys.wayang.alignment.WayangStandardAlignmentContract;
import tech.kayys.wayang.capability.WayangProviderCapabilityContract;
import tech.kayys.wayang.catalog.WayangStandardCatalogContract;
import tech.kayys.wayang.command.WayangCommandDiscoveryContract;
import tech.kayys.wayang.contract.WayangContractCoverageContract;
import tech.kayys.wayang.contract.WayangContractKey;
import tech.kayys.wayang.readiness.WayangReadinessContract;
import tech.kayys.wayang.skill.WayangSkillContract;
import tech.kayys.wayang.client.SdkText;
import tech.kayys.wayang.client.WayangPlatformContract;

public record WorkbenchCommandContract(
        String schema,
        int version,
        String envelope,
        String jsonSchemaId) {

    public WorkbenchCommandContract {
        schema = normalizeRequired("Contract schema", schema);
        version = Math.max(1, version);
        envelope = normalizeRequired("Contract envelope", envelope);
        jsonSchemaId = normalizeDefault(jsonSchemaId, defaultJsonSchemaId(schema, version, envelope));
    }

    public WorkbenchCommandContract(String schema, int version, String envelope) {
        this(schema, version, envelope, null);
    }

    public static WorkbenchCommandContract of(String schema, int version, String envelope) {
        return new WorkbenchCommandContract(schema, version, envelope);
    }

    public static WorkbenchCommandContract of(String schema, int version, String envelope, String jsonSchemaId) {
        return new WorkbenchCommandContract(schema, version, envelope, jsonSchemaId);
    }

    public static WorkbenchCommandContract lifecycle(String envelope) {
        return of(AgentRunLifecycleContract.SCHEMA, AgentRunLifecycleContract.VERSION, envelope);
    }

    public static WorkbenchCommandContract planning(String envelope) {
        return of(AgentRunPlanningContract.SCHEMA, AgentRunPlanningContract.VERSION, envelope);
    }

    public static WorkbenchCommandContract readiness(String envelope) {
        return of(WayangReadinessContract.SCHEMA, WayangReadinessContract.VERSION, envelope);
    }

    public static WorkbenchCommandContract commandDiscovery(String envelope) {
        return of(WayangCommandDiscoveryContract.SCHEMA, WayangCommandDiscoveryContract.VERSION, envelope);
    }

    public static WorkbenchCommandContract workbenchDiscovery(String envelope) {
        return of(WayangWorkbenchContract.SCHEMA, WayangWorkbenchContract.VERSION, envelope);
    }

    public static WorkbenchCommandContract platform(String envelope) {
        return of(WayangPlatformContract.SCHEMA, WayangPlatformContract.VERSION, envelope);
    }

    public static WorkbenchCommandContract skill(String envelope) {
        return of(WayangSkillContract.SCHEMA, WayangSkillContract.VERSION, envelope);
    }

    public static WorkbenchCommandContract providerCapability(String envelope) {
        return of(WayangProviderCapabilityContract.SCHEMA, WayangProviderCapabilityContract.VERSION, envelope);
    }

    public static WorkbenchCommandContract contractCoverage(String envelope) {
        return of(WayangContractCoverageContract.SCHEMA, WayangContractCoverageContract.VERSION, envelope);
    }

    public static WorkbenchCommandContract standardCatalog(String envelope) {
        return of(WayangStandardCatalogContract.SCHEMA, WayangStandardCatalogContract.VERSION, envelope);
    }

    public static WorkbenchCommandContract standardAlignment(String envelope) {
        return of(WayangStandardAlignmentContract.SCHEMA, WayangStandardAlignmentContract.VERSION, envelope);
    }

    public WayangContractKey key() {
        return WayangContractKey.from(this);
    }

    private static String normalizeRequired(String label, String value) {
        String normalized = SdkText.trimToEmpty(value);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(label + " is required.");
        }
        return normalized;
    }

    private static String normalizeDefault(String value, String fallback) {
        String normalized = SdkText.trimToEmpty(value);
        return normalized.isEmpty() ? fallback : normalized;
    }

    private static String defaultJsonSchemaId(String schema, int version, String envelope) {
        return WayangContractKey.of(schema, version, envelope).jsonSchemaId();
    }
}
