package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SkillManagementFileNamesTest {

    @Test
    void buildsSafeSkillFiles() {
        assertThat(SkillManagementFileNames.skillFile(
                Path.of("skills"),
                " planner ",
                ".properties",
                "file persistence"))
                .isEqualTo(Path.of("skills", "planner.properties"));
    }

    @Test
    void normalizesSkillIds() {
        assertThat(SkillManagementFileNames.normalizeSkillId(" planner ", "file persistence"))
                .isEqualTo("planner");
    }

    @Test
    void rejectsUnsafeSkillIds() {
        assertThatThrownBy(() -> SkillManagementFileNames.normalizeSkillId("../planner", "file persistence"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid skill id for file persistence");
        assertThatThrownBy(() -> SkillManagementFileNames.normalizeSkillId("team/planner", "file persistence"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SkillManagementFileNames.normalizeSkillId("team\\planner", "file persistence"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SkillManagementFileNames.normalizeSkillId(" ", "file persistence"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
