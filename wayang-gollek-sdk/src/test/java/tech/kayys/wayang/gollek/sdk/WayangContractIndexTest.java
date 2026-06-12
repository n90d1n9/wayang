package tech.kayys.wayang.gollek.sdk;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WayangContractIndexTest {

    @Test
    void indexesContractsByKeyAndJsonSchemaId() {
        WayangContractDescriptor preflight = WayangContractDescriptors.planning(
                AgentRunPlanningContract.RUN_PREFLIGHT,
                "Preflight",
                List.of("run-preflight-json"),
                "run <prompt> --preflight --json");
        WayangContractDescriptor preview = WayangContractDescriptors.planning(
                AgentRunPlanningContract.RUN_PREVIEW,
                "Preview",
                List.of("run-dry-json", "run-spec-dry-json"),
                "run <prompt> --dry-run --json",
                "run --spec <file> --dry-run --json");
        WayangContractIndex index = WayangContractIndex.of(List.of(preflight, preview));
        WayangContractKey previewKey = preview.key();

        assertThat(index.contracts()).containsExactly(preflight, preview);
        assertThat(index.keys()).containsExactly(preflight.key(), previewKey);
        assertThat(index.jsonSchemaIds()).containsExactly(preflight.jsonSchemaId(), preview.jsonSchemaId());
        assertThat(index.contractsByKey()).containsOnlyKeys(preflight.key(), previewKey);
        assertThat(index.contractsByJsonSchemaId()).containsOnlyKeys(preflight.jsonSchemaId(), preview.jsonSchemaId());
        assertThat(index.contractByKey(previewKey)).hasValue(preview);
        assertThat(index.contractByJsonSchemaId(" " + preview.jsonSchemaId() + " ")).hasValue(preview);
        assertThat(index.contractByJsonSchemaId("missing")).isEmpty();
        assertThat(index.duplicateKeys()).isEmpty();
        assertThat(index.duplicatesByKey()).isEmpty();
    }

    @Test
    void exposesContractFacetsAndSummaries() {
        WayangContractDescriptor preflight = WayangContractDescriptors.planning(
                AgentRunPlanningContract.RUN_PREFLIGHT,
                "Preflight",
                List.of("run-preflight-json"),
                "run <prompt> --preflight --json");
        WayangContractDescriptor preview = WayangContractDescriptors.planning(
                AgentRunPlanningContract.RUN_PREVIEW,
                "Preview",
                List.of("run-dry-json", "run-spec-dry-json"),
                "run <prompt> --dry-run --json",
                "run --spec <file> --dry-run --json");
        WayangContractDescriptor status = WayangContractDescriptors.lifecycle(
                AgentRunLifecycleContract.RUN_STATUS,
                "Status",
                List.of("run-status-json"),
                "run status <run-id> --json");
        WayangContractIndex index = WayangContractIndex.of(List.of(preflight, preview, status));

        assertThat(index.schemas())
                .containsExactly(AgentRunPlanningContract.SCHEMA, AgentRunLifecycleContract.SCHEMA);
        assertThat(index.schemaCounts())
                .containsExactly(
                        java.util.Map.entry(AgentRunPlanningContract.SCHEMA, 2),
                        java.util.Map.entry(AgentRunLifecycleContract.SCHEMA, 1));
        assertThat(index.domains())
                .containsExactly(WayangContractDescriptors.DOMAIN_PLANNING, WayangContractDescriptors.DOMAIN_LIFECYCLE);
        assertThat(index.domainCounts())
                .containsEntry(WayangContractDescriptors.DOMAIN_PLANNING, 2)
                .containsEntry(WayangContractDescriptors.DOMAIN_LIFECYCLE, 1);
        assertThat(index.envelopes())
                .containsExactly(
                        AgentRunPlanningContract.RUN_PREFLIGHT,
                        AgentRunPlanningContract.RUN_PREVIEW,
                        AgentRunLifecycleContract.RUN_STATUS);
        assertThat(index.commandIds())
                .containsExactly("run-preflight-json", "run-dry-json", "run-spec-dry-json", "run-status-json");
        assertThat(index.commandIdCounts())
                .containsEntry("run-preflight-json", 1)
                .containsEntry("run-dry-json", 1)
                .containsEntry("run-spec-dry-json", 1)
                .containsEntry("run-status-json", 1);
        assertThat(index.schemaSummaries())
                .first()
                .satisfies(summary -> {
                    assertThat(summary.name()).isEqualTo(AgentRunPlanningContract.SCHEMA);
                    assertThat(summary.count()).isEqualTo(2);
                    assertThat(summary.domains()).containsExactly(WayangContractDescriptors.DOMAIN_PLANNING);
                    assertThat(summary.envelopes())
                            .containsExactly(AgentRunPlanningContract.RUN_PREFLIGHT, AgentRunPlanningContract.RUN_PREVIEW);
                    assertThat(summary.commandIds())
                            .containsExactly("run-preflight-json", "run-dry-json", "run-spec-dry-json");
                });
        assertThat(index.domainSummaries())
                .first()
                .satisfies(summary -> assertThat(summary.schemas())
                        .containsExactly(AgentRunPlanningContract.SCHEMA));
        assertThat(index.envelopeSummaries())
                .extracting(WayangContractFacetSummary::name)
                .containsExactly(
                        AgentRunPlanningContract.RUN_PREFLIGHT,
                        AgentRunPlanningContract.RUN_PREVIEW,
                        AgentRunLifecycleContract.RUN_STATUS);
    }

    @Test
    void filtersContractsForTypedQueries() {
        WayangContractDescriptor preflight = WayangContractDescriptors.planning(
                AgentRunPlanningContract.RUN_PREFLIGHT,
                "Preflight",
                List.of("run-preflight-json"),
                "run <prompt> --preflight --json");
        WayangContractDescriptor preview = WayangContractDescriptors.planning(
                AgentRunPlanningContract.RUN_PREVIEW,
                "Preview",
                List.of("run-dry-json", "run-spec-dry-json"),
                "run <prompt> --dry-run --json",
                "run --spec <file> --dry-run --json");
        WayangContractDescriptor status = WayangContractDescriptors.lifecycle(
                AgentRunLifecycleContract.RUN_STATUS,
                "Status",
                List.of("run-status-json"),
                "run status <run-id> --json");
        WayangContractIndex index = WayangContractIndex.of(List.of(preflight, preview, status));

        assertThat(index.contractsForQuery(WayangContractQuery.planning()))
                .containsExactly(preflight, preview);
        assertThat(index.contractsForQuery(WayangContractQuery.planning(AgentRunPlanningContract.RUN_PREVIEW)))
                .containsExactly(preview);
        assertThat(index.contractsForQuery(WayangContractQuery.forCommandId(" run-dry-json ")))
                .containsExactly(preview);
        assertThat(index.contractsForQuery(WayangContractQuery.forDomain(WayangContractDescriptors.DOMAIN_LIFECYCLE)))
                .containsExactly(status);
        assertThat(index.contractsForQuery(WayangContractQuery.forKey(preview.key())))
                .containsExactly(preview);
        assertThat(index.contractsForQuery(WayangContractQuery.forJsonSchemaId(" " + preview.jsonSchemaId() + " ")))
                .containsExactly(preview);
        assertThat(index.contractsForQuery(null))
                .containsExactly(preflight, preview, status);
        assertThat(index.contractsForJsonSchemaId("missing")).isEmpty();
    }

    @Test
    void tracksDuplicateContractKeysAndKeepsFirstDescriptorInPrimaryIndex() {
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
        WayangContractIndex index = WayangContractIndex.of(List.of(first, duplicate));

        assertThat(index.contractByKey(first.key())).hasValue(first);
        assertThat(index.duplicateKeys()).containsExactly(first.key());
        assertThat(index.duplicatesByKey())
                .containsOnlyKeys(first.key());
        assertThat(index.duplicatesByKey().get(first.key()))
                .containsExactly(first, duplicate);
    }
}
