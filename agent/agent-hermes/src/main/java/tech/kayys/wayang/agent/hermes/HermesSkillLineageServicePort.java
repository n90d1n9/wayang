package tech.kayys.wayang.agent.hermes;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Skill-lineage port backed by the learned-skill repository.
 */
public final class HermesSkillLineageServicePort implements HermesSkillLineagePort {

    private final HermesLearnedSkillRepository repository;
    private final HermesSkillLineageRemediationPolicy remediationPolicy;
    private final HermesSkillLineageRemediationExecutor remediationExecutor;
    private final HermesSkillLineageRepairAdapterRegistry repairAdapterRegistry;
    private final HermesSkillLineageRepairApprovalStore repairApprovalStore;
    private final HermesSkillLineageRepairIntentMarkdownRenderer repairIntentRenderer;
    private final HermesSkillLineageRepairOperationMarkdownRenderer repairOperationRenderer;

    public HermesSkillLineageServicePort(HermesLearnedSkillRepository repository) {
        this(repository, HermesSkillLineageRemediationPolicy.dryRun());
    }

    public HermesSkillLineageServicePort(
            HermesLearnedSkillRepository repository,
            HermesSkillLineageRemediationPolicy remediationPolicy) {
        this(repository, remediationPolicy, HermesSkillLineageRepairBackendRegistry.defaultRegistry());
    }

    public HermesSkillLineageServicePort(
            HermesLearnedSkillRepository repository,
            HermesSkillLineageRemediationPolicy remediationPolicy,
            HermesSkillLineageRepairBackendRegistry repairBackendRegistry) {
        this(
                repository,
                remediationPolicy,
                repairBackendRegistry,
                HermesSkillLineageRepairAdapterRegistry.empty(),
                HermesSkillLineageRepairApprovalStore.noop(),
                new HermesSkillLineageRepairIntentMarkdownRenderer(),
                new HermesSkillLineageRepairOperationMarkdownRenderer());
    }

    public HermesSkillLineageServicePort(
            HermesLearnedSkillRepository repository,
            HermesSkillLineageRemediationPolicy remediationPolicy,
            HermesSkillLineageRepairBackendRegistry repairBackendRegistry,
            HermesSkillLineageRepairAdapterRegistry repairAdapterRegistry) {
        this(
                repository,
                remediationPolicy,
                repairBackendRegistry,
                repairAdapterRegistry,
                HermesSkillLineageRepairApprovalStore.noop(),
                new HermesSkillLineageRepairIntentMarkdownRenderer(),
                new HermesSkillLineageRepairOperationMarkdownRenderer());
    }

    public HermesSkillLineageServicePort(
            HermesLearnedSkillRepository repository,
            HermesSkillLineageRemediationPolicy remediationPolicy,
            HermesSkillLineageRepairBackendRegistry repairBackendRegistry,
            HermesSkillLineageRepairAdapterRegistry repairAdapterRegistry,
            HermesSkillLineageRepairApprovalStore repairApprovalStore) {
        this(
                repository,
                remediationPolicy,
                repairBackendRegistry,
                repairAdapterRegistry,
                repairApprovalStore,
                new HermesSkillLineageRepairIntentMarkdownRenderer(),
                new HermesSkillLineageRepairOperationMarkdownRenderer());
    }

    public HermesSkillLineageServicePort(
            HermesLearnedSkillRepository repository,
            HermesSkillLineageRemediationPolicy remediationPolicy,
            HermesSkillLineageRepairBackendRegistry repairBackendRegistry,
            HermesSkillLineageRepairIntentMarkdownRenderer repairIntentRenderer) {
        this(
                repository,
                remediationPolicy,
                repairBackendRegistry,
                HermesSkillLineageRepairAdapterRegistry.empty(),
                HermesSkillLineageRepairApprovalStore.noop(),
                repairIntentRenderer,
                new HermesSkillLineageRepairOperationMarkdownRenderer());
    }

    public HermesSkillLineageServicePort(
            HermesLearnedSkillRepository repository,
            HermesSkillLineageRemediationPolicy remediationPolicy,
            HermesSkillLineageRepairBackendRegistry repairBackendRegistry,
            HermesSkillLineageRepairAdapterRegistry repairAdapterRegistry,
            HermesSkillLineageRepairIntentMarkdownRenderer repairIntentRenderer,
            HermesSkillLineageRepairOperationMarkdownRenderer repairOperationRenderer) {
        this(
                repository,
                remediationPolicy,
                repairBackendRegistry,
                repairAdapterRegistry,
                HermesSkillLineageRepairApprovalStore.noop(),
                repairIntentRenderer,
                repairOperationRenderer);
    }

