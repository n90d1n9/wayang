package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aJsonRpcMethodDispatchCoverageProjectionTest {

    @Test
    void keepsOrderedCoverageEnvelope() {
        WayangA2aJsonRpcMethodDispatchCoverage coverage = completeCoverage();

        Map<String, Object> values = WayangA2aJsonRpcMethodDispatchCoverageProjection.coverage(coverage);

        assertThat(values.keySet()).containsExactly(
                "complete",
                "registeredMethodCount",
                "dispatchMethodCount",
                "registeredMethods",
                "dispatchMethods",
                "missingDispatchMethods",
                "orphanDispatchMethods",
                "methodGroups");
        assertThat(values)
                .containsEntry("complete", true)
                .containsEntry("registeredMethodCount", WayangA2aJsonRpcMethods.methods().size())
                .containsEntry("dispatchMethodCount", WayangA2aJsonRpcMethods.methods().size());
        assertThat(WayangA2aMaps.stringList(values.get("registeredMethods")))
                .containsExactlyElementsOf(WayangA2aJsonRpcMethods.methods());
        assertThat(WayangA2aMaps.objectList(values.get("methodGroups")))
                .hasSize(WayangA2aJsonRpcMethods.methodGroups().size());
    }

    @Test
    void parsesCoverageMapsWithExplicitGroups() {
        WayangA2aJsonRpcMethodDispatchCoverage coverage =
                WayangA2aJsonRpcMethodDispatchCoverageProjection.fromMap(Map.of(
                        "registeredMethods",
                        List.of(WayangA2aJsonRpcMethods.SEND_MESSAGE, WayangA2aJsonRpcMethods.GET_TASK),
                        "dispatchMethods",
                        List.of(WayangA2aJsonRpcMethods.SEND_MESSAGE, "CustomMethod"),
                        "missingDispatchMethods",
                        List.of(WayangA2aJsonRpcMethods.GET_TASK),
                        "orphanDispatchMethods",
                        List.of("CustomMethod"),
                        "methodGroups",
                        List.of(Map.of(
                                "group",
                                WayangA2aJsonRpcMethods.METHOD_GROUP_TASK_QUERY,
                                "registeredMethods",
                                List.of(WayangA2aJsonRpcMethods.GET_TASK),
                                "dispatchMethods",
                                List.of(),
                                "missingDispatchMethods",
                                List.of(WayangA2aJsonRpcMethods.GET_TASK),
                                "orphanDispatchMethods",
                                List.of()))));

        assertThat(coverage.complete()).isFalse();
        assertThat(coverage.registeredMethodCount()).isEqualTo(2);
        assertThat(coverage.dispatchMethodCount()).isEqualTo(2);
        assertThat(coverage.methodGroups())
                .singleElement()
                .returns(WayangA2aJsonRpcMethods.METHOD_GROUP_TASK_QUERY,
                        WayangA2aJsonRpcMethodDispatchGroupCoverage::group);
    }

    @Test
    void derivesMethodGroupsForFlattenedReportedMaps() {
        WayangA2aJsonRpcMethodDispatchCoverage coverage =
                WayangA2aJsonRpcMethodDispatchCoverageProjection.fromMap(Map.of(
                        "registeredMethods",
                        List.of(WayangA2aJsonRpcMethods.SEND_MESSAGE, WayangA2aJsonRpcMethods.GET_TASK),
                        "dispatchMethods",
                        List.of(WayangA2aJsonRpcMethods.SEND_MESSAGE, "CustomMethod"),
                        "missingDispatchMethods",
                        List.of(WayangA2aJsonRpcMethods.GET_TASK),
                        "orphanDispatchMethods",
                        List.of("CustomMethod")));

        assertThat(coverage.complete()).isFalse();
        assertThat(coverage.methodGroups())
                .anySatisfy(group -> assertThat(group)
                        .returns(WayangA2aJsonRpcMethods.METHOD_GROUP_TASK_QUERY,
                                WayangA2aJsonRpcMethodDispatchGroupCoverage::group)
                        .returns(false, WayangA2aJsonRpcMethodDispatchGroupCoverage::complete))
                .anySatisfy(group -> assertThat(group)
                        .returns("unassigned", WayangA2aJsonRpcMethodDispatchGroupCoverage::group)
                        .returns(List.of("CustomMethod"),
                                WayangA2aJsonRpcMethodDispatchGroupCoverage::orphanDispatchMethods));
    }

    @Test
    void ignoresStatusOnlyMapsWhenReportingCoverage() {
        WayangA2aJsonRpcMethodDispatchCoverage coverage =
                WayangA2aJsonRpcMethodDispatchCoverageProjection.fromMap(Map.of("complete", true));

        assertThat(coverage.complete()).isFalse();
        assertThat(coverage.registeredMethods()).isEmpty();
        assertThat(coverage.dispatchMethods()).isEmpty();
        assertThat(WayangA2aJsonRpcMethodDispatchCoverageProjection.coverage(coverage))
                .containsEntry("complete", false)
                .containsEntry("registeredMethodCount", 0)
                .containsEntry("dispatchMethodCount", 0);
    }

    @Test
    void recordDelegatesToProjectionForCoverageMap() {
        WayangA2aJsonRpcMethodDispatchCoverage coverage = completeCoverage();

        assertThat(coverage.toMap())
                .isEqualTo(WayangA2aJsonRpcMethodDispatchCoverageProjection.coverage(coverage));
        assertThat(coverage.methodGroupMaps())
                .isEqualTo(WayangA2aJsonRpcMethodDispatchCoverageProjection.methodGroups(coverage));
        assertThat(WayangA2aJsonRpcMethodDispatchCoverage.fromMap(coverage.toMap()).toMap())
                .isEqualTo(coverage.toMap());
    }

    private static WayangA2aJsonRpcMethodDispatchCoverage completeCoverage() {
        return WayangA2aJsonRpcMethodDispatchCoverage.from(
                WayangA2aJsonRpcMethods.methods(),
                WayangA2aJsonRpcMethods.methods());
    }
}
