package tech.kayys.wayang.a2ui.wayang.session;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiActions;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiSessionConfig;

import static org.assertj.core.api.Assertions.assertThat;

class SessionProfilesTest {

    @Test
    void exposesStableModeNamesAndAliases() {
        assertThat(SessionProfiles.modeNames())
                .containsExactly(
                        WayangA2uiSessionConfig.MODE_INSPECT_ONLY,
                        WayangA2uiSessionConfig.MODE_READ_ONLY,
                        WayangA2uiSessionConfig.MODE_RUN_LIFECYCLE,
                        WayangA2uiSessionConfig.MODE_CUSTOM);
        assertThat(SessionProfiles.normalizeMode(" readonly "))
                .isEqualTo(WayangA2uiSessionConfig.MODE_READ_ONLY);
        assertThat(SessionProfiles.normalizeMode("run_lifecycle"))
                .isEqualTo(WayangA2uiSessionConfig.MODE_RUN_LIFECYCLE);
        assertThat(SessionProfiles.normalizeMode("unknown"))
                .isEqualTo(WayangA2uiSessionConfig.MODE_INSPECT_ONLY);
        assertThat(SessionProfiles.normalizeMode(null))
                .isEqualTo(WayangA2uiSessionConfig.MODE_INSPECT_ONLY);
    }

    @Test
    void resolvesDefaultPoliciesByMode() {
        assertThat(SessionProfiles.actionPolicy("inspect-only").allowedActions())
                .containsExactly(WayangA2uiActions.RUN_INSPECT);
        assertThat(SessionProfiles.actionPolicy("read").allowedActions())
                .containsExactlyInAnyOrder(
                        WayangA2uiActions.RUN_INSPECT,
                        WayangA2uiActions.RUN_HISTORY,
                        WayangA2uiActions.RUN_EVENTS);
        assertThat(SessionProfiles.actionPolicy("lifecycle").allowedActions())
                .containsExactlyInAnyOrderElementsOf(WayangA2uiActions.runLifecycleActionNames());
        assertThat(SessionProfiles.actionPolicy("custom").allowedActions()).isEmpty();
    }

    @Test
    void buildsConfigsFromProfiles() {
        assertThat(SessionProfiles.config("read-only"))
                .isEqualTo(WayangA2uiSessionConfig.readOnly());
        assertThat(SessionProfiles.config("run", false))
                .matches(config -> !config.enabled())
                .matches(config -> config.actionPolicy().allowsAction(WayangA2uiActions.RUN_CANCEL));
    }
}