    public HermesSkillLineageServicePort(
            HermesLearnedSkillRepository repository,
            HermesSkillLineageRemediationPolicy remediationPolicy,
            HermesSkillLineageRepairBackendRegistry repairBackendRegistry,
            HermesSkillLineageRepairAdapterRegistry repairAdapterRegistry,
            HermesSkillLineageRepairApprovalStore repairApprovalStore,
            HermesSkillLineageRepairIntentMarkdownRenderer repairIntentRenderer,
            HermesSkillLineageRepairOperationMarkdownRenderer repairOperationRenderer) {
        this.repository = repository;
        this.remediationPolicy = remediationPolicy == null
                ? HermesSkillLineageRemediationPolicy.dryRun()
                : remediationPolicy;
        this.remediationExecutor = new HermesSkillLineageRemediationExecutor(repairBackendRegistry);
        this.repairAdapterRegistry = repairAdapterRegistry == null
                ? HermesSkillLineageRepairAdapterRegistry.empty()
                : repairAdapterRegistry;
        this.repairApprovalStore = repairApprovalStore == null
                ? HermesSkillLineageRepairApprovalStore.noop()
                : repairApprovalStore;
        this.repairIntentRenderer = repairIntentRenderer == null
                ? new HermesSkillLineageRepairIntentMarkdownRenderer()
                : repairIntentRenderer;
        this.repairOperationRenderer = repairOperationRenderer == null
                ? new HermesSkillLineageRepairOperationMarkdownRenderer()
                : repairOperationRenderer;
    }

    @Override
    public HermesPortDispatchResult inspect(HermesSkillLineageDirective directive) {
        HermesSkillLineageDirective resolved = directive == null
                ? HermesSkillLineageDirective.none()
                : directive;
        if (HermesSkillLineageDirective.CATALOG.equals(resolved.operation())) {
            return catalog(resolved);
        }
        if (repairDispatchOperation(resolved.operation())) {
            return repairDispatch(resolved);
        }
        HermesSkillLineageView view = repository == null
                ? HermesSkillLineageView.missing(resolved.skillId())
                : repository.lineage(resolved.skillId()).await().indefinitely();
        Map<String, Object> metadata = new LinkedHashMap<>(resolved.toMetadata());
        metadata.put("skillLineageView", view.toMetadata());
        metadata.put("found", view.found());
        metadata.put("rootSkillId", view.rootSkillId());
        metadata.put("currentSkillId", view.currentSkillId());
        metadata.put("currentRevision", view.currentRevision());
        metadata.put("entryCount", view.entryCount());
        metadata.put("hasRefinements", view.hasRefinements());
        return new HermesPortDispatchResult(
                HermesRuntimePortCatalog.SKILL_LINEAGE,
                resolved.operation(),
                resolved.target(),
                true,
                true,
                view.found(),
                view.found() ? "inspected" : "missing",
                view.found() ? "skill lineage inspected" : "learned skill not found",
                metadata);
    }

