package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.spi.AgentRequest;

import static org.assertj.core.api.Assertions.assertThat;

class HermesSkillLineageResolverTest {

    @Test
    void resolvesExplicitCatalogRequest() {
        HermesSkillLineagePlan plan = new HermesSkillLineageResolver(HermesAgentModeConfig.defaults())
                .resolve(AgentRequest.builder()
                        .prompt("Prepare an ops summary")
                        .parameter("skillLineageOperation", "catalog")
                        .build());

        assertThat(plan.lineageEnabled()).isTrue();
        assertThat(plan.requested()).isTrue();
        assertThat(plan.inspect()).isTrue();
        assertThat(plan.active()).isTrue();
        assertThat(plan.operation()).isEqualTo("catalog");
        assertThat(plan.target()).isEqualTo("learned-skills");
        assertThat(plan.source()).isEqualTo("explicit");
        assertThat(HermesSkillLineageDirective.from(plan).operation()).isEqualTo("catalog");
    }

    @Test
    void resolvesPromptSkillLineageInspection() {
        HermesSkillLineagePlan plan = new HermesSkillLineageResolver(HermesAgentModeConfig.defaults())
                .resolve(AgentRequest.builder()
                        .prompt("Inspect lineage for skill hermes-audit-v2.")
                        .build());

        assertThat(plan.active()).isTrue();
        assertThat(plan.operation()).isEqualTo("inspect");
        assertThat(plan.skillId()).isEqualTo("hermes-audit-v2");
        assertThat(plan.target()).isEqualTo("skill:hermes-audit-v2");
        assertThat(plan.source()).isEqualTo("prompt");
    }

    @Test
    void resolvesRepairPreviewRequestAsGlobalLineageOperation() {
        HermesSkillLineagePlan plan = new HermesSkillLineageResolver(HermesAgentModeConfig.defaults())
                .resolve(AgentRequest.builder()
                        .prompt("Preview skill lineage repair adapters before applying anything")
                        .parameter("skillLineageOperation", "repair-preview")
                        .build());

        assertThat(plan.active()).isTrue();
        assertThat(plan.operation()).isEqualTo("repair-preview");
        assertThat(plan.skillId()).isEmpty();
        assertThat(plan.target()).isEqualTo("learned-skills:repair-preview");
        assertThat(plan.reason()).isEqualTo("skill lineage repair preview requested");
        assertThat(HermesSkillLineageDirective.from(plan))
                .returns(true, HermesSkillLineageDirective::active)
                .returns("repair-preview", HermesSkillLineageDirective::operation)
                .returns("learned-skills:repair-preview", HermesSkillLineageDirective::target);
    }

    @Test
    void resolvesExplicitRepairApplyEnvelope() {
        HermesSkillLineagePlan plan = new HermesSkillLineageResolver(HermesAgentModeConfig.defaults())
                .resolve(AgentRequest.builder()
                        .prompt("Apply the approved lineage repair")
                        .parameter("skillLineageOperation", "repair-apply")
                        .parameter("skillLineageRepairApproved", true)
                        .parameter("skillLineageRepairApprovalId", "approval-001")
                        .parameter("skillLineageRepairIdempotencyKey", "repair-key-001")
                        .parameter("skillLineageRepairBackendId", "rustfs")
                        .parameter("skillLineageRepairStorageFamily", "object-storage")
                        .build());

        assertThat(plan.active()).isTrue();
        assertThat(plan.operation()).isEqualTo("repair-apply");
        assertThat(plan.target()).isEqualTo("learned-skills:repair-apply");
        assertThat(plan.repairAction()).isEqualTo("apply");
        assertThat(plan.repairApproved()).isTrue();
        assertThat(plan.repairApprovalId()).isEqualTo("approval-001");
        assertThat(plan.repairIdempotencyKey()).isEqualTo("repair-key-001");
        assertThat(plan.repairBackendId()).isEqualTo("rustfs");
        assertThat(plan.repairStorageFamily()).isEqualTo("object-storage");
        assertThat(HermesSkillLineageDirective.from(plan))
                .returns("repair-apply", HermesSkillLineageDirective::operation)
                .returns("apply", HermesSkillLineageDirective::repairAction)
                .returns(true, HermesSkillLineageDirective::repairApproved)
                .returns("approval-001", HermesSkillLineageDirective::repairApprovalId)
                .returns("repair-key-001", HermesSkillLineageDirective::repairIdempotencyKey);
    }

    @Test
    void infersCatalogWhenLineagePromptHasNoSkillId() {
        HermesSkillLineagePlan plan = new HermesSkillLineageResolver(HermesAgentModeConfig.defaults())
                .resolve(AgentRequest.builder()
                        .prompt("Show the learned skill catalog")
                        .build());

        assertThat(plan.active()).isTrue();
        assertThat(plan.operation()).isEqualTo("catalog");
        assertThat(plan.skillId()).isEmpty();
        assertThat(plan.target()).isEqualTo("learned-skills");
        assertThat(plan.toMetadata())
                .containsEntry("operation", "catalog")
                .containsEntry("active", true);
    }

    @Test
    void disablesLineageWhenSkillLearningIsDisabled() {
        HermesSkillLineagePlan plan = new HermesSkillLineageResolver(HermesAgentModeConfig.builder()
                .skillLearningEnabled(false)
                .build())
                .resolve(AgentRequest.builder()
                        .prompt("Show the learned skill catalog")
                        .build());

        assertThat(plan.lineageEnabled()).isFalse();
        assertThat(plan.requested()).isTrue();
        assertThat(plan.inspect()).isFalse();
        assertThat(plan.active()).isFalse();
        assertThat(plan.operation()).isEqualTo("catalog");
        assertThat(plan.reason()).isEqualTo("skill lineage disabled because skill learning is disabled");
    }
}
