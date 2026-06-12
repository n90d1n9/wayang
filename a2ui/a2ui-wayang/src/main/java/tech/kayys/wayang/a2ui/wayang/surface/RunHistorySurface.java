package tech.kayys.wayang.a2ui.wayang.surface;

import tech.kayys.wayang.a2ui.core.A2uiComponent;
import tech.kayys.wayang.a2ui.core.A2uiComponents;
import tech.kayys.wayang.a2ui.core.A2uiServerMessage;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiSurfaceOptions;
import tech.kayys.wayang.gollek.sdk.AgentRunHistory;
import tech.kayys.wayang.gollek.sdk.AgentRunStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Renders Wayang run history pages as A2UI surfaces.
 */
public final class RunHistorySurface {

    private RunHistorySurface() {
    }

    public static List<A2uiServerMessage> render(AgentRunHistory history, WayangA2uiSurfaceOptions options) {
        AgentRunHistory resolved = Objects.requireNonNull(history, "history");
        WayangA2uiSurfaceOptions resolvedOptions = SurfaceActions.options(options);
        String surfaceId = SurfaceIds.runHistory();
        String rootId = surfaceId + "-root";
        String titleId = surfaceId + "-title";
        String summaryId = surfaceId + "-summary";
        String messageId = surfaceId + "-message";

        List<A2uiComponent> components = SurfaceLayouts.components();
        List<String> rootChildren = SurfaceLayouts.children(titleId, summaryId, messageId);

        components.add(A2uiComponents.text(titleId, "Wayang runs"));
        components.add(A2uiComponents.text(summaryId, SurfaceText.historySummary(resolved)));
        components.add(A2uiComponents.text(messageId, SurfaceText.historyMessage(resolved)));

        for (AgentRunStatus status : resolved.runs()) {
            String prefix = SurfaceIds.runHistoryRow(status.handle().runId());
            String rowId = prefix + "-row";
            String textId = prefix + "-text";
            List<String> rowChildren = new ArrayList<>();
            rootChildren.add(rowId);
            rowChildren.add(textId);
            components.add(A2uiComponents.text(textId, SurfaceText.runLine(status)));
            RunActionControls.addHistoryRowActions(
                    components,
                    rowChildren,
                    prefix,
                    status,
                    surfaceId,
                    resolvedOptions);
            components.add(A2uiComponents.row(rowId, rowChildren));
        }
        SurfaceLayouts.prependRootColumn(components, rootId, rootChildren);

        return SurfaceMessages.standard(
                surfaceId,
                rootId,
                components,
                RunDataModels.history(resolved));
    }
}
