package tech.kayys.wayang.agent.hermes;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Dispatches active Hermes request-plan directives to configured runtime ports.
 */
public final class HermesDirectiveDispatcher {

    private final HermesRuntimePorts ports;
    private final HermesRuntimeDiagnosticsPort diagnosticsPort;

    public HermesDirectiveDispatcher() {
        this(HermesRuntimePorts.noop());
    }

    public HermesDirectiveDispatcher(HermesRuntimePorts ports) {
        this(ports, HermesRuntimeDiagnosticsPort.noop());
    }

    public HermesDirectiveDispatcher(
            HermesRuntimePorts ports,
            HermesRuntimeDiagnosticsPort diagnosticsPort) {
        this.ports = ports == null ? HermesRuntimePorts.noop() : ports;
        this.diagnosticsPort = diagnosticsPort == null
                ? HermesRuntimeDiagnosticsPort.noop()
                : diagnosticsPort;
    }

    public HermesDirectiveDispatchReport dispatch(HermesRequestPlan plan) {
        Objects.requireNonNull(plan, "plan");
        List<HermesPortDispatchResult> results = new ArrayList<>();
        HermesExecutionDirective execution = plan.executionDirective();
        HermesExecutionPort executionPort = ports.executionPort();
        results.add(dispatch(
                HermesRuntimePortCatalog.EXECUTION,
                execution.active(),
                execution.operation(),
                execution.backend(),
                execution.reason(),
                execution.toMetadata(),
                executionPort.descriptor(),
                () -> executionPort.dispatch(execution)));

        HermesGatewayDirective gateway = plan.gatewayDirective();
        HermesGatewayPort gatewayPort = ports.gatewayPort();
        results.add(dispatch(
                HermesRuntimePortCatalog.GATEWAY,
                gateway.active(),
                gateway.operation(),
                gateway.destinationId(),
                gateway.reason(),
                gateway.toMetadata(),
                gatewayPort.descriptor(),
                () -> gatewayPort.deliver(gateway)));

        HermesAutomationDirective automation = plan.automationDirective();
        HermesAutomationPort automationPort = ports.automationPort();
        results.add(dispatch(
                HermesRuntimePortCatalog.AUTOMATION,
                automation.active(),
                automation.operation(),
                automation.taskId(),
                automation.reason(),
                automation.toMetadata(),
                automationPort.descriptor(),
                () -> automationPort.register(automation)));

        HermesDelegationDirective delegation = plan.delegationDirective();
        HermesDelegationPort delegationPort = ports.delegationPort();
        results.add(dispatch(
                HermesRuntimePortCatalog.DELEGATION,
                delegation.active(),
                delegation.operation(),
                delegation.groupId(),
                delegation.reason(),
                delegation.toMetadata(),
                delegationPort.descriptor(),
                () -> delegationPort.spawn(delegation)));

        HermesProviderRoutingDirective routing = plan.providerRoutingDirective();
        HermesProviderRoutingPort routingPort = ports.providerRoutingPort();
        results.add(dispatch(
                HermesRuntimePortCatalog.PROVIDER_ROUTING,
                routing.active(),
                routing.operation(),
                routing.selectedProvider(),
                routing.reason(),
                routing.toMetadata(),
                routingPort.descriptor(),
                () -> routingPort.route(routing)));

        HermesMemoryReflectionDirective memory = plan.memoryReflectionDirective();
        HermesMemoryReflectionPort memoryPort = ports.memoryReflectionPort();
        results.add(dispatch(
                HermesRuntimePortCatalog.MEMORY_REFLECTION,
                memory.active(),
                memory.operation(),
                memory.subjectId(),
                memory.reason(),
                memory.toMetadata(),
                memoryPort.descriptor(),
                () -> memoryPort.consolidate(memory)));

        HermesTrajectoryExportDirective trajectory = plan.trajectoryExportDirective();
        HermesTrajectoryExportPort trajectoryPort = ports.trajectoryExportPort();
        results.add(dispatch(
                HermesRuntimePortCatalog.TRAJECTORY_EXPORT,
                trajectory.active(),
                trajectory.operation(),
                trajectory.exportId(),
                trajectory.reason(),
                trajectory.toMetadata(),
                trajectoryPort.descriptor(),
                () -> trajectoryPort.export(trajectory)));

        HermesSkillLineageDirective lineage = plan.skillLineageDirective();
        HermesSkillLineagePort lineagePort = ports.skillLineagePort();
        results.add(dispatch(
                HermesRuntimePortCatalog.SKILL_LINEAGE,
                lineage.active(),
                lineage.operation(),
                lineage.target(),
                lineage.reason(),
                lineage.toMetadata(),
                lineagePort.descriptor(),
                () -> lineagePort.inspect(lineage)));
        return new HermesDirectiveDispatchReport(results, ports.toMetadata());
    }

    public HermesPortDispatchResult dispatchSkillPersistence(HermesSkillPersistencePlan plan) {
        HermesSkillPersistencePort skillPersistencePort = ports.skillPersistencePort();
        return safeDispatch(
                HermesRuntimePortCatalog.SKILL_PERSISTENCE,
                "configure",
                plan == null || plan.routes().isEmpty() ? "" : plan.routes().getFirst().store(),
                "skill persistence adapter dispatch",
                plan == null ? Map.of() : plan.toMetadata(),
                skillPersistencePort.descriptor(),
                () -> skillPersistencePort.configure(plan));
    }

