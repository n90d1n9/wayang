package tech.kayys.wayang.a2ui.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2ui.core.A2uiServerMessage;
import tech.kayys.wayang.gollek.sdk.AgentRunStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WayangA2uiSurfaceRegistryTest {

    private final A2uiJsonlTestSupport jsonl = new A2uiJsonlTestSupport();

    @Test
    void rendersStandardRunStatusThroughRegisteredSurface() {
        WayangA2uiSurfaceRegistry registry = WayangA2uiSurfaceRegistry.standard(
                WayangA2uiSurfaceOptions.runLifecycle());
        AgentRunStatus status = RunSurfaceFixtures.runningStatus();

        String jsonlStream = jsonl.stream(registry.renderRequired(status));

        assertThat(registry.kindOf(status)).contains(WayangA2uiSurfaceRegistry.RUN_STATUS);
        assertThat(registry.supports(status)).isTrue();
        assertThat(registry.supports("unsupported")).isFalse();
        assertThat(registry.render("unsupported")).isEmpty();
        assertThat(jsonlStream)
                .contains("wayang-run-run-1")
                .contains("wayang.run.wait")
                .contains("wayang.run.cancel");
        assertThatThrownBy(() -> registry.renderRequired("unsupported"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("java.lang.String");
    }

    @Test
    void supportsCustomSurfaceRenderers() {
        WayangA2uiSurfaceRegistry registry = WayangA2uiSurfaceRegistry.builder()
                .register(
                        "custom.string",
                        String.class,
                        value -> WayangA2uiResultSurfaces.actionResult(
                                WayangA2uiActionResult.rejected("custom", "", value),
                                2))
                .build();

        String jsonlStream = jsonl.stream(registry.renderRequired("hello"));

        assertThat(registry.surfaceKinds()).containsExactly("custom.string");
        assertThat(registry.kindOf("hello")).contains("custom.string");
        assertThat(jsonlStream)
                .contains("wayang-action-result-2-custom")
                .contains("hello");
    }

    @Test
    void normalizesCustomRendererMessageLists() {
        WayangA2uiSurfaceRegistry registry = WayangA2uiSurfaceRegistry.builder()
                .register(
                        "custom.string",
                        String.class,
                        value -> {
                            List<A2uiServerMessage> messages = new ArrayList<>();
                            messages.add(null);
                            messages.addAll(WayangA2uiResultSurfaces.actionResult(
                                    WayangA2uiActionResult.rejected("custom", "", value),
                                    2));
                            return messages;
                        })
                .build();

        List<A2uiServerMessage> messages = registry.renderRequired("hello");

        assertThat(messages).doesNotContainNull();
        assertThat(messages).isNotEmpty();
        assertThatThrownBy(() -> messages.add(null))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void extendsStandardRegistryWithoutLosingDefaultSurfaces() {
        WayangA2uiSurfaceRegistry registry = WayangA2uiSurfaceRegistry.standardBuilder(
                        WayangA2uiSurfaceOptions.runLifecycle())
                .register(
                        "custom.string",
                        String.class,
                        value -> WayangA2uiResultSurfaces.actionResult(
                                WayangA2uiActionResult.rejected("custom", "", value),
                                3))
                .build();
        AgentRunStatus status = RunSurfaceFixtures.runningStatus();

        String statusJsonl = jsonl.stream(registry.renderRequired(status));
        String customJsonl = jsonl.stream(registry.renderRequired("hello"));

        assertThat(registry.surfaceKinds())
                .contains(
                        WayangA2uiSurfaceRegistry.RUN_STATUS,
                        WayangA2uiSurfaceRegistry.RUN_INSPECTION,
                        "custom.string");
        assertThat(statusJsonl)
                .contains("wayang-run-run-1")
                .contains("wayang.run.wait");
        assertThat(customJsonl)
                .contains("wayang-action-result-3-custom")
                .contains("hello");
    }

    @Test
    void replacesExactRendererBinding() {
        WayangA2uiSurfaceRegistry registry = WayangA2uiSurfaceRegistry.standard(
                        WayangA2uiSurfaceOptions.runLifecycle())
                .toBuilder()
                .replace(
                        WayangA2uiSurfaceRegistry.RUN_STATUS,
                        AgentRunStatus.class,
                        status -> WayangA2uiResultSurfaces.actionResult(
                                WayangA2uiActionResult.handled(
                                        "custom.status",
                                        status.handle().runId(),
                                        "Custom status surface: " + status.handle().runId(),
                                        List.of(),
                                        Map.of("state", status.handle().state().name())),
                                4))
                .build();
        AgentRunStatus status = RunSurfaceFixtures.runningStatus();

        String jsonlStream = jsonl.stream(registry.renderRequired(status));

        assertThat(registry.surfaceKinds())
                .contains(WayangA2uiSurfaceRegistry.RUN_STATUS)
                .doesNotHaveDuplicates();
        assertThat(jsonlStream)
                .contains("wayang-action-result-4-custom-status")
                .contains("Custom status surface: run-1")
                .doesNotContain("wayang-run-run-1");
    }

    @Test
    void replacesOneBindingWithinSharedSurfaceKind() {
        WayangA2uiSurfaceRegistry registry = WayangA2uiSurfaceRegistry.readOnly()
                .toBuilder()
                .replace(
                        WayangA2uiSurfaceRegistry.ACTION_RESULT,
                        WayangA2uiActionFeedback.class,
                        feedback -> WayangA2uiResultSurfaces.actionResult(
                                WayangA2uiActionResult.rejected(
                                        "custom.feedback",
                                        feedback.result().runId(),
                                        "Custom feedback " + feedback.sequence()),
                                feedback.sequence() + 10))
                .build();
        WayangA2uiActionResult result = WayangA2uiActionResult.rejected(
                WayangA2uiActions.RUN_CANCEL,
                "run-1",
                "Cancel is not allowed.");

        String feedbackJsonl = jsonl.stream(registry.renderActionFeedback(result, 2));
        String resultJsonl = jsonl.stream(registry.renderRequired(result));

        assertThat(registry.surfaceDescriptors(WayangA2uiSurfaceRegistry.ACTION_RESULT))
                .containsExactly(
                        new WayangA2uiSurfaceDescriptor(
                                WayangA2uiSurfaceRegistry.ACTION_RESULT,
                                WayangA2uiActionFeedback.class),
                        new WayangA2uiSurfaceDescriptor(
                                WayangA2uiSurfaceRegistry.ACTION_RESULT,
                                WayangA2uiActionResult.class));
        assertThat(feedbackJsonl)
                .contains("wayang-action-result-12-custom-feedback")
                .contains("Custom feedback 2");
        assertThat(resultJsonl)
                .contains("wayang-action-result-1-wayang-run-cancel")
                .contains("Cancel is not allowed.");
    }

    @Test
    void exposesExplicitBroadReplacementHelpers() {
        WayangA2uiSurfaceRegistry kindReplaced = WayangA2uiSurfaceRegistry.readOnly()
                .toBuilder()
                .replaceKind(
                        WayangA2uiSurfaceRegistry.ACTION_RESULT,
                        WayangA2uiActionFeedback.class,
                        feedback -> WayangA2uiResultSurfaces.actionResult(feedback.result(), 8))
                .build();
        WayangA2uiSurfaceRegistry modelReplaced = WayangA2uiSurfaceRegistry.readOnly()
                .toBuilder()
                .replaceModelType(
                        "custom.action.result",
                        WayangA2uiActionResult.class,
                        result -> WayangA2uiResultSurfaces.actionResult(result, 9))
                .build();

        assertThat(kindReplaced.surfaceDescriptors(WayangA2uiSurfaceRegistry.ACTION_RESULT))
                .containsExactly(new WayangA2uiSurfaceDescriptor(
                        WayangA2uiSurfaceRegistry.ACTION_RESULT,
                        WayangA2uiActionFeedback.class));
        assertThat(modelReplaced.surfaceDescriptors(WayangA2uiSurfaceRegistry.ACTION_RESULT))
                .containsExactly(new WayangA2uiSurfaceDescriptor(
                        WayangA2uiSurfaceRegistry.ACTION_RESULT,
                        WayangA2uiActionFeedback.class));
        assertThat(modelReplaced.descriptorForModelType(WayangA2uiActionResult.class))
                .contains(new WayangA2uiSurfaceDescriptor(
                        "custom.action.result",
                        WayangA2uiActionResult.class));
    }

    @Test
    void rendersSequenceAwareActionFeedback() {
        WayangA2uiSurfaceRegistry registry = WayangA2uiSurfaceRegistry.readOnly();
        WayangA2uiActionResult result = WayangA2uiActionResult.rejected(
                WayangA2uiActions.RUN_CANCEL,
                "run-1",
                "Cancel is not allowed.");

        String jsonlStream = jsonl.stream(registry.renderActionFeedback(result, 7));

        assertThat(registry.kindOf(WayangA2uiActionFeedback.of(result, 7)))
                .contains(WayangA2uiSurfaceRegistry.ACTION_RESULT);
        assertThat(jsonlStream)
                .contains("wayang-action-result-7-wayang-run-cancel")
                .contains("Cancel is not allowed.");
    }

    @Test
    void stillRendersPlainActionResultsThroughRegistry() {
        WayangA2uiSurfaceRegistry registry = WayangA2uiSurfaceRegistry.readOnly();
        WayangA2uiActionResult result = WayangA2uiActionResult.rejected(
                WayangA2uiActions.RUN_CANCEL,
                "run-1",
                "Cancel is not allowed.");

        String jsonlStream = jsonl.stream(registry.renderRequired(result));

        assertThat(registry.kindOf(result)).contains(WayangA2uiSurfaceRegistry.ACTION_RESULT);
        assertThat(registry.surfaceKinds())
                .contains(WayangA2uiSurfaceRegistry.ACTION_RESULT)
                .doesNotHaveDuplicates();
        assertThat(jsonlStream)
                .contains("wayang-action-result-1-wayang-run-cancel")
                .contains("Cancel is not allowed.");
    }

    @Test
    void exposesSurfaceDescriptorsForModelBindings() {
        WayangA2uiSurfaceRegistry registry = WayangA2uiSurfaceRegistry.readOnly();

        assertThat(registry.surfaceDescriptors())
                .contains(
                        new WayangA2uiSurfaceDescriptor(
                                WayangA2uiSurfaceRegistry.RUN_STATUS,
                                AgentRunStatus.class),
                        new WayangA2uiSurfaceDescriptor(
                                WayangA2uiSurfaceRegistry.ACTION_RESULT,
                                WayangA2uiActionFeedback.class),
                        new WayangA2uiSurfaceDescriptor(
                                WayangA2uiSurfaceRegistry.ACTION_RESULT,
                                WayangA2uiActionResult.class));
        assertThat(registry.surfaceDescriptors(WayangA2uiSurfaceRegistry.ACTION_RESULT))
                .extracting(WayangA2uiSurfaceDescriptor::modelType)
                .containsExactly(WayangA2uiActionFeedback.class, WayangA2uiActionResult.class);
        assertThat(new WayangA2uiSurfaceDescriptor(" custom.kind ", String.class).kind())
                .isEqualTo("custom.kind");
        assertThat(new WayangA2uiSurfaceDescriptor("custom.kind", String.class).modelTypeName())
                .isEqualTo(String.class.getName());
    }

    @Test
    @SuppressWarnings("unchecked")
    void exposesTransportFriendlySurfaceCatalog() {
        WayangA2uiSurfaceRegistry registry = WayangA2uiSurfaceRegistry.readOnly();

        WayangA2uiSurfaceCatalog catalog = registry.surfaceCatalog();
        Map<String, Object> catalogMap = catalog.toMap();
        List<Map<String, Object>> descriptorMaps = (List<Map<String, Object>>) catalogMap.get("descriptors");

        assertThat(catalog.surfaceKinds())
                .contains(WayangA2uiSurfaceRegistry.RUN_STATUS, WayangA2uiSurfaceRegistry.ACTION_RESULT)
                .doesNotHaveDuplicates();
        assertThat(catalog.descriptorCount()).isEqualTo(catalog.descriptors().size());
        assertThat(catalog.descriptors(WayangA2uiSurfaceRegistry.ACTION_RESULT))
                .extracting(WayangA2uiSurfaceDescriptor::modelType)
                .containsExactly(WayangA2uiActionFeedback.class, WayangA2uiActionResult.class);
        assertThat(catalogMap)
                .containsEntry("descriptorCount", catalog.descriptorCount())
                .containsKey("surfaceKinds")
                .containsKey("descriptors");
        assertThat((List<String>) catalogMap.get("surfaceKinds"))
                .contains(WayangA2uiSurfaceRegistry.ACTION_RESULT);
        assertThat(descriptorMaps)
                .anySatisfy(descriptor -> assertThat(descriptor)
                        .containsEntry("kind", WayangA2uiSurfaceRegistry.ACTION_RESULT)
                        .containsEntry("modelType", WayangA2uiActionResult.class.getName())
                        .containsEntry("modelSimpleName", WayangA2uiActionResult.class.getSimpleName()));
        assertThat(WayangA2uiSurfaceCatalog.from(null).surfaceKinds())
                .contains(WayangA2uiSurfaceRegistry.RUN_STATUS);
    }

    @Test
    void resolvesDescriptorsByModelInstanceAndType() {
        WayangA2uiSurfaceRegistry registry = WayangA2uiSurfaceRegistry.readOnly();
        AgentRunStatus status = RunSurfaceFixtures.runningStatus();

        assertThat(registry.descriptorOf(status))
                .contains(new WayangA2uiSurfaceDescriptor(
                        WayangA2uiSurfaceRegistry.RUN_STATUS,
                        AgentRunStatus.class));
        assertThat(registry.kindForModelType(AgentRunStatus.class))
                .contains(WayangA2uiSurfaceRegistry.RUN_STATUS);
        assertThat(registry.descriptorForModelType(WayangA2uiActionFeedback.class))
                .contains(new WayangA2uiSurfaceDescriptor(
                        WayangA2uiSurfaceRegistry.ACTION_RESULT,
                        WayangA2uiActionFeedback.class));
        assertThat(registry.supportsModelType(WayangA2uiActionResult.class)).isTrue();
        assertThat(registry.supportsModelType(String.class)).isFalse();
        assertThat(registry.surfaceDescriptorsForModelType(WayangA2uiActionResult.class))
                .containsExactly(new WayangA2uiSurfaceDescriptor(
                        WayangA2uiSurfaceRegistry.ACTION_RESULT,
                        WayangA2uiActionResult.class));
    }

    @Test
    void resolvesAssignableModelTypes() {
        WayangA2uiSurfaceRegistry registry = WayangA2uiSurfaceRegistry.builder()
                .register(
                        "custom.text",
                        CharSequence.class,
                        value -> WayangA2uiResultSurfaces.actionResult(
                                WayangA2uiActionResult.rejected("custom.text", "", value.toString()),
                                5))
                .build();

        assertThat(registry.kindOf("hello")).contains("custom.text");
        assertThat(registry.kindForModelType(String.class)).contains("custom.text");
        assertThat(registry.descriptorForModelType(String.class))
                .contains(new WayangA2uiSurfaceDescriptor("custom.text", CharSequence.class));
        assertThat(registry.surfaceDescriptorsForModelType(String.class))
                .containsExactly(new WayangA2uiSurfaceDescriptor("custom.text", CharSequence.class));
        assertThat(new WayangA2uiSurfaceDescriptor("custom.text", CharSequence.class)
                .supportsModelType(String.class)).isTrue();
    }
}
