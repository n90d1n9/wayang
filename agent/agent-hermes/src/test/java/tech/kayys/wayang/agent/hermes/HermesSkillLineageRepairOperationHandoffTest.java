package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.spi.skills.SkillDefinition;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HermesSkillLineageRepairOperationHandoffTest {

    @Test
    void rendersEmptyHandoffWhenExecutionIsMissing() {
        HermesSkillLineageRepairOperationHandoff handoff =
                HermesSkillLineageRepairOperationHandoff.from(null);

        assertThat(handoff)
                .returns(HermesSkillLineageRepairOperationHandoff.CURRENT_SCHEMA_VERSION,
                        HermesSkillLineageRepairOperationHandoff::schemaVersion)
                .returns(HermesSkillLineageRepairOperationHandoff.CURRENT_SCHEMA,
                        HermesSkillLineageRepairOperationHandoff::schema)
                .returns("empty", HermesSkillLineageRepairOperationHandoff::handoffStatus)
                .returns(false, HermesSkillLineageRepairOperationHandoff::adapterReady)
                .returns(0, HermesSkillLineageRepairOperationHandoff::operationCount)
                .returns(0, HermesSkillLineageRepairOperationHandoff::batchCount)
                .returns(0, HermesSkillLineageRepairOperationHandoff::adapterReadyBatchCount);
        assertThat(handoff.toMetadata())
                .containsEntry("schema", HermesSkillLineageRepairOperationHandoff.CURRENT_SCHEMA)
                .containsEntry("handoffStatus", "empty")
                .containsEntry("operationCount", 0)
                .containsEntry("batchCount", 0)
                .containsKey("operations")
                .containsKey("batches");
    }

    @Test
    void marksPreviewOnlyOperationsAsNotAdapterReady() {
        HermesSkillLineageRemediationExecution execution =
                HermesSkillLineageRemediationExecutor.defaultExecutor().dryRun(
                        orphanedCatalog(),
                        HermesSkillLineageRemediationPolicy.manual(
                                2,
                                List.of("all"),
                                List.of("lineage-root", "catalog")));

        HermesSkillLineageRepairOperationHandoff handoff = execution.repairOperationHandoff();

        assertThat(handoff)
                .returns("manual", HermesSkillLineageRepairOperationHandoff::policyMode)
                .returns("preview-only", HermesSkillLineageRepairOperationHandoff::handoffStatus)
                .returns(true, HermesSkillLineageRepairOperationHandoff::mutationAllowed)
                .returns(false, HermesSkillLineageRepairOperationHandoff::mutationSupported)
                .returns(false, HermesSkillLineageRepairOperationHandoff::adapterReady)
                .returns(2, HermesSkillLineageRepairOperationHandoff::operationCount)
                .returns(0, HermesSkillLineageRepairOperationHandoff::mutationReadyOperationCount)
                .returns(2, HermesSkillLineageRepairOperationHandoff::previewOnlyOperationCount)
                .returns(1, HermesSkillLineageRepairOperationHandoff::batchCount)
                .returns(0, HermesSkillLineageRepairOperationHandoff::adapterReadyBatchCount)
                .returns(1, HermesSkillLineageRepairOperationHandoff::previewOnlyBatchCount);
        assertThat(handoff.operations())
                .extracting(HermesSkillLineageRepairOperation::backendId)
                .containsExactly("database", "database");
        assertThat(handoff.batches().getFirst())
                .returns("database", HermesSkillLineageRepairOperationBatch::backendId)
                .returns("database", HermesSkillLineageRepairOperationBatch::storageFamily)
                .returns("preview-only", HermesSkillLineageRepairOperationBatch::batchStatus)
                .returns(false, HermesSkillLineageRepairOperationBatch::adapterReady)
                .returns(2, HermesSkillLineageRepairOperationBatch::operationCount);
        assertThat(handoff.toMetadata())
                .containsEntry("handoffStatus", "preview-only")
                .containsEntry("adapterReady", false)
                .containsEntry("previewOnlyOperationCount", 2)
                .containsEntry("batchCount", 1)
                .containsEntry("previewOnlyBatchCount", 1);
        assertThat(handoff.toMetadata().get("operations"))
                .asList()
                .hasSize(2);
        assertThat(handoff.toMetadata().get("batches"))
                .asList()
                .hasSize(1);
    }

    @Test
    void marksManualObjectStorageOperationsAsAwaitingApproval() {
        HermesSkillLineageRemediationExecutor executor = new HermesSkillLineageRemediationExecutor(
                HermesSkillLineageRepairBackendRegistry.from(List.of("rustfs"), List.of("rustfs")));
        HermesSkillLineageRemediationExecution execution = executor.dryRun(
                orphanedCatalog(),
                HermesSkillLineageRemediationPolicy.manual(
                        2,
                        List.of("all"),
                        List.of("lineage-root", "catalog")));

        HermesSkillLineageRepairOperationHandoff handoff = execution.repairOperationHandoff();

        assertThat(handoff)
                .returns("awaiting-approval", HermesSkillLineageRepairOperationHandoff::handoffStatus)
                .returns(true, HermesSkillLineageRepairOperationHandoff::approvalRequired)
                .returns(true, HermesSkillLineageRepairOperationHandoff::mutationAllowed)
                .returns(true, HermesSkillLineageRepairOperationHandoff::mutationSupported)
                .returns(true, HermesSkillLineageRepairOperationHandoff::adapterReady)
                .returns(2, HermesSkillLineageRepairOperationHandoff::mutationReadyOperationCount)
                .returns(0, HermesSkillLineageRepairOperationHandoff::previewOnlyOperationCount)
                .returns(1, HermesSkillLineageRepairOperationHandoff::batchCount)
                .returns(1, HermesSkillLineageRepairOperationHandoff::adapterReadyBatchCount);
        assertThat(handoff.handoffId())
                .isEqualTo("handoff-manual-awaiting-approval-2-2");
        assertThat(handoff.operations().getFirst())
                .returns("rustfs", HermesSkillLineageRepairOperation::backendId)
                .returns("object-storage", HermesSkillLineageRepairOperation::storageFamily)
                .returns(true, HermesSkillLineageRepairOperation::mutationReady);
        assertThat(handoff.batches().getFirst())
                .returns("batch-rustfs-object-storage-awaiting-approval-2-2",
                        HermesSkillLineageRepairOperationBatch::batchId)
                .returns("rustfs", HermesSkillLineageRepairOperationBatch::backendId)
                .returns("object-storage", HermesSkillLineageRepairOperationBatch::storageFamily)
                .returns("awaiting-approval", HermesSkillLineageRepairOperationBatch::batchStatus)
                .returns(true, HermesSkillLineageRepairOperationBatch::adapterReady)
                .returns(2, HermesSkillLineageRepairOperationBatch::mutationReadyOperationCount);
        assertThat(handoff.toMetadata())
                .containsEntry("handoffStatus", "awaiting-approval")
                .containsEntry("adapterReady", true)
                .containsEntry("mutationReadyOperationCount", 2)
                .containsEntry("batchCount", 1)
                .containsEntry("adapterReadyBatchCount", 1);
    }

    @Test
    void selectsBatchesForFutureAdapters() {
        HermesSkillLineageRemediationExecution previewExecution =
                HermesSkillLineageRemediationExecutor.defaultExecutor().dryRun(
                        orphanedCatalog(),
                        HermesSkillLineageRemediationPolicy.manual(
                                2,
                                List.of("all"),
                                List.of("lineage-root", "catalog")));
        HermesSkillLineageRepairOperationHandoff previewHandoff =
                previewExecution.repairOperationHandoff();

        HermesSkillLineageRepairOperationBatchSelection databaseSelection =
                previewHandoff.selectBatches(HermesSkillLineageRepairOperationBatchQuery.forBackend("database"));

        assertThat(databaseSelection)
                .returns("preview-only", HermesSkillLineageRepairOperationBatchSelection::selectionStatus)
                .returns(1, HermesSkillLineageRepairOperationBatchSelection::batchCount)
                .returns(2, HermesSkillLineageRepairOperationBatchSelection::operationCount)
                .returns(0, HermesSkillLineageRepairOperationBatchSelection::adapterReadyBatchCount)
                .returns(1, HermesSkillLineageRepairOperationBatchSelection::previewOnlyBatchCount);
        assertThat(databaseSelection.batches().getFirst())
                .returns("database", HermesSkillLineageRepairOperationBatch::backendId)
                .returns("database", HermesSkillLineageRepairOperationBatch::storageFamily);
        assertThat(databaseSelection.toMetadata())
                .containsEntry("selectionStatus", "preview-only")
                .containsEntry("batchCount", 1)
                .containsEntry("operationCount", 2)
                .containsKey("query");

        HermesSkillLineageRemediationExecutor rustfsExecutor = new HermesSkillLineageRemediationExecutor(
                HermesSkillLineageRepairBackendRegistry.from(List.of("rustfs"), List.of("rustfs")));
        HermesSkillLineageRepairOperationHandoff rustfsHandoff = rustfsExecutor.dryRun(
                orphanedCatalog(),
                HermesSkillLineageRemediationPolicy.manual(
                        2,
                        List.of("all"),
                        List.of("lineage-root", "catalog")))
                .repairOperationHandoff();

        HermesSkillLineageRepairOperationBatchSelection rustfsSelection =
                rustfsHandoff.selectBatches(
                        HermesSkillLineageRepairOperationBatchQuery.adapterReadyForBackend("rustfs"));
        HermesSkillLineageRepairOperationBatchSelection objectStorageSelection =
                rustfsHandoff.selectBatches(
                        HermesSkillLineageRepairOperationBatchQuery.forStorageFamily("object-storage"));
        HermesSkillLineageRepairOperationBatchSelection fileSelection =
                rustfsHandoff.selectBatches(HermesSkillLineageRepairOperationBatchQuery.forBackend("file-system"));

        assertThat(rustfsSelection)
                .returns("adapter-ready", HermesSkillLineageRepairOperationBatchSelection::selectionStatus)
                .returns(1, HermesSkillLineageRepairOperationBatchSelection::batchCount)
                .returns(2, HermesSkillLineageRepairOperationBatchSelection::operationCount)
                .returns(1, HermesSkillLineageRepairOperationBatchSelection::adapterReadyBatchCount);
        assertThat(objectStorageSelection.batches())
                .extracting(HermesSkillLineageRepairOperationBatch::backendId)
                .containsExactly("rustfs");
        assertThat(fileSelection)
                .returns("empty", HermesSkillLineageRepairOperationBatchSelection::selectionStatus)
                .returns(0, HermesSkillLineageRepairOperationBatchSelection::batchCount);
    }

    private static HermesSkillLineageCatalog orphanedCatalog() {
        return HermesSkillLineageCatalog.from(List.of(skill(
                "hermes-orphan-v2",
                "Archive audit evidence",
                Map.of(
                        "hermes.revision", "2",
                        "hermes.revisionStatus", "refined",
                        "hermes.lineageRootSkillId", "hermes-missing-root",
                        "hermes.lineageDepth", "2",
                        "hermes.sourceRequestIds", "req-c",
                        "hermes.mergeStrategy", HermesSkillRevisionMetadata.REFINEMENT_STRATEGY))));
    }

    private static SkillDefinition skill(String id, String task, Map<String, Object> metadata) {
        Map<String, Object> values = new java.util.LinkedHashMap<>(metadata);
        values.put("hermes.task", task);
        return SkillDefinition.builder()
                .id(id)
                .name(task)
                .description("Learned Hermes workflow for: " + task)
                .category(HermesAgentMode.LEARNED_SKILL_CATEGORY)
                .systemPrompt("Do the work.")
                .userPromptTemplate("{{instruction}}")
                .tools(List.of("rag"))
                .metadata(values)
                .build();
    }
}
