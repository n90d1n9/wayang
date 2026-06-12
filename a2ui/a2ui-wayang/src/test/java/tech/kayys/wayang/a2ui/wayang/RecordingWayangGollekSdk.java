package tech.kayys.wayang.a2ui.wayang;

import tech.kayys.wayang.gollek.sdk.AgentRunCancelResult;
import tech.kayys.wayang.gollek.sdk.AgentRunEvent;
import tech.kayys.wayang.gollek.sdk.AgentRunEvents;
import tech.kayys.wayang.gollek.sdk.AgentRunEventsQuery;
import tech.kayys.wayang.gollek.sdk.AgentRunHandle;
import tech.kayys.wayang.gollek.sdk.AgentRunHistory;
import tech.kayys.wayang.gollek.sdk.AgentRunHistoryQuery;
import tech.kayys.wayang.gollek.sdk.AgentRunInspection;
import tech.kayys.wayang.gollek.sdk.AgentRunRequest;
import tech.kayys.wayang.gollek.sdk.AgentRunResult;
import tech.kayys.wayang.gollek.sdk.AgentRunState;
import tech.kayys.wayang.gollek.sdk.AgentRunStatus;
import tech.kayys.wayang.gollek.sdk.AgentRunWaitOptions;
import tech.kayys.wayang.gollek.sdk.AgentRunWaitResult;
import tech.kayys.wayang.gollek.sdk.HarnessPlan;
import tech.kayys.wayang.gollek.sdk.HarnessPlanRequest;
import tech.kayys.wayang.gollek.sdk.ProductSurface;
import tech.kayys.wayang.gollek.sdk.WayangGollekSdk;
import tech.kayys.wayang.gollek.sdk.WayangPlatformStatus;
import tech.kayys.wayang.gollek.sdk.WayangWorkbenchModel;
import tech.kayys.wayang.gollek.sdk.WorkspaceInspectionRequest;
import tech.kayys.wayang.gollek.sdk.WorkspaceSnapshot;

import java.util.List;
import java.util.Map;

final class RecordingWayangGollekSdk implements WayangGollekSdk {

    int inspected;
    int cancelled;
    int waited;
    int historyQueried;
    int eventsQueried;
    AgentRunHistoryQuery lastHistoryQuery = AgentRunHistoryQuery.all();
    AgentRunEventsQuery lastEventsQuery = AgentRunEventsQuery.all();
    String lastCancelReason = "";

    @Override
    public WayangPlatformStatus status() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ProductSurface> productSurfaces() {
        return List.of();
    }

    @Override
    public WayangWorkbenchModel workbench() {
        throw new UnsupportedOperationException();
    }

    @Override
    public WorkspaceSnapshot inspectWorkspace(WorkspaceInspectionRequest request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public HarnessPlan planHarness(HarnessPlanRequest request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public AgentRunResult run(AgentRunRequest request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public AgentRunInspection inspectRun(String runId, AgentRunEventsQuery query) {
        inspected++;
        return new AgentRunInspection(
                runId,
                status(runId, AgentRunState.RUNNING, "Run is running."),
                null,
                "Inspected run.");
    }

    @Override
    public AgentRunHistory runHistory(AgentRunHistoryQuery query) {
        historyQueried++;
        lastHistoryQuery = query == null ? AgentRunHistoryQuery.all() : query;
        return new AgentRunHistory(
                lastHistoryQuery,
                List.of(
                        status("run-1", AgentRunState.RUNNING, "Run is running."),
                        status("run-2", AgentRunState.COMPLETED, "Run completed.")),
                2,
                "Loaded run history.");
    }

    @Override
    public AgentRunEvents runEvents(String runId, AgentRunEventsQuery query) {
        eventsQueried++;
        lastEventsQuery = query == null ? AgentRunEventsQuery.all() : query;
        return new AgentRunEvents(
                runId,
                lastEventsQuery,
                List.of(
                        new AgentRunEvent(runId, 1, "run.running", AgentRunState.RUNNING, "Run is running.", Map.of()),
                        new AgentRunEvent(runId, 2, "run.completed", AgentRunState.COMPLETED, "Run completed.", Map.of())),
                2,
                "Loaded run events.");
    }

    @Override
    public AgentRunCancelResult cancelRun(String runId, String reason) {
        cancelled++;
        lastCancelReason = reason;
        return AgentRunCancelResult.cancelled(status(runId, AgentRunState.CANCELLED, "Run was cancelled."));
    }

    @Override
    public AgentRunWaitResult waitForRun(String runId, AgentRunWaitOptions options) {
        waited++;
        AgentRunStatus status = status(runId, AgentRunState.COMPLETED, "Run completed.");
        return new AgentRunWaitResult(runId, status, true, false, 1, 0, "Run completed.", Map.of());
    }

    private static AgentRunStatus status(String runId, AgentRunState state, String message) {
        return new AgentRunStatus(new AgentRunHandle(runId, state, "react"), true, message, Map.of());
    }
}
