package tech.kayys.wayang.agent.hermes;

import tech.kayys.wayang.agent.spi.skills.SkillDefinition;

/**
 * Candidate existing learned skill selected for reuse or refinement.
 */
public record HermesSkillReuseMatch(
        SkillDefinition skill,
        double score,
        String reason) {

    public HermesSkillReuseMatch {
        if (skill == null) {
            throw new IllegalArgumentException("matched skill is required");
        }
        if (score < 0.0 || score > 1.0) {
            throw new IllegalArgumentException("reuse score must be between 0.0 and 1.0");
        }
        reason = reason == null ? "" : reason.trim();
    }
}
