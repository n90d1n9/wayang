package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.spi.skills.SkillDefinition;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HermesSkillLineageCatalogTest {

    @Test
    void groupsLearnedSkillsByLineageRootAndSummarizesRefinements() {
        SkillDefinition initial = skill(
                "hermes-api-report",
                "Generate nightly API backup report",
                Map.of(
                        "hermes.revision", "1",
                        "hermes.revisionStatus", "initial",
                        "hermes.lineageRootSkillId", "hermes-api-report",
                        "hermes.lineageDepth", "1",
                        "hermes.sourceRequestIds", "req-a",
                        "hermes.mergeStrategy", HermesSkillRevisionMetadata.INITIAL_STRATEGY));
        SkillDefinition refined = skill(
                "hermes-api-report-v2",
                "Create API backup nightly report",
                Map.of(
                        "hermes.revision", "2",
                        "hermes.revisionStatus", "refined",
                        "hermes.lineageRootSkillId", "hermes-api-report",
                        "hermes.lineageDepth", "2",
                        "hermes.sourceRequestIds", "req-a,req-b",
                        "hermes.mergeStrategy", HermesSkillRevisionMetadata.REFINEMENT_STRATEGY));
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

        HermesSkillLineageCatalog catalog = HermesSkillLineageCatalog.from(List.of(refined, orphaned, initial));

        assertThat(catalog.learnedSkillCount()).isEqualTo(3);
        assertThat(catalog.rootCount()).isEqualTo(2);
        assertThat(catalog.refinedRootCount()).isEqualTo(2);
        assertThat(catalog.refinedEntryCount()).isEqualTo(2);
        assertThat(catalog.orphanedRootCount()).isEqualTo(1);
        assertThat(catalog.sourceRequestIds()).containsExactly("req-a", "req-b", "req-c");
        assertThat(catalog.mergeStrategyCounts())
                .containsEntry(HermesSkillRevisionMetadata.INITIAL_STRATEGY, 1L)
                .containsEntry(HermesSkillRevisionMetadata.REFINEMENT_STRATEGY, 2L);
        assertThat(catalog.health().status()).isEqualTo("attention");
        assertThat(catalog.health().attentionRequired()).isTrue();
        assertThat(catalog.health().orphanedRootSkillIds()).containsExactly("hermes-missing-root");
        assertThat(catalog.health().refinedRootSkillIds())
                .containsExactly("hermes-api-report", "hermes-missing-root");
        assertThat(catalog.health().recommendedActions())
                .containsExactly("repair-orphaned-lineage-roots", "inspect-learned-skill-storage-consistency");
        assertThat(catalog.health().remediationPlan().required()).isTrue();
        assertThat(catalog.health().remediationPlan().strategy()).isEqualTo("repair-lineage-roots");
        assertThat(catalog.health().remediationPlan().requiredActionCount()).isEqualTo(2);
        assertThat(catalog.health().remediationPlan().actions())
                .extracting(HermesSkillLineageRemediationAction::action)
                .containsExactly("repair-orphaned-lineage-root", "inspect-learned-skill-storage-consistency");
        assertThat(catalog.consistencyReport())
                .returns("inconsistent", HermesSkillStoreConsistencyReport::status)
                .returns(false, HermesSkillStoreConsistencyReport::consistent)
                .returns(true, HermesSkillStoreConsistencyReport::attentionRequired)
                .returns(1, HermesSkillStoreConsistencyReport::criticalIssueCount)
                .returns(1, HermesSkillStoreConsistencyReport::warningIssueCount);
        assertThat(catalog.consistencyReport().issues())
                .extracting(HermesSkillStoreConsistencyIssue::issueType)
                .containsExactly("repair-orphaned-lineage-root", "inspect-learned-skill-storage-consistency");
        assertThat(catalog.roots()).extracting(HermesSkillLineageRoot::rootSkillId)
                .containsExactly("hermes-api-report", "hermes-missing-root");
        assertThat(catalog.roots().getFirst().currentSkillId()).isEqualTo("hermes-api-report-v2");
        assertThat(catalog.roots().getFirst().rootPresent()).isTrue();
        assertThat(catalog.roots().getLast().rootPresent()).isFalse();
        assertThat(catalog.toMetadata())
                .containsEntry("learnedSkillCount", 3)
                .containsEntry("rootCount", 2)
                .containsEntry("refinedRootCount", 2L)
                .containsEntry("orphanedRootCount", 1L)
                .containsKey("health")
                .containsKey("consistencyReport");
        assertThat(metadataMap(catalog.toMetadata(), "health"))
                .containsEntry("status", "attention")
                .containsEntry("attentionRequired", true);
        assertThat(metadataMap(metadataMap(catalog.toMetadata(), "health"), "remediationPlan"))
                .containsEntry("required", true)
                .containsEntry("strategy", "repair-lineage-roots")
                .containsEntry("requiredActionCount", 2);
        assertThat(metadataMap(catalog.toMetadata(), "consistencyReport"))
                .containsEntry("status", "inconsistent")
                .containsEntry("consistent", false)
                .containsEntry("issueCount", 2);
        assertThat(catalog.toMetadata().get("roots")).asList().hasSize(2);
    }

    @Test
    void healthReportsHealthyCatalogWhenRootsArePresent() {
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
        HermesSkillLineageHealth health = catalog.health();

        assertThat(health.status()).isEqualTo("healthy");
        assertThat(health.attentionRequired()).isFalse();
        assertThat(health.recommendedActions()).isEmpty();
        assertThat(health.orphanedRootSkillIds()).isEmpty();
        assertThat(health.refinedRootSkillIds()).isEmpty();
        assertThat(health.remediationPlan().required()).isFalse();
        assertThat(health.remediationPlan().strategy()).isEqualTo("none");
        assertThat(health.remediationPlan().actions()).isEmpty();
        assertThat(health.toMetadata())
                .containsEntry("status", "healthy")
                .containsEntry("attentionRequired", false)
                .containsEntry("learnedSkillCount", 1)
                .containsKey("remediationPlan");
        assertThat(catalog.consistencyReport())
                .returns("consistent", HermesSkillStoreConsistencyReport::status)
                .returns(true, HermesSkillStoreConsistencyReport::consistent)
                .returns(0, HermesSkillStoreConsistencyReport::issueCount);
    }

    @Test
    void healthBuildsAdvisoryPlanForRefinedRoots() {
        SkillDefinition initial = skill(
                "hermes-api-report",
                "Generate nightly API backup report",
                Map.of(
                        "hermes.revision", "1",
                        "hermes.revisionStatus", "initial",
                        "hermes.lineageRootSkillId", "hermes-api-report",
                        "hermes.lineageDepth", "1",
                        "hermes.sourceRequestIds", "req-a",
                        "hermes.mergeStrategy", HermesSkillRevisionMetadata.INITIAL_STRATEGY));
        SkillDefinition refined = skill(
                "hermes-api-report-v2",
                "Create API backup nightly report",
                Map.of(
                        "hermes.revision", "2",
                        "hermes.revisionStatus", "refined",
                        "hermes.lineageRootSkillId", "hermes-api-report",
                        "hermes.lineageDepth", "2",
                        "hermes.sourceRequestIds", "req-a,req-b",
                        "hermes.mergeStrategy", HermesSkillRevisionMetadata.REFINEMENT_STRATEGY));

        HermesSkillLineageCatalog catalog = HermesSkillLineageCatalog.from(List.of(initial, refined));
        HermesSkillLineageHealth health = catalog.health();

        assertThat(health.status()).isEqualTo("evolving");
        assertThat(health.attentionRequired()).isFalse();
        assertThat(health.remediationPlan().required()).isFalse();
        assertThat(health.remediationPlan().strategy()).isEqualTo("review-refined-skill-quality");
        assertThat(health.remediationPlan().advisoryActionCount()).isEqualTo(1);
        assertThat(health.remediationPlan().actions().getFirst())
                .returns("review-refined-skill-quality", HermesSkillLineageRemediationAction::action)
                .returns("lineage-root", HermesSkillLineageRemediationAction::targetType)
                .returns("hermes-api-report", HermesSkillLineageRemediationAction::target);
        assertThat(catalog.consistencyReport())
                .returns("advisory", HermesSkillStoreConsistencyReport::status)
                .returns(true, HermesSkillStoreConsistencyReport::consistent)
                .returns(false, HermesSkillStoreConsistencyReport::attentionRequired)
                .returns(1, HermesSkillStoreConsistencyReport::advisoryIssueCount);
    }

    @Test
    void emptyCatalogHasStableMetadata() {
        HermesSkillLineageCatalog catalog = HermesSkillLineageCatalog.empty();

        assertThat(catalog.emptyCatalog()).isTrue();
        assertThat(catalog.health().status()).isEqualTo("empty");
        assertThat(catalog.health().attentionRequired()).isFalse();
        assertThat(catalog.health().recommendedActions())
                .containsExactly("distill-first-successful-complex-workflow");
        assertThat(catalog.health().remediationPlan().required()).isFalse();
        assertThat(catalog.health().remediationPlan().strategy()).isEqualTo("bootstrap-learned-skill-library");
        assertThat(catalog.health().remediationPlan().advisoryActionCount()).isEqualTo(1);
        assertThat(catalog.consistencyReport())
                .returns("empty", HermesSkillStoreConsistencyReport::status)
                .returns(true, HermesSkillStoreConsistencyReport::consistent)
                .returns(1, HermesSkillStoreConsistencyReport::advisoryIssueCount);
        assertThat(catalog.toMetadata())
                .containsEntry("learnedSkillCount", 0)
                .containsEntry("rootCount", 0)
                .containsEntry("refinedRootCount", 0L)
                .containsEntry("refinedEntryCount", 0L)
                .containsEntry("orphanedRootCount", 0L)
                .containsKey("health");
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
