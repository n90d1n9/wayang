package tech.kayys.wayang.gollek.sdk;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WayangContextEnvelopesTest {

    @Test
    void workspaceEnvelopeOwnsPublishedWorkspaceShape() {
        WorkspaceSnapshot snapshot = new WorkspaceSnapshot(
                " /repo ",
                true,
                true,
                true,
                " /repo ",
                " main ",
                List.of("pom.xml"),
                List.of("maven"),
                List.of("wayang-gollek-sdk"),
                List.of("src/main/java"),
                List.of("java workspace"));

        Map<String, Object> values = WayangContextEnvelopes.workspace(snapshot);

        assertThat(values)
                .containsEntry("rootPath", "/repo")
                .containsEntry("exists", true)
                .containsEntry("directory", true)
                .containsEntry("gitRepository", true)
                .containsEntry("gitRoot", "/repo")
                .containsEntry("branch", "main")
                .containsEntry("buildFiles", List.of("pom.xml"))
                .containsEntry("packageManagers", List.of("maven"))
                .containsEntry("modules", List.of("wayang-gollek-sdk"))
                .containsEntry("importantPaths", List.of("src/main/java"))
                .containsEntry("notes", List.of("java workspace"));
    }

    @Test
    void harnessEnvelopeIncludesFullWorkspaceAndCheckDetails() {
        WorkspaceSnapshot workspace = new WorkspaceSnapshot(
                "/repo",
                true,
                true,
                false,
                "",
                "",
                List.of("package.json"),
                List.of("npm"),
                List.of("web"),
                List.of("src"),
                List.of());
        HarnessCheck check = new HarnessCheck(
                "npm-test",
                "NPM Test",
                List.of("npm", "test"),
                "/repo",
                false,
                "package.json detected");
        HarnessPlan plan = new HarnessPlan(workspace, List.of(check), List.of("prefer local test"));

        Map<String, Object> values = WayangContextEnvelopes.harness(plan);

        assertThat(objectMap(values.get("workspace")))
                .containsEntry("rootPath", "/repo")
                .containsEntry("gitRoot", "")
                .containsEntry("branch", "");
        assertThat(list(values.get("checks")))
                .singleElement()
                .satisfies(entry -> assertThat(objectMap(entry))
                        .containsEntry("id", "npm-test")
                        .containsEntry("label", "NPM Test")
                        .containsEntry("command", List.of("npm", "test"))
                        .containsEntry("commandLine", "npm test")
                        .containsEntry("workingDirectory", "/repo")
                        .containsEntry("optional", false)
                        .containsEntry("reason", "package.json detected"));
        assertThat(values).containsEntry("notes", List.of("prefer local test"));
    }

    @Test
    void nullInputsNormalizeToEmptyContextEnvelopes() {
        Map<String, Object> workspace = WayangContextEnvelopes.workspace(null);
        Map<String, Object> harness = WayangContextEnvelopes.harness(null);

        assertThat(workspace)
                .containsEntry("rootPath", ".")
                .containsEntry("exists", false)
                .containsEntry("gitRoot", "")
                .containsEntry("branch", "")
                .containsEntry("buildFiles", List.of())
                .containsEntry("notes", List.of());
        assertThat(harness)
                .containsEntry("checks", List.of())
                .containsEntry("notes", List.of());
        assertThat(objectMap(harness.get("workspace"))).containsEntry("rootPath", ".");
        assertThatThrownBy(() -> workspace.put("extra", "value"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> harness.put("extra", "value"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> objectMap(Object value) {
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> list(Object value) {
        return (List<Object>) value;
    }
}
