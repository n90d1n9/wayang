package tech.kayys.wayang.plugin.registry;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.wayang.agent.plugin.AgentPlugin;
import tech.kayys.wayang.plugin.executor.ExecutorPlugin;
import tech.kayys.wayang.plugin.registry.executor.ExecutorRegistration;
import tech.kayys.wayang.plugin.registry.executor.ExecutorStatus;
import tech.kayys.wayang.registry.ControlPlaneAgentRegistry;
import tech.kayys.wayang.plugin.WayangPlugin;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the lifecycle and registration of Wayang plugins in the Control Plane.
 */
@ApplicationScoped
public class ControlPlanePluginManager {

    private static final Logger LOG = LoggerFactory.getLogger(ControlPlanePluginManager.class);

    private final Map<String, PluginRegistration> registeredPlugins = new ConcurrentHashMap<>();

    @Inject
    jakarta.enterprise.inject.Instance<WayangPlugin> plugins;

    @jakarta.annotation.PostConstruct
    void init() {
        plugins.forEach(this::register);
    }

    @Inject
    ControlPlaneExecutorRegistry executorRegistry;

    @Inject
    ControlPlaneAgentRegistry agentRegistry;

    public void register(WayangPlugin plugin) {
        LOG.info("Registering plugin: {} ({})", plugin.name(), plugin.version());

        PluginRegistration registration = new PluginRegistration();
        registration.pluginId = plugin.id();
        registration.pluginName = plugin.name();
        registration.version = plugin.version();
        registration.registeredAt = Instant.now();

        if (plugin instanceof ExecutorPlugin executorPlugin) {
            handleExecutorPlugin(executorPlugin, registration);
        } else if (plugin instanceof tech.kayys.wayang.agent.plugin.AgentPlugin agentPlugin) {
            handleAgentPlugin(agentPlugin, registration);
        }

        registeredPlugins.put(plugin.id(), registration);
        LOG.info("Plugin registered successfully: {}", plugin.id());
    }

    private void handleExecutorPlugin(ExecutorPlugin plugin, PluginRegistration registration) {
        LOG.info("Processing executor plugin capabilities: {}", plugin.capabilities());

        tech.kayys.wayang.plugin.registry.executor.ExecutorRegistration execReg = new tech.kayys.wayang.plugin.registry.executor.ExecutorRegistration();
        execReg.executorId = plugin.id();
        execReg.capabilities = plugin.capabilities();
        execReg.status = tech.kayys.wayang.plugin.registry.executor.ExecutorStatus.HEALTHY;
        execReg.inProcess = true;

        executorRegistry.register(execReg).subscribe().with(
                v -> LOG.info("Executor capability registered for plugin: {}", plugin.id()),
                f -> LOG.error("Failed to register executor for plugin: {}", plugin.id(), f));

        registration.registeredExecutors.add(plugin.id());
    }

    private void handleAgentPlugin(AgentPlugin plugin, PluginRegistration registration) {
        LOG.info("Processing agent plugin: {} (type: {})", plugin.id(), plugin.getAgentType());
        agentRegistry.register(plugin);
        LOG.info("Agent plugin registered in ControlPlaneAgentRegistry: {}", plugin.id());
    }

    public List<PluginRegistration> getRegisteredPlugins() {
        return new ArrayList<>(registeredPlugins.values());
    }

    public PluginRegistration getPlugin(String pluginId) {
        return registeredPlugins.get(pluginId);
    }
}
