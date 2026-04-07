package tech.kayys.wayang.agent.spi.core;

/**
 * Agent Capability Type - Categorization of what an agent can do.
 */
public enum AgentCapabilityType {
    // Core capabilities
    REASONING,
    PLANNING,
    TOOL_USE,
    MEMORY,
    LEARNING,

    // Specialized capabilities
    CODE_GENERATION,
    CODE_ANALYSIS,
    DATA_ANALYSIS,
    DECOMPOSITION,
    ORCHESTRATION,
    COORDINATION,
    EVALUATION,

    // Advanced capabilities
    SELF_REFLECTION,
    COLLABORATIVE,
    ADAPTIVE,
    MULTI_MODAL
}
