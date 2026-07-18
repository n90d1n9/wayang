package tech.kayys.wayang.client;

import org.junit.jupiter.api.Test;

import tech.kayys.wayang.agent.lifecycle.AgentRunLifecycleContract;
import tech.kayys.wayang.agent.planner.AgentRunPlanningContract;
import tech.kayys.wayang.alignment.WayangStandardAlignmentContract;
import tech.kayys.wayang.command.WayangCommandDiscoveryContract;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangContractCatalogTest {

    @Test
    void discoversAllKnownContracts() {
        List<WayangContractDescriptor> contracts = WayangContractCatalog.defaultContracts();
        WayangContractDiscovery discovery = WayangContractCatalog.discover(WayangContractQuery.all());

        assertThat(discovery.totalContracts()).isEqualTo(contracts.size());
        assertThat(discovery.matchingContracts()).isEqualTo(contracts.size());
        assertThat(discovery.schemas())
                .containsExactly(
                        "wayang.run.lifecycle",
                        "wayang.run.planning",
                        "wayang.readiness",
                        "wayang.contract.coverage",
                        "wayang.platform.catalog",
                        "wayang.standard.alignment",
                        "wayang.standard.catalog",
                        "wayang.skill.catalog",
                        "wayang.provider.capability",
                        "wayang.command.discovery",
                        "wayang.workbench.discovery");
        assertThat(discovery.schemaCounts())
                .containsEntry("wayang.run.lifecycle", 15)
                .containsEntry("wayang.run.planning", 2)
                .containsEntry("wayang.readiness", 2)
                .containsEntry("wayang.contract.coverage", 1)
                .containsEntry("wayang.platform.catalog", 12)
                .containsEntry("wayang.standard.alignment", 1)
                .containsEntry("wayang.standard.catalog", 1)
                .containsEntry("wayang.skill.catalog", 2)
                .containsEntry("wayang.provider.capability", 2)
                .containsEntry("wayang.command.discovery", 1)
                .containsEntry("wayang.workbench.discovery", 1);
        assertThat(discovery.schemaSummaries())
                .anySatisfy(summary -> {
                    assertThat(summary.name()).isEqualTo("wayang.run.lifecycle");
                    assertThat(summary.count()).isEqualTo(15);
                    assertThat(summary.domains()).containsExactly("lifecycle");
                    assertThat(summary.envelopes())
                            .contains(
                                    "run-result",
                                    "run-events-follow",
                                    "run-forget",
                                    "run-store-verification",
                                    "run-store-compaction-preview",
                                    "run-store-compaction");
                    assertThat(summary.commandIds()).contains("run-result-json", "run-events-follow-result-json");
                })
                .anySatisfy(summary -> {
                    assertThat(summary.name()).isEqualTo("wayang.run.planning");
                    assertThat(summary.count()).isEqualTo(2);
                    assertThat(summary.domains()).containsExactly("planning");
                    assertThat(summary.envelopes()).containsExactly("run-preflight", "run-preview");
                    assertThat(summary.commandIds())
                            .containsExactly("run-preflight-json", "run-dry-json", "run-spec-dry-json");
                })
                .anySatisfy(summary -> {
                    assertThat(summary.name()).isEqualTo("wayang.readiness");
                    assertThat(summary.count()).isEqualTo(2);
                    assertThat(summary.domains()).containsExactly("readiness");
                    assertThat(summary.envelopes())
                            .containsExactly("readiness-report", "readiness-aggregate");
                    assertThat(summary.commandIds())
                            .containsExactly("status-readiness-json", "status-readiness-profile-json");
                })
                .anySatisfy(summary -> {
                    assertThat(summary.name()).isEqualTo("wayang.contract.coverage");
                    assertThat(summary.count()).isEqualTo(1);
                    assertThat(summary.domains()).containsExactly("contracts");
                    assertThat(summary.envelopes())
                            .containsExactly("contract-command-coverage");
                    assertThat(summary.commandIds()).containsExactly("contracts-coverage-json");
                })
                .anySatisfy(summary -> {
                    assertThat(summary.name()).isEqualTo("wayang.platform.catalog");
                    assertThat(summary.count()).isEqualTo(12);
                    assertThat(summary.domains()).containsExactly("platform");
                    assertThat(summary.envelopes())
                            .containsExactly(
                                    "platform-status",
                                    "product-catalog",
                                    "profile-list",
                                    "profile-detail",
                                    "sdk-boundary-catalog",
                                    "sdk-boundary-detail",
                                    "readiness-profile-list",
                                    "readiness-profile-detail",
                                    "readiness-profile-validation",
                                    "readiness-profile-validation-policy-list",
                                    "readiness-profile-registry-config-diagnostics",
                                    "readiness-profile-registry-resolution");
                    assertThat(summary.commandIds())
                            .containsExactly(
                                    "status-json",
                                    "products-json",
                                    "profiles-json",
                                    "profiles-surface-json",
                                    "profiles-inspect-json",
                                    "sdk-boundaries-json",
                                    "sdk-boundaries-inspect-json",
                                    "readiness-profiles-json",
                                    "readiness-profiles-inspect-json",
                                    "readiness-profiles-check-json",
                                    "readiness-profiles-policies-json",
                                    "readiness-profiles-config-json",
                                    "readiness-profiles-sources-json");
                })
                .anySatisfy(summary -> {
                    assertThat(summary.name()).isEqualTo("wayang.standard.alignment");
                    assertThat(summary.count()).isEqualTo(1);
                    assertThat(summary.domains()).containsExactly("standards");
                    assertThat(summary.envelopes())
                            .containsExactly("standard-alignment-health");
                    assertThat(summary.commandIds()).containsExactly("standards-health-json");
                })
                .anySatisfy(summary -> {
                    assertThat(summary.name()).isEqualTo("wayang.standard.catalog");
                    assertThat(summary.count()).isEqualTo(1);
                    assertThat(summary.domains()).containsExactly("standards");
                    assertThat(summary.envelopes())
                            .containsExactly("standards-catalog");
                    assertThat(summary.commandIds()).containsExactly("standards-catalog-json");
                })
                .anySatisfy(summary -> {
                    assertThat(summary.name()).isEqualTo("wayang.skill.catalog");
                    assertThat(summary.count()).isEqualTo(2);
                    assertThat(summary.domains()).containsExactly("skills");
                    assertThat(summary.envelopes()).containsExactly("skill-discovery", "skill-detail");
                    assertThat(summary.commandIds())
                            .containsExactly(
                                    "skills-list-json",
                                    "skills-list-profile-json",
                                    "skills-search-json",
                                    "skills-search-profile-json",
                                    "skills-inspect-json");
                })
                .anySatisfy(summary -> {
                    assertThat(summary.name()).isEqualTo("wayang.provider.capability");
                    assertThat(summary.count()).isEqualTo(2);
                    assertThat(summary.domains()).containsExactly("providers");
                    assertThat(summary.envelopes())
                            .containsExactly("provider-capability-discovery", "provider-capability-detail");
                    assertThat(summary.commandIds())
                            .containsExactly(
                                    "providers-json",
                                    "providers-list-json",
                                    "providers-search-json",
                                    "providers-inspect-json");
                })
                .anySatisfy(summary -> {
                    assertThat(summary.name()).isEqualTo("wayang.command.discovery");
                    assertThat(summary.count()).isEqualTo(1);
                    assertThat(summary.domains()).containsExactly("workbench");
                    assertThat(summary.envelopes()).containsExactly("commands-discovery");
                    assertThat(summary.commandIds())
                            .containsExactly(
                                    "commands-surface-json",
                                    "commands-profile-json",
                                    "commands-index-json",
                                    "commands-id-json",
                                    "commands-contract-json-schema-id-json");
                })
                .anySatisfy(summary -> {
                    assertThat(summary.name()).isEqualTo("wayang.workbench.discovery");
                    assertThat(summary.count()).isEqualTo(1);
                    assertThat(summary.domains()).containsExactly("workbench");
                    assertThat(summary.envelopes()).containsExactly("workbench-discovery");
                    assertThat(summary.commandIds())
                            .containsExactly(
                                    "workbench-surface-json",
                                    "workbench-profile-json",
                                    "workbench-command-json",
                                    "workbench-contract-json-schema-id-json");
                });
        assertThat(discovery.domains())
                .containsExactly(
                        "lifecycle",
                        "planning",
                        "readiness",
                        "contracts",
                        "platform",
                        "standards",
                        "skills",
                        "providers",
                        "workbench");
        assertThat(discovery.domainCounts())
                .containsEntry("lifecycle", 15)
                .containsEntry("planning", 2)
                .containsEntry("readiness", 2)
                .containsEntry("contracts", 1)
                .containsEntry("platform", 12)
                .containsEntry("standards", 2)
                .containsEntry("skills", 2)
                .containsEntry("providers", 2)
                .containsEntry("workbench", 2);
        assertThat(discovery.domainSummaries())
                .anySatisfy(summary -> {
                    assertThat(summary.name()).isEqualTo("planning");
                    assertThat(summary.count()).isEqualTo(2);
                    assertThat(summary.schemas()).containsExactly("wayang.run.planning");
                    assertThat(summary.envelopes()).containsExactly("run-preflight", "run-preview");
                })
                .anySatisfy(summary -> {
                    assertThat(summary.name()).isEqualTo("readiness");
                    assertThat(summary.count()).isEqualTo(2);
                    assertThat(summary.schemas()).containsExactly("wayang.readiness");
                    assertThat(summary.envelopes())
                            .containsExactly("readiness-report", "readiness-aggregate");
                    assertThat(summary.commandIds())
                            .containsExactly("status-readiness-json", "status-readiness-profile-json");
                })
                .anySatisfy(summary -> {
                    assertThat(summary.name()).isEqualTo("contracts");
                    assertThat(summary.count()).isEqualTo(1);
                    assertThat(summary.schemas()).containsExactly("wayang.contract.coverage");
                    assertThat(summary.envelopes()).containsExactly("contract-command-coverage");
                    assertThat(summary.commandIds()).containsExactly("contracts-coverage-json");
                })
                .anySatisfy(summary -> {
                    assertThat(summary.name()).isEqualTo("platform");
                    assertThat(summary.count()).isEqualTo(12);
                    assertThat(summary.schemas()).containsExactly("wayang.platform.catalog");
                    assertThat(summary.envelopes())
                            .containsExactly(
                                    "platform-status",
                                    "product-catalog",
                                    "profile-list",
                                    "profile-detail",
                                    "sdk-boundary-catalog",
                                    "sdk-boundary-detail",
                                    "readiness-profile-list",
                                    "readiness-profile-detail",
                                    "readiness-profile-validation",
                                    "readiness-profile-validation-policy-list",
                                    "readiness-profile-registry-config-diagnostics",
                                    "readiness-profile-registry-resolution");
                })
                .anySatisfy(summary -> {
                    assertThat(summary.name()).isEqualTo("standards");
                    assertThat(summary.count()).isEqualTo(2);
                    assertThat(summary.schemas())
                            .containsExactly("wayang.standard.alignment", "wayang.standard.catalog");
                    assertThat(summary.envelopes())
                            .containsExactly("standard-alignment-health", "standards-catalog");
                    assertThat(summary.commandIds())
                            .containsExactly("standards-health-json", "standards-catalog-json");
                })
                .anySatisfy(summary -> {
                    assertThat(summary.name()).isEqualTo("skills");
                    assertThat(summary.count()).isEqualTo(2);
                    assertThat(summary.schemas()).containsExactly("wayang.skill.catalog");
                    assertThat(summary.envelopes()).containsExactly("skill-discovery", "skill-detail");
                })
                .anySatisfy(summary -> {
                    assertThat(summary.name()).isEqualTo("providers");
                    assertThat(summary.count()).isEqualTo(2);
                    assertThat(summary.schemas()).containsExactly("wayang.provider.capability");
                    assertThat(summary.envelopes())
                            .containsExactly("provider-capability-discovery", "provider-capability-detail");
                    assertThat(summary.commandIds())
                            .containsExactly(
                                    "providers-json",
                                    "providers-list-json",
                                    "providers-search-json",
                                    "providers-inspect-json");
                })
                .anySatisfy(summary -> {
                    assertThat(summary.name()).isEqualTo("workbench");
                    assertThat(summary.count()).isEqualTo(2);
                    assertThat(summary.schemas())
                            .containsExactly("wayang.command.discovery", "wayang.workbench.discovery");
                    assertThat(summary.envelopes()).containsExactly("commands-discovery", "workbench-discovery");
                });
        assertThat(discovery.envelopes())
                .contains(
                        "run-result",
                        "run-events-follow",
                        "run-preflight",
                        "run-preview",
                        "readiness-report",
                        "readiness-aggregate",
                        "contract-command-coverage",
                        "platform-status",
                        "product-catalog",
                        "standard-alignment-health",
                        "standards-catalog",
                        "skill-discovery",
                        "skill-detail",
                        "provider-capability-discovery",
                        "provider-capability-detail",
                        "commands-discovery",
                        "workbench-discovery");
        assertThat(discovery.envelopeSummaries())
                .anySatisfy(summary -> {
                    assertThat(summary.name()).isEqualTo("run-preview");
                    assertThat(summary.count()).isEqualTo(1);
                    assertThat(summary.schemas()).containsExactly("wayang.run.planning");
                    assertThat(summary.domains()).containsExactly("planning");
                    assertThat(summary.jsonSchemaIds())
                            .containsExactly("urn:wayang:contract:wayang.run.planning:v1:run-preview");
                    assertThat(summary.commandIds()).containsExactly("run-dry-json", "run-spec-dry-json");
                });
        assertThat(discovery.jsonSchemaIds())
                .contains(
                        "urn:wayang:contract:wayang.run.lifecycle:v1:run-result",
                        "urn:wayang:contract:wayang.run.planning:v1:run-preview",
                        "urn:wayang:contract:wayang.readiness:v1:readiness-report",
                        "urn:wayang:contract:wayang.contract.coverage:v1:contract-command-coverage",
                        "urn:wayang:contract:wayang.platform.catalog:v1:product-catalog",
                        "urn:wayang:contract:wayang.standard.alignment:v1:standard-alignment-health",
                        "urn:wayang:contract:wayang.standard.catalog:v1:standards-catalog",
                        "urn:wayang:contract:wayang.skill.catalog:v1:skill-discovery",
                        "urn:wayang:contract:wayang.provider.capability:v1:provider-capability-discovery",
                        "urn:wayang:contract:wayang.command.discovery:v1:commands-discovery",
                        "urn:wayang:contract:wayang.workbench.discovery:v1:workbench-discovery");
        assertThat(discovery.keys())
                .contains(
                        WayangContractKey.of(
                                AgentRunLifecycleContract.SCHEMA,
                                AgentRunLifecycleContract.VERSION,
                                AgentRunLifecycleContract.RUN_RESULT),
                        WayangContractKey.of(
                                AgentRunPlanningContract.SCHEMA,
                                AgentRunPlanningContract.VERSION,
                                AgentRunPlanningContract.RUN_PREVIEW),
                        WayangContractKey.of(
                                WayangReadinessContract.SCHEMA,
                                WayangReadinessContract.VERSION,
                                WayangReadinessContract.READINESS_REPORT),
                        WayangContractKey.of(
                                WayangContractCoverageContract.SCHEMA,
                                WayangContractCoverageContract.VERSION,
                                WayangContractCoverageContract.CONTRACT_COMMAND_COVERAGE),
                        WayangContractKey.of(
                                WayangPlatformContract.SCHEMA,
                                WayangPlatformContract.VERSION,
                                WayangPlatformContract.PRODUCT_CATALOG),
                        WayangContractKey.of(
                                WayangStandardAlignmentContract.SCHEMA,
                                WayangStandardAlignmentContract.VERSION,
                                WayangStandardAlignmentContract.STANDARD_ALIGNMENT_HEALTH),
                        WayangContractKey.of(
                                WayangStandardCatalogContract.SCHEMA,
                                WayangStandardCatalogContract.VERSION,
                                WayangStandardCatalogContract.STANDARDS_CATALOG),
                        WayangContractKey.of(
                                WayangSkillContract.SCHEMA,
                                WayangSkillContract.VERSION,
                                WayangSkillContract.SKILL_DISCOVERY),
                        WayangContractKey.of(
                                WayangProviderCapabilityContract.SCHEMA,
                                WayangProviderCapabilityContract.VERSION,
                                WayangProviderCapabilityContract.PROVIDER_CAPABILITY_DISCOVERY),
                        WayangContractKey.of(
                                WayangCommandDiscoveryContract.SCHEMA,
                                WayangCommandDiscoveryContract.VERSION,
                                WayangCommandDiscoveryContract.COMMANDS_DISCOVERY),
                        WayangContractKey.of(
                                WayangWorkbenchContract.SCHEMA,
                                WayangWorkbenchContract.VERSION,
                                WayangWorkbenchContract.WORKBENCH_DISCOVERY));
        assertThat(discovery.commandIds())
                .contains(
                        "run-result-json",
                        "run-events-follow-result-json",
                        "run-dry-json",
                        "contracts-coverage-json",
                        "standards-health-json",
                        "standards-catalog-json",
                        "products-json",
                        "skills-search-json",
                        "providers-search-json",
                        "commands-index-json",
                        "workbench-command-json");
        assertThat(discovery.commandIdCounts())
                .containsEntry("run-events-follow-result-json", 2)
                .containsEntry("run-dry-json", 1);
    }

    @Test
    void exposesKeyedContractLookup() {
        WayangContractDiscovery discovery = WayangContractCatalog.discover(
                WayangContractQuery.of(AgentRunPlanningContract.SCHEMA, null));
        WayangContractKey previewKey = WayangContractKey.of(
                AgentRunPlanningContract.SCHEMA,
                AgentRunPlanningContract.VERSION,
                AgentRunPlanningContract.RUN_PREVIEW);

        assertThat(discovery.keys())
                .containsExactly(
                        WayangContractKey.of(
                                AgentRunPlanningContract.SCHEMA,
                                AgentRunPlanningContract.VERSION,
                                AgentRunPlanningContract.RUN_PREFLIGHT),
                        previewKey);
        assertThat(discovery.contractsByKey()).containsOnlyKeys(
                WayangContractKey.of(
                        AgentRunPlanningContract.SCHEMA,
                        AgentRunPlanningContract.VERSION,
                        AgentRunPlanningContract.RUN_PREFLIGHT),
                previewKey);
        assertThat(discovery.contractByKey(previewKey))
                .hasValueSatisfying(contract -> {
                    assertThat(contract.envelope()).isEqualTo(AgentRunPlanningContract.RUN_PREVIEW);
                    assertThat(contract.commandIds()).containsExactly("run-dry-json", "run-spec-dry-json");
                });
        assertThat(discovery.contractByJsonSchemaId(" urn:wayang:contract:wayang.run.planning:v1:run-preview "))
                .hasValueSatisfying(contract -> assertThat(contract.key()).isEqualTo(previewKey));
        assertThat(discovery.contractByKey(WayangContractKey.of(
                        AgentRunPlanningContract.SCHEMA,
                        AgentRunPlanningContract.VERSION,
                        "missing")))
                .isEmpty();
        assertThat(discovery.contractByJsonSchemaId("not-a-wayang-schema-id")).isEmpty();
    }

    @Test
    void filtersContractsBySchemaAndEnvelope() {
        WayangContractDiscovery planning = WayangContractCatalog.discover(
                WayangContractQuery.of(" wayang.run.planning ", null));
        WayangContractDiscovery preview = WayangContractCatalog.discover(
                WayangContractQuery.of(null, " run-preview "));
        WayangContractDiscovery command = WayangContractCatalog.discover(
                WayangContractQuery.of(null, null, " run-dry-json "));
        WayangContractDiscovery lifecycle = WayangContractCatalog.discover(
                WayangContractQuery.of(null, null, null, " lifecycle "));
        WayangContractDiscovery schemaId = WayangContractCatalog.discover(WayangContractQuery.of(
                null,
                null,
                null,
                null,
                " urn:wayang:contract:wayang.run.planning:v1:run-preview "));
        WayangContractDiscovery commandDiscovery = WayangContractCatalog.discover(
                WayangContractQuery.of(WayangCommandDiscoveryContract.SCHEMA, null));
        WayangContractDiscovery readiness = WayangContractCatalog.discover(WayangContractQuery.readiness());
        WayangContractDiscovery contractCoverage =
                WayangContractCatalog.discover(WayangContractQuery.contractCoverage());
        WayangContractDiscovery platformCatalog = WayangContractCatalog.discover(
                WayangContractQuery.of(WayangPlatformContract.SCHEMA, null));
        WayangContractDiscovery standardCatalog = WayangContractCatalog.discover(
                WayangContractQuery.of(WayangStandardCatalogContract.SCHEMA, null));
        WayangContractDiscovery standardAlignment = WayangContractCatalog.discover(
                WayangContractQuery.of(WayangStandardAlignmentContract.SCHEMA, null));
        WayangContractDiscovery skillCatalog = WayangContractCatalog.discover(
                WayangContractQuery.of(WayangSkillContract.SCHEMA, null));
        WayangContractDiscovery providerCapabilities = WayangContractCatalog.discover(
                WayangContractQuery.providerCapability());
        WayangContractDiscovery workbenchDiscovery = WayangContractCatalog.discover(
                WayangContractQuery.of(WayangWorkbenchContract.SCHEMA, null));

        assertThat(planning.matchingContracts()).isEqualTo(2);
        assertThat(planning.contracts())
                .extracting(WayangContractDescriptor::domain)
                .containsOnly("planning");
        assertThat(preview.contracts())
                .singleElement()
                .satisfies(contract -> {
                    assertThat(contract.schema()).isEqualTo("wayang.run.planning");
                    assertThat(contract.envelope()).isEqualTo("run-preview");
                    assertThat(contract.commandIds()).containsExactly("run-dry-json", "run-spec-dry-json");
                    assertThat(contract.commands())
                            .containsExactly(
                                    "run <prompt> --dry-run --json",
                                    "run --spec <file> --dry-run --json");
                });
        assertThat(command.query().commandId()).isEqualTo("run-dry-json");
        assertThat(command.domains()).containsExactly("planning");
        assertThat(command.domainCounts()).containsEntry("planning", 1);
        assertThat(command.domainSummaries())
                .singleElement()
                .satisfies(summary -> {
                    assertThat(summary.name()).isEqualTo("planning");
                    assertThat(summary.count()).isEqualTo(1);
                    assertThat(summary.envelopes()).containsExactly("run-preview");
                    assertThat(summary.jsonSchemaIds())
                            .containsExactly("urn:wayang:contract:wayang.run.planning:v1:run-preview");
                    assertThat(summary.commandIds()).containsExactly("run-dry-json", "run-spec-dry-json");
                });
        assertThat(command.commandIds()).containsExactly("run-dry-json", "run-spec-dry-json");
        assertThat(command.contracts())
                .singleElement()
                .satisfies(contract -> {
                    assertThat(contract.envelope()).isEqualTo("run-preview");
                    assertThat(contract.jsonSchemaId())
                            .isEqualTo("urn:wayang:contract:wayang.run.planning:v1:run-preview");
                });
        assertThat(lifecycle.query().domain()).isEqualTo("lifecycle");
        assertThat(lifecycle.matchingContracts()).isEqualTo(15);
        assertThat(lifecycle.domains()).containsExactly("lifecycle");
        assertThat(lifecycle.domainCounts()).containsEntry("lifecycle", 15);
        assertThat(lifecycle.contracts())
                .extracting(WayangContractDescriptor::schema)
                .containsOnly("wayang.run.lifecycle");
        assertThat(schemaId.query().jsonSchemaId())
                .isEqualTo("urn:wayang:contract:wayang.run.planning:v1:run-preview");
        assertThat(schemaId.query().jsonSchemaKey())
                .hasValue(WayangContractKey.of(
                        AgentRunPlanningContract.SCHEMA,
                        AgentRunPlanningContract.VERSION,
                        AgentRunPlanningContract.RUN_PREVIEW));
        assertThat(schemaId.matchingContracts()).isEqualTo(1);
        assertThat(schemaId.contracts())
                .singleElement()
                .satisfies(contract -> {
                    assertThat(contract.schema()).isEqualTo("wayang.run.planning");
                    assertThat(contract.envelope()).isEqualTo("run-preview");
                    assertThat(contract.jsonSchemaId())
                            .isEqualTo("urn:wayang:contract:wayang.run.planning:v1:run-preview");
                });
        assertThat(commandDiscovery.matchingContracts()).isEqualTo(1);
        assertThat(commandDiscovery.contracts())
                .singleElement()
                .satisfies(contract -> {
                    assertThat(contract.schema()).isEqualTo(WayangCommandDiscoveryContract.SCHEMA);
                    assertThat(contract.envelope()).isEqualTo(WayangCommandDiscoveryContract.COMMANDS_DISCOVERY);
                    assertThat(contract.domain()).isEqualTo("workbench");
                    assertThat(contract.commandIds())
                            .containsExactly(
                                    "commands-surface-json",
                                    "commands-profile-json",
                                    "commands-index-json",
                                    "commands-id-json",
                                    "commands-contract-json-schema-id-json");
                });
        assertThat(readiness.matchingContracts()).isEqualTo(2);
        assertThat(readiness.contracts())
                .extracting(WayangContractDescriptor::envelope)
                .containsExactly(
                        WayangReadinessContract.READINESS_REPORT,
                        WayangReadinessContract.READINESS_AGGREGATE);
        assertThat(readiness.domainCounts()).containsEntry("readiness", 2);
        assertThat(readiness.commandIds())
                .containsExactly("status-readiness-json", "status-readiness-profile-json");
        assertThat(contractCoverage.matchingContracts()).isEqualTo(1);
        assertThat(contractCoverage.contracts())
                .singleElement()
                .satisfies(contract -> {
                    assertThat(contract.schema()).isEqualTo(WayangContractCoverageContract.SCHEMA);
                    assertThat(contract.envelope())
                            .isEqualTo(WayangContractCoverageContract.CONTRACT_COMMAND_COVERAGE);
                    assertThat(contract.domain()).isEqualTo("contracts");
                    assertThat(contract.commandIds()).containsExactly("contracts-coverage-json");
                });
        assertThat(contractCoverage.domainCounts()).containsEntry("contracts", 1);
        assertThat(platformCatalog.matchingContracts()).isEqualTo(12);
        assertThat(platformCatalog.contracts())
                .extracting(WayangContractDescriptor::envelope)
                .containsExactly(
                        WayangPlatformContract.PLATFORM_STATUS,
                        WayangPlatformContract.PRODUCT_CATALOG,
                        WayangPlatformContract.PROFILE_LIST,
                        WayangPlatformContract.PROFILE_DETAIL,
                        WayangPlatformContract.SDK_BOUNDARY_CATALOG,
                        WayangPlatformContract.SDK_BOUNDARY_DETAIL,
                        WayangPlatformContract.READINESS_PROFILE_LIST,
                        WayangPlatformContract.READINESS_PROFILE_DETAIL,
                        WayangPlatformContract.READINESS_PROFILE_VALIDATION,
                        WayangPlatformContract.READINESS_PROFILE_VALIDATION_POLICY_LIST,
                        WayangPlatformContract.READINESS_PROFILE_REGISTRY_CONFIG_DIAGNOSTICS,
                        WayangPlatformContract.READINESS_PROFILE_REGISTRY_RESOLUTION);
        assertThat(platformCatalog.domainCounts()).containsEntry("platform", 12);
        assertThat(platformCatalog.commandIds())
                .containsExactly(
                        "status-json",
                        "products-json",
                        "profiles-json",
                        "profiles-surface-json",
                        "profiles-inspect-json",
                        "sdk-boundaries-json",
                        "sdk-boundaries-inspect-json",
                        "readiness-profiles-json",
                        "readiness-profiles-inspect-json",
                        "readiness-profiles-check-json",
                        "readiness-profiles-policies-json",
                        "readiness-profiles-config-json",
                        "readiness-profiles-sources-json");
        assertThat(standardCatalog.matchingContracts()).isEqualTo(1);
        assertThat(standardCatalog.contracts())
                .singleElement()
                .satisfies(contract -> {
                    assertThat(contract.schema()).isEqualTo(WayangStandardCatalogContract.SCHEMA);
                    assertThat(contract.envelope())
                            .isEqualTo(WayangStandardCatalogContract.STANDARDS_CATALOG);
                    assertThat(contract.domain()).isEqualTo("standards");
                    assertThat(contract.commandIds()).containsExactly("standards-catalog-json");
                    assertThat(contract.jsonSchemaId())
                            .isEqualTo("urn:wayang:contract:wayang.standard.catalog:v1:standards-catalog");
                });
        assertThat(standardCatalog.domainCounts()).containsEntry("standards", 1);
        assertThat(standardAlignment.matchingContracts()).isEqualTo(1);
        assertThat(standardAlignment.contracts())
                .singleElement()
                .satisfies(contract -> {
                    assertThat(contract.schema()).isEqualTo(WayangStandardAlignmentContract.SCHEMA);
                    assertThat(contract.envelope())
                            .isEqualTo(WayangStandardAlignmentContract.STANDARD_ALIGNMENT_HEALTH);
                    assertThat(contract.domain()).isEqualTo("standards");
                    assertThat(contract.commandIds()).containsExactly("standards-health-json");
                    assertThat(contract.jsonSchemaId())
                            .isEqualTo(
                                    "urn:wayang:contract:wayang.standard.alignment:v1:standard-alignment-health");
                });
        assertThat(standardAlignment.domainCounts()).containsEntry("standards", 1);
        assertThat(skillCatalog.matchingContracts()).isEqualTo(2);
        assertThat(skillCatalog.contracts())
                .extracting(WayangContractDescriptor::envelope)
                .containsExactly(
                        WayangSkillContract.SKILL_DISCOVERY,
                        WayangSkillContract.SKILL_DETAIL);
        assertThat(skillCatalog.domainCounts()).containsEntry("skills", 2);
        assertThat(skillCatalog.commandIds())
                .containsExactly(
                        "skills-list-json",
                        "skills-list-profile-json",
                        "skills-search-json",
                        "skills-search-profile-json",
                        "skills-inspect-json");
        assertThat(providerCapabilities.matchingContracts()).isEqualTo(2);
        assertThat(providerCapabilities.contracts())
                .extracting(WayangContractDescriptor::envelope)
                .containsExactly(
                        WayangProviderCapabilityContract.PROVIDER_CAPABILITY_DISCOVERY,
                        WayangProviderCapabilityContract.PROVIDER_CAPABILITY_DETAIL);
        assertThat(providerCapabilities.domainCounts()).containsEntry("providers", 2);
        assertThat(providerCapabilities.commandIds())
                .containsExactly(
                        "providers-json",
                        "providers-list-json",
                        "providers-search-json",
                        "providers-inspect-json");
        assertThat(workbenchDiscovery.matchingContracts()).isEqualTo(1);
        assertThat(workbenchDiscovery.contracts())
                .singleElement()
                .satisfies(contract -> {
                    assertThat(contract.schema()).isEqualTo(WayangWorkbenchContract.SCHEMA);
                    assertThat(contract.envelope()).isEqualTo(WayangWorkbenchContract.WORKBENCH_DISCOVERY);
                    assertThat(contract.domain()).isEqualTo("workbench");
                    assertThat(contract.commandIds())
                            .containsExactly(
                                    "workbench-surface-json",
                                    "workbench-profile-json",
                                    "workbench-command-json",
                                    "workbench-contract-json-schema-id-json");
                });
    }

    @Test
    void typedContractQueriesPreserveContractIdentity() {
        WayangContractKey previewKey = WayangContractKey.of(
                AgentRunPlanningContract.SCHEMA,
                AgentRunPlanningContract.VERSION,
                AgentRunPlanningContract.RUN_PREVIEW);

        WayangContractQuery byKey = WayangContractQuery.forKey(previewKey);
        WayangContractDiscovery previewByKey = WayangContractCatalog.discover(byKey);

        assertThat(byKey.jsonSchemaId()).isEqualTo(previewKey.jsonSchemaId());
        assertThat(byKey.jsonSchemaKey()).hasValue(previewKey);
        assertThat(byKey.schema()).isNull();
        assertThat(previewByKey.contractByKey(previewKey))
                .hasValueSatisfying(contract -> assertThat(contract.commandIds())
                        .containsExactly("run-dry-json", "run-spec-dry-json"));

        assertThat(WayangContractCatalog.discover(WayangContractQuery.planning()).matchingContracts()).isEqualTo(2);
        assertThat(WayangContractCatalog.discover(WayangContractQuery.planning(AgentRunPlanningContract.RUN_PREVIEW))
                        .commandIds())
                .containsExactly("run-dry-json", "run-spec-dry-json");
        assertThat(WayangContractCatalog.discover(
                                WayangContractQuery.readiness(WayangReadinessContract.READINESS_AGGREGATE))
                        .contracts())
                .singleElement()
                .satisfies(contract -> {
                    assertThat(contract.key()).isEqualTo(WayangContractKey.of(
                            WayangReadinessContract.SCHEMA,
                            WayangReadinessContract.VERSION,
                            WayangReadinessContract.READINESS_AGGREGATE));
                    assertThat(contract.commandIds())
                            .containsExactly("status-readiness-json", "status-readiness-profile-json");
                    assertThat(contract.commands())
                            .containsExactly(
                                    "status --readiness --json",
                                    "status --readiness-profile <profile-id> --json");
                });
        assertThat(WayangContractCatalog.discover(WayangContractQuery.contractCoverage())
                        .contracts())
                .singleElement()
                .satisfies(contract -> assertThat(contract.key()).isEqualTo(WayangContractKey.of(
                        WayangContractCoverageContract.SCHEMA,
                        WayangContractCoverageContract.VERSION,
                        WayangContractCoverageContract.CONTRACT_COMMAND_COVERAGE)));
        assertThat(WayangContractCatalog.discover(WayangContractQuery.lifecycle(AgentRunLifecycleContract.RUN_STATUS))
                        .contracts())
                .singleElement()
                .satisfies(contract -> assertThat(contract.key()).isEqualTo(WayangContractKey.of(
                        AgentRunLifecycleContract.SCHEMA,
                        AgentRunLifecycleContract.VERSION,
                        AgentRunLifecycleContract.RUN_STATUS)));
        assertThat(WayangContractCatalog.discover(WayangContractQuery.platform(WayangPlatformContract.PROFILE_LIST))
                        .commandIds())
                .containsExactly("profiles-json", "profiles-surface-json");
        assertThat(WayangContractCatalog.discover(WayangContractQuery.standardCatalog())
                        .commandIds())
                .containsExactly("standards-catalog-json");
        assertThat(WayangContractCatalog.discover(WayangContractQuery.standardAlignment())
                        .commandIds())
                .containsExactly("standards-health-json");
        assertThat(WayangContractCatalog.discover(WayangContractQuery.skill(WayangSkillContract.SKILL_DISCOVERY))
                        .commandIds())
                .containsExactly(
                        "skills-list-json",
                        "skills-list-profile-json",
                        "skills-search-json",
                        "skills-search-profile-json");
        assertThat(WayangContractCatalog.discover(WayangContractQuery.providerCapability(
                                WayangProviderCapabilityContract.PROVIDER_CAPABILITY_DISCOVERY))
                        .commandIds())
                .containsExactly(
                        "providers-json",
                        "providers-list-json",
                        "providers-search-json");
        assertThat(WayangContractCatalog.discover(WayangContractQuery.commandDiscovery()).commandIds())
                .containsExactly(
                        "commands-surface-json",
                        "commands-profile-json",
                        "commands-index-json",
                        "commands-id-json",
                        "commands-contract-json-schema-id-json");
        assertThat(WayangContractCatalog.discover(WayangContractQuery.workbenchDiscovery()).commandIds())
                .containsExactly(
                        "workbench-surface-json",
                        "workbench-profile-json",
                        "workbench-command-json",
                        "workbench-contract-json-schema-id-json");
        assertThat(WayangContractCatalog.discover(WayangContractQuery.forCommandId(" run-dry-json "))
                        .contracts())
                .singleElement()
                .satisfies(contract -> assertThat(contract.key()).isEqualTo(previewKey));
        assertThat(WayangContractCatalog.discover(WayangContractQuery.forDomain(
                                WayangContractDescriptors.DOMAIN_LIFECYCLE))
                        .matchingContracts())
                .isEqualTo(15);
        assertThat(WayangContractCatalog.discover(WayangContractQuery.forDomain(
                                WayangContractDescriptors.DOMAIN_CONTRACTS))
                        .commandIds())
                .containsExactly("contracts-coverage-json");
    }

    @Test
    void sdkExposesContractDiscovery() {
        WayangContractDiscovery discovery = WayangGollekSdk.local().contractDiscovery(
                WayangContractQuery.of("wayang.run.lifecycle", "run-status"));

        assertThat(discovery.matchingContracts()).isEqualTo(1);
        assertThat(discovery.contracts())
                .singleElement()
                .satisfies(contract -> {
                    assertThat(contract.envelope()).isEqualTo("run-status");
                    assertThat(contract.commandIds()).containsExactly("run-status-json");
                });
    }

    @Test
    void sdkExposesJsonSchemaForContractEnvelope() {
        WayangContractDescriptor preview = WayangContractCatalog.discover(
                        WayangContractQuery.of(null, "run-preview"))
                .contracts()
                .get(0);

        WayangContractJsonSchema schema = WayangGollekSdk.local().contractJsonSchema(preview);

        assertThat(schema.id()).isEqualTo("urn:wayang:contract:wayang.run.planning:v1:run-preview");
        assertThat(schema.contract()).isEqualTo(preview);
        assertThat(schema.document())
                .containsEntry("$schema", "https://json-schema.org/draft/2020-12/schema")
                .containsEntry("$id", "urn:wayang:contract:wayang.run.planning:v1:run-preview")
                .containsEntry("x-wayang-envelope", "run-preview")
                .containsEntry("x-wayang-domain", "planning");
        assertThat(schema.document()).containsKey("properties");
    }

    @Test
    void sdkExposesJsonSchemaForCommandDiscoveryEnvelope() {
        WayangContractDescriptor descriptor = WayangContractCatalog.discover(
                        WayangContractQuery.of(WayangCommandDiscoveryContract.SCHEMA, null))
                .contracts()
                .get(0);

        WayangContractJsonSchema schema = WayangGollekSdk.local().contractJsonSchema(descriptor);

        assertThat(schema.id())
                .isEqualTo("urn:wayang:contract:wayang.command.discovery:v1:commands-discovery");
        assertThat(schema.contract()).isEqualTo(descriptor);
        assertThat(schema.document())
                .containsEntry("$schema", "https://json-schema.org/draft/2020-12/schema")
                .containsEntry("$id", "urn:wayang:contract:wayang.command.discovery:v1:commands-discovery")
                .containsEntry("x-wayang-schema", WayangCommandDiscoveryContract.SCHEMA)
                .containsEntry("x-wayang-envelope", WayangCommandDiscoveryContract.COMMANDS_DISCOVERY)
                .containsEntry("x-wayang-domain", "workbench");
        @SuppressWarnings("unchecked")
        java.util.List<String> required = (java.util.List<String>) schema.document().get("required");
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> properties =
                (java.util.Map<String, Object>) schema.document().get("properties");
        assertThat(required)
                .contains(
                        "product",
                        "query",
                        "totalCommands",
                        "matchingCommands",
                        "categorySummaries",
                        "contractSummaries",
                        "commandIds")
                .doesNotContain("contract");
        assertThat(properties)
                .containsKeys(
                        "query",
                        "categories",
                        "categoryCounts",
                        "contractJsonSchemaIds",
                        "contractJsonSchemaIdCounts",
                        "contractSummaries",
                        "commands");
    }

    @Test
    void sdkExposesJsonSchemaForWorkbenchEnvelope() {
        WayangContractDescriptor descriptor = WayangContractCatalog.discover(
                        WayangContractQuery.of(WayangWorkbenchContract.SCHEMA, null))
                .contracts()
                .get(0);

        WayangContractJsonSchema schema = WayangGollekSdk.local().contractJsonSchema(descriptor);

        assertThat(schema.id())
                .isEqualTo("urn:wayang:contract:wayang.workbench.discovery:v1:workbench-discovery");
        assertThat(schema.contract()).isEqualTo(descriptor);
        assertThat(schema.document())
                .containsEntry("$schema", "https://json-schema.org/draft/2020-12/schema")
                .containsEntry("$id", "urn:wayang:contract:wayang.workbench.discovery:v1:workbench-discovery")
                .containsEntry("x-wayang-schema", WayangWorkbenchContract.SCHEMA)
                .containsEntry("x-wayang-envelope", WayangWorkbenchContract.WORKBENCH_DISCOVERY)
                .containsEntry("x-wayang-domain", "workbench");
        @SuppressWarnings("unchecked")
        java.util.List<String> required = (java.util.List<String>) schema.document().get("required");
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> properties =
                (java.util.Map<String, Object>) schema.document().get("properties");
        assertThat(required)
                .containsExactly(
                        "product",
                        "status",
                        "catalog",
                        "commandQuery",
                        "commandPalette",
                        "commands",
                        "nextActions");
        assertThat(properties)
                .containsKeys("status", "catalog", "commandQuery", "commandPalette", "commands", "nextActions");
    }

    @Test
    void sdkExposesJsonSchemaForPlatformCatalogEnvelope() {
        WayangContractDescriptor descriptor = WayangContractCatalog.discover(
                        WayangContractQuery.of(WayangPlatformContract.SCHEMA, WayangPlatformContract.PRODUCT_CATALOG))
                .contracts()
                .get(0);

        WayangContractJsonSchema schema = WayangGollekSdk.local().contractJsonSchema(descriptor);

        assertThat(schema.id())
                .isEqualTo("urn:wayang:contract:wayang.platform.catalog:v1:product-catalog");
        assertThat(schema.contract()).isEqualTo(descriptor);
        assertThat(schema.document())
                .containsEntry("$schema", "https://json-schema.org/draft/2020-12/schema")
                .containsEntry("$id", "urn:wayang:contract:wayang.platform.catalog:v1:product-catalog")
                .containsEntry("x-wayang-schema", WayangPlatformContract.SCHEMA)
                .containsEntry("x-wayang-envelope", WayangPlatformContract.PRODUCT_CATALOG)
                .containsEntry("x-wayang-domain", "platform");
        @SuppressWarnings("unchecked")
        java.util.List<String> required = (java.util.List<String>) schema.document().get("required");
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> properties =
                (java.util.Map<String, Object>) schema.document().get("properties");
        assertThat(required)
                .containsExactly("product", "coreEngine", "surfaces", "profiles");
        assertThat(properties)
                .containsKeys("product", "coreEngine", "surfaces", "profiles");
    }

    @Test
    void sdkExposesJsonSchemaForReadinessProfileListEnvelope() {
        WayangContractDescriptor descriptor = WayangContractCatalog.discover(
                        WayangContractQuery.of(
                                WayangPlatformContract.SCHEMA,
                                WayangPlatformContract.READINESS_PROFILE_LIST))
                .contracts()
                .get(0);

        WayangContractJsonSchema schema = WayangGollekSdk.local().contractJsonSchema(descriptor);

        assertThat(schema.id())
                .isEqualTo("urn:wayang:contract:wayang.platform.catalog:v1:readiness-profile-list");
        assertThat(schema.contract()).isEqualTo(descriptor);
        assertThat(schema.document())
                .containsEntry("$schema", "https://json-schema.org/draft/2020-12/schema")
                .containsEntry("$id", "urn:wayang:contract:wayang.platform.catalog:v1:readiness-profile-list")
                .containsEntry("x-wayang-schema", WayangPlatformContract.SCHEMA)
                .containsEntry("x-wayang-envelope", WayangPlatformContract.READINESS_PROFILE_LIST)
                .containsEntry("x-wayang-domain", "platform");
        @SuppressWarnings("unchecked")
        java.util.List<String> required = (java.util.List<String>) schema.document().get("required");
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> properties =
                (java.util.Map<String, Object>) schema.document().get("properties");
        assertThat(required)
                .containsExactly(
                        "product",
                        "totalProfiles",
                        "defaultProfileId",
                        "productionProfileId",
                        "profileIds",
                        "profiles");
        assertThat(properties)
                .containsKeys(
                        "product",
                        "totalProfiles",
                        "defaultProfileId",
                        "productionProfileId",
                        "profileIds",
                        "profiles");
    }

    @Test
    void sdkExposesJsonSchemaForReadinessProfileDetailEnvelope() {
        WayangContractDescriptor descriptor = WayangContractCatalog.discover(
                        WayangContractQuery.of(
                                WayangPlatformContract.SCHEMA,
                                WayangPlatformContract.READINESS_PROFILE_DETAIL))
                .contracts()
                .get(0);

        WayangContractJsonSchema schema = WayangGollekSdk.local().contractJsonSchema(descriptor);

        assertThat(schema.id())
                .isEqualTo("urn:wayang:contract:wayang.platform.catalog:v1:readiness-profile-detail");
        assertThat(schema.contract()).isEqualTo(descriptor);
        assertThat(schema.document())
                .containsEntry("$schema", "https://json-schema.org/draft/2020-12/schema")
                .containsEntry("$id", "urn:wayang:contract:wayang.platform.catalog:v1:readiness-profile-detail")
                .containsEntry("x-wayang-schema", WayangPlatformContract.SCHEMA)
                .containsEntry("x-wayang-envelope", WayangPlatformContract.READINESS_PROFILE_DETAIL)
                .containsEntry("x-wayang-domain", "platform");
        @SuppressWarnings("unchecked")
        java.util.List<String> required = (java.util.List<String>) schema.document().get("required");
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> properties =
                (java.util.Map<String, Object>) schema.document().get("properties");
        assertThat(required)
                .containsExactly("product", "profileId", "profile");
        assertThat(properties)
                .containsKeys("product", "profileId", "profile");
    }

    @Test
    void sdkExposesJsonSchemaForReadinessProfileValidationEnvelope() {
        WayangContractDescriptor descriptor = WayangContractCatalog.discover(
                        WayangContractQuery.of(
                                WayangPlatformContract.SCHEMA,
                                WayangPlatformContract.READINESS_PROFILE_VALIDATION))
                .contracts()
                .get(0);

        WayangContractJsonSchema schema = WayangGollekSdk.local().contractJsonSchema(descriptor);

        assertThat(schema.id())
                .isEqualTo("urn:wayang:contract:wayang.platform.catalog:v1:readiness-profile-validation");
        assertThat(schema.contract()).isEqualTo(descriptor);
        assertThat(schema.document())
                .containsEntry("$schema", "https://json-schema.org/draft/2020-12/schema")
                .containsEntry("$id", "urn:wayang:contract:wayang.platform.catalog:v1:readiness-profile-validation")
                .containsEntry("x-wayang-schema", WayangPlatformContract.SCHEMA)
                .containsEntry("x-wayang-envelope", WayangPlatformContract.READINESS_PROFILE_VALIDATION)
                .containsEntry("x-wayang-domain", "platform");
        @SuppressWarnings("unchecked")
        java.util.List<String> required = (java.util.List<String>) schema.document().get("required");
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> properties =
                (java.util.Map<String, Object>) schema.document().get("properties");
        assertThat(required)
                .containsExactly(
                        "product",
                        "valid",
                        "issueCount",
                        "totalProfiles",
                        "profileIds",
                        "validationPolicy",
                        "defaultProfileCount",
                        "defaultProfileIds",
                        "productionProfileCount",
                        "productionProfileIds",
                        "knownReadinessIds",
                        "coveredReadinessCount",
                        "coveredReadinessIds",
                        "uncoveredReadinessCount",
                        "uncoveredReadinessIds",
                        "issues");
        assertThat(properties)
                .containsKeys(
                        "product",
                        "valid",
                        "issueCount",
                        "totalProfiles",
                        "profileIds",
                        "validationPolicy",
                        "defaultProfileCount",
                        "defaultProfileIds",
                        "productionProfileCount",
                        "productionProfileIds",
                        "knownReadinessIds",
                        "coveredReadinessCount",
                        "coveredReadinessIds",
                        "uncoveredReadinessCount",
                        "uncoveredReadinessIds",
                        "issues");
        Map<String, Object> validationPolicy = objectMap(properties.get("validationPolicy"));
        assertThat(stringList(validationPolicy.get("required")))
                .containsExactly(
                        "policyId",
                        "strict",
                        "knownReadinessCount",
                        "requireDefaultProfile",
                        "requireProductionProfile",
                        "requireFullReadinessCoverage");
        assertThat(objectMap(validationPolicy.get("properties")))
                .containsKeys(
                        "policyId",
                        "strict",
                        "knownReadinessCount",
                        "requireDefaultProfile",
                        "requireProductionProfile",
                        "requireFullReadinessCoverage");
    }

    @Test
    void sdkExposesJsonSchemaForReadinessProfileValidationPolicyListEnvelope() {
        WayangContractDescriptor descriptor = WayangContractCatalog.discover(
                        WayangContractQuery.of(
                                WayangPlatformContract.SCHEMA,
                                WayangPlatformContract.READINESS_PROFILE_VALIDATION_POLICY_LIST))
                .contracts()
                .get(0);

        WayangContractJsonSchema schema = WayangGollekSdk.local().contractJsonSchema(descriptor);

        assertThat(schema.id())
                .isEqualTo(
                        "urn:wayang:contract:wayang.platform.catalog:v1:"
                                + "readiness-profile-validation-policy-list");
        assertThat(schema.contract()).isEqualTo(descriptor);
        assertThat(schema.document())
                .containsEntry("$schema", "https://json-schema.org/draft/2020-12/schema")
                .containsEntry(
                        "$id",
                        "urn:wayang:contract:wayang.platform.catalog:v1:"
                                + "readiness-profile-validation-policy-list")
                .containsEntry("x-wayang-schema", WayangPlatformContract.SCHEMA)
                .containsEntry("x-wayang-envelope", WayangPlatformContract.READINESS_PROFILE_VALIDATION_POLICY_LIST)
                .containsEntry("x-wayang-domain", "platform");

        assertThat(stringList(schema.document().get("required")))
                .containsExactly(
                        "product",
                        "totalPolicies",
                        "defaultPolicyId",
                        "policyIds",
                        "policies");
        Map<String, Object> properties = objectMap(schema.document().get("properties"));
        assertThat(properties)
                .containsKeys(
                        "product",
                        "totalPolicies",
                        "defaultPolicyId",
                        "policyIds",
                        "policies");
        Map<String, Object> policyItems = objectMap(objectMap(properties.get("policies")).get("items"));
        assertThat(stringList(policyItems.get("required")))
                .containsExactly(
                        "policyId",
                        "description",
                        "defaultPolicy",
                        "strict",
                        "knownReadinessCount",
                        "requireDefaultProfile",
                        "requireProductionProfile",
                        "requireFullReadinessCoverage");
        assertThat(objectMap(policyItems.get("properties")))
                .containsKeys(
                        "policyId",
                        "description",
                        "defaultPolicy",
                        "strict",
                        "knownReadinessCount",
                        "requireDefaultProfile",
                        "requireProductionProfile",
                        "requireFullReadinessCoverage");
    }

    @Test
    void sdkExposesJsonSchemaForReadinessProfileRegistryResolutionEnvelope() {
        WayangContractDescriptor descriptor = WayangContractCatalog.discover(
                        WayangContractQuery.of(
                                WayangPlatformContract.SCHEMA,
                                WayangPlatformContract.READINESS_PROFILE_REGISTRY_RESOLUTION))
                .contracts()
                .get(0);

        WayangContractJsonSchema schema = WayangGollekSdk.local().contractJsonSchema(descriptor);

        assertThat(schema.id())
                .isEqualTo(
                        "urn:wayang:contract:wayang.platform.catalog:v1:"
                                + "readiness-profile-registry-resolution");
        assertThat(schema.contract()).isEqualTo(descriptor);
        assertThat(schema.document())
                .containsEntry("$schema", "https://json-schema.org/draft/2020-12/schema")
                .containsEntry(
                        "$id",
                        "urn:wayang:contract:wayang.platform.catalog:v1:"
                                + "readiness-profile-registry-resolution")
                .containsEntry("x-wayang-schema", WayangPlatformContract.SCHEMA)
                .containsEntry("x-wayang-envelope", WayangPlatformContract.READINESS_PROFILE_REGISTRY_RESOLUTION)
                .containsEntry("x-wayang-domain", "platform");

        assertThat(stringList(schema.document().get("required")))
                .containsExactly(
                        "product",
                        "valid",
                        "activeSourceId",
                        "activeSourceType",
                        "activeSourceLocation",
                        "fallbackUsed",
                        "sourceCount",
                        "sources",
                        "totalProfiles",
                        "profileIds",
                        "profiles",
                        "validation");
        Map<String, Object> properties = objectMap(schema.document().get("properties"));
        assertThat(properties)
                .containsKeys(
                        "product",
                        "valid",
                        "activeSourceId",
                        "activeSourceType",
                        "activeSourceLocation",
                        "fallbackUsed",
                        "sourceCount",
                        "sources",
                        "totalProfiles",
                        "profileIds",
                        "profiles",
                        "validation");
        Map<String, Object> sourceItems = objectMap(objectMap(properties.get("sources")).get("items"));
        assertThat(stringList(sourceItems.get("required")))
                .containsExactly(
                        "sourceId",
                        "sourceType",
                        "location",
                        "selected",
                        "fallback",
                        "available",
                        "valid",
                        "profileCount",
                        "issueCount",
                        "message");
        assertThat(objectMap(sourceItems.get("properties")))
                .containsKeys(
                        "sourceId",
                        "sourceType",
                        "location",
                        "selected",
                        "fallback",
                        "available",
                        "valid",
                        "profileCount",
                        "issueCount",
                        "message");
        Map<String, Object> validation = objectMap(properties.get("validation"));
        assertThat(stringList(validation.get("required"))).contains("validationPolicy", "issues");
    }

    @Test
    void sdkExposesJsonSchemaForReadinessProfileRegistryConfigDiagnosticsEnvelope() {
        WayangContractDescriptor descriptor = WayangContractCatalog.discover(
                        WayangContractQuery.of(
                                WayangPlatformContract.SCHEMA,
                                WayangPlatformContract.READINESS_PROFILE_REGISTRY_CONFIG_DIAGNOSTICS))
                .contracts()
                .get(0);

        WayangContractJsonSchema schema = WayangGollekSdk.local().contractJsonSchema(descriptor);

        assertThat(schema.id())
                .isEqualTo(
                        "urn:wayang:contract:wayang.platform.catalog:v1:"
                                + "readiness-profile-registry-config-diagnostics");
        assertThat(schema.contract()).isEqualTo(descriptor);
        assertThat(schema.document())
                .containsEntry("$schema", "https://json-schema.org/draft/2020-12/schema")
                .containsEntry(
                        "$id",
                        "urn:wayang:contract:wayang.platform.catalog:v1:"
                                + "readiness-profile-registry-config-diagnostics")
                .containsEntry("x-wayang-schema", WayangPlatformContract.SCHEMA)
                .containsEntry(
                        "x-wayang-envelope",
                        WayangPlatformContract.READINESS_PROFILE_REGISTRY_CONFIG_DIAGNOSTICS)
                .containsEntry("x-wayang-domain", "platform");
        assertThat(stringList(schema.document().get("required")))
                .containsExactly("product", "valid", "issueCount", "config", "issues");
        Map<String, Object> properties = objectMap(schema.document().get("properties"));
        assertThat(properties).containsKeys("product", "valid", "issueCount", "config", "issues");
        Map<String, Object> config = objectMap(properties.get("config"));
        assertThat(stringList(config.get("required")))
                .containsExactly("mode", "fallbackToBuiltIn", "validationPolicyId");
        assertThat(objectMap(config.get("properties")))
                .containsKeys(
                        "mode",
                        "filePath",
                        "databaseUrl",
                        "objectStorage",
                        "fallbackToBuiltIn",
                        "validationPolicyId");
        Map<String, Object> issueItems = objectMap(objectMap(properties.get("issues")).get("items"));
        assertThat(stringList(issueItems.get("required"))).containsExactly("code", "field", "message");
    }

    @Test
    void sdkExposesJsonSchemaForStandardAlignmentHealthEnvelope() {
        WayangContractDescriptor descriptor = WayangContractCatalog.discover(WayangContractQuery.standardAlignment())
                .contracts()
                .get(0);

        WayangContractJsonSchema schema = WayangGollekSdk.local().contractJsonSchema(descriptor);

        assertThat(schema.id())
                .isEqualTo("urn:wayang:contract:wayang.standard.alignment:v1:standard-alignment-health");
        assertThat(schema.contract()).isEqualTo(descriptor);
        assertThat(schema.document())
                .containsEntry("$schema", "https://json-schema.org/draft/2020-12/schema")
                .containsEntry(
                        "$id",
                        "urn:wayang:contract:wayang.standard.alignment:v1:standard-alignment-health")
                .containsEntry("x-wayang-schema", WayangStandardAlignmentContract.SCHEMA)
                .containsEntry(
                        "x-wayang-envelope",
                        WayangStandardAlignmentContract.STANDARD_ALIGNMENT_HEALTH)
                .containsEntry("x-wayang-domain", "standards");
        assertThat(stringList(schema.document().get("required")))
                .containsExactly("product", "health");

        Map<String, Object> properties = objectMap(schema.document().get("properties"));
        assertThat(properties).containsKeys("product", "health");

        Map<String, Object> health = objectMap(properties.get("health"));
        assertThat(stringList(health.get("required")))
                .containsExactly(
                        "reportId",
                        "status",
                        "ready",
                        "aligned",
                        "standardCount",
                        "gapCount",
                        "standardIds",
                        "gapStandardIds",
                        "portfolio",
                        "policyAssessment",
                        "providerPolicyAssessment",
                        "registryDrift",
                        "registryDriftMode",
                        "providerDiagnostics",
                        "providerCount",
                        "providerIds",
                        "providers",
                        "providerIssueCount",
                        "providerIssues",
                        "recommendations");
        assertThat(objectMap(health.get("properties")))
                .containsKeys(
                        "portfolio",
                        "policyAssessment",
                        "providerPolicyAssessment",
                        "registryDrift",
                        "providerDiagnostics",
                        "providers",
                        "providerIssues");
    }

    @Test
    void sdkExposesJsonSchemaForStandardCatalogEnvelope() {
        WayangContractDescriptor descriptor = WayangContractCatalog.discover(WayangContractQuery.standardCatalog())
                .contracts()
                .get(0);

        WayangContractJsonSchema schema = WayangGollekSdk.local().contractJsonSchema(descriptor);

        assertThat(schema.id())
                .isEqualTo("urn:wayang:contract:wayang.standard.catalog:v1:standards-catalog");
        assertThat(schema.contract()).isEqualTo(descriptor);
        assertThat(schema.document())
                .containsEntry("$schema", "https://json-schema.org/draft/2020-12/schema")
                .containsEntry("$id", "urn:wayang:contract:wayang.standard.catalog:v1:standards-catalog")
                .containsEntry("x-wayang-schema", WayangStandardCatalogContract.SCHEMA)
                .containsEntry("x-wayang-envelope", WayangStandardCatalogContract.STANDARDS_CATALOG)
                .containsEntry("x-wayang-domain", "standards");
        assertThat(stringList(schema.document().get("required")))
                .containsExactly(
                        "product",
                        "totalStandards",
                        "standardIds",
                        "names",
                        "versions",
                        "bindings",
                        "bindingCounts",
                        "specUrls",
                        "standards");
        Map<String, Object> properties = objectMap(schema.document().get("properties"));
        assertThat(properties)
                .containsKeys(
                        "product",
                        "totalStandards",
                        "standardIds",
                        "names",
                        "versions",
                        "bindings",
                        "bindingCounts",
                        "specUrls",
                        "standards");

        Map<String, Object> standardItems = objectMap(objectMap(properties.get("standards")).get("items"));
        assertThat(stringList(standardItems.get("required")))
                .containsExactly(
                        "standardId",
                        "name",
                        "version",
                        "binding",
                        "specUrl",
                        "aliases",
                        "attributes");
        assertThat(objectMap(standardItems.get("properties")))
                .containsKeys("standardId", "name", "version", "binding", "specUrl", "aliases", "attributes");
    }

    @Test
    void sdkExposesJsonSchemaForSkillDiscoveryEnvelope() {
        WayangContractDescriptor descriptor = WayangContractCatalog.discover(
                        WayangContractQuery.of(WayangSkillContract.SCHEMA, WayangSkillContract.SKILL_DISCOVERY))
                .contracts()
                .get(0);

        WayangContractJsonSchema schema = WayangGollekSdk.local().contractJsonSchema(descriptor);

        assertThat(schema.id())
                .isEqualTo("urn:wayang:contract:wayang.skill.catalog:v1:skill-discovery");
        assertThat(schema.contract()).isEqualTo(descriptor);
        assertThat(schema.document())
                .containsEntry("$schema", "https://json-schema.org/draft/2020-12/schema")
                .containsEntry("$id", "urn:wayang:contract:wayang.skill.catalog:v1:skill-discovery")
                .containsEntry("x-wayang-schema", WayangSkillContract.SCHEMA)
                .containsEntry("x-wayang-envelope", WayangSkillContract.SKILL_DISCOVERY)
                .containsEntry("x-wayang-domain", "skills");
        @SuppressWarnings("unchecked")
        java.util.List<String> required = (java.util.List<String>) schema.document().get("required");
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> properties =
                (java.util.Map<String, Object>) schema.document().get("properties");
        assertThat(required)
                .contains(
                        "product",
                        "query",
                        "search",
                        "totalSkills",
                        "matchingSkills",
                        "skillIds",
                        "skills")
                .doesNotContain("contract");
        assertThat(properties)
                .containsKeys(
                        "query",
                        "categorySummaries",
                        "sourceSummaries",
                        "skillIds",
                        "skills");
    }

    @Test
    void sdkExposesJsonSchemaForProviderCapabilityEnvelopes() {
        WayangContractDescriptor discoveryDescriptor = WayangContractCatalog.discover(
                        WayangContractQuery.providerCapability(
                                WayangProviderCapabilityContract.PROVIDER_CAPABILITY_DISCOVERY))
                .contracts()
                .get(0);
        WayangContractDescriptor detailDescriptor = WayangContractCatalog.discover(
                        WayangContractQuery.providerCapability(
                                WayangProviderCapabilityContract.PROVIDER_CAPABILITY_DETAIL))
                .contracts()
                .get(0);

        WayangContractJsonSchema discovery = WayangGollekSdk.local().contractJsonSchema(discoveryDescriptor);
        WayangContractJsonSchema detail = WayangGollekSdk.local().contractJsonSchema(detailDescriptor);

        assertThat(discovery.id())
                .isEqualTo(
                        "urn:wayang:contract:wayang.provider.capability:v1:provider-capability-discovery");
        assertThat(discovery.document())
                .containsEntry("$schema", "https://json-schema.org/draft/2020-12/schema")
                .containsEntry(
                        "$id",
                        "urn:wayang:contract:wayang.provider.capability:v1:provider-capability-discovery")
                .containsEntry("x-wayang-schema", WayangProviderCapabilityContract.SCHEMA)
                .containsEntry(
                        "x-wayang-envelope",
                        WayangProviderCapabilityContract.PROVIDER_CAPABILITY_DISCOVERY)
                .containsEntry("x-wayang-domain", "providers");
        assertThat(stringList(discovery.document().get("required")))
                .contains(
                        "product",
                        "query",
                        "search",
                        "totalCapabilities",
                        "matchingCapabilities",
                        "providerIds",
                        "providerSummaries",
                        "moduleIds",
                        "capabilityTypes",
                        "standardIds",
                        "capabilityIds",
                        "capabilities")
                .doesNotContain("contract");
        Map<String, Object> discoveryProperties = objectMap(discovery.document().get("properties"));
        assertThat(discoveryProperties)
                .containsKeys(
                        "query",
                        "providerIdCounts",
                        "providerSummaries",
                        "capabilityTypeSummaries",
                        "standardSummaries",
                        "capabilities");

        Map<String, Object> query = objectMap(discoveryProperties.get("query"));
        assertThat(stringList(query.get("required")))
                .containsExactly(
                        "capabilityId",
                        "providerId",
                        "providerNamespace",
                        "moduleId",
                        "capabilityType",
                        "state",
                        "surfaceId",
                        "standardId",
                        "tag",
                        "filtered");

        Map<String, Object> capabilityItems = objectMap(objectMap(discoveryProperties.get("capabilities")).get("items"));
        assertThat(stringList(capabilityItems.get("required")))
                .containsExactly(
                        "id",
                        "providerId",
                        "providerNamespace",
                        "moduleId",
                        "capabilityType",
                        "name",
                        "description",
                        "state",
                        "available",
                        "surfaceIds",
                        "standardIds",
                        "tags",
                        "metadata");

        assertThat(detail.id())
                .isEqualTo("urn:wayang:contract:wayang.provider.capability:v1:provider-capability-detail");
        assertThat(stringList(detail.document().get("required")))
                .containsExactly("product", "capabilityId", "capability");
        assertThat(objectMap(detail.document().get("properties")))
                .containsKeys("product", "capabilityId", "capability");
    }

    @Test
    void sdkExposesJsonSchemaForReadinessReportEnvelope() {
        WayangContractDescriptor descriptor = WayangContractCatalog.discover(
                        WayangContractQuery.readiness(WayangReadinessContract.READINESS_REPORT))
                .contracts()
                .get(0);

        WayangContractJsonSchema schema = WayangGollekSdk.local().contractJsonSchema(descriptor);

        assertThat(schema.id())
                .isEqualTo("urn:wayang:contract:wayang.readiness:v1:readiness-report");
        assertThat(schema.contract()).isEqualTo(descriptor);
        assertThat(schema.document())
                .containsEntry("$schema", "https://json-schema.org/draft/2020-12/schema")
                .containsEntry("$id", "urn:wayang:contract:wayang.readiness:v1:readiness-report")
                .containsEntry("x-wayang-schema", WayangReadinessContract.SCHEMA)
                .containsEntry("x-wayang-envelope", WayangReadinessContract.READINESS_REPORT)
                .containsEntry("x-wayang-domain", "readiness");
        List<String> required = stringList(schema.document().get("required"));
        Map<String, Object> properties = objectMap(schema.document().get("properties"));
        assertThat(required)
                .containsExactly(
                        "readinessId",
                        "ready",
                        "exitCode",
                        "issueCount",
                        "probes",
                        "issues",
                        "attributes");
        assertThat(properties)
                .containsKeys("readinessId", "ready", "exitCode", "issueCount", "probes", "issues", "attributes");

        Map<String, Object> probeItems = objectMap(objectMap(properties.get("probes")).get("items"));
        assertThat(stringList(probeItems.get("required")))
                .containsExactly("probe", "required", "passed", "issueCount", "attributes");
        assertThat(objectMap(probeItems.get("properties")))
                .containsKeys("probe", "required", "passed", "issueCount", "attributes");

        Map<String, Object> issueItems = objectMap(objectMap(properties.get("issues")).get("items"));
        assertThat(stringList(issueItems.get("required")))
                .containsExactly("code", "source", "message");
        assertThat(objectMap(issueItems.get("properties")))
                .containsKeys("code", "source", "message", "componentReadinessId");
    }

    @Test
    void sdkExposesJsonSchemaForAggregateReadinessEnvelope() {
        WayangContractDescriptor descriptor = WayangContractCatalog.discover(
                        WayangContractQuery.readiness(WayangReadinessContract.READINESS_AGGREGATE))
                .contracts()
                .get(0);

        WayangContractJsonSchema schema = WayangGollekSdk.local().contractJsonSchema(descriptor);

        assertThat(schema.id())
                .isEqualTo("urn:wayang:contract:wayang.readiness:v1:readiness-aggregate");
        assertThat(schema.contract()).isEqualTo(descriptor);
        assertThat(schema.document())
                .containsEntry("$schema", "https://json-schema.org/draft/2020-12/schema")
                .containsEntry("$id", "urn:wayang:contract:wayang.readiness:v1:readiness-aggregate")
                .containsEntry("x-wayang-schema", WayangReadinessContract.SCHEMA)
                .containsEntry("x-wayang-envelope", WayangReadinessContract.READINESS_AGGREGATE)
                .containsEntry("x-wayang-domain", "readiness");

        Map<String, Object> properties = objectMap(schema.document().get("properties"));
        Map<String, Object> attributes = objectMap(properties.get("attributes"));
        assertThat(stringList(attributes.get("required")))
                .containsExactly(
                        "componentCount",
                        "readyComponentCount",
                        "failedComponentCount",
                        "componentReadinessIds",
                        "failedReadinessIds",
                        "componentSummaries");
        Map<String, Object> attributeProperties = objectMap(attributes.get("properties"));
        assertThat(attributeProperties)
                .containsKeys(
                        "readinessProfileId",
                        "readinessProfileDefault",
                        "readinessProfileProduction",
                        "readinessProfileComponentIds",
                        "componentCount",
                        "readyComponentCount",
                        "failedComponentCount",
                        "componentReadinessIds",
                        "failedReadinessIds",
                        "componentSummaries");

        Map<String, Object> summaryItems =
                objectMap(objectMap(attributeProperties.get("componentSummaries")).get("items"));
        assertThat(stringList(summaryItems.get("required")))
                .containsExactly("readinessId", "ready", "exitCode", "issueCount");
        assertThat(objectMap(summaryItems.get("properties")))
                .containsKeys("readinessId", "ready", "exitCode", "issueCount");
    }

    @Test
    void sdkExposesJsonSchemaForContractCoverageEnvelope() {
        WayangContractDescriptor descriptor = WayangContractCatalog.discover(WayangContractQuery.contractCoverage())
                .contracts()
                .get(0);

        WayangContractJsonSchema schema = WayangGollekSdk.local().contractJsonSchema(descriptor);

        assertThat(schema.id())
                .isEqualTo("urn:wayang:contract:wayang.contract.coverage:v1:contract-command-coverage");
        assertThat(schema.contract()).isEqualTo(descriptor);
        assertThat(schema.document())
                .containsEntry("$schema", "https://json-schema.org/draft/2020-12/schema")
                .containsEntry("$id", "urn:wayang:contract:wayang.contract.coverage:v1:contract-command-coverage")
                .containsEntry("x-wayang-schema", WayangContractCoverageContract.SCHEMA)
                .containsEntry("x-wayang-envelope", WayangContractCoverageContract.CONTRACT_COMMAND_COVERAGE)
                .containsEntry("x-wayang-domain", "contracts");

        assertThat(stringList(schema.document().get("required")))
                .containsExactly(
                        "product",
                        "totalContracts",
                        "totalCommands",
                        "commandLinkedContracts",
                        "commandlessContracts",
                        "incompleteContracts",
                        "commandContractLinks",
                        "commandlessEntries",
                        "incompleteEntries");
        Map<String, Object> properties = objectMap(schema.document().get("properties"));
        assertThat(properties)
                .containsKeys(
                        "product",
                        "totalContracts",
                        "totalCommands",
                        "commandLinkedContracts",
                        "commandlessContracts",
                        "incompleteContracts",
                        "commandContractLinks",
                        "commandlessEntries",
                        "incompleteEntries");

        Map<String, Object> entryItems =
                objectMap(objectMap(properties.get("commandlessEntries")).get("items"));
        assertThat(stringList(entryItems.get("required")))
                .containsExactly(
                        "schema",
                        "version",
                        "envelope",
                        "domain",
                        "jsonSchemaId",
                        "declaredCommandIds",
                        "linkedCommandIds",
                        "unlinkedCommandIds",
                        "undeclaredLinkedCommandIds",
                        "commandLinked",
                        "commandless",
                        "complete");
        assertThat(objectMap(entryItems.get("properties")))
                .containsKeys(
                        "schema",
                        "version",
                        "envelope",
                        "domain",
                        "jsonSchemaId",
                        "declaredCommandIds",
                        "linkedCommandIds",
                        "unlinkedCommandIds",
                        "undeclaredLinkedCommandIds",
                        "commandLinked",
                        "commandless",
                        "complete");
    }

    @Test
    void sdkExposesJsonSchemaBundleForContractDiscovery() {
        WayangContractDiscovery planning = WayangContractCatalog.discover(
                WayangContractQuery.of("wayang.run.planning", null));

        WayangContractJsonSchemaBundle bundle = WayangGollekSdk.local().contractJsonSchemaBundle(planning);

        assertThat(bundle.discovery()).isEqualTo(planning);
        assertThat(bundle.schemas()).hasSize(2);
        assertThat(bundle.schemas())
                .extracting(WayangContractJsonSchema::id)
                .containsExactly(
                        "urn:wayang:contract:wayang.run.planning:v1:run-preflight",
                        "urn:wayang:contract:wayang.run.planning:v1:run-preview");
        assertThat(bundle.ids())
                .containsExactly(
                        "urn:wayang:contract:wayang.run.planning:v1:run-preflight",
                        "urn:wayang:contract:wayang.run.planning:v1:run-preview");
        assertThat(bundle.keys())
                .containsExactly(
                        WayangContractKey.of(
                                AgentRunPlanningContract.SCHEMA,
                                AgentRunPlanningContract.VERSION,
                                AgentRunPlanningContract.RUN_PREFLIGHT),
                        WayangContractKey.of(
                                AgentRunPlanningContract.SCHEMA,
                                AgentRunPlanningContract.VERSION,
                                AgentRunPlanningContract.RUN_PREVIEW));
        assertThat(bundle.schemasById())
                .containsOnlyKeys(
                        "urn:wayang:contract:wayang.run.planning:v1:run-preflight",
                        "urn:wayang:contract:wayang.run.planning:v1:run-preview");
        assertThat(bundle.schemasByKey())
                .containsOnlyKeys(
                        WayangContractKey.of(
                                AgentRunPlanningContract.SCHEMA,
                                AgentRunPlanningContract.VERSION,
                                AgentRunPlanningContract.RUN_PREFLIGHT),
                        WayangContractKey.of(
                                AgentRunPlanningContract.SCHEMA,
                                AgentRunPlanningContract.VERSION,
                                AgentRunPlanningContract.RUN_PREVIEW));
        assertThat(bundle.documentsById().get("urn:wayang:contract:wayang.run.planning:v1:run-preview"))
                .containsEntry("$id", "urn:wayang:contract:wayang.run.planning:v1:run-preview")
                .containsEntry("x-wayang-envelope", "run-preview");
        assertThat(bundle.schemaById(" urn:wayang:contract:wayang.run.planning:v1:run-preview "))
                .hasValueSatisfying(schema -> assertThat(schema.contract().envelope()).isEqualTo("run-preview"));
        assertThat(bundle.schemaByKey(WayangContractKey.of(
                        AgentRunPlanningContract.SCHEMA,
                        AgentRunPlanningContract.VERSION,
                        AgentRunPlanningContract.RUN_PREVIEW)))
                .hasValueSatisfying(schema -> {
                    assertThat(schema.key())
                            .isEqualTo(WayangContractKey.of(
                                    AgentRunPlanningContract.SCHEMA,
                                    AgentRunPlanningContract.VERSION,
                                    AgentRunPlanningContract.RUN_PREVIEW));
                    assertThat(schema.contract().envelope()).isEqualTo("run-preview");
                });
        assertThat(bundle.schemaById("urn:wayang:contract:wayang.run.planning:v1:missing"))
                .isEmpty();
        assertThat(bundle.schemaByKey(WayangContractKey.of(
                        AgentRunPlanningContract.SCHEMA,
                        AgentRunPlanningContract.VERSION,
                        "missing")))
                .isEmpty();
        assertThat(bundle.schemas())
                .last()
                .satisfies(schema -> {
                    assertThat(schema.contract().envelope()).isEqualTo("run-preview");
                    assertThat(schema.document())
                            .containsEntry("$id", "urn:wayang:contract:wayang.run.planning:v1:run-preview")
                            .containsEntry("x-wayang-domain", "planning");
                });
    }

    @Test
    void sdkExposesProviderCapabilityJsonSchemaBundle() {
        WayangContractDiscovery providers = WayangContractCatalog.discover(WayangContractQuery.providerCapability());

        WayangContractJsonSchemaBundle bundle = WayangGollekSdk.local().contractJsonSchemaBundle(providers);

        assertThat(bundle.discovery()).isEqualTo(providers);
        assertThat(bundle.schemas()).hasSize(2);
        assertThat(bundle.ids())
                .containsExactly(
                        "urn:wayang:contract:wayang.provider.capability:v1:provider-capability-discovery",
                        "urn:wayang:contract:wayang.provider.capability:v1:provider-capability-detail");
        assertThat(bundle.keys())
                .containsExactly(
                        WayangContractKey.of(
                                WayangProviderCapabilityContract.SCHEMA,
                                WayangProviderCapabilityContract.VERSION,
                                WayangProviderCapabilityContract.PROVIDER_CAPABILITY_DISCOVERY),
                        WayangContractKey.of(
                                WayangProviderCapabilityContract.SCHEMA,
                                WayangProviderCapabilityContract.VERSION,
                                WayangProviderCapabilityContract.PROVIDER_CAPABILITY_DETAIL));
        assertThat(bundle.schemaById(
                        " urn:wayang:contract:wayang.provider.capability:v1:provider-capability-discovery "))
                .hasValueSatisfying(schema -> {
                    assertThat(schema.contract().domain()).isEqualTo("providers");
                    assertThat(schema.document())
                            .containsEntry(
                                    "$id",
                                    "urn:wayang:contract:wayang.provider.capability:v1:provider-capability-discovery")
                            .containsEntry(
                                    "x-wayang-envelope",
                                    WayangProviderCapabilityContract.PROVIDER_CAPABILITY_DISCOVERY)
                            .containsEntry("x-wayang-domain", "providers");
                    assertThat(stringList(schema.document().get("required")))
                            .contains("product", "query", "capabilities");
                });
        assertThat(bundle.schemaByKey(WayangContractKey.of(
                        WayangProviderCapabilityContract.SCHEMA,
                        WayangProviderCapabilityContract.VERSION,
                        WayangProviderCapabilityContract.PROVIDER_CAPABILITY_DETAIL)))
                .hasValueSatisfying(schema -> {
                    assertThat(schema.contract().commandIds()).containsExactly("providers-inspect-json");
                    assertThat(stringList(schema.document().get("required")))
                            .containsExactly("product", "capabilityId", "capability");
                });
        assertThat(bundle.documentsById())
                .containsOnlyKeys(
                        "urn:wayang:contract:wayang.provider.capability:v1:provider-capability-discovery",
                        "urn:wayang:contract:wayang.provider.capability:v1:provider-capability-detail");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> objectMap(Object value) {
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private static List<String> stringList(Object value) {
        return (List<String>) value;
    }
}
