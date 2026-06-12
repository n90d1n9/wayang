package tech.kayys.wayang.a2ui.wayang.surface;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SurfaceIdsTest {

    @Test
    void normalizesSafeSurfaceIdSegments() {
        assertThat(SurfaceIds.safe(" RUN 123 ")).isEqualTo("run-123");
        assertThat(SurfaceIds.safe("wayang.run.cancel")).isEqualTo("wayang-run-cancel");
        assertThat(SurfaceIds.safe(" ")).isEqualTo("unknown");
        assertThat(SurfaceIds.safe(null)).isEqualTo("unknown");
        assertThat(SurfaceIds.safe(" ", "action")).isEqualTo("action");
        assertThat(SurfaceIds.safe(" ", " ")).isEqualTo("unknown");
    }

    @Test
    void buildsStandardSurfaceIds() {
        assertThat(SurfaceIds.runStatus("RUN 123")).isEqualTo("wayang-run-run-123");
        assertThat(SurfaceIds.runEvents("run-1")).isEqualTo("wayang-run-events-run-1");
        assertThat(SurfaceIds.runHistory()).isEqualTo("wayang-run-history");
        assertThat(SurfaceIds.runHistoryRow("run-1")).isEqualTo("wayang-run-history-run-run-1");
    }

    @Test
    void buildsActionResultSurfaceIds() {
        assertThat(SurfaceIds.actionResult(7, "custom.action"))
                .isEqualTo("wayang-action-result-7-custom-action");
        assertThat(SurfaceIds.actionResult(1, " "))
                .isEqualTo("wayang-action-result-1-action");
    }
}
