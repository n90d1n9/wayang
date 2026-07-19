package tech.kayys.wayang.client;

import org.junit.jupiter.api.Test;

import tech.kayys.wayang.agent.event.AgentRunEvents;
import tech.kayys.wayang.agent.event.AgentRunEventsQuery;
import tech.kayys.wayang.agent.history.AgentRunHistory;
import tech.kayys.wayang.agent.history.AgentRunHistoryQuery;
import tech.kayys.wayang.agent.planner.AgentRunPlanningContract;
import tech.kayys.wayang.agent.run.AgentRunPreview;
import tech.kayys.wayang.agent.run.AgentRunReadiness;
import tech.kayys.wayang.agent.run.AgentRunRequest;
import tech.kayys.wayang.agent.run.AgentRunStatus;
import tech.kayys.wayang.agent.run.WayangRunApi;
import tech.kayys.wayang.agent.run.WayangRunSpec;
import tech.kayys.wayang.agent.skill.AgentSkillDiscovery;
import tech.kayys.wayang.agent.skill.AgentSkillQuery;
import tech.kayys.wayang.agent.store.AgentRunStoreDiagnostics;
import tech.kayys.wayang.agent.store.AgentRunStoreVerification;
import tech.kayys.wayang.alignment.WayangStandardAlignmentHealthReport;
import tech.kayys.wayang.alignment.WayangStandardAlignmentPolicyConfig;
import tech.kayys.wayang.harness.HarnessPlan;
import tech.kayys.wayang.harness.HarnessPlanRequest;
import tech.kayys.wayang.readiness.WayangPlatformReadinessProfileValidationReport;
import tech.kayys.wayang.registry.WayangStandardRegistry;
import tech.kayys.wayang.skill.RegisteredSkill;
import tech.kayys.wayang.workbench.WorkbenchCommandDiscovery;
import tech.kayys.wayang.workbench.WorkbenchCommandQuery;
import tech.kayys.wayang.boundry.WayangSdkBoundary;
import tech.kayys.wayang.boundry.WayangSdkBoundaryCatalogValidationReport;
import tech.kayys.wayang.capability.WayangProviderCapabilityDescriptor;
import tech.kayys.wayang.capability.WayangProviderCapabilityDiscovery;
import tech.kayys.wayang.capability.WayangProviderCapabilityQuery;
import tech.kayys.wayang.catalog.WayangStandardCatalog;
import tech.kayys.wayang.context.WayangContextApi;
import tech.kayys.wayang.contract.WayangContractDiscovery;
import tech.kayys.wayang.contract.WayangContractKey;
import tech.kayys.wayang.contract.WayangContractQuery;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WayangClientTest {

    @Test
    void exposesGroupedSdkApisWithoutHidingUnderlyingSdk() {
        try (WayangClient client = Wayang.client()) {
            WorkbenchCommandQuery commandQuery = WorkbenchCommandQuery.of("assistant-agent", "Runs", null);
            WorkbenchCommandDiscovery commands = client.commands().discover(commandQuery);
            WayangWorkbenchModel workbench = client.commands().workbench(commandQuery);

            assertThat(client.productName()).isEqualTo("Wayang");
            assertThat(client.sdk().commandDiscovery(commandQuery)).isEqualTo(commands);
            assertThat(client.commands().indexEnvelope(commandQuery))
                    .containsEntry("product", "Wayang")
                    .containsEntry("matchingCommands", commands.matchingCommands())
                    .doesNotContainKey("commands");
            assertThat(client.commands().indexJson(commandQuery))
                    .contains("\"product\":\"Wayang\"")
                    .contains("\"matchingCommands\":" + commands.matchingCommands())
                    .doesNotContain("\"commands\":[");
            assertThat(client.commands().workbenchJson(workbench, commandQuery))
                    .contains("\"product\":\"Wayang\"")
                    .contains("\"commandQuery\":{\"surfaceId\":\"assistant-agent\"")
                    .contains("\"commandPalette\":")
                    .contains("\"nextActions\":");
        }
    }

    @Test
    void exposesContractSchemasAndWireRenderingThroughSdkFacade() {
        try (WayangClient client = Wayang.client()) {
            WayangContractKey previewKey = WayangContractKey.of(
                    AgentRunPlanningContract.SCHEMA,
                    AgentRunPlanningContract.VERSION,
                    AgentRunPlanningContract.RUN_PREVIEW);

            WayangContractDiscovery discovery = client.contracts().discover(previewKey);

            assertThat(discovery.contracts()).hasSize(1);
            assertThat(client.contracts().schemaBundle(previewKey).keys()).containsExactly(previewKey);
            assertThat(client.contracts().indexEnvelope(WayangContractQuery.forKey(previewKey)))
                    .containsEntry("product", "Wayang")
                    .containsEntry("matchingContracts", 1)
                    .doesNotContainKey("contracts");
            assertThat(client.contracts().schemaJson(previewKey))
                    .contains("\"$id\":\"" + previewKey.jsonSchemaId() + "\"")
                    .contains("\"x-wayang-envelope\":\"" + AgentRunPlanningContract.RUN_PREVIEW + "\"");
            assertThat(client.wire().value(new int[]{1, 2, 3})).isEqualTo("[1,2,3]");
        }
    }

    @Test
    void exposesSkillAndProviderCapabilityApisThroughSdkFacade() {
        try (WayangClient client = Wayang.client()) {
            AgentSkillDiscovery skills = client.skills().discover(
                    AgentSkillQuery.forProfile("low-code-agent", null, null),
                    "gamelan");
            RegisteredSkill workflowSkill = client.skills().get("workflow.gamelan");
            WayangProviderCapabilityDiscovery providers = client.providers().discover(
                    WayangProviderCapabilityQuery.forStandard(WayangStandardRegistry.A2UI),
                    "contracts");
            WayangProviderCapabilityDescriptor provider = client.providers().get("a2ui.contracts");

            assertThat(skills.skillIds()).containsExactly("workflow.gamelan");
            assertThat(client.skills().discoveryJson(skills))
                    .contains("\"product\":\"Wayang\"")
                    .contains("\"skillIds\":[\"workflow.gamelan\"]");
            assertThat(client.skills().detailJson(workflowSkill))
                    .contains("\"skillId\":\"workflow.gamelan\"");
            assertThat(providers.capabilityIds()).containsExactly("a2ui.contracts");
            assertThat(client.providers().discoveryJson(providers))
                    .contains("\"product\":\"Wayang\"")
                    .contains("\"capabilityIds\":[\"a2ui.contracts\"]");
            assertThat(client.providers().detailJson(provider))
                    .contains("\"capabilityId\":\"a2ui.contracts\"");
        }
    }

    @Test
    void exposesPlatformAndStandardsApisThroughSdkFacade() {
        try (WayangClient client = Wayang.client()) {
            WayangPlatformApi platform = client.platform();
            WayangStandardsApi standards = client.standards();
            List<ProductProfile> assistantProfiles = platform.productProfilesForSurface("assistant-agent");
            WayangPlatformReadinessProfileValidationReport validation =
                    platform.readinessProfileValidation("relaxed");
            WayangSdkBoundaryCatalogValidationReport boundaryValidation =
                    platform.sdkBoundaryCatalogValidation();
            WayangStandardCatalog catalog = standards.catalog();
            WayangStandardAlignmentHealthReport health =
                    standards.health(WayangStandardAlignmentPolicyConfig.none());

            assertThat(platform.status().productName()).isEqualTo("Wayang");
            assertThat(platform.productCatalogJson())
                    .contains("\"product\":\"Wayang\"")
                    .contains("\"surfaces\":[");
            assertThat(platform.profilesJson("assistant-agent", assistantProfiles))
                    .contains("\"surfaceId\":\"assistant-agent\"")
                    .contains("\"totalProfiles\":" + assistantProfiles.size());
            assertThat(platform.sdkBoundaries())
                    .extracting(WayangSdkBoundary::id)
                    .contains("core", "run", "contract", "remote");
            assertThat(platform.sdkBoundary("workbench").intendedPackage())
                    .isEqualTo("tech.kayys.wayang.gollek.client.workbench");
            assertThat(platform.sdkBoundaryCatalogJson())
                    .contains("\"rootPackage\":\"tech.kayys.wayang.gollek.client\"")
                    .contains("\"boundaryIds\":[\"core\",\"run\",\"context\",\"capability\"");
            assertThat(platform.sdkBoundaryJson("run"))
                    .contains("\"boundaryId\":\"run\"")
                    .contains("\"contractSchemas\":[\"wayang.run.planning\",\"wayang.run.lifecycle\"]");
            assertThat(boundaryValidation.valid()).isTrue();
            assertThat(platform.sdkBoundaryCatalogValidationJson())
                    .contains("\"valid\":true")
                    .contains("\"issueCount\":0")
                    .contains("\"classPrefixes\":[");
            assertThat(platform.readinessProfileValidationJson(validation))
                    .contains("\"product\":\"Wayang\"")
                    .contains("\"validationPolicy\"");
            assertThat(standards.catalogJson(catalog))
                    .contains("\"product\":\"Wayang\"")
                    .contains("\"totalStandards\":" + catalog.totalStandards());
            assertThat(standards.healthJson(health))
                    .contains("\"product\":\"Wayang\"")
                    .contains("\"health\":{");
        }
    }

    @Test
    void exposesRunLifecycleApiThroughSdkFacade() {
        try (WayangClient client = Wayang.client()) {
            WayangRunApi runs = client.runs();
            AgentRunRequest request = AgentRunRequest.builder()
                    .prompt("hello")
                    .surfaceId("assistant-agent")
                    .build();
            AgentRunReadiness readiness = runs.preflight(request);
            AgentRunPreview preview = runs.preview(request);
            AgentRunHistory history = runs.history(AgentRunHistoryQuery.all());
            AgentRunStoreDiagnostics diagnostics = runs.diagnostics();
            AgentRunStoreVerification verification = runs.verification();
            AgentRunEvents events = runs.events("missing-run", AgentRunEventsQuery.all());
            AgentRunStatus status = runs.status("missing-run");

            assertThat(runs.preflightJson(readiness))
                    .contains("\"envelope\":\"run-preflight\"")
                    .contains("\"surfacePolicyAssessment\"");
            assertThat(runs.previewJson(preview))
                    .contains("\"envelope\":\"run-preview\"")
                    .contains("\"promptCharacters\":5");
            assertThat(runs.historyJson(history))
                    .contains("\"envelope\":\"run-list\"");
            assertThat(runs.diagnosticsJson(diagnostics))
                    .contains("\"envelope\":\"run-store\"")
                    .contains("\"backend\":\"memory\"")
                    .contains("\"retentionAssessment\"");
            assertThat(runs.verificationJson(verification))
                    .contains("\"envelope\":\"run-store-verification\"")
                    .contains("\"passed\":true")
                    .contains("\"diagnostics\":{\"backend\":\"memory\"");
            assertThat(runs.eventsJson(events))
                    .contains("\"envelope\":\"run-events\"");
            assertThat(runs.statusJson(status))
                    .contains("\"envelope\":\"run-status\"")
                    .contains("\"known\":false");
        }
    }

    @Test
    void exposesRunSpecApiThroughSdkFacade() {
        try (WayangClient client = Wayang.client()) {
            WayangSpecApi specs = client.specs();
            WayangRunSpec spec = specs.templateSpec("assistant-agent");
            AgentRunPreview preview = specs.validate(spec);

            assertThat(specs.templateProperties("assistant-agent"))
                    .contains("surfaceId=assistant-agent")
                    .contains("skills=memory,rag,mcp");
            assertThat(specs.validationJson("memory://assistant-agent", preview))
                    .contains("\"path\":\"memory://assistant-agent\"")
                    .contains("\"ready\":true")
                    .contains("\"preview\":{")
                    .contains("\"envelope\":\"run-preview\"");
        }
    }

    @Test
    void exposesContextEngineeringApiThroughSdkFacade() {
        try (WayangClient client = Wayang.client()) {
            WayangContextApi contexts = client.contexts();
            WorkspaceSnapshot snapshot = contexts.workspace(new WorkspaceInspectionRequest(".", 1, false));
            HarnessPlan plan = contexts.harness(new HarnessPlanRequest(".", 1, true));

            assertThat(contexts.workspaceJson(snapshot))
                    .contains("\"rootPath\":")
                    .contains("\"exists\":")
                    .contains("\"importantPaths\":");
            assertThat(contexts.harnessJson(plan))
                    .contains("\"workspace\":")
                    .contains("\"checks\":")
                    .contains("\"notes\":");
        }
    }
}
