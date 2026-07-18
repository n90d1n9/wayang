package tech.kayys.wayang.agent.spi.plugin;

import tech.kayys.wayang.agent.spi.AgentType;
import tech.kayys.wayang.plugin.WayangPlugin;

/**
 * Interface for Agent Plugins.
 * Allows agents to be registered as plugins in the system.
 */
public interface AgentPlugin extends WayangPlugin {

    /**
     * Get the type of agent this plugin provides.
     *
     * @return the agent type
     */
    AgentType getAgentType();
}
