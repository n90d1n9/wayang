package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aJsonRpcMethodDispatchGroupCoverageProjectionTest {

    @Test
    void keepsOrderedGroupCoverageEnvelope() {
        WayangA2aJsonRpcMethodDispatchGroupCoverage coverage = incompleteGroupCoverage();

        Map<String, Object> values =
                WayangA2aJsonRpcMethodDispatchGroupCoverageProjection.group(coverage);

        assertThat(values.keySet()).containsExactly(
                "group",
                "complete",
                "registeredMethodCount",
                "dispatchMethodCount",
                "registeredMethods",
                "dispatchMethods",
                "missingDispatchMethods",
                "orphanDispatchMethods");
        assertThat(values)
                .containsEntry("group", WayangA2aJsonRpcMethods.METHOD_GROUP_TASK_QUERY)
                .containsEntry("complete", false)
                .containsEntry("registeredMethodCount", 2)
                .containsEntry("dispatchMethodCount", 1);
        assertThat(WayangA2aMaps.stringList(values.get("missingDispatchMethods")))
                .containsExactly(WayangA2aJsonRpcMethods.LIST_TASKS);
        assertThat(WayangA2aMaps.stringList(values.get("orphanDispatchMethods"))).isEmpty();
    }

    @Test
    void parsesGroupCoverageMapsThroughProjection() {
        WayangA2aJsonRpcMethodDispatchGroupCoverage coverage =
                WayangA2aJsonRpcMethodDispatchGroupCoverageProjection.fromMap(Map.of(
                        "group", " custom ",
                        "registeredMethods", List.of("A", "B"),
                        "dispatchMethods", List.of("A", "C"),
                        "missingDispatchMethods", List.of("B"),
                        "orphanDispatchMethods", List.of("C")));

        assertThat(coverage.group()).isEqualTo("custom");
        assertThat(coverage.complete()).isFalse();
        assertThat(coverage.registeredMethods()).containsExactly("A", "B");
        assertThat(coverage.dispatchMethods()).containsExactly("A", "C");
        assertThat(coverage.missingDispatchMethods()).containsExactly("B");
        assertThat(coverage.orphanDispatchMethods()).containsExactly("C");
    }

    @Test
    void missingGroupNameFallsBackToUnknown() {
        WayangA2aJsonRpcMethodDispatchGroupCoverage coverage =
                WayangA2aJsonRpcMethodDispatchGroupCoverageProjection.fromMap(Map.of(
                        "registeredMethods", List.of("A"),
                        "dispatchMethods", List.of("A")));

        assertThat(coverage.group()).isEqualTo("unknown");
        assertThat(coverage.complete()).isTrue();
        assertThat(WayangA2aJsonRpcMethodDispatchGroupCoverageProjection.group(coverage))
                .containsEntry("group", "unknown")
                .containsEntry("complete", true);
    }

    @Test
    void recordDelegatesToProjectionForGroupMap() {
        WayangA2aJsonRpcMethodDispatchGroupCoverage coverage = incompleteGroupCoverage();

        assertThat(coverage.toMap())
                .isEqualTo(WayangA2aJsonRpcMethodDispatchGroupCoverageProjection.group(coverage));
        assertThat(WayangA2aJsonRpcMethodDispatchGroupCoverage.fromMap(coverage.toMap()).toMap())
                .isEqualTo(coverage.toMap());
    }

    private static WayangA2aJsonRpcMethodDispatchGroupCoverage incompleteGroupCoverage() {
        return WayangA2aJsonRpcMethodDispatchGroupCoverage.from(
                WayangA2aJsonRpcMethods.METHOD_GROUP_TASK_QUERY,
                List.of(
                        WayangA2aJsonRpcMethods.GET_TASK,
                        WayangA2aJsonRpcMethods.LIST_TASKS),
                List.of(
                        WayangA2aJsonRpcMethods.GET_TASK,
                        WayangA2aJsonRpcMethods.LIST_TASKS),
                List.of(
                        WayangA2aJsonRpcMethods.GET_TASK,
                        WayangA2aJsonRpcMethods.CANCEL_TASK));
    }
}
