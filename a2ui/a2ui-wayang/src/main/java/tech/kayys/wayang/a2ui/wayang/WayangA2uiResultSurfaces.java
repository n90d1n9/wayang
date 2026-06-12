package tech.kayys.wayang.a2ui.wayang;

import tech.kayys.wayang.a2ui.core.A2uiComponent;
import tech.kayys.wayang.a2ui.core.A2uiComponents;
import tech.kayys.wayang.a2ui.core.A2uiDataEntry;
import tech.kayys.wayang.a2ui.core.A2uiServerMessage;
import tech.kayys.wayang.a2ui.wayang.surface.SurfaceIds;
import tech.kayys.wayang.a2ui.wayang.surface.SurfaceLayouts;
import tech.kayys.wayang.a2ui.wayang.surface.SurfaceMessages;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Renders action-routing outcomes that do not already have domain surfaces.
 */
public final class WayangA2uiResultSurfaces {

    private WayangA2uiResultSurfaces() {
    }

    public static List<A2uiServerMessage> actionResult(WayangA2uiActionResult result, int sequence) {
        WayangA2uiActionResult resolved = Objects.requireNonNull(result, "result");
        String surfaceId = SurfaceIds.actionResult(sequence, resolved.actionName());
        String rootId = surfaceId + "-root";
        String titleId = surfaceId + "-title";
        String actionId = surfaceId + "-action";
        String runId = surfaceId + "-run";
        String messageId = surfaceId + "-message";

        List<String> children = SurfaceLayouts.children(titleId, actionId);
        if (!resolved.runId().isBlank()) {
            children.add(runId);
        }
        children.add(messageId);

        List<A2uiComponent> components = SurfaceLayouts.components();
        SurfaceLayouts.addRootColumn(components, rootId, children);
        components.add(A2uiComponents.text(titleId, resolved.handled() ? "Action handled" : "Action rejected"));
        components.add(A2uiComponents.text(actionId, "Action: " + valueOrUnknown(resolved.actionName())));
        if (!resolved.runId().isBlank()) {
            components.add(A2uiComponents.text(runId, "Run: " + resolved.runId()));
        }
        components.add(A2uiComponents.text(messageId, resolved.message()));

        return SurfaceMessages.standard(
                surfaceId,
                rootId,
                components,
                A2uiDataEntry.string("actionName", resolved.actionName()),
                A2uiDataEntry.string("runId", resolved.runId()),
                A2uiDataEntry.bool("handled", resolved.handled()),
                A2uiDataEntry.string("message", resolved.message()),
                A2uiDataEntry.map("metadata", metadataEntries(resolved.metadata())));
    }

    private static List<A2uiDataEntry> metadataEntries(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return List.of();
        }
        List<A2uiDataEntry> entries = new ArrayList<>();
        metadata.forEach((key, value) -> {
            if (key == null) {
                return;
            }
            String normalizedKey = key.trim();
            if (normalizedKey.isBlank()) {
                return;
            }
            if (value instanceof Boolean bool) {
                entries.add(A2uiDataEntry.bool(normalizedKey, bool));
            } else if (value instanceof Number number) {
                entries.add(A2uiDataEntry.number(normalizedKey, number));
            } else {
                entries.add(A2uiDataEntry.string(normalizedKey, value == null ? "" : String.valueOf(value)));
            }
        });
        return List.copyOf(entries);
    }

    private static String valueOrUnknown(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }
}
