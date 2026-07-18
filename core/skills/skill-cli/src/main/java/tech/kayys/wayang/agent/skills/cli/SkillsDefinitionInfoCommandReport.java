package tech.kayys.wayang.agent.skills.cli;

import java.util.Optional;

/**
 * Result of a CLI skill definition info lookup.
 */
record SkillsDefinitionInfoCommandReport(
        SkillsDefinitionInfoRequest request,
        Optional<SkillsDefinitionInfoReport> info) {

    SkillsDefinitionInfoCommandReport {
        request = request == null ? SkillsDefinitionInfoRequest.fromOptions("") : request;
        info = info == null ? Optional.empty() : info;
    }

    boolean found() {
        return info.isPresent();
    }

    String skillId() {
        return request.skillId();
    }
}
