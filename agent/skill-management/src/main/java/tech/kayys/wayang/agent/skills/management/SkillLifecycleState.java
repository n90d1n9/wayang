package tech.kayys.wayang.agent.skills.management;

import java.time.Instant;

/**
 * Lightweight lifecycle metadata kept beside a {@code SkillDefinition}.
 */
public record SkillLifecycleState(
        String skillId,
        SkillLifecycleStatus status,
        Instant createdAt,
        Instant updatedAt,
        int revision) {

    public SkillLifecycleState {
        status = status == null ? SkillLifecycleStatus.ACTIVE : status;
        createdAt = createdAt == null ? Instant.now() : createdAt;
        updatedAt = updatedAt == null ? createdAt : updatedAt;
        revision = Math.max(1, revision);
    }

    static SkillLifecycleState created(String skillId) {
        Instant now = Instant.now();
        return new SkillLifecycleState(skillId, SkillLifecycleStatus.ACTIVE, now, now, 1);
    }

    SkillLifecycleState withStatus(SkillLifecycleStatus nextStatus) {
        return new SkillLifecycleState(skillId, nextStatus, createdAt, Instant.now(), revision);
    }

    SkillLifecycleState nextRevision() {
        return new SkillLifecycleState(skillId, status, createdAt, Instant.now(), revision + 1);
    }
}
