package tech.kayys.wayang.agent.adapter;

import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.agent.spi.AgentSkill;
import tech.kayys.wayang.agent.spi.skills.SkillCategory;
import tech.kayys.wayang.agent.spi.skills.SkillDefinition;
import tech.kayys.wayang.agent.spi.skills.SkillHealth;
import tech.kayys.wayang.agent.spi.skills.SkillRegistry;
import tech.kayys.wayang.agent.spi.skills.SkillResult;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Thin compatibility facade over the active {@link SkillRegistry}.
 *
 * <p>
 * The facade keeps runtime {@link AgentSkill} registrations visible through the
 * data-oriented {@link SkillDefinition} methods by deriving a definition from
 * the runtime skill metadata.
 */
public class SkillRegistryAdapter implements SkillRegistry {

    private final SkillRegistry delegate;

    public SkillRegistryAdapter(SkillRegistry delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    public static SkillRegistryAdapter wrap(SkillRegistry delegate) {
        return new SkillRegistryAdapter(delegate);
    }

    @Override
    public List<AgentSkill> listAll() {
        return delegate.listAll();
    }

    @Override
    public Optional<AgentSkill> find(String id) {
        return delegate.find(id);
    }

    @Override
    public AgentSkill findOrThrow(String id) {
        return delegate.findOrThrow(id);
    }

    @Override
    public void register(AgentSkill skill) {
        delegate.register(skill);
        delegate.registerSkill(AgentSkillAdapters.toDefinition(skill));
    }

    @Override
    public void unregister(String skillId) {
        delegate.unregister(skillId);
    }

    @Override
    public List<AgentSkill> findByCategory(SkillCategory category) {
        return delegate.findByCategory(category);
    }

    @Override
    public List<AgentSkill> listAllowed(String tenantId, Set<String> allowedIds) {
        return delegate.listAllowed(tenantId, allowedIds);
    }

    @Override
    public boolean isRegistered(String skillId) {
        return delegate.isRegistered(skillId);
    }

    @Override
    public Map<String, SkillHealth> checkHealth() {
        return delegate.checkHealth();
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public Optional<SkillDefinition> getSkill(String skillId) {
        return delegate.getSkill(skillId);
    }

    @Override
    public List<SkillDefinition> listSkills() {
        return delegate.listSkills();
    }

    @Override
    public List<SkillDefinition> listByCategory(String category) {
        return delegate.listByCategory(category);
    }

    @Override
    public void registerSkill(SkillDefinition skill) {
        delegate.registerSkill(skill);
    }

    @Override
    public boolean unregisterSkill(String skillId) {
        return delegate.unregisterSkill(skillId);
    }

    public Uni<SkillResult> executeSkill(String skillId, Map<String, Object> input) {
        Optional<AgentSkill> skill = find(skillId);
        if (skill.isEmpty()) {
            return Uni.createFrom().item(SkillResult.failure(skillId, "Skill not found: " + skillId));
        }
        long start = System.currentTimeMillis();
        return skill.get().execute(input == null ? Map.of() : input)
                .map(result -> AgentSkillAdapters.toSkillResult(skillId, result, System.currentTimeMillis() - start))
                .onFailure().recoverWithItem(error -> SkillResult.failure(skillId, error));
    }
}
