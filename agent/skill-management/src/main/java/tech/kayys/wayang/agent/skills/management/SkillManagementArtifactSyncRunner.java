package tech.kayys.wayang.agent.skills.management;

import java.util.Objects;

/**
 * Runs direct artifact synchronization with management event recording.
 */
final class SkillManagementArtifactSyncRunner {

    private final SkillArtifactStoreSynchronizer synchronizer;
    private final SkillManagementEventRecorder eventRecorder;

    SkillManagementArtifactSyncRunner(SkillManagementEventSink eventSink) {
        this(new SkillArtifactStoreSynchronizer(), new SkillManagementEventRecorder(eventSink));
    }

    SkillManagementArtifactSyncRunner(SkillManagementEventRecorder eventRecorder) {
        this(new SkillArtifactStoreSynchronizer(), eventRecorder);
    }

    SkillManagementArtifactSyncRunner(
            SkillArtifactStoreSynchronizer synchronizer,
            SkillManagementEventRecorder eventRecorder) {
        this.synchronizer = Objects.requireNonNull(synchronizer, "synchronizer");
        this.eventRecorder = Objects.requireNonNull(eventRecorder, "eventRecorder");
    }

    SkillArtifactStoreSyncResult sync(
            SkillArtifactStore source,
            SkillArtifactStore target,
            SkillArtifactStoreSyncOptions options) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(target, "target");
        SkillManagementOperationContext context = SkillManagementOperationContext.root();
        return eventRecorder.recordOperation(
                SkillManagementEventOperation.SYNC_ARTIFACTS,
                "",
                context,
                () -> synchronizer.sync(source, target, options),
                SkillManagementEventAttributes::artifactSync);
    }
}
