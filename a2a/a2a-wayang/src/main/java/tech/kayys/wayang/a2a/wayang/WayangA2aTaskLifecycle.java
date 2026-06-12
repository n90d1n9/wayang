package tech.kayys.wayang.a2a.wayang;

import tech.kayys.wayang.a2a.core.A2aTask;
import tech.kayys.wayang.a2a.core.A2aTaskState;

/**
 * Shared task lifecycle invariants for local task stores.
 */
final class WayangA2aTaskLifecycle {

    private WayangA2aTaskLifecycle() {
    }

    static void requireMutable(A2aTask task, String action) {
        A2aTask resolved = requireTask(task);
        if (resolved.status().state().terminal()) {
            throw new WayangA2aTaskLifecycleException("A2A task " + resolved.id()
                    + " is terminal (" + resolved.status().state().value() + ") and cannot " + action + ".");
        }
    }

    static void requireTransition(A2aTask task, A2aTaskState target) {
        A2aTask resolved = requireTask(task);
        if (target == null || target == A2aTaskState.TASK_STATE_UNSPECIFIED) {
            throw new WayangA2aTaskLifecycleException("A2A task " + resolved.id()
                    + " cannot transition to TASK_STATE_UNSPECIFIED.");
        }
        requireMutable(resolved, "transition to " + target.value());
    }

    private static A2aTask requireTask(A2aTask task) {
        if (task == null) {
            throw new IllegalArgumentException("task must not be null");
        }
        return task;
    }
}
