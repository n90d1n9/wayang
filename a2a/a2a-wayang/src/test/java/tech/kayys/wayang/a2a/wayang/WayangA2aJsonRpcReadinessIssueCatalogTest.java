package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aJsonRpcReadinessIssueCatalogTest {

    @Test
    void exposesSummaryProbeOrder() {
        assertThat(WayangA2aJsonRpcReadinessIssueCatalog.summaryProbeNames())
                .containsExactly(
                        "readiness",
                        "bindingReport",
                        "diagnosticHandlers",
                        "methodDispatch",
                        "routeCatalog",
                        "smoke");
    }

    @Test
    void buildsSpecAlignmentCategoryProbeNames() {
        assertThat(WayangA2aJsonRpcReadinessIssueCatalog.specAlignmentCategoryProbe(" route "))
                .isEqualTo("specAlignment:route");
        assertThat(WayangA2aJsonRpcReadinessIssueCatalog.specAlignmentCategoryProbe(" "))
                .isEqualTo("specAlignment");
    }
}
