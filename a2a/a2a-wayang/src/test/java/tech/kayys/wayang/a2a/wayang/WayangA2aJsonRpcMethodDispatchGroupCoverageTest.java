package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aJsonRpcMethodDispatchGroupCoverageTest {

    @Test
    void reportsMissingMethodsInsideOneGroup() {
        WayangA2aJsonRpcMethodDispatchGroupCoverage coverage =
                WayangA2aJsonRpcMethodDispatchGroupCoverage.from(
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

        assertThat(coverage.complete()).isFalse();
        assertThat(coverage.registeredMethodCount()).isEqualTo(2);
        assertThat(coverage.dispatchMethodCount()).isEqualTo(1);
        assertThat(coverage.missingDispatchMethods()).containsExactly(WayangA2aJsonRpcMethods.LIST_TASKS);
        assertThat(coverage.orphanDispatchMethods()).isEmpty();
        assertThat(coverage.toMap())
                .containsEntry("group", WayangA2aJsonRpcMethods.METHOD_GROUP_TASK_QUERY)
                .containsEntry("complete", false)
                .containsEntry("registeredMethodCount", 2)
                .containsEntry("dispatchMethodCount", 1);
    }

    @Test
    void decodesGroupCoverageMaps() {
        WayangA2aJsonRpcMethodDispatchGroupCoverage coverage =
                WayangA2aJsonRpcMethodDispatchGroupCoverage.fromMap(Map.of(
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
}
