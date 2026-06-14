package tech.kayys.wayang.gollek.sdk;

import org.junit.jupiter.api.Test;

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
                        "tech.kayys.wayang.gollek.sdk.core",
                        "tech.kayys.wayang.gollek.sdk.run",
                        "tech.kayys.wayang.gollek.sdk.context",
                        "tech.kayys.wayang.gollek.sdk.capability",
                        "tech.kayys.wayang.gollek.sdk.platform",
                        "tech.kayys.wayang.gollek.sdk.contract",
                        "tech.kayys.wayang.gollek.sdk.workbench",
                        "tech.kayys.wayang.gollek.sdk.storage",
                        "tech.kayys.wayang.gollek.sdk.remote");
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
                .isEqualTo("tech.kayys.wayang.gollek.sdk.storage");

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
                " tech.kayys.wayang.gollek.sdk.demo ",
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
}
