package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.spi.skills.SkillDefinition;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HermesSkillLineageRemediationExecutorTest {

    @Test
    void dryRunPreviewsRequiredRepairsWithoutMutating() {
        SkillDefinition orphaned = skill(
                "hermes-orphan-v2",
                "Archive audit evidence",
                Map.of(
                        "hermes.revision", "2",
                        "hermes.revisionStatus", "refined",
                        "hermes.lineageRootSkillId", "hermes-missing-root",
                        "hermes.lineageDepth", "2",
                        "hermes.sourceRequestIds", "req-c",
                        "hermes.mergeStrategy", HermesSkillRevisionMetadata.REFINEMENT_STRATEGY));
        HermesSkillLineageCatalog catalog = HermesSkillLineageCatalog.from(List.of(orphaned));

        HermesSkillLineageRemediationExecution execution =
                HermesSkillLineageRemediationExecutor.defaultExecutor().dryRun(catalog);

        assertThat(execution)
                .returns(true, HermesSkillLineageRemediationExecution::dryRun)
                .returns(false, HermesSkillLineageRemediationExecution::mutationAllowed)
                .returns(false, HermesSkillLineageRemediationExecution::mutationSupported)
                .returns("dry-run-repair-required", HermesSkillLineageRemediationExecution::status)
                .returns(2, HermesSkillLineageRemediationExecution::proposedActionCount)
                .returns(0, HermesSkillLineageRemediationExecution::executedActionCount)
                .returns(2, HermesSkillLineageRemediationExecution::skippedActionCount);
        assertThat(execution.remediationPlan().required()).isTrue();
        assertThat(execution.repairIntentPlan().intentCount()).isZero();
        assertThat(execution.repairBackendPlan())
                .returns(3, HermesSkillLineageRepairBackendPlan::backendCount)
                .returns(0, HermesSkillLineageRepairBackendPlan::intentCount)
                .returns(false, HermesSkillLineageRepairBackendPlan::mutationSupported);
        assertThat(execution.consistencyReport())
                .returns("inconsistent", HermesSkillStoreConsistencyReport::status)
                .returns(false, HermesSkillStoreConsistencyReport::consistent)
                .returns(true, HermesSkillStoreConsistencyReport::attentionRequired);
        assertThat(execution.toMetadata())
                .containsEntry("dryRun", true)
                .containsEntry("mutationAllowed", false)
                .containsEntry("mutationSupported", false)
                .containsEntry("approvalRequired", false)
                .containsEntry("status", "dry-run-repair-required")
                .containsEntry("policyPermittedActionCount", 0)
                .containsEntry("executedActionCount", 0)
                .containsKey("consistencyReport")
                .containsKey("policy")
                .containsKey("repairIntentPlan")
                .containsKey("repairBackendPlan")
                .containsKey("repairOperationHandoff")
                .containsKey("remediationPlan");
        assertThat(metadataMap(execution.toMetadata(), "repairIntentPlan"))
                .containsEntry("intentCount", 0)
                .containsEntry("permittedActionCount", 0)
                .containsEntry("mutationSupported", false);
        assertThat(metadataMap(execution.toMetadata(), "repairBackendPlan"))
                .containsEntry("backendCount", 3)
                .containsEntry("intentCount", 0)
                .containsEntry("mutationSupported", false);
        assertThat(metadataMap(execution.toMetadata(), "repairOperationHandoff"))
                .containsEntry("handoffStatus", "empty")
                .containsEntry("operationCount", 0)
                .containsEntry("batchCount", 0)
                .containsEntry("adapterReady", false);
    }

    @Test
    void dryRunReportsPolicyReadyRepairsWithoutExecutingUnsupportedMutation() {
        SkillDefinition orphaned = skill(
                "hermes-orphan-v2",
                "Archive audit evidence",
                Map.of(
                        "hermes.revision", "2",
                        "hermes.revisionStatus", "refined",
                        "hermes.lineageRootSkillId", "hermes-missing-root",
                        "hermes.lineageDepth", "2",
                        "hermes.sourceRequestIds", "req-c",
                        "hermes.mergeStrategy", HermesSkillRevisionMetadata.REFINEMENT_STRATEGY));
        HermesSkillLineageCatalog catalog = HermesSkillLineageCatalog.from(List.of(orphaned));
        HermesSkillLineageRemediationPolicy policy = HermesSkillLineageRemediationPolicy.manual(
                3,
                List.of("all"),
                List.of("lineage-root", "catalog"));

        HermesSkillLineageRemediationExecution execution =
                HermesSkillLineageRemediationExecutor.defaultExecutor().dryRun(catalog, policy);

        assertThat(execution)
                .returns(true, HermesSkillLineageRemediationExecution::dryRun)
                .returns(true, HermesSkillLineageRemediationExecution::mutationAllowed)
                .returns(false, HermesSkillLineageRemediationExecution::mutationSupported)
                .returns(true, HermesSkillLineageRemediationExecution::approvalRequired)
                .returns("dry-run-repair-policy-ready", HermesSkillLineageRemediationExecution::status)
                .returns(2, HermesSkillLineageRemediationExecution::proposedActionCount)
                .returns(2, HermesSkillLineageRemediationExecution::policyPermittedActionCount)
                .returns(0, HermesSkillLineageRemediationExecution::executedActionCount)
                .returns(2, HermesSkillLineageRemediationExecution::skippedActionCount);
        assertThat(execution.policy().mode()).isEqualTo("manual");
        assertThat(execution.repairIntentPlan())
                .returns(true, HermesSkillLineageRepairIntentPlan::dryRun)
                .returns(false, HermesSkillLineageRepairIntentPlan::mutationSupported)
                .returns(true, HermesSkillLineageRepairIntentPlan::approvalRequired)
                .returns(2, HermesSkillLineageRepairIntentPlan::intentCount);
        assertThat(execution.repairIntentPlan().intents())
                .extracting(HermesSkillLineageRepairIntent::command)
                .containsExactly(
                        "restore-lineage-root-definition",
                        "reconcile-learned-skill-store-indexes");
        assertThat(execution.repairIntentPlan().intents().getFirst())
                .returns("intent-001-repair-orphaned-lineage-root", HermesSkillLineageRepairIntent::intentId)
                .returns("lineage-root", HermesSkillLineageRepairIntent::targetType)
                .returns("hermes-missing-root", HermesSkillLineageRepairIntent::target)
                .returns(false, HermesSkillLineageRepairIntent::mutationSupported)
                .returns(true, HermesSkillLineageRepairIntent::approvalRequired);
        assertThat(execution.repairBackendPlan())
                .returns(3, HermesSkillLineageRepairBackendPlan::backendCount)
                .returns(2, HermesSkillLineageRepairBackendPlan::intentCount)
                .returns(2, HermesSkillLineageRepairBackendPlan::commandSupportedIntentCount)
                .returns(0, HermesSkillLineageRepairBackendPlan::mutationSupportedIntentCount)
                .returns(false, HermesSkillLineageRepairBackendPlan::mutationSupported);
        assertThat(metadataMap(execution.toMetadata(), "policy"))
                .containsEntry("mode", "manual")
                .containsEntry("approvalRequired", true)
                .containsEntry("maxActionsPerRun", 3);
        assertThat(metadataMap(execution.toMetadata(), "repairIntentPlan"))
                .containsEntry("intentCount", 2)
                .containsEntry("permittedActionCount", 2)
                .containsEntry("approvalRequired", true)
                .containsEntry("mutationSupported", false);
        assertThat(metadataMap(execution.toMetadata(), "repairIntentPlan").get("intents"))
                .asList()
                .hasSize(2);
        assertThat(metadataMap(execution.toMetadata(), "repairBackendPlan"))
                .containsEntry("backendCount", 3)
                .containsEntry("intentCount", 2)
                .containsEntry("previewOnlyIntentCount", 2)
                .containsEntry("mutationSupported", false);
        assertThat(metadataMap(execution.toMetadata(), "repairOperationHandoff"))
                .containsEntry("handoffStatus", "preview-only")
                .containsEntry("operationCount", 2)
                .containsEntry("previewOnlyOperationCount", 2)
                .containsEntry("batchCount", 1)
                .containsEntry("previewOnlyBatchCount", 1)
                .containsEntry("adapterReady", false);
    }

    @Test
    void dryRunNoopsWhenCatalogIsConsistent() {
        SkillDefinition initial = skill(
                "hermes-audit",
                "Audit MCP server health",
                Map.of(
                        "hermes.revision", "1",
                        "hermes.revisionStatus", "initial",
                        "hermes.lineageRootSkillId", "hermes-audit",
                        "hermes.lineageDepth", "1",
                        "hermes.sourceRequestIds", "req-a",
                        "hermes.mergeStrategy", HermesSkillRevisionMetadata.INITIAL_STRATEGY));
        HermesSkillLineageCatalog catalog = HermesSkillLineageCatalog.from(List.of(initial));

        HermesSkillLineageRemediationExecution execution =
                HermesSkillLineageRemediationExecutor.defaultExecutor().dryRun(catalog);

        assertThat(execution.status()).isEqualTo("dry-run-noop");
        assertThat(execution.proposedActionCount()).isZero();
        assertThat(execution.executedActionCount()).isZero();
        assertThat(execution.skippedActionCount()).isZero();
        assertThat(execution.remediationPlan().required()).isFalse();
        assertThat(execution.repairIntentPlan().intentCount()).isZero();
        assertThat(execution.repairBackendPlan().intentCount()).isZero();
        assertThat(execution.consistencyReport().consistent()).isTrue();
        assertThat(execution.consistencyReport().issues()).isEmpty();
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
}
