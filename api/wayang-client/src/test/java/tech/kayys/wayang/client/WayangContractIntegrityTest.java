package tech.kayys.wayang.client;

import org.junit.jupiter.api.Test;

import tech.kayys.wayang.agent.lifecycle.AgentRunLifecycleContract;
import tech.kayys.wayang.workbench.WorkbenchCommand;
import tech.kayys.wayang.workbench.WorkbenchCommandContract;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WayangContractIntegrityTest {

    @Test
    void validatesDefaultContractAndCommandCatalogLinks() {
        List<WayangContractDescriptor> contracts = WayangContractCatalog.defaultContracts();
        List<WorkbenchCommand> commands = WayangWorkbenchCatalog.localCommands();

        WayangContractIntegrityReport report = WayangContractIntegrity.validateDefault();

        assertThat(report.valid()).isTrue();
        assertThat(report.issueCount()).isZero();
        assertThat(report.totalContracts()).isEqualTo(contracts.size());
        assertThat(report.totalCommands()).isEqualTo(commands.size());
        assertThat(report.contractCommandLinks()).isEqualTo(contractCommandLinkCount(contracts));
        assertThat(report.commandContractLinks()).isEqualTo(commandContractLinkCount(commands));
        assertThat(report.contractCommandLinks()).isEqualTo(report.commandContractLinks());
        assertThat(WayangGollekSdk.local().contractIntegrity().valid()).isTrue();
    }

    @Test
    void reportsContractCommandIdsThatDoNotExist() {
        WayangContractDescriptor contract = new WayangContractDescriptor(
                AgentRunLifecycleContract.SCHEMA,
                AgentRunLifecycleContract.VERSION,
                "run-status",
                "lifecycle",
                "Status",
                List.of("missing-command"),
                List.of("run status <run-id> --json"));

        WayangContractIntegrityReport report = WayangContractIntegrity.validate(List.of(contract), List.of());

        assertThat(report.valid()).isFalse();
        assertThat(report.issues())
                .singleElement()
                .satisfies(issue -> {
                    assertThat(issue.kind()).isEqualTo("missing-command");
                    assertThat(issue.envelope()).isEqualTo("run-status");
                    assertThat(issue.commandId()).isEqualTo("missing-command");
                });
    }

    @Test
    void reportsDuplicateContractDescriptors() {
        WayangContractDescriptor first = WayangContractDescriptors.lifecycle(
                AgentRunLifecycleContract.RUN_STATUS,
                "Status one",
                List.of(),
                "run status <run-id> --json");
        WayangContractDescriptor duplicate = WayangContractDescriptors.lifecycle(
                AgentRunLifecycleContract.RUN_STATUS,
                "Status two",
                List.of(),
                "run status <run-id> --json");

        WayangContractIntegrityReport report =
                WayangContractIntegrity.validate(List.of(first, duplicate), List.of());

        assertThat(report.valid()).isFalse();
        assertThat(report.issues())
                .singleElement()
                .satisfies(issue -> {
                    assertThat(issue.kind()).isEqualTo("duplicate-contract");
                    assertThat(issue.envelope()).isEqualTo(AgentRunLifecycleContract.RUN_STATUS);
                    assertThat(issue.commandId()).isEmpty();
                });
    }

    @Test
    void reportsCommandsThatDoNotLinkBackToDescriptors() {
        WayangContractDescriptor contract = new WayangContractDescriptor(
                AgentRunLifecycleContract.SCHEMA,
                AgentRunLifecycleContract.VERSION,
                "run-status",
                "lifecycle",
                "Status",
                List.of(),
                List.of("run status <run-id> --json"));
        WorkbenchCommand command = WorkbenchCommand.shared(
                "run-status-json",
                "Run Status JSON",
                "run status <run-id> --json",
                "Runs",
                "Render status.",
                List.of(),
                List.of(WorkbenchCommandContract.lifecycle("run-status")));

        WayangContractIntegrityReport report = WayangContractIntegrity.validate(List.of(contract), List.of(command));

        assertThat(report.valid()).isFalse();
        assertThat(report.issues())
                .singleElement()
                .satisfies(issue -> {
                    assertThat(issue.kind()).isEqualTo("missing-contract-command-id");
                    assertThat(issue.envelope()).isEqualTo("run-status");
                    assertThat(issue.commandId()).isEqualTo("run-status-json");
                });
    }

    private static int contractCommandLinkCount(List<WayangContractDescriptor> contracts) {
        return contracts.stream()
                .mapToInt(contract -> contract.commandIds().size())
                .sum();
    }

    private static int commandContractLinkCount(List<WorkbenchCommand> commands) {
        return commands.stream()
                .mapToInt(command -> command.contracts().size())
                .sum();
    }
}
