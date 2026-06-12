package tech.kayys.wayang.agent.skills.cli;

import tech.kayys.wayang.agent.skills.management.SkillLifecycleState;
import tech.kayys.wayang.agent.skills.management.SkillManagementService;
import tech.kayys.wayang.agent.spi.skills.SkillDefinition;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

final class SkillsDefinitionQueryService {

    private final SkillManagementService managementService;

    SkillsDefinitionQueryService(SkillManagementService managementService) {
        this.managementService = Objects.requireNonNull(managementService, "managementService");
    }

    SkillsDefinitionListReport list(SkillsDefinitionListRequest request) {
        SkillsDefinitionListRequest resolved = request == null
                ? SkillsDefinitionListRequest.fromOptions("", false)
                : request;
        List<SkillDefinition> skills = managementService.search(
                "",
                resolved.category(),
                resolved.includeDisabled())
                .await().indefinitely();
        return new SkillsDefinitionListReport(skills);
    }

    Optional<SkillsDefinitionInfoReport> info(SkillsDefinitionInfoRequest request) {
        SkillsDefinitionInfoRequest resolved = request == null
                ? SkillsDefinitionInfoRequest.fromOptions("")
                : request;
        return managementService.getSkill(resolved.skillId())
                .await().indefinitely()
                .map(this::report);
    }

    private SkillsDefinitionInfoReport report(SkillDefinition skill) {
        SkillLifecycleState state = managementService.getLifecycleState(skill.id()).await().indefinitely();
        return new SkillsDefinitionInfoReport(skill, state);
    }
}
