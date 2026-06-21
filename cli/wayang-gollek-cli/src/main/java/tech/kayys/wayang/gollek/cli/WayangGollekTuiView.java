package tech.kayys.wayang.gollek.cli;

import tech.kayys.wayang.gollek.sdk.WayangWorkbenchModel;
import tech.kayys.wayang.gollek.sdk.WorkspaceSnapshot;
import tech.kayys.wayang.tui.Component;
import tech.kayys.wayang.tui.Container;
import tech.kayys.wayang.tui.Border;
import tech.kayys.wayang.tui.TextView;

final class WayangGollekTuiView implements WayangWorkbenchRenderer<Component> {

    @Override
    public Component render(WayangWorkbenchModel model, WorkspaceSnapshot workspace) {
        Container root = new Container();
        
        StringBuilder wsText = new StringBuilder();
        if (workspace != null) {
            for (String path : workspace.importantPaths()) {
                wsText.append(path).append("\n");
            }
        }
        
        Component wsPanel = new Border(new TextView(wsText.toString()), "Workspace");
        Component editorPanel = new Border(new TextView("(empty editor)"), "Editor");
        
        root.add(wsPanel);
        root.add(editorPanel);
        
        return root;
    }
}
