package tech.kayys.gamelan.cli;

import jakarta.inject.Inject;
import picocli.CommandLine;
import picocli.CommandLine.*;
import tech.kayys.gamelan.config.GamelanConfig;
import tech.kayys.gamelan.evaluation.SkillBenchmark;
import tech.kayys.gamelan.governance.AuditLog;
import tech.kayys.gamelan.util.AnsiPrinter;

import java.nio.file.Path;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Evaluation and audit commands.
 *
 * <pre>
 * gamelan eval run                           # run built-in benchmark suite
 * gamelan eval run --file tasks.json         # custom benchmark file
 * gamelan eval shadow --baseline react --candidate reflexion
 * gamelan eval history                       # show past benchmark runs
 * gamelan eval audit                         # show recent audit log entries
 * gamelan eval audit --verify                # verify audit log integrity
 * </pre>
 */
@Command(
    name = "eval",
    description = "Evaluation, benchmarking, and audit log commands",
    mixinStandardHelpOptions = true,
    subcommands = {
        EvalCommand.RunCmd.class,
        EvalCommand.ShadowCmd.class,
        EvalCommand.HistoryCmd.class,
        EvalCommand.AuditCmd.class
    }
)
public class EvalCommand implements Runnable {
    @Override public void run() { new CommandLine(this).usage(System.out); }

    // ── eval run ───────────────────────────────────────────────────────────

    @Command(name = "run", description = "Run benchmark suite")
    static class RunCmd implements Runnable {
        @Inject SkillBenchmark benchmark;
        @Inject GamelanConfig  config;

        @Option(names = {"-m", "--model"})                        String model;
        @Option(names = {"-s", "--strategy"}, defaultValue = "react") String strategy;
        @Option(names = {"--file"}, description = "Custom benchmark JSON file") Path file;
        @Option(names = {"--no-color"})                           boolean noColor;

        @Override
        public void run() {
            AnsiPrinter printer = new AnsiPrinter(!noColor && config.color());
            String m = model != null ? model : config.defaultModel();
            printer.sectionHeader("Running Benchmark Suite  [model=" + m + " strategy=" + strategy + "]");

            long t0 = System.currentTimeMillis();
            SkillBenchmark.SuiteResult result = (file != null)
                    ? benchmark.runFromFile(m, strategy, file)
                    : benchmark.run(m, strategy);

            printer.println(result.summary());

            // Regression check vs previous run
            Optional<SkillBenchmark.SuiteResult> prev =
                    benchmark.loadLatest(file != null ? file.getFileName().toString() : "built-in");
            prev.ifPresent(baseline -> {
                if (result.hasRegression(baseline)) {
                    printer.warn("REGRESSION DETECTED! Score dropped from "
                            + String.format("%.0f%%", baseline.score() * 100)
                            + " → " + String.format("%.0f%%", result.score() * 100));
                } else {
                    printer.success("No regression vs previous run "
                            + String.format("(%.0f%% → %.0f%%)",
                            baseline.score() * 100, result.score() * 100));
                }
            });

            printer.info(String.format("Total: %dms", System.currentTimeMillis() - t0));
            System.exit(result.score() >= 0.75 ? 0 : 1);
        }
    }

    // ── eval shadow ────────────────────────────────────────────────────────

    @Command(name = "shadow", description = "Compare two strategies in shadow mode")
    static class ShadowCmd implements Runnable {
        @Inject SkillBenchmark benchmark;
        @Inject GamelanConfig  config;

        @Option(names = {"--baseline"},  required = true) String baseline;
        @Option(names = {"--candidate"}, required = true) String candidate;
        @Option(names = {"-m", "--model"})               String model;

        @Override
        public void run() {
            AnsiPrinter printer = new AnsiPrinter(config.color());
            String m = model != null ? model : config.defaultModel();
            printer.sectionHeader("Shadow Mode: " + baseline + " vs " + candidate);
            printer.info("Running both strategies in parallel...");

            SkillBenchmark.ShadowResult result = benchmark.shadow(m, baseline, candidate);
            printer.println("\nBaseline (" + baseline + "):\n" + result.baseline().summary());
            printer.println("Candidate (" + candidate + "):\n" + result.candidate().summary());
            if (result.improved()) {
                printer.success("Candidate IMPROVED vs baseline");
            } else {
                printer.warn("Candidate did NOT improve vs baseline");
            }
        }
    }

    // ── eval history ───────────────────────────────────────────────────────

    @Command(name = "history", description = "Show benchmark run history")
    static class HistoryCmd implements Runnable {
        @Inject GamelanConfig config;

        @Override
        public void run() {
            AnsiPrinter printer = new AnsiPrinter(config.color());
            printer.sectionHeader("Benchmark History");

            java.nio.file.Path evalDir = java.nio.file.Path.of(
                    System.getProperty("user.home"), ".gamelan", "eval");
            if (!java.nio.file.Files.isDirectory(evalDir)) {
                printer.warn("No benchmark history found. Run: gamelan eval run");
                return;
            }
            try (var stream = java.nio.file.Files.list(evalDir)) {
                var fmt = DateTimeFormatter.ofPattern("MMM d HH:mm").withZone(ZoneId.systemDefault());
                stream.filter(p -> p.toString().endsWith(".json"))
                      .sorted(java.util.Comparator.reverseOrder())
                      .limit(20)
                      .forEach(p -> printer.info(p.getFileName().toString()));
            } catch (java.io.IOException e) {
                printer.error("Cannot list history: " + e.getMessage());
            }
        }
    }

    // ── eval audit ─────────────────────────────────────────────────────────

    @Command(name = "audit", description = "View and verify the audit trail")
    static class AuditCmd implements Runnable {
        @Inject AuditLog      auditLog;
        @Inject GamelanConfig config;

        @Option(names = {"--verify"}, description = "Verify audit log integrity")
        boolean verify;

        @Option(names = {"-n"}, defaultValue = "20",
                description = "Number of recent entries to show")
        int limit;

        @Override
        public void run() {
            AnsiPrinter printer = new AnsiPrinter(config.color());

            if (verify) {
                printer.sectionHeader("Audit Log Verification");
                boolean ok = auditLog.verifyChain();
                if (ok) {
                    printer.success("Audit log integrity verified — hash chain intact");
                } else {
                    printer.error("INTEGRITY VIOLATION — audit log may have been tampered");
                    System.exit(1);
                }
                return;
            }

            printer.sectionHeader("Recent Audit Events (last " + limit + ")");
            var entries = auditLog.recent(limit);
            if (entries.isEmpty()) {
                printer.warn("No audit entries found.");
                return;
            }
            var fmt = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());
            entries.forEach(e -> {
                String meta = e.metadata() != null ? e.metadata().toString() : "";
                if (meta.length() > 60) meta = meta.substring(0, 60) + "…";
                printer.listItem(
                        fmt.format(e.timestamp()) + " [" + e.type() + "]",
                        e.subject() + " " + meta);
            });
        }
    }
}
