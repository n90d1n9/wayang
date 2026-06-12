package tech.kayys.wayang.a2ui.wayang.http;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HttpActionBindingProbeProjectionTest {

    @Test
    void ignoresNullActionsWhenProjectingIssues() {
        List<String> missingActions = new ArrayList<>();
        missingActions.add(null);
        missingActions.add("custom.allowed");

        List<Map<String, Object>> issues = HttpActionBindingProbeProjection.issues(missingActions);

        assertThat(issues)
                .singleElement()
                .satisfies(issue -> assertThat(issue)
                        .containsEntry("source", "actionBinding")
                        .containsEntry("field", "missingHandlerActions")
                        .containsEntry("action", "custom.allowed")
                        .containsEntry("message",
                                "A2UI action policy allows an action with no registered handler."));
    }
}
