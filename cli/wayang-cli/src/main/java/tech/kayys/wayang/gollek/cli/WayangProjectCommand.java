package tech.kayys.wayang.gollek.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import tech.kayys.wayang.gollek.sdk.session.WayangSessionStore;
import tech.kayys.wayang.sdk.gollek.ProjectStore;
import tech.kayys.wayang.sdk.gollek.model.Project;

import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

@Command(name = "project", description = "Manage projects and grouped sessions. Subcommands: add|remove|rename|export|import|switch. Examples: 'wayang project add --dir . --name myproj', 'wayang project switch <id>'",
        mixinStandardHelpOptions = true)
public final class WayangProjectCommand implements Callable<Integer> {

    @Parameters(index = "0", arity = "1", description = "Action: add|remove|rename|export|import|switch")
    String action;

    @Parameters(index = "1", arity = "0..1", description = "Target (project id or file for import/export)")
    String target;

    @Option(names = {"--dir", "--directory"}, description = "Directory for project (defaults to current directory)")
    String directory;

    @Option(names = {"--name"}, description = "Project display name")
    String name;

    @Option(names = {"--file"}, description = "File path for export/import")
    String filePath;

    @Option(names = {"--switch"}, description = "Switch to project id (alias for action 'switch')")
    String switchTo;

    public WayangGollekCli parent;

    @Override
    public Integer call() {
        PrintStream out = parent == null ? System.out : parent.context().out();
        try {
            // Prefer SDK ProjectStore when available, fall back to older WayangSessionStore
            try {
                ProjectStore store = new ProjectStore(null);
                switch (action) {
                    case "add": {
                        Path dir = directory == null ? Path.of(".") : Path.of(directory);
                        String display = name == null ? dir.getFileName().toString() : name;
                        Project meta = store.createProject(null, display, dir.toAbsolutePath().toString());
                        out.println("Added project: " + meta.id() + " (dir=" + meta.directory() + ")");
                        return 0;
                    }
                    case "remove":
                        if (target == null) { out.println("Usage: wayang project remove <project-id>"); return 2; }
                        store.removeProject(target);
                        out.println("Removed project: " + target);
                        return 0;
                    case "rename":
                        if (target == null || name == null) { out.println("Usage: wayang project rename <project-id> --name <new-name>"); return 2; }
                        store.renameProject(target, name);
                        out.println("Renamed project: " + target + " -> " + name);
                        return 0;
                    case "export":
                        if (target == null || filePath == null) { out.println("Usage: wayang project export <project-id> --file <path>"); return 2; }
                        store.exportProject(target, Path.of(filePath));
                        out.println("Exported project: " + target + " -> " + filePath);
                        return 0;
                    case "import":
                        if (filePath == null) { out.println("Usage: wayang project import --file <path>"); return 2; }
                        store.importProject(Path.of(filePath));
                        out.println("Imported project from: " + filePath);
                        return 0;
                    case "switch":
                        String to = target != null ? target : switchTo;
                        if (to == null) { out.println("Usage: wayang project switch <project-id>"); return 2; }
                        store.switchProject(to);
                        out.println("Switched current project to: " + to);
                        return 0;
                    case "migrate":
                        int migrated = store.migrateLegacySessions();
                        out.println("Migration complete. Projects migrated: " + migrated);
                        return 0;
                    default:
                        out.println("Unknown action: " + action);
                        return 2;
                }
            } catch (NoClassDefFoundError | Exception sdkEx) {
                // Fall back to older WayangSessionStore implementation
                WayangSessionStore store = openStore();
                switch (action) {
                    case "add":
                        Path dir = directory == null ? Path.of(".") : Path.of(directory);
                        var meta = store.addProject(name == null ? dir.getFileName().toString() : name, dir);
                        out.println("Added project: " + meta.id() + " (dir=" + meta.directory() + ")");
                        return 0;
                    case "remove":
                        if (target == null) { out.println("Usage: wayang project remove <project-id>"); return 2; }
                        store.removeProject(target);
                        out.println("Removed project: " + target);
                        return 0;
                    case "rename":
                        if (target == null || name == null) { out.println("Usage: wayang project rename <project-id> --name <new-name>"); return 2; }
                        store.renameProject(target, name);
                        out.println("Renamed project: " + target + " -> " + name);
                        return 0;
                    case "export":
                        if (target == null || filePath == null) { out.println("Usage: wayang project export <project-id> --file <path>"); return 2; }
                        store.exportProject(target, Path.of(filePath));
                        out.println("Exported project: " + target + " -> " + filePath);
                        return 0;
                    case "import":
                        if (filePath == null) { out.println("Usage: wayang project import --file <path>"); return 2; }
                        store.importProject(Path.of(filePath));
                        out.println("Imported project from: " + filePath);
                        return 0;
                    case "switch":
                        String to = target != null ? target : switchTo;
                        if (to == null) { out.println("Usage: wayang project switch <project-id>"); return 2; }
                        // write current project pointer
                        Path cfg = Path.of(System.getProperty("user.home"), ".wayang", "current_project.txt");
                        Files.createDirectories(cfg.getParent());
                        Files.writeString(cfg, to);
                        out.println("Switched current project to: " + to);
                        return 0;
                    case "migrate":
                        out.println("Migration requested, but SDK unavailable in this environment.");
                        out.println("Please use the SDK-enabled CLI or run 'wayang project migrate' after installing the SDK.");
                        return 2;
                    default:
                        out.println("Unknown action: " + action);
                        return 2;
                }
            }
        } catch (Exception e) {
            out.println("Error: " + e.getMessage());
            return 1;
        }
    }

    private WayangSessionStore openStore() throws Exception {
        return new WayangSessionStore();
    }
}
