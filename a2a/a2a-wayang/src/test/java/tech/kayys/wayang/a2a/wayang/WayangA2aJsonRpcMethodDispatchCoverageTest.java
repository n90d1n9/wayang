package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aJsonRpcMethodDispatchCoverageTest {

    @Test
    void reportsCompleteCoverageForRegisteredDispatchMethods() {
        WayangA2aJsonRpcMethodDispatchCoverage coverage = WayangA2aJsonRpcMethodDispatchCoverage.from(
                WayangA2aJsonRpcMethods.methods(),
                WayangA2aJsonRpcMethods.methods());

        assertThat(coverage.complete()).isTrue();
        assertThat(coverage.registeredMethodCount()).isEqualTo(WayangA2aJsonRpcMethods.methods().size());
        assertThat(coverage.dispatchMethodCount()).isEqualTo(WayangA2aJsonRpcMethods.methods().size());
        assertThat(coverage.registeredMethods()).containsExactlyElementsOf(WayangA2aJsonRpcMethods.methods());
        assertThat(coverage.dispatchMethods()).containsExactlyElementsOf(WayangA2aJsonRpcMethods.methods());
        assertThat(coverage.missingDispatchMethods()).isEmpty();
        assertThat(coverage.orphanDispatchMethods()).isEmpty();
        assertThat(coverage.methodGroups())
                .allSatisfy(group -> assertThat(group.complete()).isTrue());
        assertThat(coverage.methodGroups().stream()
                        .flatMap(group -> group.registeredMethods().stream())
                        .toList())
                .containsExactlyElementsOf(WayangA2aJsonRpcMethods.methods());
        assertThat(coverage.toMap())
                .containsEntry("complete", true)
                .containsEntry("registeredMethodCount", WayangA2aJsonRpcMethods.methods().size())
                .containsEntry("dispatchMethodCount", WayangA2aJsonRpcMethods.methods().size());
        assertThat(WayangA2aMaps.objectList(coverage.toMap().get("methodGroups")))
                .hasSize(WayangA2aJsonRpcMethods.methodGroups().size());
        String coverageJson = WayangA2aHttpJson.write(coverage.toMap());
        assertThat(coverageJson).startsWith("{\"complete\":");
        assertThat(coverageJson.indexOf("\"registeredMethods\""))
                .isGreaterThan(coverageJson.indexOf("\"dispatchMethodCount\""));
        assertThat(coverageJson.indexOf("\"orphanDispatchMethods\""))
                .isGreaterThan(coverageJson.indexOf("\"missingDispatchMethods\""));
    }

    @Test
    void reportsMissingAndOrphanDispatchMethods() {
        WayangA2aJsonRpcMethodDispatchCoverage coverage = WayangA2aJsonRpcMethodDispatchCoverage.from(
                List.of(
                        WayangA2aJsonRpcMethods.SEND_MESSAGE,
                        WayangA2aJsonRpcMethods.GET_TASK),
                List.of(WayangA2aJsonRpcMethods.SEND_MESSAGE, "CustomMethod"));

        assertThat(coverage.complete()).isFalse();
        assertThat(coverage.registeredMethods()).containsExactly(
                WayangA2aJsonRpcMethods.SEND_MESSAGE,
                WayangA2aJsonRpcMethods.GET_TASK);
        assertThat(coverage.dispatchMethods()).containsExactly(WayangA2aJsonRpcMethods.SEND_MESSAGE, "CustomMethod");
        assertThat(coverage.missingDispatchMethods()).containsExactly(WayangA2aJsonRpcMethods.GET_TASK);
        assertThat(coverage.orphanDispatchMethods()).containsExactly("CustomMethod");
        assertThat(coverage.methodGroups())
                .anySatisfy(group -> assertThat(group)
                        .returns(WayangA2aJsonRpcMethods.METHOD_GROUP_TASK_QUERY,
                                WayangA2aJsonRpcMethodDispatchGroupCoverage::group)
                        .returns(false, WayangA2aJsonRpcMethodDispatchGroupCoverage::complete)
                        .returns(List.of(WayangA2aJsonRpcMethods.GET_TASK),
                                WayangA2aJsonRpcMethodDispatchGroupCoverage::missingDispatchMethods))
                .anySatisfy(group -> assertThat(group)
                        .returns("unassigned", WayangA2aJsonRpcMethodDispatchGroupCoverage::group)
                        .returns(List.of("CustomMethod"),
                                WayangA2aJsonRpcMethodDispatchGroupCoverage::orphanDispatchMethods));
    }

    @Test
    void decodesCoverageMap() {
        WayangA2aJsonRpcMethodDispatchCoverage coverage = WayangA2aJsonRpcMethodDispatchCoverage.fromMap(Map.of(
                "registeredMethods", List.of(WayangA2aJsonRpcMethods.SEND_MESSAGE, WayangA2aJsonRpcMethods.GET_TASK),
                "dispatchMethods", List.of(WayangA2aJsonRpcMethods.SEND_MESSAGE, "CustomMethod"),
                "missingDispatchMethods", List.of(WayangA2aJsonRpcMethods.GET_TASK),
                "orphanDispatchMethods", List.of("CustomMethod"),
                "methodGroups", List.of(Map.of(
                        "group", WayangA2aJsonRpcMethods.METHOD_GROUP_TASK_QUERY,
                        "registeredMethods", List.of(WayangA2aJsonRpcMethods.GET_TASK),
                        "dispatchMethods", List.of(),
                        "missingDispatchMethods", List.of(WayangA2aJsonRpcMethods.GET_TASK),
                        "orphanDispatchMethods", List.of()))));

        assertThat(coverage.complete()).isFalse();
        assertThat(coverage.registeredMethodCount()).isEqualTo(2);
        assertThat(coverage.dispatchMethodCount()).isEqualTo(2);
        assertThat(coverage.registeredMethods()).containsExactly(
                WayangA2aJsonRpcMethods.SEND_MESSAGE,
                WayangA2aJsonRpcMethods.GET_TASK);
        assertThat(coverage.dispatchMethods()).containsExactly(WayangA2aJsonRpcMethods.SEND_MESSAGE, "CustomMethod");
        assertThat(coverage.missingDispatchMethods()).containsExactly(WayangA2aJsonRpcMethods.GET_TASK);
        assertThat(coverage.orphanDispatchMethods()).containsExactly("CustomMethod");
        assertThat(coverage.methodGroups())
                .singleElement()
                .returns(WayangA2aJsonRpcMethods.METHOD_GROUP_TASK_QUERY,
                        WayangA2aJsonRpcMethodDispatchGroupCoverage::group);
    }

    @Test
    void treatsMissingCoverageMapAsIncomplete() {
        WayangA2aJsonRpcMethodDispatchCoverage coverage =
                WayangA2aJsonRpcMethodDispatchCoverage.fromMap(Map.of());

        assertThat(coverage.complete()).isFalse();
        assertThat(coverage.registeredMethodCount()).isZero();
        assertThat(coverage.dispatchMethodCount()).isZero();
        assertThat(coverage.registeredMethods()).isEmpty();
        assertThat(coverage.dispatchMethods()).isEmpty();
        assertThat(coverage.missingDispatchMethods()).isEmpty();
        assertThat(coverage.orphanDispatchMethods()).isEmpty();
        assertThat(coverage.methodGroups()).isEmpty();
        assertThat(coverage.toMap()).containsEntry("complete", false);
    }

    @Test
    void treatsStatusOnlyCoverageMapAsIncomplete() {
        WayangA2aJsonRpcMethodDispatchCoverage coverage =
                WayangA2aJsonRpcMethodDispatchCoverage.fromMap(Map.of("complete", true));

        assertThat(coverage.complete()).isFalse();
        assertThat(coverage.registeredMethods()).isEmpty();
        assertThat(coverage.dispatchMethods()).isEmpty();
    }
}
