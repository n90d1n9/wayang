package tech.kayys.wayang.gollek.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;
import tech.kayys.wayang.skill.spi.SkillDefinition;
import tech.kayys.wayang.skills.store.SkillEntry;
import tech.kayys.wayang.skills.store.WayangSkillStore;

import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

/**
 * {@code wayang skills} — full CRUD skill management commands.
 *
 * <p>All CRUD operations use {@link WayangSkillStore} which seamlessly
 * merges builtin (classpath), user (~/.wayang/skills/), custom load paths,
 * and the enterprise DB backend (if available) into one unified index.
 */
final class WayangSkillCommands {

    private WayangSkillCommands() {}

    @Command(
            name = "skills",
            aliases = "capabilities",
            description = "Manage Wayang skills (list, create, delete, import, export, load-paths).",
            mixinStandardHelpOptions = true,
            subcommands = {
                    SkillsCommand.ListCommand.class,
                    SkillsCommand.InspectCommand.class,
                    SkillsCommand.SearchCommand.class,
                    SkillsCommand.CreateCommand.class,
                    SkillsCommand.DeleteCommand.class,
                    SkillsCommand.EnableCommand.class,
                    SkillsCommand.DisableCommand.class,
                    SkillsCommand.ImportCommand.class,
                    SkillsCommand.ExportCommand.class,
                    SkillsCommand.LoadPathCommand.class
            })
    static final class SkillsCommand implements Callable<Integer> {

        @ParentCommand
        WayangGollekCli parent;

        @Override
        public Integer call() {
            // Default: list all skills
            return new ListCommand().callWithParent(this);
        }

        // ─────────────────────────────────────────────────────────────────────
        // list
        // ─────────────────────────────────────────────────────────────────────

        @Command(name = "list", aliases = "ls",
                description = "List all skills. Filter by --source or --category.")
        static final class ListCommand implements Callable<Integer> {
            @ParentCommand SkillsCommand parent;

            @Option(names = {"--source", "-s"},
                    description = "Filter by source: builtin, user, custom, db")
            String source;

            @Option(names = {"--category", "-c"},
                    description = "Filter by category")
            String category;

            @Option(names = {"--all", "-a"},
                    description = "Include disabled skills")
            boolean all;

            @Option(names = {"--json"}, description = "Output as JSON")
            boolean json;

            @Override
            public Integer call() { return callWithParent(parent); }

            Integer callWithParent(SkillsCommand p) {
                PrintStream out = p.parent.context().out();
                WayangSkillStore store = WayangSkillStore.getInstance();
                List<SkillEntry> entries;

                if (source != null && !source.isBlank()) {
                    entries = store.listBySource(source);
                } else if (category != null && !category.isBlank()) {
                    entries = store.listByCategory(category);
                } else if (all) {
                    entries = store.listAll();
                } else {
                    entries = store.list();
                }

                if (json) {
                    printJson(out, entries);
                } else {
                    printTable(out, entries, store);
                }
                return 0;
            }

            private void printTable(PrintStream out, List<SkillEntry> entries, WayangSkillStore store) {
                out.println();
                out.println("  Wayang Skills");
                out.println("  " + store.summary());
                out.println();
                out.printf("  %-30s %-10s %-12s %s%n", "ID", "SOURCE", "CATEGORY", "DESCRIPTION");
                out.println("  " + "─".repeat(80));
                for (SkillEntry e : entries) {
                    String desc = e.description() != null ? e.description() : "";
                    if (desc.length() > 40) desc = desc.substring(0, 37) + "...";
                    String disabled = e.enabled() ? "" : " [disabled]";
                    out.printf("  %-30s %-10s %-12s %s%s%n",
                            e.id(), e.source(), e.category() != null ? e.category() : "-",
                            desc, disabled);
                }
                out.println();
                out.println("  " + entries.size() + " skill(s). Use 'skills inspect <id>' for details.");
                out.println();
            }

            private void printJson(PrintStream out, List<SkillEntry> entries) {
                out.println("[");
                for (int i = 0; i < entries.size(); i++) {
                    SkillEntry e = entries.get(i);
                    out.printf("  {\"id\":\"%s\",\"name\":\"%s\",\"source\":\"%s\",\"category\":\"%s\",\"enabled\":%b}%s%n",
                            esc(e.id()), esc(e.name()), esc(e.source()),
                            esc(e.category()), e.enabled(),
                            i < entries.size() - 1 ? "," : "");
                }
                out.println("]");
            }
        }

