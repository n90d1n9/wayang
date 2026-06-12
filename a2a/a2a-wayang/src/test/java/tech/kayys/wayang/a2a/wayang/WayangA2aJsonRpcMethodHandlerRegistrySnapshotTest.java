package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aJsonRpcMethodHandlerRegistrySnapshotTest {

    @Test
    void projectsRegistryGroupsPolicyAndOverridesToMap() {
        WayangA2aJsonRpcMethodDispatchTable.Handler firstHandler =
                (request, preflight) -> WayangA2aHttpResponse.json(200, "{}");
        WayangA2aJsonRpcMethodDispatchTable.Handler overrideHandler =
                (request, preflight) -> WayangA2aHttpResponse.json(201, "{}");
        WayangA2aJsonRpcMethodHandlerRegistry registry = WayangA2aJsonRpcMethodHandlerRegistry.builder()
                .add(WayangA2aJsonRpcMethodHandlerGroup.of(
                        "task",
                        Map.of(WayangA2aJsonRpcMethods.GET_TASK, firstHandler)))
                .add(WayangA2aJsonRpcMethodHandlerGroup.of(
                        "extension",
                        Map.of(WayangA2aJsonRpcMethods.GET_TASK, overrideHandler)))
                .build();

        WayangA2aJsonRpcMethodHandlerRegistrySnapshot snapshot =
                WayangA2aJsonRpcMethodHandlerRegistrySnapshot.from(registry);
        Map<String, Object> values = snapshot.toMap();

        assertThat(snapshot.reported()).isTrue();
        assertThat(values)
                .containsEntry("reported", true)
                .containsEntry("groupCount", 2)
                .containsEntry("providerCount", 2)
                .containsEntry("overridePolicy",
                        WayangA2aJsonRpcMethodRegistryTestFixtures.OVERRIDE_POLICY_ALLOW_REPLACE)
                .containsEntry("overrideCount", 1);
        assertThat(WayangA2aMaps.objectList(values.get("groups")))
                .hasSize(2)
                .first()
                .satisfies(group -> assertThat(group)
                        .containsEntry("name", WayangA2aJsonRpcMethodRegistryTestFixtures.GROUP_TASK)
                        .containsEntry("methodCount", 1));
        assertThat(WayangA2aMaps.copyMap(
                (Map<?, ?>) WayangA2aMaps.objectList(values.get("groups")).getFirst().get("contribution")))
                .containsEntry("providerId", WayangA2aJsonRpcMethodRegistryTestFixtures.GROUP_TASK)
                .containsEntry("priority", 0);
        assertThat(WayangA2aMaps.stringList(values.get("providerIds")))
                .containsExactly(
                        WayangA2aJsonRpcMethodRegistryTestFixtures.GROUP_TASK,
                        WayangA2aJsonRpcMethodRegistryTestFixtures.GROUP_EXTENSION);
        assertThat(WayangA2aMaps.stringList(values.get("moduleIds"))).isEmpty();
        assertThat(WayangA2aMaps.stringList(values.get("capabilityTags"))).isEmpty();
        assertThat(WayangA2aMaps.stringList(WayangA2aMaps.objectList(values.get("groups")).getFirst().get("methods")))
                .containsExactly(WayangA2aJsonRpcMethods.GET_TASK);
        assertThat(WayangA2aMaps.objectList(values.get("overrides")))
                .singleElement()
                .satisfies(override -> assertThat(override)
                        .containsEntry("method", WayangA2aJsonRpcMethods.GET_TASK)
                        .containsEntry("originalGroup", WayangA2aJsonRpcMethodRegistryTestFixtures.GROUP_TASK)
                        .containsEntry("replacementGroup",
                                WayangA2aJsonRpcMethodRegistryTestFixtures.GROUP_EXTENSION));
    }

    @Test
    void missingRegistryProducesUnreportedSnapshot() {
        WayangA2aJsonRpcMethodHandlerRegistrySnapshot snapshot =
                WayangA2aJsonRpcMethodHandlerRegistrySnapshot.from(null);

        assertThat(snapshot.reported()).isFalse();
        assertThat(snapshot.toMap())
                .containsEntry("reported", false)
                .containsEntry("groupCount", 0)
                .containsEntry("overrideCount", 0);
    }

    @Test
    void decodesRegistrySnapshotFromMap() {
        WayangA2aJsonRpcMethodHandlerRegistrySnapshot snapshot =
                WayangA2aJsonRpcMethodHandlerRegistrySnapshot.fromMap(Map.of(
                        "reported", "true",
                        "groups", List.of(WayangA2aJsonRpcMethodRegistryTestFixtures.taskGroupMap()),
                        "overridePolicy",
                        " " + WayangA2aJsonRpcMethodRegistryTestFixtures.OVERRIDE_POLICY_ALLOW_REPLACE + " ",
                        "overrides", List.of(WayangA2aJsonRpcMethodRegistryTestFixtures.taskOverrideMap())));

        assertThat(snapshot.reported()).isTrue();
        assertThat(snapshot.overridePolicy())
                .isEqualTo(WayangA2aJsonRpcMethodRegistryTestFixtures.OVERRIDE_POLICY_ALLOW_REPLACE);
        assertThat(snapshot.groupCount()).isEqualTo(1);
        assertThat(snapshot.providerCount()).isEqualTo(1);
        assertThat(snapshot.providerIds()).containsExactly(WayangA2aJsonRpcMethodRegistryTestFixtures.PROVIDER_TASK);
        assertThat(snapshot.moduleIds()).containsExactly(WayangA2aJsonRpcMethodRegistryTestFixtures.MODULE_TEST);
        assertThat(snapshot.capabilityTags()).containsExactly("test", "task");
        assertThat(snapshot.overrideCount()).isEqualTo(1);
        assertThat(snapshot.toMap())
                .containsEntry("overridePolicy",
                        WayangA2aJsonRpcMethodRegistryTestFixtures.OVERRIDE_POLICY_ALLOW_REPLACE);
    }

    @Test
    void preservesExplicitProviderSummaryFromMap() {
        WayangA2aJsonRpcMethodHandlerRegistrySnapshot snapshot =
                WayangA2aJsonRpcMethodHandlerRegistrySnapshot.fromMap(Map.of(
                        "reported", true,
                        "providerIds", List.of("db.skill", "file.skill"),
                        "moduleIds", List.of("wayang-skills"),
                        "capabilityTags", List.of("skill", "rag"),
                        "overridePolicy", "ALLOW_REPLACE"));

        assertThat(snapshot.providerCount()).isEqualTo(2);
        assertThat(snapshot.providerIds()).containsExactly("db.skill", "file.skill");
        assertThat(snapshot.moduleIds()).containsExactly("wayang-skills");
        assertThat(snapshot.capabilityTags()).containsExactly("skill", "rag");
        assertThat(snapshot.toMap())
                .containsEntry("providerCount", 2)
                .containsEntry("providerIds", List.of("db.skill", "file.skill"));
    }
}
