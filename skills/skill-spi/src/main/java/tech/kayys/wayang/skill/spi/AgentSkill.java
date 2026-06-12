package tech.kayys.wayang.skill.spi;

/**
 * Compatibility alias for older agent-oriented skill implementations.
 *
 * @deprecated Use {@link Skill} for standalone dynamic skill contracts.
 */
@Deprecated(since = "2026-05-26", forRemoval = false)
public interface AgentSkill extends Skill {
}
