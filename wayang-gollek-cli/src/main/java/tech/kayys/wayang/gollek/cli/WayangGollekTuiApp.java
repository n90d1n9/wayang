package tech.kayys.wayang.gollek.cli;

import dev.tamboui.toolkit.app.ToolkitApp;
import dev.tamboui.toolkit.element.Element;
import tech.kayys.wayang.gollek.sdk.WayangClient;
import tech.kayys.wayang.gollek.sdk.WorkbenchCommandQuery;

/**
 * Tamboui-backed terminal app for rendering the SDK-owned Wayang workbench model.
 *
 * <p>The app depends on {@link WayangClient} rather than raw SDK services so the
 * TUI follows the same facade boundary as CLI and future UI wrappers.</p>
 */
final class WayangGollekTuiApp extends ToolkitApp {

    private final WayangClient client;
    private final WayangGollekTuiView view;

    WayangGollekTuiApp(WayangClient client) {
        this.client = client;
        this.view = new WayangGollekTuiView();
    }

    @Override
    protected Element render() {
        return view.render(client.commands().workbench(WorkbenchCommandQuery.all()));
    }
}
