package tech.kayys.wayang.gollek.cli;

import dev.tamboui.style.Color;
import dev.tamboui.toolkit.element.Element;
import tech.kayys.wayang.gollek.sdk.ComponentStatus;
import tech.kayys.wayang.gollek.sdk.WayangPlatformStatus;
import tech.kayys.wayang.gollek.sdk.WayangWorkbenchModel;

import java.util.List;

import static dev.tamboui.toolkit.Toolkit.column;
import static dev.tamboui.toolkit.Toolkit.gauge;
import static dev.tamboui.toolkit.Toolkit.list;
import static dev.tamboui.toolkit.Toolkit.panel;
import static dev.tamboui.toolkit.Toolkit.row;
import static dev.tamboui.toolkit.Toolkit.text;

final class WayangGollekTuiView implements WayangWorkbenchRenderer<Element> {

    @Override
    public Element render(WayangWorkbenchModel model) {
        WayangPlatformStatus status = model.status();
        return column(
                panel("Wayang",
                        text(status.productName()).bold().cyan(),
                        text("Agentic platform above Gollek inference/training and Gamelan workflows").dim(),
                        text("Press q to exit").gray())
                        .rounded()
                        .borderColor(Color.CYAN),
                row(
                        componentPanel(status.gollek()),
                        componentPanel(status.gamelan()),
                        componentPanel(status.agentCore()))
                        .spacing(2),
                row(
                        componentPanel(status.rag()),
                        componentPanel(status.mcp()),
                        panel("Skills",
                                text("Active skills: " + status.activeSkills()).bold(),
                                gauge(status.activeSkills() > 0 ? 1.0 : 0.15)
                                        .label(status.activeSkills() + " registered")
                                        .gaugeColor(status.activeSkills() > 0 ? Color.GREEN : Color.YELLOW))
                                .rounded()
                                .borderColor(Color.YELLOW))
                        .spacing(2),
                panel("Product Surfaces",
                        list(model.productSurfaces().stream()
                                .map(surface -> surface.name() + " - " + surface.role())
                                .toList())
                                .displayOnly())
                        .rounded()
                        .borderColor(Color.CYAN),
                row(
                        panel("Command Palette", list(model.commandPalette()).displayOnly())
                                .rounded()
                                .borderColor(Color.GREEN),
                        panel("Next Actions", list(model.nextActions()).displayOnly())
                                .rounded()
                                .borderColor(Color.YELLOW))
                        .spacing(2),
                panel("Platform Notes", list(status.notes()).displayOnly())
                        .rounded()
                        .borderColor(Color.GRAY))
                .spacing(1);
    }

    private Element componentPanel(ComponentStatus component) {
        return panel(component.name(),
                text(component.role()).dim(),
                text("state: " + component.state()).bold(),
                text(component.endpoint()),
                gauge(component.healthPercent() / 100.0)
                        .label(component.healthPercent() + "% ready")
                        .gaugeColor(healthColor(component.healthPercent())))
                .rounded()
                .borderColor(healthColor(component.healthPercent()));
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
