package tech.kayys.gamelan.runtime.background;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.observability.metrics.AgentTelemetry;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.Consumer;

/**
 * BackgroundProcessManager — lifecycle management for long-running background tasks.
 *
 * <h2>From the OPENDEV paper (§2.4.3 — Shell Execution and Background Tasks)</h2>
 * Long-running processes like development servers require background execution with output
 * capture. The system auto-promotes server-like commands (detected via regex against 16
 * framework patterns) to background mode. Background commands are registered with the
 * BackgroundTaskManager, which assigns 7-character hex IDs.
 *
 * <h2>State machine (paper §2.4.3)</h2>
 * <pre>
 *   STARTING → RUNNING → COMPLETED (exit 0)
 *                      → FAILED    (non-zero exit)
 *                      → KILLED    (via signal)
 * </pre>
 *
 * <h2>Key design decisions from the paper</h2>
 * <ul>
 *   <li>Tasks transition via listener callbacks that notify the UI of each state change</li>
 *   <li>Output file at {@code ~/.gamelan/scratch/<session>/<id>.output}</li>
 *   <li>Daemon thread continuously streams PTY output to the file via polling</li>
 *   <li>Graceful termination: SIGTERM → 5s wait → SIGKILL (process group)</li>
 *   <li>Server detection: 16 regex patterns auto-promote to background mode</li>
 *   <li>Idle timeout: 60s of no output kills foreground commands</li>
 *   <li>Absolute timeout: 600s absolute cap on all commands</li>
 * </ul>
 */
@ApplicationScoped
public class BackgroundProcessManager {

    private static final Logger log = LoggerFactory.getLogger(BackgroundProcessManager.class);

    // Paper constants (§2.4.3, Table 9)
    private static final int    TASK_ID_LENGTH   = 7;     // 7-char hex = ~268M unique values
    private static final int    IDLE_TIMEOUT_SEC = 60;
    private static final int    ABS_TIMEOUT_SEC  = 600;
    private static final int    TERM_WAIT_SEC    = 5;
    private static final int    OUTPUT_TAIL_LINES = 100;  // paper: last 100 lines

    // Server detection patterns (paper Table 6 — 16 patterns)
    private static final List<String> SERVER_PATTERNS = List.of(
            "flask\\s+run", "python.*app\\.py", "python.*manage\\.py\\s+runserver",
            "django.*runserver", "uvicorn", "gunicorn",
            "python.*-m\\s+http\\.server", "npm\\s+(run\\s+)?(start|dev|serve)",
            "yarn\\s+(run\\s+)?(start|dev|serve)", "node.*server", "nodemon",
            "next\\s+(dev|start)", "rails\\s+server", "php.*artisan\\s+serve",
            "hugo\\s+server", "jekyll\\s+serve"
    );

    @Inject AgentTelemetry telemetry;

    private final ConcurrentHashMap<String, BackgroundTask> tasks = new ConcurrentHashMap<>();
    private volatile Path outputBaseDir;

    // ── Auto-detection ─────────────────────────────────────────────────────

    /**
     * Detects whether a command should be auto-promoted to background mode.
     * Matches against the 16 server patterns from the paper.
     *
     * @param command the shell command
     * @return true if the command matches a known server pattern
     */
    public boolean isServerLike(String command) {
        if (command == null) return false;
        String lower = command.toLowerCase();
        return SERVER_PATTERNS.stream().anyMatch(pattern ->
                lower.matches(".*" + pattern + ".*"));
    }

    // ── Task lifecycle ─────────────────────────────────────────────────────

    /**
     * Starts a command as a background task and returns its task ID.
     *
     * @param command       the shell command to execute
     * @param workingDir    the working directory
     * @param sessionId     the owning session (used for output file path)
     * @param onStateChange callback invoked on each state transition
     * @return the 7-character hex task ID
     */
    public String start(String command, Path workingDir, String sessionId,
                        Consumer<BackgroundTask> onStateChange) {
        String taskId = generateTaskId();
        Path outputFile = resolveOutputFile(sessionId, taskId);

        BackgroundTask task = new BackgroundTask(taskId, command, workingDir,
                outputFile, Instant.now(), onStateChange);
        tasks.put(taskId, task);

        // Launch in virtual thread
        Thread.ofVirtual().name("bg-task-" + taskId).start(() -> runTask(task));

        log.info("[bg-process] started task {} — {}", taskId, truncate(command, 60));
        telemetry.count("background.task.started");
        return taskId;
    }

