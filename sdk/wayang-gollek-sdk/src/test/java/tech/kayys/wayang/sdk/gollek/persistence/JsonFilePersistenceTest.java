package tech.kayys.wayang.sdk.gollek.persistence;

import org.junit.jupiter.api.Test;

import tech.kayys.wayang.sdk.gollek.model.Project;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class JsonFilePersistenceTest {

    @Test
    public void basicLifecycle() throws Exception {
        Path tmp = Files.createTempDirectory("wf-test-");
        try {
            JsonFilePersistence p = new JsonFilePersistence(tmp);
            Project proj = new Project("p1", "MyProj", "/tmp/proj");
            p.saveProject(proj);

            var list = p.listProjects();
            assertTrue(list.stream().anyMatch(pr -> pr.id().equals("p1")));

            p.setCurrentProjectId("p1");
            assertEquals("p1", p.getCurrentProjectId());

            Path out = tmp.resolve("export.json");
            p.exportProject("p1", out);
            assertTrue(Files.exists(out));

            var imported = p.importProject(out);
            assertNotNull(imported);
            assertEquals("p1", imported.id());

            p.removeProject("p1");
            var after = p.listProjects();
            assertFalse(after.stream().anyMatch(pr -> pr.id().equals("p1")));
        } finally {
            try { Files.walk(tmp).sorted(java.util.Comparator.reverseOrder()).forEach(p -> p.toFile().delete()); } catch (Exception ignored) {}
        }
    }
}
