package tech.kayys.wayang.gollek.sdk;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WayangPlatformReadinessTest {

    @Test
    void aggregatesDefaultSdkProductionReadinessComponents() {
        WayangReadinessReport report = WayangGollekSdk.local().platformReadiness();

        assertThat(report.readinessId()).isEqualTo(WayangPlatformReadiness.READINESS_ID);
        assertThat(report.ready()).isTrue();
        assertThat(report.issueCount()).isZero();
        assertThat(report.probes())
                .extracting(probe -> probe.get("probe"))
                .containsExactly(
                        WayangStorageReadiness.READINESS_ID,
                        WayangPlatformReadiness.CONTRACT_INTEGRITY_READINESS_ID,
                        WayangPlatformReadiness.CONTRACT_COVERAGE_READINESS_ID,
                        WayangPlatformReadiness.SKILL_CATALOG_READINESS_ID,
                        WayangPlatformReadiness.PROVIDER_CAPABILITY_READINESS_ID,
                        WayangPlatformReadiness.STANDARD_ALIGNMENT_READINESS_ID);
        assertThat(report.attributes())
                .containsEntry("componentCount", 6)
                .containsEntry("failedComponentCount", 0)
                .containsEntry("failedReadinessIds", List.of());
    }

    @Test
    void defaultComponentRegistryDefinesAggregateOrder() {
        List<String> expectedReadinessIds = List.of(
                WayangStorageReadiness.READINESS_ID,
                WayangPlatformReadiness.CONTRACT_INTEGRITY_READINESS_ID,
                WayangPlatformReadiness.CONTRACT_COVERAGE_READINESS_ID,
                WayangPlatformReadiness.SKILL_CATALOG_READINESS_ID,
                WayangPlatformReadiness.PROVIDER_CAPABILITY_READINESS_ID,
                WayangPlatformReadiness.STANDARD_ALIGNMENT_READINESS_ID);

        assertThat(WayangPlatformReadinessComponents.defaultReadinessIds())
                .containsExactlyElementsOf(expectedReadinessIds);
        assertThat(WayangPlatformReadinessComponents.defaultComponents())
                .extracting(WayangPlatformReadinessComponent::readinessId)
                .containsExactlyElementsOf(expectedReadinessIds);
    }

    @Test
    void builtInPlatformReadinessProfilesSelectStableComponentSets() {
        assertThat(WayangPlatformReadinessProfiles.profileIds())
                .containsExactly("default", "production", "minimal", "contracts", "catalogs");
        assertThat(WayangPlatformReadinessProfiles.defaultProfile().readinessIds())
                .containsExactlyElementsOf(WayangPlatformReadinessComponents.defaultReadinessIds());
        assertThat(WayangPlatformReadinessProfiles.profile("production").readinessIds())
                .containsExactlyElementsOf(WayangPlatformReadinessComponents.defaultReadinessIds());
        assertThat(WayangPlatformReadinessProfiles.profile(" Production ").readinessIds())
                .containsExactlyElementsOf(WayangPlatformReadinessComponents.defaultReadinessIds());
        assertThat(WayangPlatformReadinessProfiles.profile("minimal").readinessIds())
                .containsExactly(
                        WayangStorageReadiness.READINESS_ID,
                        WayangContractIntegrityReadiness.READINESS_ID);
        assertThat(WayangPlatformReadinessProfiles.profile("contracts").readinessIds())
                .containsExactly(
                        WayangContractIntegrityReadiness.READINESS_ID,
                        WayangContractCoverageReadiness.READINESS_ID,
                        WayangStandardAlignmentReadiness.READINESS_ID);
        assertThat(WayangPlatformReadinessProfiles.profile("catalogs").readinessIds())
                .containsExactly(
                        WayangSkillCatalogReadiness.READINESS_ID,
                        WayangProviderCapabilityReadiness.READINESS_ID,
                        WayangStandardAlignmentReadiness.READINESS_ID);
    }

    @Test
    void readinessProfileCatalogDescribesBuiltInProfiles() {
        List<WayangPlatformReadinessProfileDescriptor> profiles =
                WayangGollekSdk.local().platformReadinessProfiles();

        assertThat(profiles)
                .extracting(WayangPlatformReadinessProfileDescriptor::profileId)
                .containsExactly("default", "production", "minimal", "contracts", "catalogs");
        assertThat(WayangGollekSdk.local().platformReadinessProfile("minimal"))
                .satisfies(profile -> {
                    assertThat(profile.profileId()).isEqualTo("minimal");
                    assertThat(profile.defaultProfile()).isFalse();
                    assertThat(profile.productionProfile()).isFalse();
                    assertThat(profile.componentCount()).isEqualTo(2);
                    assertThat(profile.description()).contains("startup");
                    assertThat(profile.readinessIds())
                            .containsExactly(
                                    WayangStorageReadiness.READINESS_ID,
                                    WayangContractIntegrityReadiness.READINESS_ID);
                });
        assertThat(WayangGollekSdk.local().platformReadinessProfile(" production "))
                .satisfies(profile -> {
                    assertThat(profile.profileId()).isEqualTo("production");
                    assertThat(profile.productionProfile()).isTrue();
                    assertThat(profile.readinessIds())
                            .containsExactlyElementsOf(WayangPlatformReadinessComponents.defaultReadinessIds());
                });
    }

    @Test
    void readinessProfileRegistryResolvesBuiltInSourceByDefault() {
        WayangPlatformReadinessProfileRegistryResolution resolution =
                WayangGollekSdk.local().platformReadinessProfileRegistryResolution();

        assertThat(resolution.valid()).isTrue();
        assertThat(resolution.fallbackUsed()).isFalse();
        assertThat(resolution.activeSourceId()).isEqualTo("builtin");
        assertThat(resolution.activeSourceType()).isEqualTo("builtin");
        assertThat(resolution.totalProfiles()).isEqualTo(5);
        assertThat(resolution.profiles())
                .extracting(WayangPlatformReadinessProfileDescriptor::profileId)
                .containsExactly("default", "production", "minimal", "contracts", "catalogs");
        assertThat(resolution.validation().valid()).isTrue();
        assertThat(resolution.sources())
                .singleElement()
                .satisfies(source -> {
                    assertThat(source.selected()).isTrue();
                    assertThat(source.fallback()).isFalse();
                    assertThat(source.available()).isTrue();
                    assertThat(source.valid()).isTrue();
                    assertThat(source.profileCount()).isEqualTo(5);
                    assertThat(source.issueCount()).isZero();
                });
    }

    @Test
    void readinessProfileRegistryCanResolveFileSource(@TempDir Path tempDir) throws Exception {
        Path catalog = tempDir.resolve("readiness-profiles.properties");
        Files.writeString(
                catalog,
                """
                schema=wayang.platform.readiness-profiles
                version=1
                profileIds=file-default,file-production
                defaultProfileId=file-default
                productionProfileId=file-production
                profile.file-default.description=File-backed startup readiness.
                profile.file-default.readinessIds=wayang.storage.readiness,wayang.contract.integrity.readiness
                profile.file-production.description=File-backed production readiness.
                profile.file-production.readinessIds=wayang.storage.readiness,wayang.contract.integrity.readiness,wayang.contract.coverage.readiness,wayang.skill-catalog.readiness,wayang.provider-capability.readiness,wayang.standard-alignment.readiness
                """);

        WayangPlatformReadinessProfileRegistryResolution resolution =
                WayangGollekSdk.local().platformReadinessProfileRegistryResolution(
                        WayangPlatformReadinessProfileFileSource.of("file-catalog", catalog));

        assertThat(resolution.valid()).isTrue();
        assertThat(resolution.fallbackUsed()).isFalse();
        assertThat(resolution.activeSourceId()).isEqualTo("file-catalog");
        assertThat(resolution.activeSourceType()).isEqualTo("file");
        assertThat(resolution.profiles())
                .extracting(WayangPlatformReadinessProfileDescriptor::profileId)
                .containsExactly("file-default", "file-production");
        assertThat(resolution.profiles())
                .filteredOn(WayangPlatformReadinessProfileDescriptor::defaultProfile)
                .singleElement()
                .extracting(WayangPlatformReadinessProfileDescriptor::profileId)
                .isEqualTo("file-default");
        assertThat(resolution.validation().defaultProfileIds()).containsExactly("file-default");
        assertThat(resolution.validation().productionProfileIds()).containsExactly("file-production");
        assertThat(resolution.validation().coveredReadinessIds())
                .containsExactlyElementsOf(WayangPlatformReadinessComponents.defaultReadinessIds());
        assertThat(resolution.sources())
                .singleElement()
                .satisfies(source -> {
                    assertThat(source.selected()).isTrue();
                    assertThat(source.available()).isTrue();
                    assertThat(source.valid()).isTrue();
                    assertThat(source.profileCount()).isEqualTo(2);
                });
    }

    @Test
    void readinessProfileRegistryFallsBackWhenExternalSourceIsUnavailable(@TempDir Path tempDir) {
        Path missing = tempDir.resolve("missing.properties");

        WayangPlatformReadinessProfileRegistryResolution resolution =
                WayangGollekSdk.local().platformReadinessProfileRegistryResolution(
                        WayangPlatformReadinessProfileFileSource.of("missing-file", missing));

        assertThat(resolution.valid()).isTrue();
        assertThat(resolution.fallbackUsed()).isTrue();
        assertThat(resolution.activeSourceId()).isEqualTo("builtin");
        assertThat(resolution.profiles())
                .extracting(WayangPlatformReadinessProfileDescriptor::profileId)
                .containsExactly("default", "production", "minimal", "contracts", "catalogs");
        assertThat(resolution.sources())
                .hasSize(2)
                .anySatisfy(source -> {
                    assertThat(source.sourceId()).isEqualTo("missing-file");
                    assertThat(source.selected()).isFalse();
                    assertThat(source.available()).isFalse();
                    assertThat(source.valid()).isFalse();
                    assertThat(source.message()).contains("does not exist");
                })
                .anySatisfy(source -> {
                    assertThat(source.sourceId()).isEqualTo("builtin");
                    assertThat(source.selected()).isTrue();
                    assertThat(source.fallback()).isTrue();
                    assertThat(source.available()).isTrue();
                    assertThat(source.valid()).isTrue();
                });
    }

    @Test
    void readinessProfileRegistryFallsBackWhenExternalCatalogFailsStrictValidation(
            @TempDir Path tempDir) throws Exception {
        Path catalog = tempDir.resolve("invalid-readiness-profiles.properties");
        Files.writeString(
                catalog,
                """
                schema=wayang.platform.readiness-profiles
                version=1
                profileIds=custom
                profile.custom.description=Missing required default and production roles.
                profile.custom.readinessIds=wayang.storage.readiness
                """);

        WayangPlatformReadinessProfileRegistryResolution resolution =
                WayangGollekSdk.local().platformReadinessProfileRegistryResolution(
                        WayangPlatformReadinessProfileFileSource.of("invalid-file", catalog));

        assertThat(resolution.valid()).isTrue();
        assertThat(resolution.fallbackUsed()).isTrue();
        assertThat(resolution.activeSourceId()).isEqualTo("builtin");
        assertThat(resolution.sources())
                .hasSize(2)
                .anySatisfy(source -> {
                    assertThat(source.sourceId()).isEqualTo("invalid-file");
                    assertThat(source.available()).isTrue();
                    assertThat(source.valid()).isFalse();
                    assertThat(source.issueCount()).isGreaterThan(0);
                    assertThat(source.selected()).isFalse();
                })
                .anySatisfy(source -> {
                    assertThat(source.sourceId()).isEqualTo("builtin");
                    assertThat(source.selected()).isTrue();
                    assertThat(source.valid()).isTrue();
                });
    }

    @Test
    void readinessProfileValidationChecksCatalogBindings() {
        WayangPlatformReadinessProfileValidationReport report =
                WayangGollekSdk.local().platformReadinessProfileValidation();

        assertThat(report.valid()).isTrue();
        assertThat(report.issueCount()).isZero();
        assertThat(report.totalProfiles()).isEqualTo(5);
        assertThat(report.profileIds())
                .containsExactly("default", "production", "minimal", "contracts", "catalogs");
        assertThat(report.validationPolicy())
                .satisfies(policy -> {
                    assertThat(policy.policyId()).isEqualTo("strict");
                    assertThat(policy.strict()).isTrue();
                    assertThat(policy.knownReadinessCount()).isEqualTo(6);
                    assertThat(policy.requireDefaultProfile()).isTrue();
                    assertThat(policy.requireProductionProfile()).isTrue();
                    assertThat(policy.requireFullReadinessCoverage()).isTrue();
                });
        assertThat(report.defaultProfileIds()).containsExactly("default");
        assertThat(report.defaultProfileCount()).isEqualTo(1);
        assertThat(report.productionProfileIds()).containsExactly("production");
        assertThat(report.productionProfileCount()).isEqualTo(1);
        assertThat(report.knownReadinessIds())
                .containsExactlyElementsOf(WayangPlatformReadinessComponents.defaultReadinessIds());
        assertThat(report.coveredReadinessIds())
                .containsExactlyElementsOf(WayangPlatformReadinessComponents.defaultReadinessIds());
        assertThat(report.coveredReadinessCount()).isEqualTo(6);
        assertThat(report.uncoveredReadinessIds()).isEmpty();
        assertThat(report.uncoveredReadinessCount()).isZero();

        WayangPlatformReadinessProfileValidationReport invalid =
                WayangPlatformReadinessProfileValidation.validate(List.of(
                        new WayangPlatformReadinessProfileDescriptor(
                                "custom",
                                "Custom profile.",
                                false,
                                false,
                                List.of(WayangStorageReadiness.READINESS_ID, "missing.readiness")),
                        new WayangPlatformReadinessProfileDescriptor(
                                "custom",
                                "Duplicate custom profile.",
                                false,
                                false,
                                List.of(WayangContractIntegrityReadiness.READINESS_ID))));

        assertThat(invalid.valid()).isFalse();
        assertThat(invalid.defaultProfileIds()).isEmpty();
        assertThat(invalid.productionProfileIds()).isEmpty();
        assertThat(invalid.uncoveredReadinessIds())
                .contains(
                        WayangContractCoverageReadiness.READINESS_ID,
                        WayangSkillCatalogReadiness.READINESS_ID,
                        WayangProviderCapabilityReadiness.READINESS_ID,
                        WayangStandardAlignmentReadiness.READINESS_ID);
        assertThat(invalid.issues())
                .anySatisfy(issue -> assertThat(issue)
                        .extracting(
                                WayangPlatformReadinessProfileValidationIssue::kind,
                                WayangPlatformReadinessProfileValidationIssue::profileId)
                        .containsExactly("unknown-readiness-component", "custom"))
                .anySatisfy(issue -> assertThat(issue)
                        .extracting(
                                WayangPlatformReadinessProfileValidationIssue::kind,
                                WayangPlatformReadinessProfileValidationIssue::profileId)
                        .containsExactly("duplicate-profile", "custom"))
                .anySatisfy(issue -> assertThat(issue.kind()).isEqualTo("missing-default-profile"))
                .anySatisfy(issue -> assertThat(issue.kind()).isEqualTo("missing-production-profile"))
                .anySatisfy(issue -> assertThat(issue)
                        .extracting(
                                WayangPlatformReadinessProfileValidationIssue::kind,
                                WayangPlatformReadinessProfileValidationIssue::readinessId)
                        .containsExactly(
                                "uncovered-readiness-component",
                                WayangStandardAlignmentReadiness.READINESS_ID));

        WayangPlatformReadinessProfileValidationReport duplicateRoles =
                WayangPlatformReadinessProfileValidation.validate(List.of(
                        new WayangPlatformReadinessProfileDescriptor(
                                "default-a",
                                "First default profile.",
                                true,
                                false,
                                WayangPlatformReadinessComponents.defaultReadinessIds()),
                        new WayangPlatformReadinessProfileDescriptor(
                                "default-b",
                                "Second default and first production profile.",
                                true,
                                true,
                                WayangPlatformReadinessComponents.defaultReadinessIds()),
                        new WayangPlatformReadinessProfileDescriptor(
                                "production-b",
                                "Second production profile.",
                                false,
                                true,
                                WayangPlatformReadinessComponents.defaultReadinessIds())));

        assertThat(duplicateRoles.valid()).isFalse();
        assertThat(duplicateRoles.defaultProfileIds()).containsExactly("default-a", "default-b");
        assertThat(duplicateRoles.productionProfileIds()).containsExactly("default-b", "production-b");
        assertThat(duplicateRoles.uncoveredReadinessIds()).isEmpty();
        assertThat(duplicateRoles.issues())
                .anySatisfy(issue -> assertThat(issue.kind()).isEqualTo("multiple-default-profiles"))
                .anySatisfy(issue -> assertThat(issue.kind()).isEqualTo("multiple-production-profiles"));
    }

    @Test
    void readinessProfileValidationCanUseCustomPolicyForExternalCatalogs() {
        WayangPlatformReadinessProfileValidationPolicy relaxedPolicy =
                WayangPlatformReadinessProfileValidationPolicy.relaxed(List.of(
                        "custom.storage.readiness",
                        "custom.audit.readiness"));
        List<WayangPlatformReadinessProfileDescriptor> profiles = List.of(
                new WayangPlatformReadinessProfileDescriptor(
                        "custom-minimal",
                        "External custom profile.",
                        false,
                        false,
                        List.of("custom.storage.readiness")));

        WayangPlatformReadinessProfileValidationReport relaxed =
                WayangGollekSdk.local().validatePlatformReadinessProfiles(profiles, relaxedPolicy);

        assertThat(relaxed.valid()).isTrue();
        assertThat(relaxed.issueCount()).isZero();
        assertThat(relaxed.validationPolicy())
                .satisfies(policy -> {
                    assertThat(policy.policyId()).isEqualTo("relaxed");
                    assertThat(policy.strict()).isFalse();
                    assertThat(policy.knownReadinessCount()).isEqualTo(2);
                    assertThat(policy.requireDefaultProfile()).isFalse();
                    assertThat(policy.requireProductionProfile()).isFalse();
                    assertThat(policy.requireFullReadinessCoverage()).isFalse();
                });
        assertThat(relaxed.defaultProfileIds()).isEmpty();
        assertThat(relaxed.productionProfileIds()).isEmpty();
        assertThat(relaxed.knownReadinessIds())
                .containsExactly("custom.storage.readiness", "custom.audit.readiness");
        assertThat(relaxed.coveredReadinessIds()).containsExactly("custom.storage.readiness");
        assertThat(relaxed.uncoveredReadinessIds()).containsExactly("custom.audit.readiness");

        WayangPlatformReadinessProfileValidationPolicy coveragePolicy =
                WayangPlatformReadinessProfileValidationPolicy.relaxed(List.of(
                                "custom.storage.readiness",
                                "custom.audit.readiness"))
                        .withFullCoverageRequirement();

        WayangPlatformReadinessProfileValidationReport strictCoverage =
                WayangPlatformReadinessProfileValidation.validate(profiles, coveragePolicy);

        assertThat(strictCoverage.valid()).isFalse();
        assertThat(strictCoverage.validationPolicy())
                .satisfies(policy -> {
                    assertThat(policy.policyId()).isEqualTo("relaxed-with-full-coverage");
                    assertThat(policy.strict()).isFalse();
                    assertThat(policy.knownReadinessCount()).isEqualTo(2);
                    assertThat(policy.requireFullReadinessCoverage()).isTrue();
                });
        assertThat(strictCoverage.issues())
                .anySatisfy(issue -> assertThat(issue)
                        .extracting(
                                WayangPlatformReadinessProfileValidationIssue::kind,
                                WayangPlatformReadinessProfileValidationIssue::readinessId)
                        .containsExactly("uncovered-readiness-component", "custom.audit.readiness"));
    }

    @Test
    void readinessProfileValidationPoliciesResolveNamedBuiltInPolicies() {
        assertThat(WayangGollekSdk.local().platformReadinessProfileValidationPolicyIds())
                .containsExactly(
                        "strict",
                        "relaxed",
                        "strict-without-profile-roles",
                        "strict-without-full-coverage",
                        "relaxed-with-full-coverage");

        assertThat(WayangPlatformReadinessProfileValidationPolicies.policy(" relaxed ").policyId())
                .isEqualTo("relaxed");
        assertThat(WayangPlatformReadinessProfileValidationPolicies.policy("strict-without-full-coverage"))
                .satisfies(policy -> {
                    assertThat(policy.policyId()).isEqualTo("strict-without-full-coverage");
                    assertThat(policy.requireDefaultProfile()).isTrue();
                    assertThat(policy.requireProductionProfile()).isTrue();
                    assertThat(policy.requireFullReadinessCoverage()).isFalse();
                });

        WayangPlatformReadinessProfileValidationReport relaxed =
                WayangGollekSdk.local().platformReadinessProfileValidation("relaxed");

        assertThat(relaxed.valid()).isTrue();
        assertThat(relaxed.validationPolicy().policyId()).isEqualTo("relaxed");
        assertThat(relaxed.validationPolicy().strict()).isFalse();

        assertThatThrownBy(() -> WayangPlatformReadinessProfileValidationPolicies.policy("future"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown platform readiness profile validation policy 'future'.")
                .hasMessageContaining(
                        "Available policies: strict, relaxed, strict-without-profile-roles, "
                                + "strict-without-full-coverage, relaxed-with-full-coverage.");
    }

    @Test
    void readinessProfileValidationPoliciesExposeDescriptors() {
        List<WayangPlatformReadinessProfileValidationPolicyDescriptor> policies =
                WayangGollekSdk.local().platformReadinessProfileValidationPolicies();

        assertThat(policies)
                .extracting(WayangPlatformReadinessProfileValidationPolicyDescriptor::policyId)
                .containsExactly(
                        "strict",
                        "relaxed",
                        "strict-without-profile-roles",
                        "strict-without-full-coverage",
                        "relaxed-with-full-coverage");
        assertThat(policies)
                .filteredOn(WayangPlatformReadinessProfileValidationPolicyDescriptor::defaultPolicy)
                .singleElement()
                .satisfies(policy -> {
                    assertThat(policy.policyId()).isEqualTo("strict");
                    assertThat(policy.strict()).isTrue();
                    assertThat(policy.knownReadinessCount()).isEqualTo(6);
                    assertThat(policy.requireDefaultProfile()).isTrue();
                    assertThat(policy.requireProductionProfile()).isTrue();
                    assertThat(policy.requireFullReadinessCoverage()).isTrue();
                    assertThat(policy.description()).contains("production");
                });
        assertThat(WayangGollekSdk.local().platformReadinessProfileValidationPolicy("relaxed"))
                .satisfies(policy -> {
                    assertThat(policy.policyId()).isEqualTo("relaxed");
                    assertThat(policy.strict()).isFalse();
                    assertThat(policy.defaultPolicy()).isFalse();
                    assertThat(policy.requireFullReadinessCoverage()).isFalse();
                });
    }

    @Test
    void readinessProfileValidationPolicyRejectsInvalidKnownReadinessIds() {
        assertThatThrownBy(() -> WayangPlatformReadinessProfileValidationPolicy.relaxed(List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("At least one known readiness component id is required.");
        assertThatThrownBy(() -> WayangPlatformReadinessProfileValidationPolicy.relaxed(List.of("known", " known ")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duplicate known readiness component id 'known'.");
    }

    @Test
    void platformReadinessCanAssessSelectedProfile() {
        WayangReadinessReport report = WayangGollekSdk.local().platformReadiness("minimal");

        assertThat(report.ready()).isTrue();
        assertThat(report.attributes())
                .containsEntry("readinessProfileId", "minimal")
                .containsEntry("readinessProfileDefault", false)
                .containsEntry("readinessProfileProduction", false)
                .containsEntry("readinessProfileComponentIds", List.of(
                        WayangStorageReadiness.READINESS_ID,
                        WayangContractIntegrityReadiness.READINESS_ID))
                .containsEntry("componentCount", 2)
                .containsEntry("failedComponentCount", 0);
        assertThat(report.probes())
                .extracting(probe -> probe.get("probe"))
                .containsExactly(
                        WayangStorageReadiness.READINESS_ID,
                        WayangContractIntegrityReadiness.READINESS_ID);
    }

    @Test
    void rejectsUnknownPlatformReadinessProfilesAndProfileComponents() {
        assertThatThrownBy(() -> WayangPlatformReadinessProfiles.profile("unknown-profile"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown platform readiness profile 'unknown-profile'.")
                .hasMessageContaining("Available profiles: default, production, minimal, contracts, catalogs.");
        assertThatThrownBy(() -> WayangPlatformReadinessComponents.componentsFor(
                WayangPlatformReadinessProfile.of("custom", List.of("missing.readiness"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown platform readiness component id 'missing.readiness' for profile 'custom'.");
    }

    @Test
    void defaultComponentRegistryAssessesReportsInOrder() {
        List<WayangReadinessReport> reports = WayangPlatformReadinessComponents.assessDefault(
                WayangGollekSdk.local());

        assertThat(reports)
                .extracting(WayangReadinessReport::readinessId)
                .containsExactlyElementsOf(WayangPlatformReadinessComponents.defaultReadinessIds());
        assertThat(reports).allSatisfy(report -> assertThat(report.ready()).isTrue());
    }

    @Test
    void isolatesRuntimeFailuresDuringPlatformReadinessAssessment() {
        WayangPlatformReadinessComponent failing = WayangPlatformReadinessComponents.component(
                "failing.readiness",
                ignored -> {
                    throw new IllegalStateException("backend unavailable");
                });
        WayangPlatformReadinessComponent ready = WayangPlatformReadinessComponents.component(
                "ready.readiness",
                ignored -> readinessReport("ready.readiness"));

        List<WayangReadinessReport> reports = WayangPlatformReadinessComponents.assess(
                List.of(failing, ready),
                WayangGollekSdk.local());

        assertThat(reports)
                .extracting(WayangReadinessReport::readinessId)
                .containsExactly("failing.readiness", "ready.readiness");
        assertThat(reports.get(0).ready()).isFalse();
        assertThat(reports.get(0).exitCode()).isEqualTo(WayangReadinessReports.EXIT_FAILURE);
        assertThat(reports.get(0).attributes())
                .containsEntry("componentReadinessId", "failing.readiness")
                .containsEntry("assessmentStatus", "failed")
                .containsEntry("exceptionType", IllegalStateException.class.getName())
                .containsEntry("exceptionMessage", "backend unavailable");
        assertThat(reports.get(0).probes())
                .singleElement()
                .satisfies(probe -> assertThat(probe)
                        .containsEntry("probe", "failing.readiness.execution")
                        .containsEntry("passed", false)
                        .containsEntry("issueCount", 1));
        assertThat(reports.get(0).issues())
                .singleElement()
                .satisfies(issue -> assertThat(issue)
                        .containsEntry("code", "platform_readiness_component_failed")
                        .containsEntry("source", "platform-readiness")
                        .containsEntry("componentReadinessId", "failing.readiness")
                        .containsEntry("assessmentStatus", "failed")
                        .containsEntry("exceptionType", IllegalStateException.class.getName())
                        .containsEntry("exceptionMessage", "backend unavailable"));
        assertThat(reports.get(1).ready()).isTrue();
    }

    @Test
    void canIncludePlatformReadinessExecutionDurationDiagnostics() {
        WayangPlatformReadinessComponent component = WayangPlatformReadinessComponents.component(
                "timed.readiness",
                ignored -> readinessReport("timed.readiness"));

        WayangReadinessReport report = WayangPlatformReadinessExecution.assessSafely(
                component,
                WayangGollekSdk.local(),
                true);

        assertThat(report.attributes())
                .containsEntry("componentReadinessId", "timed.readiness")
                .containsEntry("assessmentStatus", "completed")
                .containsKey("durationMillis");
        assertThat(report.attributes().get("durationMillis"))
                .isInstanceOf(Number.class)
                .satisfies(value -> assertThat(((Number) value).longValue()).isNotNegative());
    }

    @Test
    void rejectsDuplicatePlatformReadinessComponentIds() {
        WayangPlatformReadinessComponent component = WayangPlatformReadinessComponents.component(
                "duplicate.readiness",
                ignored -> readinessReport("duplicate.readiness"));

        assertThatThrownBy(() -> WayangPlatformReadinessComponents.components(component, component))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duplicate platform readiness component id 'duplicate.readiness'");
    }

    @Test
    void rejectsEmptyPlatformReadinessAssessmentRegistry() {
        assertThatThrownBy(() -> WayangPlatformReadinessComponents.assess(List.of(), WayangGollekSdk.local()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("At least one platform readiness component is required.");
        assertThatThrownBy(() -> WayangPlatformReadinessComponents.assess(null, WayangGollekSdk.local()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("At least one platform readiness component is required.");
    }

    @Test
    void rejectsPlatformReadinessComponentWithoutReport() {
        WayangPlatformReadinessComponent component = WayangPlatformReadinessComponents.component(
                "null-report.readiness",
                ignored -> null);

        assertThatThrownBy(() -> component.assess(WayangGollekSdk.local()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Platform readiness component 'null-report.readiness' returned no readiness report.");
    }

    @Test
    void rejectsMiswiredPlatformReadinessComponentsDuringAggregateAssessment() {
        WayangPlatformReadinessComponent nullReport = WayangPlatformReadinessComponents.component(
                "null-report.readiness",
                ignored -> null);
        WayangPlatformReadinessComponent mismatch = WayangPlatformReadinessComponents.component(
                "expected.readiness",
                ignored -> readinessReport("actual.readiness"));

        assertThatThrownBy(() -> WayangPlatformReadinessComponents.assess(
                List.of(nullReport),
                WayangGollekSdk.local()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Platform readiness component 'null-report.readiness' returned no readiness report.");
        assertThatThrownBy(() -> WayangPlatformReadinessComponents.assess(
                List.of(mismatch),
                WayangGollekSdk.local()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Platform readiness component 'expected.readiness' returned readiness report 'actual.readiness'.");
    }

    @Test
    void rejectsPlatformReadinessComponentReportIdMismatch() {
        WayangPlatformReadinessComponent component = WayangPlatformReadinessComponents.component(
                "expected.readiness",
                ignored -> readinessReport("actual.readiness"));

        assertThatThrownBy(() -> component.assess(WayangGollekSdk.local()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Platform readiness component 'expected.readiness' returned readiness report 'actual.readiness'.");
    }

    @Test
    void surfacesStorageFailuresInPlatformReadiness() {
        WayangGollekSdk sdk = new LocalWayangGollekSdk(WayangGollekSdkConfig.local()
                .withStorage(WayangStorageConfig.fromMap(Map.of(
                        "backend", "rustfs",
                        "bucket", "wayang-runs"))));

        WayangReadinessReport report = sdk.platformReadiness();

        assertThat(report.ready()).isFalse();
        assertThat(report.issues())
                .extracting(issue -> issue.get("code"))
                .contains("storage_object_endpoint_missing", "storage_file_fallback_missing");
        assertThat(objectList(report.attributes().get("componentSummaries")))
                .anySatisfy(summary -> assertThat(summary)
                        .containsEntry("readinessId", WayangStorageReadiness.READINESS_ID)
                        .containsEntry("ready", false));
    }

    @Test
    void reportsContractIntegrityReadiness() {
        WayangContractIntegrityReport integrity = WayangGollekSdk.local().contractIntegrity();

        WayangReadinessReport report = WayangContractIntegrityReadiness.assess(integrity);

        assertThat(report.ready()).isTrue();
        assertThat(report.readinessId()).isEqualTo(WayangContractIntegrityReadiness.READINESS_ID);
        assertThat(WayangPlatformReadiness.CONTRACT_INTEGRITY_READINESS_ID)
                .isEqualTo(WayangContractIntegrityReadiness.READINESS_ID);
        assertThat(report.attributes())
                .containsEntry("totalContracts", integrity.totalContracts())
                .containsKey("contractCommandLinks")
                .containsKey("commandContractLinks");
    }

    @Test
    void reportsContractCoverageGapsAsReadinessIssues() {
        WayangContractDescriptor descriptor = WayangContractDescriptors.lifecycle(
                AgentRunLifecycleContract.RUN_STATUS,
                "Run status",
                List.of("run-status-json", "missing-command"),
                "run status <run-id> --json");
        WayangContractCommandCoverageReport coverage = new WayangContractCommandCoverageReport(
                1,
                1,
                List.of(new WayangContractCommandCoverageEntry(
                        descriptor,
                        descriptor.commandIds(),
                        List.of("run-status-json", "extra-command"))));

        WayangReadinessReport report = WayangContractCoverageReadiness.assess(coverage);

        assertThat(report.ready()).isFalse();
        assertThat(report.readinessId()).isEqualTo(WayangContractCoverageReadiness.READINESS_ID);
        assertThat(WayangPlatformReadiness.CONTRACT_COVERAGE_READINESS_ID)
                .isEqualTo(WayangContractCoverageReadiness.READINESS_ID);
        assertThat(report.issues())
                .singleElement()
                .satisfies(issue -> assertThat(issue)
                        .containsEntry("code", "contract_command_coverage_incomplete")
                        .containsEntry("source", "contracts")
                        .containsEntry("unlinkedCommandIds", List.of("missing-command"))
                        .containsEntry("undeclaredLinkedCommandIds", List.of("extra-command")));
    }

    @Test
    void reportsStandardAlignmentReadiness() {
        WayangReadinessReport report = WayangStandardAlignmentReadiness.assess(
                WayangGollekSdk.local().standardAlignmentHealth());

        assertThat(report.ready()).isTrue();
        assertThat(report.readinessId()).isEqualTo(WayangStandardAlignmentReadiness.READINESS_ID);
        assertThat(WayangPlatformReadiness.STANDARD_ALIGNMENT_READINESS_ID)
                .isEqualTo(WayangStandardAlignmentReadiness.READINESS_ID);
        assertThat(report.attributes())
                .containsEntry("status", "ready")
                .containsEntry("blocked", false)
                .containsEntry("warning", false);
    }

    @Test
    void reportsSkillCatalogReadiness() {
        AgentSkillDiscovery discovery = WayangGollekSdk.local()
                .skillDiscovery(AgentSkillQuery.all());

        WayangReadinessReport report = WayangSkillCatalogReadiness.assess(discovery);

        assertThat(report.ready()).isTrue();
        assertThat(report.readinessId()).isEqualTo(WayangSkillCatalogReadiness.READINESS_ID);
        assertThat(WayangPlatformReadiness.SKILL_CATALOG_READINESS_ID)
                .isEqualTo(WayangSkillCatalogReadiness.READINESS_ID);
        assertThat(report.attributes())
                .containsEntry("totalSkills", WayangSkillCatalog.defaultSkills().size())
                .containsEntry("matchingSkills", WayangSkillCatalog.defaultSkills().size())
                .containsEntry("availableSkills", WayangSkillCatalog.defaultAvailableSkillCount())
                .containsEntry("categories", discovery.categories())
                .containsEntry("sources", discovery.sources());
        assertThat(stringList(report.attributes().get("availableSkillIds")))
                .contains("rag.retrieve", "mcp.bridge", "workflow.gamelan");
        assertThat(stringList(report.attributes().get("surfaceIds")))
                .contains("coding-agent", "assistant-agent", "workflow-platform", "platform-admin");
    }

    @Test
    void reportsEmptySkillCatalogAsReadinessIssue() {
        WayangReadinessReport report = WayangSkillCatalogReadiness.assess(
                AgentSkillDiscovery.of(AgentSkillQuery.all(), "", List.of(), 0));

        assertThat(report.ready()).isFalse();
        assertThat(report.issueCount()).isEqualTo(1);
        assertThat(report.issues())
                .singleElement()
                .satisfies(issue -> assertThat(issue)
                        .containsEntry("code", "skill_catalog_empty")
                        .containsEntry("source", "skills")
                        .containsEntry("totalSkills", 0)
                        .containsEntry("matchingSkills", 0));
    }

    @Test
    void reportsProviderCapabilityCatalogReadiness() {
        WayangProviderCapabilityDiscovery discovery = WayangGollekSdk.local()
                .providerCapabilityDiscovery(WayangProviderCapabilityQuery.all());

        WayangReadinessReport report = WayangProviderCapabilityReadiness.assess(discovery);

        assertThat(report.ready()).isTrue();
        assertThat(report.readinessId()).isEqualTo(WayangProviderCapabilityReadiness.READINESS_ID);
        assertThat(WayangPlatformReadiness.PROVIDER_CAPABILITY_READINESS_ID)
                .isEqualTo(WayangProviderCapabilityReadiness.READINESS_ID);
        assertThat(report.attributes())
                .containsEntry("totalCapabilities", WayangProviderCapabilityCatalog.defaultCapabilities().size())
                .containsEntry("matchingCapabilities", WayangProviderCapabilityCatalog.defaultCapabilities().size())
                .containsEntry("availableCapabilities", WayangProviderCapabilityCatalog.defaultAvailableCapabilityCount())
                .containsEntry("providerIds", discovery.providerIds())
                .containsEntry("moduleIds", discovery.moduleIds())
                .containsEntry("standardIds", discovery.standardIds());
        assertThat(stringList(report.attributes().get("availableCapabilityIds")))
                .contains("skills.dynamic", "a2a.alignment", "runtime.lifecycle");
    }

    @Test
    void reportsEmptyProviderCapabilityCatalogAsReadinessIssue() {
        WayangReadinessReport report = WayangProviderCapabilityReadiness.assess(
                WayangProviderCapabilityDiscovery.of(WayangProviderCapabilityQuery.all(), "", List.of(), 0));

        assertThat(report.ready()).isFalse();
        assertThat(report.issueCount()).isEqualTo(1);
        assertThat(report.issues())
                .singleElement()
                .satisfies(issue -> assertThat(issue)
                        .containsEntry("code", "provider_capability_catalog_empty")
                        .containsEntry("source", "providers")
                        .containsEntry("totalCapabilities", 0)
                        .containsEntry("matchingCapabilities", 0));
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> objectList(Object value) {
        assertThat(value).isInstanceOf(List.class);
        return (List<Map<String, Object>>) value;
    }

    @SuppressWarnings("unchecked")
    private static List<String> stringList(Object value) {
        assertThat(value).isInstanceOf(List.class);
        return (List<String>) value;
    }

    private static WayangReadinessReport readinessReport(String readinessId) {
        return WayangReadinessReport.from(
                readinessId,
                true,
                0,
                0,
                List.of(),
                List.of(),
                Map.of());
    }
}
