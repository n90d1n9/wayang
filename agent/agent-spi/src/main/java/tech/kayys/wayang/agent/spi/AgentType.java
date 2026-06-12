package tech.kayys.wayang.agent.spi;

/**
 * Agent Type - Enum representing the type/variant of the agent.
 */
public enum AgentType {
    COMMON(AgentRole.EXECUTOR),
    PLANNER(AgentRole.PLANNER),
    CODER(AgentRole.CODER),
    ANALYTICS(AgentRole.ANALYST),
    ORCHESTRATOR(AgentRole.ORCHESTRATOR),
    HERMES(AgentRole.ORCHESTRATOR),
    EVALUATOR(AgentRole.EVALUATOR),
    SPECIALIST(AgentRole.SPECIALIST);

    private final AgentRole defaultRole;

    AgentType(AgentRole defaultRole) {
        this.defaultRole = defaultRole;
    }

    public AgentRole getDefaultRole() {
        return defaultRole;
    }

    public String typeName() {
        return name().toLowerCase();
    }
}
