package tech.kayys.gamelan.memory;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for {@link AgentMemory} using a temp directory for persistence.
 */
class AgentMemoryTest {

    // We test the logic without CDI by constructing directly
    private AgentMemory memory;

    @BeforeEach
    void setUp() {
        memory = new AgentMemory();
        memory.init(); // @PostConstruct equivalent
    }

    @Test
    void remembersAndRetrievesFact() {
        memory.remember("java-version", "21", AgentMemory.MemoryType.FACT);

        var relevant = memory.relevant();
        assertThat(relevant).anyMatch(e ->
                e.key().equals("java-version") && e.value().equals("21"));
    }

    @Test
    void forgetsEntry() {
        memory.remember("to-delete", "value", AgentMemory.MemoryType.FACT);
        assertThat(memory.relevant()).anyMatch(e -> e.key().equals("to-delete"));

        memory.forget("to-delete");
        assertThat(memory.relevant()).noneMatch(e -> e.key().equals("to-delete"));
    }

    @Test
    void overwritesExistingKey() {
        memory.remember("key", "original", AgentMemory.MemoryType.FACT);
        memory.remember("key", "updated", AgentMemory.MemoryType.PREFERENCE);

        var matches = memory.relevant().stream()
                .filter(e -> e.key().equals("key")).toList();
        assertThat(matches).hasSize(1);
        assertThat(matches.get(0).value()).isEqualTo("updated");
        assertThat(matches.get(0).type()).isEqualTo(AgentMemory.MemoryType.PREFERENCE);
    }

    @Test
    void extractsRememberProtocolFromLlmOutput() {
        String output = """
                I found the test command for this project.
                REMEMBER: test-command = mvn test -pl auth-service
                You can run the full suite with that command.
                REMEMBER: java-version = 21
                """;

        int count = memory.extractAndStore(output);

        assertThat(count).isEqualTo(2);
        assertThat(memory.relevant()).anyMatch(e ->
                e.key().equals("test-command")
                && e.value().equals("mvn test -pl auth-service"));
        assertThat(memory.relevant()).anyMatch(e ->
                e.key().equals("java-version") && e.value().equals("21"));
    }

    @Test
    void extractHandlesEmojiPrefix() {
        String output = "🧠 REMEMBER: style = Google Java Style";
        int count = memory.extractAndStore(output);
        assertThat(count).isEqualTo(1);
        assertThat(memory.relevant()).anyMatch(e -> e.key().equals("style"));
    }

    @Test
    void extractIgnoresLinesWithoutEquals() {
        String output = "REMEMBER: no equals sign here\nREMEMBER: key = valid value";
        int count = memory.extractAndStore(output);
        assertThat(count).isEqualTo(1);
        assertThat(memory.relevant()).anyMatch(e -> e.key().equals("key"));
    }

    @Test
    void extractReturnsZeroForNullInput() {
        assertThat(memory.extractAndStore(null)).isEqualTo(0);
        assertThat(memory.extractAndStore("")).isEqualTo(0);
        assertThat(memory.extractAndStore("no remember lines here")).isEqualTo(0);
    }

    @Test
    void promptBlockIncludesMemories() {
        memory.remember("test-cmd", "mvn test", AgentMemory.MemoryType.COMMAND);
        String block = memory.promptBlock();
        assertThat(block).contains("test-cmd");
        assertThat(block).contains("mvn test");
        assertThat(block).contains("Remembered Context");
    }

    @Test
    void promptBlockEmptyWhenNoMemories() {
        // Fresh instance, no memories
        assertThat(memory.promptBlock()).isEmpty();
    }

    @Test
    void allReturnsEverythingIncludingGlobal() {
        memory.remember("project-fact", "val", AgentMemory.MemoryType.FACT);
        memory.rememberGlobal("global-pref", "val2", AgentMemory.MemoryType.PREFERENCE);

        assertThat(memory.all()).hasSize(2);
        assertThat(memory.all()).anyMatch(e -> e.project().equals("_global"));
    }

    @Test
    void infersFACTByDefault() {
        memory.extractAndStore("REMEMBER: thing = plain value");
        var entry = memory.relevant().stream()
                .filter(e -> e.key().equals("thing")).findFirst().orElseThrow();
        assertThat(entry.type()).isEqualTo(AgentMemory.MemoryType.FACT);
    }

    @Test
    void infersCOMMANDForCommandLikeValues() {
        memory.extractAndStore("REMEMBER: build = mvn clean package");
        var entry = memory.relevant().stream()
                .filter(e -> e.key().equals("build")).findFirst().orElseThrow();
        assertThat(entry.type()).isEqualTo(AgentMemory.MemoryType.COMMAND);
    }
}
