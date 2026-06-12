package tech.kayys.wayang.agent.skills.management;

import java.util.Objects;

/**
 * Startup helper that assembles skill management and applies configured repair.
 */
public final class SkillManagementBootstrapper {

    private final SkillManagementServiceFactory serviceFactory;
    private final SkillManagementEventRecorder eventRecorder;

    public SkillManagementBootstrapper(SkillManagementServiceFactory serviceFactory) {
        this(serviceFactory, SkillManagementEventSink.noop());
    }

    public SkillManagementBootstrapper(
            SkillManagementServiceFactory serviceFactory,
            SkillManagementEventSink eventSink) {
        this.serviceFactory = Objects.requireNonNull(serviceFactory, "serviceFactory");
        this.eventRecorder = new SkillManagementEventRecorder(eventSink);
    }

    public SkillManagementBootstrapResult bootstrap() {
        return bootstrap(SkillManagementConfigResolution.serviceConfig(null));
    }

    public SkillManagementBootstrapResult bootstrap(SkillManagementServiceConfig config) {
        SkillManagementServiceConfig resolved = SkillManagementConfigResolution.serviceConfig(config);
        SkillManagementOperationContext context = SkillManagementOperationContext.root();
        return eventRecorder.recordOperation(
                SkillManagementEventOperation.BOOTSTRAP,
                "",
                context,
                () -> {
                    SkillManagementService service = serviceFactory.create(resolved);
                    SkillManagementInspection initialInspection = service.inspectManagement().await().indefinitely();
                    SkillLifecycleStateReconcileResult reconcileResult = service.reconcileLifecycleState(
                            resolved.lifecycleStateReconcileOptions()).await().indefinitely();
                    SkillManagementInspection finalInspection = service.inspectManagement().await().indefinitely();
                    return new SkillManagementBootstrapResult(
                            service,
                            resolved,
                            initialInspection,
                            reconcileResult,
                            finalInspection);
                },
                SkillManagementEventAttributes::bootstrap);
    }
}
