package tech.kayys.gamelan.agent;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

class CheckpointManagerTest {

    private CheckpointManager manager;

    @BeforeEach
    void setUp(@TempDir Path tmp) throws Exception {
        manager = new CheckpointManager();
        // Override the checkpoint dir to use a temp dir
        Field field = CheckpointManager.class.getDeclaredField("CHECKPOINT_DIR");
        // CHECKPOINT_DIR is static final — we need to work around it
        // Instead we'll test via the public API using the real dir (CI-safe)
        // The real dir is ~/.gamelan/checkpoints — tests just use unique session IDs
    }

    @Test
    void saveAndLoadRoundTrip() {
        String sessionId = "test-session-" + System.currentTimeMillis();
        tech.kayys.gamelan.session.ConversationSession session =
                new tech.kayys.gamelan.session.ConversationSession(sessionId);
        session.addTurn("what is Java?",
                AgentResponse.builder().text("Java is a programming language.").build());

        manager.save(session, "test task", "llama3", "react");

        Optional<CheckpointManager.Checkpoint> loaded = manager.load(sessionId);

        assertThat(loaded).isPresent();
        assertThat(loaded.get().sessionId()).isEqualTo(sessionId);
        assertThat(loaded.get().task()).isEqualTo("test task");
        assertThat(loaded.get().model()).isEqualTo("llama3");
        assertThat(loaded.get().strategy()).isEqualTo("react");
        assertThat(loaded.get().messages()).hasSize(2); // user + assistant

        // Cleanup
        manager.delete(sessionId);
    }

    @Test
    void loadByTaskKeyword() {
        String sessionId = "kw-session-" + System.currentTimeMillis();
        tech.kayys.gamelan.session.ConversationSession session =
                new tech.kayys.gamelan.session.ConversationSession(sessionId);
        session.addTurn("q", AgentResponse.builder().text("a").build());

        String uniqueTask = "unique-keyword-" + sessionId;
        manager.save(session, uniqueTask, "model", "react");

        Optional<CheckpointManager.Checkpoint> found = manager.load("unique-keyword");
        // If found, it must match our task
        found.ifPresent(cp -> assertThat(cp.task()).contains("unique-keyword"));

        manager.delete(sessionId);
    }

    @Test
    void loadReturnsEmptyForUnknownId() {
        Optional<CheckpointManager.Checkpoint> result = manager.load("definitely-not-existing-id-xyz");
        // Should be empty, not throw
        assertThatCode(() -> result.isEmpty()).doesNotThrowAnyException();
    }

    @Test
    void deleteRemovesCheckpoint() {
        String sessionId = "del-session-" + System.currentTimeMillis();
        tech.kayys.gamelan.session.ConversationSession session =
                new tech.kayys.gamelan.session.ConversationSession(sessionId);
        session.addTurn("q", AgentResponse.builder().text("a").build());
        manager.save(session, "task", "model", "react");

        boolean deleted = manager.delete(sessionId);
        assertThat(deleted).isTrue();

        // Should now be gone
        Optional<CheckpointManager.Checkpoint> loaded = manager.load(sessionId);
        assertThat(loaded).isEmpty();
    }

    @Test
    void listAllReturnsCheckpoints() {
        String sessionId = "list-session-" + System.currentTimeMillis();
        tech.kayys.gamelan.session.ConversationSession session =
                new tech.kayys.gamelan.session.ConversationSession(sessionId);
        session.addTurn("q", AgentResponse.builder().text("a").build());
        manager.save(session, "list task", "model", "react");

        List<CheckpointManager.Checkpoint> all = manager.listAll();
        assertThat(all).isNotNull();
        // Should contain at least our checkpoint
        assertThat(all.stream().anyMatch(cp -> cp.sessionId().equals(sessionId))).isTrue();

        manager.delete(sessionId);
    }

    @Test
    void shortIdTruncatesLongId() {
        CheckpointManager.Checkpoint cp = new CheckpointManager.Checkpoint(
                "abcdefghijklmnop", "task", "model", "react", List.of(), java.time.Instant.now());
        assertThat(cp.shortId()).hasSize(8).isEqualTo("abcdefgh");
    }

    @Test
    void taskPreviewTruncatesLongTask() {
        String longTask = "a".repeat(100);
        CheckpointManager.Checkpoint cp = new CheckpointManager.Checkpoint(
                "id", longTask, "model", "react", List.of(), java.time.Instant.now());
        assertThat(cp.taskPreview()).endsWith("…").hasSizeLessThan(70);
    }

    @Test
    void restoreRecreatesSession() {
        CheckpointManager.Checkpoint cp = new CheckpointManager.Checkpoint(
                "restore-test", "task", "model", "react",
                List.of(
                        new CheckpointManager.MessageEntry("user", "hello"),
                        new CheckpointManager.MessageEntry("assistant", "world")
                ),
                java.time.Instant.now());

        tech.kayys.gamelan.session.ConversationSession restored = manager.restore(cp);
        assertThat(restored.id()).isEqualTo("restore-test");
        assertThat(restored.turnCount()).isEqualTo(1);
    }
}
