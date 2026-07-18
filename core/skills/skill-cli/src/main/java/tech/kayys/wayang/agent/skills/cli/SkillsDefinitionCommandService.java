package tech.kayys.wayang.agent.skills.cli;

import tech.kayys.wayang.agent.skills.management.SkillManagementService;
import tech.kayys.wayang.agent.spi.skills.SkillDefinition;
import tech.kayys.wayang.agent.spi.skills.SkillValidation;

import java.util.Objects;

final class SkillsDefinitionCommandService {

    private final SkillManagementService managementService;

    SkillsDefinitionCommandService(SkillManagementService managementService) {
        this.managementService = Objects.requireNonNull(managementService, "managementService");
    }

    SkillsDefinitionRegistrationReport register(SkillsDefinitionRequest request) {
        SkillDefinition skill = Objects.requireNonNull(request, "request").registrationDefinition();
        managementService.createSkill(skill).await().indefinitely();
        return new SkillsDefinitionRegistrationReport(skill);
    }

    SkillsDefinitionValidationReport validate(SkillsDefinitionRequest request) {
        SkillValidation validation = managementService.validateSkill(
                Objects.requireNonNull(request, "request").validationDefinition());
        return new SkillsDefinitionValidationReport(validation);
    }
}
