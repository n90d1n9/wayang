package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aJsonRpcReadinessProbeProjectionTest {

    @Test
    void keepsOrderedProbeEnvelopeWithoutUnreportedMethodDispatch() {
        WayangA2aJsonRpcReadinessProbeResult readiness = legacyReadiness();

        Map<String, Object> values = WayangA2aJsonRpcReadinessProbeProjection.probe(readiness);

        assertThat(values.keySet()).containsExactly(
                "passed",
                "exitCode",
                "bindingReportPassed",
                "routeCatalogRequired",
                "routeCatalogPassed",
                "smokeRequired",
                "smokePassed",
                "issueCount",
                "issues",
                "bindingReportProbe",
                "routeCatalogProbe",
                "smokeProbe");
        assertThat(values)
                .containsEntry("passed", true)
                .containsEntry("bindingReportPassed", true)
                .containsEntry("routeCatalogRequired", false)
                .containsEntry("smokeRequired", false)
                .doesNotContainKey("methodDispatch")
                .doesNotContainKey("methodRegistry");
    }

    @Test
    void insertsReportedMethodDispatchBeforeIssues() {
        WayangA2aJsonRpcReadinessProbeResult readiness = readinessWithMethodDispatchGap();

        Map<String, Object> values = WayangA2aJsonRpcReadinessProbeProjection.probe(readiness);
        Map<String, Object> methodDispatch = map(values.get("methodDispatch"));

        assertThat(values.keySet()).containsExactly(
                "passed",
                "exitCode",
                "bindingReportPassed",
                "routeCatalogRequired",
                "routeCatalogPassed",
                "smokeRequired",
                "smokePassed",
                "methodDispatch",
                "issueCount",
                "issues",
                "bindingReportProbe",
                "routeCatalogProbe",
                "smokeProbe");
        assertThat(methodDispatch)
                .containsEntry("reported", true)
                .containsEntry("complete", false)
                .containsEntry("passed", false)
                .containsEntry("registeredMethodCount", 2)
                .containsEntry("dispatchMethodCount", 2);
        assertThat(WayangA2aMaps.stringList(methodDispatch.get("missingDispatchMethods")))
                .containsExactly(WayangA2aJsonRpcMethods.GET_TASK);
        assertThat(WayangA2aMaps.stringList(methodDispatch.get("orphanDispatchMethods")))
                .containsExactly("agent/unknown");
    }

    @Test
    void insertsReportedMethodRegistryAfterMethodDispatchBeforeIssues() {
        WayangA2aJsonRpcReadinessProbeResult readiness = readinessWithMethodDispatchAndRegistry();

        Map<String, Object> values = WayangA2aJsonRpcReadinessProbeProjection.probe(readiness);
        Map<String, Object> methodRegistry = map(values.get("methodRegistry"));

        assertThat(values.keySet()).containsExactly(
                "passed",
                "exitCode",
                "bindingReportPassed",
                "routeCatalogRequired",
                "routeCatalogPassed",
                "smokeRequired",
                "smokePassed",
                "methodDispatch",
                "methodRegistry",
                "issueCount",
                "issues",
                "bindingReportProbe",
                "routeCatalogProbe",
                "smokeProbe");
        assertThat(methodRegistry)
                .containsEntry("reported", true)
                .containsEntry("groupCount", 1)
                .containsEntry("overridePolicy",
                        WayangA2aJsonRpcMethodRegistryTestFixtures.OVERRIDE_POLICY_ALLOW_REPLACE)
                .containsEntry("overrideCount", 0);
        assertThat(WayangA2aMaps.objectList(methodRegistry.get("groups")))
                .singleElement()
                .satisfies(group -> assertThat(group)
                        .containsEntry("name", WayangA2aJsonRpcMethodRegistryTestFixtures.GROUP_TASK)
                        .containsEntry("methodCount", 1));
    }

    @Test
    void keepsOrderedStandardReadinessAttributes() {
        WayangA2aJsonRpcReadinessProbeResult readiness = readinessWithMethodDispatchGap();

        Map<String, Object> attributes = WayangA2aJsonRpcReadinessProbeProjection.standardAttributes(readiness);

        assertThat(attributes.keySet()).containsExactly(
                "bindingReportPassed",
                "routeCatalogRequired",
                "routeCatalogPassed",
                "smokeRequired",
                "smokePassed",
                "methodDispatch");
        assertThat(attributes)
                .containsEntry("bindingReportPassed", false)
                .containsEntry("routeCatalogRequired", false)
                .containsEntry("smokeRequired", false);
        assertThat(map(attributes.get("methodDispatch"))).containsEntry("complete", false);
    }

    @Test
    void keepsOrderedStandardReadinessAttributesWithMethodRegistry() {
        WayangA2aJsonRpcReadinessProbeResult readiness = readinessWithMethodDispatchAndRegistry();

        Map<String, Object> attributes = WayangA2aJsonRpcReadinessProbeProjection.standardAttributes(readiness);

        assertThat(attributes.keySet()).containsExactly(
                "bindingReportPassed",
                "routeCatalogRequired",
                "routeCatalogPassed",
                "smokeRequired",
                "smokePassed",
                "methodDispatch",
                "methodRegistry");
        assertThat(map(attributes.get("methodRegistry")))
                .containsEntry("reported", true)
                .containsEntry("groupCount", 1);
    }

    @Test
    void buildsReadinessResponseThroughProjection() {
        WayangA2aJsonRpcReadinessProbeResult readiness = legacyReadiness();

        WayangA2aHttpResponse response = WayangA2aJsonRpcReadinessProbeProjection.response(readiness);

        assertThat(response.headers())
                .containsEntry(WayangA2aHttpResponse.HEADER_A2A_ROUTE_OPERATION,
                        WayangA2aJsonRpcReadinessProbeResult.OPERATION_JSON_RPC_READINESS)
                .containsEntry(WayangA2aHttpResponse.HEADER_A2A_PROTOCOL_VERSION,
                        readiness.bindingReportProbe().protocolVersion());
        assertThat(WayangA2aJsonRpcReadinessProbeResult.from(response).toJson())
                .isEqualTo(readiness.toJson());
    }

    private static WayangA2aJsonRpcReadinessProbeResult legacyReadiness() {
        return new WayangA2aJsonRpcReadinessProbeResult(
                WayangA2aJsonRpcBindingReportProbeResult.from(
                        WayangA2aJsonRpcBindingReport.defaults().response()),
                null,
                false,
                null,
                false);
    }

    private static WayangA2aJsonRpcReadinessProbeResult readinessWithMethodDispatchGap() {
        WayangA2aJsonRpcMethodDispatchCoverage coverage =
                WayangA2aJsonRpcMethodDispatchCoverage.from(
                        List.of(
                                WayangA2aJsonRpcMethods.SEND_MESSAGE,
                                WayangA2aJsonRpcMethods.GET_TASK),
                        List.of(
                                WayangA2aJsonRpcMethods.SEND_MESSAGE,
                                "agent/unknown"));
        WayangA2aJsonRpcBindingReport report = new WayangA2aJsonRpcBindingReport(
                WayangA2aJsonRpcHttpConfig.defaults(),
                WayangA2aJsonRpcMethods.methods(),
                coverage);
        return new WayangA2aJsonRpcReadinessProbeResult(
                WayangA2aJsonRpcBindingReportProbeResult.from(report.response()),
                null,
                false,
                null,
                false);
    }

    private static WayangA2aJsonRpcReadinessProbeResult readinessWithMethodDispatchAndRegistry() {
        WayangA2aJsonRpcMethodDispatchCoverage coverage =
                WayangA2aJsonRpcMethodDispatchCoverage.from(
                        List.of(WayangA2aJsonRpcMethods.SEND_MESSAGE),
                        List.of(WayangA2aJsonRpcMethods.SEND_MESSAGE));
        WayangA2aJsonRpcBindingReport report = new WayangA2aJsonRpcBindingReport(
                WayangA2aJsonRpcHttpConfig.defaults(),
                WayangA2aJsonRpcMethods.methods(),
                coverage,
                WayangA2aJsonRpcMethodRegistryTestFixtures.taskRegistrySnapshot());
        return new WayangA2aJsonRpcReadinessProbeResult(
                WayangA2aJsonRpcBindingReportProbeResult.from(report.response()),
                null,
                false,
                null,
                false);
    }

    private static Map<String, Object> map(Object value) {
        assertThat(value).isInstanceOf(Map.class);
        return WayangA2aMaps.copyMap((Map<?, ?>) value);
    }
}
