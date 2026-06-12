package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aJsonRpcBindingReportProbeContextTest {

    @Test
    void parsesBindingReportProbeContextFromHttpResponse() {
        Map<String, Object> body = new LinkedHashMap<>(
                WayangA2aJsonRpcBindingReport.defaults().toMap());
        body.put("methodDispatch", WayangA2aJsonRpcMethodDispatchCoverage.from(
                List.of(WayangA2aJsonRpcMethods.SEND_MESSAGE, WayangA2aJsonRpcMethods.GET_TASK),
                List.of(WayangA2aJsonRpcMethods.SEND_MESSAGE)).toMap());
        body.put("methodRegistry", WayangA2aJsonRpcMethodRegistryTestFixtures.taskRegistryMap());

        WayangA2aJsonRpcBindingReportProbeContext context =
                WayangA2aJsonRpcBindingReportProbeContext.from(bindingReportResponse(body));

        assertThat(context.methodCount()).isPositive();
        assertThat(context.sections().bindingReport().path()).isNotBlank();
        assertThat(context.diagnosticHandlerCoverage().reported()).isTrue();
        assertThat(context.methodDispatchCoverage().reported()).isTrue();
        assertThat(context.methodDispatchCoverage().complete()).isFalse();
        assertThat(context.methodRegistrySnapshot().reported()).isTrue();
        assertThat(context.methodRegistrySnapshot().providerIds())
                .containsExactly(WayangA2aJsonRpcMethodRegistryTestFixtures.PROVIDER_TASK);
        assertThat(context.issues().issues())
                .anySatisfy(issue -> assertThat(issue)
                        .containsEntry(
                                "code",
                                WayangA2aJsonRpcReadinessIssueCatalog
                                        .ISSUE_METHOD_DISPATCH_COVERAGE_INCOMPLETE));
    }

    @Test
    void defaultsMissingNestedCoverageAndRegistrySections() {
        WayangA2aJsonRpcBindingReportProbeContext context =
                WayangA2aJsonRpcBindingReportProbeContext.from(WayangA2aHttpResponse.json(200, "{}")
                        .withHeaders(Map.of(
                                WayangA2aHttpResponse.HEADER_A2A_ROUTE_OPERATION,
                                WayangA2aJsonRpcBindingReport.OPERATION_JSON_RPC_BINDING_REPORT)));

        assertThat(context.methodCount()).isZero();
        assertThat(context.diagnosticHandlerCoverage().reported()).isFalse();
        assertThat(context.methodDispatchCoverage().reported()).isFalse();
        assertThat(context.methodRegistrySnapshot().reported()).isFalse();
        assertThat(context.issues().issueCount()).isPositive();
    }

    private static WayangA2aHttpResponse bindingReportResponse(Map<String, Object> body) {
        return new WayangA2aHttpResponse(
                200,
                WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON,
                WayangA2aHttpJson.write(body),
                Map.of(
                        WayangA2aHttpResponse.HEADER_CONTENT_TYPE,
                        WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON,
                        WayangA2aHttpResponse.HEADER_A2A_ROUTE_OPERATION,
                        WayangA2aJsonRpcBindingReport.OPERATION_JSON_RPC_BINDING_REPORT));
    }
}
