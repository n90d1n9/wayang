package tech.kayys.wayang.client;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

final public class LocalWorkspaceInspector {

    private static final List<String> BUILD_FILE_NAMES = List.of(
            "pom.xml",
            "build.gradle",
            "build.gradle.kts",
            "settings.gradle",
            "settings.gradle.kts",
            "package.json",
            "pyproject.toml",
            "Cargo.toml",
            "go.mod",
            "Makefile");

   public  WorkspaceSnapshot inspect(WorkspaceInspectionRequest request) {
        WorkspaceInspectionRequest normalized = request == null ? WorkspaceInspectionRequest.current() : request;
        Path root = Paths.get(normalized.rootPath()).toAbsolutePath().normalize();
        boolean exists = Files.exists(root);
        boolean directory = Files.isDirectory(root);
        if (!exists || !directory) {
            return new WorkspaceSnapshot(
                    root.toString(),
                    exists,
                    directory,
                    false,
                    "",
                    "",
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of("Workspace path is not an existing directory."));
        }

        Path gitRoot = findGitRoot(root);
        List<String> buildFiles = buildFiles(root);
        return new WorkspaceSnapshot(
                root.toString(),
                true,
                true,
                gitRoot != null,
                gitRoot == null ? "" : gitRoot.toString(),
                gitRoot == null ? "" : branch(gitRoot),
                buildFiles,
                packageManagers(buildFiles),
                modules(root),
                importantPaths(root, normalized),
                notes(root, gitRoot, buildFiles));
    }

    private Path findGitRoot(Path root) {
        Path current = root;
        while (current != null) {
            if (Files.exists(current.resolve(".git"))) {
                return current;
            }
            current = current.getParent();
        }
        return null;
    }

    private String branch(Path gitRoot) {
        Path gitDirectory = gitDirectory(gitRoot);
        if (gitDirectory == null) {
            return "";
        }
        Path head = gitDirectory.resolve("HEAD");
        if (!Files.isRegularFile(head)) {
            return "";
        }
        try {
            String value = Files.readString(head).trim();
            String prefix = "ref: refs/heads/";
            return value.startsWith(prefix) ? value.substring(prefix.length()) : value;
        } catch (IOException e) {
            return "";
        }
    }

    private Path gitDirectory(Path gitRoot) {
        Path dotGit = gitRoot.resolve(".git");
        if (Files.isDirectory(dotGit)) {
            return dotGit;
        }
        if (!Files.isRegularFile(dotGit)) {
            return null;
        }
        try {
            String value = Files.readString(dotGit).trim();
            String prefix = "gitdir:";
            if (!value.startsWith(prefix)) {
                return null;
            }
            Path path = Paths.get(value.substring(prefix.length()).trim());
            return path.isAbsolute() ? path : gitRoot.resolve(path).normalize();
        } catch (IOException e) {
            return null;
        }
    }

    private List<String> buildFiles(Path root) {
        List<String> files = new ArrayList<>();
        for (String fileName : BUILD_FILE_NAMES) {
            if (Files.isRegularFile(root.resolve(fileName))) {
                files.add(fileName);
            }
        }
        return files;
    }

    private List<String> packageManagers(List<String> buildFiles) {
        Set<String> managers = new LinkedHashSet<>();
        for (String file : buildFiles) {
            switch (file) {
                case "pom.xml" -> managers.add("maven");
                case "build.gradle", "build.gradle.kts", "settings.gradle", "settings.gradle.kts" -> managers.add("gradle");
                case "package.json" -> managers.add("npm");
                case "pyproject.toml" -> managers.add("python");
                case "Cargo.toml" -> managers.add("cargo");
                case "go.mod" -> managers.add("go");
                case "Makefile" -> managers.add("make");
                default -> {
                }
            }
        }
        return List.copyOf(managers);
    }

    private List<String> modules(Path root) {
        Set<String> modules = new LinkedHashSet<>(mavenModules(root.resolve("pom.xml")));
        try (Stream<Path> stream = Files.list(root)) {
            stream.filter(Files::isDirectory)
                    .filter(path -> !path.getFileName().toString().startsWith("."))
                    .filter(this::looksLikeModule)
                    .map(path -> path.getFileName().toString())
                    .sorted()
                    .forEach(modules::add);
        } catch (IOException e) {
            modules.add("unable-to-list-modules");
        }
        return List.copyOf(modules);
    }

    private boolean looksLikeModule(Path path) {
        return BUILD_FILE_NAMES.stream().anyMatch(name -> Files.isRegularFile(path.resolve(name)));
    }

    private List<String> mavenModules(Path pom) {
        if (!Files.isRegularFile(pom)) {
            return List.of();
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setExpandEntityReferences(false);
            Document document = factory.newDocumentBuilder().parse(pom.toFile());
            NodeList moduleNodes = document.getElementsByTagName("module");
            List<String> modules = new ArrayList<>();
            for (int i = 0; i < moduleNodes.getLength(); i++) {
                String value = moduleNodes.item(i).getTextContent().trim();
                if (!value.isEmpty()) {
                    modules.add(value);
                }
            }
            return modules;
        } catch (Exception e) {
            return List.of("unable-to-parse-maven-modules");
        }
    }

    private List<String> importantPaths(Path root, WorkspaceInspectionRequest request) {
        try (Stream<Path> stream = Files.list(root)) {
            return stream
                    .filter(path -> request.includeHidden() || !path.getFileName().toString().startsWith("."))
                    .map(path -> path.getFileName().toString() + (Files.isDirectory(path) ? "/" : ""))
                    .sorted()
                    .limit(request.maxEntries())
                    .toList();
        } catch (IOException e) {
            return List.of("unable-to-list-workspace");
        }
    }

    private List<String> notes(Path root, Path gitRoot, List<String> buildFiles) {
        List<String> notes = new ArrayList<>();
        notes.add("Workspace inspected locally by Wayang SDK.");
        if (gitRoot != null) {
            notes.add("Git root: " + gitRoot);
        }
        if (!buildFiles.isEmpty()) {
            notes.add("Build descriptors: " + String.join(", ", buildFiles));
        }
        if (!root.equals(gitRoot) && gitRoot != null) {
            notes.add("Workspace path is inside a larger git repository.");
        }
        return notes;
    }
}
