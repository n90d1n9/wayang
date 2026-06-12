package tech.kayys.wayang.gollek.sdk;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AgentRunPlanningContractTest {

    @Test
    void normalizesPlanningContractDescriptors() {
        AgentRunPlanningContract contract = AgentRunPlanningContract.of(" run-preview ");
        AgentRunPlanningContract fallback = new AgentRunPlanningContract("", 0, null);

        assertThat(contract.schema()).isEqualTo("wayang.run.planning");
        assertThat(contract.version()).isEqualTo(1);
        assertThat(contract.envelope()).isEqualTo(AgentRunPlanningContract.RUN_PREVIEW);
        assertThat(fallback.schema()).isEqualTo("wayang.run.planning");
        assertThat(fallback.version()).isEqualTo(1);
        assertThat(fallback.envelope()).isEmpty();
        assertThat(AgentRunPlanningContract.runPreflight().envelope())
                .isEqualTo(AgentRunPlanningContract.RUN_PREFLIGHT);
        assertThat(AgentRunPlanningContract.runPreview().envelope())
                .isEqualTo(AgentRunPlanningContract.RUN_PREVIEW);
    }

    @Test
    void exposesConcretePreflightJsonSchema() {
        WayangContractDescriptor descriptor = WayangContractCatalog.discover(
                        WayangContractQuery.of(AgentRunPlanningContract.SCHEMA, "run-preflight"))
                .contracts()
                .get(0);

        WayangContractJsonSchema schema = WayangGollekSdk.local().contractJsonSchema(descriptor);

        assertThat(schema.id()).isEqualTo("urn:wayang:contract:wayang.run.planning:v1:run-preflight");
        assertThat(schema.document())
                .containsEntry("x-wayang-schema", AgentRunPlanningContract.SCHEMA)
                .containsEntry("x-wayang-envelope", AgentRunPlanningContract.RUN_PREFLIGHT)
                .containsEntry("x-wayang-domain", "planning");
        @SuppressWarnings("unchecked")
        java.util.List<String> required = (java.util.List<String>) schema.document().get("required");
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> properties =
                (java.util.Map<String, Object>) schema.document().get("properties");
        assertThat(required)
                .containsExactly("contract", "surfaceId", "ready", "surfacePolicyAssessment", "skillAssessment");
        assertThat(properties)
                .containsKeys("contract", "surfaceId", "ready", "surfacePolicyAssessment", "skillAssessment");
    }

    @Test
    void exposesConcretePreviewJsonSchema() {
        WayangContractDescriptor descriptor = WayangContractCatalog.discover(
                        WayangContractQuery.of(AgentRunPlanningContract.SCHEMA, "run-preview"))
                .contracts()
                .get(0);

        WayangContractJsonSchema schema = WayangGollekSdk.local().contractJsonSchema(descriptor);

        assertThat(schema.id()).isEqualTo("urn:wayang:contract:wayang.run.planning:v1:run-preview");
        assertThat(schema.document())
                .containsEntry("x-wayang-schema", AgentRunPlanningContract.SCHEMA)
                .containsEntry("x-wayang-envelope", AgentRunPlanningContract.RUN_PREVIEW)
                .containsEntry("x-wayang-domain", "planning");
        @SuppressWarnings("unchecked")
        java.util.List<String> required = (java.util.List<String>) schema.document().get("required");
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> properties =
                (java.util.Map<String, Object>) schema.document().get("properties");
        assertThat(required)
                .contains(
                        "contract",
                        "requestId",
                        "surfaceId",
                        "promptCharacters",
                        "context",
                        "parameters",
                        "surfacePolicyAssessment",
                        "skillAssessment");
        assertThat(properties)
                .containsKeys(
                        "contract",
                        "requestId",
                        "tenantId",
                        "modelId",
                        "workflowId",
                        "surfaceId",
                        "systemPromptPresent",
                        "promptCharacters",
                        "context",
                        "parameters",
                        "surfacePolicyAssessment",
                        "skillAssessment");
    }
}
