package tech.kayys.wayang.a2ui.wayang;

import tech.kayys.wayang.a2ui.core.A2uiBeginRendering;
import tech.kayys.wayang.a2ui.core.A2uiComponent;
import tech.kayys.wayang.a2ui.core.A2uiComponents;
import tech.kayys.wayang.a2ui.core.A2uiDataEntry;
import tech.kayys.wayang.a2ui.core.A2uiDataModelUpdate;
import tech.kayys.wayang.a2ui.core.A2uiServerMessage;
import tech.kayys.wayang.a2ui.core.A2uiSurfaceUpdate;

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
        String surfaceId = "wayang-action-result-" + sequence + "-" + safeId(resolved.actionName(), "action");
        String rootId = surfaceId + "-root";
        String titleId = surfaceId + "-title";
        String actionId = surfaceId + "-action";
        String runId = surfaceId + "-run";
        String messageId = surfaceId + "-message";

        List<String> children = new ArrayList<>();
        children.add(titleId);
        children.add(actionId);
        if (!resolved.runId().isBlank()) {
            children.add(runId);
        }
        children.add(messageId);

        List<A2uiComponent> components = new ArrayList<>();
        components.add(A2uiComponents.column(rootId, children));
        components.add(A2uiComponents.text(titleId, resolved.handled() ? "Action handled" : "Action rejected"));
        components.add(A2uiComponents.text(actionId, "Action: " + valueOrUnknown(resolved.actionName())));
        if (!resolved.runId().isBlank()) {
            components.add(A2uiComponents.text(runId, "Run: " + resolved.runId()));
        }
        components.add(A2uiComponents.text(messageId, resolved.message()));

        return List.of(
                A2uiDataModelUpdate.root(
                        surfaceId,
                        A2uiDataEntry.string("actionName", resolved.actionName()),
                        A2uiDataEntry.string("runId", resolved.runId()),
                        A2uiDataEntry.bool("handled", resolved.handled()),
                        A2uiDataEntry.string("message", resolved.message()),
                        A2uiDataEntry.map("metadata", metadataEntries(resolved.metadata()))),
                new A2uiSurfaceUpdate(surfaceId, components),
                A2uiBeginRendering.standard(surfaceId, rootId));
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

    private static String safeId(String value, String fallback) {
        String normalized = value == null ? "" : value.trim().toLowerCase(java.util.Locale.ROOT);
        if (normalized.isBlank()) {
            return fallback;
        }
        String safe = normalized.replaceAll("[^a-z0-9_-]+", "-");
        return safe.isBlank() ? fallback : safe;
    }

    private static String valueOrUnknown(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }
}
