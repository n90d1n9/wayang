package tech.kayys.wayang.a2a.wayang;

import tech.kayys.wayang.a2a.core.A2aArtifact;
import tech.kayys.wayang.a2a.core.A2aMessage;
import tech.kayys.wayang.a2a.core.A2aTask;
import tech.kayys.wayang.a2a.core.A2aTaskStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * Immutable task snapshot mutation helpers shared by task-store adapters.
 */
final class WayangA2aTaskSnapshots {

    private WayangA2aTaskSnapshots() {
    }

    static A2aTask withStatus(A2aTask task, A2aTaskStatus status) {
        A2aTask resolved = requireTask(task);
        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }
        return new A2aTask(
                resolved.id(),
                resolved.contextId(),
                status,
                resolved.artifacts(),
                resolved.history(),
                resolved.metadata());
    }

    static A2aTask withAppendedMessage(A2aTask task, A2aMessage message) {
        A2aTask resolved = requireTask(task);
        if (message == null) {
            throw new IllegalArgumentException("message must not be null");
        }
        List<A2aMessage> history = new ArrayList<>(resolved.history());
        history.add(message);
        return new A2aTask(
                resolved.id(),
                resolved.contextId(),
                resolved.status(),
                resolved.artifacts(),
                history,
                resolved.metadata());
    }

    static A2aTask withAppendedArtifact(A2aTask task, A2aArtifact artifact) {
        A2aTask resolved = requireTask(task);
        if (artifact == null) {
            throw new IllegalArgumentException("artifact must not be null");
        }
        List<A2aArtifact> artifacts = new ArrayList<>(resolved.artifacts());
        artifacts.add(artifact);
        return new A2aTask(
                resolved.id(),
                resolved.contextId(),
                resolved.status(),
                artifacts,
                resolved.history(),
                resolved.metadata());
    }

    private static A2aTask requireTask(A2aTask task) {
        if (task == null) {
            throw new IllegalArgumentException("task must not be null");
        }
        return task;
    }
}
