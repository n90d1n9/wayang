package tech.kayys.wayang.a2ui.wayang.session;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiActions;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiSessionConfig;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class SessionConfigSourcesTest {

    @TempDir
    Path tempDir;

    @Test
    void loadsFileConfigWhenPresent() throws Exception {
        Path configPath = tempDir.resolve("a2ui-session.json");
        Files.writeString(configPath, """
                {
                  "mode": "read-only",
                  "enabled": true
                }
                """, StandardCharsets.UTF_8);

        WayangA2uiSessionConfig config = SessionConfigSources.file(configPath).load().orElseThrow();

        assertThat(config.enabled()).isTrue();
        assertThat(config.actionPolicy().allowedActions())
                .containsExactlyInAnyOrder(
                        WayangA2uiActions.RUN_INSPECT,
                        WayangA2uiActions.RUN_HISTORY,
                        WayangA2uiActions.RUN_EVENTS);
    }

    @Test
    void fallsBackAcrossMissingSourcesToAdapterJson() {
        SessionConfigSource source = SessionConfigSources.firstAvailable(
                SessionConfigSources.file(tempDir.resolve("missing-session.json")),
                SessionConfigSources.json("database:tenant-a", () -> Optional.of("""
                        {
                          "mode": "run-lifecycle",
                          "allowedRunIds": ["run-a"]
                        }
                        """)));

        WayangA2uiSessionConfig config = source.load().orElseThrow();

        assertThat(source.description()).contains("file:", "database:tenant-a");
        assertThat(config.actionPolicy().allowedActions())
                .contains(WayangA2uiActions.RUN_WAIT, WayangA2uiActions.RUN_CANCEL);
        assertThat(config.actionPolicy().allowedRunIds()).containsExactly("run-a");
    }

    @Test
    void loadsClasspathConfig() {
        WayangA2uiSessionConfig config = SessionConfigSources
                .classpath("a2ui/session-config-readonly.json")
                .load()
                .orElseThrow();

        assertThat(config.actionPolicy().allowedActions())
                .containsExactlyInAnyOrder(
                        WayangA2uiActions.RUN_INSPECT,
                        WayangA2uiActions.RUN_HISTORY,
                        WayangA2uiActions.RUN_EVENTS);
    }

    @Test
    void defaultsWhenNoSourceProvidesJson() {
        assertThat(SessionConfigSources.loadFirstOrDefault(
                SessionConfigSources.file(tempDir.resolve("missing-session.json"))))
                .isEqualTo(WayangA2uiSessionConfig.defaultConfig());
        assertThat(WayangA2uiSessionConfig.fromSource(null))
                .isEqualTo(WayangA2uiSessionConfig.defaultConfig());
    }

    @Test
    void reportsLoadedMissingAndFailedSources() {
        SessionConfigLoadResult loaded = SessionConfigSources
                .inlineJson("inline", """
                        {
                          "mode": "read-only"
                        }
                        """)
                .loadResult();
        SessionConfigLoadResult missing = SessionConfigSources
                .file(tempDir.resolve("missing-session.json"))
                .loadResult();
        SessionConfigLoadResult failed = SessionConfigSources
                .inlineJson("broken", "{")
                .loadResult();

        assertThat(loaded.loaded()).isTrue();
        assertThat(loaded.config()).isEqualTo(WayangA2uiSessionConfig.readOnly());
        assertThat(missing.missing()).isTrue();
        assertThat(missing.config()).isEqualTo(WayangA2uiSessionConfig.defaultConfig());
        assertThat(failed.failed()).isTrue();
        assertThat(failed.message()).contains("Unable to load A2UI session config");
        assertThat(failed.toMap())
                .containsEntry("status", SessionConfigLoadStatus.FAILED.name())
                .containsEntry("loaded", false);
    }

    @Test
    void reportsSelectedSourceWhenLoadingFirstAvailableResult() {
        SessionConfigLoadResult result = SessionConfigSources.loadFirstResult(
                SessionConfigSources.file(tempDir.resolve("missing-session.json")),
                SessionConfigSources.json("database:tenant-a", () -> Optional.of("""
                        {
                          "mode": "run-lifecycle"
                        }
                        """)));

        assertThat(result.loaded()).isTrue();
        assertThat(result.sourceDescription()).isEqualTo("database:tenant-a");
        assertThat(result.traced()).isTrue();
        assertThat(result.attempts())
                .extracting(SessionConfigLoadAttempt::sourceDescription)
                .containsExactly(
                        "file:" + tempDir.resolve("missing-session.json").toAbsolutePath().normalize(),
                        "database:tenant-a");
        assertThat(result.attempts())
                .extracting(SessionConfigLoadAttempt::status)
                .containsExactly(SessionConfigLoadStatus.MISSING, SessionConfigLoadStatus.LOADED);
        assertThat(result.config().actionPolicy().allowedActions())
                .contains(WayangA2uiActions.RUN_WAIT, WayangA2uiActions.RUN_CANCEL);
    }

    @Test
    void firstAvailableSourceLoadResultIncludesFallbackAttempts() {
        SessionConfigSource source = SessionConfigSources.firstAvailable(
                SessionConfigSources.file(tempDir.resolve("missing-session.json")),
                SessionConfigSources.inlineJson("inline:readonly", """
                        {
                          "mode": "read-only"
                        }
                        """));

        SessionConfigLoadResult result = source.loadResult();

        assertThat(result.loaded()).isTrue();
        assertThat(result.sourceDescription()).isEqualTo("inline:readonly");
        assertThat(result.toMap()).containsKey("attempts");
        assertThat(result.attempts())
                .extracting(SessionConfigLoadAttempt::status)
                .containsExactly(SessionConfigLoadStatus.MISSING, SessionConfigLoadStatus.LOADED);
    }

    @Test
    void fallbackTraceStopsOnFailedSourceToPreserveExistingFailureSemantics() {
        SessionConfigLoadResult result = SessionConfigSources.loadFirstResult(
                SessionConfigSources.file(tempDir.resolve("missing-session.json")),
                SessionConfigSources.inlineJson("broken", "{"),
                SessionConfigSources.inlineJson("unreached", """
                        {
                          "mode": "read-only"
                        }
                        """));

        assertThat(result.failed()).isTrue();
        assertThat(result.sourceDescription()).isEqualTo("broken");
        assertThat(result.attempts())
                .extracting(SessionConfigLoadAttempt::sourceDescription)
                .containsExactly(
                        "file:" + tempDir.resolve("missing-session.json").toAbsolutePath().normalize(),
                        "broken");
        assertThat(result.attempts())
                .extracting(SessionConfigLoadAttempt::status)
                .containsExactly(SessionConfigLoadStatus.MISSING, SessionConfigLoadStatus.FAILED);
    }

    @Test
    void missingFallbackTraceReportsEveryAttempt() {
        SessionConfigLoadResult result = SessionConfigSources.loadFirstResult(
                SessionConfigSources.file(tempDir.resolve("missing-a.json")),
                SessionConfigSources.file(tempDir.resolve("missing-b.json")));

        assertThat(result.missing()).isTrue();
        assertThat(result.attempts())
                .extracting(SessionConfigLoadAttempt::status)
                .containsExactly(SessionConfigLoadStatus.MISSING, SessionConfigLoadStatus.MISSING);
    }
}
