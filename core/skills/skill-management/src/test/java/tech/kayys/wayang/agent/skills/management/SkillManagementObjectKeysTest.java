package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SkillManagementObjectKeysTest {

    @Test
    void normalizesObjectStoragePrefixes() {
        assertThat(SkillManagementObjectKeys.normalizePrefix(null, "")).isEmpty();
        assertThat(SkillManagementObjectKeys.normalizePrefix("  /tenant-a/skills  ", "unused"))
                .isEqualTo("tenant-a/skills/");
        assertThat(SkillManagementObjectKeys.normalizePrefix(null, "skill-management/events"))
                .isEqualTo("skill-management/events/");
    }

    @Test
    void buildsSafeSkillKeys() {
        assertThat(SkillManagementObjectKeys.skillKey(
                "tenant-a/skills/",
                " planner ",
                ".properties",
                "persistence"))
                .isEqualTo("tenant-a/skills/planner.properties");
    }

    @Test
    void rejectsUnsafeSkillIds() {
        assertThatThrownBy(() -> SkillManagementObjectKeys.normalizeSkillId("../planner", "persistence"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid skill id for object storage persistence");
        assertThatThrownBy(() -> SkillManagementObjectKeys.normalizeSkillId("team/planner", "persistence"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SkillManagementObjectKeys.normalizeSkillId("team\\planner", "persistence"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SkillManagementObjectKeys.normalizeSkillId(" ", "persistence"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
