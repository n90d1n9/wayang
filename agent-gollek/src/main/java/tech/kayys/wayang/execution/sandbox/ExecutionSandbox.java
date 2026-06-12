package tech.kayys.gamelan.execution.sandbox;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.agent.ToolCall;
import tech.kayys.gamelan.tool.ToolResult;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Execution Sandbox — copy-on-write filesystem isolation and dry-run environment.
 */
@ApplicationScoped
public class ExecutionSandbox {

    private static final Logger log = LoggerFactory.getLogger(ExecutionSandbox.class);

    private static final Set<String> READ_ONLY_PREFIXES = Set.of(
        "ls","cat","head","tail","find","grep","wc","echo","pwd",
        "git log","git diff","git status","git show","git branch",
        "mvn test","mvn verify","mvn compile","npm test","npm run test",
        "python -m pytest","python3 -m pytest","which","env","date","whoami");

    // Package-private so tests can set directly without CDI
    @Inject SandboxInterceptor interceptor;

    private volatile SandboxSession activeSession = null;

    public SandboxSession enter(String label) {
        if (activeSession != null) discardInternal(false);
        Path overlay = createOverlay(label);
        activeSession = new SandboxSession(UUID.randomUUID().toString(), label,
                overlay, Instant.now(), new ConcurrentHashMap<>(), new ArrayList<>());
        if (interceptor != null) interceptor.onEnter(label);
        log.info("[sandbox] entered: label='{}' overlay={}", label, overlay);
        return activeSession;
    }

    public List<SandboxDiff> diff() {
        return activeSession == null ? List.of() : List.copyOf(activeSession.changes());
    }

    public List<SandboxDiff> commit() {
        if (activeSession == null) return List.of();
        List<SandboxDiff> applied = new ArrayList<>();
        for (SandboxDiff c : activeSession.changes()) {
            try {
                switch (c.type()) {
                    case CREATE, MODIFY -> {
                        Path src  = c.overlayDir().resolve(sanitize(c.relativePath()));
                        Path dest = Path.of(c.relativePath());
                        if (Files.exists(src)) {
                            if (dest.getParent() != null) Files.createDirectories(dest.getParent());
                            Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
                            applied.add(c);
                        }
                    }
                    case DELETE -> {
                        if (Files.deleteIfExists(Path.of(c.relativePath()))) applied.add(c);
                    }
                    default -> {}
                }
            } catch (IOException e) {
                log.error("[sandbox] commit error for '{}': {}", c.relativePath(), e.getMessage());
            }
        }
        if (interceptor != null) interceptor.onCommit(applied.size());
        discardInternal(false);
        return applied;
    }

    public void discard() { discardInternal(true); }

    public boolean isActive() { return activeSession != null; }

    public Optional<SandboxSession> activeSession() { return Optional.ofNullable(activeSession); }

    public Optional<ToolResult> intercept(ToolCall call) {
        if (!isActive()) return Optional.empty();
        return switch (call.name()) {
            case "write_file"  -> Optional.of(interceptWrite(call));
            case "apply_patch" -> Optional.of(interceptPatch(call));
            case "run_command" -> interceptCommand(call);
            default            -> Optional.empty();
        };
    }

    public Optional<String> interceptRead(String path) {
        if (!isActive()) return Optional.empty();
        Path p = activeSession.overlay().resolve(sanitize(path));
        if (!Files.exists(p)) return Optional.empty();
        try { return Optional.of(Files.readString(p)); }
        catch (IOException e) { return Optional.empty(); }
    }

    private ToolResult interceptWrite(ToolCall call) {
        String path = call.param("path",""), content = call.param("content","");
        Path op = activeSession.overlay().resolve(sanitize(path));
        try {
            Files.createDirectories(op.getParent());
            Files.writeString(op, content);
            DiffType t = Files.exists(Path.of(path)) ? DiffType.MODIFY : DiffType.CREATE;
            record(path, t, content);
            return ToolResult.success("write_file",
                    "[SANDBOX] " + t + ": " + path + " — staged, not committed");
        } catch (IOException e) {
            return ToolResult.failure("write_file","[SANDBOX] overlay error: " + e.getMessage());
        }
    }

