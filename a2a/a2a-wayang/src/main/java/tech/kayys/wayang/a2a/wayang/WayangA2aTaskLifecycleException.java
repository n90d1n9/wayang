package tech.kayys.wayang.a2a.wayang;

/**
 * Raised when a task lifecycle mutation violates A2A state invariants.
 */
final class WayangA2aTaskLifecycleException extends IllegalArgumentException {

    WayangA2aTaskLifecycleException(String message) {
        super(WayangA2aMaps.required(message, "message"));
    }
}
