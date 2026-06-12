package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HermesSkillLineageRepairApprovalStoreTest {

    @Test
    void inMemoryStoreApprovesMatchingMutationRequest() {
        HermesSkillLineageRepairAdapterDispatchRequest request = request(
                HermesSkillLineageRepairAdapter.APPLY,
                true,
                "approval-001",
                "repair-key-001",
                "rustfs",
                "object-storage");
        HermesSkillLineageRepairApprovalStore store =
                HermesSkillLineageRepairApprovalStore.inMemory(List.of(
                        new HermesSkillLineageRepairApproval(
                                "approval-001",
                                HermesSkillLineageRepairAdapter.APPLY,
                                "repair-key-001",
                                "rustfs",
                                "object-storage",
                                true,
                                "operator-a",
                                "approved for test",
                                Map.of("ticket", "OPS-1"))));

        HermesSkillLineageRepairApprovalDecision decision = store.authorize(request);

        assertThat(decision)
                .returns(true, HermesSkillLineageRepairApprovalDecision::mutationRequested)
                .returns(true, HermesSkillLineageRepairApprovalDecision::approved)
                .returns("approved", HermesSkillLineageRepairApprovalDecision::status)
                .returns("approved for test", HermesSkillLineageRepairApprovalDecision::reason);
        assertThat(decision.toMetadata())
                .containsEntry("approved", true)
                .containsKey("approval");
        assertThat(store.toMetadata())
                .containsEntry("storeType", "in-memory")
                .containsEntry("approvalCount", 1);
    }

    @Test
    void inMemoryStoreRejectsMissingAndMismatchedApprovals() {
        HermesSkillLineageRepairApprovalStore store =
                HermesSkillLineageRepairApprovalStore.inMemory(List.of(
                        new HermesSkillLineageRepairApproval(
                                "approval-001",
                                HermesSkillLineageRepairAdapter.APPLY,
                                "repair-key-001",
                                "database",
                                "",
                                true,
                                "operator-a",
                                "database-only approval",
                                Map.of())));

        HermesSkillLineageRepairApprovalDecision missing = store.authorize(request(
                HermesSkillLineageRepairAdapter.APPLY,
                true,
                "approval-missing",
                "repair-key-001",
                "rustfs",
                "object-storage"));
        HermesSkillLineageRepairApprovalDecision mismatch = store.authorize(request(
                HermesSkillLineageRepairAdapter.APPLY,
                true,
                "approval-001",
                "repair-key-001",
                "rustfs",
                "object-storage"));

        assertThat(missing)
                .returns(false, HermesSkillLineageRepairApprovalDecision::approved)
                .returns("missing", HermesSkillLineageRepairApprovalDecision::status);
        assertThat(mismatch)
                .returns(false, HermesSkillLineageRepairApprovalDecision::approved)
                .returns("scope-mismatch", HermesSkillLineageRepairApprovalDecision::status);
    }

    @Test
    void noopStoreAllowsPreviewButRejectsMutation() {
        HermesSkillLineageRepairApprovalStore store = HermesSkillLineageRepairApprovalStore.noop();

        HermesSkillLineageRepairApprovalDecision preview = store.authorize(request(
                HermesSkillLineageRepairAdapter.PREVIEW,
                false,
                "",
                "repair-key-preview",
                "rustfs",
                "object-storage"));
        HermesSkillLineageRepairApprovalDecision apply = store.authorize(request(
                HermesSkillLineageRepairAdapter.APPLY,
                true,
                "approval-001",
                "repair-key-001",
                "rustfs",
                "object-storage"));

        assertThat(preview)
                .returns(false, HermesSkillLineageRepairApprovalDecision::mutationRequested)
                .returns(true, HermesSkillLineageRepairApprovalDecision::approved)
                .returns("not-required", HermesSkillLineageRepairApprovalDecision::status);
        assertThat(apply)
                .returns(true, HermesSkillLineageRepairApprovalDecision::mutationRequested)
                .returns(false, HermesSkillLineageRepairApprovalDecision::approved)
                .returns("not-configured", HermesSkillLineageRepairApprovalDecision::status);
    }

    private static HermesSkillLineageRepairAdapterDispatchRequest request(
            String action,
            boolean approved,
            String approvalId,
            String idempotencyKey,
            String backendId,
            String storageFamily) {
        HermesSkillLineageRepairOperationBatchSelection selection =
                HermesSkillLineageRepairOperationBatchSelection.from(
                        HermesSkillLineageRepairOperationBatchQuery.adapterReadyForBackend(backendId),
                        List.of(HermesSkillLineageRepairOperationBatch.from(
                                backendId,
                                storageFamily,
                                false,
                                true,
                                true,
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
                                        Map.of())))));
        return new HermesSkillLineageRepairAdapterDispatchRequest(
                action,
                selection,
                approved,
                approvalId,
                idempotencyKey,
                Map.of("source", "approval-store-test"));
    }
}
