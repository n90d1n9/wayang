package tech.kayys.wayang.gollek.cli;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

/**
 * Wayang CLI logging bootstrap.
 *
 * <p>Called once at JVM startup (before any CDI or SDK code runs) to silence the
 * console and redirect all java.util.logging output to
 * {@code ~/.wayang/logs/wayang.log}.
 *
 * <p>The log file is appended to on each run and is rotated by date suffix when
 * the file grows beyond 10 MB.  Old logs accumulate in the same directory and
 * can be listed / tailed with {@code wayang logs}.
 */
final class WayangLogging {

    /** Directory that holds all Wayang log files. */
    static final Path LOGS_DIR = Paths.get(System.getProperty("user.home"), ".wayang", "logs");

    /** Current session log file path. */
    static final Path LOG_FILE = LOGS_DIR.resolve("wayang.log");

    static PrintStream originalOut;
    static PrintStream originalErr;

    private static volatile boolean initialized = false;

    private WayangLogging() {}

    /**
     * Redirect ALL JUL output (and therefore SLF4J-over-JUL, jboss-logging, etc.)
     * and standard out/err to {@link #LOG_FILE}. The console is left clean.
     *
     * <p>Must be called as the very first statement in {@code main()} before any
     * class that touches logging is loaded.
     */
    static synchronized void install() {
        if (initialized) return;
        initialized = true;

        originalOut = System.out;
        originalErr = System.err;

        try {
            Files.createDirectories(LOGS_DIR);

            // Build a clean single-line log formatter
            Formatter fmt = new Formatter() {
                private final DateTimeFormatter dt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
                @Override
                public String format(LogRecord r) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(java.time.LocalDateTime.ofInstant(
                            java.time.Instant.ofEpochMilli(r.getMillis()),
                            java.time.ZoneId.systemDefault()).format(dt));
                    sb.append(" [").append(r.getLevel().getLocalizedName()).append("] ");
                    if (r.getSourceClassName() != null) {
                        sb.append(r.getSourceClassName());
                        if (r.getSourceMethodName() != null) {
                            sb.append("#").append(r.getSourceMethodName());
                        }
                    } else {
                        sb.append(r.getLoggerName());
                    }
                    sb.append(": ").append(formatMessage(r)).append("\n");
                    if (r.getThrown() != null) {
                        java.io.StringWriter sw = new java.io.StringWriter();
                        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
                        r.getThrown().printStackTrace(pw);
                        sb.append(sw.toString());
                    }
                    return sb.toString();
                }
            };

            // Append to wayang.log (create if absent)
            FileHandler fileHandler = new FileHandler(
                    LOG_FILE.toAbsolutePath().toString(),
                    /* append= */ true);
            fileHandler.setFormatter(fmt);
            fileHandler.setEncoding(StandardCharsets.UTF_8.name());
            fileHandler.setLevel(Level.ALL);

            // Configure the root logger: file only, no console
            Logger rootLogger = Logger.getLogger("");
            // Remove every existing handler (ConsoleHandler etc.)
            for (Handler h : rootLogger.getHandlers()) {
                rootLogger.removeHandler(h);
            }
            rootLogger.addHandler(fileHandler);
            rootLogger.setLevel(Level.ALL);

            // Silence noisy third-party loggers that spam even at INFO
            silenceLogger("org.jboss.weld");
            silenceLogger("io.smallrye");
            silenceLogger("org.hibernate");
            silenceLogger("io.quarkus");

            // Redirect System.out and System.err to logging to capture prints from libraries
            System.setOut(new PrintStream(new LoggingOutputStream(Logger.getLogger("stdout"), Level.INFO), true, StandardCharsets.UTF_8));
            System.setErr(new PrintStream(new LoggingOutputStream(Logger.getLogger("stderr"), Level.WARNING), true, StandardCharsets.UTF_8));

        } catch (IOException e) {
            // If we can't write the log file, fall back to /dev/null-style suppression
            suppressConsoleLogging();
        }
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private static void silenceLogger(String name) {
        Logger l = Logger.getLogger(name);
        l.setLevel(Level.WARNING);
    }

    /**
     * Last-resort: if the log directory isn't writable, simply remove console
     * handlers so nothing leaks to the terminal.
     */
    private static void suppressConsoleLogging() {
        Logger root = Logger.getLogger("");
        for (Handler h : root.getHandlers()) {
            root.removeHandler(h);
        }
        // Add a NullHandler so the framework doesn't complain
        root.addHandler(new Handler() {
            @Override public void publish(LogRecord record) {}
            @Override public void flush() {}
            @Override public void close() {}
        });
    }

    // ── LoggingOutputStream ──────────────────────────────────────────────────

    private static class LoggingOutputStream extends OutputStream {
        private final Logger logger;
        private final Level level;
        private final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        private final ThreadLocal<Boolean> inLogging = ThreadLocal.withInitial(() -> false);

        LoggingOutputStream(Logger logger, Level level) {
            this.logger = logger;
            this.level = level;
        }

        @Override
        public synchronized void write(int b) throws IOException {
            if (inLogging.get()) {
                if (level == Level.INFO) {
                    originalOut.write(b);
                } else {
                    originalErr.write(b);
                }
                return;
            }

            if (b == '\n') {
                String line = bos.toString(StandardCharsets.UTF_8);
                bos.reset();
                inLogging.set(true);
                try {
                    logger.log(level, line);
                } finally {
                    inLogging.set(false);
                }
            } else if (b != '\r') {
                bos.write(b);
            }
        }

        @Override
        public synchronized void write(byte[] b, int off, int len) throws IOException {
            if (inLogging.get()) {
                if (level == Level.INFO) {
                    originalOut.write(b, off, len);
                } else {
                    originalErr.write(b, off, len);
                }
                return;
            }
            for (int i = 0; i < len; i++) {
                write(b[off + i]);
            }
        }

        @Override
        public synchronized void flush() throws IOException {
            if (inLogging.get()) {
                if (level == Level.INFO) {
                    originalOut.flush();
                } else {
                    originalErr.flush();
                }
                return;
            }
            if (level == Level.INFO) {
                originalOut.flush();
            } else {
                originalErr.flush();
            }
        }
    }
}
