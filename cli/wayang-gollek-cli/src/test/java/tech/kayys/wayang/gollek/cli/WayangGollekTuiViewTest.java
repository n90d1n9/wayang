package tech.kayys.wayang.gollek.cli;

import dev.tamboui.toolkit.element.Element;
import org.junit.jupiter.api.Test;
import tech.kayys.wayang.gollek.sdk.WayangGollekSdk;
import tech.kayys.wayang.gollek.sdk.WayangWorkbenchModel;

import static org.assertj.core.api.Assertions.assertThat;

class WayangGollekTuiViewTest {

    @Test
    void buildsTambouiElementTreeFromWorkbenchModel() {
        var sdk = WayangGollekSdk.local();
        WayangWorkbenchModel model = sdk.workbench();
        var workspace = sdk.inspectWorkspace(new tech.kayys.wayang.gollek.sdk.WorkspaceInspectionRequest(".", 80, false));
        WayangGollekTuiView view = new WayangGollekTuiView();

        Element element = view.render(model, workspace);

        assertThat(element).isNotNull();
        assertThat(view.previewLines(model))
                .contains("Wayang 1.0.0-SNAPSHOT")
                .anySatisfy(line -> assertThat(line).contains("Gollek"))
                .anySatisfy(line -> assertThat(line).contains("workbench"));
    }
}
