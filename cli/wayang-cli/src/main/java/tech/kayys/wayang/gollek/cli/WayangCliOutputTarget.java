package tech.kayys.wayang.gollek.cli;

import java.io.PrintStream;

/**
 * CLI output destination for commands that can either print text or persist it to a file.
 *
 * <p>The destination owns only path validation and user-facing messages; the
 * actual write operation is supplied by the SDK facade for the command domain.</p>
 */
record WayangCliOutputTarget(String path, boolean force) {

    /**
     * File writer callback supplied by an SDK facade.
     */
    @FunctionalInterface
    interface Writer {
        void write(String label, String path, String text, boolean force);
    }

    static WayangCliOutputTarget of(String path, boolean force) {
        if (path != null && path.isBlank()) {
            throw new IllegalArgumentException("--output requires a non-empty path.");
        }
        if (force && path == null) {
            throw new IllegalArgumentException("--force requires --output.");
        }
        return new WayangCliOutputTarget(path, force);
    }

    boolean hasPath() {
        return path != null;
    }

    void ensureSupported(boolean supported, String message) {
        if (hasPath() && !supported) {
            throw new IllegalArgumentException(message);
        }
    }

    void writeOrPrint(
            PrintStream out,
            Writer writer,
            String label,
            String text) {
        if (!hasPath()) {
            out.print(text);
            return;
        }
        writer.write(label, path, text, force);
        out.println("Wrote " + label + ": " + path);
    }
}
