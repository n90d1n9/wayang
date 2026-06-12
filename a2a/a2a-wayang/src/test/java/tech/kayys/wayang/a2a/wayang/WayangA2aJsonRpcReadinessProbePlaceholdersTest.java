package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aJsonRpcReadinessProbePlaceholdersTest {

    @Test
    void buildsDisabledSmokeProbePlaceholder() {
        WayangA2aJsonRpcSmokeProbeResult probe =
                WayangA2aJsonRpcReadinessProbePlaceholders.disabledSmokeProbe();

        assertThat(probe.statusCode()).isZero();
        assertThat(probe.httpSuccessful()).isFalse();
        assertThat(probe.routeOperation()).isEmpty();
        assertThat(probe.protocolVersion()).isEmpty();
        assertThat(probe.contentType()).isEmpty();
        assertThat(probe.summary().issueCount()).isZero();
        assertThat(probe.passed()).isFalse();
    }

    @Test
    void buildsDisabledRouteCatalogProbePlaceholder() {
        WayangA2aJsonRpcRouteCatalogProbeResult probe =
                WayangA2aJsonRpcReadinessProbePlaceholders.disabledRouteCatalogProbe();

        assertThat(probe.statusCode()).isZero();
        assertThat(probe.httpSuccessful()).isFalse();
        assertThat(probe.routeOperation()).isEmpty();
        assertThat(probe.protocolVersion()).isEmpty();
        assertThat(probe.contentType()).isEmpty();
        assertThat(probe.routeCount()).isZero();
        assertThat(probe.issueCount()).isZero();
        assertThat(probe.passed()).isFalse();
    }
}
