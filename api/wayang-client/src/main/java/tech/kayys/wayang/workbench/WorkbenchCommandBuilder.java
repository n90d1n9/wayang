package tech.kayys.wayang.workbench;

import java.util.List;

import tech.kayys.wayang.agent.lifecycle.AgentRunLifecycleContract;
import tech.kayys.wayang.agent.planner.AgentRunPlanningContract;
import tech.kayys.wayang.alignment.WayangStandardAlignmentContract;
import tech.kayys.wayang.capability.WayangProviderCapabilityContract;
import tech.kayys.wayang.catalog.WayangStandardCatalogContract;
import tech.kayys.wayang.client.WayangPlatformContract;
import tech.kayys.wayang.command.WayangCommandDiscoveryContract;
import tech.kayys.wayang.contract.WayangContractCoverageContract;
import tech.kayys.wayang.readiness.WayangReadinessContract;
import tech.kayys.wayang.skill.WayangSkillContract;

/**
 * Builder helper for creating WorkbenchCommand instances with contracts.
 * Provides static factory methods for creating commands with various contract types.
 */
public final class WorkbenchCommandBuilder {

    private WorkbenchCommandBuilder() {
    }

    // Command creation methods

    static WorkbenchCommand command(
            String id,
            String title,
            String command,
            String category,
            String description,
            String... surfaceIds) {
        return command(id, title, command, category, description, List.of(), surfaceIds);
    }

    static WorkbenchCommand command(
            String id,
            String title,
            String command,
            String category,
            String description,
            List<WorkbenchCommandContract> contracts,
            String... surfaceIds) {
        return WorkbenchCommand.shared(id, title, command, category, description, List.of(surfaceIds), contracts);
    }

    // Contract building methods

    static List<WorkbenchCommandContract> lifecycle(String... envelopes) {
        return List.of(envelopes).stream()
                .map(WorkbenchCommandContract::lifecycle)
                .toList();
    }

    static List<WorkbenchCommandContract> planning(String... envelopes) {
        return List.of(envelopes).stream()
                .map(WorkbenchCommandContract::planning)
                .toList();
    }

    static List<WorkbenchCommandContract> readiness(String... envelopes) {
        return List.of(envelopes).stream()
                .map(WorkbenchCommandContract::readiness)
                .toList();
    }

    static List<WorkbenchCommandContract> platform(String... envelopes) {
        return List.of(envelopes).stream()
                .map(WorkbenchCommandContract::platform)
                .toList();
    }

    static List<WorkbenchCommandContract> skill(String... envelopes) {
        return List.of(envelopes).stream()
                .map(WorkbenchCommandContract::skill)
                .toList();
    }

    static List<WorkbenchCommandContract> providerCapability(String... envelopes) {
        return List.of(envelopes).stream()
                .map(WorkbenchCommandContract::providerCapability)
                .toList();
    }

    static List<WorkbenchCommandContract> contractCoverage(String... envelopes) {
        return List.of(envelopes).stream()
                .map(WorkbenchCommandContract::contractCoverage)
                .toList();
    }

    static List<WorkbenchCommandContract> standardCatalog(String... envelopes) {
        return List.of(envelopes).stream()
                .map(WorkbenchCommandContract::standardCatalog)
                .toList();
    }

    static List<WorkbenchCommandContract> standardAlignment(String... envelopes) {
        return List.of(envelopes).stream()
                .map(WorkbenchCommandContract::standardAlignment)
                .toList();
    }

    static List<WorkbenchCommandContract> commandDiscovery(String... envelopes) {
        return List.of(envelopes).stream()
                .map(WorkbenchCommandContract::commandDiscovery)
                .toList();
    }

    static List<WorkbenchCommandContract> workbenchDiscovery(String... envelopes) {
        return List.of(envelopes).stream()
                .map(WorkbenchCommandContract::workbenchDiscovery)
                .toList();
    }
}
