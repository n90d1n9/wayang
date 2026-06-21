/*
 * PolyForm Noncommercial License 1.0.0
 *
 * Copyright (c) 2026 Kayys.tech
 *
 * This software is licensed for non-commercial use only.
 * You may use, modify, and distribute this software for personal,
 * educational, or research purposes.
 *
 * Commercial use, including SaaS or revenue-generating services,
 * requires a separate commercial license from Kayys.tech.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND.
 *
 * @author Bhangun
 */

package tech.kayys.wayang.registry;

import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.agent.AgentType;
import tech.kayys.wayang.agent.plugin.AgentPlugin;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for Agent Plugins in the Control Plane.
 */
@ApplicationScoped
public class ControlPlaneAgentRegistry {

    private final Map<String, AgentPlugin> pluginsById = new ConcurrentHashMap<>();
    private final Map<AgentType, AgentPlugin> pluginsByType = new ConcurrentHashMap<>();

    /**
     * Register an agent plugin.
     *
     * @param plugin the plugin to register
     */
    public void register(AgentPlugin plugin) {
        pluginsById.put(plugin.id(), plugin);
        pluginsByType.put(plugin.getAgentType(), plugin);
    }

    /**
     * Get an agent plugin by type.
     *
     * @param type the agent type
     * @return the plugin, or null if not found
     */
    public AgentPlugin getPluginByType(AgentType type) {
        return pluginsByType.get(type);
    }

    /**
     * Get an agent plugin by ID.
     *
     * @param id the plugin ID
     * @return the plugin, or null if not found
     */
    public AgentPlugin getPluginById(String id) {
        return pluginsById.get(id);
    }
}
