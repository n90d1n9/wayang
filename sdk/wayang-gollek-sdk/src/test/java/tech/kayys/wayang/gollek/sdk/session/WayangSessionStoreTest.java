package tech.kayys.wayang.gollek.sdk.session;

import org.junit.jupiter.api.Test;
import tech.kayys.gollek.spi.Message;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class WayangSessionStoreTest {

    @Test
    public void addAndListProjectAndSession() throws Exception {
        WayangSessionStore store = new WayangSessionStore();
        Path tmp = Files.createTempDirectory("wayang-test-");
        var meta = store.addProject("testproj", tmp);
        assertNotNull(meta);
        assertTrue(store.listProjects().contains(meta.id()));

        // Save a simple transcript
        Message[] msgs = new Message[] { Message.system("sys"), Message.user("hello"), Message.assistant("world") };
        store.saveTranscript(meta.id(), "s1", List.of(msgs));
        var sessions = store.listSessions(meta.id());
        assertTrue(sessions.contains("s1"));

        var loaded = store.loadTranscript(meta.id(), "s1");
        assertEquals(3, loaded.size());
    }
}
