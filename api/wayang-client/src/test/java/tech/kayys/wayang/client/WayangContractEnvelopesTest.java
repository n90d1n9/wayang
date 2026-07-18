package tech.kayys.wayang.client;

import org.junit.jupiter.api.Test;

import tech.kayys.wayang.agent.planner.AgentRunPlanningContract;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WayangContractEnvelopesTest {

    @Test
    void catalogEnvelopeOwnsPublishedContractShape() {
        WayangContractDiscovery discovery = WayangContractCatalog.discover(WayangContractQuery.planning());

        Map<String, Object> values = WayangContractEnvelopes.catalog(" Wayang ", discovery);

        assertThat(values)
                .containsEntry("product", "Wayang")
                .containsEntry("schema", AgentRunPlanningContract.SCHEMA)
                .containsEntry("domain", WayangContractDescriptors.DOMAIN_PLANNING)
                .containsEntry("totalContracts", WayangContractCatalog.defaultContracts().size())
                .containsEntry("matchingContracts", 2)
                .containsEntry("schemas", List.of(AgentRunPlanningContract.SCHEMA))
                .containsEntry("domains", List.of(WayangContractDescriptors.DOMAIN_PLANNING))
                .containsEntry("envelopes", List.of(
                        AgentRunPlanningContract.RUN_PREFLIGHT,
                        AgentRunPlanningContract.RUN_PREVIEW));
        assertThat(objectMap(values.get("query")))
                .containsEntry("schema", AgentRunPlanningContract.SCHEMA)
                .containsEntry("envelope", null)
                .containsEntry("commandId", null)
                .containsEntry("domain", WayangContractDescriptors.DOMAIN_PLANNING)
                .containsEntry("jsonSchemaId", null)
                .containsEntry("filtered", true);
        assertThat(list(values.get("schemaSummaries")))
                .singleElement()
                .satisfies(summary -> assertThat(objectMap(summary))
                        .containsEntry("name", AgentRunPlanningContract.SCHEMA)
                        .containsEntry("count", 2)
                        .containsEntry("commandIds", List.of(
                                "run-preflight-json",
                                "run-dry-json",
                                "run-spec-dry-json")));
        assertThat(list(values.get("contracts")))
                .anySatisfy(contract -> assertThat(objectMap(contract))
                        .containsEntry("schema", AgentRunPlanningContract.SCHEMA)
                        .containsEntry("envelope", AgentRunPlanningContract.RUN_PREVIEW)
                        .containsEntry("jsonSchemaId", WayangContractKey.of(
                                AgentRunPlanningContract.SCHEMA,
                                AgentRunPlanningContract.VERSION,
                                AgentRunPlanningContract.RUN_PREVIEW).jsonSchemaId()));
    }

    @Test
    void indexEnvelopeOmitsContractEntriesButKeepsFacets() {
        WayangContractDiscovery discovery = WayangContractCatalog.discover(WayangContractQuery.forDomain("providers"));

        Map<String, Object> values = WayangContractEnvelopes.index("Wayang", discovery);

        assertThat(values)
                .doesNotContainKey("contracts")
                .containsEntry("domain", WayangContractDescriptors.DOMAIN_PROVIDERS)
                .containsEntry("matchingContracts", 2)
                .containsEntry("schemas", List.of(WayangProviderCapabilityContract.SCHEMA))
                .containsEntry("envelopes", List.of(
                        WayangProviderCapabilityContract.PROVIDER_CAPABILITY_DISCOVERY,
                        WayangProviderCapabilityContract.PROVIDER_CAPABILITY_DETAIL));
    }

    @Test
    void schemaBundleEnvelopeIncludesDocumentsByIdAndEntries() {
        WayangContractDiscovery discovery = WayangContractCatalog.discover(WayangContractQuery.providerCapability());
        WayangContractJsonSchemaBundle bundle = WayangContractJsonSchemas.bundle(discovery);

        Map<String, Object> values = WayangContractEnvelopes.schemaBundle("Wayang", bundle);

        assertThat(values)
                .containsEntry("schemaCount", 2)
                .containsEntry("schemaIds", bundle.ids())
                .containsKey("schemaDocumentsById");
        assertThat(list(values.get("schemaDocuments")))
                .hasSize(2)
                .allSatisfy(entry -> assertThat(objectMap(entry))
                        .containsKeys("id", "schema", "version", "envelope", "domain", "commandIds", "document"));
    }

    @Test
    void integrityReportEnvelopeOwnsPublishedContractCheckShape() {
        WayangContractIntegrityIssue issue = new WayangContractIntegrityIssue(
                "missing-command",
                "Command is not registered.",
                "wayang.custom",
                1,
                "custom-envelope",
                "custom-command");
        WayangContractIntegrityReport report = new WayangContractIntegrityReport(2, 3, 4, 5, List.of(issue));

        Map<String, Object> values = WayangContractEnvelopes.integrityReport(" Wayang ", report);

        assertThat(values)
                .containsEntry("product", "Wayang")
                .containsEntry("valid", false)
                .containsEntry("issueCount", 1)
                .containsEntry("totalContracts", 2)
                .containsEntry("totalCommands", 3)
                .containsEntry("contractCommandLinks", 4)
                .containsEntry("commandContractLinks", 5);
        assertThat(list(values.get("issues")))
                .singleElement()
                .satisfies(entry -> assertThat(objectMap(entry))
                        .containsEntry("kind", "missing-command")
                        .containsEntry("schema", "wayang.custom")
                        .containsEntry("envelope", "custom-envelope")
                        .containsEntry("commandId", "custom-command"));
    }

    @Test
    void coverageReportEnvelopeOwnsPublishedCommandCoverageShape() {
        WayangContractDescriptor contract = WayangContractDescriptors.contractCoverage(
                WayangContractCoverageContract.CONTRACT_COMMAND_COVERAGE,
                "Coverage report.",
                List.of("contracts-coverage-json", "missing-command"),
                "contracts --coverage --json");
        WayangContractCommandCoverageEntry entry = new WayangContractCommandCoverageEntry(
                contract,
                List.of("contracts-coverage-json", "missing-command"),
                List.of("contracts-coverage-json", "undeclared-command"));
        WayangContractCommandCoverageReport report =
                new WayangContractCommandCoverageReport(1, 4, List.of(entry));

        Map<String, Object> values = WayangContractEnvelopes.coverageReport("Wayang", report);

        assertThat(values)
                .containsEntry("product", "Wayang")
                .containsEntry("totalContracts", 1)
                .containsEntry("totalCommands", 4)
                .containsEntry("commandLinkedContracts", 1)
                .containsEntry("commandlessContracts", 0)
                .containsEntry("incompleteContracts", 1)
                .containsEntry("commandContractLinks", 2)
                .containsEntry("commandlessEntries", List.of());
        assertThat(list(values.get("incompleteEntries")))
                .singleElement()
                .satisfies(incomplete -> assertThat(objectMap(incomplete))
                        .containsEntry("schema", WayangContractCoverageContract.SCHEMA)
                        .containsEntry("version", WayangContractCoverageContract.VERSION)
                        .containsEntry("envelope", WayangContractCoverageContract.CONTRACT_COMMAND_COVERAGE)
                        .containsEntry("domain", WayangContractDescriptors.DOMAIN_CONTRACTS)
                        .containsEntry("unlinkedCommandIds", List.of("missing-command"))
                        .containsEntry("undeclaredLinkedCommandIds", List.of("undeclared-command"))
                        .containsEntry("commandLinked", true)
                        .containsEntry("commandless", false)
                        .containsEntry("complete", false));
    }

    @Test
    void queryFacetContractAndSchemaEntryNormalizeForJsonContracts() {
        WayangContractQuery query = WayangContractQuery.of(
                " wayang.custom ",
                " custom-envelope ",
                " custom-command ",
                " custom-domain ",
                " custom-schema-id ");
        WayangContractDescriptor descriptor = new WayangContractDescriptor(
                " custom.schema ",
                2,
                " custom-envelope ",
                " custom-domain ",
                " Custom contract. ",
                List.of("command-one"),
                List.of("custom --json"));
        WayangContractJsonSchema schema = new WayangContractJsonSchema(
                descriptor,
                " custom-id ",
                Map.of("type", "object"));
        WayangContractFacetSummary summary = new WayangContractFacetSummary(
                " custom ",
                1,
                List.of("custom.schema"),
                List.of("custom-domain"),
                List.of("custom-envelope"),
                List.of("custom-id"),
                List.of("command-one"));

        assertThat(WayangContractEnvelopes.query(query))
                .containsEntry("schema", "wayang.custom")
                .containsEntry("envelope", "custom-envelope")
                .containsEntry("commandId", "custom-command")
                .containsEntry("domain", "custom-domain")
                .containsEntry("jsonSchemaId", "custom-schema-id")
                .containsEntry("filtered", true);
        assertThat(WayangContractEnvelopes.facetSummary(summary))
                .containsEntry("name", "custom")
                .containsEntry("count", 1)
                .containsEntry("commandIds", List.of("command-one"));
        assertThat(WayangContractEnvelopes.contract(descriptor))
                .containsEntry("schema", "custom.schema")
                .containsEntry("version", 2)
                .containsEntry("envelope", "custom-envelope")
                .containsEntry("domain", "custom-domain")
                .containsEntry("commandIds", List.of("command-one"));
        assertThat(WayangContractEnvelopes.schemaEntry(schema))
                .containsEntry("id", "custom-id")
                .containsEntry("schema", "custom.schema")
                .containsEntry("document", Map.of("type", "object"));
    }

    @Test
    void nullDiscoveryProducesEmptyCatalogEnvelope() {
        Map<String, Object> values = WayangContractEnvelopes.catalog(null, null);
        Map<String, Object> integrity = WayangContractEnvelopes.integrityReport(null, null);
        Map<String, Object> coverage = WayangContractEnvelopes.coverageReport(null, null);

        assertThat(values)
                .containsEntry("product", "")
                .containsEntry("schema", null)
                .containsEntry("totalContracts", 0)
                .containsEntry("matchingContracts", 0)
                .containsEntry("schemas", List.of())
                .containsEntry("contracts", List.of());
        assertThat(objectMap(values.get("query"))).containsEntry("filtered", false);
        assertThat(integrity)
                .containsEntry("product", "")
                .containsEntry("valid", true)
                .containsEntry("issueCount", 0)
                .containsEntry("issues", List.of());
        assertThat(coverage)
                .containsEntry("product", "")
                .containsEntry("totalContracts", 0)
                .containsEntry("commandlessEntries", List.of())
                .containsEntry("incompleteEntries", List.of());
        assertThatThrownBy(() -> values.put("extra", "value"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> integrity.put("extra", "value"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> coverage.put("extra", "value"))
                .isInstanceOf(UnsupportedOperationException.class);
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
