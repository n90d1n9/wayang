package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.spi.skills.SkillDefinition;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SkillManagementCatalogReaderTest {

    @Test
    void listReturnsDefinitionsSortedById() {
        TestSkillDefinitionStore definitions = new TestSkillDefinitionStore();
        SkillDefinition writer = TestSkillDefinitions.withTools(
                "writer",
                "Writer",
                "WRITING",
                "Writes drafts",
                List.of("draft"));
        SkillDefinition planner = TestSkillDefinitions.withTools(
                "planner",
                "Planner",
                "REASONING",
                "Plans tasks",
                List.of("search"));
        definitions.registerSkill(writer);
        definitions.registerSkill(planner);
        SkillManagementCatalogReader reader = reader(definitions, new InMemorySkillLifecycleStateStore());

        assertThat(reader.get("planner")).contains(planner);
        assertThat(reader.list()).extracting(SkillDefinition::id)
                .containsExactly("planner", "writer");
    }

    @Test
    void listActiveUsesImplicitDefaultsWithoutPersisting() {
        TestSkillDefinitionStore definitions = new TestSkillDefinitionStore();
        definitions.registerSkill(TestSkillDefinitions.withTools(
                "planner",
                "Planner",
                "REASONING",
                "Plans tasks",
                List.of("search")));
        definitions.registerSkill(TestSkillDefinitions.withTools(
                "writer",
                "Writer",
                "WRITING",
                "Writes drafts",
                List.of("draft")));
        InMemorySkillLifecycleStateStore states = new InMemorySkillLifecycleStateStore();
        states.save(SkillLifecycleState.created("writer").withStatus(SkillLifecycleStatus.DISABLED));
        SkillManagementCatalogReader reader = reader(definitions, states);

        assertThat(reader.listActive()).extracting(SkillDefinition::id)
                .containsExactly("planner");
        assertThat(states.snapshot()).containsOnlyKeys("writer");
    }

    @Test
    void searchFiltersByLifecycleCategoryAndQuery() {
        TestSkillDefinitionStore definitions = new TestSkillDefinitionStore();
        definitions.registerSkill(TestSkillDefinitions.withTools(
                "planner",
                "Planner",
                "REASONING",
                "Plans tasks",
                List.of("search")));
        definitions.registerSkill(TestSkillDefinitions.withTools(
                "writer",
                "Writer",
                "WRITING",
                "Writes drafts",
                List.of("draft")));
        definitions.registerSkill(TestSkillDefinitions.withTools(
                "coder",
                "Coder",
                "ENGINEERING",
                "Writes code",
                List.of("build")));
        InMemorySkillLifecycleStateStore states = new InMemorySkillLifecycleStateStore();
        states.save(SkillLifecycleState.created("writer").withStatus(SkillLifecycleStatus.DISABLED));
        SkillManagementCatalogReader reader = reader(definitions, states);

        assertThat(reader.search("draft", "writing", false)).isEmpty();
        assertThat(reader.search("draft", "writing", true)).extracting(SkillDefinition::id)
                .containsExactly("writer");
        assertThat(reader.search("write", "", true)).extracting(SkillDefinition::id)
                .containsExactly("coder", "writer");
        assertThat(reader.search("search", "reasoning", false)).extracting(SkillDefinition::id)
                .containsExactly("planner");
    }

    @Test
    void listByCategoryDelegatesThenSorts() {
        TestSkillDefinitionStore definitions = new TestSkillDefinitionStore();
        definitions.registerSkill(TestSkillDefinitions.withTools(
                "writer-b",
                "Writer B",
                "WRITING",
                "Writes drafts",
                List.of()));
        definitions.registerSkill(TestSkillDefinitions.withTools(
                "writer-a",
                "Writer A",
                "WRITING",
                "Writes drafts",
                List.of()));
        definitions.registerSkill(TestSkillDefinitions.withTools(
                "planner",
                "Planner",
                "REASONING",
                "Plans tasks",
                List.of()));
        SkillManagementCatalogReader reader = reader(definitions, new InMemorySkillLifecycleStateStore());

        assertThat(reader.listByCategory("writing")).extracting(SkillDefinition::id)
                .containsExactly("writer-a", "writer-b");
    }

    private SkillManagementCatalogReader reader(
            SkillDefinitionStore definitions,
            SkillLifecycleStateStore states) {
        SkillManagementLifecycleRunner lifecycleRunner = new SkillManagementLifecycleRunner(
                definitions,
                states,
                new SkillLifecycleStateReconciler(),
                new SkillManagementEventRecorder(SkillManagementEventSink.noop()));
        return new SkillManagementCatalogReader(definitions, lifecycleRunner);
    }

}
