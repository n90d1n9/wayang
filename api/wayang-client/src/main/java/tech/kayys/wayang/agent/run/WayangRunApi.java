package tech.kayys.wayang.agent.run;

import java.util.Map;
import java.util.function.Consumer;

import tech.kayys.wayang.agent.event.AgentRunEventEnvelopes;
import tech.kayys.wayang.agent.event.AgentRunEvents;
import tech.kayys.wayang.agent.event.AgentRunEventsFollowOptions;
import tech.kayys.wayang.agent.event.AgentRunEventsFollowResult;
import tech.kayys.wayang.agent.event.AgentRunEventsQuery;
import tech.kayys.wayang.agent.history.AgentRunHistory;
import tech.kayys.wayang.agent.history.AgentRunHistoryEnvelopes;
import tech.kayys.wayang.agent.history.AgentRunHistoryQuery;
import tech.kayys.wayang.agent.store.AgentRunStoreCompactionPreview;
import tech.kayys.wayang.agent.store.AgentRunStoreCompactionPreviewEnvelopes;
import tech.kayys.wayang.agent.store.AgentRunStoreCompactionResult;
import tech.kayys.wayang.agent.store.AgentRunStoreCompactionResultEnvelopes;
import tech.kayys.wayang.agent.store.AgentRunStoreDiagnostics;
import tech.kayys.wayang.agent.store.AgentRunStoreDiagnosticsEnvelopes;
import tech.kayys.wayang.agent.store.AgentRunStoreVerification;
import tech.kayys.wayang.agent.store.AgentRunStoreVerificationEnvelopes;
import tech.kayys.wayang.agent.store.AgentRunStoreVerificationPolicy;
import tech.kayys.wayang.client.Wayang;
import tech.kayys.wayang.client.WayangGollekSdk;
import tech.kayys.wayang.client.WayangWireApi;

/**
 * Run lifecycle API for preflight, preview, execution, event, history, and control surfaces.
 *
 * <p>This facade keeps agent run lifecycle operations and JSON envelope rendering
 * in the SDK so CLI, TUI, HTTP, and future product shells share one contract boundary.</p>
 */
public final class WayangRunApi {

    private final WayangGollekSdk sdk;
    private final WayangWireApi wire;

    public WayangRunApi(WayangGollekSdk sdk, WayangWireApi wire) {
        this.sdk = sdk == null ? Wayang.local() : sdk;
        this.wire = wire == null ? new WayangWireApi() : wire;
    }

    public AgentRunReadiness preflight(AgentRunRequest request) {
        return sdk.assessRunReadiness(request);
    }

    public AgentRunPreview preview(AgentRunRequest request) {
        return sdk.previewRun(request);
    }

    public AgentRunResult run(AgentRunRequest request) {
        return sdk.run(request);
    }

    public AgentRunStatus status(String runId) {
        return sdk.runStatus(runId);
    }

    public AgentRunInspection inspect(String runId, AgentRunEventsQuery query) {
        return sdk.inspectRun(runId, query);
    }

    public AgentRunHistory history(AgentRunHistoryQuery query) {
        return sdk.runHistory(query);
    }

    public AgentRunStoreDiagnostics diagnostics() {
        return sdk.runStoreDiagnostics();
    }

    public AgentRunStoreVerification verification() {
        return sdk.runStoreVerification();
    }

    public AgentRunStoreCompactionPreview compactionPreview() {
        return sdk.runStoreCompactionPreview();
    }

    public AgentRunStoreCompactionResult compact() {
        return sdk.compactRunStore();
    }

    public AgentRunEvents events(String runId, AgentRunEventsQuery query) {
        return sdk.runEvents(runId, query);
    }

    public AgentRunEventsFollowResult followEvents(String runId, AgentRunEventsFollowOptions options) {
        return followEvents(runId, options, null);
    }

    public AgentRunEventsFollowResult followEvents(
            String runId,
            AgentRunEventsFollowOptions options,
            Consumer<AgentRunEvents> eventConsumer) {
        return sdk.followRunEvents(runId, options, eventConsumer);
    }

    public AgentRunWaitResult waitFor(String runId, AgentRunWaitOptions options) {
        return sdk.waitForRun(runId, options);
    }

    public AgentRunCancelResult cancel(String runId, String reason) {
        return sdk.cancelRun(runId, reason);
    }

    public AgentRunForgetResult forget(String runId) {
        return sdk.forgetRun(runId);
    }

    public Map<String, Object> preflightEnvelope(AgentRunReadiness readiness) {
        return AgentRunEnvelopes.preflight(readiness);
    }

    public Map<String, Object> previewEnvelope(AgentRunPreview preview) {
        return AgentRunEnvelopes.preview(preview);
    }

    public Map<String, Object> resultEnvelope(AgentRunResult result) {
        return AgentRunEnvelopes.result(result);
    }