    private ToolResult interceptPatch(ToolCall call) {
        String patch = call.param("patch","");
        List<String> files = patch.lines().filter(l -> l.startsWith("+++ b/"))
                .map(l -> l.substring(6).strip()).toList();
        long adds = patch.lines().filter(l -> l.startsWith("+") && !l.startsWith("+++")).count();
        long dels = patch.lines().filter(l -> l.startsWith("-") && !l.startsWith("---")).count();
        files.forEach(f -> record(f, DiffType.MODIFY, "(patch +"+adds+" -"+dels+")"));
        return ToolResult.success("apply_patch",
                "[SANDBOX] Patch staged: "+files.size()+" file(s) (+"+adds+" -"+dels+") — not committed");
    }

    private Optional<ToolResult> interceptCommand(ToolCall call) {
        String cmd = call.param("command","").strip();
        if (isReadOnly(cmd)) return Optional.empty();
        record("[cmd]:" + cmd, DiffType.COMMAND_SIDE_EFFECT, cmd);
        return Optional.of(ToolResult.success("run_command",
                "[SANDBOX] Staged but NOT executed: " + cmd));
    }

    private boolean isReadOnly(String c) {
        String lower = c.toLowerCase().strip();
        return READ_ONLY_PREFIXES.stream().anyMatch(lower::startsWith);
    }

    private void record(String path, DiffType type, String content) {
        SandboxDiff d = new SandboxDiff(path, type, content, activeSession.overlay(), Instant.now());
        activeSession.changes().add(d);
        activeSession.overlayIndex().put(path, d);
    }

    private String sanitize(String p) { return p.startsWith("/") ? p.substring(1) : p; }

    private Path createOverlay(String label) {
        try { return Files.createTempDirectory("gamelan-sandbox-" + label.replaceAll("[^a-zA-Z0-9_-]","-") + "-"); }
        catch (IOException e) { throw new IllegalStateException("Cannot create overlay", e); }
    }

    private void discardInternal(boolean notify) {
        if (activeSession == null) return;
        Path overlay = activeSession.overlay();
        activeSession = null;
        if (notify && interceptor != null) interceptor.onDiscard();
        Thread.ofVirtual().start(() -> deleteTree(overlay));
    }

    private void deleteTree(Path dir) {
        try { Files.walkFileTree(dir, new SimpleFileVisitor<>() {
            @Override public FileVisitResult visitFile(Path f, BasicFileAttributes a) throws IOException { Files.deleteIfExists(f); return FileVisitResult.CONTINUE; }
            @Override public FileVisitResult postVisitDirectory(Path d, IOException e) throws IOException { Files.deleteIfExists(d); return FileVisitResult.CONTINUE; }
        }); } catch (IOException ignored) {}
    }

    public record SandboxSession(String id, String label, Path overlay, Instant startedAt,
            Map<String,SandboxDiff> overlayIndex, List<SandboxDiff> changes) {
        public String summary() {
            long c = changes.stream().filter(x -> x.type()==DiffType.CREATE).count();
            long m = changes.stream().filter(x -> x.type()==DiffType.MODIFY).count();
            long d = changes.stream().filter(x -> x.type()==DiffType.DELETE).count();
            return String.format("Sandbox '%s': +%d ~%d -%d", label, c, m, d);
        }
    }

    public record SandboxDiff(String relativePath, DiffType type, String content,
            Path overlayDir, Instant recordedAt) {
        public String display() { return switch(type) { case CREATE -> "+ "; case MODIFY -> "~ "; case DELETE -> "- "; case COMMAND_SIDE_EFFECT -> "! "; } + relativePath; }
    }

    public enum DiffType { CREATE, MODIFY, DELETE, COMMAND_SIDE_EFFECT }
}
