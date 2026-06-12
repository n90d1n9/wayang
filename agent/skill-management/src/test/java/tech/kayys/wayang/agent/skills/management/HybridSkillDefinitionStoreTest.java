package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.spi.skills.SkillDefinition;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HybridSkillDefinitionStoreTest {

    @Test
    void readPathsUseFallbackWhenPrimaryIsUnavailable() {
        TestSkillDefinitionStore fallback = new TestSkillDefinitionStore();
        fallback.registerSkill(TestSkillDefinitions.basic("backup"));
        HybridSkillDefinitionStore hybrid = new HybridSkillDefinitionStore(
                new FailingReadSkillDefinitionStore(),
                fallback);

        assertThat(hybrid.getSkill("backup")).isPresent();
        assertThat(hybrid.listSkills()).extracting(SkillDefinition::id)
                .containsExactly("backup");
    }

    @Test
    void fallbackReadRepairsMissingPrimaryDefinition() {
        TestSkillDefinitionStore primary = new TestSkillDefinitionStore();
        TestSkillDefinitionStore fallback = new TestSkillDefinitionStore();
        fallback.registerSkill(TestSkillDefinitions.basic("backup"));
        HybridSkillDefinitionStore hybrid = new HybridSkillDefinitionStore(primary, fallback);

        assertThat(hybrid.getSkill("backup")).isPresent();

        assertThat(primary.getSkill("backup")).isPresent();
    }

    @Test
    void writesStillRequirePrimary() {
        HybridSkillDefinitionStore hybrid = new HybridSkillDefinitionStore(
                new FailingWriteSkillDefinitionStore(),
                new TestSkillDefinitionStore());

        assertThatThrownBy(() -> hybrid.registerSkill(TestSkillDefinitions.basic("planner")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("primary write unavailable");
    }

    private static final class FailingReadSkillDefinitionStore extends TestSkillDefinitionStore {
        @Override
        public Optional<SkillDefinition> getSkill(String skillId) {
            throw new IllegalStateException("primary read unavailable");
        }

        @Override
        public List<SkillDefinition> listSkills() {
            throw new IllegalStateException("primary read unavailable");
        }
    }

    private static final class FailingWriteSkillDefinitionStore extends TestSkillDefinitionStore {
        @Override
        public void registerSkill(SkillDefinition skill) {
            throw new IllegalStateException("primary write unavailable");
        }
    }
}
