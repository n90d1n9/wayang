package tech.kayys.wayang.a2ui.wayang.surface;

import tech.kayys.wayang.a2ui.core.A2uiComponent;
import tech.kayys.wayang.a2ui.core.A2uiComponents;
import tech.kayys.wayang.a2ui.core.A2uiServerMessage;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiSurfaceOptions;
import tech.kayys.wayang.gollek.sdk.AgentRunStatus;

import java.util.List;
import java.util.Objects;

/**
 * Renders a single Wayang run status surface.
 */
public final class RunStatusSurface {

    private RunStatusSurface() {
    }

    public static List<A2uiServerMessage> render(AgentRunStatus status, WayangA2uiSurfaceOptions options) {
        AgentRunStatus resolved = Objects.requireNonNull(status, "status");
        WayangA2uiSurfaceOptions resolvedOptions = SurfaceActions.options(options);
        String surfaceId = SurfaceIds.runStatus(resolved.handle().runId());
        String rootId = surfaceId + "-root";
        String titleId = surfaceId + "-title";
        String stateId = surfaceId + "-state";
        String messageId = surfaceId + "-message";

        List<A2uiComponent> components = SurfaceLayouts.components();
        List<String> rootChildren = SurfaceLayouts.children(titleId, stateId, messageId);

        components.add(A2uiComponents.text(titleId, "Run " + resolved.handle().runId()));
        components.add(A2uiComponents.text(stateId, "State: " + resolved.handle().state().name()));
        components.add(A2uiComponents.text(messageId, resolved.message()));

        RunActionControls.addStatusActions(
                components,
                rootChildren,
                surfaceId,
                resolved,
                surfaceId,
                resolvedOptions);
        SurfaceLayouts.prependRootColumn(components, rootId, rootChildren);

        return SurfaceMessages.standard(
                surfaceId,
                rootId,
                components,
                RunDataModels.status(resolved));
    }
}
