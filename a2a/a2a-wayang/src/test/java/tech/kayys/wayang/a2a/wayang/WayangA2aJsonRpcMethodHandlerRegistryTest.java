package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WayangA2aJsonRpcMethodHandlerRegistryTest {

    @Test
    void assemblesNamedHandlerGroupsInOrder() {
        WayangA2aJsonRpcMethodDispatchTable.Handler firstHandler =
                (request, preflight) -> WayangA2aHttpResponse.json(200, "{}");
        WayangA2aJsonRpcMethodDispatchTable.Handler overrideHandler =
                (request, preflight) -> WayangA2aHttpResponse.json(201, "{}");
        WayangA2aJsonRpcMethodDispatchTable.Handler secondHandler =
                (request, preflight) -> WayangA2aHttpResponse.json(202, "{}");
        Map<String, WayangA2aJsonRpcMethodDispatchTable.Handler> taskHandlers = new LinkedHashMap<>();
        taskHandlers.put(WayangA2aJsonRpcMethods.SEND_MESSAGE, overrideHandler);
        taskHandlers.put(WayangA2aJsonRpcMethods.GET_TASK, secondHandler);

        WayangA2aJsonRpcMethodHandlerRegistry registry = WayangA2aJsonRpcMethodHandlerRegistry.builder()
                .add(WayangA2aJsonRpcMethodHandlerGroup.of(
                        WayangA2aJsonRpcCoreMethodHandlerContributions.GROUP_SEND_MESSAGE,
                        Map.of(WayangA2aJsonRpcMethods.SEND_MESSAGE, firstHandler)))
                .add(WayangA2aJsonRpcMethodHandlerGroup.of(
                        WayangA2aJsonRpcCoreMethodHandlerContributions.GROUP_TASK,
                        taskHandlers))
                .build();

        assertThat(registry.groupNames()).containsExactly(
                WayangA2aJsonRpcCoreMethodHandlerContributions.GROUP_SEND_MESSAGE,
                WayangA2aJsonRpcCoreMethodHandlerContributions.GROUP_TASK);
        assertThat(registry.handlers().keySet()).containsExactly(
                WayangA2aJsonRpcMethods.SEND_MESSAGE,
                WayangA2aJsonRpcMethods.GET_TASK);
        assertThat(registry.handlers().get(WayangA2aJsonRpcMethods.SEND_MESSAGE)).isSameAs(overrideHandler);
        assertThat(registry.overridePolicy()).isEqualTo(WayangA2aJsonRpcMethodHandlerOverridePolicy.ALLOW_REPLACE);
        assertThat(registry.overrides())
                .containsExactly(new WayangA2aJsonRpcMethodHandlerOverride(
                        WayangA2aJsonRpcMethods.SEND_MESSAGE,
                        WayangA2aJsonRpcCoreMethodHandlerContributions.GROUP_SEND_MESSAGE,
                        WayangA2aJsonRpcCoreMethodHandlerContributions.GROUP_TASK));
        assertThat(registry.overrideMaps())
                .singleElement()
                .satisfies(override -> assertThat(override)
                        .containsEntry("method", WayangA2aJsonRpcMethods.SEND_MESSAGE)
                        .containsEntry("originalGroup",
                                WayangA2aJsonRpcCoreMethodHandlerContributions.GROUP_SEND_MESSAGE)
                        .containsEntry("replacementGroup",
                                WayangA2aJsonRpcCoreMethodHandlerContributions.GROUP_TASK));
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> registry.handlers().put(WayangA2aJsonRpcMethods.LIST_TASKS, secondHandler));
    }

    @Test
    void copiesBuilderGroupsAndExposesImmutableGroupList() {
        WayangA2aJsonRpcMethodHandlerGroup group = WayangA2aJsonRpcMethodHandlerGroup.of(
                WayangA2aJsonRpcCoreMethodHandlerContributions.GROUP_AGENT_CARD,
                Map.of(WayangA2aJsonRpcMethods.GET_EXTENDED_AGENT_CARD,
                        (request, preflight) -> WayangA2aHttpResponse.json(200, "{}")));
        List<WayangA2aJsonRpcMethodHandlerGroup> groups = new ArrayList<>();
        groups.add(group);

        WayangA2aJsonRpcMethodHandlerRegistry registry =
                WayangA2aJsonRpcMethodHandlerRegistry.builder().addAll(groups).build();
        groups.clear();

        assertThat(registry.groups()).containsExactly(group);
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> registry.groups().add(group));
    }

    @Test
    void acceptsMethodHandlerProvidersAsRegistryContributors() {
        WayangA2aJsonRpcMethodDispatchTable.Handler handler =
                (request, preflight) -> WayangA2aHttpResponse.json(200, "{}");
        WayangA2aJsonRpcMethodHandlerProvider provider = () -> WayangA2aJsonRpcMethodHandlerGroup.of(
                "provider",
                Map.of(WayangA2aJsonRpcMethods.GET_TASK, handler));

        WayangA2aJsonRpcMethodHandlerRegistry registry =
                WayangA2aJsonRpcMethodHandlerRegistry.builder().addProvider(provider).build();

        assertThat(registry.groupNames()).containsExactly("provider");
        assertThat(registry.handlers()).containsEntry(WayangA2aJsonRpcMethods.GET_TASK, handler);
    }

    @Test
    void preservesContributionMetadataOnHandlerGroups() {
        WayangA2aJsonRpcMethodDispatchTable.Handler handler =
                (request, preflight) -> WayangA2aHttpResponse.json(200, "{}");
        WayangA2aJsonRpcMethodHandlerContribution contribution =
                new WayangA2aJsonRpcMethodHandlerContribution(
                        "skill.weather",
                        "wayang-skills",
                        List.of("skill", "mcp", "weather"),
                        20);
        WayangA2aJsonRpcMethodHandlerGroup group = WayangA2aJsonRpcMethodHandlerGroup.of(
                "skill-weather",
                Map.of(WayangA2aJsonRpcMethods.GET_TASK, handler),
                contribution);

        WayangA2aJsonRpcMethodHandlerRegistry registry =
                WayangA2aJsonRpcMethodHandlerRegistry.builder().add(group).build();

        assertThat(registry.groups()).singleElement().satisfies(registered -> {
            assertThat(registered.name()).isEqualTo("skill-weather");
            assertThat(registered.contribution())
                    .isEqualTo(contribution);
            assertThat(registered.contribution().toMap())
                    .containsEntry("providerId", "skill.weather")
                    .containsEntry("moduleId", "wayang-skills")
                    .containsEntry("priority", 20);
            assertThat(WayangA2aMaps.stringList(registered.contribution().toMap().get("capabilityTags")))
                    .containsExactly("skill", "mcp", "weather");
        });
    }

    @Test
    void defaultsContributionMetadataToGroupName() {
        WayangA2aJsonRpcMethodHandlerGroup group = WayangA2aJsonRpcMethodHandlerGroup.of(
                "provider",
                Map.of(WayangA2aJsonRpcMethods.GET_TASK,
                        (request, preflight) -> WayangA2aHttpResponse.json(200, "{}")));

        assertThat(group.contribution().toMap())
                .containsEntry("providerId", "provider")
                .containsEntry("capabilityTags", List.of())
                .containsEntry("priority", 0);
    }

    @Test
    void copiesProviderListsBeforeBuildResultIsUsed() {
        WayangA2aJsonRpcMethodHandlerProvider provider = () -> WayangA2aJsonRpcMethodHandlerGroup.of(
                "provider",
                Map.of(WayangA2aJsonRpcMethods.GET_TASK,
                        (request, preflight) -> WayangA2aHttpResponse.json(200, "{}")));
        List<WayangA2aJsonRpcMethodHandlerProvider> providers = new ArrayList<>();
        providers.add(provider);

        WayangA2aJsonRpcMethodHandlerRegistry registry =
                WayangA2aJsonRpcMethodHandlerRegistry.builder().addProviders(providers).build();
        providers.clear();

        assertThat(registry.groupNames()).containsExactly("provider");
    }

    @Test
    void canRejectDuplicateMethodHandlersWhenOverridePolicyIsStrict() {
        WayangA2aJsonRpcMethodDispatchTable.Handler firstHandler =
                (request, preflight) -> WayangA2aHttpResponse.json(200, "{}");
        WayangA2aJsonRpcMethodDispatchTable.Handler overrideHandler =
                (request, preflight) -> WayangA2aHttpResponse.json(201, "{}");

        assertThatThrownBy(() -> WayangA2aJsonRpcMethodHandlerRegistry.builder()
                .overridePolicy(WayangA2aJsonRpcMethodHandlerOverridePolicy.REJECT_DUPLICATES)
                .add(WayangA2aJsonRpcMethodHandlerGroup.of(
                        WayangA2aJsonRpcCoreMethodHandlerContributions.GROUP_SEND_MESSAGE,
                        Map.of(WayangA2aJsonRpcMethods.SEND_MESSAGE, firstHandler)))
                .add(WayangA2aJsonRpcMethodHandlerGroup.of(
                        "extension",
                        Map.of(WayangA2aJsonRpcMethods.SEND_MESSAGE, overrideHandler)))
                .build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duplicate A2A JSON-RPC method handler for SendMessage")
                .hasMessageContaining(WayangA2aJsonRpcCoreMethodHandlerContributions.GROUP_SEND_MESSAGE
                        + " -> extension");
    }

    @Test
    void rejectsDuplicateHandlerGroupNames() {
        WayangA2aJsonRpcMethodDispatchTable.Handler firstHandler =
                (request, preflight) -> WayangA2aHttpResponse.json(200, "{}");
        WayangA2aJsonRpcMethodDispatchTable.Handler secondHandler =
                (request, preflight) -> WayangA2aHttpResponse.json(201, "{}");

        assertThatThrownBy(() -> WayangA2aJsonRpcMethodHandlerRegistry.builder()
                .add(WayangA2aJsonRpcMethodHandlerGroup.of(
                        "task-extension",
                        Map.of(WayangA2aJsonRpcMethods.GET_TASK, firstHandler)))
                .add(WayangA2aJsonRpcMethodHandlerGroup.of(
                        "task-extension",
                        Map.of(WayangA2aJsonRpcMethods.LIST_TASKS, secondHandler)))
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duplicate A2A JSON-RPC method handler group: task-extension");
    }

    @Test
    void handlerGroupRejectsBlankName() {
        assertThatThrownBy(() -> WayangA2aJsonRpcMethodHandlerGroup.of(" ", Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name must not be blank");
    }

    @Test
    void handlerGroupRejectsEmptyHandlers() {
        assertThatThrownBy(() -> WayangA2aJsonRpcMethodHandlerGroup.of("empty", Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("A2A JSON-RPC method handler group empty must contribute at least one handler");
    }

    @Test
    void handlerGroupRejectsUnsupportedMethods() {
        assertThatThrownBy(() -> WayangA2aJsonRpcMethodHandlerGroup.of(
                "unknown-method",
                Map.of("UnknownMethod", (request, preflight) -> WayangA2aHttpResponse.json(200, "{}"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("A2A JSON-RPC method handler group unknown-method")
                .hasMessageContaining("contributed unsupported method: UnknownMethod")
                .hasCauseInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void handlerContributionRejectsBlankProviderId() {
        assertThatThrownBy(() -> new WayangA2aJsonRpcMethodHandlerContribution(" ", "module", List.of(), 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("providerId must not be blank");
    }
}
