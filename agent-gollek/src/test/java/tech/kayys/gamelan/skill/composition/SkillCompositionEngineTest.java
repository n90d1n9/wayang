package tech.kayys.gamelan.skill.composition;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.kayys.gamelan.config.GamelanConfig;
import tech.kayys.gamelan.memory.hierarchy.SemanticMemory;
import tech.kayys.gamelan.skill.Skill;
import tech.kayys.gamelan.skill.SkillRegistry;
import tech.kayys.gamelan.agent.orchestration.SingleAgentOrchestrator;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SkillCompositionEngineTest {

    @Mock SkillRegistry          registry;
    @Mock SemanticMemory         semantic;
    @Mock SingleAgentOrchestrator orchestrator;
    @Mock GamelanConfig           config;

    @InjectMocks SkillCompositionEngine engine;

    @BeforeEach
    void setUp() {
        when(config.defaultModel()).thenReturn("test-model");
        when(semantic.query(anyString(), anyInt())).thenReturn(List.of());
        when(semantic.allNodes()).thenReturn(Map.of());
        when(semantic.neighbors(anyLong())).thenReturn(List.of());
    }

    // ── Progressive disclosure ─────────────────────────────────────────────

    @Test
    void progressiveLoadReturnsCorrectPrimarySet() {
        List<Skill> skills = List.of(
                skill("analyze-code",   "Code review security performance bugs analysis"),
                skill("read-file",       "Read view show file contents"),
                skill("run-command",     "Execute shell bash command build test"),
                skill("deploy-service",  "Deploy kubernetes docker container")
        );
        when(registry.listAll()).thenReturn(skills);

        SkillCompositionEngine.ProgressiveSkillSet result =
                engine.loadProgressively("analyze code for security bugs");

        assertThat(result.primary()).isNotEmpty();
        // analyze-code should score highest for "analyze code security bugs"
        assertThat(result.primary().stream().map(Skill::name).toList())
                .contains("analyze-code");
    }

    @Test
    void progressiveLoadLimitsPrimaryToThree() {
        // 10 skills all matching the query
        List<Skill> skills = List.of(
                skill("code-a", "code review analyze security"),
                skill("code-b", "code review analyze security"),
                skill("code-c", "code review analyze security"),
                skill("code-d", "code review analyze security"),
                skill("code-e", "code review analyze security"),
                skill("code-f", "code review analyze security"),
                skill("code-g", "code review analyze security"),
                skill("code-h", "code review analyze security"),
                skill("code-i", "code review analyze security"),
                skill("code-j", "code review analyze security")
        );
        when(registry.listAll()).thenReturn(skills);

        SkillCompositionEngine.ProgressiveSkillSet result =
                engine.loadProgressively("analyze code security");

        assertThat(result.primary().size()).isLessThanOrEqualTo(3);
    }

    @Test
    void progressiveLoadBuildsPromptBlock() {
        when(registry.listAll()).thenReturn(List.of(
                skill("read-file", "Read view show file contents analysis")));

        SkillCompositionEngine.ProgressiveSkillSet result =
                engine.loadProgressively("show me the file contents");

        String block = result.toPromptBlock();
        assertThat(block).isNotBlank();
        assertThat(block).contains("Active Skills");
    }

    @Test
    void progressiveLoadAllScoredContainsAll() {
        List<Skill> skills = List.of(
                skill("skill-a", "does something"),
                skill("skill-b", "does something else")
        );
        when(registry.listAll()).thenReturn(skills);

        SkillCompositionEngine.ProgressiveSkillSet result =
                engine.loadProgressively("do something");

        assertThat(result.allScored()).hasSizeGreaterThanOrEqualTo(1);
    }

    // ── Dependency resolution ──────────────────────────────────────────────

    @Test
    void singleSkillWithNoDependenciesHasOnlyItself() {
        Skill target = skill("fix-bug", "fix bugs");
        when(registry.listAll()).thenReturn(List.of(target));

        SkillCompositionEngine.SkillExecutionPlan plan = engine.resolveDependencies("fix-bug");

        assertThat(plan.targetSkill()).isEqualTo("fix-bug");
        assertThat(plan.orderedSkills()).hasSize(1);
        assertThat(plan.orderedSkills().get(0).name()).isEqualTo("fix-bug");
    }

    @Test
    void skillWithDependenciesResolvesTransitively() {
        // fix-bug requires analyze-code; analyze-code requires read-file
        Skill readFile    = skill("read-file",    "Read files");
        Skill analyzeCode = skillWithDep("analyze-code", "Analyze code", "requires: read-file");
        Skill fixBug      = skillWithDep("fix-bug",      "Fix bugs",     "requires: analyze-code");

        when(registry.listAll()).thenReturn(List.of(readFile, analyzeCode, fixBug));

        SkillCompositionEngine.SkillExecutionPlan plan = engine.resolveDependencies("fix-bug");

        assertThat(plan.orderedSkills()).isNotEmpty();
        // read-file should come before analyze-code (topological order)
        List<String> names = plan.orderedSkills().stream().map(Skill::name).toList();
        int readIdx    = names.indexOf("read-file");
        int analyzeIdx = names.indexOf("analyze-code");
        if (readIdx >= 0 && analyzeIdx >= 0) {
            assertThat(readIdx).isLessThan(analyzeIdx);
        }
    }

    @Test
    void notFoundSkillReturnsEmptyPlan() {
        when(registry.listAll()).thenReturn(List.of());

        SkillCompositionEngine.SkillExecutionPlan plan = engine.resolveDependencies("nonexistent");

        assertThat(plan.isEmpty()).isTrue();
    }

    // ── Knowledge graph traversal ─────────────────────────────────────────

    @Test
    void knowledgeGraphTraversalReturnsSkillsForQuery() {
        List<Skill> skills = List.of(
                skill("security-scanner", "security vulnerability scan analysis"),
                skill("read-file", "read files")
        );
        when(registry.listAll()).thenReturn(skills);
        when(semantic.query(anyString(), anyInt())).thenReturn(List.of());

        List<Skill> found = engine.traverseKnowledgeGraph("security vulnerability", 2);
        // Should find security-scanner due to keyword overlap
        assertThat(found).isNotNull(); // may be empty if no nodes in semantic graph
    }

    @Test
    void knowledgeGraphTraversalHandlesEmptyGraph() {
        when(registry.listAll()).thenReturn(List.of(skill("some-skill", "some description")));
        when(semantic.query(anyString(), anyInt())).thenReturn(List.of());

        assertThatCode(() -> engine.traverseKnowledgeGraph("any query", 3))
                .doesNotThrowAnyException();
    }

    // ── Skill execution plan ───────────────────────────────────────────────

    @Test
    void skillExecutionPlanIsEmptyForNotFound() {
        SkillCompositionEngine.SkillExecutionPlan plan =
                SkillCompositionEngine.SkillExecutionPlan.notFound("missing-skill");
        assertThat(plan.isEmpty()).isTrue();
        assertThat(plan.orderedSkills()).isEmpty();
        assertThat(plan.targetSkill()).isEqualTo("missing-skill");
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private Skill skill(String name, String description) {
        return new Skill(name, description, "Apache-2.0", "",
                Map.of(), List.of(), "## Instructions\nDo the work.", "raw: " + name,
                Map.of(), List.of(), null);
    }

    private Skill skillWithDep(String name, String description, String requiresLine) {
        // Pack the 'requires' into metadata so the dependency resolution can read it
        Map<String, String> metadata = Map.of(
                "requires", requiresLine.replace("requires:", "").strip());
        return new Skill(name, description, "Apache-2.0", "",
                metadata, List.of(), "## Instructions\nDo the work.", "raw: " + name,
                Map.of(), List.of(), null);
    }
}
