package tech.kayys.wayang.agent.skills.management;

import java.util.function.Supplier;

/**
 * Shared preflight rules for maintenance plans.
 */
final class SkillManagementMaintenancePreflight {

    private SkillManagementMaintenancePreflight() {
    }

    static SkillManagementMaintenancePlan plan(SkillManagementMaintenancePlan plan) {
        return SkillManagementConfigResolution.maintenancePlan(plan);
    }

    static SkillManagementPreflightReport report(
            SkillManagementMaintenancePlan plan,
            SkillManagementEventPruner eventPruner) {
        return report(
                plan,
                () -> validateEventPruneCapability(eventPruner));
    }

    static SkillManagementPreflightReport report(
            SkillManagementMaintenancePlan plan,
            Supplier<SkillStoreConfigValidationResult> eventPruneValidation) {
        return new SkillManagementPreflightReport(
                SkillStoreConfigValidationResult.valid(),
                SkillStoreConfigValidationResult.valid(),
                SkillStoreConfigValidationResult.valid(),
                validateCapabilities(plan, eventPruneValidation));
    }

    static SkillStoreConfigValidationResult validateCapabilities(
            SkillManagementMaintenancePlan plan,
            SkillManagementEventPruner eventPruner) {
        return validateCapabilities(
                plan,
                () -> validateEventPruneCapability(eventPruner));
    }

    static SkillStoreConfigValidationResult validateCapabilities(
            SkillManagementMaintenancePlan plan,
            Supplier<SkillStoreConfigValidationResult> eventPruneValidation) {
        SkillManagementMaintenancePlan resolved = plan(plan);
        if (!resolved.eventPrunePolicy().enabled()) {
            return SkillStoreConfigValidationResult.valid();
        }
        return validation(eventPruneValidation);
    }

    static SkillStoreConfigValidationResult validateEventPruneCapability(
            SkillManagementEventPruner eventPruner) {
        SkillManagementEventPruner resolved =
                eventPruner == null ? SkillManagementEventPruner.unsupported(null) : eventPruner;
        return SkillStoreCapabilityRequirement.eventSinkPruning()
                .validate(SkillStoreCapabilities.eventStore(resolved));
    }

    private static SkillStoreConfigValidationResult validation(
            Supplier<SkillStoreConfigValidationResult> supplier) {
        if (supplier == null) {
            return SkillStoreCapabilityRequirement.eventSinkPruning()
                    .validate(SkillStoreCapabilities.none());
        }
        SkillStoreConfigValidationResult validation = supplier.get();
        return validation == null ? SkillStoreConfigValidationResult.valid() : validation;
    }
}
