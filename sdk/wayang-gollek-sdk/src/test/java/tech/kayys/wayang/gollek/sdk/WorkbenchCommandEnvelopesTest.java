package tech.kayys.wayang.gollek.sdk;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkbenchCommandEnvelopesTest {

    @Test
    void discoveryEnvelopeOwnsPublishedCommandShape() {
        WorkbenchCommandDiscovery discovery = WayangCommandDiscoveryService.create().commandDiscovery(
                WayangWorkbenchCatalog.localCommands(),
                new WorkbenchCommandQuery(" assistant-agent ", null, " Runs ", null, null));

        Map<String, Object> values = WorkbenchCommandEnvelopes.discovery(" Wayang ", discovery);

        assertThat(values)
                .containsEntry("product", "Wayang")
                .containsEntry("surfaceId", "assistant-agent")
                .containsEntry("profileId", null)
                .containsEntry("resolvedSurfaceId", "assistant-agent")
                .containsEntry("category", "Runs")
                .containsEntry("commandId", null)
                .containsEntry("contractJsonSchemaId", null)
                .containsEntry("totalCommands", WayangWorkbenchCatalog.localCommands().size())
                .containsEntry("matchingCommands", discovery.matchingCommands())
                .containsEntry("categories", List.of("Runs"));
        assertThat(objectMap(values.get("query")))
                .containsEntry("surfaceId", "assistant-agent")
                .containsEntry("profileId", null)
                .containsEntry("resolvedSurfaceId", "assistant-agent")
                .containsEntry("category", "Runs")
                .containsEntry("commandId", null)
                .containsEntry("contractJsonSchemaId", null)
                .containsEntry("filtered", true);
        assertThat(list(values.get("categorySummaries")))
                .singleElement()
                .satisfies(summary -> assertThat(objectMap(summary))
                        .containsEntry("name", "Runs")
                        .containsEntry("count", discovery.matchingCommands()));
        assertThat(list(values.get("commands")))
                .anySatisfy(command -> assertThat(objectMap(command))
                        .containsEntry("id", "run-session-context")
                        .containsEntry("category", "Runs")
                        .containsEntry("localOnly", false));
    }

    @Test
    void indexEnvelopeOmitsCommandEntriesButKeepsFacets() {
        WorkbenchCommandDiscovery discovery = WayangCommandDiscoveryService.create().commandDiscovery(
                WayangWorkbenchCatalog.localCommands(),
                WorkbenchCommandQuery.forCommandId("run-print-spec-output"));

        Map<String, Object> values = WorkbenchCommandEnvelopes.index("Wayang", discovery);

        assertThat(values)
                .doesNotContainKey("commands")
                .containsEntry("commandId", "run-print-spec-output")
                .containsEntry("matchingCommands", 1)
                .containsEntry("categories", List.of("Run Specs"))
                .containsEntry("commandIds", List.of("run-print-spec-output"));
    }

    @Test
    void commandMapIncludesContractsOnlyWhenPresent() {
        WorkbenchCommand contractCommand = command("run-dry-json");
        WorkbenchCommand plainCommand = command("run-print-spec-output");

        Map<String, Object> contractValues = WorkbenchCommandEnvelopes.command(contractCommand);
        Map<String, Object> plainValues = WorkbenchCommandEnvelopes.command(plainCommand);

        assertThat(contractValues)
                .containsEntry("id", "run-dry-json")
                .containsKey("contracts");
        assertThat(list(contractValues.get("contracts")))
                .singleElement()
                .satisfies(contract -> assertThat(objectMap(contract))
                        .containsEntry("schema", AgentRunPlanningContract.SCHEMA)
                        .containsEntry("version", AgentRunPlanningContract.VERSION)
                        .containsEntry("envelope", AgentRunPlanningContract.RUN_PREVIEW));
        assertThat(plainValues)
                .containsEntry("id", "run-print-spec-output")
                .doesNotContainKey("contracts");
    }

    @Test
    void queryAndContractSummaryNormalizeForJsonContracts() {
        WorkbenchCommandQuery query = WorkbenchCommandQuery.forProfile(" low-code-agent ");
        WorkbenchCommandContractSummary summary = new WorkbenchCommandContractSummary(
                " schema-id ",
                " wayang.custom ",
                2,
                " custom-envelope ",
                3,
                List.of("one", "two"));

        assertThat(WorkbenchCommandEnvelopes.query(query))
                .containsEntry("surfaceId", null)
                .containsEntry("profileId", "low-code-agent")
                .containsEntry("resolvedSurfaceId", "workflow-platform")
                .containsEntry("category", null)
                .containsEntry("commandId", null)
                .containsEntry("contractJsonSchemaId", null)
                .containsEntry("filtered", true);
        assertThat(WorkbenchCommandEnvelopes.contractSummary(summary))
                .containsEntry("jsonSchemaId", "schema-id")
                .containsEntry("schema", "wayang.custom")
                .containsEntry("version", 2)
                .containsEntry("envelope", "custom-envelope")
                .containsEntry("count", 3)
                .containsEntry("commandIds", List.of("one", "two"));
    }

    @Test
    void nullDiscoveryProducesEmptyDiscoveryEnvelope() {
        Map<String, Object> values = WorkbenchCommandEnvelopes.discovery(null, null);

        assertThat(values)
                .containsEntry("product", "")
                .containsEntry("surfaceId", null)
                .containsEntry("resolvedSurfaceId", null)
                .containsEntry("totalCommands", 0)
                .containsEntry("matchingCommands", 0)
                .containsEntry("commandIds", List.of())
                .containsEntry("commands", List.of());
        assertThat(objectMap(values.get("query"))).containsEntry("filtered", false);
        assertThatThrownBy(() -> values.put("extra", "value"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    private static WorkbenchCommand command(String id) {
        return WorkbenchCommandIndex.of(WayangWorkbenchCatalog.localCommands())
                .findCommand(id)
                .orElseThrow();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> objectMap(Object value) {
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> list(Object value) {
        return (List<Object>) value;
    }
}
