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

import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.plugin.CommunicationProtocol;
import tech.kayys.wayang.plugin.registry.executor.ExecutorRegistration;
import tech.kayys.wayang.plugin.registry.executor.ExecutorStatus;

/**
 * Control Plane Executor Registry — authority for executor capabilities.
 *
 * <p><b>Security model:</b>
 * <ul>
 *   <li>register / unregister / registerInProcess: requires {@code plugin-admin} role.</li>
 *   <li>get / list / resolve: requires {@code plugin-user} role.</li>
 * </ul>
 */
@ApplicationScoped
public class ControlPlaneExecutorRegistry {

    private static final Logger LOG = Logger.getLogger(ControlPlaneExecutorRegistry.class);
    private static final Logger AUDIT = Logger.getLogger("wayang.plugin.audit");

    private final Map<String, ExecutorRegistration> executorRegistry = new ConcurrentHashMap<>();

    // -------------------------------------------------------------------------
    // Mutating operations — restricted to plugin administrators
    // -------------------------------------------------------------------------

    /**
     * Register a remote executor with capabilities.
     * Only callers with the {@code plugin-admin} role may invoke this.
     */
    @RolesAllowed("plugin-admin")
    public Uni<Void> register(ExecutorRegistration registration) {
        LOG.infof("Registering executor: %s (%s) at %s",
                registration.executorId,
                registration.protocol,
                registration.endpoint);

        executorRegistry.put(registration.executorId, registration);

        return performHealthCheck(registration)
                .onItem().invoke(healthy -> {
                    if (healthy) {
                        registration.status = ExecutorStatus.HEALTHY;
                        AUDIT.infof("EXECUTOR_REGISTRY_REGISTER id=%s endpoint=%s healthy=true",
                                registration.executorId, registration.endpoint);
                        LOG.infof("Executor %s is healthy", registration.executorId);
                    } else {
                        registration.status = ExecutorStatus.UNHEALTHY;
                        AUDIT.warnf("EXECUTOR_REGISTRY_REGISTER id=%s endpoint=%s healthy=false",
                                registration.executorId, registration.endpoint);
                        LOG.warnf("Executor %s is unhealthy", registration.executorId);
                    }
                })
                .replaceWithVoid();
    }

    /**
     * Register an in-process executor instance (for embedded plugins).
     * Only callers with the {@code plugin-admin} role may invoke this.
     */
    @RolesAllowed("plugin-admin")
    public void registerInProcessExecutor(String executorId, Object instance) {
        LOG.infof("Registering in-process executor instance: %s", executorId);
        ExecutorRegistration reg = executorRegistry.get(executorId);
        if (reg != null) {
            reg.executorInstance = instance;
            reg.inProcess = true;
            reg.status = ExecutorStatus.HEALTHY;
        } else {
            ExecutorRegistration newReg = new ExecutorRegistration();
            newReg.executorId = executorId;
            newReg.executorInstance = instance;
            newReg.inProcess = true;
            newReg.status = ExecutorStatus.HEALTHY;
            newReg.protocol = CommunicationProtocol.GRPC;
            executorRegistry.put(executorId, newReg);
        }
        AUDIT.infof("EXECUTOR_REGISTRY_REGISTER_INPROCESS id=%s", executorId);
    }

    /**
     * Remove an executor registration.
     * Only callers with the {@code plugin-admin} role may invoke this.
     */
    @RolesAllowed("plugin-admin")
    public void unregister(String executorId) {
        ExecutorRegistration removed = executorRegistry.remove(executorId);
        if (removed != null) {
            AUDIT.infof("EXECUTOR_REGISTRY_UNREGISTER id=%s", executorId);
            LOG.infof("Unregistered executor: %s", executorId);
        }
    }

    // -------------------------------------------------------------------------
    // Read operations — available to authenticated plugin users
    // -------------------------------------------------------------------------

    /** Look up an executor by its ID. */
    @RolesAllowed({"plugin-admin", "plugin-user"})
    public ExecutorRegistration get(String executorId) {
        return executorRegistry.get(executorId);
    }

    /** Return all registered executors. */
    @RolesAllowed({"plugin-admin", "plugin-user"})
    public List<ExecutorRegistration> getAll() {
        return new ArrayList<>(executorRegistry.values());
    }

    /** Return all executors that advertise a given capability. */
    @RolesAllowed({"plugin-admin", "plugin-user"})
    public List<ExecutorRegistration> getByCapability(String capability) {
        return executorRegistry.values().stream()
                .filter(e -> e.capabilities.contains(capability))
                .toList();
    }

    /**
     * Resolve the best healthy executor for a given node type.
     * Executors must be HEALTHY and explicitly declare support for the node type.
     */
    @RolesAllowed({"plugin-admin", "plugin-user"})
    public ExecutorRegistration resolveForNode(String nodeType) {
        return executorRegistry.values().stream()
                .filter(e -> e.supportedNodes.contains(nodeType))
                .filter(e -> e.status == ExecutorStatus.HEALTHY)
                .findFirst()
                .orElse(null);
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private Uni<Boolean> performHealthCheck(ExecutorRegistration registration) {
        // TODO: implement real gRPC / HTTP health probe once protocol is stable
        return Uni.createFrom().item(true);
    }
}
