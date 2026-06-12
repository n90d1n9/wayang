package tech.kayys.gamelan.context;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

/**
 * Automatic project context — scans the working directory at startup and
 * injects relevant project metadata into every agent turn's system prompt.
 *
 * <h2>Why this matters for competing with Claude Code / Qwen-Agent</h2>
 * Claude Code always knows: what project it's in, what language, what
 * framework, what the top-level structure looks like. Without this,
 * a local CLI agent gives generic answers that don't fit the actual codebase.
 *
 * <h2>What we detect</h2>
 * <ul>
 *   <li>Project type (Maven, Gradle, npm, Cargo, Go, Poetry, Mix, etc.)</li>
 *   <li>Primary language(s) by file extension count</li>
 *   <li>Framework signals (Quarkus, Spring, React, FastAPI, etc.) from config files</li>
 *   <li>Git branch and recent commit summary</li>
 *   <li>Top-level directory tree (depth 2, excludes build artifacts)</li>
 *   <li>README excerpt (first 800 chars)</li>
 * </ul>
 *
 * <h2>Cost</h2>
 * The context block is ~300-600 tokens and is cached after the first scan.
 * Re-scan with {@link #refresh()}.
 */
@ApplicationScoped
public class ProjectContext {

    private static final Logger log = LoggerFactory.getLogger(ProjectContext.class);
    private static final int README_PREVIEW_CHARS = 800;
    private static final int MAX_TREE_ENTRIES = 40;

    private volatile String cachedContext;
    private volatile ProjectInfo info;

    @PostConstruct
    void scan() {
        try {
            info = detect();
            cachedContext = buildContextBlock(info);
            log.info("Project context: {} / {} ({})",
                    info.projectType(), info.primaryLanguage(), info.framework());
        } catch (Exception e) {
            log.warn("Project context scan failed: {}", e.getMessage());
            cachedContext = "";
            info = ProjectInfo.unknown();
        }
    }

    /** Returns the cached context block for injection into system prompts. */
    public String contextBlock() {
        return cachedContext != null ? cachedContext : "";
    }

    /** Re-scans the project (call after the user changes directories). */
    public void refresh() {
        scan();
    }

    public ProjectInfo info() { return info; }

    // ── Detection ──────────────────────────────────────────────────────────

    private ProjectInfo detect() throws IOException {
        Path cwd = Path.of(".").toAbsolutePath().normalize();

        // Project type
        String projectType = detectProjectType(cwd);

        // Language stats
        Map<String, Long> langCounts = countLanguages(cwd);
        String primaryLang = langCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey).orElse("unknown");

        // Framework
        String framework = detectFramework(cwd, projectType);

        // Git info
        String gitBranch = runGit(cwd, "git", "branch", "--show-current");
        String gitLog    = runGit(cwd, "git", "log", "--oneline", "-5");

        // README excerpt
        String readme = readmeExcerpt(cwd);

        // Tree
        String tree = buildTree(cwd);

