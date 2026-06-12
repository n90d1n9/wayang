package tech.kayys.wayang.a2ui.wayang.surface;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2ui.core.A2uiAction;
import tech.kayys.wayang.a2ui.core.A2uiActionContextEntry;
import tech.kayys.wayang.a2ui.core.A2uiComponent;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiActions;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiSurfaceOptions;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SurfaceActionsTest {

    @Test
    void defaultsNullOptionsToReadOnlyPolicy() {
        WayangA2uiSurfaceOptions options = SurfaceActions.options(null);

        assertThat(options).isEqualTo(WayangA2uiSurfaceOptions.readOnly());
        assertThat(options.shows(WayangA2uiActions.RUN_INSPECT)).isTrue();
        assertThat(options.shows(WayangA2uiActions.RUN_WAIT)).isFalse();
    }

    @Test
    void buildsActionWithRunSurfacePolicyAndExtraContext() {
        Map<String, String> context = new LinkedHashMap<>();
        context.put("tenantId", "tenant-a");
        WayangA2uiSurfaceOptions options = new WayangA2uiSurfaceOptions(
                Set.of(WayangA2uiActions.RUN_EVENTS),
                context);

        A2uiAction action = SurfaceActions.action(
                WayangA2uiActions.RUN_EVENTS,
                options,
                "run-1",
                "surface-1",
                A2uiActionContextEntry.literalNumber("limit", 25));

        assertThat(action.name()).isEqualTo(WayangA2uiActions.RUN_EVENTS);
        assertThat(action.context())
                .extracting(A2uiActionContextEntry::key)
                .containsExactly("runId", "surfaceId", "tenantId", "limit");
        assertThat(action.context().get(0).value().value()).containsEntry("literalString", "run-1");
        assertThat(action.context().get(1).value().value()).containsEntry("literalString", "surface-1");
        assertThat(action.context().get(2).value().value()).containsEntry("literalString", "tenant-a");
        assertThat(action.context().get(3).value().value()).containsEntry("literalNumber", 25);
    }

    @Test
    @SuppressWarnings("unchecked")
    void appendsLabelAndButtonComponentsInOrder() {
        List<A2uiComponent> components = new ArrayList<>();
        List<String> children = new ArrayList<>();

        SurfaceActions.addButton(
                components,
                children,
                "button-1",
                "label-1",
                "Inspect",
                WayangA2uiActions.RUN_INSPECT,
                null,
                "run-1",
                "surface-1");

        assertThat(children).containsExactly("button-1");
        assertThat(components)
                .extracting(A2uiComponent::id)
                .containsExactly("label-1", "button-1");

        Map<String, Object> label = (Map<String, Object>) components.get(0).component().get("Text");
        Map<String, Object> button = (Map<String, Object>) components.get(1).component().get("Button");
        Map<String, Object> action = (Map<String, Object>) button.get("action");

        assertThat(label).containsKey("text");
        assertThat(button).containsEntry("child", "label-1");
        assertThat(action).containsEntry("name", WayangA2uiActions.RUN_INSPECT);
    }
}
