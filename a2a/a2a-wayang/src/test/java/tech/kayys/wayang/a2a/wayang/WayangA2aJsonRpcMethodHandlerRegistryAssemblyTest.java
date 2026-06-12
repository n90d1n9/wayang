package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WayangA2aJsonRpcMethodHandlerRegistryAssemblyTest {

    @Test
    void assemblesHandlersInGroupOrderAndRecordsOverrides() {
        WayangA2aJsonRpcMethodDispatchTable.Handler firstHandler =
                (request, preflight) -> WayangA2aHttpResponse.json(200, "{}");
        WayangA2aJsonRpcMethodDispatchTable.Handler replacementHandler =
                (request, preflight) -> WayangA2aHttpResponse.json(201, "{}");
        WayangA2aJsonRpcMethodDispatchTable.Handler taskHandler =
                (request, preflight) -> WayangA2aHttpResponse.json(202, "{}");
        Map<String, WayangA2aJsonRpcMethodDispatchTable.Handler> replacementHandlers = new LinkedHashMap<>();
        replacementHandlers.put(WayangA2aJsonRpcMethods.SEND_MESSAGE, replacementHandler);
        replacementHandlers.put(WayangA2aJsonRpcMethods.GET_TASK, taskHandler);

        WayangA2aJsonRpcMethodHandlerRegistryAssembly assembly =
                WayangA2aJsonRpcMethodHandlerRegistryAssembly.from(
                        List.of(
                                WayangA2aJsonRpcMethodHandlerGroup.of(
                                        WayangA2aJsonRpcCoreMethodHandlerContributions.GROUP_SEND_MESSAGE,
                                        Map.of(WayangA2aJsonRpcMethods.SEND_MESSAGE, firstHandler)),
                                WayangA2aJsonRpcMethodHandlerGroup.of(
                                        WayangA2aJsonRpcCoreMethodHandlerContributions.GROUP_TASK,
                                        replacementHandlers)),
                        WayangA2aJsonRpcMethodHandlerOverridePolicy.ALLOW_REPLACE);

        assertThat(assembly.handlers().keySet()).containsExactly(
                WayangA2aJsonRpcMethods.SEND_MESSAGE,
                WayangA2aJsonRpcMethods.GET_TASK);
        assertThat(assembly.handlers().get(WayangA2aJsonRpcMethods.SEND_MESSAGE))
                .isSameAs(replacementHandler);
        assertThat(assembly.overrides())
                .containsExactly(new WayangA2aJsonRpcMethodHandlerOverride(
                        WayangA2aJsonRpcMethods.SEND_MESSAGE,
                        WayangA2aJsonRpcCoreMethodHandlerContributions.GROUP_SEND_MESSAGE,
                        WayangA2aJsonRpcCoreMethodHandlerContributions.GROUP_TASK));
        assertThat(assembly.overrideMaps())
                .singleElement()
                .satisfies(override -> assertThat(override)
                        .containsEntry("method", WayangA2aJsonRpcMethods.SEND_MESSAGE)
                        .containsEntry("originalGroup",
                                WayangA2aJsonRpcCoreMethodHandlerContributions.GROUP_SEND_MESSAGE)
                        .containsEntry("replacementGroup",
                                WayangA2aJsonRpcCoreMethodHandlerContributions.GROUP_TASK));
    }

    @Test
    void exposesImmutableAssemblyCollections() {
        WayangA2aJsonRpcMethodDispatchTable.Handler handler =
                (request, preflight) -> WayangA2aHttpResponse.json(200, "{}");
        WayangA2aJsonRpcMethodHandlerRegistryAssembly assembly =
                WayangA2aJsonRpcMethodHandlerRegistryAssembly.from(
                        List.of(WayangA2aJsonRpcMethodHandlerGroup.of(
                                WayangA2aJsonRpcCoreMethodHandlerContributions.GROUP_TASK,
                                Map.of(WayangA2aJsonRpcMethods.GET_TASK, handler))),
                        WayangA2aJsonRpcMethodHandlerOverridePolicy.ALLOW_REPLACE);

        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> assembly.handlers().put(WayangA2aJsonRpcMethods.LIST_TASKS, handler));
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> assembly.overrides().add(new WayangA2aJsonRpcMethodHandlerOverride(
                        WayangA2aJsonRpcMethods.LIST_TASKS,
                        WayangA2aJsonRpcCoreMethodHandlerContributions.GROUP_TASK,
                        "extension")));
    }

    @Test
    void canRejectDuplicateMethodHandlersWhenOverridePolicyIsStrict() {
        WayangA2aJsonRpcMethodDispatchTable.Handler firstHandler =
                (request, preflight) -> WayangA2aHttpResponse.json(200, "{}");
        WayangA2aJsonRpcMethodDispatchTable.Handler replacementHandler =
                (request, preflight) -> WayangA2aHttpResponse.json(201, "{}");

        assertThatThrownBy(() -> WayangA2aJsonRpcMethodHandlerRegistryAssembly.from(
                List.of(
                        WayangA2aJsonRpcMethodHandlerGroup.of(
                                WayangA2aJsonRpcCoreMethodHandlerContributions.GROUP_SEND_MESSAGE,
                                Map.of(WayangA2aJsonRpcMethods.SEND_MESSAGE, firstHandler)),
                        WayangA2aJsonRpcMethodHandlerGroup.of(
                                "extension",
                                Map.of(WayangA2aJsonRpcMethods.SEND_MESSAGE, replacementHandler))),
                WayangA2aJsonRpcMethodHandlerOverridePolicy.REJECT_DUPLICATES))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duplicate A2A JSON-RPC method handler for SendMessage")
                .hasMessageContaining(WayangA2aJsonRpcCoreMethodHandlerContributions.GROUP_SEND_MESSAGE
                        + " -> extension");
    }
}
