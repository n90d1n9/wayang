/*
 * PolyForm Noncommercial License 1.0.0
 * Copyright (c) 2026 Kayys.tech
 */
package tech.kayys.wayang.plugin.registry.node;

import java.util.ArrayList;
import java.util.List;

import org.jboss.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.plugin.CommunicationProtocol;
import tech.kayys.wayang.plugin.execution.ExecutionMode;
import tech.kayys.wayang.plugin.multi.MultiNodePlugin;
import tech.kayys.wayang.plugin.registry.UIReference;
import tech.kayys.wayang.plugin.registry.executor.ExecutorBinding;
import tech.kayys.wayang.schema.validator.SchemaValidator;

/**
 * Discovers nodes from annotations
 */
@ApplicationScoped
public class AnnotationBasedNodeDiscovery {

    private static final Logger LOG = Logger.getLogger(AnnotationBasedNodeDiscovery.class);

    @Inject
    SchemaValidator schemaValidator;

    public List<NodeDefinition> discoverNodes(Class<?> pluginClass) {
        List<NodeDefinition> nodes = new ArrayList<>();
        MultiNodePlugin pluginAnnotation = pluginClass.getAnnotation(MultiNodePlugin.class);
        if (pluginAnnotation == null) {
            return nodes;
        }

        Nodes nodeDefinitions = pluginClass.getAnnotation(Nodes.class);
        Node[] nodeDefs = nodeDefinitions != null ? nodeDefinitions.value()
                : new Node[] { pluginClass.getAnnotation(Node.class) };

        if (nodeDefs.length == 0 || nodeDefs[0] == null) {
            return nodes;
        }

        for (Node nodeDef : nodeDefs) {
            NodeDefinition node = convertAnnotationToNode(nodeDef, pluginAnnotation, pluginClass);
            nodes.add(node);
        }
        return nodes;
    }

    private NodeDefinition convertAnnotationToNode(Node annotation, MultiNodePlugin plugin, Class<?> pluginClass) {
        NodeDefinition node = new NodeDefinition();
        node.type = annotation.type();
        node.label = annotation.label();
        node.category = !annotation.category().isEmpty() ? annotation.category() : plugin.family();
        node.subCategory = annotation.subCategory();
        node.description = annotation.description();
        node.version = plugin.version();
        node.author = plugin.author();

        if (!annotation.configSchema().isEmpty()) {
            node.configSchema = loadSchemaFromResource(annotation.configSchema(), pluginClass);
        }
        if (!annotation.inputSchema().isEmpty()) {
            node.inputSchema = loadSchemaFromResource(annotation.inputSchema(), pluginClass);
        }
        if (!annotation.outputSchema().isEmpty()) {
            node.outputSchema = loadSchemaFromResource(annotation.outputSchema(), pluginClass);
        }

        String executorId = !annotation.executorId().isEmpty() ? annotation.executorId() : plugin.id() + ".executor";
        node.executorBinding = new ExecutorBinding(executorId, ExecutionMode.SYNC, CommunicationProtocol.GRPC);

        if (!annotation.widgetId().isEmpty()) {
            node.uiReference = new UIReference(annotation.widgetId());
        }
        return node;
    }

    private com.networknt.schema.JsonSchema loadSchemaFromResource(String schemaPath, Class<?> pluginClass) {
        try {
            if (schemaPath.trim().startsWith("{")) {
                return schemaValidator.createSchema(schemaPath);
            }
            java.io.InputStream is = pluginClass.getResourceAsStream(schemaPath);
            if (is != null) {
                String schemaJson = new String(is.readAllBytes());
                return schemaValidator.createSchema(schemaJson);
            }
        } catch (Exception e) {
            LOG.errorf(e, "Failed to load schema from: %s", schemaPath);
        }
        return null;
    }
}
