package tech.kayys.wayang.gollek.cli;

import tech.kayys.wayang.tui.Component;
import org.junit.jupiter.api.Test;
import tech.kayys.wayang.gollek.sdk.WayangGollekSdk;
import tech.kayys.wayang.gollek.sdk.WayangWorkbenchModel;

import static org.assertj.core.api.Assertions.assertThat;

class WayangGollekTuiViewTest {

    @Test
    void buildsElementTreeFromWorkbenchModel() {
        var sdk = WayangGollekSdk.local();
        WayangWorkbenchModel model = sdk.workbench();
        var workspace = sdk.inspectWorkspace(new tech.kayys.wayang.gollek.sdk.WorkspaceInspectionRequest(".", 80, false));
        WayangGollekTuiView view = new WayangGollekTuiView();

        Component element = view.render(model, workspace);

        assertThat(element).isNotNull();
        String output = element.render(80, 24).stream()
                .map(org.jline.utils.AttributedString::toAnsi)
                .collect(java.util.stream.Collectors.joining("\n"));
        assertThat(output).contains("Workspace").contains("Editor");
    }
}
