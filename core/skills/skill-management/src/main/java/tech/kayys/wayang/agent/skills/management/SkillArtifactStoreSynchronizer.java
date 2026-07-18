package tech.kayys.wayang.agent.skills.management;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Synchronizes dynamic skill artifacts between persistence backends.
 */
public final class SkillArtifactStoreSynchronizer {

    private static final SkillArtifactQuery SYNC_QUERY = new SkillArtifactQuery(
            "",
            null,
            "",
            "",
            SkillArtifactQuery.MAX_LIMIT);
    private static final SkillStoreSyncSupport.SkillStoreSyncDetails DETAILS =
            new SkillStoreSyncSupport.SkillStoreSyncDetails(
                    "Artifact missing from target",
                    "Artifact already matches target",
                    "Target artifact replaced from source",
                    "Target artifact differs and overwrite is disabled",
                    "Target artifact missing from source");

    public SkillArtifactStoreSyncResult sync(
            SkillArtifactStore source,
            SkillArtifactStore target,
            SkillArtifactStoreSyncOptions options) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(target, "target");
        SkillArtifactStoreSyncOptions resolvedOptions =
                options == null ? SkillArtifactStoreSyncOptions.bootstrap() : options;
        Map<SkillArtifactReference, SkillArtifact> sourceArtifacts = artifacts(source);
        Map<SkillArtifactReference, SkillArtifact> targetArtifacts = artifacts(target);
        List<SkillArtifactStoreSyncChange> changes = SkillStoreSyncSupport.sync(
                sourceArtifacts,
                targetArtifacts,
                toPolicy(resolvedOptions),
                DETAILS,
                new ArtifactSyncOperations(target));

        return new SkillArtifactStoreSyncResult(resolvedOptions.dryRun(), changes);
    }

    private Map<SkillArtifactReference, SkillArtifact> artifacts(SkillArtifactStore store) {
        Map<SkillArtifactReference, SkillArtifact> artifacts = new LinkedHashMap<>();
        store.listArtifacts(SYNC_QUERY).stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(SkillArtifactReference::qualifiedName))
                .forEach(reference -> store.getArtifact(reference)
                        .ifPresent(artifact -> artifacts.put(reference, artifact)));
        return artifacts;
    }

    private boolean equivalent(SkillArtifact source, SkillArtifact target) {
        return source.contentType().equals(target.contentType())
                && source.metadata().equals(target.metadata())
                && source.sizeBytes() == target.sizeBytes()
                && source.sha256().equals(target.sha256());
    }

    private static SkillStoreSyncPolicy toPolicy(SkillArtifactStoreSyncOptions options) {
        return SkillStoreSyncPolicy.of(
                options.overwriteExisting(),
                options.deleteMissingFromTarget(),
                options.dryRun());
    }

    private final class ArtifactSyncOperations implements
            SkillStoreSyncSupport.SkillStoreSyncOperations<
                    SkillArtifactReference, SkillArtifact, SkillArtifactStoreSyncChange> {

        private final SkillArtifactStore target;

        private ArtifactSyncOperations(SkillArtifactStore target) {
            this.target = target;
        }

        @Override
        public boolean equivalent(SkillArtifact sourceValue, SkillArtifact targetValue) {
            return SkillArtifactStoreSynchronizer.this.equivalent(sourceValue, targetValue);
        }

        @Override
        public void copy(SkillArtifact sourceValue) {
            target.putArtifact(sourceValue);
        }

        @Override
        public void delete(SkillArtifactReference key) {
            target.deleteArtifact(key);
        }

        @Override
        public SkillArtifactStoreSyncChange change(
                SkillArtifactReference key,
                SkillStoreSyncSupport.SkillStoreSyncActionType action,
                String detail) {
            return new SkillArtifactStoreSyncChange(key, artifactAction(action), detail);
        }

        private SkillArtifactStoreSyncAction artifactAction(
                SkillStoreSyncSupport.SkillStoreSyncActionType action) {
            return switch (action) {
                case COPIED -> SkillArtifactStoreSyncAction.COPIED;
                case UPDATED -> SkillArtifactStoreSyncAction.UPDATED;
                case UNCHANGED -> SkillArtifactStoreSyncAction.UNCHANGED;
                case CONFLICT -> SkillArtifactStoreSyncAction.CONFLICT;
                case DELETED -> SkillArtifactStoreSyncAction.DELETED;
            };
        }
    }
}
