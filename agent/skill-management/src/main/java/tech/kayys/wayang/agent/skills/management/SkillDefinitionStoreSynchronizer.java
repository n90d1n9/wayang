package tech.kayys.wayang.agent.skills.management;

import tech.kayys.wayang.agent.spi.skills.SkillDefinition;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Synchronizes skill definitions between persistence backends.
 */
public final class SkillDefinitionStoreSynchronizer {

    private static final SkillStoreSyncSupport.SkillStoreSyncDetails DETAILS =
            new SkillStoreSyncSupport.SkillStoreSyncDetails(
                    "Skill missing from target",
                    "Skill already matches target",
                    "Target skill replaced from source",
                    "Target skill differs and overwrite is disabled",
                    "Target skill missing from source");

    private final SkillDefinitionPropertiesCodec codec;

    public SkillDefinitionStoreSynchronizer() {
        this(new SkillDefinitionPropertiesCodec());
    }

    SkillDefinitionStoreSynchronizer(SkillDefinitionPropertiesCodec codec) {
        this.codec = Objects.requireNonNull(codec, "codec");
    }

    public SkillDefinitionStoreSyncResult sync(
            SkillDefinitionStore source,
            SkillDefinitionStore target,
            SkillDefinitionStoreSyncOptions options) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(target, "target");
        SkillDefinitionStoreSyncOptions resolvedOptions =
                options == null ? SkillDefinitionStoreSyncOptions.bootstrap() : options;
        Map<String, SkillDefinition> sourceSkills = byId(source.listSkills());
        Map<String, SkillDefinition> targetSkills = byId(target.listSkills());
        List<SkillDefinitionStoreSyncChange> changes = SkillStoreSyncSupport.sync(
                sourceSkills,
                targetSkills,
                toPolicy(resolvedOptions),
                DETAILS,
                new DefinitionSyncOperations(target));

        return new SkillDefinitionStoreSyncResult(resolvedOptions.dryRun(), changes);
    }

    private Map<String, SkillDefinition> byId(List<SkillDefinition> skills) {
        Map<String, SkillDefinition> byId = new LinkedHashMap<>();
        skills.stream()
                .filter(skill -> skill != null && skill.id() != null && !skill.id().isBlank())
                .sorted(Comparator.comparing(SkillDefinition::id))
                .forEach(skill -> byId.put(skill.id(), skill));
        return byId;
    }

    private String fingerprint(SkillDefinition skill) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(codec.toCanonicalBytes(skill)));
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is not available", error);
        }
    }

    private static SkillStoreSyncPolicy toPolicy(SkillDefinitionStoreSyncOptions options) {
        return SkillStoreSyncPolicy.of(
                options.overwriteExisting(),
                options.deleteMissingFromTarget(),
                options.dryRun());
    }

    private final class DefinitionSyncOperations implements
            SkillStoreSyncSupport.SkillStoreSyncOperations<String, SkillDefinition, SkillDefinitionStoreSyncChange> {

        private final SkillDefinitionStore target;

        private DefinitionSyncOperations(SkillDefinitionStore target) {
            this.target = target;
        }

        @Override
        public boolean equivalent(SkillDefinition sourceValue, SkillDefinition targetValue) {
            return fingerprint(sourceValue).equals(fingerprint(targetValue));
        }

        @Override
        public void copy(SkillDefinition sourceValue) {
            target.registerSkill(sourceValue);
        }

        @Override
        public void delete(String key) {
            target.unregisterSkill(key);
        }

        @Override
        public SkillDefinitionStoreSyncChange change(
                String key,
                SkillStoreSyncSupport.SkillStoreSyncActionType action,
                String detail) {
            return new SkillDefinitionStoreSyncChange(key, definitionAction(action), detail);
        }

        private SkillDefinitionStoreSyncAction definitionAction(
                SkillStoreSyncSupport.SkillStoreSyncActionType action) {
            return switch (action) {
                case COPIED -> SkillDefinitionStoreSyncAction.COPIED;
                case UPDATED -> SkillDefinitionStoreSyncAction.UPDATED;
                case UNCHANGED -> SkillDefinitionStoreSyncAction.UNCHANGED;
                case CONFLICT -> SkillDefinitionStoreSyncAction.CONFLICT;
                case DELETED -> SkillDefinitionStoreSyncAction.DELETED;
            };
        }
    }
}
