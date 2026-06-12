package tech.kayys.wayang.a2ui.wayang.session;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiSessionConfig;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SessionConfigLoadResultDecoderTest {

    @Test
    void decodesLoadResultsAndAttemptsFromMapAndJson() {
        SessionConfigLoadResult result = SessionConfigLoadResult
                .loaded("database:tenant-a", WayangA2uiSessionConfig.readOnly())
                .withAttempts(List.of(
                        SessionConfigLoadAttempt.missing("file:/etc/wayang/a2ui-session.json"),
                        SessionConfigLoadAttempt.loaded("database:tenant-a")));

        assertThat(SessionConfigLoadResult.fromMap(result.toMap())).isEqualTo(result);
        assertThat(SessionConfigLoadResult.fromJson(result.toJson())).isEqualTo(result);
        assertThat(SessionConfigLoadResultDecoder.fromMap(result.toMap())).isEqualTo(result);
        assertThat(SessionConfigLoadAttempt.fromMap(result.attempts().get(0).toMap()))
                .isEqualTo(result.attempts().get(0));
    }

    @Test
    void toleratesLowercaseStatusAndMissingOptionalFields() {
        SessionConfigLoadResult result = SessionConfigLoadResult.fromMap(Map.of(
                "sourceDescription", "database:tenant-a",
                "status", "loaded",
                "config", Map.of("mode", "read-only")));

        assertThat(result.status()).isEqualTo(SessionConfigLoadStatus.LOADED);
        assertThat(result.message()).isEqualTo("A2UI session config loaded.");
        assertThat(result.config()).isEqualTo(WayangA2uiSessionConfig.readOnly());
    }

    @Test
    void rejectsBlankJsonLikeOtherSessionDecoders() {
        assertThatThrownBy(() -> SessionConfigLoadResult.fromJson(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("load result JSON must not be blank");
    }
}