    /**
     * Returns the last {@code lines} lines of output from a background task.
     * Paper: {@code get_process_output} retrieves last 100 lines.
     */
    public Optional<String> getOutput(String taskId, int lines) {
        BackgroundTask task = tasks.get(taskId);
        if (task == null) return Optional.empty();
        return task.readLastLines(lines);
    }

    /**
     * Terminates a running background task gracefully.
     * SIGTERM → 5s wait → SIGKILL (paper §2.4.3: kill_process).
     */
    public boolean kill(String taskId) {
        BackgroundTask task = tasks.get(taskId);
        if (task == null) return false;
        task.terminate();
        telemetry.count("background.task.killed");
        return true;
    }

    /**
     * Returns all tracked tasks with status.
     */
    public List<BackgroundTask> listAll() {
        return List.copyOf(tasks.values());
    }

    /**
     * Returns all running tasks.
     */
    public List<BackgroundTask> listRunning() {
        return tasks.values().stream()
                .filter(t -> t.state() == TaskState.RUNNING)
                .toList();
    }

    /**
     * Returns a specific task by ID.
     */
    public Optional<BackgroundTask> get(String taskId) {
        return Optional.ofNullable(tasks.get(taskId));
    }

    /** Sets the base directory for output files. */
    public void setOutputBaseDir(Path dir) { this.outputBaseDir = dir; }

    // ── Private ────────────────────────────────────────────────────────────

    private void runTask(BackgroundTask task) {
        task.setState(TaskState.STARTING);

        try {
            ProcessBuilder pb = new ProcessBuilder("bash", "-c", task.command())
                    .directory(task.workingDir().toFile())
                    .redirectErrorStream(true);

            Process process = pb.start();
            task.setProcess(process);
            task.setState(TaskState.RUNNING);
            if (task.onStateChange() != null) task.onStateChange().accept(task);

            // Stream output to file
            try (InputStream is = process.getInputStream();
                 BufferedWriter writer = Files.newBufferedWriter(task.outputFile(),
                         StandardCharsets.UTF_8, StandardOpenOption.CREATE,
                         StandardOpenOption.APPEND)) {
                byte[] buf = new byte[4096];
                long lastOutputMs = System.currentTimeMillis();
                long startMs = lastOutputMs;

                while (true) {
                    // Idle timeout check
                    long now = System.currentTimeMillis();
                    if (now - lastOutputMs > IDLE_TIMEOUT_SEC * 1000L) {
                        log.warn("[bg-process] task {} idle timeout", task.taskId());
                        break;
                    }
                    // Absolute timeout check
                    if (now - startMs > ABS_TIMEOUT_SEC * 1000L) {
                        log.warn("[bg-process] task {} absolute timeout", task.taskId());
                        break;
                    }
                    // Read output
                    if (is.available() > 0) {
                        int n = is.read(buf);
                        if (n < 0) break;
                        writer.write(new String(buf, 0, n, StandardCharsets.UTF_8));
                        writer.flush();
                        task.appendOutput(new String(buf, 0, n, StandardCharsets.UTF_8));
                        lastOutputMs = System.currentTimeMillis();
                    } else if (!process.isAlive()) {
                        break;
                    } else {
                        Thread.sleep(100); // 100ms polling (paper §2.4.3)
                    }
                }
            }

            int exitCode = process.waitFor();
            TaskState finalState = exitCode == 0 ? TaskState.COMPLETED : TaskState.FAILED;
            task.setState(finalState);
            task.setExitCode(exitCode);
            if (task.onStateChange() != null) task.onStateChange().accept(task);
            log.info("[bg-process] task {} {} (exit={})", task.taskId(), finalState, exitCode);
            telemetry.count("background.task." + finalState.name().toLowerCase());

        } catch (Exception e) {
            if (task.state() != TaskState.KILLED) {
                task.setState(TaskState.FAILED);
                if (task.onStateChange() != null) task.onStateChange().accept(task);
                log.warn("[bg-process] task {} error: {}", task.taskId(), e.getMessage());
            }
        }
    }

