package tech.kayys.wayang.a2ui.wayang;

import java.util.Objects;

/**
 * Sequence-aware action feedback model for session-level A2UI result surfaces.
 */
public record WayangA2uiActionFeedback(
        WayangA2uiActionResult result,
        int sequence) {

    public WayangA2uiActionFeedback {
        result = Objects.requireNonNull(result, "result");
        sequence = Math.max(1, sequence);
    }

    public static WayangA2uiActionFeedback of(WayangA2uiActionResult result, int sequence) {
        return new WayangA2uiActionFeedback(result, sequence);
    }
}