    private HermesPortDispatchResult repairDispatch(HermesSkillLineageDirective directive) {
        HermesSkillLineageCatalog catalog = repository == null
                ? HermesSkillLineageCatalog.empty()
                : repository.lineageCatalog().await().indefinitely();
        HermesSkillLineageHealth health = catalog.health();
        HermesSkillLineageRemediationExecution remediationDryRun =
                remediationExecutor.dryRun(catalog, remediationPolicy);
        HermesSkillLineageRepairOperationHandoff handoff = remediationDryRun.repairOperationHandoff();
        HermesSkillLineageRepairOperationBatchSelection selection = handoff.selectBatches(
                new HermesSkillLineageRepairOperationBatchQuery(
                        directive.repairBackendId(),
                        directive.repairStorageFamily(),
                        "",
                        directive.repairAdapterReadyOnly()));
        HermesSkillLineageRepairAdapterDispatchRequest request =
                new HermesSkillLineageRepairAdapterDispatchRequest(
                        directive.repairAction(),
                        selection,
                        directive.repairApproved(),
                        directive.repairApprovalId(),
                        directive.repairIdempotencyKey(),
                        repairDispatchRequestMetadata(directive));
        HermesSkillLineageRepairApprovalDecision approvalDecision =
                repairApprovalStore.authorize(request);
        HermesSkillLineageRepairAdapterDispatch dispatch = approvalDecision.approved()
                ? repairAdapterRegistry.dispatch(request)
                : approvalBlockedDispatch(request, selection, approvalDecision);
        boolean successful = dispatchSuccessful(dispatch);
        Map<String, Object> metadata = new LinkedHashMap<>(directive.toMetadata());
        metadata.put("skillLineageHealth", health.toMetadata());
        metadata.put("skillLineageRemediationPolicy", remediationPolicy.toMetadata());
        metadata.put("skillLineageRemediationDryRun", remediationDryRun.toMetadata());
        metadata.put("skillLineageRepairOperationHandoff", handoff.toMetadata());
        metadata.put("skillLineageRepairBatchSelection", selection.toMetadata());
        metadata.put("skillLineageRepairAdapterDispatchRequest", request.toMetadata());
        metadata.put("skillLineageRepairApprovalDecision", approvalDecision.toMetadata());
        metadata.put("skillLineageRepairApprovalStore", repairApprovalStore.toMetadata());
        metadata.put("skillLineageRepairAdapterDispatch", dispatch.toMetadata());
        metadata.put("skillLineageRepairAdapterRegistry", repairAdapterRegistry.toMetadata());
        metadata.put("lineageHealthStatus", health.status());
        metadata.put("lineageRemediationPolicyMode", remediationPolicy.mode());
        metadata.put("lineageRepairOperationBatchCount", handoff.batchCount());
        metadata.put("lineageRepairAdapterReadyBatchCount", handoff.adapterReadyBatchCount());
        metadata.put("lineageRepairSelectedBatchCount", selection.batchCount());
        metadata.put("lineageRepairSelectedOperationCount", selection.operationCount());
        metadata.put("lineageRepairApprovalStatus", approvalDecision.status());
        metadata.put("lineageRepairApprovalSatisfied", approvalDecision.approved());
        metadata.put("lineageRepairDispatchAction", dispatch.action());
        metadata.put("lineageRepairDispatchStatus", dispatch.dispatchStatus());
        metadata.put("lineageRepairDispatchBatchCount", dispatch.batchCount());
        metadata.put("lineageRepairDispatchedBatchCount", dispatch.dispatchedBatchCount());
        metadata.put("lineageRepairSuccessfulBatchCount", dispatch.successfulBatchCount());
        metadata.put("lineageRepairUnsupportedBatchCount", dispatch.unsupportedBatchCount());
        metadata.put("lineageRepairAdapterCount", repairAdapterRegistry.adapterCount());
        return new HermesPortDispatchResult(
                HermesRuntimePortCatalog.SKILL_LINEAGE,
                directive.operation(),
                directive.target(),
                true,
                true,
                successful,
                "repair-" + dispatch.action() + "-" + dispatch.dispatchStatus(),
                repairDispatchReason(selection, dispatch),
                metadata);
    }

