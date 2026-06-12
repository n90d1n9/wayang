package tech.kayys.wayang.a2ui.wayang;

import tech.kayys.wayang.a2ui.wayang.action.ActionBindingReportProjection;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2uiActionBindingReportTest {

    @Test
    void reportsCompletePolicyAndHandlerCoverage() {
        WayangA2uiActionBindingReport report = WayangA2uiActionBindingReport.of(
                WayangA2uiActionPolicy.runLifecycle(),
                WayangA2uiActionHandlers.standard(
                        new RecordingWayangGollekSdk(),
                        WayangA2uiSurfaceRegistry.readOnly()));

        assertThat(report.complete()).isTrue();
        assertThat(report.policyActionCount()).isEqualTo(5);
        assertThat(report.handlerActionCount()).isEqualTo(5);
        assertThat(report.policyActions())
                .containsExactly(
                        WayangA2uiActions.RUN_INSPECT,
                        WayangA2uiActions.RUN_HISTORY,
                        WayangA2uiActions.RUN_EVENTS,
                        WayangA2uiActions.RUN_WAIT,
                        WayangA2uiActions.RUN_CANCEL);
        assertThat(report.handlerActions()).containsExactlyElementsOf(report.policyActions());
        assertThat(report.missingHandlerActions()).isEmpty();
        assertThat(report.orphanHandlerActions()).isEmpty();
    }

    @Test
    void reportsMissingAndOrphanActionHandlersInStableOrder() {
        WayangA2uiActionPolicy policy = new WayangA2uiActionPolicy(
                Set.of(WayangA2uiActions.RUN_INSPECT, WayangA2uiActions.RUN_WAIT, "custom.allowed"),
                Set.of(),
                Map.of());
        WayangA2uiActionHandlers handlers = WayangA2uiActionHandlers.builder()
                .register(WayangA2uiActions.RUN_INSPECT, (action, runId) -> WayangA2uiActionResult.rejected(
                        action.name(),
                        runId,
                        "unused"))
                .register("custom.orphan", (action, runId) -> WayangA2uiActionResult.rejected(
                        action.name(),
                        runId,
                        "unused"))
                .build();

        WayangA2uiActionBindingReport report = handlers.bindingReport(policy);

        assertThat(report.complete()).isFalse();
        assertThat(report.policyActions())
                .containsExactly(WayangA2uiActions.RUN_INSPECT, WayangA2uiActions.RUN_WAIT, "custom.allowed");
        assertThat(report.handlerActions())
                .containsExactly(WayangA2uiActions.RUN_INSPECT, "custom.orphan");
        assertThat(report.missingHandlerActions())
                .containsExactly(WayangA2uiActions.RUN_WAIT, "custom.allowed");
        assertThat(report.orphanHandlerActions()).containsExactly("custom.orphan");
    }

    @Test
    void nullPolicyUsesDefaultPolicyForBindingReport() {
        WayangA2uiActionBindingReport report = WayangA2uiActionBindingReport.of(null, null);

        assertThat(report.policyActions()).containsExactly(WayangA2uiActions.RUN_INSPECT);
        assertThat(report.handlerActions()).isEmpty();
        assertThat(report.missingHandlerActions()).containsExactly(WayangA2uiActions.RUN_INSPECT);
        assertThat(report.orphanHandlerActions()).isEmpty();
    }

    @Test
    void projectsOrderedReportAndRecordDelegates() {
        WayangA2uiActionBindingReport report = new WayangA2uiActionBindingReport(
                List.of(" action.one ", "action.two", "action.one", "", " "),
                List.of("action.two", " action.extra ", "action.two"),
                List.of(" action.one ", "action.one"),
                List.of(" action.extra "));

        Map<String, Object> values = ActionBindingReportProjection.report(report);

        assertThat(report.toMap()).isEqualTo(values);
        assertThat(values.keySet()).containsExactly(
                "complete",
                "policyActionCount",
                "handlerActionCount",
                "policyActions",
                "handlerActions",
                "missingHandlerActions",
                "orphanHandlerActions");
        assertThat(values)
                .containsEntry("complete", false)
                .containsEntry("policyActionCount", 2)
                .containsEntry("handlerActionCount", 2);
        assertThat((Iterable<String>) values.get("policyActions"))
                .containsExactly("action.one", "action.two");
        assertThat((Iterable<String>) values.get("handlerActions"))
                .containsExactly("action.two", "action.extra");
        assertThat((Iterable<String>) values.get("missingHandlerActions"))
                .containsExactly("action.one");
        assertThat((Iterable<String>) values.get("orphanHandlerActions"))
                .containsExactly("action.extra");
    }

    @Test
    void routerExposesPolicyHandlerBindingReport() {
        WayangA2uiActionPolicy policy = new WayangA2uiActionPolicy(Set.of("custom.action"), Set.of(), Map.of());
        WayangA2uiActionRouter router = new WayangA2uiActionRouter(
                policy,
                WayangA2uiSurfaceRegistry.readOnly(),
                WayangA2uiActionHandlers.builder().build());

        WayangA2uiActionBindingReport report = router.actionBindingReport();

        assertThat(report.complete()).isFalse();
        assertThat(report.policyActions()).containsExactly("custom.action");
        assertThat(report.handlerActions()).isEmpty();
        assertThat(report.missingHandlerActions()).containsExactly("custom.action");
    }
}
