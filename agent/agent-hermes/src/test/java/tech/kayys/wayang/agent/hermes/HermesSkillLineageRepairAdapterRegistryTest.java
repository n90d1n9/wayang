package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tech.kayys.wayang.agent.spi.skills.SkillDefinition;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HermesSkillLineageRepairAdapterRegistryTest {

    @Test
    void routesAdapterReadySelectionToMatchingAdapter() {
        HermesSkillLineageRepairOperationBatch batch = adapterReadyBatch("rustfs", "object-storage");
        HermesSkillLineageRepairOperationBatchSelection selection =
                HermesSkillLineageRepairOperationBatchSelection.from(
                        HermesSkillLineageRepairOperationBatchQuery.adapterReadyForBackend("rustfs"),
                        List.of(batch));
        RecordingRepairAdapter adapter = new RecordingRepairAdapter(
                HermesSkillLineageRepairAdapterCapabilities.mutating(
                        "Object_Storage_Repair",
                        List.of("s3"),
                        List.of("object-storage"),
                        false));
        HermesSkillLineageRepairAdapterRegistry registry =
                HermesSkillLineageRepairAdapterRegistry.builder()
                        .register(adapter)
                        .build();

        HermesSkillLineageRepairAdapterDispatch dispatch = registry.preview(selection);

        assertThat(registry.resolve(HermesSkillLineageRepairAdapter.PREVIEW, batch))
                .contains(adapter);
        assertThat(registry.toMetadata())
                .containsEntry("adapterCount", 1)
                .containsKey("capabilities");
        assertThat(dispatch)
                .returns(HermesSkillLineageRepairAdapter.PREVIEW,
                        HermesSkillLineageRepairAdapterDispatch::action)
                .returns("dispatched", HermesSkillLineageRepairAdapterDispatch::dispatchStatus)
                .returns(1, HermesSkillLineageRepairAdapterDispatch::batchCount)
                .returns(1, HermesSkillLineageRepairAdapterDispatch::dispatchedBatchCount)
                .returns(1, HermesSkillLineageRepairAdapterDispatch::successfulBatchCount)
                .returns(0, HermesSkillLineageRepairAdapterDispatch::unsupportedBatchCount);
        assertThat(dispatch.results().getFirst())
                .returns("object-storage-repair", HermesSkillLineageRepairAdapterResult::adapterId)
                .returns("previewed", HermesSkillLineageRepairAdapterResult::status)
                .returns("rustfs", HermesSkillLineageRepairAdapterResult::backendId)
                .returns("object-storage", HermesSkillLineageRepairAdapterResult::storageFamily)
                .returns(false, HermesSkillLineageRepairAdapterResult::mutationAttempted);
        assertThat(dispatch.toMetadata())
                .containsEntry("dispatchStatus", "dispatched")
                .containsKey("results")
                .containsKey("metadata");
        assertThat(dispatch.metadata())
                .containsKey("registry")
                .containsKey("request");
    }

    @Test
    void appliesAndRollsBackWhenApprovalIsNotRequired() {
        HermesSkillLineageRepairOperationBatch batch = adapterReadyBatch("database", "database");
        HermesSkillLineageRepairOperationBatchSelection selection =
                HermesSkillLineageRepairOperationBatchSelection.from(
                        HermesSkillLineageRepairOperationBatchQuery.adapterReadyForBackend("database"),
                        List.of(batch));
        HermesSkillLineageRepairAdapterRegistry registry =
                HermesSkillLineageRepairAdapterRegistry.builder()
                        .register(new RecordingRepairAdapter(
                                HermesSkillLineageRepairAdapterCapabilities.mutating(
                                        "database-repair",
                                        List.of("database"),
                                        List.of("database"),
                                        false)))
                        .build();

        HermesSkillLineageRepairAdapterDispatch apply = registry.apply(selection);
        HermesSkillLineageRepairAdapterDispatch rollback = registry.rollback(selection);

        assertThat(apply)
                .returns(HermesSkillLineageRepairAdapter.APPLY,
                        HermesSkillLineageRepairAdapterDispatch::action)
                .returns("dispatched", HermesSkillLineageRepairAdapterDispatch::dispatchStatus)
                .returns(1, HermesSkillLineageRepairAdapterDispatch::successfulBatchCount);
        assertThat(apply.results().getFirst())
                .returns("applied", HermesSkillLineageRepairAdapterResult::status)
                .returns(true, HermesSkillLineageRepairAdapterResult::mutationAttempted)
                .returns(true, HermesSkillLineageRepairAdapterResult::successful);
        assertThat(rollback)
                .returns(HermesSkillLineageRepairAdapter.ROLLBACK,
                        HermesSkillLineageRepairAdapterDispatch::action)
                .returns("dispatched", HermesSkillLineageRepairAdapterDispatch::dispatchStatus);
        assertThat(rollback.results().getFirst())
                .returns("rolled-back", HermesSkillLineageRepairAdapterResult::status)
                .returns(true, HermesSkillLineageRepairAdapterResult::mutationAttempted);
    }

    @Test
    void blocksPreviewOnlyBatchesAndApprovalGatedMutations() {
        HermesSkillLineageRemediationExecution previewExecution =
                HermesSkillLineageRemediationExecutor.defaultExecutor().dryRun(
                        orphanedCatalog(),
                        HermesSkillLineageRemediationPolicy.manual(
                                2,
                                List.of("all"),
                                List.of("lineage-root", "catalog")));
        HermesSkillLineageRepairOperationBatchSelection previewOnlySelection =
                previewExecution.repairOperationHandoff()
                        .selectBatches(HermesSkillLineageRepairOperationBatchQuery.forBackend("database"));
        HermesSkillLineageRepairAdapterRegistry databaseRegistry =
                HermesSkillLineageRepairAdapterRegistry.builder()
                        .register(new RecordingRepairAdapter(
                                HermesSkillLineageRepairAdapterCapabilities.mutating(
                                        "database-repair",
                                        List.of("database"),
                                        List.of("database"),
                                        false)))
                        .build();

        HermesSkillLineageRepairAdapterDispatch previewOnly = databaseRegistry.preview(previewOnlySelection);

        assertThat(previewOnly)
                .returns("unsupported", HermesSkillLineageRepairAdapterDispatch::dispatchStatus)
                .returns(1, HermesSkillLineageRepairAdapterDispatch::unsupportedBatchCount)
                .returns(0, HermesSkillLineageRepairAdapterDispatch::dispatchedBatchCount);
        assertThat(previewOnly.results().getFirst())
                .returns("unavailable", HermesSkillLineageRepairAdapterResult::status)
                .returns(false, HermesSkillLineageRepairAdapterResult::dispatched);
        assertThat(previewOnly.results().getFirst().reason())
                .contains("not adapter-ready");

        HermesSkillLineageRemediationExecutor rustfsExecutor = new HermesSkillLineageRemediationExecutor(
                HermesSkillLineageRepairBackendRegistry.from(List.of("rustfs"), List.of("rustfs")));
        HermesSkillLineageRepairOperationBatchSelection approvalSelection = rustfsExecutor.dryRun(
                orphanedCatalog(),
                HermesSkillLineageRemediationPolicy.manual(
                        2,
                        List.of("all"),
                        List.of("lineage-root", "catalog")))
                .repairOperationHandoff()
                .selectBatches(HermesSkillLineageRepairOperationBatchQuery.adapterReadyForBackend("rustfs"));
        HermesSkillLineageRepairAdapterRegistry rustfsRegistry =
                HermesSkillLineageRepairAdapterRegistry.builder()
                        .register(new RecordingRepairAdapter(
                                HermesSkillLineageRepairAdapterCapabilities.mutating(
                                        "rustfs-repair",
                                        List.of("rustfs"),
                                        List.of("object-storage"),
                                        false)))
                        .build();

        HermesSkillLineageRepairAdapterDispatch preview = rustfsRegistry.preview(approvalSelection);
        HermesSkillLineageRepairAdapterDispatch apply = rustfsRegistry.apply(approvalSelection);

        assertThat(preview)
                .returns("dispatched", HermesSkillLineageRepairAdapterDispatch::dispatchStatus)
                .returns(1, HermesSkillLineageRepairAdapterDispatch::successfulBatchCount);
        assertThat(apply)
                .returns("unsupported", HermesSkillLineageRepairAdapterDispatch::dispatchStatus)
                .returns(1, HermesSkillLineageRepairAdapterDispatch::unsupportedBatchCount);
        assertThat(apply.results().getFirst().reason())
                .contains("requires approval");
    }

    @Test
    void approvedRequestCarriesApprovalAndIdempotencyThroughMutation() {
        HermesSkillLineageRemediationExecutor rustfsExecutor = new HermesSkillLineageRemediationExecutor(
                HermesSkillLineageRepairBackendRegistry.from(List.of("rustfs"), List.of("rustfs")));
        HermesSkillLineageRepairOperationBatchSelection selection = rustfsExecutor.dryRun(
                orphanedCatalog(),
                HermesSkillLineageRemediationPolicy.manual(
                        2,
                        List.of("all"),
                        List.of("lineage-root", "catalog")))
                .repairOperationHandoff()
                .selectBatches(HermesSkillLineageRepairOperationBatchQuery.adapterReadyForBackend("rustfs"));
        HermesSkillLineageRepairAdapterRegistry registry =
                HermesSkillLineageRepairAdapterRegistry.builder()
                        .register(new RecordingRepairAdapter(
                                HermesSkillLineageRepairAdapterCapabilities.mutating(
                                        "rustfs-repair",
                                        List.of("rustfs"),
                                        List.of("object-storage"),
                                        true)))
                        .build();
        HermesSkillLineageRepairAdapterDispatchRequest request =
                new HermesSkillLineageRepairAdapterDispatchRequest(
                        HermesSkillLineageRepairAdapter.APPLY,
                        selection,
                        true,
                        "approval-2026-06-03",
                        "repair-key-rustfs-001",
                        Map.of("requestedBy", "hermes-test"));

        HermesSkillLineageRepairAdapterDispatch dispatch = registry.dispatch(request);

        assertThat(request)
                .returns(true, HermesSkillLineageRepairAdapterDispatchRequest::mutationRequested)
                .returns(true, HermesSkillLineageRepairAdapterDispatchRequest::approvalSatisfied)
                .returns("repair-key-rustfs-001",
                        HermesSkillLineageRepairAdapterDispatchRequest::idempotencyKey);
        assertThat(request.toMetadata())
                .containsEntry("action", HermesSkillLineageRepairAdapter.APPLY)
                .containsEntry("approved", true)
                .containsEntry("approvalId", "approval-2026-06-03")
                .containsEntry("idempotencyKey", "repair-key-rustfs-001")
                .containsKey("selection")
                .containsKey("metadata");
        assertThat(dispatch)
                .returns(HermesSkillLineageRepairAdapter.APPLY,
                        HermesSkillLineageRepairAdapterDispatch::action)
                .returns("dispatched", HermesSkillLineageRepairAdapterDispatch::dispatchStatus)
                .returns(1, HermesSkillLineageRepairAdapterDispatch::successfulBatchCount);
        assertThat(metadataMap(dispatch.metadata(), "request"))
                .containsEntry("idempotencyKey", "repair-key-rustfs-001")
                .containsEntry("approvalId", "approval-2026-06-03")
                .containsEntry("approvalSatisfied", true);
        assertThat(dispatch.results().getFirst())
                .returns("applied", HermesSkillLineageRepairAdapterResult::status)
                .returns(true, HermesSkillLineageRepairAdapterResult::mutationAttempted)
                .returns(true, HermesSkillLineageRepairAdapterResult::successful);
        assertThat(dispatch.results().getFirst().metadata())
                .containsEntry("idempotencyKey", "repair-key-rustfs-001")
                .containsEntry("approvalId", "approval-2026-06-03")
                .containsKey("dispatchRequest");
        assertThat(metadataMap(dispatch.results().getFirst().metadata(), "dispatchRequest"))
                .containsEntry("approvalSatisfied", true)
                .containsEntry("mutationRequested", true)
                .containsEntry("selectionStatus", "adapter-ready");
    }

    @Test
    void ledgerReplaysDuplicateMutationsWithoutCallingAdapterAgain() {
        HermesSkillLineageRepairAdapterDispatchLedger ledger =
                HermesSkillLineageRepairAdapterDispatchLedger.inMemory();
        CountingRepairAdapter adapter = new CountingRepairAdapter(
                HermesSkillLineageRepairAdapterCapabilities.mutating(
                        "database-repair",
                        List.of("database"),
                        List.of("database"),
                        false));
        HermesSkillLineageRepairAdapterRegistry registry =
                HermesSkillLineageRepairAdapterRegistry.builder()
                        .dispatchLedger(ledger)
                        .register(adapter)
                        .build();
        HermesSkillLineageRepairOperationBatchSelection selection =
                HermesSkillLineageRepairOperationBatchSelection.from(
                        HermesSkillLineageRepairOperationBatchQuery.adapterReadyForBackend("database"),
                        List.of(adapterReadyBatch("database", "database")));
        HermesSkillLineageRepairAdapterDispatchRequest request =
                new HermesSkillLineageRepairAdapterDispatchRequest(
                        HermesSkillLineageRepairAdapter.APPLY,
                        selection,
                        true,
                        "approval-db-001",
                        "repeat-key-001",
                        Map.of());

        HermesSkillLineageRepairAdapterDispatch first = registry.dispatch(request);
        HermesSkillLineageRepairAdapterDispatch second = registry.dispatch(request);

        assertThat(adapter.applyCount()).isEqualTo(1);
        assertThat(ledger.recordCount()).isEqualTo(1);
        assertThat(ledger.find("repeat-key-001")).isPresent();
        assertThat(first)
                .returns("dispatched", HermesSkillLineageRepairAdapterDispatch::dispatchStatus)
                .returns(1, HermesSkillLineageRepairAdapterDispatch::successfulBatchCount);
        assertThat(first.metadata())
                .containsEntry("ledgerStatus", "recorded")
                .containsEntry("ledgerReplay", false)
                .containsEntry("idempotencyKey", "repeat-key-001")
                .containsKey("dispatchLedger");
        assertThat(metadataMap(first.metadata(), "dispatchLedger"))
                .containsEntry("ledgerType", "in-memory")
                .containsEntry("recordCount", 1)
                .containsEntry("replaySupported", true);
        assertThat(second)
                .returns("dispatched", HermesSkillLineageRepairAdapterDispatch::dispatchStatus)
                .returns(1, HermesSkillLineageRepairAdapterDispatch::successfulBatchCount);
        assertThat(second.metadata())
                .containsEntry("ledgerStatus", "replayed")
                .containsEntry("ledgerReplay", true)
                .containsEntry("idempotencyKey", "repeat-key-001");
        assertThat(second.results().getFirst())
                .returns("applied", HermesSkillLineageRepairAdapterResult::status)
                .returns(true, HermesSkillLineageRepairAdapterResult::mutationAttempted);
    }

    @Test
    void ledgerDoesNotRecordPreviewDispatches() {
        HermesSkillLineageRepairAdapterDispatchLedger ledger =
                HermesSkillLineageRepairAdapterDispatchLedger.inMemory();
        CountingRepairAdapter adapter = new CountingRepairAdapter(
                HermesSkillLineageRepairAdapterCapabilities.mutating(
                        "database-repair",
                        List.of("database"),
                        List.of("database"),
                        false));
        HermesSkillLineageRepairAdapterRegistry registry =
                HermesSkillLineageRepairAdapterRegistry.builder()
                        .dispatchLedger(ledger)
                        .register(adapter)
                        .build();
        HermesSkillLineageRepairOperationBatchSelection selection =
                HermesSkillLineageRepairOperationBatchSelection.from(
                        HermesSkillLineageRepairOperationBatchQuery.adapterReadyForBackend("database"),
                        List.of(adapterReadyBatch("database", "database")));

        HermesSkillLineageRepairAdapterDispatch first = registry.preview(selection);
        HermesSkillLineageRepairAdapterDispatch second = registry.preview(selection);

        assertThat(adapter.previewCount()).isEqualTo(2);
        assertThat(ledger.recordCount()).isZero();
        assertThat(first.metadata()).doesNotContainKey("ledgerStatus");
        assertThat(second.metadata()).doesNotContainKey("ledgerStatus");
    }

    @Test
    void fileSystemLedgerReplaysMutationsAcrossRegistryInstances(@TempDir Path tempDir) {
        Path ledgerPath = tempDir.resolve("repair-dispatch-ledger.jsonl");
        CountingRepairAdapter firstAdapter = new CountingRepairAdapter(
                HermesSkillLineageRepairAdapterCapabilities.mutating(
                        "database-repair",
                        List.of("database"),
                        List.of("database"),
                        false));
        HermesSkillLineageRepairAdapterRegistry firstRegistry =
                HermesSkillLineageRepairAdapterRegistry.builder()
                        .dispatchLedger(new FileSystemHermesSkillLineageRepairAdapterDispatchLedger(ledgerPath))
                        .register(firstAdapter)
                        .build();
        HermesSkillLineageRepairOperationBatchSelection selection =
                HermesSkillLineageRepairOperationBatchSelection.from(
                        HermesSkillLineageRepairOperationBatchQuery.adapterReadyForBackend("database"),
                        List.of(adapterReadyBatch("database", "database")));
        HermesSkillLineageRepairAdapterDispatchRequest request =
                new HermesSkillLineageRepairAdapterDispatchRequest(
                        HermesSkillLineageRepairAdapter.APPLY,
                        selection,
                        true,
                        "approval-file-001",
                        "file-ledger-key-001",
                        Map.of());

        HermesSkillLineageRepairAdapterDispatch first = firstRegistry.dispatch(request);

        CountingRepairAdapter secondAdapter = new CountingRepairAdapter(
                HermesSkillLineageRepairAdapterCapabilities.mutating(
                        "database-repair",
                        List.of("database"),
                        List.of("database"),
                        false));
        FileSystemHermesSkillLineageRepairAdapterDispatchLedger secondLedger =
                new FileSystemHermesSkillLineageRepairAdapterDispatchLedger(ledgerPath);
        HermesSkillLineageRepairAdapterRegistry secondRegistry =
                HermesSkillLineageRepairAdapterRegistry.builder()
                        .dispatchLedger(secondLedger)
                        .register(secondAdapter)
                        .build();

        HermesSkillLineageRepairAdapterDispatch second = secondRegistry.dispatch(request);

        assertThat(firstAdapter.applyCount()).isEqualTo(1);
        assertThat(secondAdapter.applyCount()).isZero();
        assertThat(secondLedger.recordCount()).isEqualTo(1);
        assertThat(secondLedger.find("file-ledger-key-001")).isPresent();
        assertThat(first.metadata())
                .containsEntry("ledgerStatus", "recorded")
                .containsEntry("ledgerReplay", false);
        assertThat(second.metadata())
                .containsEntry("ledgerStatus", "replayed")
                .containsEntry("ledgerReplay", true)
                .containsEntry("idempotencyKey", "file-ledger-key-001");
        assertThat(second.results().getFirst())
                .returns("applied", HermesSkillLineageRepairAdapterResult::status)
                .returns(true, HermesSkillLineageRepairAdapterResult::mutationAttempted);
        assertThat(Files.exists(ledgerPath)).isTrue();
    }

    @Test
    void fileSystemLedgerPrunesToConfiguredCapacity(@TempDir Path tempDir) {
        Path ledgerPath = tempDir.resolve("repair-dispatch-ledger.jsonl");
        FileSystemHermesSkillLineageRepairAdapterDispatchLedger ledger =
                new FileSystemHermesSkillLineageRepairAdapterDispatchLedger(ledgerPath, 1);
        CountingRepairAdapter adapter = new CountingRepairAdapter(
                HermesSkillLineageRepairAdapterCapabilities.mutating(
                        "database-repair",
                        List.of("database"),
                        List.of("database"),
                        false));
        HermesSkillLineageRepairAdapterRegistry registry =
                HermesSkillLineageRepairAdapterRegistry.builder()
                        .dispatchLedger(ledger)
                        .register(adapter)
                        .build();
        HermesSkillLineageRepairOperationBatchSelection selection =
                HermesSkillLineageRepairOperationBatchSelection.from(
                        HermesSkillLineageRepairOperationBatchQuery.adapterReadyForBackend("database"),
                        List.of(adapterReadyBatch("database", "database")));

        registry.dispatch(new HermesSkillLineageRepairAdapterDispatchRequest(
                HermesSkillLineageRepairAdapter.APPLY,
                selection,
                true,
                "approval-file-001",
                "file-ledger-key-001",
                Map.of()));
        registry.dispatch(new HermesSkillLineageRepairAdapterDispatchRequest(
                HermesSkillLineageRepairAdapter.APPLY,
                selection,
                true,
                "approval-file-002",
                "file-ledger-key-002",
                Map.of()));

        assertThat(adapter.applyCount()).isEqualTo(2);
        assertThat(ledger.recordCount()).isEqualTo(1);
        assertThat(ledger.find("file-ledger-key-001")).isEmpty();
        assertThat(ledger.find("file-ledger-key-002")).isPresent();
        assertThat(ledger.toMetadata())
                .containsEntry("ledgerType", "file-system")
                .containsEntry("recordCount", 1)
                .containsEntry("maxRecords", 1)
                .containsEntry("replaySupported", true);
    }

    @Test
    void builderResolvesDispatchLedgerFromModeConfig(@TempDir Path tempDir) {
        Path ledgerPath = tempDir.resolve("configured-dispatch-ledger.jsonl");
        HermesAgentModeConfig config = HermesAgentModeConfig.builder()
                .skillLineageRepairDispatchLedgerStore("file-system")
                .skillLineageRepairDispatchLedgerPath(ledgerPath.toString())
                .skillLineageRepairDispatchLedgerMaxRecords(7)
                .build();
        HermesSkillLineageRepairAdapterRegistry registry =
                HermesSkillLineageRepairAdapterRegistry.builder()
                        .dispatchLedger(config)
                        .build();

        assertThat(metadataMap(registry.toMetadata(), "dispatchLedger"))
                .containsEntry("ledgerType", "file-system")
                .containsEntry("ledgerPath", ledgerPath.toString())
                .containsEntry("maxRecords", 7)
                .containsEntry("replaySupported", true);
    }

    @Test
    void emptyRegistryReportsUnavailableDispatch() {
        HermesSkillLineageRepairOperationBatch batch = adapterReadyBatch("database", "database");
        HermesSkillLineageRepairOperationBatchSelection selection =
                HermesSkillLineageRepairOperationBatchSelection.from(
                        HermesSkillLineageRepairOperationBatchQuery.adapterReadyForBackend("database"),
                        List.of(batch));
        HermesSkillLineageRepairAdapterRegistry registry = HermesSkillLineageRepairAdapterRegistry.empty();

        HermesSkillLineageRepairAdapterDispatch dispatch = registry.preview(selection);

        assertThat(registry.isEmpty()).isTrue();
        assertThat(registry.toMetadata())
                .containsEntry("adapterCount", 0)
                .containsKey("capabilities");
        assertThat(dispatch)
                .returns("unsupported", HermesSkillLineageRepairAdapterDispatch::dispatchStatus)
                .returns(1, HermesSkillLineageRepairAdapterDispatch::unsupportedBatchCount);
        assertThat(dispatch.results().getFirst())
                .returns("repair-adapter-registry", HermesSkillLineageRepairAdapterResult::adapterId)
                .returns("unavailable", HermesSkillLineageRepairAdapterResult::status);
        assertThat(dispatch.results().getFirst().reason())
                .contains("no repair adapter registered");
    }

    @Test
    void nullAdapterResultReportsUnavailableInsteadOfThrowing() {
        HermesSkillLineageRepairOperationBatch batch = adapterReadyBatch("database", "database");
        HermesSkillLineageRepairOperationBatchSelection selection =
                HermesSkillLineageRepairOperationBatchSelection.from(
                        HermesSkillLineageRepairOperationBatchQuery.adapterReadyForBackend("database"),
                        List.of(batch));
        HermesSkillLineageRepairAdapterRegistry registry =
                HermesSkillLineageRepairAdapterRegistry.builder()
                        .register(new NullResultRepairAdapter(
                                HermesSkillLineageRepairAdapterCapabilities.previewOnly(
                                        "null-result-repair",
                                        List.of("database"),
                                        List.of("database"))))
                        .build();

        HermesSkillLineageRepairAdapterDispatch dispatch = registry.preview(selection);

        assertThat(dispatch)
                .returns("unsupported", HermesSkillLineageRepairAdapterDispatch::dispatchStatus)
                .returns(1, HermesSkillLineageRepairAdapterDispatch::unsupportedBatchCount);
        assertThat(dispatch.results().getFirst())
                .returns("null-result-repair", HermesSkillLineageRepairAdapterResult::adapterId)
                .returns("unavailable", HermesSkillLineageRepairAdapterResult::status)
                .returns(false, HermesSkillLineageRepairAdapterResult::dispatched);
        assertThat(dispatch.results().getFirst().reason())
                .contains("returned no dispatch result");
    }


    private static HermesSkillLineageRepairOperationBatch adapterReadyBatch(
            String backendId,
            String storageFamily) {
        return HermesSkillLineageRepairOperationBatch.from(
                backendId,
                storageFamily,
                false,
                true,
                false,
                List.of(new HermesSkillLineageRepairOperation(
                        "",
                        "intent-001",
                        backendId,
                        storageFamily,
                        "restore-lineage-root-definition",
                        "lineage-root",
                        "hermes-root",
                        true,
                        true,
                        true,
                        false,
                        "",
                        "",
                        Map.of())));
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

    @SuppressWarnings("unchecked")
    private static Map<String, Object> metadataMap(Map<String, Object> metadata, String key) {
        return (Map<String, Object>) metadata.get(key);
    }

    private record RecordingRepairAdapter(
            HermesSkillLineageRepairAdapterCapabilities capabilities)
            implements HermesSkillLineageRepairAdapter {

        @Override
        public HermesSkillLineageRepairAdapterResult preview(
                HermesSkillLineageRepairOperationBatch batch) {
            return result(HermesSkillLineageRepairAdapter.PREVIEW, batch, "previewed");
        }

        @Override
        public HermesSkillLineageRepairAdapterResult apply(
                HermesSkillLineageRepairOperationBatch batch) {
            return result(HermesSkillLineageRepairAdapter.APPLY, batch, "applied");
        }

        @Override
        public HermesSkillLineageRepairAdapterResult rollback(
                HermesSkillLineageRepairOperationBatch batch) {
            return result(HermesSkillLineageRepairAdapter.ROLLBACK, batch, "rolled-back");
        }

        private HermesSkillLineageRepairAdapterResult result(
                String action,
                HermesSkillLineageRepairOperationBatch batch,
                String status) {
            return HermesSkillLineageRepairAdapterResult.dispatched(
                    adapterId(),
                    action,
                    batch,
                    status,
                    "recording adapter accepted batch",
                    Map.of(
                            "capabilities", capabilities.toMetadata(),
                            "batch", batch.toMetadata()));
        }
    }

    private record NullResultRepairAdapter(
            HermesSkillLineageRepairAdapterCapabilities capabilities)
            implements HermesSkillLineageRepairAdapter {

        @Override
        public HermesSkillLineageRepairAdapterResult preview(
                HermesSkillLineageRepairOperationBatch batch) {
            return null;
        }
    }

    private static final class CountingRepairAdapter implements HermesSkillLineageRepairAdapter {

        private final HermesSkillLineageRepairAdapterCapabilities capabilities;
        private int previewCount;
        private int applyCount;
        private int rollbackCount;

        private CountingRepairAdapter(HermesSkillLineageRepairAdapterCapabilities capabilities) {
            this.capabilities = capabilities;
        }

        @Override
        public HermesSkillLineageRepairAdapterCapabilities capabilities() {
            return capabilities;
        }

        @Override
        public HermesSkillLineageRepairAdapterResult preview(
                HermesSkillLineageRepairOperationBatch batch) {
            previewCount++;
            return result(HermesSkillLineageRepairAdapter.PREVIEW, batch, "previewed");
        }

        @Override
        public HermesSkillLineageRepairAdapterResult apply(
                HermesSkillLineageRepairOperationBatch batch) {
            applyCount++;
            return result(HermesSkillLineageRepairAdapter.APPLY, batch, "applied");
        }

        @Override
        public HermesSkillLineageRepairAdapterResult rollback(
                HermesSkillLineageRepairOperationBatch batch) {
            rollbackCount++;
            return result(HermesSkillLineageRepairAdapter.ROLLBACK, batch, "rolled-back");
        }

        private int previewCount() {
            return previewCount;
        }

        private int applyCount() {
            return applyCount;
        }

        @SuppressWarnings("unused")
        private int rollbackCount() {
            return rollbackCount;
        }

        private HermesSkillLineageRepairAdapterResult result(
                String action,
                HermesSkillLineageRepairOperationBatch batch,
                String status) {
            return HermesSkillLineageRepairAdapterResult.dispatched(
                    adapterId(),
                    action,
                    batch,
                    status,
                    "counting adapter accepted batch",
                    Map.of("batch", batch.toMetadata()));
        }
    }
}
