package tech.kayys.wayang.a2ui.wayang.surface;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2ui.core.A2uiComponent;
import tech.kayys.wayang.a2ui.core.A2uiComponents;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SurfaceLayoutsTest {

    @Test
    void createsMutableChildrenAndComponentLists() {
        List<A2uiComponent> components = SurfaceLayouts.components();
        List<String> children = SurfaceLayouts.children("title", "message");

        children.add("action");
        components.add(A2uiComponents.text("title", "Title"));

        assertThat(children).containsExactly("title", "message", "action");
        assertThat(components).extracting(A2uiComponent::id).containsExactly("title");
    }

    @Test
    void insertsRootColumnAtRequestedPosition() {
        List<A2uiComponent> prepend = SurfaceLayouts.components();
        List<A2uiComponent> append = SurfaceLayouts.components();
        List<String> children = SurfaceLayouts.children("title");

        prepend.add(A2uiComponents.text("title", "Title"));
        SurfaceLayouts.prependRootColumn(prepend, "root", children);
        SurfaceLayouts.addRootColumn(append, "root", children);
        append.add(A2uiComponents.text("title", "Title"));

        assertThat(prepend).extracting(A2uiComponent::id).containsExactly("root", "title");
        assertThat(append).extracting(A2uiComponent::id).containsExactly("root", "title");
        assertThat(columnChildren(prepend.get(0))).containsEntry("explicitList", List.of("title"));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> columnChildren(A2uiComponent component) {
        Map<String, Object> column = (Map<String, Object>) component.component().get("Column");
        return (Map<String, Object>) column.get("children");
    }
}
