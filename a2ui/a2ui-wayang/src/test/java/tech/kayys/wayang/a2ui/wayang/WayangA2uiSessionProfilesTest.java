package tech.kayys.wayang.a2ui.wayang;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2uiSessionProfilesTest {

    @Test
    void exposesStableModeNamesAndAliases() {
        assertThat(WayangA2uiSessionProfiles.modeNames())
                .containsExactly(
                        WayangA2uiSessionConfig.MODE_INSPECT_ONLY,
                        WayangA2uiSessionConfig.MODE_READ_ONLY,
                        WayangA2uiSessionConfig.MODE_RUN_LIFECYCLE,
                        WayangA2uiSessionConfig.MODE_CUSTOM);
        assertThat(WayangA2uiSessionProfiles.normalizeMode(" readonly "))
                .isEqualTo(WayangA2uiSessionConfig.MODE_READ_ONLY);
        assertThat(WayangA2uiSessionProfiles.normalizeMode("run_lifecycle"))
                .isEqualTo(WayangA2uiSessionConfig.MODE_RUN_LIFECYCLE);
        assertThat(WayangA2uiSessionProfiles.normalizeMode("unknown"))
                .isEqualTo(WayangA2uiSessionConfig.MODE_INSPECT_ONLY);
        assertThat(WayangA2uiSessionProfiles.normalizeMode(null))
                .isEqualTo(WayangA2uiSessionConfig.MODE_INSPECT_ONLY);
    }

    @Test
    void resolvesDefaultPoliciesByMode() {
        assertThat(WayangA2uiSessionProfiles.actionPolicy("inspect-only").allowedActions())
                .containsExactly(WayangA2uiActions.RUN_INSPECT);
        assertThat(WayangA2uiSessionProfiles.actionPolicy("read").allowedActions())
                .containsExactlyInAnyOrder(
                        WayangA2uiActions.RUN_INSPECT,
                        WayangA2uiActions.RUN_HISTORY,
                        WayangA2uiActions.RUN_EVENTS);
        assertThat(WayangA2uiSessionProfiles.actionPolicy("lifecycle").allowedActions())
                .containsExactlyInAnyOrderElementsOf(WayangA2uiActions.runLifecycleActionNames());
        assertThat(WayangA2uiSessionProfiles.actionPolicy("custom").allowedActions()).isEmpty();
    }

    @Test
    void buildsConfigsFromProfiles() {
        assertThat(WayangA2uiSessionProfiles.config("read-only"))
                .isEqualTo(WayangA2uiSessionConfig.readOnly());
        assertThat(WayangA2uiSessionProfiles.config("run", false))
                .matches(config -> !config.enabled())
                .matches(config -> config.actionPolicy().allowsAction(WayangA2uiActions.RUN_CANCEL));
    }
}
