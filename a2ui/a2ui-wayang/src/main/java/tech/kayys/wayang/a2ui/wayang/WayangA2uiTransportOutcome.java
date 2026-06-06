package tech.kayys.wayang.a2ui.wayang;

/**
 * Transport-neutral classification for an A2UI bridge response.
 */
public enum WayangA2uiTransportOutcome {
    SUCCESS,
    PARTIAL_SUCCESS,
    ACTION_REJECTED,
    EMPTY,
    TRANSPORT_ERROR
}
