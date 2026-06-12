package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SkillArtifactQueryTest {

    @Test
    void defaultsToAllArtifactsWithSharedLimitPolicy() {
        SkillArtifactQuery query = SkillArtifactQuery.all();

        assertThat(query.skillId()).isBlank();
        assertThat(query.kind()).isNull();
        assertThat(query.name()).isBlank();
        assertThat(query.version()).isBlank();
        assertThat(query.limit()).isEqualTo(SkillManagementQueryLimits.DEFAULT_LIMIT);
        assertThat(new SkillArtifactQuery("", null, "", "", 0).limit())
                .isEqualTo(SkillManagementQueryLimits.DEFAULT_LIMIT);
    }

    @Test
    void normalizesFiltersAndLimits() {
        SkillArtifactQuery query = new SkillArtifactQuery(
                " planner ",
                SkillArtifactKind.RESOURCE,
                " prompt ",
                " v1 ",
                10_000);

        assertThat(query.skillId()).isEqualTo("planner");
        assertThat(query.name()).isEqualTo("prompt");
        assertThat(query.version()).isEqualTo("v1");
        assertThat(query.limit()).isEqualTo(SkillManagementQueryLimits.MAX_LIMIT);
    }

    @Test
    void matchesReferencesByOptionalFilters() {
        SkillArtifactReference prompt = SkillArtifactReference.resource("planner", "prompt", "v1");
        SkillArtifactReference packageArtifact = SkillArtifactReference.packageArtifact("planner", "v1");

        assertThat(SkillArtifactQuery.all().matches(prompt)).isTrue();
        assertThat(SkillArtifactQuery.all().matches(null)).isFalse();
        assertThat(SkillArtifactQuery.forSkill("planner").matches(prompt)).isTrue();
        assertThat(SkillArtifactQuery.forKind("planner", SkillArtifactKind.RESOURCE, 100).matches(prompt))
                .isTrue();
        assertThat(SkillArtifactQuery.forKind("planner", SkillArtifactKind.PACKAGE, 100).matches(prompt))
                .isFalse();
        assertThat(new SkillArtifactQuery("", SkillArtifactKind.RESOURCE, "prompt", "", 10).matches(prompt))
                .isTrue();
        assertThat(new SkillArtifactQuery("planner", null, "prompt", "v1", 10).matches(prompt))
                .isTrue();
        assertThat(new SkillArtifactQuery("planner", null, "prompt", "v2", 10).matches(prompt))
                .isFalse();
        assertThat(SkillArtifactQuery.forKind("planner", SkillArtifactKind.PACKAGE, 100).matches(packageArtifact))
                .isTrue();
    }

    @Test
    void rejectsUnsafeFilterSegments() {
        assertThatThrownBy(() -> SkillArtifactQuery.forSkill("team/planner"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("skill artifact query skill id");
        assertThatThrownBy(() -> new SkillArtifactQuery("planner", null, "../prompt", "", 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("skill artifact query name");
        assertThatThrownBy(() -> new SkillArtifactQuery("planner", null, "", "v1/latest", 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("skill artifact query version");
    }
}
