package tech.kayys.wayang.agent.skills.cli;

/**
 * Input envelope for skill lifecycle transition commands.
 */
record SkillsLifecycleCommandRequest(
        String skillId,
        Action action) {

    SkillsLifecycleCommandRequest {
        skillId = skillId == null ? "" : skillId;
        action = action == null ? Action.ENABLE : action;
    }

    static SkillsLifecycleCommandRequest enable(String skillId) {
        return new SkillsLifecycleCommandRequest(skillId, Action.ENABLE);
    }

    static SkillsLifecycleCommandRequest disable(String skillId) {
        return new SkillsLifecycleCommandRequest(skillId, Action.DISABLE);
    }

    enum Action {
        ENABLE("Enabled"),
        DISABLE("Disabled");

        private final String label;

        Action(String label) {
            this.label = label;
        }

        String label() {
            return label;
        }
    }
}