    public Map<String, Object> statusEnvelope(AgentRunStatus status) {
        return AgentRunInspectionEnvelopes.statusEnvelope(status);
    }

    public Map<String, Object> inspectionEnvelope(AgentRunInspection inspection) {
        return AgentRunInspectionEnvelopes.inspection(inspection);
    }

    public Map<String, Object> historyEnvelope(AgentRunHistory history) {
        return AgentRunHistoryEnvelopes.history(history);
    }

    public Map<String, Object> historyStatsEnvelope(AgentRunHistory history) {
        return AgentRunHistoryEnvelopes.stats(history);
    }

    public Map<String, Object> diagnosticsEnvelope(AgentRunStoreDiagnostics diagnostics) {
        return AgentRunStoreDiagnosticsEnvelopes.diagnostics(diagnostics);
    }

    public Map<String, Object> verificationEnvelope(AgentRunStoreVerification verification) {
        return verificationEnvelope(verification, AgentRunStoreVerificationPolicy.lenient());
    }

    public Map<String, Object> verificationEnvelope(
            AgentRunStoreVerification verification,
            AgentRunStoreVerificationPolicy policy) {
        return AgentRunStoreVerificationEnvelopes.verification(
                verification == null ? sdk.runStoreVerification() : verification,
                policy);
    }

    public Map<String, Object> compactionPreviewEnvelope(AgentRunStoreCompactionPreview preview) {
        return AgentRunStoreCompactionPreviewEnvelopes.preview(
                preview == null ? sdk.runStoreCompactionPreview() : preview);
    }

    public Map<String, Object> compactionResultEnvelope(AgentRunStoreCompactionResult result) {
        return AgentRunStoreCompactionResultEnvelopes.result(
                result == null ? sdk.compactRunStore() : result);
    }

    public Map<String, Object> eventsEnvelope(AgentRunEvents events) {
        return AgentRunEventEnvelopes.events(events);
    }

    public Map<String, Object> eventsStatsEnvelope(AgentRunEvents events) {
        return AgentRunEventEnvelopes.eventsStats(events);
    }

    public Map<String, Object> followResultEnvelope(AgentRunEventsFollowResult result, boolean stats) {
        return AgentRunEventEnvelopes.followResult(result, stats);
    }

    public Map<String, Object> waitEnvelope(AgentRunWaitResult result) {
        return AgentRunControlEnvelopes.waitResult(result);
    }

    public Map<String, Object> cancelEnvelope(AgentRunCancelResult result) {
        return AgentRunControlEnvelopes.cancel(result);
    }

    public Map<String, Object> forgetEnvelope(AgentRunForgetResult result) {
        return AgentRunControlEnvelopes.forget(result);
    }

    public String preflightJson(AgentRunReadiness readiness) {
        return wire.object(preflightEnvelope(readiness));
    }

    public String previewJson(AgentRunPreview preview) {
        return wire.object(previewEnvelope(preview));
    }

    public String resultJson(AgentRunResult result) {
        return wire.object(resultEnvelope(result));
    }

    public String statusJson(AgentRunStatus status) {
        return wire.object(statusEnvelope(status));
    }

    public String inspectionJson(AgentRunInspection inspection) {
        return wire.object(inspectionEnvelope(inspection));
    }

    public String historyJson(AgentRunHistory history) {
        return wire.object(historyEnvelope(history));
    }

    public String historyStatsJson(AgentRunHistory history) {
        return wire.object(historyStatsEnvelope(history));
    }

    public String diagnosticsJson(AgentRunStoreDiagnostics diagnostics) {
        return wire.object(diagnosticsEnvelope(diagnostics));
    }

    public String verificationJson(AgentRunStoreVerification verification) {
        return wire.object(verificationEnvelope(verification));
    }

    public String verificationJson(
            AgentRunStoreVerification verification,
            AgentRunStoreVerificationPolicy policy) {
        return wire.object(verificationEnvelope(verification, policy));
    }

    public String compactionPreviewJson(AgentRunStoreCompactionPreview preview) {
        return wire.object(compactionPreviewEnvelope(preview));
    }

    public String compactionResultJson(AgentRunStoreCompactionResult result) {
        return wire.object(compactionResultEnvelope(result));
    }

    public String eventsJson(AgentRunEvents events) {
        return wire.object(eventsEnvelope(events));
    }

    public String eventsStatsJson(AgentRunEvents events) {
        return wire.object(eventsStatsEnvelope(events));
    }

    public String followResultJson(AgentRunEventsFollowResult result, boolean stats) {
        return wire.object(followResultEnvelope(result, stats));
    }

    public String waitJson(AgentRunWaitResult result) {
        return wire.object(waitEnvelope(result));
    }

    public String cancelJson(AgentRunCancelResult result) {
        return wire.object(cancelEnvelope(result));
    }

    public String forgetJson(AgentRunForgetResult result) {
        return wire.object(forgetEnvelope(result));
    }
}
