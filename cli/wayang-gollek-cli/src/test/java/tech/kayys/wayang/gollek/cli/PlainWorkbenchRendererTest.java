package tech.kayys.wayang.gollek.cli;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.gollek.sdk.WayangGollekSdk;

import static org.assertj.core.api.Assertions.assertThat;

class PlainWorkbenchRendererTest {

    @Test
    void rendersSameWorkbenchModelWithoutTamboui() {
        var sdk = WayangGollekSdk.local();
        var workspace = sdk.inspectWorkspace(new tech.kayys.wayang.gollek.sdk.WorkspaceInspectionRequest(".", 80, false));
        String text = new PlainWorkbenchRenderer().render(WayangGollekSdk.local().workbench(), workspace);

        assertThat(text)
                .contains("Wayang Workbench")
                .contains("Gollek")
                .contains("Gamelan")
                .contains("Command Palette")
                .contains("Product Surfaces")
                .contains("coding-agent");
    }
}
