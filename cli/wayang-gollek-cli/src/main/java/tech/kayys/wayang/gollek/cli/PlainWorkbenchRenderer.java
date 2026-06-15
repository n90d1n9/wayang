package tech.kayys.wayang.gollek.cli;

import tech.kayys.wayang.gollek.sdk.ComponentStatus;
import tech.kayys.wayang.gollek.sdk.ProductSurface;
import tech.kayys.wayang.gollek.sdk.WayangPlatformStatus;
import tech.kayys.wayang.gollek.sdk.WayangWorkbenchModel;
import tech.kayys.wayang.gollek.sdk.WorkspaceSnapshot;

final class PlainWorkbenchRenderer implements WayangWorkbenchRenderer<String> {

    @Override
    public String render(WayangWorkbenchModel model, WorkspaceSnapshot workspace) {
        WayangPlatformStatus status = model.status();
        StringBuilder out = new StringBuilder();
        out.append("Wayang Workbench\n");
        out.append("Version: ").append(status.version()).append('\n');
        out.append("Boundary: Wayang agent platform above Gollek inference/training and Gamelan workflows\n\n");

        out.append("Components\n");
        appendComponent(out, status.gollek());
        appendComponent(out, status.gamelan());
        appendComponent(out, status.agentCore());
        appendComponent(out, status.rag());
        appendComponent(out, status.mcp());
        out.append("  Skills: ").append(status.activeSkills()).append(" active\n\n");

        out.append("Product Surfaces\n");
        for (ProductSurface surface : model.productSurfaces()) {
            out.append("  - ")
                    .append(surface.name())
                    .append(" [")
                    .append(surface.id())
                    .append("]: ")
                    .append(surface.role())
                    .append('\n');
        }
        out.append('\n');

        appendSection(out, "Command Palette", model.commandPalette());
        appendSection(out, "Next Actions", model.nextActions());
        appendSection(out, "Platform Notes", status.notes());
        return out.toString();
    }

    private void appendComponent(StringBuilder out, ComponentStatus component) {
        out.append("  - ")
                .append(component.name())
                .append(": ")
                .append(component.state())
                .append(" (")
                .append(component.healthPercent())
                .append("%) - ")
                .append(component.role())
                .append('\n');
    }

    private void appendSection(StringBuilder out, String title, Iterable<String> lines) {
        out.append(title).append('\n');
        for (String line : lines) {
            out.append("  - ").append(line).append('\n');
        }
        out.append('\n');
    }
}
