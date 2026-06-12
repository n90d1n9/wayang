package tech.kayys.wayang.agent.skills.cli;

import java.util.Objects;

final class SkillsDefinitionInfoCommandService {

    private final SkillsDefinitionQueryService queryService;

    SkillsDefinitionInfoCommandService(SkillsDefinitionQueryService queryService) {
        this.queryService = Objects.requireNonNull(queryService, "queryService");
    }

    SkillsDefinitionInfoCommandReport report(SkillsDefinitionInfoRequest request) {
        SkillsDefinitionInfoRequest resolved = request == null
                ? SkillsDefinitionInfoRequest.fromOptions("")
                : request;
        return new SkillsDefinitionInfoCommandReport(resolved, queryService.info(resolved));
    }
}
