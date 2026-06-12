package tech.kayys.wayang.a2ui.wayang;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2uiActionsTest {

    @Test
    void exposesDefaultActionGroups() {
        assertThat(WayangA2uiActions.inspectOnlyActionNames())
                .containsExactly(WayangA2uiActions.RUN_INSPECT);
        assertThat(WayangA2uiActions.readOnlyActionNames())
                .containsExactlyInAnyOrder(
                        WayangA2uiActions.RUN_INSPECT,
                        WayangA2uiActions.RUN_HISTORY,
                        WayangA2uiActions.RUN_EVENTS);
        assertThat(WayangA2uiActions.runLifecycleActionNames())
                .containsExactlyInAnyOrder(
                        WayangA2uiActions.RUN_INSPECT,
                        WayangA2uiActions.RUN_HISTORY,
                        WayangA2uiActions.RUN_EVENTS,
                        WayangA2uiActions.RUN_WAIT,
                        WayangA2uiActions.RUN_CANCEL);
        assertThat(WayangA2uiActions.runLifecycleActionOrder())
                .containsExactly(
                        WayangA2uiActions.RUN_INSPECT,
                        WayangA2uiActions.RUN_HISTORY,
                        WayangA2uiActions.RUN_EVENTS,
                        WayangA2uiActions.RUN_WAIT,
                        WayangA2uiActions.RUN_CANCEL);
    }

    @Test
    void classifiesActionsThatRequireRunId() {
        assertThat(WayangA2uiActions.requiresRunId(WayangA2uiActions.RUN_INSPECT)).isTrue();
        assertThat(WayangA2uiActions.requiresRunId(WayangA2uiActions.RUN_EVENTS)).isTrue();
        assertThat(WayangA2uiActions.requiresRunId(WayangA2uiActions.RUN_WAIT)).isTrue();
        assertThat(WayangA2uiActions.requiresRunId(WayangA2uiActions.RUN_CANCEL)).isTrue();
        assertThat(WayangA2uiActions.requiresRunId(WayangA2uiActions.RUN_HISTORY)).isFalse();
        assertThat(WayangA2uiActions.requiresRunId("custom.action")).isFalse();
        assertThat(WayangA2uiActions.requiresRunId(null)).isFalse();
    }
}
