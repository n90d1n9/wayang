package tech.kayys.wayang.agenticcommerce.core;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AgenticCommerceCheckoutHttpSmokeProbeResultTest {

    @Test
    void acceptsPassingSmokeSummaryHttpResponses() {
        AgenticCommerceCheckoutHttpSmokeSummary summary = AgenticCommerceCheckoutHttpSmoke.result().summary();
        AgenticCommerceHttpResponse response = AgenticCommerceHttpResponse
                .json(200, AgenticCommerceJson.write(summary.toMap()))
                .withHeaders(Map.of(AgenticCommerceProtocol.HEADER_REQUEST_ID, "probe-1"))
                .withAttributes(Map.of("operation", AgenticCommerceProtocol.OPERATION_CHECKOUT_SMOKE));

        AgenticCommerceCheckoutHttpSmokeProbeResult result = AgenticCommerceCheckoutHttpSmokeProbeResult.fromResponse(response);

        assertThat(result.valid()).isTrue();
        assertThat(result.passed()).isTrue();
        assertThat(result.failed()).isFalse();
        assertThat(result.exitCode()).isZero();
        assertThat(result.operation()).isEqualTo(AgenticCommerceProtocol.OPERATION_CHECKOUT_SMOKE);
        assertThat(result.issueCount()).isZero();
        assertThat(result.toMap())
                .containsEntry("statusCode", 200)
                .containsEntry("requestId", "probe-1")
                .containsEntry("summaryPassed", true)
                .containsEntry("operation", AgenticCommerceProtocol.OPERATION_CHECKOUT_SMOKE);
    }

    @Test
    void createsProbeResultsFromSummaries() {
        AgenticCommerceCheckoutHttpSmokeProbeResult result = AgenticCommerceCheckoutHttpSmokeProbeResult.fromSummary(
                AgenticCommerceCheckoutHttpSmoke.result().summary());

        assertThat(result.passed()).isTrue();
        assertThat(result.response().statusCode()).isEqualTo(200);
        assertThat(result.summary().exchangeCount()).isEqualTo(5);
    }

    @Test
    void reportsNonSuccessfulHttpStatusEvenWhenSummaryPassed() {
        AgenticCommerceCheckoutHttpSmokeSummary summary = AgenticCommerceCheckoutHttpSmoke.result().summary();
        AgenticCommerceCheckoutHttpSmokeProbeResult result = AgenticCommerceCheckoutHttpSmokeProbeResult.fromResponse(
                AgenticCommerceHttpResponse.json(503, AgenticCommerceJson.write(summary.toMap())));

        assertThat(result.valid()).isFalse();
        assertThat(result.passed()).isFalse();
        assertThat(result.exitCode()).isEqualTo(1);
        assertThat(result.issues())
                .extracting(AgenticCommerceCheckoutHttpIssueSummary::code)
                .containsExactly("probe_http_status");
    }

    @Test
    void reportsInvalidJsonProbeBodies() {
        AgenticCommerceCheckoutHttpSmokeProbeResult result = AgenticCommerceCheckoutHttpSmokeProbeResult.fromResponse(
                AgenticCommerceHttpResponse.json(200, "{\"passed\":"));

        assertThat(result.valid()).isFalse();
        assertThat(result.passed()).isFalse();
        assertThat(result.summary().passed()).isFalse();
        assertThat(result.issues())
                .extracting(AgenticCommerceCheckoutHttpIssueSummary::code)
                .containsExactly("probe_invalid_json");
    }

    @Test
    void reportsNonJsonContentType() {
        AgenticCommerceCheckoutHttpSmokeSummary summary = AgenticCommerceCheckoutHttpSmoke.result().summary();
        AgenticCommerceCheckoutHttpSmokeProbeResult result = AgenticCommerceCheckoutHttpSmokeProbeResult.fromResponse(
                new AgenticCommerceHttpResponse(
                        200,
                        AgenticCommerceJson.write(summary.toMap()),
                        Map.of(AgenticCommerceProtocol.HEADER_CONTENT_TYPE, "text/plain"),
                        Map.of()));

        assertThat(result.valid()).isFalse();
        assertThat(result.passed()).isFalse();
        assertThat(result.issues())
                .extracting(AgenticCommerceCheckoutHttpIssueSummary::code)
                .containsExactly("probe_content_type");
    }
}
