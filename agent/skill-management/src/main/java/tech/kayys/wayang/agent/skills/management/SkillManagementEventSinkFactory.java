package tech.kayys.wayang.agent.skills.management;

import java.util.Objects;

/**
 * Resolves the effective event sink from configured stores or an explicit override.
 */
final class SkillManagementEventSinkFactory {

    private final SkillManagementEventStoreFactory eventStoreFactory;
    private final SkillManagementEventSink eventSinkOverride;

    SkillManagementEventSinkFactory(
            SkillManagementEventStoreFactory eventStoreFactory,
            SkillManagementEventSink eventSinkOverride) {
        this.eventStoreFactory = Objects.requireNonNull(eventStoreFactory, "eventStoreFactory");
        this.eventSinkOverride = eventSinkOverride;
    }

    SkillManagementEventSink create(SkillManagementEventStoreConfig config) {
        return eventSinkOverride == null
                ? eventStoreFactory.create(config)
                : eventSinkOverride;
    }

    SkillStoreConfigValidationResult validate(SkillManagementEventStoreConfig config) {
        return eventSinkOverride == null
                ? eventStoreFactory.validate(config)
                : SkillStoreConfigValidationResult.valid();
    }

    SkillStoreConfigValidationResult validatePruneSupport(SkillManagementEventStoreConfig config) {
        return eventSinkOverride == null
                ? eventStoreFactory.validatePruneSupport(config)
                : SkillManagementMaintenancePreflight.validateEventPruneCapability(
                        SkillManagementEventPruner.forSink(eventSinkOverride));
    }

    SkillManagementEventSink override() {
        return eventSinkOverride;
    }
}
