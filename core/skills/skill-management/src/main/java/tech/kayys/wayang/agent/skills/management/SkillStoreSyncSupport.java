package tech.kayys.wayang.agent.skills.management;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Shared source-to-target synchronization loop for skill store domains.
 */
final class SkillStoreSyncSupport {

    private SkillStoreSyncSupport() {
    }

    static <K, V, C> List<C> sync(
            Map<K, V> sourceItems,
            Map<K, V> targetItems,
            SkillStoreSyncPolicy options,
            SkillStoreSyncDetails details,
            SkillStoreSyncOperations<K, V, C> operations) {
        Objects.requireNonNull(sourceItems, "sourceItems");
        Objects.requireNonNull(targetItems, "targetItems");
        SkillStoreSyncPolicy resolvedOptions = options == null ? SkillStoreSyncPolicy.bootstrap() : options;
        SkillStoreSyncDetails resolvedDetails = Objects.requireNonNull(details, "details");
        SkillStoreSyncOperations<K, V, C> resolvedOperations =
                Objects.requireNonNull(operations, "operations");
        List<C> changes = new ArrayList<>();

        sourceItems.forEach((key, sourceValue) -> {
            V targetValue = targetItems.get(key);
            if (targetValue == null) {
                if (!resolvedOptions.dryRun()) {
                    resolvedOperations.copy(sourceValue);
                }
                changes.add(resolvedOperations.change(key, SkillStoreSyncActionType.COPIED,
                        resolvedDetails.missingFromTarget()));
                return;
            }

            if (resolvedOperations.equivalent(sourceValue, targetValue)) {
                changes.add(resolvedOperations.change(key, SkillStoreSyncActionType.UNCHANGED,
                        resolvedDetails.alreadyMatchesTarget()));
            } else if (resolvedOptions.overwriteExisting()) {
                if (!resolvedOptions.dryRun()) {
                    resolvedOperations.copy(sourceValue);
                }
                changes.add(resolvedOperations.change(key, SkillStoreSyncActionType.UPDATED,
                        resolvedDetails.replacedFromSource()));
            } else {
                changes.add(resolvedOperations.change(key, SkillStoreSyncActionType.CONFLICT,
                        resolvedDetails.differsAndOverwriteDisabled()));
            }
        });

        if (resolvedOptions.deleteMissingFromTarget()) {
            targetItems.keySet().stream()
                    .filter(key -> !sourceItems.containsKey(key))
                    .forEach(key -> {
                        if (!resolvedOptions.dryRun()) {
                            resolvedOperations.delete(key);
                        }
                        changes.add(resolvedOperations.change(key, SkillStoreSyncActionType.DELETED,
                                resolvedDetails.missingFromSource()));
                    });
        }

        return changes;
    }

    enum SkillStoreSyncActionType {
        COPIED,
        UPDATED,
        UNCHANGED,
        CONFLICT,
        DELETED
    }

    record SkillStoreSyncDetails(
            String missingFromTarget,
            String alreadyMatchesTarget,
            String replacedFromSource,
            String differsAndOverwriteDisabled,
            String missingFromSource) {
    }

    interface SkillStoreSyncOperations<K, V, C> {

        boolean equivalent(V sourceValue, V targetValue);

        void copy(V sourceValue);

        void delete(K key);

        C change(K key, SkillStoreSyncActionType action, String detail);
    }
}
