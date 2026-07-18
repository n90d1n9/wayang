/*
 * PolyForm Noncommercial License 1.0.0
 * Copyright (c) 2026 Kayys.tech
 */
package tech.kayys.wayang.plugin.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.logging.Logger;

import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.plugin.registry.node.NodeDefinition;
import tech.kayys.wayang.schema.validator.SchemaValidator;
import tech.kayys.wayang.schema.validator.ValidationResult;

/**
 * Control Plane Node Registry — authority for node definitions.
 *
 * <p><b>Security model:</b>
 * <ul>
 *   <li>Register / unregister: requires {@code plugin-admin} role.</li>
 *   <li>Read operations (get / list / validate): requires {@code plugin-user} role.</li>
 * </ul>
 */
@ApplicationScoped
public class ControlPlaneNodeRegistry {

    private static final Logger LOG = Logger.getLogger(ControlPlaneNodeRegistry.class);
    private static final Logger AUDIT = Logger.getLogger("wayang.plugin.audit");

    private final Map<String, NodeDefinition> nodeRegistry = new ConcurrentHashMap<>();

    @Inject
    SchemaValidator schemaValidator;

    // -------------------------------------------------------------------------
    // Mutating operations — restricted to plugin administrators
    // -------------------------------------------------------------------------

    /**
     * Register a node definition.
     * Only callers with the {@code plugin-admin} role may invoke this.
     */
    @RolesAllowed("plugin-admin")
    public void register(NodeDefinition node) {
        validateSchemas(node);
        nodeRegistry.put(node.type, node);
        AUDIT.infof("PLUGIN_REGISTRY_REGISTER type=%s executor=%s protocol=%s",
                node.type,
                node.executorBinding != null ? node.executorBinding.executorId : "none",
                node.executorBinding != null ? node.executorBinding.protocol : "none");
        LOG.infof("Registered node: %s", node.type);
    }

    /**
     * Remove a node definition.
     * Only callers with the {@code plugin-admin} role may invoke this.
     */
    @RolesAllowed("plugin-admin")
    public void unregister(String nodeType) {
        NodeDefinition removed = nodeRegistry.remove(nodeType);
        if (removed != null) {
            AUDIT.infof("PLUGIN_REGISTRY_UNREGISTER type=%s", nodeType);
            LOG.infof("Unregistered node: %s", nodeType);
        } else {
            LOG.warnf("Attempted to unregister unknown node: %s", nodeType);
        }
    }

    // -------------------------------------------------------------------------
    // Read operations — available to authenticated plugin users
    // -------------------------------------------------------------------------

    /** Look up a node definition by type. */
    @RolesAllowed({"plugin-admin", "plugin-user"})
    public NodeDefinition get(String nodeType) {
        return nodeRegistry.get(nodeType);
    }

    /** Return all registered node definitions. */
    @RolesAllowed({"plugin-admin", "plugin-user"})
    public List<NodeDefinition> getAll() {
        return new ArrayList<>(nodeRegistry.values());
    }

    /** Return all registered node definitions in a given category. */
    @RolesAllowed({"plugin-admin", "plugin-user"})
    public List<NodeDefinition> getByCategory(String category) {
        return nodeRegistry.values().stream()
                .filter(n -> category.equals(n.category))
                .toList();
    }

    // -------------------------------------------------------------------------
    // Runtime validation — available to authenticated plugin users
    // -------------------------------------------------------------------------

    /** Validate a node's configuration map against its declared JSON Schema. */
    @RolesAllowed({"plugin-admin", "plugin-user"})
    public ValidationResult validateConfig(String nodeType, Map<String, Object> config) {
        NodeDefinition node = nodeRegistry.get(nodeType);
        if (node == null) {
            return ValidationResult.failure("Node type not found: " + nodeType);
        }
        return schemaValidator.validate(node.configSchema, config);
    }

    /** Validate a node's input map against its declared JSON Schema. */
    @RolesAllowed({"plugin-admin", "plugin-user"})
    public ValidationResult validateInputs(String nodeType, Map<String, Object> inputs) {
        NodeDefinition node = nodeRegistry.get(nodeType);
        if (node == null) {
            return ValidationResult.failure("Node type not found: " + nodeType);
        }
        return schemaValidator.validate(node.inputSchema, inputs);
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private void validateSchemas(NodeDefinition node) {
        if (node.configSchema == null && node.inputSchema == null && node.outputSchema == null) {
            LOG.warnf("Node %s has no schemas defined — it will accept any input/config", node.type);
        }
    }
}
