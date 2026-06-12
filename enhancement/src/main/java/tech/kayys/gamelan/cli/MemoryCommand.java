package tech.kayys.gamelan.cli;

import jakarta.inject.Inject;
import picocli.CommandLine.*;
import tech.kayys.gamelan.memory.AgentMemory;
import tech.kayys.gamelan.util.AnsiPrinter;

import java.util.List;

/**
 * CLI interface for managing the agent's persistent cross-session memory.
 *
 * <pre>
 * gamelan memory list                    # show all memories for current project
 * gamelan memory list --global           # show global memories
 * gamelan memory add "key" "value"       # store a fact
 * gamelan memory add --type PREFERENCE "indent" "2 spaces"
 * gamelan memory forget "key"            # delete an entry
 * gamelan memory clear                   # delete all project memories
 * gamelan memory search "flyway"         # find relevant entries
 * </pre>
 */
@Command(
    name = "memory",
    description = "Manage the agent's persistent cross-session memory",
    mixinStandardHelpOptions = true,
    subcommands = {
        MemoryCommand.ListCmd.class,
        MemoryCommand.AddCmd.class,
        MemoryCommand.ForgetCmd.class,
        MemoryCommand.ClearCmd.class,
        MemoryCommand.SearchCmd.class
    }
)
public class MemoryCommand implements Runnable {

    @Override public void run() { new CommandLine(this).usage(System.out); }

    // ── memory list ────────────────────────────────────────────────────────

    @Command(name = "list", aliases = {"ls"}, description = "List stored memories")
    static class ListCmd implements Runnable {

        @Inject AgentMemory memory;

        @Option(names = {"--global"}, description = "Show only global memories")
        boolean global;

        @Option(names = {"--type"}, description = "Filter by type: FACT|PREFERENCE|DECISION|COMMAND")
        String type;

        @Override
        public void run() {
            AnsiPrinter printer = new AnsiPrinter(true);
            List<AgentMemory.MemoryEntry> entries = global ? memory.all() : memory.relevant();

            if (type != null) {
                AgentMemory.MemoryType mt;
                try { mt = AgentMemory.MemoryType.valueOf(type.toUpperCase()); }
                catch (IllegalArgumentException e) {
                    printer.error("Unknown type: " + type + ". Valid: FACT, PREFERENCE, DECISION, COMMAND");
                    return;
                }
                entries = entries.stream().filter(e -> e.type() == mt).toList();
            }

            if (entries.isEmpty()) {
                printer.warn("No memories stored" + (global ? "" : " for this project") + ".");
                printer.info("The agent adds memories automatically, or use: gamelan memory add");
                return;
            }

            printer.sectionHeader("Memories (" + entries.size() + ")");
            AgentMemory.MemoryType lastType = null;
            for (AgentMemory.MemoryEntry e : entries) {
                if (e.type() != lastType) {
                    printer.println("\n@|bold " + e.type().label() + "|@");
                    lastType = e.type();
                }
                printer.listItem(e.key(), e.value());
            }
        }
    }

    // ── memory add ─────────────────────────────────────────────────────────

    @Command(name = "add", description = "Add a memory entry")
    static class AddCmd implements Runnable {

        @Inject AgentMemory memory;

        @Parameters(index = "0", description = "Memory key")   String key;
        @Parameters(index = "1", description = "Memory value") String value;

        @Option(names = {"--type"}, defaultValue = "FACT",
                description = "Type: FACT|PREFERENCE|DECISION|COMMAND")
        String type;

        @Option(names = {"--global"}, description = "Store as global (across all projects)")
        boolean global;

        @Override
        public void run() {
            AnsiPrinter printer = new AnsiPrinter(true);
            AgentMemory.MemoryType mt;
            try { mt = AgentMemory.MemoryType.valueOf(type.toUpperCase()); }
            catch (IllegalArgumentException e) {
                printer.error("Unknown type: " + type);
                return;
            }
            if (global) {
                memory.rememberGlobal(key, value, mt);
            } else {
                memory.remember(key, value, mt);
            }
            printer.success("Remembered [" + mt + "]: " + key + " = " + value);
        }
    }

    // ── memory forget ──────────────────────────────────────────────────────

    @Command(name = "forget", aliases = {"rm", "delete"},
             description = "Remove a memory entry")
    static class ForgetCmd implements Runnable {

        @Inject AgentMemory memory;

        @Parameters(index = "0", description = "Key to forget")
        String key;

        @Override
        public void run() {
            AnsiPrinter printer = new AnsiPrinter(true);
            memory.forget(key);
            printer.success("Forgot: " + key);
        }
    }

    // ── memory clear ───────────────────────────────────────────────────────

    @Command(name = "clear", description = "Clear all memories for the current project")
    static class ClearCmd implements Runnable {

        @Inject AgentMemory memory;

        @Option(names = {"--yes", "-y"}, description = "Confirm without prompt")
        boolean yes;

        @Override
        public void run() {
            AnsiPrinter printer = new AnsiPrinter(true);
            if (!yes) {
                printer.warn("This deletes ALL memories for this project.");
                printer.info("Re-run with --yes to confirm.");
                return;
            }
            memory.all().stream()
                  .filter(e -> !e.project().equals("_global"))
                  .map(AgentMemory.MemoryEntry::key)
                  .toList()
                  .forEach(memory::forget);
            printer.success("Project memories cleared.");
        }
    }

    // ── memory search ──────────────────────────────────────────────────────

    @Command(name = "search", description = "Search memories by keyword")
    static class SearchCmd implements Runnable {

        @Inject AgentMemory memory;

        @Parameters(index = "0", description = "Search term")
        String query;

        @Override
        public void run() {
            AnsiPrinter printer = new AnsiPrinter(true);
            String lower = query.toLowerCase();
            List<AgentMemory.MemoryEntry> hits = memory.all().stream()
                    .filter(e -> e.key().toLowerCase().contains(lower)
                            || e.value().toLowerCase().contains(lower))
                    .toList();

            if (hits.isEmpty()) { printer.warn("No memories match: " + query); return; }
            printer.sectionHeader("Results (" + hits.size() + ")");
            hits.forEach(e -> printer.listItem(e.key(), e.value()));
        }
    }
}
