package tech.kayys.wayang.client;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import tech.kayys.wayang.harness.HarnessCheck;
import tech.kayys.wayang.harness.HarnessPlan;
import tech.kayys.wayang.harness.HarnessPlanRequest;

final public class LocalHarnessPlanner {

    private final LocalWorkspaceInspector workspaceInspector;

    LocalHarnessPlanner() {
        this(new LocalWorkspaceInspector());
    }

   public  LocalHarnessPlanner(LocalWorkspaceInspector workspaceInspector) {
        this.workspaceInspector = workspaceInspector == null ? new LocalWorkspaceInspector() : workspaceInspector;
    }

    public HarnessPlan plan(HarnessPlanRequest request) {
        HarnessPlanRequest normalized = request == null ? HarnessPlanRequest.current() : request;
        WorkspaceSnapshot workspace = workspaceInspector.inspect(new WorkspaceInspectionRequest(
                normalized.rootPath(),
                80,
                false));
        Map<String, HarnessCheck> checks = new LinkedHashMap<>();
        List<String> notes = new ArrayList<>();
        notes.add("Harness plan generated locally by Wayang SDK.");
        notes.add("Commands are planned, not executed.");

        if (!workspace.exists() || !workspace.directory()) {
            notes.add("No checks were planned because the workspace path is not an existing directory.");
            return new HarnessPlan(workspace, List.of(), notes);
        }

        Path root = Paths.get(workspace.rootPath()).toAbsolutePath().normalize();
        planMaven(root, workspace, checks);
        planGradle(root, workspace, checks);
        planJavaScript(root, workspace, checks);
        planPython(root, workspace, checks);
        planGo(root, workspace, checks);
        planCargo(root, workspace, checks);
        planMake(root, workspace, checks);

        List<HarnessCheck> selected = checks.values().stream()
                .filter(check -> normalized.includeOptional() || !check.optional())
                .limit(normalized.maxChecks())
                .toList();
        if (selected.isEmpty()) {
            notes.add("No known build or test harness was detected from workspace descriptors.");
        }
        return new HarnessPlan(workspace, selected, notes);
    }

    private void planMaven(Path root, WorkspaceSnapshot workspace, Map<String, HarnessCheck> checks) {
        if (!workspace.buildFiles().contains("pom.xml")) {
            return;
        }
        put(checks, new HarnessCheck(
                "maven-compile",
                "Maven compile smoke",
                List.of("mvn", "-q", "compile", "-DskipTests"),
                root.toString(),
                false,
                "Validate Java source and module wiring without running tests."));
        put(checks, new HarnessCheck(
                "maven-test",
                "Maven test suite",
                List.of("mvn", "-q", "test"),
                root.toString(),
                false,
                "Run the Maven unit-test harness for the workspace."));
    }

    private void planGradle(Path root, WorkspaceSnapshot workspace, Map<String, HarnessCheck> checks) {
        if (!workspace.packageManagers().contains("gradle")) {
            return;
        }
        String runner = Files.isRegularFile(root.resolve("gradlew")) ? "./gradlew" : "gradle";
        put(checks, new HarnessCheck(
                "gradle-test",
                "Gradle test suite",
                List.of(runner, "test"),
                root.toString(),
                false,
                "Run the Gradle test harness using the wrapper when present."));
    }

    private void planJavaScript(Path root, WorkspaceSnapshot workspace, Map<String, HarnessCheck> checks) {
        if (!workspace.buildFiles().contains("package.json")) {
            return;
        }
        String runner = javascriptRunner(root);
        put(checks, new HarnessCheck(
                "javascript-test",
                runner + " test",
                List.of(runner, "test"),
                root.toString(),
                true,
                "Run the JavaScript test script when the package defines one."));
        put(checks, new HarnessCheck(
                "javascript-lint",
                runner + " lint",
                List.of(runner, "run", "lint"),
                root.toString(),
                true,
                "Run the JavaScript lint script when the package defines one."));
    }

    private String javascriptRunner(Path root) {
        if (Files.isRegularFile(root.resolve("pnpm-lock.yaml"))) {
            return "pnpm";
        }
        if (Files.isRegularFile(root.resolve("yarn.lock"))) {
            return "yarn";
        }
        if (Files.isRegularFile(root.resolve("bun.lock")) || Files.isRegularFile(root.resolve("bun.lockb"))) {
            return "bun";
        }
        return "npm";
    }

    private void planPython(Path root, WorkspaceSnapshot workspace, Map<String, HarnessCheck> checks) {
        if (!workspace.buildFiles().contains("pyproject.toml")) {
            return;
        }
        put(checks, new HarnessCheck(
                "python-test",
                "Python pytest suite",
                List.of("python", "-m", "pytest"),
                root.toString(),
                true,
                "Run pytest when the project uses pytest-compatible tests."));
    }

    private void planGo(Path root, WorkspaceSnapshot workspace, Map<String, HarnessCheck> checks) {
        if (!workspace.buildFiles().contains("go.mod")) {
            return;
        }
        put(checks, new HarnessCheck(
                "go-test",
                "Go test suite",
                List.of("go", "test", "./..."),
                root.toString(),
                false,
                "Run all Go package tests."));
    }

    private void planCargo(Path root, WorkspaceSnapshot workspace, Map<String, HarnessCheck> checks) {
        if (!workspace.buildFiles().contains("Cargo.toml")) {
            return;
        }
        put(checks, new HarnessCheck(
                "cargo-test",
                "Cargo test suite",
                List.of("cargo", "test"),
                root.toString(),
                false,
                "Run Rust unit and integration tests."));
    }

    private void planMake(Path root, WorkspaceSnapshot workspace, Map<String, HarnessCheck> checks) {
        if (!workspace.buildFiles().contains("Makefile")) {
            return;
        }
        put(checks, new HarnessCheck(
                "make-test",
                "Make test target",
                List.of("make", "test"),
                root.toString(),
                true,
                "Run the conventional Makefile test target when available."));
    }

    private void put(Map<String, HarnessCheck> checks, HarnessCheck check) {
        checks.putIfAbsent(check.id(), check);
    }
}
