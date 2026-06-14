package tech.kayys.wayang.gollek.sdk;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WayangContractCommandCoverageTest {

    @Test
    void reportsDefaultCommandCoverageWithoutTreatingCommandlessContractsAsBroken() {
        WayangContractCommandCoverageReport coverage = WayangContractCommandCoverage.defaultCoverage();
        WayangContractIntegrityReport integrity = WayangContractIntegrity.validateDefault();

        assertThat(coverage.totalContracts()).isEqualTo(WayangContractCatalog.defaultContracts().size());
        assertThat(coverage.totalCommands()).isEqualTo(WayangWorkbenchCatalog.localCommands().size());
        assertThat(coverage.commandLinkedContracts()).isEqualTo(coverage.commandLinkedEntries().size());
        assertThat(coverage.commandlessContracts()).isEqualTo(coverage.commandlessEntries().size());
        assertThat(coverage.incompleteContracts()).isZero();
        assertThat(coverage.commandContractLinks()).isEqualTo(integrity.commandContractLinks());
        assertThat(coverage.commandlessEntries())
                .extracting(WayangContractCommandCoverageEntry::key)
                .containsExactly(WayangContractKey.of(
                        WayangReadinessContract.SCHEMA,
                        WayangReadinessContract.VERSION,
                        WayangReadinessContract.READINESS_REPORT));
        assertThat(coverage.entryForKey(WayangContractKey.of(
                        WayangContractCoverageContract.SCHEMA,
                        WayangContractCoverageContract.VERSION,
                        WayangContractCoverageContract.CONTRACT_COMMAND_COVERAGE)))
                .hasValueSatisfying(entry -> {
                    assertThat(entry.commandLinked()).isTrue();
                    assertThat(entry.commandless()).isFalse();
                    assertThat(entry.declaredCommandIds()).containsExactly("contracts-coverage-json");
                    assertThat(entry.linkedCommandIds()).containsExactly("contracts-coverage-json");
                });
        assertThat(coverage.entryForKey(WayangContractKey.of(
                        WayangStandardAlignmentContract.SCHEMA,
                        WayangStandardAlignmentContract.VERSION,
                        WayangStandardAlignmentContract.STANDARD_ALIGNMENT_HEALTH)))
                .hasValueSatisfying(entry -> {
                    assertThat(entry.commandLinked()).isTrue();
                    assertThat(entry.commandless()).isFalse();
                    assertThat(entry.declaredCommandIds()).containsExactly("standards-health-json");
                    assertThat(entry.linkedCommandIds()).containsExactly("standards-health-json");
                });
        assertThat(coverage.entryForKey(WayangContractKey.of(
                        WayangStandardCatalogContract.SCHEMA,
                        WayangStandardCatalogContract.VERSION,
                        WayangStandardCatalogContract.STANDARDS_CATALOG)))
                .hasValueSatisfying(entry -> {
                    assertThat(entry.commandLinked()).isTrue();
                    assertThat(entry.commandless()).isFalse();
                    assertThat(entry.declaredCommandIds()).containsExactly("standards-catalog-json");
                    assertThat(entry.linkedCommandIds()).containsExactly("standards-catalog-json");
                });
        assertThat(coverage.entryForKey(WayangContractKey.of(
                        WayangPlatformContract.SCHEMA,
                        WayangPlatformContract.VERSION,
                        WayangPlatformContract.READINESS_PROFILE_VALIDATION_POLICY_LIST)))
                .hasValueSatisfying(entry -> {
                    assertThat(entry.commandLinked()).isTrue();
                    assertThat(entry.commandless()).isFalse();
                    assertThat(entry.declaredCommandIds()).containsExactly("readiness-profiles-policies-json");
                    assertThat(entry.linkedCommandIds()).containsExactly("readiness-profiles-policies-json");
                });
        assertThat(coverage.entryForKey(WayangContractKey.of(
                        WayangPlatformContract.SCHEMA,
                        WayangPlatformContract.VERSION,
                        WayangPlatformContract.READINESS_PROFILE_REGISTRY_CONFIG_DIAGNOSTICS)))
                .hasValueSatisfying(entry -> {
                    assertThat(entry.commandLinked()).isTrue();
                    assertThat(entry.commandless()).isFalse();
                    assertThat(entry.declaredCommandIds()).containsExactly("readiness-profiles-config-json");
                    assertThat(entry.linkedCommandIds()).containsExactly("readiness-profiles-config-json");
                });
        assertThat(coverage.entryForKey(WayangContractKey.of(
                        WayangPlatformContract.SCHEMA,
                        WayangPlatformContract.VERSION,
                        WayangPlatformContract.READINESS_PROFILE_REGISTRY_RESOLUTION)))
                .hasValueSatisfying(entry -> {
                    assertThat(entry.commandLinked()).isTrue();
                    assertThat(entry.commandless()).isFalse();
                    assertThat(entry.declaredCommandIds()).containsExactly("readiness-profiles-sources-json");
                    assertThat(entry.linkedCommandIds()).containsExactly("readiness-profiles-sources-json");
                });
    }

    @Test
    void exposesLookupForCommandLinkedAndCommandlessContracts() {
        WayangContractCommandCoverageReport coverage = WayangGollekSdk.local().contractCommandCoverage();
        WayangContractKey previewKey = WayangContractKey.of(
                AgentRunPlanningContract.SCHEMA,
                AgentRunPlanningContract.VERSION,
                AgentRunPlanningContract.RUN_PREVIEW);
        WayangContractKey readinessKey = WayangContractKey.of(
                WayangReadinessContract.SCHEMA,
                WayangReadinessContract.VERSION,
                WayangReadinessContract.READINESS_AGGREGATE);

        assertThat(coverage.entryForKey(previewKey))
                .hasValueSatisfying(entry -> {
                    assertThat(entry.commandLinked()).isTrue();
                    assertThat(entry.commandless()).isFalse();
                    assertThat(entry.declaredCommandIds()).containsExactly("run-dry-json", "run-spec-dry-json");
                    assertThat(entry.linkedCommandIds()).containsExactly("run-dry-json", "run-spec-dry-json");
                    assertThat(entry.unlinkedCommandIds()).isEmpty();
                    assertThat(entry.undeclaredLinkedCommandIds()).isEmpty();
                });
        assertThat(coverage.entryForJsonSchemaId(readinessKey.jsonSchemaId()))
                .hasValueSatisfying(entry -> {
                    assertThat(entry.schema()).isEqualTo(WayangReadinessContract.SCHEMA);
                    assertThat(entry.envelope()).isEqualTo(WayangReadinessContract.READINESS_AGGREGATE);
                    assertThat(entry.commandLinked()).isTrue();
                    assertThat(entry.commandless()).isFalse();
                    assertThat(entry.declaredCommandIds())
                            .containsExactly("status-readiness-json", "status-readiness-profile-json");
                    assertThat(entry.linkedCommandIds())
                            .containsExactly("status-readiness-json", "status-readiness-profile-json");
                    assertThat(entry.unlinkedCommandIds()).isEmpty();
                    assertThat(entry.undeclaredLinkedCommandIds()).isEmpty();
                });
    }

    @Test
    void surfacesIncompleteCoverageWhenDescriptorsAndCommandsDrift() {
        WayangContractDescriptor contract = WayangContractDescriptors.lifecycle(
                AgentRunLifecycleContract.RUN_STATUS,
                "Status",
                List.of("run-status-json", "missing-command"),
                "run status <run-id> --json");
        WorkbenchCommand command = WorkbenchCommand.shared(
                "run-status-json",
                "Run Status JSON",
                "run status <run-id> --json",
                "Runs",
                "Render status.",
                List.of(),
                List.of(WorkbenchCommandContract.lifecycle(AgentRunLifecycleContract.RUN_STATUS)));
        WorkbenchCommand extraCommand = WorkbenchCommand.shared(
                "run-status-extra-json",
                "Run Status Extra JSON",
                "run status <run-id> --extra --json",
                "Runs",
                "Render status with an undeclared command link.",
                List.of(),
                List.of(WorkbenchCommandContract.lifecycle(AgentRunLifecycleContract.RUN_STATUS)));

        WayangContractCommandCoverageReport coverage =
                WayangContractCommandCoverage.of(List.of(contract), List.of(command, extraCommand));

        assertThat(coverage.incompleteContracts()).isEqualTo(1);
        assertThat(coverage.incompleteEntries())
                .singleElement()
                .satisfies(entry -> {
                    assertThat(entry.unlinkedCommandIds()).containsExactly("missing-command");
                    assertThat(entry.undeclaredLinkedCommandIds()).containsExactly("run-status-extra-json");
                    assertThat(entry.complete()).isFalse();
                });
    }
}
