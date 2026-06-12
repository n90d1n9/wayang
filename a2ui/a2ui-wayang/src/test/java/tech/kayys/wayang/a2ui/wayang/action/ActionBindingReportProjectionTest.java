package tech.kayys.wayang.a2ui.wayang.action;

import tech.kayys.wayang.a2ui.wayang.WayangA2uiActionBindingReport;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ActionBindingReportProjectionTest {

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
}
