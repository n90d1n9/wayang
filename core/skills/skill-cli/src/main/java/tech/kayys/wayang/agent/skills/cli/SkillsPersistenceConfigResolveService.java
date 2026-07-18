package tech.kayys.wayang.agent.skills.cli;

final class SkillsPersistenceConfigResolveService {

    private final SkillsPersistenceConfigResolutionService resolutionService;

    SkillsPersistenceConfigResolveService(SkillsPersistenceConfigResolutionService resolutionService) {
        this.resolutionService = java.util.Objects.requireNonNull(resolutionService, "resolutionService");
    }

    SkillsPersistenceConfigResolveReport report(String profileName, boolean runtimeConfig) {
        return report(SkillsPersistenceConfigResolveRequest.fromOptions(profileName, runtimeConfig));
    }

    SkillsPersistenceConfigResolveReport report(SkillsPersistenceConfigResolveRequest request) {
        SkillsPersistenceConfigResolveRequest resolved = request == null
                ? SkillsPersistenceConfigResolveRequest.defaults()
                : request;
        return SkillsPersistenceConfigResolveReport.from(
                resolutionService.resolve(resolved.profileName(), resolved.runtimeConfig()));
    }
}
