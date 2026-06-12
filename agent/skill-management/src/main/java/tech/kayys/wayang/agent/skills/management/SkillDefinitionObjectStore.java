package tech.kayys.wayang.agent.skills.management;

/**
 * Compatibility alias for the neutral skill-management object-store contract.
 *
 * @deprecated use {@link SkillManagementObjectStore}; object storage now backs
 * skill definitions, lifecycle state, and event history.
 */
@Deprecated(forRemoval = false)
public interface SkillDefinitionObjectStore extends SkillManagementObjectStore {
}
