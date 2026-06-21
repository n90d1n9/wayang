/*
 * PolyForm Noncommercial License 1.0.0
 * Copyright (c) 2026 Kayys.tech
 */
package tech.kayys.wayang.plugin.registry.execution;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

import org.jboss.logging.Logger;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.plugin.TraceContext;
import tech.kayys.wayang.plugin.execution.ExecutionContract;
import tech.kayys.wayang.plugin.execution.ExecutionContext;
import tech.kayys.wayang.plugin.registry.node.NodeDefinition;
import tech.kayys.wayang.plugin.registry.executor.ExecutorRegistration;
import tech.kayys.wayang.plugin.registry.ControlPlaneExecutorRegistry;
import tech.kayys.wayang.plugin.registry.ControlPlaneNodeRegistry;
import tech.kayys.wayang.plugin.executor.ExecutorDescriptor;
import tech.kayys.wayang.schema.validator.SchemaValidator;
import tech.kayys.wayang.schema.validator.ValidationResult;

/**
 * Builder for creating execution contracts
 */
@ApplicationScoped
public class ExecutionContractBuilder {

    private static final Logger LOG = Logger.getLogger(ExecutionContractBuilder.class);

    @Inject
    ControlPlaneNodeRegistry nodeRegistry;

    @Inject
    ControlPlaneExecutorRegistry executorRegistry;

    @Inject
    SchemaValidator schemaValidator;

    public Uni<ExecutionContract> build(
            String workflowRunId,
            String nodeType,
            String nodeInstanceId,
            Map<String, Object> inputs,
            Map<String, Object> config,
            ExecutionContext context) {

        return Uni.createFrom().item(() -> {
            NodeDefinition node = nodeRegistry.get(nodeType);
            if (node == null) {
                throw new IllegalArgumentException("Node type not found: " + nodeType);
            }

            ValidationResult inputValidation = schemaValidator.validate(node.inputSchema, inputs);
            if (!inputValidation.isValid()) {
                throw new IllegalArgumentException("Input validation failed: " + inputValidation.getMessage());
            }

            ValidationResult configValidation = schemaValidator.validate(node.configSchema, config);
            if (!configValidation.isValid()) {
                throw new IllegalArgumentException("Config validation failed: " + configValidation.getMessage());
            }

            ExecutorRegistration executor = executorRegistry.resolveForNode(nodeType);
            if (executor == null) {
                throw new IllegalStateException("No healthy executor found for node: " + nodeType);
            }

            ExecutionContract contract = new ExecutionContract();
            contract.executionId = UUID.randomUUID().toString();
            contract.workflowRunId = workflowRunId;
            contract.nodeType = nodeType;
            contract.nodeVersion = node.version;
            contract.nodeInstanceId = nodeInstanceId;

            contract.executor = new ExecutorDescriptor();
            contract.executor.executorId = executor.executorId;
            contract.executor.capabilities = new HashSet<>(executor.capabilities);
            contract.executor.endpoint = executor.endpoint;
            contract.executor.protocol = executor.protocol;

            contract.mode = node.executorBinding.mode;
            contract.inputs = new HashMap<>(inputs);
            contract.config = new HashMap<>(config);
            contract.context = context;

            contract.trace = new TraceContext();
            contract.trace.traceId = UUID.randomUUID().toString();
            contract.trace.spanId = UUID.randomUUID().toString();

            contract.createdAt = Instant.now();
            contract.expiresAt = Instant.now().plusSeconds(executor.metadata.timeoutMs / 1000);

            LOG.infof("Built execution contract: %s for node %s using executor %s",
                    contract.executionId, nodeType, executor.executorId);

            return contract;
        });
    }
}
