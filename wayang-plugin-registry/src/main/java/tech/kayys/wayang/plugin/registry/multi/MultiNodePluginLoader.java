/*
 * PolyForm Noncommercial License 1.0.0
 * Copyright (c) 2026 Kayys.tech
 */
package tech.kayys.wayang.plugin.registry.multi;

import java.util.HashSet;
import java.util.Map;

import org.jboss.logging.Logger;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.plugin.CommunicationProtocol;
import tech.kayys.wayang.plugin.registry.ControlPlaneExecutorRegistry;
import tech.kayys.wayang.plugin.registry.ControlPlaneNodeRegistry;
import tech.kayys.wayang.plugin.registry.LoadedPlugin;
import tech.kayys.wayang.plugin.registry.PluginRegistration;
import tech.kayys.wayang.plugin.registry.PluginResourceLoader;
import tech.kayys.wayang.plugin.registry.SharedResources;
import tech.kayys.wayang.plugin.registry.UIReference;
import tech.kayys.wayang.plugin.execution.ExecutionMode;
import tech.kayys.wayang.plugin.registry.executor.ExecutorBinding;
import tech.kayys.wayang.plugin.registry.executor.ExecutorManifest;
import tech.kayys.wayang.plugin.registry.executor.ExecutorRegistration;
import tech.kayys.wayang.plugin.registry.node.NodeDefinition;
import tech.kayys.wayang.plugin.registry.node.NodeManifest;
import tech.kayys.wayang.schema.validator.SchemaReference;
import tech.kayys.wayang.schema.validator.SchemaValidator;

/**
 * Loads and registers plugins with multiple nodes
 */
@ApplicationScoped
public class MultiNodePluginLoader {

    private static final Logger LOG = Logger.getLogger(MultiNodePluginLoader.class);

    @Inject
    ControlPlaneNodeRegistry nodeRegistry;

    @Inject
    ControlPlaneExecutorRegistry executorRegistry;

    @Inject
    SchemaValidator schemaValidator;

    @Inject
    PluginResourceLoader resourceLoader;

    public Uni<PluginRegistration> loadPlugin(MultiNodePluginManifest manifest, LoadedPlugin loadedPlugin) {
        LOG.infof("Loading multi-node plugin: %s with %d nodes", manifest.name, manifest.nodes.size());

        return Uni.createFrom().item(() -> {
            PluginRegistration registration = new PluginRegistration();
            registration.pluginId = manifest.pluginId;
            registration.pluginName = manifest.name;
            registration.version = manifest.version;
            registration.family = manifest.family;
            registration.registeredAt = java.time.Instant.now();

            loadSharedResources(manifest.shared, loadedPlugin);

            for (ExecutorManifest executorManifest : manifest.executors) {
                registerExecutor(executorManifest, loadedPlugin, registration);
            }

            for (NodeManifest nodeManifest : manifest.nodes) {
                registerNode(nodeManifest, manifest, loadedPlugin, registration);
            }
            return registration;
        });
    }

    private void loadSharedResources(SharedResources shared, LoadedPlugin loadedPlugin) {
        for (Map.Entry<String, String> entry : shared.schemas.entrySet()) {
            String schemaPath = entry.getValue();
            byte[] schemaContent = loadedPlugin.resources.get(schemaPath);
            if (schemaContent != null) {
                resourceLoader.cacheSchema(entry.getKey(), new String(schemaContent));
            }
        }
        for (Map.Entry<String, String> entry : shared.widgets.entrySet()) {
            String widgetPath = entry.getValue();
            byte[] widgetContent = loadedPlugin.resources.get(widgetPath);
            if (widgetContent != null) {
                resourceLoader.cacheWidget(entry.getKey(), widgetContent);
            }
        }
    }

    private void registerExecutor(ExecutorManifest executorManifest, LoadedPlugin loadedPlugin, PluginRegistration registration) {
        ExecutorRegistration executorReg = new ExecutorRegistration();
        executorReg.executorId = executorManifest.executorId;
        executorReg.executorType = "plugin";
        executorReg.protocol = CommunicationProtocol.GRPC;
        executorReg.inProcess = executorManifest.inProcess;
        executorReg.supportedNodes = new HashSet<>(executorManifest.nodeTypes);

        if (executorManifest.inProcess && executorManifest.className != null) {
            Object executorInstance = loadedPlugin.instances.get(executorManifest.className);
            if (executorInstance != null) {
                executorRegistry.registerInProcessExecutor(executorManifest.executorId, executorInstance);
            }
        }
        executorRegistry.register(executorReg).await().indefinitely();
        registration.registeredExecutors.add(executorManifest.executorId);
    }

    private void registerNode(NodeManifest nodeManifest, MultiNodePluginManifest pluginManifest, LoadedPlugin loadedPlugin, PluginRegistration registration) {
        NodeDefinition nodeDef = new NodeDefinition();
        nodeDef.type = nodeManifest.type;
        nodeDef.label = nodeManifest.label;
        nodeDef.category = nodeManifest.category != null ? nodeManifest.category : pluginManifest.family;
        nodeDef.subCategory = nodeManifest.subCategory;
        nodeDef.description = nodeManifest.description;
        nodeDef.version = pluginManifest.version;
        nodeDef.author = pluginManifest.author;

        nodeDef.configSchema = loadSchema(nodeManifest.configSchema, loadedPlugin);
        nodeDef.inputSchema = loadSchema(nodeManifest.inputSchema, loadedPlugin);
        nodeDef.outputSchema = loadSchema(nodeManifest.outputSchema, loadedPlugin);

        String executorId = nodeManifest.executorId != null ? nodeManifest.executorId : getDefaultExecutorId(pluginManifest, nodeManifest);
        nodeDef.executorBinding = new ExecutorBinding(executorId, ExecutionMode.SYNC, CommunicationProtocol.GRPC);

        if (nodeManifest.widgetId != null) {
            nodeDef.uiReference = new UIReference(nodeManifest.widgetId);
        }
        nodeRegistry.register(nodeDef);
        registration.registeredNodes.add(nodeManifest.type);
    }

    private com.networknt.schema.JsonSchema loadSchema(SchemaReference schemaRef, LoadedPlugin loadedPlugin) {
        if (schemaRef == null || schemaRef.content == null) return null;
        String schemaJson = switch (schemaRef.type) {
            case INLINE -> schemaRef.content;
            case FILE -> {
                byte[] content = loadedPlugin.resources.get(schemaRef.content);
                yield content != null ? new String(content) : null;
            }
            case URL -> resourceLoader.loadFromUrl(schemaRef.content);
        };
        return schemaJson != null ? schemaValidator.createSchema(schemaJson) : null;
    }

    private String getDefaultExecutorId(MultiNodePluginManifest plugin, NodeManifest node) {
        if (plugin.executors.size() == 1) return plugin.executors.get(0).executorId;
        return plugin.pluginId + ".executor." + node.type.replace(".", "-");
    }
}
