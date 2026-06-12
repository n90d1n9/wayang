package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.spi.skills.SkillDefinition;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HermesSkillLineageRepairIntentMarkdownRendererTest {

    @Test
    void rendersNoopPreviewWhenExecutionIsMissing() {
        String markdown = new HermesSkillLineageRepairIntentMarkdownRenderer().render(null);

        assertThat(markdown)
                .contains("# Hermes Skill-Lineage Repair Preview")
                .contains("Status: `dry-run-noop`")
                .contains("Policy mode: `dry-run`")
                .contains("No repair intents are pending.");
    }

    @Test
    void rendersPolicyReadyIntentPreview() {
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
                2,
                List.of("all"),
                List.of("lineage-root", "catalog"));
        HermesSkillLineageRemediationExecution execution =
                HermesSkillLineageRemediationExecutor.defaultExecutor().dryRun(catalog, policy);

        String markdown = new HermesSkillLineageRepairIntentMarkdownRenderer().render(execution);

        assertThat(markdown)
                .contains("# Hermes Skill-Lineage Repair Preview")
                .contains("Status: `dry-run-repair-policy-ready`")
                .contains("Policy mode: `manual`")
                .contains("Policy-permitted intents: 2")
                .contains("`restore-lineage-root-definition` -> `hermes-missing-root`")
                .contains("Intent: `intent-001-repair-orphaned-lineage-root`")
                .contains("Candidate backends: database, file-system, object-storage")
                .contains("Approval: required")
                .contains("Mutation: preview-only")
                .contains("Approval is required before any future mutating adapter may apply this plan.");
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
