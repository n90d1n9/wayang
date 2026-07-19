package tech.kayys.wayang.agent.skills.cli;

import tech.kayys.wayang.agent.skills.management.SkillLifecycleState;

import java.util.Objects;

/**
 * Result of a skill lifecycle transition command.
 */
record SkillsLifecycleCommandReport(
        SkillsLifecycleCommandRequest.Action action,
        SkillLifecycleState state) {

    SkillsLifecycleCommandReport {
        action = Objects.requireNonNull(action, "action");
        state = Objects.requireNonNull(state, "state");
    }
}
