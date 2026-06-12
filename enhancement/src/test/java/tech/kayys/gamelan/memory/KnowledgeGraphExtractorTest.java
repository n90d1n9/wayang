package tech.kayys.gamelan.memory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.kayys.gamelan.agent.orchestration.AgentRequest;
import tech.kayys.gamelan.agent.orchestration.DirectCallOrchestrator;
import tech.kayys.gamelan.agent.orchestration.OrchestratorResult;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KnowledgeGraphExtractorTest {

    @Mock DirectCallOrchestrator llm;
    @Mock SemanticMemoryStore    semanticStore;
    @InjectMocks KnowledgeGraphExtractor extractor;

    private OrchestratorResult resp(String text) {
        return OrchestratorResult.ok(text, "direct", 1, List.of(), Duration.ZERO);
    }

    private MemoryHierarchy.Episode episode(String task, String outcome) {
        return new MemoryHierarchy.Episode(
                "ep-" + System.nanoTime(), task, outcome,
                List.of("read_file", "apply_patch"), true, 5000, Instant.now());
    }

    @Test
    void extractsParsedFacts() {
        String llmResponse = """
                FACT: build-tool = Maven 3.9 with Java 21
                PREFERENCE: test-style = JUnit 5 with AssertJ
                COMMAND: test-run = mvn test -pl auth-service
                DECISION: auth-pattern = JWT in HttpOnly cookies
                """;
        when(llm.execute(any())).thenReturn(resp(llmResponse));

        List<KnowledgeGraphExtractor.ExtractedFact> facts =
                extractor.extract(episode("refactor auth", "Updated JWT handling"), "llama3");

        assertThat(facts).hasSize(4);
        assertThat(facts).extracting(KnowledgeGraphExtractor.ExtractedFact::type)
                .containsExactlyInAnyOrder("FACT", "PREFERENCE", "COMMAND", "DECISION");
        assertThat(facts).anyMatch(f -> f.topic().equals("build-tool") &&
                f.value().contains("Maven"));
    }

    @Test
    void parsesProperlyFormattedLines() {
        List<KnowledgeGraphExtractor.ExtractedFact> facts = extractor.parse("""
                FACT: java-version = Java 21
                PREFERENCE: style = Google Java Style
                PROCEDURE: test-flow = 1. Read 2. Write tests 3. Run mvn test
                """);

        assertThat(facts).hasSize(3);
        assertThat(facts.get(0).type()).isEqualTo("FACT");
        assertThat(facts.get(0).topic()).isEqualTo("java-version");
        assertThat(facts.get(0).value()).isEqualTo("Java 21");
    }

    @Test
    void ignoresNoFactsResponse() {
        when(llm.execute(any())).thenReturn(resp("NO_FACTS"));
        var facts = extractor.extract(episode("simple task", "done"), "llama3");
        assertThat(facts).isEmpty();
    }

    @Test
    void returnsEmptyForNullEpisode() {
        assertThat(extractor.extract(null, "model")).isEmpty();
    }

    @Test
    void returnsEmptyForBlankOutcome() {
        when(llm.execute(any())).thenReturn(resp("NO_FACTS"));
        var ep = new MemoryHierarchy.Episode("id", "task", "", List.of(), true, 100, Instant.now());
        // Empty outcome — should not extract but not crash either
        assertThatCode(() -> extractor.extract(ep, "model")).doesNotThrowAnyException();
    }

    @Test
    void storesExtractedFactsInSemanticStore() {
        String llmResponse = "FACT: db = PostgreSQL 15";
        when(llm.execute(any())).thenReturn(resp(llmResponse));

        extractor.extract(episode("add db migration", "Used Flyway with PostgreSQL"), "m");

        verify(semanticStore, times(1)).store(eq("db"), eq("PostgreSQL 15"), eq("FACT"), any());
    }

    @Test
    void parseHandlesTopicWithSpaces() {
        var facts = extractor.parse("FACT: test command = mvn test -Dtest=Foo");
        assertThat(facts).hasSize(1);
        assertThat(facts.get(0).topic()).isEqualTo("test-command"); // spaces → hyphens
        assertThat(facts.get(0).value()).isEqualTo("mvn test -Dtest=Foo");
    }

    @Test
    void parseHandlesInvalidLines() {
        var facts = extractor.parse("""
                This is not a fact line
                FACT: valid-key = valid value
                INVALID: no-match
                FACT: = missing topic
                """);
        assertThat(facts).hasSize(1); // only the valid FACT line
    }
}
