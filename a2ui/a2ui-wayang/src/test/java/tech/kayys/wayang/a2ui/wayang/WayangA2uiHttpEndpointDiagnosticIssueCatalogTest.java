package tech.kayys.wayang.a2ui.wayang;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2uiHttpEndpointDiagnosticIssueCatalogTest {

    @Test
    void resolvesIssueCategoriesByEndpointState() {
        assertThat(WayangA2uiHttpEndpointDiagnosticIssueCatalog.category(false, false, false))
                .isEqualTo(WayangA2uiHttpEndpointDiagnosticIssueCatalog.CATEGORY_UNKNOWN_PATH);
        assertThat(WayangA2uiHttpEndpointDiagnosticIssueCatalog.category(true, false, false))
                .isEqualTo(WayangA2uiHttpEndpointDiagnosticIssueCatalog.CATEGORY_ROUTE_MISMATCH);
        assertThat(WayangA2uiHttpEndpointDiagnosticIssueCatalog.category(true, true, true))
                .isEqualTo(WayangA2uiHttpEndpointDiagnosticIssueCatalog.CATEGORY_TRANSPORT_ERROR);
        assertThat(WayangA2uiHttpEndpointDiagnosticIssueCatalog.category(true, true, false))
                .isEqualTo(WayangA2uiHttpEndpointDiagnosticIssueCatalog.CATEGORY_HTTP_STATUS);
    }

    @Test
    void resolvesFallbackErrorCodesByEndpointState() {
        assertThat(WayangA2uiHttpEndpointDiagnosticIssueCatalog.fallbackErrorCode(false, false, 404))
                .isEqualTo(WayangA2uiHttpEndpointDiagnosticIssueCatalog.ERROR_UNKNOWN_ENDPOINT_PATH);
        assertThat(WayangA2uiHttpEndpointDiagnosticIssueCatalog.fallbackErrorCode(true, false, 405))
                .isEqualTo(WayangA2uiHttpEndpointDiagnosticIssueCatalog.ERROR_ENDPOINT_ROUTE_MISMATCH);
        assertThat(WayangA2uiHttpEndpointDiagnosticIssueCatalog.fallbackErrorCode(true, true, 503))
                .isEqualTo(WayangA2uiHttpEndpointDiagnosticIssueCatalog.ERROR_HTTP_STATUS_PREFIX + "503");
        assertThat(WayangA2uiHttpEndpointDiagnosticIssueCatalog.httpStatusErrorCode(-1))
                .isEqualTo(WayangA2uiHttpEndpointDiagnosticIssueCatalog.ERROR_HTTP_STATUS_PREFIX + "0");
    }

    @Test
    void normalizesBlankCategoryAndErrorCode() {
        assertThat(WayangA2uiHttpEndpointDiagnosticIssueCatalog.normalizeCategory(" "))
                .isEqualTo(WayangA2uiHttpEndpointDiagnosticIssueCatalog.CATEGORY_HTTP_STATUS);
        assertThat(WayangA2uiHttpEndpointDiagnosticIssueCatalog.normalizeCategory(" custom "))
                .isEqualTo("custom");
        assertThat(WayangA2uiHttpEndpointDiagnosticIssueCatalog.normalizeErrorCode(null))
                .isEqualTo(WayangA2uiHttpEndpointDiagnosticIssueCatalog.ERROR_ENDPOINT_DIAGNOSTIC_ISSUE);
        assertThat(WayangA2uiHttpEndpointDiagnosticIssueCatalog.normalizeErrorCode(" custom_error "))
                .isEqualTo("custom_error");
    }
}
