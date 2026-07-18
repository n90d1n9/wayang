package tech.kayys.wayang.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import tech.kayys.wayang.agent.event.AgentRunEvents;
import tech.kayys.wayang.agent.event.AgentRunEventsFollowOptions;
import tech.kayys.wayang.agent.event.AgentRunEventsFollowResult;
import tech.kayys.wayang.agent.event.AgentRunEventsQuery;
import tech.kayys.wayang.agent.planner.AgentRunPlanningContract;
import tech.kayys.wayang.agent.run.AgentRunCancelResult;
import tech.kayys.wayang.agent.run.AgentRunForgetResult;
import tech.kayys.wayang.agent.run.AgentRunHandle;
import tech.kayys.wayang.agent.run.AgentRunInspection;
import tech.kayys.wayang.agent.run.AgentRunOutcomes;
import tech.kayys.wayang.agent.run.AgentRunPreview;
import tech.kayys.wayang.agent.run.AgentRunReadiness;
import tech.kayys.wayang.agent.run.AgentRunRequest;
import tech.kayys.wayang.agent.run.AgentRunResult;
import tech.kayys.wayang.agent.run.AgentRunState;
import tech.kayys.wayang.agent.run.AgentRunStatus;
import tech.kayys.wayang.agent.run.AgentRunWaitOptions;
import tech.kayys.wayang.agent.run.AgentRunWaitResult;
import tech.kayys.wayang.agent.skill.AgentSkillDiscovery;
import tech.kayys.wayang.agent.spi.AgentRequest;
import tech.kayys.wayang.agent.store.AgentRunStore;
import tech.kayys.wayang.harness.HarnessCheck;
import tech.kayys.wayang.harness.HarnessPlan;
import tech.kayys.wayang.harness.HarnessPlanRequest;
import tech.kayys.wayang.workbench.WorkbenchCommand;
import tech.kayys.wayang.workbench.WorkbenchCommandDiscovery;
import tech.kayys.wayang.workbench.WorkbenchCommandQuery;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WayangGollekSdkTest {

    private final WayangGollekSdk sdk = WayangGollekSdk.local();

    @Test
    void exposesAgentPlatformTruthForCliApiAndProductSurfaces() {
        WayangPlatformStatus status = sdk.status();

        assertThat(status.productName()).isEqualTo("Wayang");
        assertThat(status.notes()).anySatisfy(note -> assertThat(note).contains("SDK"));
        assertThat(sdk.productSurfaces())
                .extracting(ProductSurface::id)
                .containsExactly("coding-agent", "assistant-agent", "workflow-platform", "platform-admin");
        assertThat(sdk.productProfiles())
                .extracting(ProductProfile::id)
                .contains("coding-agent", "assistant-agent", "openclaw-agent", "low-code-agent");
    }

    @Test
    void buildsWorkbenchModelFromSdkContract() {
        WayangWorkbenchModel model = sdk.workbench();

        assertThat(model.status().productName()).isEqualTo("Wayang");
        assertThat(model.commandPalette())
                .containsExactlyElementsOf(WayangWorkbenchCatalog.localCommandPalette());
        assertThat(model.commands())
                .containsExactlyElementsOf(WayangWorkbenchCatalog.localCommands());
        assertThat(model.nextActions())
                .anySatisfy(action -> assertThat(action).contains("SDK"));
    }

    @Test
    void discoversWorkbenchCommandsThroughSdkContract() {
        assertThat(sdk.discoverCommands(WorkbenchCommandQuery.all()))
                .containsExactlyElementsOf(WayangWorkbenchCatalog.localCommands());

        WorkbenchCommandDiscovery discovery = sdk.commandDiscovery("assistant-agent", "Runs", "run-session-context");
        assertThat(discovery.totalCommands()).isEqualTo(WayangWorkbenchCatalog.localCommands().size());
        assertThat(discovery.matchingCommands()).isOne();
        assertThat(discovery.categoryCounts()).containsEntry("Runs", 1);
        assertThat(discovery.categorySummaries())
                .singleElement()
                .satisfies(summary -> {
                    assertThat(summary.name()).isEqualTo("Runs");
                    assertThat(summary.commandIds()).containsExactly("run-session-context");
                });
        assertThat(discovery.commandIds()).containsExactly("run-session-context");

        assertThat(sdk.discoverCommands("assistant-agent", "Runs", "run-session-context"))
                .singleElement()
                .satisfies(command -> {
                    assertThat(command.id()).isEqualTo("run-session-context");
                    assertThat(command.command()).contains("--session <id>");
                });

        WorkbenchCommandDiscovery profileDiscovery = sdk.commandDiscoveryForProfile("low-code-agent", "Runs", null);
        assertThat(profileDiscovery.query().profileId()).isEqualTo("low-code-agent");
        assertThat(profileDiscovery.query().resolvedSurfaceId()).isEqualTo("workflow-platform");
        assertThat(profileDiscovery.commandIds())
                .contains("run-profile", "run-workflow-skill")
                .doesNotContain("run-assistant-surface", "workspace-inspect");

        WorkbenchCommandDiscovery contractDiscovery = sdk.commandDiscoveryForContractJsonSchemaId(
                "urn:wayang:contract:wayang.run.planning:v1:run-preview");
        assertThat(contractDiscovery.query().contractJsonSchemaId())
                .isEqualTo("urn:wayang:contract:wayang.run.planning:v1:run-preview");
        assertThat(contractDiscovery.commandIds()).containsExactly("run-dry-json", "run-spec-dry-json");
        assertThat(contractDiscovery.contractJsonSchemaIds())
                .containsExactly("urn:wayang:contract:wayang.run.planning:v1:run-preview");
        assertThat(contractDiscovery.contractJsonSchemaIdCounts())
                .containsEntry("urn:wayang:contract:wayang.run.planning:v1:run-preview", 2);
        assertThat(sdk.discoverCommandsForContractJsonSchemaId(
                "urn:wayang:contract:wayang.run.planning:v1:run-preview"))
                .extracting(WorkbenchCommand::id)
                .containsExactly("run-dry-json", "run-spec-dry-json");

        WayangContractKey previewKey = WayangContractKey.of(
                AgentRunPlanningContract.SCHEMA,
                AgentRunPlanningContract.VERSION,
                AgentRunPlanningContract.RUN_PREVIEW);
        assertThat(sdk.commandDiscoveryForContractKey(previewKey).commandIds())
                .containsExactly("run-dry-json", "run-spec-dry-json");
        assertThat(sdk.discoverCommandsForContractKey(previewKey))
                .extracting(WorkbenchCommand::id)
                .containsExactly("run-dry-json", "run-spec-dry-json");
        assertThat(sdk.workbenchForContractKey(previewKey).commands())
                .extracting(WorkbenchCommand::id)
                .containsExactly("run-dry-json", "run-spec-dry-json");
    }

    @Test
    void discoversContractsThroughTypedSdkKeys() {
        WayangContractKey previewKey = WayangContractKey.of(
                AgentRunPlanningContract.SCHEMA,
                AgentRunPlanningContract.VERSION,
                AgentRunPlanningContract.RUN_PREVIEW);

        WayangContractDiscovery discovery = sdk.contractDiscovery(previewKey);
        WayangContractJsonSchemaBundle bundle = sdk.contractJsonSchemaBundle(previewKey);

        assertThat(discovery.query().jsonSchemaId()).isEqualTo(previewKey.jsonSchemaId());
        assertThat(discovery.query().jsonSchemaKey()).hasValue(previewKey);
        assertThat(discovery.contracts())
                .singleElement()
                .satisfies(contract -> {
                    assertThat(contract.key()).isEqualTo(previewKey);
                    assertThat(contract.commandIds()).containsExactly("run-dry-json", "run-spec-dry-json");
                });
        assertThat(bundle.keys()).containsExactly(previewKey);
        assertThat(bundle.schemaByKey(previewKey))
                .hasValueSatisfying(schema -> assertThat(schema.id()).isEqualTo(previewKey.jsonSchemaId()));
    }

    @Test
    void discoversSkillsThroughProductProfiles() {
        AgentSkillDiscovery discovery = sdk.skillDiscoveryForProfile("low-code-agent", "");

        assertThat(discovery.query().profileId()).isEqualTo("low-code-agent");
        assertThat(discovery.query().resolvedSurfaceId()).isEqualTo("workflow-platform");
        assertThat(discovery.skillIds())
                .containsExactly("workflow.gamelan", "hitl.approval", "observability.traces");
        assertThat(sdk.skillsForProfile("low-code-agent"))
                .extracting(RegisteredSkill::id)
                .containsExactly("workflow.gamelan", "hitl.approval", "observability.traces");
    }

    @Test
    void exposesProviderCapabilitiesThroughSdkFacade() {
        assertThat(sdk.providerCapabilityRegistry().capabilityIds())
                .contains("skills.dynamic", "mcp.bridge", "a2ui.contracts", "agentic-commerce.protocol");
        assertThat(sdk.providerCapabilities(WayangProviderCapabilityQuery.forStandard("a2ui-v0.8")))
                .singleElement()
                .satisfies(capability -> {
                    assertThat(capability.id()).isEqualTo("a2ui.contracts");
                    assertThat(capability.providerId()).isEqualTo("wayang-a2ui");
                    assertThat(capability.standardIds()).containsExactly(WayangStandardRegistry.A2UI);
                    assertThat(capability.tags()).contains("pro", "enterprise", "addon");
                    assertThat(capability.metadata())
                            .containsEntry("edition", "pro-enterprise")
                            .containsEntry("defaultCommunity", false)
                            .containsEntry("activationProfile", "pro-enterprise-addons");
                });
        assertThat(sdk.providerCapability("agentic-commerce.protocol").capabilityType())
                .isEqualTo(WayangProviderCapabilityCatalog.TYPE_COMMERCE);
        assertThat(sdk.providerCapabilityDiscovery(WayangProviderCapabilityQuery.forModule("a2ui"), "contracts")
                .capabilityIds()).containsExactly("a2ui.contracts");
        assertThat(sdk.providerCapabilityDiscovery("hybrid").capabilityIds())
                .containsExactly("storage.hybrid-persistence");
    }

    @Test
    void buildsFilteredWorkbenchThroughSdkContract() {
        WayangWorkbenchModel model = sdk.workbench("assistant-agent", "Runs", "run-session-context");

        assertThat(model.commands())
                .singleElement()
                .satisfies(command -> assertThat(command.id()).isEqualTo("run-session-context"));
        assertThat(model.commandPalette())
                .containsExactly("run <task> --session <id> --user <id> --context rag.collection=<name>");
        assertThat(model.productSurfaces())
                .extracting(ProductSurface::id)
                .containsExactly("coding-agent", "assistant-agent", "workflow-platform", "platform-admin");
    }

    @Test
    void preparesAgentRunThroughSdk() {
        AgentRunResult result = sdk.run(new AgentRunRequest(
                " plan next eval ",
                " tenant-a ",
                " model-a ",
                " workflow-a ",
                List.of("rag"),
                true,
                5));

        assertThat(result.successful()).isTrue();
        assertThat(result.answer()).contains("plan next eval");
        assertThat(result.handle().state()).isEqualTo(AgentRunState.COMPLETED);
        assertThat(result.handle().strategy()).isEqualTo("wayang-agent-over-gollek");
        assertThat(result.handle().terminal()).isTrue();
        assertThat(result.metadata())
                .containsEntry("tenant", "tenant-a")
                .containsEntry("model", "model-a")
                .containsEntry("workflow", "workflow-a")
                .containsEntry("surface", "coding-agent")
                .containsEntry("memoryEnabled", true)
                .containsEntry("maxSteps", 5)
                .containsEntry("sdkMode", "local");
        assertThat(result.metadata()).containsKey("skillAssessment").containsKey("runReadiness");
        @SuppressWarnings("unchecked")
        Map<String, Object> skillAssessment = (Map<String, Object>) result.metadata().get("skillAssessment");
        assertThat(skillAssessment)
                .containsEntry("ready", false)
                .containsEntry("surfaceId", "coding-agent");
        @SuppressWarnings("unchecked")
        List<Object> incompatibleSkillIds = (List<Object>) skillAssessment.get("incompatibleSkillIds");
        assertThat(incompatibleSkillIds).contains("rag.retrieve");
    }

    @Test
    void reportsUnknownStatusForMissingLocalRuns() {
        AgentRunStatus status = sdk.runStatus("run-missing");

        assertThat(status.known()).isFalse();
        assertThat(status.outcome()).isEqualTo(AgentRunOutcomes.UNKNOWN);
        assertThat(status.handle().runId()).isEqualTo("run-missing");
        assertThat(status.handle().state()).isEqualTo(AgentRunState.UNKNOWN);
        assertThat(status.message()).contains("No run status is recorded");
    }

    @Test
    void recordsLocalRunStatusAfterSubmission() {
        AgentRunResult result = sdk.run(AgentRunRequest.builder()
                .prompt("record this run")
                .tenantId("tenant-a")
                .modelId("model-a")
                .maxSteps(3)
                .build());

        AgentRunStatus status = sdk.runStatus(result.runId());

        assertThat(status.known()).isTrue();
        assertThat(status.outcome()).isEqualTo(AgentRunOutcomes.TERMINAL);
        assertThat(status.handle()).isEqualTo(result.handle());
        assertThat(status.message()).isEqualTo("Run state is completed.");
        assertThat(status.metadata())
                .containsEntry("tenant", "tenant-a")
                .containsEntry("model", "model-a")
                .containsEntry("successful", true)
                .containsEntry("stepCount", result.steps().size());
        assertThat(sdk.runHistory().runs())
                .extracting(run -> run.handle().runId())
                .contains(result.runId());
        assertThat(sdk.runHistory().empty()).isFalse();
        assertThat(sdk.runEvents(result.runId()).events())
                .singleElement()
                .satisfies(event -> {
                    assertThat(event.runId()).isEqualTo(result.runId());
                    assertThat(event.type()).isEqualTo("run.completed");
                    assertThat(event.state()).isEqualTo(AgentRunState.COMPLETED);
                    assertThat(event.message()).isEqualTo("Run state is completed.");
                });
        AgentRunInspection inspection = sdk.inspectRun(result.runId(), AgentRunEventsQuery.of("completed", "", 10));
        assertThat(inspection.known()).isTrue();
        assertThat(inspection.outcome()).isEqualTo(AgentRunOutcomes.TERMINAL);
        assertThat(inspection.empty()).isFalse();
        assertThat(inspection.status().handle()).isEqualTo(result.handle());
        assertThat(inspection.events().query().state()).isEqualTo(AgentRunState.COMPLETED);
        assertThat(inspection.events().returnedEvents()).isEqualTo(1);
        assertThat(inspection.events().nextAfterSequence()).isEqualTo(1);
    }

    @Test
    void followsLocalRunEventsThroughSdkContract() {
        AgentRunResult result = sdk.run(AgentRunRequest.builder()
                .prompt("follow this run")
                .tenantId("tenant-a")
                .build());
        List<AgentRunEvents> windows = new ArrayList<>();

        AgentRunEventsFollowResult follow = sdk.followRunEvents(
                result.runId(),
                AgentRunEventsFollowOptions.of(AgentRunEventsQuery.all(), 2, 1L),
                windows::add);

        assertThat(follow.successful()).isTrue();
        assertThat(follow.outcome()).isEqualTo(AgentRunOutcomes.TERMINAL);
        assertThat(follow.terminalState()).isEqualTo("completed");
        assertThat(follow.terminalEventType()).isEqualTo("run.completed");
        assertThat(follow.terminalSequence()).isEqualTo(1);
        assertThat(follow.polls()).isEqualTo(1);
        assertThat(follow.lastEvents().runId()).isEqualTo(result.runId());
        assertThat(follow.nextAfterSequence()).isEqualTo(1);
        assertThat(follow.message()).contains("terminal state").contains("completed");
        assertThat(windows)
                .singleElement()
                .satisfies(events -> assertThat(events.events())
                        .singleElement()
                        .satisfies(event -> {
                            assertThat(event.runId()).isEqualTo(result.runId());
                            assertThat(event.state()).isEqualTo(AgentRunState.COMPLETED);
                        }));
    }

    @Test
    void forgetsLocalRunStatusSnapshots() {
        AgentRunResult result = sdk.run(AgentRunRequest.builder()
                .prompt("forget this run")
                .tenantId("tenant-a")
                .build());

        AgentRunForgetResult forget = sdk.forgetRun(result.runId());

        assertThat(forget.forgotten()).isTrue();
        assertThat(forget.outcome()).isEqualTo(AgentRunOutcomes.FORGOTTEN);
        assertThat(forget.runId()).isEqualTo(result.runId());
        assertThat(sdk.runStatus(result.runId()).known()).isFalse();
        assertThat(sdk.runHistory().runs())
                .extracting(run -> run.handle().runId())
                .doesNotContain(result.runId());
    }

    @Test
    void reportsTerminalLocalRunsAsNotCancellable() {
        AgentRunResult result = sdk.run(AgentRunRequest.builder()
                .prompt("cancel this terminal run")
                .tenantId("tenant-a")
                .build());

        AgentRunCancelResult cancel = sdk.cancelRun(result.runId(), "stop");

        assertThat(cancel.cancelled()).isFalse();
        assertThat(cancel.outcome()).isEqualTo(AgentRunOutcomes.NOT_CANCELLABLE);
        assertThat(cancel.runId()).isEqualTo(result.runId());
        assertThat(cancel.handle().state()).isEqualTo(AgentRunState.COMPLETED);
        assertThat(cancel.message()).contains("completed").contains("cannot be cancelled");
        assertThat(sdk.runStatus(result.runId()).handle().state()).isEqualTo(AgentRunState.COMPLETED);
    }

    @Test
    void waitsForTerminalLocalRun() {
        AgentRunResult result = sdk.run(AgentRunRequest.builder()
                .prompt("wait this terminal run")
                .tenantId("tenant-a")
                .build());

        AgentRunWaitResult wait = sdk.waitForRun(result.runId(), new AgentRunWaitOptions(0, 1));

        assertThat(wait.terminal()).isTrue();
        assertThat(wait.outcome()).isEqualTo(AgentRunOutcomes.TERMINAL);
        assertThat(wait.timedOut()).isFalse();
        assertThat(wait.attempts()).isEqualTo(1);
        assertThat(wait.status().handle()).isEqualTo(result.handle());
        assertThat(wait.message()).contains("terminal state").contains("completed");
    }

    @Test
    void waitTimesOutForNonTerminalLocalRunSnapshots() {
        AgentRunStore store = AgentRunStore.memory();
        store.save(new AgentRunStatus(
                new AgentRunHandle("run-wait-1", AgentRunState.RUNNING, "strategy-a"),
                true,
                "running",
                Map.of()));
        WayangGollekSdk local = new LocalWayangGollekSdk(WayangGollekSdkConfig.local(), store);

        AgentRunWaitResult wait = local.waitForRun("run-wait-1", new AgentRunWaitOptions(0, 1));

        assertThat(wait.terminal()).isFalse();
        assertThat(wait.outcome()).isEqualTo(AgentRunOutcomes.TIMEOUT);
        assertThat(wait.timedOut()).isTrue();
        assertThat(wait.status().handle().state()).isEqualTo(AgentRunState.RUNNING);
        assertThat(wait.message()).contains("timeout");
    }

    @Test
    void preparesAgentRunForExplicitProductSurface() {
        AgentRunRequest request = AgentRunRequest.builder()
                .prompt("answer a support question")
                .tenantId("tenant-a")
                .modelId("model-a")
                .surfaceId("assistant-agent")
                .maxSteps(5)
                .build();

        AgentRunResult result = sdk.run(request);

        assertThat(result.metadata())
                .containsEntry("surface", "assistant-agent")
                .containsEntry("tenant", "tenant-a")
                .containsEntry("model", "model-a");
        assertThat(result.metadata()).containsKey("surfacePolicy");
        @SuppressWarnings("unchecked")
        Map<String, Object> surfacePolicy = (Map<String, Object>) result.metadata().get("surfacePolicy");
        assertThat(surfacePolicy)
                .containsEntry("surfaceId", "assistant-agent")
                .containsEntry("memoryPreferred", true)
                .containsEntry("workspacePreferred", false);
        assertThat(result.metadata()).containsKey("surfacePolicyAssessment");
        @SuppressWarnings("unchecked")
        Map<String, Object> assessment = (Map<String, Object>) result.metadata().get("surfacePolicyAssessment");
        assertThat(assessment)
                .containsEntry("surfaceId", "assistant-agent")
                .containsEntry("ready", true);
        assertThat(result.metadata()).containsKey("skillAssessment").containsKey("runReadiness");
        @SuppressWarnings("unchecked")
        Map<String, Object> skillAssessment = (Map<String, Object>) result.metadata().get("skillAssessment");
        assertThat(skillAssessment)
                .containsEntry("surfaceId", "assistant-agent")
                .containsEntry("ready", true);
        assertThat(sdk.toCoreAgentRequest(request).context())
                .containsEntry("surfaceId", "assistant-agent");
    }

    @Test
    void preparesAgentRunWithSessionUserAndAdditionalContext() {
        AgentRunResult result = sdk.run(AgentRunRequest.builder()
                .prompt("answer with retrieved context")
                .systemPrompt("be concise")
                .tenantId("tenant-a")
                .surfaceId("assistant-agent")
                .sessionId("session-a")
                .userId("user-a")
                .context("rag.collection", "docs")
                .context("mcp.server", "filesystem")
                .build());

        assertThat(result.metadata())
                .containsEntry("session", "session-a")
                .containsEntry("user", "user-a")
                .containsEntry("systemPrompt", "be concise");
        @SuppressWarnings("unchecked")
        Map<String, Object> context = (Map<String, Object>) result.metadata().get("context");
        assertThat(context)
                .containsEntry("rag.collection", "docs")
                .containsEntry("mcp.server", "filesystem");
        AgentRequest coreRequest = sdk.toCoreAgentRequest(AgentRunRequest.builder()
                .prompt("answer with retrieved context")
                .systemPrompt("be concise")
                .tenantId("tenant-a")
                .surfaceId("assistant-agent")
                .sessionId("session-a")
                .userId("user-a")
                .context("rag.collection", "docs")
                .build());
        assertThat(coreRequest.sessionId()).isEqualTo("session-a");
        assertThat(coreRequest.userId()).isEqualTo("user-a");
        assertThat(coreRequest.systemPrompt()).isEqualTo("be concise");
        assertThat(coreRequest.context()).containsEntry("rag.collection", "docs");
    }

    @Test
    void rejectsUnknownProductSurfaceForLocalRuns() {
        AgentRunRequest request = AgentRunRequest.builder()
                .prompt("plan")
                .surfaceId("future-agent")
                .build();

        assertThatThrownBy(() -> sdk.run(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown Wayang product surface 'future-agent'")
                .hasMessageContaining("coding-agent")
                .hasMessageContaining("workflow-platform");
    }

    @Test
    void preparesAgentRunWithWorkspaceContext(@TempDir Path workspace) throws Exception {
        Files.writeString(workspace.resolve("pom.xml"), """
                <project>
                  <modules>
                    <module>agent</module>
                  </modules>
                </project>
                """);
        Files.createDirectories(workspace.resolve("agent"));
        Files.writeString(workspace.resolve("agent").resolve("pom.xml"), "<project />");
        Files.createDirectories(workspace.resolve(".git"));
        Files.writeString(workspace.resolve(".git").resolve("HEAD"), "ref: refs/heads/main");

        AgentRunResult result = sdk.run(new AgentRunRequest(
                " inspect this repo ",
                " tenant-a ",
                " model-a ",
                "",
                List.of("repo"),
                true,
                5,
                workspace.toString(),
                true,
                20));

        assertThat(result.steps()).contains("Inspect workspace and attach compact context to the agent request");
        assertThat(result.metadata()).containsKey("workspace");
        @SuppressWarnings("unchecked")
        Map<String, Object> workspaceMetadata = (Map<String, Object>) result.metadata().get("workspace");
        assertThat(workspaceMetadata)
                .containsEntry("rootPath", workspace.toAbsolutePath().normalize().toString())
                .containsEntry("branch", "main");
        assertThat((List<String>) workspaceMetadata.get("buildFiles")).contains("pom.xml");
        assertThat((List<String>) workspaceMetadata.get("modules")).contains("agent");
    }

    @Test
    void plansHarnessChecksFromWorkspaceDescriptors(@TempDir Path workspace) throws Exception {
        Files.writeString(workspace.resolve("pom.xml"), "<project />");
        Files.writeString(workspace.resolve("package.json"), "{}");

        HarnessPlan plan = sdk.planHarness(new HarnessPlanRequest(
                workspace.toString(),
                8,
                true));

        assertThat(plan.workspace().rootPath()).isEqualTo(workspace.toAbsolutePath().normalize().toString());
        assertThat(plan.checks())
                .extracting(HarnessCheck::id)
                .contains("maven-compile", "maven-test", "javascript-test");
        assertThat(plan.checks())
                .anySatisfy(check -> {
                    assertThat(check.id()).isEqualTo("maven-compile");
                    assertThat(check.command()).containsExactly("mvn", "-q", "compile", "-DskipTests");
                    assertThat(check.optional()).isFalse();
                });
        assertThat(plan.notes()).anySatisfy(note -> assertThat(note).contains("Harness plan generated locally"));
    }

    @Test
    void preparesAgentRunWithHarnessContext(@TempDir Path workspace) throws Exception {
        Files.writeString(workspace.resolve("pom.xml"), "<project />");

        AgentRunResult result = sdk.run(new AgentRunRequest(
                " make a safe change ",
                " tenant-a ",
                " model-a ",
                "",
                List.of("repo"),
                true,
                5,
                workspace.toString(),
                false,
                20,
                true,
                4,
                false));

        assertThat(result.steps()).contains("Plan harness checks and attach verification context to the agent request");
        assertThat(result.metadata()).containsKey("harness");
        @SuppressWarnings("unchecked")
        Map<String, Object> harnessMetadata = (Map<String, Object>) result.metadata().get("harness");
        assertThat(harnessMetadata)
                .containsEntry("workspaceRoot", workspace.toAbsolutePath().normalize().toString());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> checks = (List<Map<String, Object>>) harnessMetadata.get("checks");
        assertThat(checks)
                .extracting(check -> check.get("id"))
                .contains("maven-compile", "maven-test");
        assertThat(checks)
                .allSatisfy(check -> assertThat(check.get("optional")).isEqualTo(false));
        assertThat(sdk.toCoreAgentRequest(new AgentRunRequest(
                " make a safe change ",
                " tenant-a ",
                " model-a ",
                "",
                List.of("repo"),
                true,
                5,
                workspace.toString(),
                false,
                20,
                true,
                4,
                false)).context()).containsKey("harness");
    }

    @Test
    void previewsAgentRunWithCoreContextWithoutSubmitting(@TempDir Path workspace) throws Exception {
        Files.writeString(workspace.resolve("pom.xml"), "<project />");

        AgentRunPreview preview = sdk.previewRun(AgentRunRequest.builder()
                .prompt("make a safe change")
                .systemPrompt("stay inside repo")
                .tenantId("tenant-a")
                .modelId("model-a")
                .workspace(workspace.toString())
                .harness(4, false)
                .skill("repo")
                .build());

        assertThat(preview.ready()).isTrue();
        assertThat(preview.tenantId()).isEqualTo("tenant-a");
        assertThat(preview.modelId()).isEqualTo("model-a");
        assertThat(preview.surfaceId()).isEqualTo("coding-agent");
        assertThat(preview.systemPromptPresent()).isTrue();
        assertThat(preview.workspaceAttached()).isTrue();
        assertThat(preview.harnessAttached()).isTrue();
        assertThat(preview.context())
                .containsKeys("surfacePolicy", "surfacePolicyAssessment", "workspace", "harness");
        assertThat(preview.contextKeys()).contains("workspace", "harness");
        assertThat(preview.surfacePolicyAssessment().missingContextKeys()).isEmpty();
        assertThat(preview.skillAssessment().ready()).isTrue();
        assertThat(preview.skillAssessment().resolvedSkillIds()).containsExactly("repo.context");
    }

    @Test
    void previewRunReportsUnknownAndSurfaceIncompatibleSkills() {
        AgentRunPreview preview = sdk.previewRun(AgentRunRequest.builder()
                .prompt("answer with docs")
                .surfaceId("assistant-agent")
                .skill("repo")
                .skill("missing")
                .build());

        assertThat(preview.ready()).isFalse();
        assertThat(preview.surfacePolicyAssessment().ready()).isTrue();
        assertThat(preview.skillAssessment().ready()).isFalse();
        assertThat(preview.skillAssessment().resolvedSkillIds()).containsExactly("repo.context");
        assertThat(preview.skillAssessment().unknownSkills()).containsExactly("missing");
        assertThat(preview.skillAssessment().incompatibleSkillIds()).containsExactly("repo.context");
    }

    @Test
    void assessesRunReadinessAcrossSurfacePolicyAndSkills() {
        AgentRunReadiness readiness = sdk.assessRunReadiness(AgentRunRequest.builder()
                .prompt("answer with docs")
                .surfaceId("assistant-agent")
                .skill("repo")
                .skill("missing")
                .build());

        assertThat(readiness.ready()).isFalse();
        assertThat(readiness.surfacePolicyAssessment().ready()).isTrue();
        assertThat(readiness.skillAssessment().ready()).isFalse();
        assertThat(readiness.skillAssessment().unknownSkills()).containsExactly("missing");
        assertThat(readiness.skillAssessment().incompatibleSkillIds()).containsExactly("repo.context");
    }

    @Test
    void exposesShortWayangFacadeForSdkEntryPoint() {
        assertThat(Wayang.local().status().productName()).isEqualTo("Wayang");
    }

    @Test
    void registersLocalSdkProviderThroughServiceLoader() {
        assertThat(ServiceLoader.load(WayangGollekSdkProvider.class))
                .anySatisfy(provider -> {
                    assertThat(provider).isInstanceOf(LocalWayangGollekSdkProvider.class);
                    assertThat(provider.mode()).isEqualTo(WayangGollekSdkProvider.Mode.LOCAL);
                });
    }

    @Test
    void inspectsLocalWorkspaceForCodingAgentContext(@TempDir Path workspace) throws Exception {
        Files.writeString(workspace.resolve("pom.xml"), """
                <project>
                  <modules>
                    <module>agent</module>
                  </modules>
                </project>
                """);
        Files.createDirectories(workspace.resolve("agent"));
        Files.writeString(workspace.resolve("agent").resolve("pom.xml"), "<project />");
        Files.createDirectories(workspace.resolve(".git"));
        Files.writeString(workspace.resolve(".git").resolve("HEAD"), "ref: refs/heads/main");

        WorkspaceSnapshot snapshot = sdk.inspectWorkspace(new WorkspaceInspectionRequest(
                workspace.toString(),
                20,
                false));

        assertThat(snapshot.exists()).isTrue();
        assertThat(snapshot.directory()).isTrue();
        assertThat(snapshot.gitRepository()).isTrue();
        assertThat(snapshot.branch()).isEqualTo("main");
        assertThat(snapshot.buildFiles()).contains("pom.xml");
        assertThat(snapshot.packageManagers()).contains("maven");
        assertThat(snapshot.modules()).contains("agent");
        assertThat(snapshot.importantPaths()).contains("agent/", "pom.xml");
        assertThat(snapshot.notes()).anySatisfy(note -> assertThat(note).contains("Workspace inspected locally"));
    }
}
