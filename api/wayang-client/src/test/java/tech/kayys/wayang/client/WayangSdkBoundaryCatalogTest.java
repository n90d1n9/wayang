package tech.kayys.wayang.client;

import org.junit.jupiter.api.Test;

import tech.kayys.wayang.agent.lifecycle.AgentRunLifecycleContract;
import tech.kayys.wayang.boundry.WayangSdkBoundary;
import tech.kayys.wayang.boundry.WayangSdkBoundaryCatalog;
import tech.kayys.wayang.boundry.WayangSdkBoundaryCatalogValidationIssue;
import tech.kayys.wayang.boundry.WayangSdkBoundaryCatalogValidationReport;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WayangSdkBoundaryCatalogTest {

    @Test
    void exposesStableBoundaryOrderForFuturePackageSplit() {
        assertThat(WayangSdkBoundaryCatalog.knownBoundaryIds())
                .containsExactly(
                        "core",
                        "run",
                        "context",
                        "capability",
                        "platform",
                        "contract",
                        "workbench",
                        "storage",
                        "remote");

        assertThat(WayangSdkBoundaryCatalog.defaultBoundaries())
                .extracting(WayangSdkBoundary::intendedPackage)
                .containsExactly(
                        "tech.kayys.wayang.gollek.client.core",
                        "tech.kayys.wayang.gollek.client.run",
                        "tech.kayys.wayang.gollek.client.context",
                        "tech.kayys.wayang.gollek.client.capability",
                        "tech.kayys.wayang.gollek.client.platform",
                        "tech.kayys.wayang.gollek.client.contract",
                        "tech.kayys.wayang.gollek.client.workbench",
                        "tech.kayys.wayang.gollek.client.storage",
                        "tech.kayys.wayang.gollek.client.remote");
    }

    @Test
    void resolvesClassOwnershipWithoutMovingPublicImports() {
        assertThat(WayangSdkBoundaryCatalog.boundaryForClassName("AgentRunRequest"))
                .hasValueSatisfying(boundary -> assertThat(boundary.id()).isEqualTo("run"));
        assertThat(WayangSdkBoundaryCatalog.boundaryForClassName("WorkspaceSnapshot"))
                .hasValueSatisfying(boundary -> assertThat(boundary.id()).isEqualTo("context"));
        assertThat(WayangSdkBoundaryCatalog.boundaryForClassName("WayangSkillCatalog"))
                .hasValueSatisfying(boundary -> assertThat(boundary.id()).isEqualTo("capability"));
        assertThat(WayangSdkBoundaryCatalog.boundaryForClassName("WayangWorkbenchModel"))
                .hasValueSatisfying(boundary -> assertThat(boundary.id()).isEqualTo("workbench"));
        assertThat(WayangSdkBoundaryCatalog.boundaryForClassName("WayangContractCatalog"))
                .hasValueSatisfying(boundary -> assertThat(boundary.id()).isEqualTo("contract"));
        assertThat(WayangSdkBoundaryCatalog.boundaryForClassName("RemoteWayangGollekSdk"))
                .hasValueSatisfying(boundary -> assertThat(boundary.id()).isEqualTo("remote"));
        assertThat(WayangSdkBoundaryCatalog.boundaryForClassName("FutureThing")).isEmpty();
    }

    @Test
    void resolvesClassOwnershipByMostSpecificPrefix() {
        assertThat(WayangSdkBoundaryCatalog.boundaryForClassName("WayangPlatformStatus"))
                .hasValueSatisfying(boundary -> assertThat(boundary.id()).isEqualTo("platform"));
        assertThat(WayangSdkBoundaryCatalog.boundaryForClassName("WayangPlatformContract"))
                .hasValueSatisfying(boundary -> assertThat(boundary.id()).isEqualTo("contract"));
        assertThat(WayangSdkBoundaryCatalog.boundaryForClassName("WayangReadinessContract"))
                .hasValueSatisfying(boundary -> assertThat(boundary.id()).isEqualTo("contract"));
    }

    @Test
    void resolvesContractSchemaOwnership() {
        assertThat(WayangSdkBoundaryCatalog.boundaryForContractSchema(AgentRunLifecycleContract.SCHEMA))
                .hasValueSatisfying(boundary -> assertThat(boundary.id()).isEqualTo("run"));
        assertThat(WayangSdkBoundaryCatalog.require("contract").ownsContractSchema(WayangWorkbenchContract.SCHEMA))
                .isTrue();
        assertThat(WayangSdkBoundaryCatalog.boundaryForContractSchema("unknown.schema")).isEmpty();
    }

    @Test
    void normalizesAndRejectsUnknownBoundaries() {
        assertThat(WayangSdkBoundaryCatalog.normalizeBoundaryId(" "))
                .isEqualTo(WayangSdkBoundaryCatalog.DEFAULT_BOUNDARY_ID);
        assertThat(WayangSdkBoundaryCatalog.require(" run ").name())
                .isEqualTo("Run Lifecycle");
        assertThat(WayangSdkBoundaryCatalog.intendedPackage("storage"))
                .isEqualTo("tech.kayys.wayang.gollek.client.storage");

        assertThatThrownBy(() -> WayangSdkBoundaryCatalog.require("future"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown Wayang SDK boundary 'future'")
                .hasMessageContaining("core")
                .hasMessageContaining("remote");
    }

    @Test
    void boundaryValuesAreNormalizedAndImmutable() {
        WayangSdkBoundary boundary = new WayangSdkBoundary(
                " demo ",
                " Demo ",
                " tech.kayys.wayang.gollek.client.demo ",
                " Demo boundary ",
                List.of(" DemoClass ", "", "DemoClass"),
                List.of(" wayang.demo ", ""),
                List.of(" core ", ""));

        assertThat(boundary.id()).isEqualTo("demo");
        assertThat(boundary.classPrefixes()).containsExactly("DemoClass");
        assertThat(boundary.contractSchemas()).containsExactly("wayang.demo");
        assertThat(boundary.dependsOn()).containsExactly("core");
        assertThatThrownBy(() -> boundary.classPrefixes().add("Other"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void validatesDefaultBoundaryCatalog() {
        WayangSdkBoundaryCatalogValidationReport report = WayangSdkBoundaryCatalog.validateDefault();

        assertThat(report.valid()).isTrue();
        assertThat(report.issueCount()).isZero();
        assertThat(report.totalBoundaries()).isEqualTo(9);
        assertThat(report.boundaryIds()).contains("core", "run", "remote");
        assertThat(report.intendedPackages()).contains("tech.kayys.wayang.gollek.client.contract");
        assertThat(report.classPrefixes()).contains("WayangPlatformContract", "Remote");
        assertThat(report.contractSchemas()).contains(WayangWorkbenchContract.SCHEMA);
    }

    @Test
    void reportsBoundaryCatalogDrift() {
        WayangSdkBoundary core = boundary(
                "core",
                "Core",
                ".core",
                List.of("Core"),
                List.of(),
                List.of());
        WayangSdkBoundary duplicateCore = boundary(
                "core",
                "Duplicate Core",
                ".core",
                List.of("DuplicateCore"),
                List.of(),
                List.of());
        WayangSdkBoundary app = new WayangSdkBoundary(
                "app",
                "App",
                "example.app",
                "Out-of-root app boundary.",
                List.of("Shared"),
                List.of(),
                List.of("missing", "app"));
        WayangSdkBoundary other = boundary(
                "other",
                "Other",
                ".core",
                List.of("Shared"),
                List.of(),
                List.of());
        WayangSdkBoundary cycleA = boundary(
                "cycle-a",
                "Cycle A",
                ".cyclea",
                List.of("CycleA"),
                List.of(),
                List.of("cycle-b"));
        WayangSdkBoundary cycleB = boundary(
                "cycle-b",
                "Cycle B",
                ".cycleb",
                List.of("CycleB"),
                List.of(),
                List.of("cycle-a"));

        WayangSdkBoundaryCatalogValidationReport report = WayangSdkBoundaryCatalog.validate(
                List.of(core, duplicateCore, app, other, cycleA, cycleB));

        assertThat(report.valid()).isFalse();
        assertThat(report.totalBoundaries()).isEqualTo(6);
        assertThat(report.issues())
                .extracting(WayangSdkBoundaryCatalogValidationIssue::kind)
                .contains(
                        "duplicate-boundary-id",
                        "duplicate-intended-package",
                        "invalid-intended-package",
                        "unknown-dependency",
                        "self-dependency",
                        "duplicate-class-prefix",
                        "dependency-cycle");
    }

    private static WayangSdkBoundary boundary(
            String id,
            String name,
            String packageSuffix,
            List<String> classPrefixes,
            List<String> contractSchemas,
            List<String> dependsOn) {
        return new WayangSdkBoundary(
                id,
                name,
                WayangSdkBoundaryCatalog.SDK_ROOT_PACKAGE + packageSuffix,
                name + " test boundary.",
                classPrefixes,
                contractSchemas,
                dependsOn);
    }
}
