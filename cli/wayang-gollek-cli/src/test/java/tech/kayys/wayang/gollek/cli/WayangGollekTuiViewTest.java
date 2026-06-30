package tech.kayys.wayang.gollek.cli;

// import tech.kayys.wayang.tui.Component; // removed: class no longer exists
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import tech.kayys.wayang.gollek.sdk.WayangGollekSdk;
import tech.kayys.wayang.gollek.sdk.WayangWorkbenchModel;

import static org.assertj.core.api.Assertions.assertThat;

@Disabled("WayangGollekTuiView and Component have been removed; test needs migration")
class WayangGollekTuiViewTest {

    @Test
    void buildsElementTreeFromWorkbenchModel() {
        var sdk = WayangGollekSdk.local();
        WayangWorkbenchModel model = sdk.workbench();
        var workspace = sdk.inspectWorkspace(new tech.kayys.wayang.gollek.sdk.WorkspaceInspectionRequest(".", 80, false));
        // WayangGollekTuiView view = new WayangGollekTuiView();
        // Component element = view.render(model, workspace);
        // assertThat(element).isNotNull();
    }
}
