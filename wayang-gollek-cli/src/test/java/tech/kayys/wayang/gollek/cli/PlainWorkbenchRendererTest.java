package tech.kayys.wayang.gollek.cli;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.gollek.sdk.WayangGollekSdk;

import static org.assertj.core.api.Assertions.assertThat;

class PlainWorkbenchRendererTest {

    @Test
    void rendersSameWorkbenchModelWithoutTamboui() {
        String text = new PlainWorkbenchRenderer().render(WayangGollekSdk.local().workbench());

        assertThat(text)
                .contains("Wayang Workbench")
                .contains("Gollek")
                .contains("Gamelan")
                .contains("Command Palette")
                .contains("Product Surfaces")
                .contains("coding-agent");
    }
}
