package tech.kayys.gamelan.cli;

import jakarta.inject.Inject;
import picocli.CommandLine.*;
import tech.kayys.gamelan.agent.orchestration.*;
import tech.kayys.gamelan.config.GamelanConfig;
import tech.kayys.gamelan.session.ConversationSession;
import tech.kayys.gamelan.util.AnsiPrinter;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.*;

import static java.nio.file.StandardWatchEventKinds.*;

/**
 * File-watch mode — triggers agent tasks automatically on file changes.
 *
 * <h2>Migration to orchestrator</h2>
 * Previously called {@code agentLoop.process()} directly. Now routes through
 * {@link OrchestratorSelector}, so watch tasks benefit from auto-tier
 * selection and can be forced to a specific strategy with {@code --strategy}.
 *
 * <h2>Usage</h2>
 * <pre>
 * gamelan watch src/ --on-change "run tests for {file}"
 * gamelan watch . --glob "*.go" --on-change "check {file} for issues"
 * gamelan watch src/ --strategy direct --on-change "summarise changes in {file}"
 * gamelan watch src/ --debounce 3 --on-change "review {file}" --once
 * </pre>
 *
 * <h2>Template variables in --on-change</h2>
 * <ul>
 *   <li>{@code {file}}     — relative path of changed file</li>
 *   <li>{@code {filename}} — just the filename</li>
 *   <li>{@code {dir}}      — directory containing the file</li>
 * </ul>
 */
@Command(
    name = "watch",
    description = "Watch files and trigger agent tasks on change",
    mixinStandardHelpOptions = true
)
public class WatchCommand implements Runnable {

    @Inject OrchestratorSelector selector;
    @Inject GamelanConfig         config;

    @Parameters(index = "0", description = "Directory to watch", defaultValue = ".")
    Path watchDir;

    @Option(names = {"--on-change"}, required = true,
            description = "Task template. Variables: {file} {filename} {dir}")
    String taskTemplate;

    @Option(names = {"--glob"},      description = "File glob filter (e.g. *.java, *.py)")
    String glob;

    @Option(names = {"-m", "--model"})
    String model;

    @Option(names = {"--strategy", "-s"}, defaultValue = "auto",
            description = "Orchestration strategy: auto|direct|react|reflexion|multi")
    String strategy;

    @Option(names = {"--debounce"}, defaultValue = "2",
            description = "Seconds to wait after last change (default: 2)")
    int debounceSeconds;

    @Option(names = {"--once"},     description = "Stop after the first triggered run")
    boolean once;

    @Option(names = {"--no-color"})
    boolean noColor;

    private final ScheduledExecutorService debouncer =
            Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual().factory());
    private ScheduledFuture<?>             pending;
    private final Set<String>              changedFiles = ConcurrentHashMap.newKeySet();

    @Override
    public void run() {
        AnsiPrinter printer = new AnsiPrinter(!noColor && config.color());

        if (!Files.isDirectory(watchDir)) {
            printer.error("Not a directory: " + watchDir);
            System.exit(1);
        }

        PathMatcher matcher = (glob != null && !glob.isBlank())
                ? FileSystems.getDefault().getPathMatcher("glob:**/" + glob)
                : null;

        printer.sectionHeader("Watching: " + watchDir.toAbsolutePath());
        printer.info("Template: " + taskTemplate);
        if (matcher != null) printer.info("Filter: " + glob);
        printer.info("Strategy: " + strategy + "  |  Debounce: " + debounceSeconds + "s  |  Ctrl+C to stop");
        printer.println();

        try (WatchService watcher = FileSystems.getDefault().newWatchService()) {
            registerRecursive(watchDir, watcher);

            while (true) {
                WatchKey key;
                try { key = watcher.poll(1, TimeUnit.SECONDS); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }

                if (key == null) continue;

                Path parent = (Path) key.watchable();
                for (WatchEvent<?> event : key.pollEvents()) {
                    if (event.kind() == OVERFLOW) continue;
                    @SuppressWarnings("unchecked")
                    Path changed = parent.resolve(((WatchEvent<Path>) event).context());
                    if (!Files.isRegularFile(changed)) continue;
                    if (matcher != null && !matcher.matches(changed)) continue;

                    String rel = watchDir.toAbsolutePath()
                            .relativize(changed.toAbsolutePath()).toString();
                    changedFiles.add(rel);

                    if (pending != null && !pending.isDone()) pending.cancel(false);
                    pending = debouncer.schedule(() -> triggerTask(printer), debounceSeconds, TimeUnit.SECONDS);
                }

                if (!key.reset()) { printer.warn("Watch directory no longer accessible."); break; }
            }
        } catch (IOException e) {
            printer.error("Watch error: " + e.getMessage());
            System.exit(1);
        } finally {
            debouncer.shutdownNow();
        }
    }

    private void triggerTask(AnsiPrinter printer) {
        Set<String> batch = new HashSet<>(changedFiles);
        changedFiles.clear();

        String effectiveModel = model != null ? model : config.defaultModel();
        ConversationSession session = new ConversationSession(null,
                config.sessionPersist(), config.tokenBudget());

        for (String file : batch) {
            String task = expandTemplate(taskTemplate, file);
            printer.sectionHeader("Changed: " + file);
            printer.info("Task: " + task);

            try {
                AgentRequest req = AgentRequest.builder(task)
                        .model(effectiveModel)
                        .session(session)
                        .stream(true)
                        .maxSteps(10)
                        .build();

                AgentOrchestrator orch = selector.select(
                        strategy.equals("auto") ? null : strategy, task);
                long t0 = System.currentTimeMillis();
                OrchestratorResult result = orch.execute(req);
                printer.agentFooter(result, System.currentTimeMillis() - t0);

            } catch (Exception e) {
                printer.error("Task failed: " + e.getMessage());
            }
        }

        if (once) {
            printer.info("--once: stopping after first trigger.");
            System.exit(0);
        }
    }

    private String expandTemplate(String template, String file) {
        Path p = Path.of(file);
        return template
                .replace("{file}",     file)
                .replace("{filename}", p.getFileName().toString())
                .replace("{dir}",      p.getParent() != null ? p.getParent().toString() : ".");
    }

    private void registerRecursive(Path root, WatchService watcher) throws IOException {
        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                    throws IOException {
                String name = dir.getFileName().toString();
                if (name.equals(".git") || name.equals("target") || name.equals("node_modules")
                        || name.equals(".gradle") || name.equals("__pycache__")) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                dir.register(watcher, ENTRY_CREATE, ENTRY_MODIFY);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
