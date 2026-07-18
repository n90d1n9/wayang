package tech.kayys.wayang.agent.spi;

/**
 * Agent Role in the system.
 */
public enum AgentRole {
    EXECUTOR, // Executes tasks
    PLANNER, // Creates plans
    CODER, // Generates/analyzes code
    ANALYST, // Analyzes data
    ORCHESTRATOR, // Coordinates agents
    EVALUATOR, // Evaluates results
    SPECIALIST // Domain specialist
}