        // ─────────────────────────────────────────────────────────────────────
        // inspect
        // ─────────────────────────────────────────────────────────────────────

        @Command(name = "inspect", description = "Show full details of a skill.")
        static final class InspectCommand implements Callable<Integer> {
            @ParentCommand SkillsCommand parent;

            @Parameters(index = "0", description = "Skill id to inspect.")
            String id;

            @Option(names = {"--json"}) boolean json;

            @Override
            public Integer call() {
                PrintStream out = parent.parent.context().out();
                Optional<SkillEntry> entry = WayangSkillStore.getInstance().find(id);
                if (entry.isEmpty()) {
                    out.println("  Skill '" + id + "' not found. Use 'skills list' to see available skills.");
                    return 1;
                }
                SkillEntry e = entry.get();
                if (json) {
                    out.println("{\"id\":\"" + esc(e.id()) + "\",\"name\":\"" + esc(e.name()) + "\","
                            + "\"source\":\"" + esc(e.source()) + "\",\"category\":\"" + esc(e.category()) + "\","
                            + "\"enabled\":" + e.enabled() + ",\"readOnly\":" + e.readOnly() + ","
                            + "\"path\":\"" + esc(e.path()) + "\","
                            + "\"description\":\"" + esc(e.description()) + "\"}");
                } else {
                    SkillDefinition def = e.definition();
                    out.println();
                    out.println("  ┌──────────────────────────────────────────────────────────────────");
                    out.println("  │  Skill: " + e.id());
                    out.println("  ├──────────────────────────────────────────────────────────────────");
                    out.println("  │  Name       : " + (def.name() != null ? def.name() : e.name()));
                    out.println("  │  Source     : " + e.source() + (e.readOnly() ? " (read-only)" : ""));
                    out.println("  │  Category   : " + (def.category() != null ? def.category() : "-"));
                    out.println("  │  Enabled    : " + e.enabled());
                    out.println("  │  Format     : " + e.format());
                    out.println("  │  Path       : " + (e.path() != null ? e.path() : "(classpath)"));
                    if (def.description() != null && !def.description().isBlank())
                        out.println("  │  Description: " + def.description());
                    if (def.defaultProvider() != null)
                        out.println("  │  Provider   : " + def.defaultProvider());
                    if (def.temperature() != null)
                        out.println("  │  Temperature: " + def.temperature());
                    if (def.maxTokens() != null)
                        out.println("  │  MaxTokens  : " + def.maxTokens());
                    if (!def.tools().isEmpty())
                        out.println("  │  Tools      : " + String.join(", ", def.tools()));
                    out.println("  ├──────────────────────────────────────────────────────────────────");
                    out.println("  │  System Prompt:");
                    if (def.systemPrompt() != null) {
                        for (String line : def.systemPrompt().split("\n")) {
                            out.println("  │    " + (line.length() > 100 ? line.substring(0, 97) + "..." : line));
                        }
                    }
                    out.println("  └──────────────────────────────────────────────────────────────────");
                    out.println();
                }
                return 0;
            }
        }

        // ─────────────────────────────────────────────────────────────────────
        // search
        // ─────────────────────────────────────────────────────────────────────

        @Command(name = "search", description = "Search skills by keyword.")
        static final class SearchCommand implements Callable<Integer> {
            @ParentCommand SkillsCommand parent;

            @Parameters(index = "0", description = "Search term.")
            String term;

            @Override
            public Integer call() {
                PrintStream out = parent.parent.context().out();
                List<SkillEntry> results = WayangSkillStore.getInstance().search(term);
                out.println();
                if (results.isEmpty()) {
                    out.println("  No skills found matching '" + term + "'.");
                } else {
                    out.println("  Found " + results.size() + " skill(s) matching '" + term + "':");
                    out.println();
                    for (SkillEntry e : results) out.println(e.toSummaryLine());
                }
                out.println();
                return 0;
            }
        }

        // ─────────────────────────────────────────────────────────────────────
        // create
        // ─────────────────────────────────────────────────────────────────────

        @Command(name = "create", description = "Create a new user skill.")
        static final class CreateCommand implements Callable<Integer> {
            @ParentCommand SkillsCommand parent;

