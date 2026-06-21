package tech.kayys.gamelan.skill;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class SkillSelectorTest {

    private final SkillSelector selector = new SkillSelector();

    private Skill skill(String name, String description) {
        return new Skill(name, description, "", "", Map.of(), List.of(),
                "instructions", "raw", Map.of(), List.of(), java.nio.file.Path.of(""));
    }

    @Test
    void selectsMatchingSkillByName() {
        Skill readFile = skill("read-file", "Read files from disk. Use when viewing files.");
        Skill writeFile = skill("write-file", "Write content to files.");
        List<Skill> all = List.of(readFile, writeFile);

        List<Skill> selected = selector.select("show me the contents of Main.java", all);

        assertThat(selected).extracting(Skill::name).contains("read-file");
    }

    @Test
    void selectsMatchingSkillByDescriptionKeywords() {
        Skill analyze = skill("analyze-code", "Code review security performance bugs");
        Skill other   = skill("run-command",  "Execute shell commands build test");
        List<Skill> all = List.of(analyze, other);

        List<Skill> selected = selector.select("review this code for security issues", all);

        assertThat(selected).extracting(Skill::name).contains("analyze-code");
    }

    @Test
    void returnsEmptyForUnrelatedInput() {
        Skill cooking = skill("cooking", "Recipes ingredients kitchen baking");
        List<Skill> selected = selector.select("fix the Java null pointer exception", List.of(cooking));
        // "cooking" has nothing to do with Java code
        assertThat(selected).isEmpty();
    }

    @Test
    void limitsToThreeSkills() {
        List<Skill> many = List.of(
                skill("read-file",    "Read view show file contents code"),
                skill("write-file",   "Write create modify file code"),
                skill("analyze-code", "Review analyze code security"),
                skill("run-command",  "Execute run command shell code"),
                skill("git-ops",      "Git diff status log code review")
        );

        List<Skill> selected = selector.select("read and review code then write changes", many);
        assertThat(selected).hasSizeLessThanOrEqualTo(3);
    }

    @Test
    void returnsEmptyListForEmptyInput() {
        List<Skill> skills = List.of(skill("read-file", "Read files."));
        assertThat(selector.select("", skills)).isEmpty();
        assertThat(selector.select(null, skills)).isEmpty();
    }

    @Test
    void returnsEmptyListForEmptySkills() {
        assertThat(selector.select("read a file", List.of())).isEmpty();
    }

    @Test
    void exactNameMatchScoresHighest() {
        Skill target  = skill("read-file", "Other stuff unrelated");
        Skill similar = skill("file-ops",  "Read show open files view contents");
        List<Skill> all = List.of(target, similar);

        List<Skill> selected = selector.select("use read-file skill", all);
        // The exact name match should be first
        assertThat(selected.get(0).name()).isEqualTo("read-file");
    }
}
