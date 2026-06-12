package tech.kayys.wayang.agenticcommerce.wayang;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgenticCommerceHttpAdapterConfigTest {

    @Test
    void defaultsExposeCheckoutAndSmokePaths() {
        AgenticCommerceHttpAdapterConfig config = AgenticCommerceHttpAdapterConfig.defaults();

        assertThat(config.checkoutBasePath()).isEqualTo(AgenticCommerceHttpAdapter.DEFAULT_CHECKOUT_BASE_PATH);
        assertThat(config.smokePath()).isEqualTo(AgenticCommerceHttpAdapter.DEFAULT_SMOKE_PATH);
        assertThat(config.smokeEnabled()).isTrue();
        assertThat(config.bindingReportPath()).isEqualTo(AgenticCommerceHttpAdapter.DEFAULT_BINDING_REPORT_PATH);
        assertThat(config.bindingReportEnabled()).isTrue();
    }

    @Test
    void bindsFromMapAndNormalizesPaths() {
        AgenticCommerceHttpAdapterConfig config = AgenticCommerceHttpAdapterConfig.fromMap(Map.of(
                "basePath",
                "commerce/acp?debug=true",
                "smokePath",
                "internal/acp/smoke",
                "smokeEnabled",
                "false",
                "bindingPath",
                "internal/acp/binding",
                "bindingReportEnabled",
                "false"));

        assertThat(config.checkoutBasePath()).isEqualTo("/commerce/acp");
        assertThat(config.smokePath()).isEqualTo("/internal/acp/smoke");
        assertThat(config.smokeEnabled()).isFalse();
        assertThat(config.bindingReportPath()).isEqualTo("/internal/acp/binding");
        assertThat(config.bindingReportEnabled()).isFalse();
        assertThat(config.toMap())
                .containsEntry("checkoutBasePath", "/commerce/acp")
                .containsEntry("smokePath", "/internal/acp/smoke")
                .containsEntry("smokeEnabled", false)
                .containsEntry("bindingReportPath", "/internal/acp/binding")
                .containsEntry("bindingReportEnabled", false);
    }

    @Test
    void rejectsCollidingEnabledPaths() {
        assertThatThrownBy(() -> AgenticCommerceHttpAdapterConfig.builder()
                .checkoutBasePath("/commerce")
                .smokePath("/commerce")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must differ");

        assertThatThrownBy(() -> AgenticCommerceHttpAdapterConfig.builder()
                .smokePath("/diagnostics")
                .bindingReportPath("/diagnostics")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("binding report path");

        AgenticCommerceHttpAdapterConfig disabledReport = AgenticCommerceHttpAdapterConfig.builder()
                .checkoutBasePath("/diagnostics")
                .bindingReportPath("/diagnostics")
                .bindingReportEnabled(false)
                .build();
        assertThat(disabledReport.toMap())
                .containsEntry("checkoutBasePath", "/diagnostics")
                .containsEntry("bindingReportPath", "/diagnostics")
                .containsEntry("bindingReportEnabled", false);
    }
}