    private String generateTaskId() {
        return Long.toHexString(System.nanoTime()).substring(0, TASK_ID_LENGTH);
    }

    private Path resolveOutputFile(String sessionId, String taskId) {
        Path base = outputBaseDir != null ? outputBaseDir
                : Path.of(System.getProperty("user.home"), ".gamelan", "scratch", sessionId);
        try { Files.createDirectories(base); } catch (IOException ignored) {}
        return base.resolve(taskId + ".output");
    }

    private String truncate(String s, int max) {
        return s.length() > max ? s.substring(0, max) + "…" : s;
    }

    // ── Data types ─────────────────────────────────────────────────────────

    public enum TaskState { STARTING, RUNNING, COMPLETED, FAILED, KILLED }

    public static final class BackgroundTask {
        private final String              taskId;
        private final String              command;
        private final Path                workingDir;
        private final Path                outputFile;
        private final Instant             startedAt;
        private final Consumer<BackgroundTask> onStateChange;
        private final StringBuilder       buffer    = new StringBuilder();
        private volatile TaskState        state     = TaskState.STARTING;
        private volatile Process          process;
        private volatile int              exitCode  = -1;
        private volatile Instant          endedAt;

        BackgroundTask(String taskId, String command, Path workingDir, Path outputFile,
                       Instant startedAt, Consumer<BackgroundTask> onStateChange) {
            this.taskId       = taskId;
            this.command      = command;
            this.workingDir   = workingDir;
            this.outputFile   = outputFile;
            this.startedAt    = startedAt;
            this.onStateChange = onStateChange;
        }

        void setState(TaskState s)    { this.state = s; if (s != TaskState.STARTING && s != TaskState.RUNNING) endedAt = Instant.now(); }
        void setProcess(Process p)    { this.process = p; }
        void setExitCode(int code)    { this.exitCode = code; }
        void appendOutput(String out) { synchronized(buffer) { buffer.append(out); if (buffer.length() > 500_000) buffer.delete(0, 100_000); } }

        public String    taskId()    { return taskId; }
        public String    command()   { return command; }
        public Path      workingDir(){ return workingDir; }
        public Path      outputFile(){ return outputFile; }
        public Instant   startedAt() { return startedAt; }
        public TaskState state()     { return state; }
        public int       exitCode()  { return exitCode; }
        public boolean   isRunning() { return state == TaskState.RUNNING; }
        Consumer<BackgroundTask> onStateChange() { return onStateChange; }

        public Duration runtime() {
            Instant end = endedAt != null ? endedAt : Instant.now();
            return Duration.between(startedAt, end);
        }

        public Optional<String> readLastLines(int n) {
            try {
                if (!Files.exists(outputFile)) {
                    // Fall back to in-memory buffer
                    synchronized(buffer) {
                        String all = buffer.toString();
                        String[] lines = all.split("\n", -1);
                        int from = Math.max(0, lines.length - n);
                        return Optional.of(String.join("\n",
                                Arrays.copyOfRange(lines, from, lines.length)));
                    }
                }
                List<String> all = Files.readAllLines(outputFile);
                int from = Math.max(0, all.size() - n);
                return Optional.of(String.join("\n", all.subList(from, all.size())));
            } catch (IOException e) {
                return Optional.empty();
            }
        }

        public void terminate() {
            setState(TaskState.KILLED);
            if (process == null) return;
            process.destroy(); // SIGTERM
            Thread.ofVirtual().start(() -> {
                try {
                    if (!process.waitFor(TERM_WAIT_SEC, java.util.concurrent.TimeUnit.SECONDS)) {
                        process.destroyForcibly(); // SIGKILL
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        public String summary() {
            return String.format("[%s] %s — %s (%ds)",
                    taskId, state, truncate(command, 40), runtime().toSeconds());
        }

        private String truncate(String s, int max) {
            return s.length() > max ? s.substring(0, max) + "…" : s;
        }
    }
}