    public HermesPortDispatchResult dispatchRuntimeJournal(HermesRuntimeJournalDirective directive) {
        HermesRuntimeJournalDirective resolved = directive == null
                ? HermesRuntimeJournalDirective.latest(HermesRuntimeEventQuery.DEFAULT_LIMIT)
                : directive;
        HermesRuntimeJournalPort runtimeJournalPort = ports.runtimeJournalPort();
        return dispatch(
                HermesRuntimePortCatalog.RUNTIME_JOURNAL,
                resolved.active(),
                resolved.operation(),
                resolved.target(),
                resolved.reason(),
                resolved.toMetadata(),
                runtimeJournalPort.descriptor(),
                () -> runtimeJournalPort.inspect(resolved));
    }

    public HermesPortDispatchResult dispatchLearningAudit(HermesLearningAuditDirective directive) {
        HermesLearningAuditDirective resolved = directive == null
                ? HermesLearningAuditDirective.latest(HermesLearningPromotionReceiptQuery.DEFAULT_LIMIT)
                : directive;
        HermesLearningAuditPort learningAuditPort = ports.learningAuditPort();
        return dispatch(
                HermesRuntimePortCatalog.LEARNING_AUDIT,
                resolved.active(),
                resolved.operation(),
                resolved.target(),
                resolved.reason(),
                resolved.toMetadata(),
                learningAuditPort.descriptor(),
                () -> learningAuditPort.inspect(resolved));
    }

    public HermesPortDispatchResult dispatchRuntimeDiagnostics(HermesRuntimeDiagnosticsDirective directive) {
        HermesRuntimeDiagnosticsDirective resolved = directive == null
                ? HermesRuntimeDiagnosticsDirective.summary()
                : directive;
        return dispatch(
                HermesRuntimePortCatalog.RUNTIME_DIAGNOSTICS,
                resolved.active(),
                resolved.operation(),
                resolved.target(),
                resolved.reason(),
                resolved.toMetadata(),
                diagnosticsPort.descriptor(),
                () -> diagnosticsPort.inspect(resolved));
    }

    public HermesPortDispatchResult dispatchSkillLineage(HermesSkillLineageDirective directive) {
        HermesSkillLineageDirective resolved = directive == null
                ? HermesSkillLineageDirective.none()
                : directive;
        HermesSkillLineagePort skillLineagePort = ports.skillLineagePort();
        return dispatch(
                HermesRuntimePortCatalog.SKILL_LINEAGE,
                resolved.active(),
                resolved.operation(),
                resolved.target(),
                resolved.reason(),
                resolved.toMetadata(),
                skillLineagePort.descriptor(),
                () -> skillLineagePort.inspect(resolved));
    }

    private static HermesPortDispatchResult dispatch(
            String port,
            boolean active,
            String operation,
            String target,
            String reason,
            Map<String, Object> metadata,
            HermesRuntimePortDescriptor descriptor,
            Supplier<HermesPortDispatchResult> invoker) {
        Map<String, Object> dispatchMetadata = withRuntimePort(metadata, descriptor);
        if (!active) {
            return HermesPortDispatchResult.skipped(port, operation, target, reason, dispatchMetadata);
        }
        return safeDispatch(port, operation, target, reason, dispatchMetadata, descriptor, invoker);
    }

    private static HermesPortDispatchResult safeDispatch(
            String port,
            String operation,
            String target,
            String reason,
            Map<String, Object> metadata,
            HermesRuntimePortDescriptor descriptor,
            Supplier<HermesPortDispatchResult> invoker) {
        Map<String, Object> dispatchMetadata = withRuntimePort(metadata, descriptor);
        if (descriptor != null && !descriptor.ready()) {
            return HermesPortDispatchResult.unavailable(
                    port,
                    operation,
                    target,
                    HermesDirectiveSupport.clean(descriptor.reason(), reason),
                    dispatchMetadata);
        }
        try {
            HermesPortDispatchResult result = invoker.get();
            if (result != null) {
                return withRuntimePort(result, descriptor);
            }
            return HermesPortDispatchResult.failed(
                    port,
                    operation,
                    target,
                    "runtime port returned no dispatch result",
                    dispatchMetadata);
        } catch (RuntimeException error) {
            Map<String, Object> failure = new LinkedHashMap<>(dispatchMetadata);
            failure.put("errorType", error.getClass().getName());
            failure.put("error", error.getMessage());
            return HermesPortDispatchResult.failed(port, operation, target, reason, failure);
        }
    }

    private static HermesPortDispatchResult withRuntimePort(
            HermesPortDispatchResult result,
            HermesRuntimePortDescriptor descriptor) {
        return new HermesPortDispatchResult(
                result.port(),
                result.operation(),
                result.target(),
                result.active(),
                result.dispatched(),
                result.successful(),
                result.status(),
                result.reason(),
                withRuntimePort(result.metadata(), descriptor));
    }

    private static Map<String, Object> withRuntimePort(
            Map<String, Object> metadata,
            HermesRuntimePortDescriptor descriptor) {
        Map<String, Object> values = new LinkedHashMap<>(metadata == null ? Map.of() : metadata);
        if (descriptor != null) {
            values.put("runtimePort", descriptor.toMetadata());
        }
        return Map.copyOf(values);
    }
}
