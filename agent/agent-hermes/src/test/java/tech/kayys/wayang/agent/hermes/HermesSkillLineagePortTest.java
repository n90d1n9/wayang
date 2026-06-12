package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tech.kayys.wayang.agent.skills.management.FileSystemSkillDefinitionStore;
import tech.kayys.wayang.agent.skills.management.InMemorySkillArtifactStore;
import tech.kayys.wayang.agent.skills.management.InMemorySkillLifecycleStateStore;
import tech.kayys.wayang.agent.skills.management.SkillDefinitionStoreInspector;
import tech.kayys.wayang.agent.skills.management.SkillLifecycleStateStoreInspector;
import tech.kayys.wayang.agent.skills.management.SkillManagementEventSink;
import tech.kayys.wayang.agent.skills.management.SkillManagementService;
import tech.kayys.wayang.agent.spi.skills.SkillDefinition;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HermesSkillLineagePortTest {

    @Test
    void directiveBuildsStableTargetsAndMetadata() {
        HermesSkillLineageDirective directive = HermesSkillLineageDirective.inspect("hermes-audit");
        HermesSkillLineageDirective catalog = HermesSkillLineageDirective.catalog();
        HermesSkillLineageDirective repairPreview = HermesSkillLineageDirective.repairPreview();
        HermesSkillLineageDirective repairApply =
                HermesSkillLineageDirective.repairApply("approval-001", "repair-key-001");
        HermesSkillLineageDirective inactive = HermesSkillLineageDirective.none();
        HermesSkillLineageDirective blank = HermesSkillLineageDirective.inspect(" ");

        assertThat(directive.active()).isTrue();
        assertThat(directive.operation()).isEqualTo("inspect");
        assertThat(directive.target()).isEqualTo("skill:hermes-audit");
        assertThat(directive.toMetadata())
                .containsEntry("active", true)
                .containsEntry("operation", "inspect")
                .containsEntry("skillId", "hermes-audit")
                .containsEntry("target", "skill:hermes-audit");
        assertThat(catalog.active()).isTrue();
        assertThat(catalog.operation()).isEqualTo("catalog");
        assertThat(catalog.target()).isEqualTo("learned-skills");
        assertThat(catalog.toMetadata())
                .containsEntry("active", true)
                .containsEntry("operation", "catalog")
                .containsEntry("skillId", "")
                .containsEntry("target", "learned-skills");
        assertThat(repairPreview.active()).isTrue();
        assertThat(repairPreview.operation()).isEqualTo("repair-preview");
        assertThat(repairPreview.target()).isEqualTo("learned-skills:repair-preview");
        assertThat(repairPreview.toMetadata())
                .containsEntry("active", true)
                .containsEntry("operation", "repair-preview")
                .containsEntry("skillId", "")
                .containsEntry("target", "learned-skills:repair-preview");
        assertThat(repairApply.active()).isTrue();
        assertThat(repairApply.operation()).isEqualTo("repair-apply");
        assertThat(repairApply.target()).isEqualTo("learned-skills:repair-apply");
        assertThat(repairApply.toMetadata())
                .containsEntry("repairAction", "apply")
                .containsEntry("repairApproved", true)
                .containsEntry("repairApprovalId", "approval-001")
                .containsEntry("repairIdempotencyKey", "repair-key-001");
        assertThat(inactive.active()).isFalse();
        assertThat(inactive.operation()).isEqualTo("none");
        assertThat(blank.active()).isFalse();
    }

    @Test
    void serviceBackedPortReturnsLineageViewMetadata(@TempDir Path tempDir) {
        SkillManagementService service = service(tempDir);
        service.createSkill(skill("hermes-audit", Map.of(
                "hermes.task", "Audit MCP server health",
                "hermes.revision", "2",
                "hermes.previousRevision", "1",
                "hermes.revisionStatus", "refined",
                "hermes.lineageRootSkillId", "hermes-audit",
                "hermes.lineageDepth", "2",
                "hermes.sourceRequestIds", "req-a,req-b",
                "hermes.mergeStrategy", HermesSkillRevisionMetadata.REFINEMENT_STRATEGY)))
                .await()
                .indefinitely();
        HermesSkillLineagePort port = HermesSkillLineagePort.service(new HermesLearnedSkillRepository(
                service,
                new HermesSkillMarkdownRenderer()));

        HermesPortDispatchResult result = port.inspect(HermesSkillLineageDirective.inspect("hermes-audit"));

        assertThat(result.port()).isEqualTo("skill-lineage");
        assertThat(result.status()).isEqualTo("inspected");
        assertThat(result.successful()).isTrue();
        assertThat(result.metadata())
                .containsEntry("found", true)
                .containsEntry("rootSkillId", "hermes-audit")
                .containsEntry("currentSkillId", "hermes-audit")
                .containsEntry("currentRevision", "2")
                .containsEntry("entryCount", 1)
                .containsEntry("hasRefinements", true)
                .containsKey("skillLineageView");
        assertThat(metadataMap(result.metadata(), "skillLineageView"))
                .containsEntry("found", true)
                .containsEntry("currentRevision", "2")
                .containsEntry("entryCount", 1);
    }

    @Test
    void serviceBackedPortReturnsLineageCatalogMetadata(@TempDir Path tempDir) {
        SkillManagementService service = service(tempDir);
        service.createSkill(skill("hermes-audit", Map.of(
                "hermes.task", "Audit MCP server health",
                "hermes.revision", "1",
                "hermes.revisionStatus", "initial",
                "hermes.lineageRootSkillId", "hermes-audit",
                "hermes.lineageDepth", "1",
                "hermes.sourceRequestIds", "req-a",
                "hermes.mergeStrategy", HermesSkillRevisionMetadata.INITIAL_STRATEGY)))
                .await()
                .indefinitely();
        service.createSkill(skill("hermes-audit-v2", Map.of(
                "hermes.task", "Audit MCP server health and export report",
                "hermes.revision", "2",
                "hermes.previousRevision", "1",
                "hermes.revisionStatus", "refined",
                "hermes.lineageRootSkillId", "hermes-audit",
                "hermes.lineageDepth", "2",
                "hermes.sourceRequestIds", "req-a,req-b",
                "hermes.mergeStrategy", HermesSkillRevisionMetadata.REFINEMENT_STRATEGY)))
                .await()
                .indefinitely();
        HermesSkillLineagePort port = HermesSkillLineagePort.service(new HermesLearnedSkillRepository(
                service,
                new HermesSkillMarkdownRenderer()));

        HermesPortDispatchResult result = port.inspect(HermesSkillLineageDirective.catalog());

        assertThat(result.port()).isEqualTo("skill-lineage");
        assertThat(result.operation()).isEqualTo("catalog");
        assertThat(result.target()).isEqualTo("learned-skills");
        assertThat(result.status()).isEqualTo("cataloged");
        assertThat(result.successful()).isTrue();
        assertThat(result.metadata())
                .containsEntry("learnedSkillCount", 2)
                .containsEntry("rootCount", 1)
                .containsEntry("refinedRootCount", 1L)
                .containsEntry("refinedEntryCount", 1L)
                .containsEntry("orphanedRootCount", 0L)
                .containsEntry("lineageHealthStatus", "evolving")
                .containsEntry("lineageAttentionRequired", false)
                .containsEntry("lineageRemediationRequired", false)
                .containsEntry("lineageRemediationPolicyMode", "dry-run")
                .containsEntry("lineageRemediationPolicyPermittedActionCount", 0)
                .containsEntry("lineageRepairIntentCount", 0)
                .containsEntry("lineageRepairBackendMutationSupported", false)
                .containsEntry("lineageRepairOperationCount", 0)
                .containsEntry("lineageRepairMutationReadyOperationCount", 0)
                .containsEntry("lineageRepairPreviewOnlyOperationCount", 0)
                .containsEntry("lineageRepairUnsupportedOperationCount", 0)
                .containsEntry("lineageRepairOperationBatchCount", 0)
                .containsEntry("lineageRepairAdapterReadyBatchCount", 0)
                .containsEntry("skillStoreConsistencyStatus", "advisory")
                .containsEntry("skillStoreConsistent", true)
                .containsEntry("lineageDryRunActionCount", 1)
                .containsKey("skillLineageHealth")
                .containsKey("skillLineageRemediationPlan")
                .containsKey("skillLineageRemediationPolicy")
                .containsKey("skillLineageRepairIntentPlan")
                .containsKey("skillLineageRepairBackendPlan")
                .containsKey("skillLineageRepairOperationHandoff")
                .containsKey("skillLineageRepairIntentPreview")
                .containsKey("skillLineageRepairOperationPreview")
                .containsKey("skillStoreConsistencyReport")
                .containsKey("skillLineageRemediationDryRun")
                .containsKey("skillLineageCatalog");
        assertThat(metadataMap(result.metadata(), "skillLineageCatalog"))
                .containsEntry("learnedSkillCount", 2)
                .containsEntry("rootCount", 1)
                .containsEntry("refinedRootCount", 1L)
                .containsKey("health")
                .containsKey("consistencyReport");
        assertThat(metadataMap(result.metadata(), "skillLineageHealth"))
                .containsEntry("status", "evolving")
                .containsEntry("attentionRequired", false)
                .containsEntry("refinedRootCount", 1L)
                .containsKey("remediationPlan");
        assertThat(metadataMap(result.metadata(), "skillLineageRemediationPlan"))
                .containsEntry("required", false)
                .containsEntry("strategy", "review-refined-skill-quality")
                .containsEntry("advisoryActionCount", 1);
        assertThat(metadataMap(result.metadata(), "skillLineageRemediationPolicy"))
                .containsEntry("mode", "dry-run")
                .containsEntry("mutationAllowed", false);
        assertThat(metadataMap(result.metadata(), "skillStoreConsistencyReport"))
                .containsEntry("status", "advisory")
                .containsEntry("consistent", true)
                .containsEntry("issueCount", 1);
        assertThat(metadataMap(result.metadata(), "skillLineageRemediationDryRun"))
                .containsEntry("dryRun", true)
                .containsEntry("approvalRequired", false)
                .containsEntry("mutationSupported", false)
                .containsEntry("status", "dry-run-advisory")
                .containsEntry("proposedActionCount", 1)
                .containsEntry("policyPermittedActionCount", 0)
                .containsEntry("executedActionCount", 0)
                .containsEntry("skippedActionCount", 1)
                .containsKey("repairIntentPlan");
        assertThat(metadataMap(result.metadata(), "skillLineageRepairIntentPlan"))
                .containsEntry("intentCount", 0)
                .containsEntry("permittedActionCount", 0)
                .containsEntry("mutationSupported", false);
        assertThat(metadataMap(result.metadata(), "skillLineageRepairBackendPlan"))
                .containsEntry("backendCount", 3)
                .containsEntry("intentCount", 0)
                .containsEntry("mutationSupported", false);
        assertThat(metadataMap(result.metadata(), "skillLineageRepairOperationHandoff"))
                .containsEntry("handoffStatus", "empty")
                .containsEntry("operationCount", 0)
                .containsEntry("batchCount", 0)
                .containsEntry("adapterReady", false);
        assertThat((String) result.metadata().get("skillLineageRepairIntentPreview"))
                .contains("Status: `dry-run-advisory`")
                .contains("Repair actions are visible in the remediation plan");
        assertThat((String) result.metadata().get("skillLineageRepairOperationPreview"))
                .contains("Status: `dry-run-advisory`")
                .contains("Operation count: 0")
                .contains("Repair actions are visible");
    }

    @Test
    void serviceBackedPortReportsConfiguredRemediationPolicy(@TempDir Path tempDir) {
        SkillManagementService service = service(tempDir);
        service.createSkill(skill("hermes-orphan-v2", Map.of(
                "hermes.task", "Archive audit evidence",
                "hermes.revision", "2",
                "hermes.revisionStatus", "refined",
                "hermes.lineageRootSkillId", "hermes-missing-root",
                "hermes.lineageDepth", "2",
                "hermes.sourceRequestIds", "req-c",
                "hermes.mergeStrategy", HermesSkillRevisionMetadata.REFINEMENT_STRATEGY)))
                .await()
                .indefinitely();
        HermesSkillLineageRemediationPolicy policy = HermesSkillLineageRemediationPolicy.manual(
                2,
                List.of("all"),
                List.of("lineage-root", "catalog"));
        HermesSkillLineagePort port = HermesSkillLineagePort.service(
                new HermesLearnedSkillRepository(service, new HermesSkillMarkdownRenderer()),
                policy);

        HermesPortDispatchResult result = port.inspect(HermesSkillLineageDirective.catalog());

        assertThat(result.metadata())
                .containsEntry("lineageHealthStatus", "attention")
                .containsEntry("lineageRemediationPolicyMode", "manual")
                .containsEntry("lineageRemediationPolicyPermittedActionCount", 2)
                .containsEntry("lineageRepairIntentCount", 2)
                .containsEntry("lineageRepairBackendMutationSupported", false)
                .containsEntry("lineageRepairOperationCount", 2)
                .containsEntry("lineageRepairMutationReadyOperationCount", 0)
                .containsEntry("lineageRepairPreviewOnlyOperationCount", 2)
                .containsEntry("lineageRepairUnsupportedOperationCount", 0)
                .containsEntry("lineageRepairOperationBatchCount", 1)
                .containsEntry("lineageRepairAdapterReadyBatchCount", 0)
                .containsEntry("lineageDryRunActionCount", 2);
        assertThat(metadataMap(result.metadata(), "skillLineageRemediationDryRun"))
                .containsEntry("dryRun", true)
                .containsEntry("mutationAllowed", true)
                .containsEntry("mutationSupported", false)
                .containsEntry("approvalRequired", true)
                .containsEntry("status", "dry-run-repair-policy-ready")
                .containsEntry("policyPermittedActionCount", 2)
                .containsEntry("executedActionCount", 0)
                .containsKey("repairIntentPlan");
        assertThat(metadataMap(result.metadata(), "skillLineageRepairIntentPlan"))
                .containsEntry("intentCount", 2)
                .containsEntry("permittedActionCount", 2)
                .containsEntry("approvalRequired", true)
                .containsEntry("mutationSupported", false);
        assertThat(metadataMap(result.metadata(), "skillLineageRepairIntentPlan").get("intents"))
                .asList()
                .hasSize(2);
        assertThat(metadataMap(result.metadata(), "skillLineageRepairBackendPlan"))
                .containsEntry("backendCount", 3)
                .containsEntry("intentCount", 2)
                .containsEntry("commandSupportedIntentCount", 2)
                .containsEntry("previewOnlyIntentCount", 2)
                .containsEntry("operationCount", 2)
                .containsEntry("previewOnlyOperationCount", 2)
                .containsEntry("mutationSupported", false);
        assertThat(metadataMap(result.metadata(), "skillLineageRepairOperationHandoff"))
                .containsEntry("handoffStatus", "preview-only")
                .containsEntry("operationCount", 2)
                .containsEntry("batchCount", 1)
                .containsEntry("previewOnlyBatchCount", 1)
                .containsEntry("adapterReady", false);
        assertThat((String) result.metadata().get("skillLineageRepairIntentPreview"))
                .contains("Status: `dry-run-repair-policy-ready`")
                .contains("`restore-lineage-root-definition` -> `hermes-missing-root`")
                .contains("Approval is required before any future mutating adapter may apply this plan.");
        assertThat((String) result.metadata().get("skillLineageRepairOperationPreview"))
                .contains("Operation count: 2")
                .contains("`restore-lineage-root-definition` via `database` -> `hermes-missing-root`")
                .contains("Backend status: `preview-only`");
    }

    @Test
    void serviceBackedPortUsesConfiguredRepairBackendRegistry(@TempDir Path tempDir) {
        SkillManagementService service = service(tempDir);
        service.createSkill(skill("hermes-orphan-v2", Map.of(
                "hermes.task", "Archive audit evidence",
                "hermes.revision", "2",
                "hermes.revisionStatus", "refined",
                "hermes.lineageRootSkillId", "hermes-missing-root",
                "hermes.lineageDepth", "2",
                "hermes.sourceRequestIds", "req-c",
                "hermes.mergeStrategy", HermesSkillRevisionMetadata.REFINEMENT_STRATEGY)))
                .await()
                .indefinitely();
        HermesSkillLineageRemediationPolicy policy = HermesSkillLineageRemediationPolicy.manual(
                2,
                List.of("all"),
                List.of("lineage-root", "catalog"));
        HermesSkillLineagePort port = HermesSkillLineagePort.service(
                new HermesLearnedSkillRepository(service, new HermesSkillMarkdownRenderer()),
                policy,
                HermesSkillLineageRepairBackendRegistry.from(List.of("rustfs"), List.of("rustfs")));

        HermesPortDispatchResult result = port.inspect(HermesSkillLineageDirective.catalog());

        assertThat(result.metadata())
                .containsEntry("lineageRepairIntentCount", 2)
                .containsEntry("lineageRepairBackendMutationSupported", true)
                .containsEntry("lineageRepairOperationCount", 2)
                .containsEntry("lineageRepairMutationReadyOperationCount", 2)
                .containsEntry("lineageRepairPreviewOnlyOperationCount", 0)
                .containsEntry("lineageRepairOperationBatchCount", 1)
                .containsEntry("lineageRepairAdapterReadyBatchCount", 1);
        assertThat(metadataMap(result.metadata(), "skillLineageRemediationDryRun"))
                .containsEntry("mutationAllowed", true)
                .containsEntry("mutationSupported", true)
                .containsEntry("executedActionCount", 0);
        assertThat(metadataMap(result.metadata(), "skillLineageRepairBackendPlan"))
                .containsEntry("backendCount", 1)
                .containsEntry("intentCount", 2)
                .containsEntry("mutationSupported", true)
                .containsEntry("mutationSupportedIntentCount", 2)
                .containsEntry("operationCount", 2)
                .containsEntry("mutationReadyOperationCount", 2);
        assertThat(metadataMap(result.metadata(), "skillLineageRepairOperationHandoff"))
                .containsEntry("handoffStatus", "awaiting-approval")
                .containsEntry("operationCount", 2)
                .containsEntry("batchCount", 1)
                .containsEntry("adapterReadyBatchCount", 1)
                .containsEntry("adapterReady", true);
        assertThat((String) result.metadata().get("skillLineageRepairOperationPreview"))
                .contains("Mutation-ready operations: 2")
                .contains("via `rustfs`")
                .contains("Storage family: `object-storage`");
    }

    @Test
    void serviceBackedPortReportsRepairAdapterRegistryMetadata(@TempDir Path tempDir) {
        SkillManagementService service = service(tempDir);
        service.createSkill(skill("hermes-orphan-v2", Map.of(
                "hermes.task", "Archive audit evidence",
                "hermes.revision", "2",
                "hermes.revisionStatus", "refined",
                "hermes.lineageRootSkillId", "hermes-missing-root",
                "hermes.lineageDepth", "2",
                "hermes.sourceRequestIds", "req-c",
                "hermes.mergeStrategy", HermesSkillRevisionMetadata.REFINEMENT_STRATEGY)))
                .await()
                .indefinitely();
        HermesSkillLineageRepairAdapterRegistry adapterRegistry =
                HermesSkillLineageRepairAdapterRegistry.builder()
                        .dispatchLedger(HermesSkillLineageRepairAdapterDispatchLedger.inMemory())
                        .register(HermesSkillLineageRepairAdapter.previewOnly(
                                "rustfs-repair",
                                "rustfs",
                                "object-storage"))
                        .build();
        HermesSkillLineagePort port = HermesSkillLineagePort.service(
                new HermesLearnedSkillRepository(service, new HermesSkillMarkdownRenderer()),
                HermesSkillLineageRemediationPolicy.manual(
                        2,
                        List.of("all"),
                        List.of("lineage-root", "catalog")),
                HermesSkillLineageRepairBackendRegistry.from(List.of("rustfs"), List.of("rustfs")),
                adapterRegistry);

        HermesPortDispatchResult result = port.inspect(HermesSkillLineageDirective.catalog());

        assertThat(result.metadata())
                .containsEntry("lineageRepairAdapterCount", 1)
                .containsKey("skillLineageRepairAdapterRegistry");
        assertThat(metadataMap(result.metadata(), "skillLineageRepairAdapterRegistry"))
                .containsEntry("adapterCount", 1)
                .containsKey("dispatchLedger");
        assertThat(metadataMap(
                metadataMap(result.metadata(), "skillLineageRepairAdapterRegistry"),
                "dispatchLedger"))
                .containsEntry("ledgerType", "in-memory")
                .containsEntry("replaySupported", true);
    }

    @Test
    void serviceBackedPortPreviewsAdapterReadyRepairDispatch(@TempDir Path tempDir) {
        SkillManagementService service = service(tempDir);
        service.createSkill(skill("hermes-orphan-v2", Map.of(
                "hermes.task", "Archive audit evidence",
                "hermes.revision", "2",
                "hermes.revisionStatus", "refined",
                "hermes.lineageRootSkillId", "hermes-missing-root",
                "hermes.lineageDepth", "2",
                "hermes.sourceRequestIds", "req-c",
                "hermes.mergeStrategy", HermesSkillRevisionMetadata.REFINEMENT_STRATEGY)))
                .await()
                .indefinitely();
        HermesSkillLineageRepairAdapterRegistry adapterRegistry =
                HermesSkillLineageRepairAdapterRegistry.builder()
                        .register(HermesSkillLineageRepairAdapter.previewOnly(
                                "rustfs-repair",
                                "rustfs",
                                "object-storage"))
                        .build();
        HermesSkillLineagePort port = HermesSkillLineagePort.service(
                new HermesLearnedSkillRepository(service, new HermesSkillMarkdownRenderer()),
                HermesSkillLineageRemediationPolicy.manual(
                        2,
                        List.of("all"),
                        List.of("lineage-root", "catalog")),
                HermesSkillLineageRepairBackendRegistry.from(List.of("rustfs"), List.of("rustfs")),
                adapterRegistry);

        HermesPortDispatchResult result = port.inspect(HermesSkillLineageDirective.repairPreview());

        assertThat(result)
                .returns("skill-lineage", HermesPortDispatchResult::port)
                .returns("repair-preview", HermesPortDispatchResult::operation)
                .returns("learned-skills:repair-preview", HermesPortDispatchResult::target)
                .returns(true, HermesPortDispatchResult::successful)
                .returns("repair-preview-dispatched", HermesPortDispatchResult::status)
                .returns("skill lineage repair preview dispatched", HermesPortDispatchResult::reason);
        assertThat(result.metadata())
                .containsEntry("lineageRepairAdapterReadyBatchCount", 1)
                .containsEntry("lineageRepairSelectedBatchCount", 1)
                .containsEntry("lineageRepairDispatchStatus", "dispatched")
                .containsEntry("lineageRepairDispatchBatchCount", 1)
                .containsEntry("lineageRepairDispatchedBatchCount", 1)
                .containsEntry("lineageRepairSuccessfulBatchCount", 1)
                .containsEntry("lineageRepairUnsupportedBatchCount", 0)
                .containsEntry("lineageRepairAdapterCount", 1)
                .containsKey("skillLineageRepairBatchSelection")
                .containsKey("skillLineageRepairAdapterDispatch")
                .containsKey("skillLineageRepairAdapterRegistry");
        assertThat(metadataMap(result.metadata(), "skillLineageRepairBatchSelection"))
                .containsEntry("selectionStatus", "adapter-ready")
                .containsEntry("adapterReadyBatchCount", 1)
                .containsEntry("operationCount", 2);
        assertThat(metadataMap(result.metadata(), "skillLineageRepairAdapterDispatch"))
                .containsEntry("action", "preview")
                .containsEntry("dispatchStatus", "dispatched")
                .containsEntry("successfulBatchCount", 1);
    }

    @Test
    void serviceBackedPortBlocksUnapprovedRepairApply(@TempDir Path tempDir) {
        SkillManagementService service = orphanedSkillService(tempDir);
        CountingRepairAdapter adapter = new CountingRepairAdapter(
                HermesSkillLineageRepairAdapterCapabilities.mutating(
                        "rustfs-repair",
                        List.of("rustfs"),
                        List.of("object-storage"),
                        true));
        HermesSkillLineagePort port = HermesSkillLineagePort.service(
                new HermesLearnedSkillRepository(service, new HermesSkillMarkdownRenderer()),
                HermesSkillLineageRemediationPolicy.manual(
                        2,
                        List.of("all"),
                        List.of("lineage-root", "catalog")),
                HermesSkillLineageRepairBackendRegistry.from(List.of("rustfs"), List.of("rustfs")),
                HermesSkillLineageRepairAdapterRegistry.builder()
                        .register(adapter)
                        .build());

        HermesPortDispatchResult result = port.inspect(new HermesSkillLineageDirective(
                true,
                HermesSkillLineageDirective.REPAIR_APPLY,
                "",
                "",
                "skill lineage repair apply requested",
                HermesSkillLineageRepairAdapter.APPLY,
                false,
                "",
                "repair-key-unapproved",
                "",
                "",
                true));

        assertThat(result)
                .returns(false, HermesPortDispatchResult::successful)
                .returns("repair-apply-unsupported", HermesPortDispatchResult::status)
                .returns("skill lineage repair apply unavailable", HermesPortDispatchResult::reason);
        assertThat(adapter.applyCount()).isZero();
        assertThat(result.metadata())
                .containsEntry("lineageRepairDispatchAction", "apply")
                .containsEntry("lineageRepairApprovalStatus", "not-configured")
                .containsEntry("lineageRepairApprovalSatisfied", false)
                .containsEntry("lineageRepairDispatchStatus", "unsupported")
                .containsEntry("lineageRepairUnsupportedBatchCount", 1);
        assertThat(metadataMap(result.metadata(), "skillLineageRepairAdapterDispatchRequest"))
                .containsEntry("approved", false)
                .containsEntry("approvalSatisfied", false)
                .containsEntry("idempotencyKey", "repair-key-unapproved");
        assertThat(metadataMap(result.metadata(), "skillLineageRepairApprovalDecision"))
                .containsEntry("status", "not-configured")
                .containsEntry("approved", false)
                .containsEntry("reason", "repair approval store not configured");
    }

    @Test
    void serviceBackedPortAppliesApprovedRepairOnceAndReplaysDuplicate(@TempDir Path tempDir) {
        SkillManagementService service = orphanedSkillService(tempDir);
        CountingRepairAdapter adapter = new CountingRepairAdapter(
                HermesSkillLineageRepairAdapterCapabilities.mutating(
                        "rustfs-repair",
                        List.of("rustfs"),
                        List.of("object-storage"),
                        true));
        HermesSkillLineagePort port = HermesSkillLineagePort.service(
                new HermesLearnedSkillRepository(service, new HermesSkillMarkdownRenderer()),
                HermesSkillLineageRemediationPolicy.manual(
                        2,
                        List.of("all"),
                        List.of("lineage-root", "catalog")),
                HermesSkillLineageRepairBackendRegistry.from(List.of("rustfs"), List.of("rustfs")),
                HermesSkillLineageRepairAdapterRegistry.builder()
                        .dispatchLedger(HermesSkillLineageRepairAdapterDispatchLedger.inMemory())
                        .register(adapter)
                        .build(),
                HermesSkillLineageRepairApprovalStore.inMemory(List.of(
                        HermesSkillLineageRepairApproval.approved(
                                "approval-001",
                                HermesSkillLineageRepairAdapter.APPLY,
                                "repair-key-approved"))));
        HermesSkillLineageDirective directive = HermesSkillLineageDirective.repairApply(
                "approval-001",
                "repair-key-approved");

        HermesPortDispatchResult first = port.inspect(directive);
        HermesPortDispatchResult second = port.inspect(directive);

        assertThat(adapter.applyCount()).isEqualTo(1);
        assertThat(first)
                .returns(true, HermesPortDispatchResult::successful)
                .returns("repair-apply-dispatched", HermesPortDispatchResult::status)
                .returns("skill lineage repair apply dispatched", HermesPortDispatchResult::reason);
        assertThat(first.metadata())
                .containsEntry("lineageRepairApprovalStatus", "approved")
                .containsEntry("lineageRepairApprovalSatisfied", true);
        assertThat(second)
                .returns(true, HermesPortDispatchResult::successful)
                .returns("repair-apply-dispatched", HermesPortDispatchResult::status);
        assertThat(metadataMap(
                metadataMap(first.metadata(), "skillLineageRepairAdapterDispatch"),
                "metadata"))
                .containsEntry("ledgerStatus", "recorded")
                .containsEntry("ledgerReplay", false)
                .containsEntry("idempotencyKey", "repair-key-approved");
        assertThat(metadataMap(
                metadataMap(second.metadata(), "skillLineageRepairAdapterDispatch"),
                "metadata"))
                .containsEntry("ledgerStatus", "replayed")
                .containsEntry("ledgerReplay", true)
                .containsEntry("idempotencyKey", "repair-key-approved");
    }

    @Test
    void serviceBackedPortReportsMissingSkill(@TempDir Path tempDir) {
        HermesSkillLineagePort port = HermesSkillLineagePort.service(new HermesLearnedSkillRepository(
                service(tempDir),
                new HermesSkillMarkdownRenderer()));

        HermesPortDispatchResult result = port.inspect(HermesSkillLineageDirective.inspect("missing"));

        assertThat(result.successful()).isFalse();
        assertThat(result.status()).isEqualTo("missing");
        assertThat(result.reason()).isEqualTo("learned skill not found");
        assertThat(result.metadata())
                .containsEntry("found", false)
                .containsEntry("entryCount", 0)
                .containsKey("skillLineageView");
    }

    private static SkillManagementService orphanedSkillService(Path tempDir) {
        SkillManagementService service = service(tempDir);
        service.createSkill(skill("hermes-orphan-v2", Map.of(
                "hermes.task", "Archive audit evidence",
                "hermes.revision", "2",
                "hermes.revisionStatus", "refined",
                "hermes.lineageRootSkillId", "hermes-missing-root",
                "hermes.lineageDepth", "2",
                "hermes.sourceRequestIds", "req-c",
                "hermes.mergeStrategy", HermesSkillRevisionMetadata.REFINEMENT_STRATEGY)))
                .await()
                .indefinitely();
        return service;
    }

    private static SkillManagementService service(Path tempDir) {
        return new SkillManagementService(
                new FileSystemSkillDefinitionStore(tempDir.resolve("definitions")),
                new SkillDefinitionStoreInspector(),
                new InMemorySkillLifecycleStateStore(),
                new SkillLifecycleStateStoreInspector(),
                new InMemorySkillArtifactStore(),
                SkillManagementEventSink.noop());
    }

    private static SkillDefinition skill(String id, Map<String, Object> metadata) {
        return SkillDefinition.builder()
                .id(id)
                .name("Audit MCP server health")
                .description("Learned Hermes workflow for: Audit MCP server health")
                .category(HermesAgentMode.LEARNED_SKILL_CATEGORY)
                .systemPrompt("Do the work.")
                .userPromptTemplate("{{instruction}}")
                .tools(List.of("rag"))
                .metadata(metadata)
                .build();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> metadataMap(Map<String, Object> metadata, String key) {
        return (Map<String, Object>) metadata.get(key);
    }

    private static final class CountingRepairAdapter implements HermesSkillLineageRepairAdapter {

        private final HermesSkillLineageRepairAdapterCapabilities capabilities;
        private int applyCount;

        private CountingRepairAdapter(HermesSkillLineageRepairAdapterCapabilities capabilities) {
            this.capabilities = capabilities;
        }

        @Override
        public HermesSkillLineageRepairAdapterCapabilities capabilities() {
            return capabilities;
        }

        @Override
        public HermesSkillLineageRepairAdapterResult apply(
                HermesSkillLineageRepairOperationBatch batch) {
            applyCount++;
            return HermesSkillLineageRepairAdapterResult.dispatched(
                    adapterId(),
                    HermesSkillLineageRepairAdapter.APPLY,
                    batch,
                    "applied",
                    "counting adapter accepted batch",
                    Map.of("batch", batch.toMetadata()));
        }

        private int applyCount() {
            return applyCount;
        }
    }
}
