package tech.kayys.wayang.gollek.sdk.remote;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import tech.kayys.wayang.gollek.sdk.AgentSkillQuery;
import tech.kayys.wayang.gollek.sdk.AgentRunCancelResult;
import tech.kayys.wayang.gollek.sdk.AgentRunEvents;
import tech.kayys.wayang.gollek.sdk.AgentRunEventsCursor;
import tech.kayys.wayang.gollek.sdk.AgentRunEventsQuery;
import tech.kayys.wayang.gollek.sdk.AgentRunEventsSummary;
import tech.kayys.wayang.gollek.sdk.AgentRunForgetResult;
import tech.kayys.wayang.gollek.sdk.AgentRunHistory;
import tech.kayys.wayang.gollek.sdk.AgentRunHistoryQuery;
import tech.kayys.wayang.gollek.sdk.AgentRunInspection;
import tech.kayys.wayang.gollek.sdk.AgentRunRequest;
import tech.kayys.wayang.gollek.sdk.AgentRunResult;
import tech.kayys.wayang.gollek.sdk.AgentRunState;
import tech.kayys.wayang.gollek.sdk.AgentRunStatus;
import tech.kayys.wayang.gollek.sdk.AgentSkillDiscovery;
import tech.kayys.wayang.gollek.sdk.HarnessPlan;
import tech.kayys.wayang.gollek.sdk.HarnessPlanRequest;
import tech.kayys.wayang.gollek.sdk.RegisteredSkill;
import tech.kayys.wayang.gollek.sdk.Wayang;
import tech.kayys.wayang.gollek.sdk.WayangGollekSdk;
import tech.kayys.wayang.gollek.sdk.WayangGollekSdkConfig;
import tech.kayys.wayang.gollek.sdk.WayangGollekSdkProvider;
import tech.kayys.wayang.gollek.sdk.WayangPlatformStatus;
import tech.kayys.wayang.gollek.sdk.WayangWorkbenchCatalog;
import tech.kayys.wayang.gollek.sdk.WayangWorkbenchModel;
import tech.kayys.wayang.gollek.sdk.WorkbenchCommandDiscovery;
import tech.kayys.wayang.gollek.sdk.WorkbenchCommandQuery;
import tech.kayys.wayang.gollek.sdk.WorkspaceInspectionRequest;
import tech.kayys.wayang.gollek.sdk.WorkspaceSnapshot;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RemoteWayangGollekSdkTest {

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void registersRemoteProviderThroughServiceLoader() {
        assertThat(ServiceLoader.load(WayangGollekSdkProvider.class))
                .anySatisfy(provider -> {
                    assertThat(provider).isInstanceOf(RemoteWayangGollekSdkProvider.class);
                    assertThat(provider.mode()).isEqualTo(WayangGollekSdkProvider.Mode.REMOTE);
                });
    }

    @Test
    void callsRemoteStatusAndRunEndpoints() throws Exception {
        AtomicReference<String> runBody = new AtomicReference<>();
        AtomicReference<String> workspaceBody = new AtomicReference<>();
        AtomicReference<String> harnessBody = new AtomicReference<>();
        AtomicReference<String> authorization = new AtomicReference<>();
        AtomicReference<String> statusPath = new AtomicReference<>();
        AtomicReference<String> eventsPath = new AtomicReference<>();
        AtomicReference<String> historyQuery = new AtomicReference<>();
        AtomicReference<String> cancelPath = new AtomicReference<>();
        AtomicReference<String> cancelBody = new AtomicReference<>();
        AtomicReference<String> forgetPath = new AtomicReference<>();
        AtomicReference<String> skillsPath = new AtomicReference<>();
        AtomicReference<String> skillDetailPath = new AtomicReference<>();
        startServer(
                runBody,
                workspaceBody,
                harnessBody,
                authorization,
                statusPath,
                eventsPath,
                historyQuery,
                cancelPath,
                cancelBody,
                forgetPath,
                skillsPath,
                skillDetailPath);

        WayangGollekSdk sdk = Wayang.create(new WayangGollekSdkConfig(
                WayangGollekSdkProvider.Mode.REMOTE,
                endpoint(),
                "secret",
                "tenant-r",
                "model-r"));

        WayangPlatformStatus status = sdk.status();
        WayangWorkbenchModel workbench = sdk.workbench();
        WayangWorkbenchModel filteredWorkbench = sdk.workbench(WorkbenchCommandQuery.of(
                "assistant-agent",
                "Runs",
                "run-session-context"));
        WorkbenchCommandDiscovery commandDiscovery = sdk.commandDiscovery(WorkbenchCommandQuery.of(
                "assistant-agent",
                "Runs",
                null));
        AgentSkillQuery skillQuery = new AgentSkillQuery(
                "assistant-agent",
                null,
                "remote-rag",
                null,
                null,
                "remote",
                "query",
                "citations");
        AgentSkillDiscovery remoteDiscovery = sdk.skillDiscovery(skillQuery, "remote");
        java.util.List<RegisteredSkill> remoteSkills = remoteDiscovery.skills();
        RegisteredSkill remoteSkill = sdk.skill("remote-rag");
        WorkspaceSnapshot workspace = sdk.inspectWorkspace(new WorkspaceInspectionRequest(
                "/repo",
                12,
                true));
        HarnessPlan harness = sdk.planHarness(new HarnessPlanRequest(
                "/repo",
                5,
                true));
        AgentRunResult result = sdk.run(AgentRunRequest.builder()
                .prompt(" draft a workflow ")
                .systemPrompt("keep remote boundaries")
                .tenantId("default")
                .workflowId("planner")
                .skill("rag")
                .sessionId("session-r")
                .userId("user-r")
                .context("rag.collection", "docs")
                .memoryEnabled(true)
                .maxSteps(4)
                .workspace("/repo", 12)
                .harness(5, false)
                .build());
        AgentRunStatus runStatus = sdk.runStatus("remote-test");
        AgentRunEvents runEvents = sdk.runEvents(
                "remote-test",
                AgentRunEventsQuery.of("completed", "run.completed", 3L, 7));
        AgentRunInspection inspection = sdk.inspectRun(
                "remote-test",
                AgentRunEventsQuery.of("completed", "run.completed", 3L, 7));
        AgentRunHistory history = sdk.runHistory(new AgentRunHistoryQuery(AgentRunState.COMPLETED, 3));
        AgentRunCancelResult cancel = sdk.cancelRun("remote-test", "user stop");
        AgentRunForgetResult forget = sdk.forgetRun("remote-test");

        assertThat(status.productName()).isEqualTo("Wayang");
        assertThat(status.gollek().state()).isEqualTo("remote");
        assertThat(status.activeSkills()).isEqualTo(1);
        assertThat(status.notes()).anySatisfy(note -> assertThat(note).contains(endpoint()));
        assertThat(workbench.commandPalette())
                .containsExactlyElementsOf(WayangWorkbenchCatalog.remoteCommandPalette())
                .contains("run <task> --workflow <gamelan-workflow> --skill <skill-id>")
                .doesNotContain("tui");
        assertThat(workbench.commands())
                .containsExactlyElementsOf(WayangWorkbenchCatalog.remoteCommands())
                .noneSatisfy(command -> assertThat(command.localOnly()).isTrue());
        assertThat(filteredWorkbench.commandPalette())
                .containsExactly("run <task> --session <id> --user <id> --context rag.collection=<name>");
        assertThat(filteredWorkbench.commands())
                .singleElement()
                .satisfies(command -> {
                    assertThat(command.id()).isEqualTo("run-session-context");
                    assertThat(command.localOnly()).isFalse();
                });
        assertThat(commandDiscovery.totalCommands()).isEqualTo(WayangWorkbenchCatalog.remoteCommands().size());
        assertThat(commandDiscovery.matchingCommands()).isEqualTo(26);
        assertThat(commandDiscovery.categorySummaries())
                .singleElement()
                .satisfies(summary -> {
                    assertThat(summary.name()).isEqualTo("Runs");
                    assertThat(summary.commandIds()).contains(
                            "run-result-json",
                            "run-preflight-json",
                            "run-profile",
                            "run-session-context",
                            "run-events-follow-json",
                            "run-events-follow-result-json",
                            "run-events-follow-result-only-json",
                            "run-events-follow-result-only-stats-json",
                            "run-list-profile-json",
                            "run-stats-profile-json");
                    assertThat(summary.commandIds()).doesNotContain("tui");
                });
        assertThat(commandDiscovery.commandIds())
                .contains(
                        "run-profile",
                        "run-session-context",
                        "run-events-follow-json",
                        "run-events-follow-result-json",
                        "run-events-follow-result-only-json",
                        "run-events-follow-result-only-stats-json",
                        "run-list-profile-json",
                        "run-stats-profile-json")
                .doesNotContain("tui");
        assertThat(remoteDiscovery.query()).isEqualTo(skillQuery);
        assertThat(remoteDiscovery.search()).isEqualTo("remote");
        assertThat(remoteDiscovery.totalSkills()).isEqualTo(7);
        assertThat(remoteDiscovery.matchingSkills()).isEqualTo(1);
        assertThat(remoteDiscovery.categoryCounts()).containsEntry("Retrieval", 1);
        assertThat(remoteDiscovery.sourceCounts()).containsEntry("remote-rag", 1);
        assertThat(remoteSkills)
                .singleElement()
                .satisfies(skill -> {
                    assertThat(skill.id()).isEqualTo("remote.rag");
                    assertThat(skill.state().name()).isEqualTo("PREVIEW");
                    assertThat(skill.descriptor().source()).isEqualTo("remote-rag");
                    assertThat(skill.descriptor().surfaceIds()).containsExactly("assistant-agent");
                    assertThat(skill.descriptor().inputKeys()).containsExactly("query");
                    assertThat(skill.descriptor().outputKeys()).containsExactly("citations");
                    assertThat(skill.descriptor().tags()).contains("rag", "remote");
                    assertThat(skill.aliases()).containsExactly("remote-rag");
                    assertThat(skill.descriptor().metadata()).containsEntry("priority", 3);
                });
        assertThat(remoteSkill.id()).isEqualTo("remote.rag");
        assertThat(remoteSkill.descriptor().version()).isEqualTo("2.0.0");
        assertThat(workspace.notes()).anySatisfy(note -> assertThat(note).contains("Remote workspace endpoint returned HTTP 200"));
        assertThat(workspaceBody.get())
                .contains("\"rootPath\":\"/repo\"")
                .contains("\"maxEntries\":12")
                .contains("\"includeHidden\":true");
        assertThat(harness.notes()).anySatisfy(note -> assertThat(note).contains("Remote harness endpoint returned HTTP 200"));
        assertThat(harnessBody.get())
                .contains("\"rootPath\":\"/repo\"")
                .contains("\"maxChecks\":5")
                .contains("\"includeOptional\":true");
        assertThat(result.successful()).isTrue();
        assertThat(result.runId()).isEqualTo("remote-test");
        assertThat(result.strategy()).isEqualTo("remote-submit");
        assertThat(result.handle().state()).isEqualTo(AgentRunState.RUNNING);
        assertThat(result.handle().strategy()).isEqualTo("remote-submit");
        assertThat(runStatus.known()).isTrue();
        assertThat(runStatus.handle().runId()).isEqualTo("remote-test");
        assertThat(runStatus.handle().state()).isEqualTo(AgentRunState.RUNNING);
        assertThat(runStatus.handle().strategy()).isEqualTo("remote-status");
        assertThat(runStatus.metadata())
                .containsEntry("endpoint", endpoint())
                .containsEntry("httpStatus", 200)
                .containsEntry("tenant", "tenant-status")
                .containsKey("responsePreview");
        assertThat(runEvents.totalEvents()).isEqualTo(1);
        assertThat(runEvents.query().limit()).isEqualTo(7);
        assertThat(runEvents.query().afterSequence()).isEqualTo(3);
        assertThat(runEvents.cursor()).isEqualTo(new AgentRunEventsCursor(3, 4, 4, 4, 7, 1, 1));
        assertThat(runEvents.summary()).isEqualTo(new AgentRunEventsSummary(
                1,
                1,
                Map.of("completed", 1),
                Map.of("run.completed", 1)));
        assertThat(runEvents.lastSequence()).isEqualTo(4);
        assertThat(runEvents.nextAfterSequence()).isEqualTo(4);
        assertThat(runEvents.events())
                .singleElement()
                .satisfies(event -> {
                    assertThat(event.runId()).isEqualTo("remote-test");
                    assertThat(event.state()).isEqualTo(AgentRunState.COMPLETED);
                    assertThat(event.type()).isEqualTo("run.completed");
                    assertThat(event.metadata())
                            .containsEntry("endpoint", endpoint())
                            .containsEntry("httpStatus", 200)
                            .containsEntry("tenant", "tenant-event")
                            .containsEntry("retry", 2)
                            .containsKey("responsePreview");
                });
        assertThat(inspection.known()).isTrue();
        assertThat(inspection.status().handle().state()).isEqualTo(AgentRunState.RUNNING);
        assertThat(inspection.events().nextAfterSequence()).isEqualTo(4);
        assertThat(inspection.events().stateCounts()).containsEntry("completed", 1);
        assertThat(history.query().state()).isEqualTo(AgentRunState.COMPLETED);
        assertThat(history.query().limit()).isEqualTo(3);
        assertThat(history.totalRuns()).isEqualTo(2);
        assertThat(history.returnedRuns()).isEqualTo(1);
        assertThat(history.pageSize()).isEqualTo(3);
        assertThat(history.offset()).isZero();
        assertThat(history.windowStart()).isEqualTo(1);
        assertThat(history.windowEnd()).isEqualTo(1);
        assertThat(history.previousOffset()).isZero();
        assertThat(history.hasPrevious()).isFalse();
        assertThat(history.nextOffset()).isEqualTo(1);
        assertThat(history.hasMore()).isTrue();
        assertThat(history.truncated()).isTrue();
        assertThat(history.stateCounts()).containsEntry("completed", 1);
        assertThat(history.surfaceCounts()).containsEntry("assistant-agent", 1);
        assertThat(history.message()).isEqualTo("remote history");
        assertThat(history.runs())
                .singleElement()
                .satisfies(runStatusSnapshot -> {
                    assertThat(runStatusSnapshot.handle().runId()).isEqualTo("remote-done");
                    assertThat(runStatusSnapshot.handle().state()).isEqualTo(AgentRunState.COMPLETED);
                    assertThat(runStatusSnapshot.metadata())
                            .containsEntry("endpoint", endpoint())
                            .containsEntry("httpStatus", 200)
                            .containsEntry("surface", "assistant-agent")
                            .containsKey("responsePreview");
                });
        assertThat(cancel.cancelled()).isTrue();
        assertThat(cancel.handle().runId()).isEqualTo("remote-test");
        assertThat(cancel.handle().state()).isEqualTo(AgentRunState.CANCELLED);
        assertThat(cancel.handle().strategy()).isEqualTo("remote-cancel");
        assertThat(cancel.metadata())
                .containsEntry("endpoint", endpoint())
                .containsEntry("httpStatus", 200)
                .containsEntry("tenant", "tenant-cancel")
                .containsEntry("reason", "user stop")
                .containsKey("responsePreview");
        assertThat(forget.runId()).isEqualTo("remote-test");
        assertThat(forget.forgotten()).isTrue();
        assertThat(forget.message()).isEqualTo("forgotten");
        assertThat(forget.metadata())
                .containsEntry("endpoint", endpoint())
                .containsEntry("httpStatus", 200)
                .containsEntry("tenant", "tenant-forget")
                .containsEntry("state", "COMPLETED")
                .containsEntry("strategy", "remote-forget")
                .containsKey("responsePreview");
        assertThat(result.metadata())
                .containsEntry("sdkMode", "remote")
                .containsEntry("tenant", "tenant-r")
                .containsEntry("session", "session-r")
                .containsEntry("user", "user-r")
                .containsEntry("systemPrompt", "keep remote boundaries")
                .containsEntry("model", "model-r")
                .containsEntry("workflow", "planner")
                .containsEntry("surface", "coding-agent")
                .containsEntry("httpStatus", 200)
                .containsEntry("remoteRunId", "remote-test")
                .containsEntry("remoteRunState", "RUNNING")
                .containsKey("surfacePolicy")
                .containsKey("surfacePolicyAssessment")
                .containsKey("skillAssessment")
                .containsKey("runReadiness")
                .containsKey("remoteMetadata")
                .containsKey("workspace")
                .containsKey("harness");
        @SuppressWarnings("unchecked")
        Map<String, Object> context = (Map<String, Object>) result.metadata().get("context");
        assertThat(context).containsEntry("rag.collection", "docs");
        @SuppressWarnings("unchecked")
        Map<String, Object> remoteMetadata = (Map<String, Object>) result.metadata().get("remoteMetadata");
        assertThat(remoteMetadata)
                .containsEntry("queue", "priority")
                .containsEntry("attempt", 1)
                .containsEntry("endpoint", "shadow")
                .containsEntry("httpStatus", 499);
        assertThat(result.metadata())
                .containsEntry("endpoint", endpoint())
                .containsEntry("httpStatus", 200);
        @SuppressWarnings("unchecked")
        Map<String, Object> surfacePolicy = (Map<String, Object>) result.metadata().get("surfacePolicy");
        assertThat(surfacePolicy)
                .containsEntry("surfaceId", "coding-agent")
                .containsEntry("workspacePreferred", true)
                .containsEntry("harnessPreferred", true);
        @SuppressWarnings("unchecked")
        Map<String, Object> surfaceAssessment = (Map<String, Object>) result.metadata().get("surfacePolicyAssessment");
        assertThat(surfaceAssessment)
                .containsEntry("surfaceId", "coding-agent")
                .containsEntry("ready", true);
        @SuppressWarnings("unchecked")
        Map<String, Object> skillAssessment = (Map<String, Object>) result.metadata().get("skillAssessment");
        assertThat(skillAssessment)
                .containsEntry("surfaceId", "coding-agent")
                .containsEntry("ready", false);
        @SuppressWarnings("unchecked")
        java.util.List<Object> unknownSkills = (java.util.List<Object>) skillAssessment.get("unknownSkills");
        assertThat(unknownSkills).contains("rag");
        assertThat(authorization.get()).isEqualTo("Bearer secret");
        assertThat(statusPath.get()).isEqualTo("/runs/remote-test/status");
        assertThat(eventsPath.get()).isEqualTo("/runs/remote-test/events?limit=7&state=completed&type=run.completed&afterSequence=3");
        assertThat(historyQuery.get()).contains("state=completed").contains("limit=3");
        assertThat(cancelPath.get()).isEqualTo("/runs/remote-test/cancel");
        assertThat(cancelBody.get()).contains("\"reason\":\"user stop\"");
        assertThat(forgetPath.get()).isEqualTo("/runs/remote-test");
        assertThat(skillsPath.get())
                .startsWith("/skills?")
                .contains("surfaceId=assistant-agent")
                .contains("source=remote-rag")
                .contains("tag=remote")
                .contains("inputKey=query")
                .contains("outputKey=citations")
                .contains("search=remote");
        assertThat(skillDetailPath.get()).isEqualTo("/skills/remote-rag");
        assertThat(runBody.get())
                .contains("\"prompt\":\"draft a workflow\"")
                .contains("\"systemPrompt\":\"keep remote boundaries\"")
                .contains("\"tenantId\":\"tenant-r\"")
                .contains("\"modelId\":\"model-r\"")
                .contains("\"workflowId\":\"planner\"")
                .contains("\"surfaceId\":\"coding-agent\"")
                .contains("\"sessionId\":\"session-r\"")
                .contains("\"userId\":\"user-r\"")
                .contains("\"context\":{\"rag.collection\":\"docs\"}")
                .contains("\"skills\":[\"rag\"]")
                .contains("\"workspacePath\":\"/repo\"")
                .contains("\"workspaceEnabled\":true")
                .contains("\"workspaceMaxEntries\":12")
                .contains("\"harnessEnabled\":true")
                .contains("\"harnessMaxChecks\":5")
                .contains("\"harnessIncludeOptional\":false");
    }

    @Test
    void requiresRemoteEndpoint() {
        assertThatThrownBy(() -> Wayang.create(new WayangGollekSdkConfig(
                WayangGollekSdkProvider.Mode.REMOTE,
                "",
                "",
                "default",
                "")))
                .isInstanceOf(RemoteWayangGollekException.class)
                .hasMessageContaining("endpoint is required");
    }

    @Test
    void rejectsUnknownProductSurfaceBeforeCallingRemoteApi() {
        WayangGollekSdk sdk = Wayang.create(new WayangGollekSdkConfig(
                WayangGollekSdkProvider.Mode.REMOTE,
                "http://127.0.0.1:1",
                "",
                "default",
                ""));

        assertThatThrownBy(() -> sdk.run(AgentRunRequest.builder()
                .prompt("draft")
                .surfaceId("future-agent")
                .build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown Wayang product surface 'future-agent'")
                .hasMessageContaining("coding-agent")
                .hasMessageContaining("assistant-agent");
    }

    @Test
    void resolvesSkillThroughFilteredDiscoveryWhenDetailEndpointIsMissing() throws Exception {
        AtomicReference<String> detailPath = new AtomicReference<>();
        AtomicReference<String> discoveryPath = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/skills", exchange -> {
            String path = exchange.getRequestURI().getPath();
            if ("/skills/remote-rag".equals(path)) {
                detailPath.set(path);
                respond(exchange, 404, "{}");
                return;
            }
            discoveryPath.set(exchange.getRequestURI().toString());
            respond(exchange, 200, "{\"totalSkills\":1,\"skills\":[" + remoteSkillJson() + "]}");
        });
        server.start();

        WayangGollekSdk sdk = Wayang.create(new WayangGollekSdkConfig(
                WayangGollekSdkProvider.Mode.REMOTE,
                endpoint(),
                "",
                "default",
                ""));

        RegisteredSkill skill = sdk.skill("remote-rag");

        assertThat(skill.id()).isEqualTo("remote.rag");
        assertThat(skill.descriptor().source()).isEqualTo("remote-rag");
        assertThat(detailPath.get()).isEqualTo("/skills/remote-rag");
        assertThat(discoveryPath.get())
                .startsWith("/skills?")
                .contains("skillId=remote-rag");
    }

    @Test
    void mapsSkillDetailFromListEnvelopeResponse() throws Exception {
        AtomicReference<String> detailPath = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/skills", exchange -> {
            detailPath.set(exchange.getRequestURI().toString());
            respond(exchange, 200, "{\"totalSkills\":1,\"skills\":[" + remoteSkillJson() + "]}");
        });
        server.start();

        WayangGollekSdk sdk = Wayang.create(new WayangGollekSdkConfig(
                WayangGollekSdkProvider.Mode.REMOTE,
                endpoint(),
                "",
                "default",
                ""));

        RegisteredSkill skill = sdk.skill("remote-rag");

        assertThat(skill.id()).isEqualTo("remote.rag");
        assertThat(skill.aliases()).containsExactly("remote-rag");
        assertThat(detailPath.get()).isEqualTo("/skills/remote-rag");
    }

    private void startServer(
            AtomicReference<String> runBody,
            AtomicReference<String> workspaceBody,
            AtomicReference<String> harnessBody,
            AtomicReference<String> authorization,
            AtomicReference<String> statusPath,
            AtomicReference<String> eventsPath,
            AtomicReference<String> historyQuery,
            AtomicReference<String> cancelPath,
            AtomicReference<String> cancelBody,
            AtomicReference<String> forgetPath,
            AtomicReference<String> skillsPath,
            AtomicReference<String> skillDetailPath) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/status", exchange -> respond(exchange, 200, "{\"activeSkills\":1}"));
        server.createContext("/skills", exchange -> {
            String path = exchange.getRequestURI().getPath();
            if ("GET".equals(exchange.getRequestMethod()) && "/skills/remote-rag".equals(path)) {
                skillDetailPath.set(path);
                respond(exchange, 200, "{\"skill\":" + remoteSkillJson() + "}");
                return;
            }
            if (exchange.getRequestURI().getQuery() != null) {
                skillsPath.set(exchange.getRequestURI().toString());
            }
            respond(exchange, 200, "{\"totalSkills\":7,\"skills\":[" + remoteSkillJson() + "]}");
        });
        server.createContext("/workspace/inspect", exchange -> {
            workspaceBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            respond(exchange, 200, "{\"workspace\":\"remote\"}");
        });
        server.createContext("/harness/plan", exchange -> {
            harnessBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            respond(exchange, 200, "{\"checks\":[]}");
        });
        server.createContext("/runs", exchange -> {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();
            if ("GET".equals(method) && path.endsWith("/status")) {
                statusPath.set(path);
                respond(exchange, 200, """
                        {"runId":"remote-test","state":"RUNNING","strategy":"remote-status","known":true,"message":"remote status","metadata":{"tenant":"tenant-status","endpoint":"shadow"}}
                        """);
                return;
            }
            if ("GET".equals(method) && path.endsWith("/events")) {
                eventsPath.set(exchange.getRequestURI().toString());
                respond(exchange, 200, """
                        {"runId":"remote-test","totalEvents":1,"message":"remote events","events":[{"runId":"remote-test","sequence":4,"type":"run.completed","state":"COMPLETED","message":"done","metadata":{"tenant":"tenant-event","retry":2}}]}
                        """);
                return;
            }
            if ("GET".equals(method) && "/runs".equals(path)) {
                historyQuery.set(exchange.getRequestURI().getQuery());
                respond(exchange, 200, """
                        {"totalRuns":2,"message":"remote history","runs":[{"runId":"remote-done","state":"COMPLETED","strategy":"remote-status","known":true,"message":"done","metadata":{"surface":"assistant-agent"}}]}
                        """);
                return;
            }
            if ("POST".equals(method) && path.endsWith("/cancel")) {
                cancelPath.set(path);
                cancelBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                respond(exchange, 200, """
                        {"runId":"remote-test","state":"CANCELLED","strategy":"remote-cancel","cancelled":true,"message":"cancelled","metadata":{"tenant":"tenant-cancel"}}
                        """);
                return;
            }
            if ("DELETE".equals(method) && "/runs/remote-test".equals(path)) {
                forgetPath.set(path);
                respond(exchange, 200, """
                        {"runId":"remote-test","state":"COMPLETED","strategy":"remote-forget","forgotten":true,"message":"forgotten","metadata":{"tenant":"tenant-forget"}}
                        """);
                return;
            }
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            runBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            respond(exchange, 200, """
                    {"runId":"remote-test","state":"RUNNING","strategy":"remote-submit","metadata":{"queue":"priority","attempt":1,"endpoint":"shadow","httpStatus":499}}
                    """);
        });
        server.start();
    }

    private static String remoteSkillJson() {
        return """
                {"id":"remote.rag","name":"Remote RAG","description":"Remote RAG capability.","category":"Retrieval","source":"remote-rag","version":"2.0.0","state":"PREVIEW","surfaceIds":["assistant-agent"],"inputKeys":["query"],"outputKeys":["citations"],"tags":["rag","remote"],"aliases":["remote-rag"],"metadata":{"endpoint":"remote-skills","priority":3}}
                """;
    }

    private String endpoint() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private void respond(HttpExchange exchange, int statusCode, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
