package tech.kayys.wayang.agent.skills.management;

/**
 * A named operation capability requirement that can be reported through preflight validation.
 */
record SkillStoreCapabilityRequirement(
        SkillStoreCapability capability,
        String errorMessage) {

    static SkillStoreCapabilityRequirement eventStorePruning() {
        return new SkillStoreCapabilityRequirement(
                SkillStoreCapability.PRUNE_EVENTS,
                "Event history pruning requires an event store with capability: prune-events");
    }

    static SkillStoreCapabilityRequirement eventSinkPruning() {
        return new SkillStoreCapabilityRequirement(
                SkillStoreCapability.PRUNE_EVENTS,
                "Event history pruning requires event sink capability: prune-events");
    }

    static SkillStoreCapabilityRequirement customEventStorePruning(String name) {
        return new SkillStoreCapabilityRequirement(
                SkillStoreCapability.PRUNE_EVENTS,
                "Custom event store " + name + " requires capability: prune-events");
    }

    boolean satisfiedBy(SkillStoreCapabilities capabilities) {
        SkillStoreCapabilities resolved = capabilities == null ? SkillStoreCapabilities.none() : capabilities;
        return resolved.supports(capability);
    }

    SkillStoreConfigValidationResult validate(SkillStoreCapabilities capabilities) {
        return satisfiedBy(capabilities)
                ? SkillStoreConfigValidationResult.valid()
                : SkillStoreConfigValidationResult.error(errorMessage);
    }
}
