package tech.kayys.wayang.client;

import org.junit.jupiter.api.Test;

import tech.kayys.wayang.agent.lifecycle.AgentRunLifecycleContract;
import tech.kayys.wayang.agent.planner.AgentRunPlanningContract;
import tech.kayys.wayang.workbench.WorkbenchCommand;
import tech.kayys.wayang.workbench.WorkbenchCommandCategorySummary;
import tech.kayys.wayang.workbench.WorkbenchCommandContract;
import tech.kayys.wayang.workbench.WorkbenchCommandIndex;
import tech.kayys.wayang.workbench.WorkbenchCommandQuery;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkbenchCommandIndexTest {

    @Test
    void indexesCommandsByIdCategorySurfaceAndContract() {
        WorkbenchCommand runDry = command(
                "run-dry-json",
                "Runs",
                List.of("coding-agent"),
                WorkbenchCommandContract.planning(AgentRunPlanningContract.RUN_PREVIEW));
        WorkbenchCommand runSpecDry = command(
                "run-spec-dry-json",
                "Run Specs",
                List.of(),
                WorkbenchCommandContract.planning(AgentRunPlanningContract.RUN_PREVIEW));
        WorkbenchCommand status = command(
                "status-json",
                "Platform",
                List.of(),
                WorkbenchCommandContract.platform(WayangPlatformContract.PLATFORM_STATUS));
        WorkbenchCommandIndex index = WorkbenchCommandIndex.of(List.of(runDry, runSpecDry, status));
        WayangContractKey previewKey = WayangContractKey.of(
                AgentRunPlanningContract.SCHEMA,
                AgentRunPlanningContract.VERSION,
                AgentRunPlanningContract.RUN_PREVIEW);

        assertThat(index.commands()).containsExactly(runDry, runSpecDry, status);
        assertThat(index.commandIds()).containsExactly("run-dry-json", "run-spec-dry-json", "status-json");
        assertThat(index.categories()).containsExactly("Runs", "Run Specs", "Platform");
        assertThat(index.categoryCounts())
                .containsExactly(
                        java.util.Map.entry("Runs", 1),
                        java.util.Map.entry("Run Specs", 1),
                        java.util.Map.entry("Platform", 1));
        assertThat(index.categorySummaries())
                .extracting(WorkbenchCommandCategorySummary::name)
                .containsExactly("Runs", "Run Specs", "Platform");
        assertThat(index.findCommand("run-dry-json")).hasValue(runDry);
        assertThat(index.commandsForId("run-spec-dry-json")).containsExactly(runSpecDry);
        assertThat(index.commandsForCategory("run specs")).containsExactly(runSpecDry);
        assertThat(index.commandsForSurface("coding-agent"))
                .containsExactly(runDry, runSpecDry, status);
        assertThat(index.commandsForSurface("assistant-agent"))
                .containsExactly(runSpecDry, status);
        assertThat(index.commandsForContractKey(previewKey))
                .containsExactly(runDry, runSpecDry);
        assertThat(index.commandsForContractJsonSchemaId(previewKey.jsonSchemaId()))
                .containsExactly(runDry, runSpecDry);
        assertThat(index.contractKeys())
                .containsExactly(previewKey, WayangContractKey.from(status.contracts().get(0)));
        assertThat(index.contractKeyCounts())
                .containsEntry(previewKey, 2)
                .containsEntry(WayangContractKey.from(status.contracts().get(0)), 1);
        assertThat(index.contractJsonSchemaIds())
                .containsExactly(
                        "urn:wayang:contract:wayang.run.planning:v1:run-preview",
                        "urn:wayang:contract:wayang.platform.catalog:v1:platform-status");
        assertThat(index.contractJsonSchemaIdCounts())
                .containsEntry("urn:wayang:contract:wayang.run.planning:v1:run-preview", 2)
                .containsEntry("urn:wayang:contract:wayang.platform.catalog:v1:platform-status", 1);
        assertThat(index.commandIdsForContractKey(previewKey))
                .containsExactly("run-dry-json", "run-spec-dry-json");
        assertThat(index.contractSummaryByJsonSchemaId(" " + previewKey.jsonSchemaId() + " "))
                .hasValueSatisfying(summary -> {
                    assertThat(summary.key()).isEqualTo(previewKey);
                    assertThat(summary.commandIds()).containsExactly("run-dry-json", "run-spec-dry-json");
                });
    }

    @Test
    void preservesExactMatchingForCustomContractSchemaIds() {
        WorkbenchCommandContract contract = WorkbenchCommandContract.of(
                "custom.contract",
                1,
                "custom-envelope",
                "custom-schema-id");
        WorkbenchCommand command = command("custom-json", "Custom", List.of(), contract);
        WorkbenchCommandIndex index = WorkbenchCommandIndex.of(List.of(command));

        assertThat(index.commandsForContractJsonSchemaId("custom-schema-id")).containsExactly(command);
        assertThat(index.contractSummaryByJsonSchemaId(" custom-schema-id "))
                .hasValueSatisfying(summary -> {
                    assertThat(summary.key()).isEqualTo(contract.key());
                    assertThat(summary.jsonSchemaId()).isEqualTo("custom-schema-id");
                });
        assertThat(index.commandsForContractJsonSchemaId(contract.key().jsonSchemaId())).containsExactly(command);
    }

    @Test
    void filtersCommandsForComposedQueries() {
        WorkbenchCommand runDry = command(
                "run-dry-json",
                "Runs",
                List.of("coding-agent"),
                WorkbenchCommandContract.planning(AgentRunPlanningContract.RUN_PREVIEW));
        WorkbenchCommand runSpecDry = command(
                "run-spec-dry-json",
                "Run Specs",
                List.of(),
                WorkbenchCommandContract.planning(AgentRunPlanningContract.RUN_PREVIEW));
        WorkbenchCommand runStatus = command(
                "run-status-json",
                "Runs",
                List.of("assistant-agent"),
                WorkbenchCommandContract.lifecycle(AgentRunLifecycleContract.RUN_STATUS));
        WorkbenchCommandIndex index = WorkbenchCommandIndex.of(List.of(runDry, runSpecDry, runStatus));
        WayangContractKey previewKey = WayangContractKey.of(
                AgentRunPlanningContract.SCHEMA,
                AgentRunPlanningContract.VERSION,
                AgentRunPlanningContract.RUN_PREVIEW);

        assertThat(index.commandsForQuery(null))
                .containsExactly(runDry, runSpecDry, runStatus);
        assertThat(index.commandsForQuery(WorkbenchCommandQuery.forSurface(" coding-agent ")))
                .containsExactly(runDry, runSpecDry);
        assertThat(index.commandsForQuery(WorkbenchCommandQuery.forCategory(" runs ")))
                .containsExactly(runDry, runStatus);
        assertThat(index.commandsForQuery(WorkbenchCommandQuery.forCommandId(" run-status-json ")))
                .containsExactly(runStatus);
        assertThat(index.commandsForQuery(WorkbenchCommandQuery.forContractKey(previewKey)))
                .containsExactly(runDry, runSpecDry);
        assertThat(index.commandsForQuery(WorkbenchCommandQuery.of(
                        "assistant-agent",
                        null,
                        "Runs",
                        null,
                        previewKey.jsonSchemaId())))
                .isEmpty();
        assertThat(index.commandsForQuery(WorkbenchCommandQuery.of(
                        "coding-agent",
                        null,
                        "Runs",
                        null,
                        previewKey.jsonSchemaId())))
                .containsExactly(runDry);
    }

    @Test
    void reportsUnknownCategoryAndCommandIdWithKnownValues() {
        WorkbenchCommandIndex index = WorkbenchCommandIndex.of(List.of(
                command("run-dry-json", "Runs", List.of(), WorkbenchCommandContract.planning(
                        AgentRunPlanningContract.RUN_PREVIEW))));

        assertThatThrownBy(() -> index.commandsForCategory("Future"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown Wayang command category 'Future'")
                .hasMessageContaining("Runs");
        assertThatThrownBy(() -> index.commandsForId("future-command"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown Wayang command id 'future-command'")
                .hasMessageContaining("run-dry-json");
    }

    private static WorkbenchCommand command(
            String id,
            String category,
            List<String> surfaceIds,
            WorkbenchCommandContract contract) {
        return WorkbenchCommand.shared(
                id,
                id,
                id,
                category,
                id,
                surfaceIds,
                List.of(contract));
    }
}