            @Option(names = {"--id", "-i"}, required = true, description = "Unique skill id (e.g. my-coder)")
            String id;

            @Option(names = {"--name", "-n"}, description = "Human-readable name")
            String name;

            @Option(names = {"--description", "-d"}, description = "Short description of the skill")
            String description;

            @Option(names = {"--category", "-c"}, description = "Category (default: custom)",
                    defaultValue = "custom")
            String category;

            @Option(names = {"--prompt", "-p"}, required = true,
                    description = "System prompt text for this skill")
            String systemPrompt;

            @Option(names = {"--tools", "-t"}, split = ",",
                    description = "Comma-separated list of tool ids (e.g. bash,read_file)")
            List<String> tools;

            @Option(names = {"--provider"}, description = "Default LLM provider id")
            String provider;

            @Option(names = {"--temperature"}, description = "Inference temperature (0.0–1.0)")
            Double temperature;

            @Option(names = {"--max-tokens"}, description = "Maximum tokens per response")
            Integer maxTokens;

            @Override
            public Integer call() {
                PrintStream out = parent.parent.context().out();
                try {
                    SkillDefinition.Builder builder = SkillDefinition.builder()
                            .id(id)
                            .name(name != null ? name : id)
                            .description(description)
                            .category(category)
                            .systemPrompt(systemPrompt)
                            .defaultProvider(provider)
                            .tools(tools != null ? tools : List.of());
                    if (temperature != null) builder.temperature(temperature);
                    if (maxTokens != null)   builder.maxTokens(maxTokens);

                    SkillEntry entry = WayangSkillStore.getInstance().create(builder.build());
                    out.println();
                    out.println("  ✓ Skill '" + entry.id() + "' created.");
                    out.println("    Path: " + entry.path());
                    out.println("    Use 'skills inspect " + entry.id() + "' to verify.");
                    out.println();
                    return 0;
                } catch (Exception e) {
                    out.println("  ✗ Failed to create skill: " + e.getMessage());
                    return 1;
                }
            }
        }

        // ─────────────────────────────────────────────────────────────────────
        // delete
        // ─────────────────────────────────────────────────────────────────────

        @Command(name = "delete", aliases = "rm",
                description = "Delete a skill. Use --force to delete built-in skills.")
        static final class DeleteCommand implements Callable<Integer> {
            @ParentCommand SkillsCommand parent;

            @Parameters(index = "0", description = "Skill id to delete.")
            String id;

            @Option(names = {"--force", "-f"},
                    description = "Force deletion even for built-in skills.")
            boolean force;

            @Override
            public Integer call() {
                PrintStream out = parent.parent.context().out();
                try {
                    WayangSkillStore.getInstance().delete(id, force);
                    out.println("  ✓ Skill '" + id + "' deleted.");
                    return 0;
                } catch (Exception e) {
                    out.println("  ✗ " + e.getMessage());
                    return 1;
                }
            }
        }

        // ─────────────────────────────────────────────────────────────────────
        // enable / disable
        // ─────────────────────────────────────────────────────────────────────

        @Command(name = "enable", description = "Enable a disabled skill.")
        static final class EnableCommand implements Callable<Integer> {
            @ParentCommand SkillsCommand parent;
            @Parameters(index = "0") String id;

            @Override
            public Integer call() {
                PrintStream out = parent.parent.context().out();
                try {
                    WayangSkillStore.getInstance().enable(id);
                    out.println("  ✓ Skill '" + id + "' enabled.");
                    return 0;
                } catch (Exception e) { out.println("  ✗ " + e.getMessage()); return 1; }
            }
        }

        @Command(name = "disable", description = "Disable a skill without deleting it.")
        static final class DisableCommand implements Callable<Integer> {
            @ParentCommand SkillsCommand parent;
            @Parameters(index = "0") String id;

            @Override
            public Integer call() {
                PrintStream out = parent.parent.context().out();
                try {
                    WayangSkillStore.getInstance().disable(id);
                    out.println("  ✓ Skill '" + id + "' disabled.");
                    return 0;
                } catch (Exception e) { out.println("  ✗ " + e.getMessage()); return 1; }
            }
        }

        // ─────────────────────────────────────────────────────────────────────
        // import / export
        // ─────────────────────────────────────────────────────────────────────

        @Command(name = "import", description = "Import a skill from a SKILL.md, JSON, or YAML file.")
        static final class ImportCommand implements Callable<Integer> {
            @ParentCommand SkillsCommand parent;

