package tech.kayys.wayang.a2ui.wayang.surface;

import tech.kayys.wayang.a2ui.core.A2uiActionContextEntry;
import tech.kayys.wayang.a2ui.core.A2uiComponent;
import tech.kayys.wayang.a2ui.core.A2uiComponents;
import tech.kayys.wayang.a2ui.core.A2uiServerMessage;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiActions;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiSurfaceOptions;
import tech.kayys.wayang.gollek.sdk.AgentRunEvent;
import tech.kayys.wayang.gollek.sdk.AgentRunEvents;

import java.util.List;
import java.util.Objects;

/**
 * Renders Wayang run event streams as A2UI surfaces.
 */
public final class RunEventsSurface {

    private RunEventsSurface() {
    }

    public static List<A2uiServerMessage> render(AgentRunEvents events, WayangA2uiSurfaceOptions options) {
        AgentRunEvents resolved = Objects.requireNonNull(events, "events");
        WayangA2uiSurfaceOptions resolvedOptions = SurfaceActions.options(options);
        String surfaceId = SurfaceIds.runEvents(resolved.runId());
        String rootId = surfaceId + "-root";
        String titleId = surfaceId + "-title";
        String summaryId = surfaceId + "-summary";
        String messageId = surfaceId + "-message";
        String refreshLabelId = surfaceId + "-refresh-label";
        String refreshButtonId = surfaceId + "-refresh";

        List<A2uiComponent> components = SurfaceLayouts.components();
        List<String> rootChildren = SurfaceLayouts.children(titleId, summaryId, messageId);

        components.add(A2uiComponents.text(titleId, "Run events " + resolved.runId()));
        components.add(A2uiComponents.text(summaryId, SurfaceText.eventsSummary(resolved)));
        components.add(A2uiComponents.text(messageId, SurfaceText.eventsMessage(resolved)));

        for (AgentRunEvent event : resolved.events()) {
            String eventId = surfaceId + "-event-" + event.sequence();
            rootChildren.add(eventId);
            components.add(A2uiComponents.text(eventId, SurfaceText.eventLine(event)));
        }

        if (resolvedOptions.shows(WayangA2uiActions.RUN_EVENTS)) {
            rootChildren.add(refreshButtonId);
            components.add(A2uiComponents.text(refreshLabelId, "Refresh events"));
            components.add(A2uiComponents.button(
                    refreshButtonId,
                    refreshLabelId,
                    SurfaceActions.action(
                            WayangA2uiActions.RUN_EVENTS,
                            resolvedOptions,
                            resolved.runId(),
                            surfaceId,
                            A2uiActionContextEntry.literalNumber("afterSequence", resolved.nextAfterSequence()),
                            A2uiActionContextEntry.literalNumber("limit", resolved.query().limit()))));
        }
        SurfaceLayouts.prependRootColumn(components, rootId, rootChildren);

        return SurfaceMessages.standard(
                surfaceId,
                rootId,
                components,
                RunDataModels.events(resolved));
    }
}
