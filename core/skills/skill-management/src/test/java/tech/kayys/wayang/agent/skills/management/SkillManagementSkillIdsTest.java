package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SkillManagementSkillIdsTest {

    @Test
    void detectsBlankSkillIds() {
        assertThat(SkillManagementSkillIds.isBlank(null)).isTrue();
        assertThat(SkillManagementSkillIds.isBlank(" ")).isTrue();
        assertThat(SkillManagementSkillIds.isBlank("planner")).isFalse();
    }

    @Test
    void trimsSkillIdsForStorage() {
        assertThat(SkillManagementSkillIds.normalizeForStorage(" planner ", "file persistence"))
                .isEqualTo("planner");
    }

    @Test
    void rejectsPathLikeSkillIds() {
        assertThatThrownBy(() -> SkillManagementSkillIds.normalizeForStorage("../planner", "file persistence"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid skill id for file persistence");
        assertThatThrownBy(() -> SkillManagementSkillIds.normalizeForStorage("team/planner", "file persistence"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SkillManagementSkillIds.normalizeForStorage("team\\planner", "file persistence"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SkillManagementSkillIds.normalizeForStorage(" ", "file persistence"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