            @Parameters(index = "0", description = "Path to SKILL.md, .json, or .yaml file.")
            String filePath;

            @Override
            public Integer call() {
                PrintStream out = parent.parent.context().out();
                try {
                    Path file = Paths.get(filePath);
                    SkillEntry entry = WayangSkillStore.getInstance().importFrom(file);
                    out.println("  ✓ Imported skill '" + entry.id() + "' from " + file);
                    out.println("    Path: " + entry.path());
                    return 0;
                } catch (Exception e) {
                    out.println("  ✗ Import failed: " + e.getMessage());
                    return 1;
                }
            }
        }

        @Command(name = "export", description = "Export a skill to a SKILL.md, JSON, or YAML file.")
        static final class ExportCommand implements Callable<Integer> {
            @ParentCommand SkillsCommand parent;

            @Parameters(index = "0", description = "Skill id to export.")
            String id;

            @Option(names = {"--output", "-o"}, description = "Output file path (default: <id>.md)")
            String outputPath;

            @Override
            public Integer call() {
                PrintStream out = parent.parent.context().out();
                try {
                    Path file = outputPath != null ? Paths.get(outputPath) : Paths.get(id + ".md");
                    WayangSkillStore.getInstance().export(id, file);
                    out.println("  ✓ Skill '" + id + "' exported to " + file.toAbsolutePath());
                    return 0;
                } catch (Exception e) {
                    out.println("  ✗ Export failed: " + e.getMessage());
                    return 1;
                }
            }
        }

        // ─────────────────────────────────────────────────────────────────────
        // load-path
        // ─────────────────────────────────────────────────────────────────────

        @Command(name = "load-path",
                description = "Manage custom skill load paths.",
                mixinStandardHelpOptions = true,
                subcommands = {
                        LoadPathCommand.AddCommand.class,
                        LoadPathCommand.RemoveCommand.class,
                        LoadPathCommand.ListCommand.class
                })
        static final class LoadPathCommand implements Callable<Integer> {
            @ParentCommand SkillsCommand parent;

            @Override
            public Integer call() {
                return new ListCommand().callWithParent(this);
            }

            @Command(name = "add", description = "Add a directory to scan for skills.")
            static final class AddCommand implements Callable<Integer> {
                @ParentCommand LoadPathCommand parent;
                @Parameters(index = "0", description = "Directory path to add.") String path;

                @Override
                public Integer call() {
                    PrintStream out = parent.parent.parent.context().out();
                    try {
                        Path p = Paths.get(path).toAbsolutePath();
                        WayangSkillStore.getInstance().addLoadPath(p);
                        out.println("  ✓ Load path added: " + p);
                        out.println("    Skills from this path are now available.");
                        return 0;
                    } catch (Exception e) { out.println("  ✗ " + e.getMessage()); return 1; }
                }
            }

            @Command(name = "remove", aliases = "rm",
                    description = "Remove a custom skill load path.")
            static final class RemoveCommand implements Callable<Integer> {
                @ParentCommand LoadPathCommand parent;
                @Parameters(index = "0") String path;

                @Override
                public Integer call() {
                    PrintStream out = parent.parent.parent.context().out();
                    Path p = Paths.get(path).toAbsolutePath();
                    WayangSkillStore.getInstance().removeLoadPath(p);
                    out.println("  ✓ Load path removed: " + p);
                    return 0;
                }
            }

            @Command(name = "list", aliases = "ls", description = "List all active skill load paths.")
            static final class ListCommand implements Callable<Integer> {
                @ParentCommand LoadPathCommand parent;

                @Override
                public Integer call() { return callWithParent(parent); }

                Integer callWithParent(LoadPathCommand p) {
                    PrintStream out = p.parent.parent.context().out();
                    WayangSkillStore store = WayangSkillStore.getInstance();
                    out.println();
                    out.println("  Skill load paths:");
                    out.println("  [builtin]  classpath:default-skills/");
                    out.println("  [user]     " + store.getBaseDir().toAbsolutePath());
                    List<Path> custom = store.getLoadPaths();
                    if (custom.isEmpty()) {
                        out.println("  (no custom load paths)");
                    } else {
                        custom.forEach(cp -> out.println("  [custom]   " + cp));
                    }
                    out.println();
                    return 0;
                }
            }
        }
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private static String esc(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
