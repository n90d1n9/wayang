package tech.kayys.wayang.agent.skills.cli;

final class SkillsPersistenceConfigValidationService {

    private final SkillsPersistenceConfigResolutionService resolutionService;

    SkillsPersistenceConfigValidationService(SkillsPersistenceConfigResolutionService resolutionService) {
        this.resolutionService = java.util.Objects.requireNonNull(resolutionService, "resolutionService");
    }

    SkillsPersistenceConfigValidationReport report(String profileName, boolean runtimeConfig) {
        return report(SkillsPersistenceConfigValidationRequest.fromOptions(
                profileName,
                runtimeConfig,
                false));
    }

    SkillsPersistenceConfigValidationReport report(
            String profileName,
            boolean runtimeConfig,
            boolean requireDurable) {
        return report(SkillsPersistenceConfigValidationRequest.fromOptions(
                profileName,
                runtimeConfig,
                requireDurable));
    }

    SkillsPersistenceConfigValidationReport report(
            String profileName,
            boolean runtimeConfig,
            SkillsPersistenceConfigValidationPolicy policy) {
        return report(new SkillsPersistenceConfigValidationRequest(
                profileName,
                runtimeConfig,
                policy));
    }

    SkillsPersistenceConfigValidationReport report(SkillsPersistenceConfigValidationRequest request) {
        SkillsPersistenceConfigValidationRequest resolved = request == null
                ? SkillsPersistenceConfigValidationRequest.defaults()
                : request;
        return SkillsPersistenceConfigValidationReport.from(
                resolutionService.resolve(resolved.profileName(), resolved.runtimeConfig()),
                resolved.policy());
    }
}
