package tech.kayys.wayang.agent.skills.cli;

import tech.kayys.wayang.agent.skills.management.SkillLifecycleState;
import tech.kayys.wayang.agent.skills.management.SkillManagementService;

import java.util.Objects;

final class SkillsLifecycleCommandService {

    private final SkillManagementService managementService;

    SkillsLifecycleCommandService(SkillManagementService managementService) {
        this.managementService = Objects.requireNonNull(managementService, "managementService");
    }

    SkillsLifecycleCommandReport execute(SkillsLifecycleCommandRequest request) {
        SkillsLifecycleCommandRequest resolved = request == null
                ? SkillsLifecycleCommandRequest.enable("")
                : request;
        SkillLifecycleState state = switch (resolved.action()) {
            case ENABLE -> managementService.enableSkill(resolved.skillId()).await().indefinitely();
            case DISABLE -> managementService.disableSkill(resolved.skillId()).await().indefinitely();
        };
        return new SkillsLifecycleCommandReport(resolved.action(), state);
    }
}
