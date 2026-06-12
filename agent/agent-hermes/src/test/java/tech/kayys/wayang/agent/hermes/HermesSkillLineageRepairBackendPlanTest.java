package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.spi.skills.SkillDefinition;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HermesSkillLineageRepairBackendPlanTest {

    @Test
    void defaultRegistryReportsPreviewOnlyBackendReadiness() {
        HermesSkillLineageRemediationExecution execution =
                HermesSkillLineageRemediationExecutor.defaultExecutor().dryRun(
                        orphanedCatalog(),
                        HermesSkillLineageRemediationPolicy.manual(
                                2,
                                List.of("all"),
                                List.of("lineage-root", "catalog")));

        HermesSkillLineageRepairBackendPlan plan = execution.repairBackendPlan();

        assertThat(plan)
                .returns(3, HermesSkillLineageRepairBackendPlan::backendCount)
                .returns(2, HermesSkillLineageRepairBackendPlan::intentCount)
                .returns(2, HermesSkillLineageRepairBackendPlan::commandSupportedIntentCount)
                .returns(0, HermesSkillLineageRepairBackendPlan::mutationSupportedIntentCount)
                .returns(2, HermesSkillLineageRepairBackendPlan::previewOnlyIntentCount)
                .returns(2, HermesSkillLineageRepairBackendPlan::operationCount)
                .returns(0, HermesSkillLineageRepairBackendPlan::mutationReadyOperationCount)
                .returns(2, HermesSkillLineageRepairBackendPlan::previewOnlyOperationCount)
                .returns(0, HermesSkillLineageRepairBackendPlan::unsupportedOperationCount)
                .returns(false, HermesSkillLineageRepairBackendPlan::mutationSupported);
        assertThat(plan.assessments())
                .extracting(HermesSkillLineageRepairBackendAssessment::status)
                .containsExactly("preview-only", "preview-only");
        assertThat(plan.operations())
                .extracting(HermesSkillLineageRepairOperation::status)
                .containsExactly("preview-only", "preview-only");
        assertThat(plan.operations().getFirst())
                .returns("database", HermesSkillLineageRepairOperation::backendId)
                .returns("database", HermesSkillLineageRepairOperation::storageFamily)
                .returns("restore-lineage-root-definition", HermesSkillLineageRepairOperation::command)
                .returns(true, HermesSkillLineageRepairOperation::dryRun)
                .returns(true, HermesSkillLineageRepairOperation::commandSupported)
                .returns(false, HermesSkillLineageRepairOperation::mutationReady);
        assertThat(plan.operations().getFirst().operationId())
                .startsWith("operation-intent-001-repair-orphaned-lineage-root-database-");
        assertThat(plan.assessments().getFirst().probes())
                .extracting(HermesSkillLineageRepairBackendProbe::backendId)
                .containsExactly("database", "file-system", "object-storage");
        assertThat(plan.assessments().getFirst().probes().getFirst().metadata())
                .containsEntry("storageFamily", "database")
                .containsEntry("adapterMode", "preview-only")
                .containsKey("supportedCommands");
        assertThat(plan.toMetadata())
                .containsEntry("backendCount", 3)
                .containsEntry("intentCount", 2)
                .containsEntry("mutationSupported", false)
                .containsEntry("operationCount", 2)
                .containsEntry("previewOnlyOperationCount", 2)
                .containsKey("operations");
        assertThat(plan.toMetadata().get("operations"))
                .asList()
                .hasSize(2);
    }

    @Test
    void customRegistryCanReportMutationReadyDryRunWithoutExecuting() {
        HermesSkillLineageRepairBackend mutatingBackend = new HermesSkillLineageRepairBackend() {
            @Override
            public String backendId() {
                return "database";
            }

            @Override
            public boolean mutationSupported() {
                return true;
            }

            @Override
            public boolean supports(HermesSkillLineageRepairIntent intent) {
                return intent != null && !intent.command().isBlank();
            }
        };
        HermesSkillLineageRemediationExecutor executor =
                new HermesSkillLineageRemediationExecutor(new HermesSkillLineageRepairBackendRegistry(
                        List.of(mutatingBackend)));

        HermesSkillLineageRemediationExecution execution = executor.dryRun(
                orphanedCatalog(),
                HermesSkillLineageRemediationPolicy.manual(
                        2,
                        List.of("all"),
                        List.of("lineage-root", "catalog")));

        assertThat(execution)
                .returns(true, HermesSkillLineageRemediationExecution::dryRun)
                .returns(true, HermesSkillLineageRemediationExecution::mutationAllowed)
                .returns(true, HermesSkillLineageRemediationExecution::mutationSupported)
                .returns(0, HermesSkillLineageRemediationExecution::executedActionCount);
        assertThat(execution.repairBackendPlan())
                .returns(1, HermesSkillLineageRepairBackendPlan::backendCount)
                .returns(2, HermesSkillLineageRepairBackendPlan::mutationSupportedIntentCount)
                .returns(2, HermesSkillLineageRepairBackendPlan::mutationReadyOperationCount)
                .returns(true, HermesSkillLineageRepairBackendPlan::mutationSupported);
        assertThat(execution.repairBackendPlan().assessments())
                .extracting(HermesSkillLineageRepairBackendAssessment::status)
                .containsExactly("mutation-ready", "mutation-ready");
        assertThat(execution.repairBackendPlan().operations())
                .extracting(HermesSkillLineageRepairOperation::status)
                .containsExactly("mutation-ready", "mutation-ready");
    }

    @Test
    void configuredCloudAliasCanReportMutationReadyForObjectStorageCandidate() {
        HermesSkillLineageRepairBackendRegistry registry =
                HermesSkillLineageRepairBackendRegistry.from(List.of("rustfs"), List.of("rustfs"));
        HermesSkillLineageRemediationExecutor executor = new HermesSkillLineageRemediationExecutor(registry);

        HermesSkillLineageRemediationExecution execution = executor.dryRun(
                orphanedCatalog(),
                HermesSkillLineageRemediationPolicy.manual(
                        2,
                        List.of("all"),
                        List.of("lineage-root", "catalog")));

        assertThat(registry.toMetadata())
                .containsEntry("backendCount", 1)
                .containsEntry("mutationBackendCount", 1)
                .containsKey("backendProfiles");
        assertThat(registry.profiles().getFirst())
                .returns("rustfs", HermesSkillLineageRepairBackendProfile::backendId)
                .returns("object-storage", HermesSkillLineageRepairBackendProfile::storageFamily)
                .returns("configured-mutation", HermesSkillLineageRepairBackendProfile::adapterMode)
                .returns(true, HermesSkillLineageRepairBackendProfile::mutationSupported);
        assertThat(registry.profiles().getFirst().aliases())
                .contains("object-storage", "s3", "minio", "rustfs");
        assertThat(registry.profiles().getFirst().supportedCommands())
                .contains("restore-lineage-root-definition", "reconcile-learned-skill-store-indexes");
        assertThat(execution.repairBackendPlan())
                .returns(1, HermesSkillLineageRepairBackendPlan::backendCount)
                .returns(2, HermesSkillLineageRepairBackendPlan::commandSupportedIntentCount)
                .returns(2, HermesSkillLineageRepairBackendPlan::mutationSupportedIntentCount)
                .returns(2, HermesSkillLineageRepairBackendPlan::operationCount)
                .returns(2, HermesSkillLineageRepairBackendPlan::mutationReadyOperationCount)
                .returns(0, HermesSkillLineageRepairBackendPlan::previewOnlyOperationCount)
                .returns(true, HermesSkillLineageRepairBackendPlan::mutationSupported);
        assertThat(execution.repairBackendPlan().operations().getFirst())
                .returns("rustfs", HermesSkillLineageRepairOperation::backendId)
                .returns("object-storage", HermesSkillLineageRepairOperation::storageFamily)
                .returns(true, HermesSkillLineageRepairOperation::mutationReady)
                .returns("mutation-ready", HermesSkillLineageRepairOperation::status);
        assertThat(execution.repairBackendPlan().operations().getFirst().metadata())
                .containsEntry("selectedBackend", "rustfs")
                .containsKey("probe");
        assertThat(execution.repairBackendPlan().assessments().getFirst().probes().getFirst())
                .returns("rustfs", HermesSkillLineageRepairBackendProbe::backendId)
                .returns(true, HermesSkillLineageRepairBackendProbe::candidate)
                .returns(true, HermesSkillLineageRepairBackendProbe::mutationSupported)
                .returns("mutation-ready", HermesSkillLineageRepairBackendProbe::status);
        assertThat(execution.repairBackendPlan().assessments().getFirst().probes().getFirst().metadata())
                .containsEntry("storageFamily", "object-storage")
                .containsEntry("adapterMode", "configured-mutation");
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
