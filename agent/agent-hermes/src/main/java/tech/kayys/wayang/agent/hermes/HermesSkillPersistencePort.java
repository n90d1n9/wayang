package tech.kayys.wayang.agent.hermes;

import java.util.Map;

/**
 * Runtime adapter boundary for learned-skill persistence routes.
 */
public interface HermesSkillPersistencePort {

    HermesPortDispatchResult configure(HermesSkillPersistencePlan plan);

    default HermesRuntimePortDescriptor descriptor() {
        return HermesRuntimePortDescriptor.configured(HermesRuntimePortCatalog.SKILL_PERSISTENCE, this);
    }

    static HermesSkillPersistencePort noop() {
        return new HermesSkillPersistencePort() {
            @Override
            public HermesPortDispatchResult configure(HermesSkillPersistencePlan plan) {
                return HermesPortDispatchResult.noop(
                        HermesRuntimePortCatalog.SKILL_PERSISTENCE,
                        "configure",
                        plan == null || plan.routes().isEmpty() ? "" : plan.routes().getFirst().store(),
                        "skill persistence adapter not configured",
                        plan == null ? Map.of() : plan.toMetadata());
            }

            @Override
            public HermesRuntimePortDescriptor descriptor() {
                return HermesRuntimePortDescriptor.noop(HermesRuntimePortCatalog.SKILL_PERSISTENCE);
            }
        };
    }
}
