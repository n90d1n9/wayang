package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.spi.skills.SkillDefinition;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HermesSkillLineageRepairOperationMarkdownRendererTest {

    @Test
    void rendersNoopPreviewWhenExecutionIsMissing() {
        String markdown = new HermesSkillLineageRepairOperationMarkdownRenderer().render(null);

        assertThat(markdown)
                .contains("# Hermes Skill-Lineage Backend Operation Preview")
                .contains("Status: `dry-run-noop`")
                .contains("Operation count: 0")
                .contains("No backend repair operations are pending.");
    }

    @Test
    void rendersPreviewOnlyBackendOperations() {
        HermesSkillLineageRemediationExecution execution =
                HermesSkillLineageRemediationExecutor.defaultExecutor().dryRun(
                        orphanedCatalog(),
                        HermesSkillLineageRemediationPolicy.manual(
                                2,
                                List.of("all"),
                                List.of("lineage-root", "catalog")));

        String markdown = new HermesSkillLineageRepairOperationMarkdownRenderer().render(execution);

        assertThat(markdown)
                .contains("# Hermes Skill-Lineage Backend Operation Preview")
                .contains("Status: `dry-run-repair-policy-ready`")
                .contains("Operation count: 2")
                .contains("Preview-only operations: 2")
                .contains("`restore-lineage-root-definition` via `database` -> `hermes-missing-root`")
                .contains("Storage family: `database`")
                .contains("Backend status: `preview-only`")
                .contains("Mutation: preview-only")
                .contains("no backend store writes were performed");
    }

    @Test
    void rendersMutationReadyObjectStorageOperations() {
        HermesSkillLineageRemediationExecutor executor = new HermesSkillLineageRemediationExecutor(
                HermesSkillLineageRepairBackendRegistry.from(List.of("rustfs"), List.of("rustfs")));
        HermesSkillLineageRemediationExecution execution = executor.dryRun(
                orphanedCatalog(),
                HermesSkillLineageRemediationPolicy.manual(
                        2,
                        List.of("all"),
                        List.of("lineage-root", "catalog")));

        String markdown = new HermesSkillLineageRepairOperationMarkdownRenderer().render(execution);

        assertThat(markdown)
                .contains("Operation count: 2")
                .contains("Mutation-ready operations: 2")
                .contains("`restore-lineage-root-definition` via `rustfs` -> `hermes-missing-root`")
                .contains("Storage family: `object-storage`")
                .contains("Backend status: `mutation-ready`")
                .contains("Mutation: ready")
                .contains("Approval is required before a future mutating repair adapter may apply these operations.");
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
