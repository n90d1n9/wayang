package tech.kayys.wayang.client;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WayangProductCatalogTest {

    @Test
    void exposesKnownProductSurfacesInStableOrder() {
        assertThat(WayangProductCatalog.defaultSurfaces())
                .extracting(ProductSurface::id)
                .containsExactly("coding-agent", "assistant-agent", "workflow-platform", "platform-admin");
        assertThat(WayangProductCatalog.knownSurfaceIds())
                .containsExactly("coding-agent", "assistant-agent", "workflow-platform", "platform-admin");
    }

    @Test
    void normalizesAndFindsProductSurfaces() {
        assertThat(WayangProductCatalog.normalizeSurfaceId(" "))
                .isEqualTo(WayangProductCatalog.DEFAULT_SURFACE_ID);
        assertThat(WayangProductCatalog.findSurface(" assistant-agent "))
                .hasValueSatisfying(surface -> assertThat(surface.name()).isEqualTo("Assistant Agent"));
        assertThat(WayangProductCatalog.requireKnownSurfaceId(" workflow-platform "))
                .isEqualTo("workflow-platform");
    }

    @Test
    void exposesSurfacePoliciesForKnownProducts() {
        assertThat(WayangProductCatalog.defaultPolicies())
                .extracting(ProductSurfacePolicy::surfaceId)
                .containsExactly("coding-agent", "assistant-agent", "workflow-platform", "platform-admin");

        ProductSurfacePolicy coding = WayangProductCatalog.policyFor("coding-agent");
        assertThat(coding.memoryPreferred()).isTrue();
        assertThat(coding.workspacePreferred()).isTrue();
        assertThat(coding.harnessPreferred()).isTrue();
        assertThat(coding.routingHints()).contains("inspect-workspace", "plan-harness");

        ProductSurfacePolicy workflow = WayangProductCatalog.policyFor("workflow-platform");
        assertThat(workflow.workflowPreferred()).isTrue();
        assertThat(workflow.requiredContextKeys()).contains("workflowId");
    }

    @Test
    void exposesReusableProductProfiles() {
        assertThat(WayangProductCatalog.defaultProfiles())
                .extracting(ProductProfile::id)
                .containsExactly(
                        "coding-agent",
                        "openclaw-agent",
                        "assistant-agent",
                        "workflow-agent",
                        "low-code-agent",
                        "platform-admin");

        ProductProfile openclaw = WayangProductCatalog.profileFor("openclaw-agent");
        assertThat(openclaw.surfaceId()).isEqualTo("coding-agent");
        assertThat(openclaw.skills()).contains("repo", "tools", "patching", "mcp");
        assertThat(openclaw.requestTemplate().workspaceEnabled()).isTrue();
        assertThat(openclaw.requestTemplate().harnessEnabled()).isTrue();
        assertThat(openclaw.requestTemplate().context())
                .containsEntry("wayang.profile", "openclaw-agent")
                .containsEntry("wayang.surface", "coding-agent");

        assertThat(WayangProductCatalog.profilesForSurface("workflow-platform"))
                .extracting(ProductProfile::id)
                .containsExactly("workflow-agent", "low-code-agent");
    }

    @Test
    void rejectsUnknownProductSurfaceIds() {
        assertThatThrownBy(() -> WayangProductCatalog.requireKnownSurfaceId("future-agent"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown Wayang product surface 'future-agent'")
                .hasMessageContaining("coding-agent")
                .hasMessageContaining("assistant-agent")
                .hasMessageContaining("workflow-platform");
    }

    @Test
    void rejectsUnknownProductProfileIds() {
        assertThatThrownBy(() -> WayangProductCatalog.profileFor("future-profile"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown Wayang product profile 'future-profile'")
                .hasMessageContaining("openclaw-agent")
                .hasMessageContaining("low-code-agent");
    }
}
