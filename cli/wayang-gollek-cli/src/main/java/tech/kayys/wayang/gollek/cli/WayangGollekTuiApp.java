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
        var model = client.commands().workbench(WorkbenchCommandQuery.all());
        var workspace = client.contexts().workspace(".", 200, false);
        return view.render(model, workspace);
    }
}
