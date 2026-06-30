package tech.kayys.wayang.tui.term;

import java.io.IOException;

/**
 * Puts the controlling TTY into raw mode (no line buffering, no echo,
 * no signal generation for Ctrl-C/Ctrl-Z) and restores it on exit.
 *
 * This shells out to `stty` rather than using JNI/JNA, keeping the
 * project dependency-free. It works on Linux and macOS; on Windows
 * this would need a different backend (e.g. JNI or running under
 * WSL/Git-Bash), which is out of scope for v1.
 */
public final class TerminalMode {

    private String savedSttyConfig;
    private boolean raw = false;

    public boolean enterRaw() {
        try {
            // Save current settings so we can restore exactly, then flip to raw.
            ExecResult saved = execCapture("stty", "-g");
            if (saved.exitCode != 0) { raw = false; return false; }
            savedSttyConfig = saved.stdout;

            ExecResult result = exec("stty", "raw", "-echo");
            if (result.exitCode != 0) { raw = false; return false; }

            raw = true;
            return true;
        } catch (Exception e) {
            // Not an interactive TTY (e.g. piped input) -- fall back gracefully.
            raw = false;
            return false;
        }
    }

    public void restore() {
        if (!raw) return;
        try {
            if (savedSttyConfig != null && !savedSttyConfig.isBlank()) {
                exec("stty", savedSttyConfig.trim());
            } else {
                exec("stty", "sane");
            }
        } catch (Exception ignored) {
            // Best-effort restore.
        } finally {
            raw = false;
        }
    }

    public boolean isRaw() { return raw; }

    /** Queries terminal size as {columns, rows} via `stty size`, falling back to 80x24. */
    public static int[] size() {
        try {
            ExecResult result = execCapture("stty", "size"); // prints "rows cols"
            if (result.exitCode != 0) return new int[]{80, 24};
            String[] parts = result.stdout.trim().split("\\s+");
            int rows = Integer.parseInt(parts[0]);
            int cols = Integer.parseInt(parts[1]);
            return new int[]{cols, rows};
        } catch (Exception e) {
            return new int[]{80, 24};
        }
    }

    private record ExecResult(String stdout, int exitCode) {}

    private static ExecResult exec(String... cmd) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectInput(ProcessBuilder.Redirect.INHERIT);
        pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
        pb.redirectError(ProcessBuilder.Redirect.DISCARD);
        Process p = pb.start();
        int code = p.waitFor();
        return new ExecResult("", code);
    }

    private static ExecResult execCapture(String... cmd) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectInput(ProcessBuilder.Redirect.INHERIT);
        pb.redirectErrorStream(false);
        pb.redirectError(ProcessBuilder.Redirect.DISCARD);
        Process p = pb.start();
        String out = new String(p.getInputStream().readAllBytes());
        int code = p.waitFor();
        return new ExecResult(out, code);
    }
}
