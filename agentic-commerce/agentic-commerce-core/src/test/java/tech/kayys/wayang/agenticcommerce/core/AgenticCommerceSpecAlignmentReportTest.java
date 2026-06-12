package tech.kayys.wayang.agenticcommerce.core;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AgenticCommerceSpecAlignmentReportTest {

    @Test
    void checkoutCatalogIsAlignedWithPinnedSpecSnapshot() {
        AgenticCommerceSpecAlignmentReport report = AgenticCommerceSpecAlignmentReport.checkout();
        Map<String, Object> values = report.toMap();

        assertThat(report.aligned()).isTrue();
        assertThat(report.requirementCount()).isEqualTo(13);
        assertThat(report.alignedCount()).isEqualTo(13);
        assertThat(report.gapCount()).isZero();
        assertThat(report.gapIds()).isEmpty();
        assertThat(report.requirementIds())
                .contains(
                        "protocol.metadata",
                        "http.required_headers",
                        "route." + AgenticCommerceProtocol.OPERATION_CREATE_CHECKOUT_SESSION,
                        "route." + AgenticCommerceProtocol.OPERATION_RETRIEVE_CHECKOUT_SESSION,
                        "route." + AgenticCommerceProtocol.OPERATION_UPDATE_CHECKOUT_SESSION,
                        "route." + AgenticCommerceProtocol.OPERATION_COMPLETE_CHECKOUT_SESSION,
                        "route." + AgenticCommerceProtocol.OPERATION_CANCEL_CHECKOUT_SESSION,
                        "payload.checkout_session.create_request",
                        "payload.checkout_session.update_request",
                        "payload.checkout_session.complete_request",
                        "payload.checkout_session.cancel_request",
                        "payload.checkout_session.response",
                        "payload.error");
        assertThat(values)
                .containsEntry("protocol", "agentic-commerce")
                .containsEntry("specVersion", AgenticCommerceProtocol.SPEC_VERSION)
                .containsEntry("aligned", true)
                .containsEntry("requirementCount", 13)
                .containsEntry("gapCount", 0);
        assertThat(map(values.get("standard")))
                .containsEntry("standardId", "agentic-commerce")
                .containsEntry("name", AgenticCommerceSpecAlignmentReport.STANDARD_NAME)
                .containsEntry("version", AgenticCommerceProtocol.SPEC_VERSION)
                .containsEntry("binding", AgenticCommerceSpecAlignmentReport.BINDING_HTTP_JSON)
                .containsEntry("specUrl", AgenticCommerceProtocol.SPEC_HOME)
                .containsEntry("githubUrl", AgenticCommerceProtocol.SPEC_GITHUB);
        assertThat(map(values.get("routeCatalog")))
                .containsEntry("routeCount", 5);
        assertThat(routeRequirement(report, AgenticCommerceProtocol.OPERATION_CREATE_CHECKOUT_SESSION).aligned())
                .isTrue();
        assertThat(strings(map(payloadRequirement(report, "payload.checkout_session.create_request")
                        .actual()).get("fields")))
                .contains("line_items", "fulfillment_details", "quote_id")
                .doesNotContain("lineItems", "fulfillmentDetails", "quoteId");
        assertThat(strings(map(payloadRequirement(report, "payload.checkout_session.complete_request")
                        .actual()).get("fields")))
                .contains("payment_data", "authentication_result", "risk_signals");
    }

    @Test
    void missingCheckoutRoutesAreReportedAsGaps() {
        AgenticCommerceRouteCatalog catalog = new AgenticCommerceRouteCatalog(List.of(
                AgenticCommerceHttpRoute.createCheckoutSession()));

        AgenticCommerceSpecAlignmentReport report = AgenticCommerceSpecAlignmentReport.from(catalog);
        AgenticCommerceSpecAlignmentRequirement retrieve =
                routeRequirement(report, AgenticCommerceProtocol.OPERATION_RETRIEVE_CHECKOUT_SESSION);

        assertThat(report.aligned()).isFalse();
        assertThat(report.requirementCount()).isEqualTo(13);
        assertThat(report.alignedCount()).isEqualTo(9);
        assertThat(report.gapCount()).isEqualTo(4);
        assertThat(report.gapIds())
                .containsExactly(
                        "route." + AgenticCommerceProtocol.OPERATION_RETRIEVE_CHECKOUT_SESSION,
                        "route." + AgenticCommerceProtocol.OPERATION_UPDATE_CHECKOUT_SESSION,
                        "route." + AgenticCommerceProtocol.OPERATION_COMPLETE_CHECKOUT_SESSION,
                        "route." + AgenticCommerceProtocol.OPERATION_CANCEL_CHECKOUT_SESSION);
        assertThat(retrieve.aligned()).isFalse();
        assertThat(map(retrieve.actual()))
                .containsEntry("present", false);
        assertThat(retrieve.message()).contains("missing");
    }

    private static AgenticCommerceSpecAlignmentRequirement routeRequirement(
            AgenticCommerceSpecAlignmentReport report,
            String operation) {
        return report.requirements().stream()
                .filter(requirement -> requirement.id().equals("route." + operation))
                .findFirst()
                .orElseThrow();
    }

    private static AgenticCommerceSpecAlignmentRequirement payloadRequirement(
            AgenticCommerceSpecAlignmentReport report,
            String id) {
        return report.requirements().stream()
                .filter(requirement -> requirement.id().equals(id))
                .findFirst()
                .orElseThrow();
    }

    private static Map<String, Object> map(Object value) {
        assertThat(value).isInstanceOf(Map.class);
        return AgenticCommerceMaps.copy((Map<?, ?>) value);
    }

    @SuppressWarnings("unchecked")
    private static List<String> strings(Object value) {
        assertThat(value).isInstanceOf(List.class);
        return (List<String>) value;
    }
}
