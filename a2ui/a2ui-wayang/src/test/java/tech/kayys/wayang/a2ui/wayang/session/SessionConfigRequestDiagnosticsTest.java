package tech.kayys.wayang.a2ui.wayang.session;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiSessionConfig;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SessionConfigRequestDiagnosticsTest {

    @Test
    void roundTripsRequestDiagnosticsWithNestedSourceDiagnostics() {
        SessionConfigSourceDiagnostics sourceDiagnostics = SessionConfigSourceDiagnostics.load(
                SessionConfigSourceSpec.inlineJson("inline-test", """
                        {
                          "mode": "read-only"
                        }
                        """),
                SessionConfigSourceRegistry.standard());
        SessionConfigRequestDiagnostics diagnostics = new SessionConfigRequestDiagnostics(
                SessionConfigRequestDiagnostics.DIAGNOSTICS_ID,
                "a2ui",
                "sessionConfig",
                "sessionConfigSource",
                true,
                false,
                true,
                SessionConfigRequestDiagnostics.ACTIVE_SOURCE,
                sourceDiagnostics.loadResult(),
                sourceDiagnostics);

        assertThat(SessionConfigRequestDiagnostics.fromMap(diagnostics.toMap())).isEqualTo(diagnostics);
        assertThat(SessionConfigRequestDiagnostics.fromJson(diagnostics.toJson())).isEqualTo(diagnostics);
        assertThat(SessionConfigRequestDiagnosticsDecoder.fromMap(diagnostics.toMap())).isEqualTo(diagnostics);

        SessionConfigRequestDiagnosticsSummary summary = diagnostics.summary();
        assertThat(summary.passed()).isTrue();
        assertThat(summary.successfulExit()).isTrue();
        assertThat(summary.activeInput()).isEqualTo(SessionConfigRequestDiagnostics.ACTIVE_SOURCE);
        assertThat(summary.sourceDiagnosticsPresent()).isTrue();
        assertThat(summary.sourceType()).isEqualTo("inline");
        assertThat(summary.sourceValid()).isTrue();
        assertThat(summary.sourceAllowed()).isTrue();
        assertThat(SessionConfigRequestDiagnosticsSummary.fromJson(summary.toJson())).isEqualTo(summary);
        assertThat(SessionConfigRequestDiagnosticsSummaryDecoder.fromMap(summary.toMap())).isEqualTo(summary);
    }

    @Test
    void extractsSummaryFromFullDiagnosticsPayloads() {
        SessionConfigRequestDiagnostics diagnostics = new SessionConfigRequestDiagnostics(
                SessionConfigRequestDiagnostics.DIAGNOSTICS_ID,
                "a2ui",
                "sessionConfig",
                "sessionConfigSource",
                true,
                true,
                false,
                SessionConfigRequestDiagnostics.ACTIVE_DIRECT_CONFIG,
                SessionConfigLoadResult.loaded("a2ui.sessionConfig", WayangA2uiSessionConfig.readOnly()),
                null);
        SessionConfigRequestDiagnosticsSummary summary = diagnostics.summary();

        assertThat(SessionConfigRequestDiagnosticsSummary.fromDiagnosticsMap(diagnostics.toMap()))
                .isEqualTo(summary);
        assertThat(SessionConfigRequestDiagnosticsSummary.fromDiagnosticsJson(diagnostics.toJson()))
                .isEqualTo(summary);
        assertThat(SessionConfigRequestDiagnosticsSummaryDecoder.fromDiagnosticsMap(Map.of("summary", summary.toMap())))
                .isEqualTo(summary);

        Map<String, Object> legacyDiagnostics = new LinkedHashMap<>(diagnostics.toMap());
        legacyDiagnostics.remove("summary");
        assertThat(SessionConfigRequestDiagnosticsSummary.fromDiagnosticsMap(legacyDiagnostics))
                .isEqualTo(summary);
    }

    @Test
    void derivesConvenienceFlagsFromLoadResultAndActiveInput() {
        SessionConfigRequestDiagnostics diagnostics = new SessionConfigRequestDiagnostics(
                SessionConfigRequestDiagnostics.DIAGNOSTICS_ID,
                "a2ui",
                "sessionConfig",
                "sessionConfigSource",
                true,
                true,
                false,
                SessionConfigRequestDiagnostics.ACTIVE_DIRECT_CONFIG,
                SessionConfigLoadResult.loaded("a2ui.sessionConfig", WayangA2uiSessionConfig.readOnly()),
                null);

        assertThat(diagnostics.directConfigActive()).isTrue();
        assertThat(diagnostics.sourceActive()).isFalse();
        assertThat(diagnostics.loaded()).isTrue();
        assertThat(diagnostics.sourceDiagnosticsPresent()).isFalse();
        assertThat(diagnostics.summary().activeInput())
                .isEqualTo(SessionConfigRequestDiagnostics.ACTIVE_DIRECT_CONFIG);
    }

    @Test
    void rejectsBlankJsonLikeOtherSessionDecoders() {
        assertThatThrownBy(() -> SessionConfigRequestDiagnostics.fromJson(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("request diagnostics JSON must not be blank");
        assertThatThrownBy(() -> SessionConfigRequestDiagnosticsSummary.fromJson(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("request diagnostics summary JSON must not be blank");
        assertThatThrownBy(() -> SessionConfigRequestDiagnosticsSummary.fromDiagnosticsJson(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("request diagnostics JSON must not be blank");
    }
}
