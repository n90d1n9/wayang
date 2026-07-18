package tech.kayys.wayang.client;

import org.junit.jupiter.api.Test;

import tech.kayys.wayang.boundry.WayangSdkBoundaryCatalog;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WayangPlatformEnvelopesTest {

    @Test
    void statusEnvelopeOwnsPublishedPlatformShape() {
        Map<String, Object> values = WayangPlatformEnvelopes.status(Wayang.local().status());

        assertThat(values)
                .containsEntry("product", "Wayang")
                .containsEntry("version", "1.0.0-SNAPSHOT")
                .containsEntry("gollek", "external")
                .containsEntry("gamelan", "external")
                .containsEntry("agentCore", "local")
                .containsEntry("rag", "local")
                .containsEntry("mcp", "adapter-ready")
                .containsEntry("activeSkills", WayangSkillCatalog.defaultAvailableSkillCount());
    }

    @Test
    void productCatalogEnvelopeGroupsPoliciesProfilesAndSurfaces() {
        Map<String, Object> values = WayangPlatformEnvelopes.productCatalog(
                WayangProductCatalog.defaultSurfaces(),
                WayangProductCatalog.defaultPolicies(),
                WayangProductCatalog.defaultProfiles());

        assertThat(values)
                .containsEntry("product", "Wayang")
                .containsEntry("coreEngine", "agents, skills, tools, MCP, RAG, memory, workflows, and harness checks");
        assertThat(list(values.get("surfaces")))
                .first()
                .satisfies(surface -> assertThat(objectMap(surface))
                        .containsEntry("id", "coding-agent")
                        .containsEntry("profiles", List.of("coding-agent", "openclaw-agent"))
                        .containsKey("policy"));
        assertThat(list(values.get("profiles")))
                .anySatisfy(profile -> assertThat(objectMap(profile))
                        .containsEntry("id", "openclaw-agent")
                        .containsEntry("surfaceId", "coding-agent")
                        .containsEntry("requireReady", true));
    }

    @Test
    void profileListAndDetailEnvelopesUseSharedProfileProjection() {
        ProductProfile profile = WayangProductCatalog.profileFor("low-code-agent");

        Map<String, Object> listValues = WayangPlatformEnvelopes.profiles(
                " Wayang ",
                " workflow-platform ",
                WayangProductCatalog.profilesForSurface("workflow-platform"));
        Map<String, Object> detailValues = WayangPlatformEnvelopes.profileDetail("Wayang", profile);

        assertThat(listValues)
                .containsEntry("product", "Wayang")
                .containsEntry("surfaceId", "workflow-platform")
                .containsEntry("totalProfiles", 2);
        assertThat(list(listValues.get("profiles")))
                .extracting(value -> objectMap(value).get("id"))
                .containsExactly("workflow-agent", "low-code-agent");
        assertThat(detailValues)
                .containsEntry("product", "Wayang")
                .containsEntry("profileId", "low-code-agent");
        assertThat(objectMap(detailValues.get("profile")))
                .containsEntry("id", "low-code-agent")
                .containsEntry("workflowId", "gamelan-low-code-workflow")
                .containsEntry("skills", List.of("workflow", "hitl", "observability"));
    }

    @Test
    void sdkBoundaryEnvelopesUseSharedBoundaryProjection() {
        Map<String, Object> catalog = WayangPlatformEnvelopes.sdkBoundaryCatalog(
                WayangSdkBoundaryCatalog.defaultBoundaries());
        Map<String, Object> detail = WayangPlatformEnvelopes.sdkBoundaryDetail(
                WayangSdkBoundaryCatalog.require("run"));
        Map<String, Object> validation = WayangPlatformEnvelopes.sdkBoundaryCatalogValidation(
                WayangSdkBoundaryCatalog.validateDefault());

        assertThat(catalog)
                .containsEntry("product", "Wayang")
                .containsEntry("rootPackage", "tech.kayys.wayang.gollek.client")
                .containsEntry("defaultBoundaryId", "core")
                .containsEntry("totalBoundaries", 9);
        assertThat(catalog.get("boundaryIds"))
                .isEqualTo(List.of(
                        "core",
                        "run",
                        "context",
                        "capability",
                        "platform",
                        "contract",
                        "workbench",
                        "storage",
                        "remote"));
        assertThat(list(catalog.get("boundaries")))
                .anySatisfy(boundary -> assertThat(objectMap(boundary))
                        .containsEntry("id", "contract")
                        .containsEntry("intendedPackage", "tech.kayys.wayang.gollek.client.contract")
                        .containsEntry("dependsOn", List.of("core")));
        assertThat(detail)
                .containsEntry("product", "Wayang")
                .containsEntry("boundaryId", "run");
        assertThat(objectMap(detail.get("boundary")))
                .containsEntry("id", "run")
                .containsEntry("intendedPackage", "tech.kayys.wayang.gollek.client.run")
                .containsEntry("contractSchemas", List.of("wayang.run.planning", "wayang.run.lifecycle"));
        assertThat(validation)
                .containsEntry("product", "Wayang")
                .containsEntry("valid", true)
                .containsEntry("issueCount", 0)
                .containsEntry("totalBoundaries", 9);
        assertThat(validation.get("classPrefixes"))
                .asList()
                .contains("WayangPlatformContract", "Remote");
    }

    @Test
    void nullAndBlankInputsNormalizeForJsonContracts() {
        Map<String, Object> status = WayangPlatformEnvelopes.status(null);
        Map<String, Object> catalog = WayangPlatformEnvelopes.productCatalog(null, null, null);
        Map<String, Object> profiles = WayangPlatformEnvelopes.profiles(null, " ", null);
        Map<String, Object> boundaryDetail = WayangPlatformEnvelopes.sdkBoundaryDetail(null);
        Map<String, Object> boundaryValidation = WayangPlatformEnvelopes.sdkBoundaryCatalogValidation(null);

        assertThat(status)
                .containsEntry("product", "Wayang")
                .containsEntry("version", "unknown")
                .containsEntry("activeSkills", 0);
        assertThat(catalog)
                .containsEntry("product", "Wayang")
                .containsEntry("surfaces", List.of())
                .containsEntry("profiles", List.of());
        assertThat(profiles)
                .containsEntry("product", "")
                .containsEntry("surfaceId", null)
                .containsEntry("totalProfiles", 0)
                .containsEntry("profiles", List.of());
        assertThat(boundaryDetail)
                .containsEntry("boundaryId", "core");
        assertThat(boundaryValidation)
                .containsEntry("valid", true)
                .containsEntry("issueCount", 0);
        assertThatThrownBy(() -> catalog.put("extra", "value"))
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
