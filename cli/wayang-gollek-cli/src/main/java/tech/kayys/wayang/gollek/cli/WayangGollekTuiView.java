package tech.kayys.wayang.gollek.cli;

import dev.tamboui.style.Color;
import dev.tamboui.toolkit.element.Element;
import tech.kayys.wayang.gollek.sdk.ComponentStatus;
import tech.kayys.wayang.gollek.sdk.WayangPlatformStatus;
import tech.kayys.wayang.gollek.sdk.WayangWorkbenchModel;

import java.util.List;
import java.nio.file.Path;
import tech.kayys.wayang.gollek.sdk.WorkspaceSnapshot;

import static dev.tamboui.toolkit.Toolkit.column;
import static dev.tamboui.toolkit.Toolkit.gauge;
import static dev.tamboui.toolkit.Toolkit.list;
import static dev.tamboui.toolkit.Toolkit.panel;
import static dev.tamboui.toolkit.Toolkit.row;
import static dev.tamboui.toolkit.Toolkit.text;

final class WayangGollekTuiView implements WayangWorkbenchRenderer<Element> {

    @Override
    public Element render(WayangWorkbenchModel model, WorkspaceSnapshot workspace) {
        WayangPlatformStatus status = model.status();

        // Left: Workspace file tree
        Element workspacePanel = panel("Workspace",
                list(workspace.importantPaths()).displayOnly())
                .rounded()
                .borderColor(Color.CYAN);

        // Center: Editor preview (first important path content)
        String editorContent = "(No file selected)";
        if (!workspace.importantPaths().isEmpty()) {
            String first = workspace.importantPaths().get(0).replaceAll("/$", "");
            Path root = Path.of(workspace.rootPath());
            Path file = root.resolve(first);
            try {
                var lines = java.nio.file.Files.readAllLines(file);
                editorContent = String.join("\n", lines.stream().limit(200).toList());
            } catch (Exception e) {
                editorContent = "Unable to read file: " + first + "\n" + e.getMessage();
            }
        }

        Element editorPanel = panel("Editor (preview)",
                text(editorContent).displayOnly())
                .rounded()
                .borderColor(Color.GREEN);

        // Right: Completions / Suggestions (use workbench command palette as placeholders)
        Element completionsPanel = panel("Completions",
                list(model.commandPalette()).displayOnly())
                .rounded()
                .borderColor(Color.YELLOW);

        // Bottom: Command palette and status
        Element palettePanel = panel("Command Palette", list(model.commandPalette()).displayOnly())
                .rounded()
                .borderColor(Color.MAGENTA);

        Element statusPanel = panel("Status",
                text("Product: " + status.productName()).dim(),
                text("Model default provider: " + (status.notes().isEmpty() ? "n/a" : status.notes().get(0))).dim())
                .rounded()
                .borderColor(Color.GRAY);

        return column(
                row(
                        workspacePanel,
                        editorPanel,
                        completionsPanel).spacing(2),
                row(palettePanel, statusPanel).spacing(2)
        ).spacing(1);
    }

    List<String> previewLines(WayangWorkbenchModel model) {
        WayangPlatformStatus status = model.status();
        return List.of(
                status.productName() + " " + status.version(),
                status.gollek().name() + ": " + status.gollek().role(),
                status.gamelan().name() + ": " + status.gamelan().role(),
                status.agentCore().name() + ": " + status.agentCore().state(),
                "commands: " + String.join(", ", model.commandPalette()));
    }

    private Color healthColor(int percent) {
        if (percent >= 85) {
            return Color.GREEN;
        }
        if (percent >= 70) {
            return Color.YELLOW;
        }
        return Color.RED;
    }
}

