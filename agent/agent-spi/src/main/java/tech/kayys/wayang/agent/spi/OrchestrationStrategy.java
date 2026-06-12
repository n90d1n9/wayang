package tech.kayys.wayang.agent.spi;

/**
 * Supported agent orchestration strategies.
 */
public enum OrchestrationStrategy {
    REACT("react", "Iterative reason -> act -> observe loop"),
    PLAN_AND_EXECUTE("plan-and-execute", "Up-front planning, then sequential execution"),
    CHAIN_OF_THOUGHT("cot", "Pure reasoning without tool calls"),
    REFLEXION("reflexion", "ReAct with self-reflection and correction"),
    TREE_OF_THOUGHT("tree-of-thought", "Branching reasoning with beam search"),
    HERMES_AGENT("hermes-agent", "Always-on persistent agent mode with memory, skills, MCP, scheduling, and delegation"),
    CUSTOM("custom", "User-supplied orchestration logic");

    public final String id;
    public final String description;

    OrchestrationStrategy(String id, String desc) {
        this.id = id;
        this.description = desc;
    }

    public static OrchestrationStrategy fromId(String id) {
        for (OrchestrationStrategy s : values()) {
            if (s.id.equalsIgnoreCase(id)) return s;
        }
        return REACT;
    }
}