    private HermesPortDispatchResult catalog(HermesSkillLineageDirective directive) {
        HermesSkillLineageCatalog catalog = repository == null
                ? HermesSkillLineageCatalog.empty()
                : repository.lineageCatalog().await().indefinitely();
        HermesSkillLineageHealth health = catalog.health();
        HermesSkillLineageRemediationPlan remediationPlan = health.remediationPlan();
        HermesSkillStoreConsistencyReport consistencyReport = catalog.consistencyReport();
        HermesSkillLineageRemediationExecution remediationDryRun =
                remediationExecutor.dryRun(catalog, remediationPolicy);
        Map<String, Object> metadata = new LinkedHashMap<>(directive.toMetadata());
        metadata.put("skillLineageCatalog", catalog.toMetadata());
        metadata.put("skillLineageHealth", health.toMetadata());
        metadata.put("skillLineageRemediationPlan", remediationPlan.toMetadata());
        metadata.put("skillLineageRemediationPolicy", remediationPolicy.toMetadata());
        metadata.put("skillStoreConsistencyReport", consistencyReport.toMetadata());
        metadata.put("skillLineageRemediationDryRun", remediationDryRun.toMetadata());
        metadata.put("skillLineageRepairIntentPlan", remediationDryRun.repairIntentPlan().toMetadata());
        metadata.put("skillLineageRepairBackendPlan", remediationDryRun.repairBackendPlan().toMetadata());
        metadata.put("skillLineageRepairOperationHandoff",
                remediationDryRun.repairOperationHandoff().toMetadata());
        metadata.put("skillLineageRepairAdapterRegistry", repairAdapterRegistry.toMetadata());
        metadata.put("skillLineageRepairIntentPreview", repairIntentRenderer.render(remediationDryRun));
        metadata.put("skillLineageRepairOperationPreview", repairOperationRenderer.render(remediationDryRun));
        metadata.put("lineageHealthStatus", health.status());
        metadata.put("lineageAttentionRequired", health.attentionRequired());
        metadata.put("lineageRemediationRequired", remediationPlan.required());
        metadata.put("lineageRemediationPolicyMode", remediationPolicy.mode());
        metadata.put("lineageRemediationPolicyPermittedActionCount",
                remediationDryRun.policyPermittedActionCount());
        metadata.put("lineageRepairIntentCount", remediationDryRun.repairIntentPlan().intentCount());
        metadata.put("lineageRepairBackendMutationSupported",
                remediationDryRun.repairBackendPlan().mutationSupported());
        metadata.put("lineageRepairOperationCount", remediationDryRun.repairBackendPlan().operationCount());
        metadata.put("lineageRepairMutationReadyOperationCount",
                remediationDryRun.repairBackendPlan().mutationReadyOperationCount());
        metadata.put("lineageRepairPreviewOnlyOperationCount",
                remediationDryRun.repairBackendPlan().previewOnlyOperationCount());
        metadata.put("lineageRepairUnsupportedOperationCount",
                remediationDryRun.repairBackendPlan().unsupportedOperationCount());
        metadata.put("lineageRepairOperationBatchCount",
                remediationDryRun.repairOperationHandoff().batchCount());
        metadata.put("lineageRepairAdapterReadyBatchCount",
                remediationDryRun.repairOperationHandoff().adapterReadyBatchCount());
        metadata.put("lineageRepairAdapterCount", repairAdapterRegistry.adapterCount());
        metadata.put("skillStoreConsistencyStatus", consistencyReport.status());
        metadata.put("skillStoreConsistent", consistencyReport.consistent());
        metadata.put("lineageDryRunActionCount", remediationDryRun.proposedActionCount());
        metadata.put("learnedSkillCount", catalog.learnedSkillCount());
        metadata.put("rootCount", catalog.rootCount());
        metadata.put("refinedRootCount", catalog.refinedRootCount());
        metadata.put("refinedEntryCount", catalog.refinedEntryCount());
        metadata.put("orphanedRootCount", catalog.orphanedRootCount());
        return new HermesPortDispatchResult(
                HermesRuntimePortCatalog.SKILL_LINEAGE,
                directive.operation(),
                directive.target(),
                true,
                true,
                true,
                "cataloged",
                "skill lineage catalog inspected",
                metadata);
    }

    private static boolean repairDispatchOperation(String operation) {
        return HermesSkillLineageDirective.REPAIR_PREVIEW.equals(operation)
                || HermesSkillLineageDirective.REPAIR_APPLY.equals(operation)
                || HermesSkillLineageDirective.REPAIR_ROLLBACK.equals(operation);
    }

    private static Map<String, Object> repairDispatchRequestMetadata(
            HermesSkillLineageDirective directive) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("source", "skill-lineage-port");
        metadata.put("operation", directive.operation());
        metadata.put("target", directive.target());
        metadata.put("backendId", directive.repairBackendId());
        metadata.put("storageFamily", directive.repairStorageFamily());
        metadata.put("adapterReadyOnly", directive.repairAdapterReadyOnly());
        return Map.copyOf(metadata);
    }

    private static HermesSkillLineageRepairAdapterDispatch approvalBlockedDispatch(
            HermesSkillLineageRepairAdapterDispatchRequest request,
            HermesSkillLineageRepairOperationBatchSelection selection,
            HermesSkillLineageRepairApprovalDecision approvalDecision) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("request", request.toMetadata());
        metadata.put("approvalDecision", approvalDecision.toMetadata());
        return HermesSkillLineageRepairAdapterDispatch.from(
                request.action(),
                selection.batches().stream()
                        .map(batch -> HermesSkillLineageRepairAdapterResult.unavailable(
                                "repair-approval-store",
                                request.action(),
                                batch,
                                approvalDecision.reason(),
                                Map.of(
                                        "dispatchRequest", request.toResultMetadata(),
                                        "approvalDecision", approvalDecision.toMetadata())))
                        .toList(),
                metadata);
    }

    private static boolean dispatchSuccessful(HermesSkillLineageRepairAdapterDispatch dispatch) {
        return switch (dispatch.dispatchStatus()) {
            case "failed", "unsupported" -> false;
            default -> true;
        };
    }

    private static String repairDispatchReason(
            HermesSkillLineageRepairOperationBatchSelection selection,
            HermesSkillLineageRepairAdapterDispatch dispatch) {
        if (selection.batchCount() == 0) {
            return "no adapter-ready lineage repair batches available";
        }
        if (dispatch.dispatchedBatchCount() > 0) {
            return "skill lineage repair " + dispatch.action() + " dispatched";
        }
        return "skill lineage repair " + dispatch.action() + " unavailable";
    }
}