        return new ProjectInfo(cwd.getFileName().toString(), projectType,
                primaryLang, framework, gitBranch, gitLog, readme, tree, langCounts);
    }

    private String detectProjectType(Path cwd) {
        if (exists(cwd, "pom.xml"))             return "maven";
        if (exists(cwd, "build.gradle") || exists(cwd, "build.gradle.kts")) return "gradle";
        if (exists(cwd, "package.json"))        return "npm";
        if (exists(cwd, "Cargo.toml"))          return "cargo";
        if (exists(cwd, "go.mod"))              return "go";
        if (exists(cwd, "pyproject.toml"))      return "python/pyproject";
        if (exists(cwd, "setup.py"))            return "python/setup";
        if (exists(cwd, "requirements.txt"))    return "python/pip";
        if (exists(cwd, "mix.exs"))             return "elixir";
        if (exists(cwd, "CMakeLists.txt"))      return "cmake";
        if (exists(cwd, "Makefile"))            return "make";
        if (exists(cwd, "composer.json"))       return "php/composer";
        if (exists(cwd, "Gemfile"))             return "ruby";
        if (exists(cwd, "*.csproj", true))      return "dotnet";
        return "unknown";
    }

    private String detectFramework(Path cwd, String projectType) {
        // Java frameworks
        if (projectType.startsWith("maven") || projectType.startsWith("gradle")) {
            if (hasDep(cwd, "quarkus"))   return "Quarkus";
            if (hasDep(cwd, "spring"))    return "Spring Boot";
            if (hasDep(cwd, "micronaut")) return "Micronaut";
            if (hasDep(cwd, "vertx"))     return "Vert.x";
        }
        // Node/JS frameworks
        if (projectType.equals("npm")) {
            if (hasDep(cwd, "next"))    return "Next.js";
            if (hasDep(cwd, "react"))   return "React";
            if (hasDep(cwd, "vue"))     return "Vue";
            if (hasDep(cwd, "svelte"))  return "Svelte";
            if (hasDep(cwd, "express")) return "Express";
            if (hasDep(cwd, "fastify")) return "Fastify";
            if (hasDep(cwd, "nest"))    return "NestJS";
        }
        // Python frameworks
        if (projectType.startsWith("python")) {
            if (hasDep(cwd, "fastapi")) return "FastAPI";
            if (hasDep(cwd, "django"))  return "Django";
            if (hasDep(cwd, "flask"))   return "Flask";
        }
        return "";
    }

    private Map<String, Long> countLanguages(Path cwd) {
        Map<String, Long> counts = new LinkedHashMap<>();
        try (Stream<Path> walk = Files.walk(cwd, 4)) {
            walk.filter(Files::isRegularFile)
                .filter(p -> !isExcluded(p))
                .forEach(p -> {
                    String name = p.getFileName().toString();
                    String lang = extensionToLang(name);
                    if (lang != null) counts.merge(lang, 1L, Long::sum);
                });
        } catch (IOException e) {
            // ignore
        }
        return counts;
    }

    private String extensionToLang(String name) {
        if (name.endsWith(".java") || name.endsWith(".kt") || name.endsWith(".scala")) return "JVM";
        if (name.endsWith(".ts") || name.endsWith(".tsx"))   return "TypeScript";
        if (name.endsWith(".js") || name.endsWith(".jsx"))   return "JavaScript";
        if (name.endsWith(".py"))  return "Python";
        if (name.endsWith(".go"))  return "Go";
        if (name.endsWith(".rs"))  return "Rust";
        if (name.endsWith(".rb"))  return "Ruby";
        if (name.endsWith(".cs"))  return "C#";
        if (name.endsWith(".cpp") || name.endsWith(".cc") || name.endsWith(".c")) return "C/C++";
        if (name.endsWith(".ex") || name.endsWith(".exs")) return "Elixir";
        if (name.endsWith(".php")) return "PHP";
        return null;
    }

    private String readmeExcerpt(Path cwd) {
        for (String name : List.of("README.md", "README.MD", "readme.md", "README", "README.txt")) {
            Path f = cwd.resolve(name);
            if (Files.exists(f)) {
                try {
                    String content = Files.readString(f, StandardCharsets.UTF_8);
                    return content.length() > README_PREVIEW_CHARS
                            ? content.substring(0, README_PREVIEW_CHARS) + "\n...(truncated)"
                            : content;
                } catch (IOException ignored) {}
            }
        }
        return "";
    }

    private String buildTree(Path cwd) {
        StringBuilder sb = new StringBuilder();
        sb.append(cwd.getFileName()).append("/\n");
        try (Stream<Path> entries = Files.list(cwd).sorted()) {
            entries.filter(p -> !isExcluded(p))
                   .limit(MAX_TREE_ENTRIES)
                   .forEach(p -> {
                       boolean isDir = Files.isDirectory(p);
                       sb.append("  ").append(p.getFileName())
                         .append(isDir ? "/" : "").append("\n");
                   });
        } catch (IOException ignored) {}
        return sb.toString();
    }

    private String runGit(Path cwd, String... cmd) {
        try {
            ProcessBuilder pb = new ProcessBuilder(cmd)
                    .directory(cwd.toFile()).redirectErrorStream(true);
            Process p = pb.start();
            String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8).strip();
            p.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
            return out;
        } catch (Exception e) { return ""; }
    }

    private String buildContextBlock(ProjectInfo p) {
        StringBuilder sb = new StringBuilder();
        sb.append("## Project Context\n");
        sb.append("- **Directory**: `").append(p.name()).append("`\n");
        sb.append("- **Build system**: ").append(p.projectType()).append("\n");
        sb.append("- **Primary language**: ").append(p.primaryLanguage()).append("\n");
        if (!p.framework().isBlank())
            sb.append("- **Framework**: ").append(p.framework()).append("\n");
        if (!p.gitBranch().isBlank())
            sb.append("- **Git branch**: `").append(p.gitBranch()).append("`\n");
        if (!p.langCounts().isEmpty()) {
            sb.append("- **Languages**: ");
            p.langCounts().entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .forEach(e -> sb.append(e.getKey()).append("(").append(e.getValue()).append(") "));
            sb.append("\n");
        }
        if (!p.tree().isBlank()) {
            sb.append("\n### Directory Structure\n```\n").append(p.tree()).append("```\n");
        }
        if (!p.readme().isBlank()) {
            sb.append("\n### README\n").append(p.readme()).append("\n");
        }
        if (!p.gitLog().isBlank()) {
            sb.append("\n### Recent commits\n```\n").append(p.gitLog()).append("\n```\n");
        }
        return sb.toString();
    }

    private boolean exists(Path cwd, String pattern, boolean glob) {
        if (!glob) return Files.exists(cwd.resolve(pattern));
        try (Stream<Path> s = Files.list(cwd)) {
            return s.anyMatch(p -> p.getFileName().toString().endsWith(
                    pattern.replace("*", "")));
        } catch (IOException e) { return false; }
    }

    private boolean exists(Path cwd, String file) { return exists(cwd, file, false); }

    private boolean hasDep(Path cwd, String keyword) {
        for (String f : List.of("pom.xml","build.gradle","build.gradle.kts",
                "package.json","requirements.txt","pyproject.toml","Cargo.toml")) {
            Path fp = cwd.resolve(f);
            if (Files.exists(fp)) {
                try {
                    String content = Files.readString(fp, StandardCharsets.UTF_8).toLowerCase();
                    if (content.contains(keyword)) return true;
                } catch (IOException ignored) {}
            }
        }
        return false;
    }

    private boolean isExcluded(Path p) {
        String s = p.toString();
        return s.contains("/.git") || s.contains("/target/") || s.contains("/node_modules/")
                || s.contains("/__pycache__") || s.contains("/.gradle") || s.contains("/dist/")
                || s.contains("/build/") || s.contains("/.idea") || s.contains("/.vscode");
    }

    /** Value object for detected project information. */
    public record ProjectInfo(
            String name, String projectType, String primaryLanguage,
            String framework, String gitBranch, String gitLog,
            String readme, String tree, Map<String, Long> langCounts
    ) {
        static ProjectInfo unknown() {
            return new ProjectInfo(".", "unknown", "unknown", "", "", "", "", "", Map.of());
        }
    }
}
