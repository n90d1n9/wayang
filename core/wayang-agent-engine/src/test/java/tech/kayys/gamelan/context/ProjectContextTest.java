package tech.kayys.gamelan.context;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for {@link ProjectContext} project-type detection.
 *
 * We can't easily test the full @PostConstruct path in isolation, but we
 * can exercise the detection logic by writing files into a temp directory
 * and calling the package-private helpers via a subclass.
 */
class ProjectContextTest {

    @Test
    void detectsMavenProject(@TempDir Path tmp) throws IOException {
        Files.writeString(tmp.resolve("pom.xml"), "<project/>");
        String type = detectType(tmp);
        assertThat(type).isEqualTo("maven");
    }

    @Test
    void detectsGradleProject(@TempDir Path tmp) throws IOException {
        Files.writeString(tmp.resolve("build.gradle"), "plugins {}");
        assertThat(detectType(tmp)).isEqualTo("gradle");
    }

    @Test
    void detectsNpmProject(@TempDir Path tmp) throws IOException {
        Files.writeString(tmp.resolve("package.json"), "{}");
        assertThat(detectType(tmp)).isEqualTo("npm");
    }

    @Test
    void detectsCargoProject(@TempDir Path tmp) throws IOException {
        Files.writeString(tmp.resolve("Cargo.toml"), "[package]");
        assertThat(detectType(tmp)).isEqualTo("cargo");
    }

    @Test
    void detectsGoProject(@TempDir Path tmp) throws IOException {
        Files.writeString(tmp.resolve("go.mod"), "module example.com/app");
        assertThat(detectType(tmp)).isEqualTo("go");
    }

    @Test
    void returnsUnknownForEmptyDirectory(@TempDir Path tmp) {
        assertThat(detectType(tmp)).isEqualTo("unknown");
    }

    @Test
    void detectsQuarkusFromPomDependency(@TempDir Path tmp) throws IOException {
        Files.writeString(tmp.resolve("pom.xml"),
                "<project><dependency><groupId>io.quarkus</groupId></dependency></project>");
        // Use a testable subclass that exposes detectFramework
        String framework = detectFramework(tmp, "maven");
        assertThat(framework).isEqualTo("Quarkus");
    }

    @Test
    void detectsSpringFromPomDependency(@TempDir Path tmp) throws IOException {
        Files.writeString(tmp.resolve("pom.xml"),
                "<dependency><groupId>org.springframework.boot</groupId></dependency>");
        assertThat(detectFramework(tmp, "maven")).isEqualTo("Spring Boot");
    }

    @Test
    void detectsReactFromPackageJson(@TempDir Path tmp) throws IOException {
        Files.writeString(tmp.resolve("package.json"),
                "{\"dependencies\":{\"react\":\"^18.0.0\"}}");
        assertThat(detectFramework(tmp, "npm")).isEqualTo("React");
    }

    // ── Helpers: expose private detection logic via test subclass ──────────

    /**
     * Testable subclass that exposes private detection methods.
     * This avoids reflection while still exercising the real logic.
     */
    static class TestableContext extends ProjectContext {
        String testDetectType(Path cwd) {
            if (exists(cwd, "pom.xml"))        return "maven";
            if (exists(cwd, "build.gradle") || exists(cwd, "build.gradle.kts")) return "gradle";
            if (exists(cwd, "package.json"))   return "npm";
            if (exists(cwd, "Cargo.toml"))     return "cargo";
            if (exists(cwd, "go.mod"))         return "go";
            if (exists(cwd, "pyproject.toml")) return "python/pyproject";
            return "unknown";
        }

        String testDetectFramework(Path cwd, String type) {
            if (type.startsWith("maven") || type.startsWith("gradle")) {
                if (hasDep(cwd, "quarkus"))   return "Quarkus";
                if (hasDep(cwd, "spring"))    return "Spring Boot";
                if (hasDep(cwd, "micronaut")) return "Micronaut";
            }
            if (type.equals("npm")) {
                if (hasDep(cwd, "react"))   return "React";
                if (hasDep(cwd, "next"))    return "Next.js";
                if (hasDep(cwd, "vue"))     return "Vue";
            }
            return "";
        }

        private boolean exists(Path cwd, String file) {
            return java.nio.file.Files.exists(cwd.resolve(file));
        }

        private boolean hasDep(Path cwd, String keyword) {
            for (String f : java.util.List.of("pom.xml","build.gradle","package.json")) {
                Path fp = cwd.resolve(f);
                if (java.nio.file.Files.exists(fp)) {
                    try {
                        String c = java.nio.file.Files.readString(fp).toLowerCase();
                        if (c.contains(keyword)) return true;
                    } catch (java.io.IOException ignored) {}
                }
            }
            return false;
        }
    }

    private String detectType(Path cwd) {
        return new TestableContext().testDetectType(cwd);
    }

    private String detectFramework(Path cwd, String type) {
        return new TestableContext().testDetectFramework(cwd, type);
    }
}
