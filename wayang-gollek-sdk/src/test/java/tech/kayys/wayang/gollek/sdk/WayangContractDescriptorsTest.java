package tech.kayys.wayang.gollek.sdk;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WayangContractDescriptorsTest {

    @Test
    void createsTypedLifecycleDescriptors() {
        WayangContractDescriptor descriptor = WayangContractDescriptors.lifecycle(
                " run-status ",
                " Status ",
                List.of("run-status-json"),
                "run status <run-id> --json");

        assertThat(descriptor.schema()).isEqualTo(AgentRunLifecycleContract.SCHEMA);
        assertThat(descriptor.version()).isEqualTo(AgentRunLifecycleContract.VERSION);
        assertThat(descriptor.envelope()).isEqualTo(AgentRunLifecycleContract.RUN_STATUS);
        assertThat(descriptor.domain()).isEqualTo(WayangContractDescriptors.DOMAIN_LIFECYCLE);
        assertThat(descriptor.description()).isEqualTo("Status");
        assertThat(descriptor.commandIds()).containsExactly("run-status-json");
        assertThat(descriptor.commands()).containsExactly("run status <run-id> --json");
        assertThat(descriptor.jsonSchemaId())
                .isEqualTo("urn:wayang:contract:wayang.run.lifecycle:v1:run-status");
    }

    @Test
    void createsTypedReadinessDescriptors() {
        WayangContractDescriptor descriptor = WayangContractDescriptors.readiness(
                " readiness-aggregate ",
                " Aggregate readiness ",
                List.of(),
                "status --readiness --json");

        assertThat(descriptor.schema()).isEqualTo(WayangReadinessContract.SCHEMA);
        assertThat(descriptor.version()).isEqualTo(WayangReadinessContract.VERSION);
        assertThat(descriptor.envelope()).isEqualTo(WayangReadinessContract.READINESS_AGGREGATE);
        assertThat(descriptor.domain()).isEqualTo(WayangContractDescriptors.DOMAIN_READINESS);
        assertThat(descriptor.description()).isEqualTo("Aggregate readiness");
        assertThat(descriptor.commandIds()).isEmpty();
        assertThat(descriptor.commands()).containsExactly("status --readiness --json");
        assertThat(descriptor.jsonSchemaId())
                .isEqualTo("urn:wayang:contract:wayang.readiness:v1:readiness-aggregate");
    }

    @Test
    void createsTypedContractCoverageDescriptors() {
        WayangContractDescriptor descriptor = WayangContractDescriptors.contractCoverage(
                " contract-command-coverage ",
                " Coverage ",
                List.of("contracts-coverage-json"),
                "contracts --coverage --json");

        assertThat(descriptor.schema()).isEqualTo(WayangContractCoverageContract.SCHEMA);
        assertThat(descriptor.version()).isEqualTo(WayangContractCoverageContract.VERSION);
        assertThat(descriptor.envelope())
                .isEqualTo(WayangContractCoverageContract.CONTRACT_COMMAND_COVERAGE);
        assertThat(descriptor.domain()).isEqualTo(WayangContractDescriptors.DOMAIN_CONTRACTS);
        assertThat(descriptor.description()).isEqualTo("Coverage");
        assertThat(descriptor.commandIds()).containsExactly("contracts-coverage-json");
        assertThat(descriptor.commands()).containsExactly("contracts --coverage --json");
        assertThat(descriptor.jsonSchemaId())
                .isEqualTo("urn:wayang:contract:wayang.contract.coverage:v1:contract-command-coverage");
    }

    @Test
    void createsTypedStandardCatalogDescriptors() {
        WayangContractDescriptor descriptor = WayangContractDescriptors.standardCatalog(
                " standards-catalog ",
                " Standards ",
                List.of("standards-catalog-json"),
                "standards --catalog --json");

        assertThat(descriptor.schema()).isEqualTo(WayangStandardCatalogContract.SCHEMA);
        assertThat(descriptor.version()).isEqualTo(WayangStandardCatalogContract.VERSION);
        assertThat(descriptor.envelope()).isEqualTo(WayangStandardCatalogContract.STANDARDS_CATALOG);
        assertThat(descriptor.domain()).isEqualTo(WayangContractDescriptors.DOMAIN_STANDARDS);
        assertThat(descriptor.description()).isEqualTo("Standards");
        assertThat(descriptor.commandIds()).containsExactly("standards-catalog-json");
        assertThat(descriptor.commands()).containsExactly("standards --catalog --json");
        assertThat(descriptor.jsonSchemaId())
                .isEqualTo("urn:wayang:contract:wayang.standard.catalog:v1:standards-catalog");
    }

    @Test
    void createsTypedStandardAlignmentDescriptors() {
        WayangContractDescriptor descriptor = WayangContractDescriptors.standardAlignment(
                " standard-alignment-health ",
                " Standards health ",
                List.of("standards-health-json"),
                "standards --json");

        assertThat(descriptor.schema()).isEqualTo(WayangStandardAlignmentContract.SCHEMA);
        assertThat(descriptor.version()).isEqualTo(WayangStandardAlignmentContract.VERSION);
        assertThat(descriptor.envelope())
                .isEqualTo(WayangStandardAlignmentContract.STANDARD_ALIGNMENT_HEALTH);
        assertThat(descriptor.domain()).isEqualTo(WayangContractDescriptors.DOMAIN_STANDARDS);
        assertThat(descriptor.description()).isEqualTo("Standards health");
        assertThat(descriptor.commandIds()).containsExactly("standards-health-json");
        assertThat(descriptor.commands()).containsExactly("standards --json");
        assertThat(descriptor.jsonSchemaId())
                .isEqualTo("urn:wayang:contract:wayang.standard.alignment:v1:standard-alignment-health");
    }

    @Test
    void createsTypedProviderCapabilityDescriptors() {
        WayangContractDescriptor descriptor = WayangContractDescriptors.providerCapability(
                " provider-capability-discovery ",
                " Providers ",
                List.of("providers-json"),
                "providers --json");

        assertThat(descriptor.schema()).isEqualTo(WayangProviderCapabilityContract.SCHEMA);
        assertThat(descriptor.version()).isEqualTo(WayangProviderCapabilityContract.VERSION);
        assertThat(descriptor.envelope())
                .isEqualTo(WayangProviderCapabilityContract.PROVIDER_CAPABILITY_DISCOVERY);
        assertThat(descriptor.domain()).isEqualTo(WayangContractDescriptors.DOMAIN_PROVIDERS);
        assertThat(descriptor.description()).isEqualTo("Providers");
        assertThat(descriptor.commandIds()).containsExactly("providers-json");
        assertThat(descriptor.commands()).containsExactly("providers --json");
        assertThat(descriptor.jsonSchemaId())
                .isEqualTo(
                        "urn:wayang:contract:wayang.provider.capability:v1:provider-capability-discovery");
    }

    @Test
    void centralizesJsonSchemaIdsForCommandLinks() {
        WorkbenchCommandContract commandContract =
                WorkbenchCommandContract.lifecycle(AgentRunLifecycleContract.RUN_STATUS);
        WorkbenchCommandContract coverageContract =
                WorkbenchCommandContract.contractCoverage(
                        WayangContractCoverageContract.CONTRACT_COMMAND_COVERAGE);
        WorkbenchCommandContract standardsContract =
                WorkbenchCommandContract.standardCatalog(
                        WayangStandardCatalogContract.STANDARDS_CATALOG);
        WorkbenchCommandContract standardAlignmentContract =
                WorkbenchCommandContract.standardAlignment(
                        WayangStandardAlignmentContract.STANDARD_ALIGNMENT_HEALTH);
        WorkbenchCommandContract providerContract =
                WorkbenchCommandContract.providerCapability(
                        WayangProviderCapabilityContract.PROVIDER_CAPABILITY_DISCOVERY);

        assertThat(commandContract.jsonSchemaId())
                .isEqualTo(WayangContractDescriptors.jsonSchemaId(
                        AgentRunLifecycleContract.SCHEMA,
                        AgentRunLifecycleContract.VERSION,
                        AgentRunLifecycleContract.RUN_STATUS));
        assertThat(coverageContract.jsonSchemaId())
                .isEqualTo(WayangContractDescriptors.jsonSchemaId(
                        WayangContractCoverageContract.SCHEMA,
                        WayangContractCoverageContract.VERSION,
                        WayangContractCoverageContract.CONTRACT_COMMAND_COVERAGE));
        assertThat(standardsContract.jsonSchemaId())
                .isEqualTo(WayangContractDescriptors.jsonSchemaId(
                        WayangStandardCatalogContract.SCHEMA,
                        WayangStandardCatalogContract.VERSION,
                        WayangStandardCatalogContract.STANDARDS_CATALOG));
        assertThat(standardAlignmentContract.jsonSchemaId())
                .isEqualTo(WayangContractDescriptors.jsonSchemaId(
                        WayangStandardAlignmentContract.SCHEMA,
                        WayangStandardAlignmentContract.VERSION,
                        WayangStandardAlignmentContract.STANDARD_ALIGNMENT_HEALTH));
        assertThat(providerContract.jsonSchemaId())
                .isEqualTo(WayangContractDescriptors.jsonSchemaId(
                        WayangProviderCapabilityContract.SCHEMA,
                        WayangProviderCapabilityContract.VERSION,
                        WayangProviderCapabilityContract.PROVIDER_CAPABILITY_DISCOVERY));
        assertThat(WayangContractDescriptors.jsonSchemaId(" wayang.run.lifecycle ", 0, " run-status "))
                .isEqualTo("urn:wayang:contract:wayang.run.lifecycle:v1:run-status");
    }

    @Test
    void exposesStableEmptyDescriptor() {
        WayangContractDescriptor empty = WayangContractDescriptors.empty();

        assertThat(empty.schema()).isEmpty();
        assertThat(empty.version()).isEqualTo(1);
        assertThat(empty.envelope()).isEmpty();
        assertThat(empty.domain()).isEmpty();
        assertThat(empty.commandIds()).isEmpty();
        assertThat(empty.commands()).isEmpty();
    }
}
