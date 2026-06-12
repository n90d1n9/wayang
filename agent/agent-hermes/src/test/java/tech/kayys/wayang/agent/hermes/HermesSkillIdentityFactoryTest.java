package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HermesSkillIdentityFactoryTest {

    @Test
    void createsStableIdentityFromTask() {
        HermesSkillIdentityFactory factory = new HermesSkillIdentityFactory();

        HermesSkillIdentity first = factory.fromTask("Generate nightly API backup report");
        HermesSkillIdentity second = factory.fromTask("Generate nightly API backup report");

        assertThat(first).isEqualTo(second);
        assertThat(first.id()).startsWith("hermes-generate-nightly-api-backup-report-");
        assertThat(first.name()).isEqualTo("Generate nightly API backup report");
        assertThat(first.description()).isEqualTo(
                "Learned Hermes workflow for: Generate nightly API backup report");
    }

    @Test
    void fallsBackForBlankTasksAndTruncatesLongNames() {
        HermesSkillIdentityFactory factory = new HermesSkillIdentityFactory();
        String longTask = "Review ".repeat(20).trim();

        HermesSkillIdentity blank = factory.fromTask("   ");
        HermesSkillIdentity longIdentity = factory.fromTask(longTask);

        assertThat(blank.id()).startsWith("hermes-learned-workflow-");
        assertThat(blank.name()).isEqualTo("Hermes Learned Workflow");
        assertThat(blank.description()).isEqualTo("Learned Hermes workflow from a successful multi-step run.");
        assertThat(longIdentity.id()).startsWith("hermes-review-review-review-review-review-review-");
        assertThat(longIdentity.name()).hasSize(80).endsWith("...");
    }
}
