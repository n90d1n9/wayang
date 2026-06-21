package tech.kayys.wayang.sdk.gollek;

import org.junit.jupiter.api.Test;

import tech.kayys.wayang.sdk.gollek.model.Project;
import tech.kayys.wayang.sdk.gollek.model.Session;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class ProjectStoreTest {

    @Test
    public void storeWorkflow() throws Exception {
        Path tmp = Files.createTempDirectory("ps-test-");
        try {
            ProjectStore store = new ProjectStore(tmp);
            Project p = store.createProject("pid-1", "TestProject", ".");
            assertNotNull(p);
            var list = store.listProjects();
            assertTrue(list.stream().anyMatch(pr -> pr.id().equals("pid-1")));

            store.switchProject("pid-1");
            assertEquals("pid-1", store.currentProject());

            Session s = store.createSession("pid-1", "sess-1");
            assertNotNull(s);
            var projects = store.listProjects();
            var proj = projects.stream().filter(pr -> pr.id().equals("pid-1")).findFirst().orElse(null);
            assertNotNull(proj);
            assertTrue(proj.sessions().stream().anyMatch(ss -> ss.id().equals(s.id())));

            store.removeProject("pid-1");
            var after = store.listProjects();
            assertFalse(after.stream().anyMatch(pr -> pr.id().equals("pid-1")));
        } finally {
            try { Files.walk(tmp).sorted(java.util.Comparator.reverseOrder()).forEach(p -> p.toFile().delete()); } catch (Exception ignored) {}
        }
    }
}
