package tech.kayys.gamelan.cli;

import jakarta.inject.Inject;
import picocli.CommandLine.*;
import tech.kayys.gamelan.agent.GamelanWorkflowEngine;
import tech.kayys.gamelan.agent.GamelanWorkflowEngine.*;
import tech.kayys.gamelan.config.GamelanConfig;
import tech.kayys.gamelan.session.ConversationSession;
import tech.kayys.gamelan.util.AnsiPrinter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

/**
 * Multi-step workflow orchestration commands.
 *
 * <h2>Presets</h2>
 * <pre>
 * gamelan workflow review src/             # parallel: bugs+security+perf+style → synthesis
 * gamelan workflow refactor Svc.java       # sequential: analyse→plan→apply→verify
 * gamelan workflow document src/api/       # map-reduce: doc each file → merged doc
 * gamelan workflow test src/               # generate missing unit tests
 * gamelan workflow list                    # show all presets
 * </pre>
 *
 * <h2>Real bug fixes vs. previous version</h2>
 * <ul>
 *   <li>ReviewCmd printed only the "synthesis" step output and silently skipped
 *       all individual step results. If synthesis failed, users saw nothing.
 *       Fixed: print each step result labelled, then the synthesis.</li>
 *   <li>ReviewCmd's {@code --fix} flag printed a warn but did nothing. Now
 *       it adds a sequential fix phase after the review.</li>
 *   <li>DocumentCmd's file discovery called {@code Files.walk} but never closed
 *       the stream (resource leak). Fixed with try-with-resources.</li>
 *   <li>WorkflowResult.summary() used box-drawing chars that may not render on
 *       Windows terminals. Now uses plain ASCII with colour via AnsiPrinter.</li>
 * </ul>
 */
@Command(
    name = "workflow",
    description = "Run multi-step agentic workflows (parallel / sequential / map-reduce)",
    mixinStandardHelpOptions = true,
    subcommands = {
        WorkflowCommand.ReviewCmd.class,
        WorkflowCommand.RefactorCmd.class,
        WorkflowCommand.DocumentCmd.class,
        WorkflowCommand.TestCmd.class,
        WorkflowCommand.ListCmd.class
    }
)
public class WorkflowCommand implements Runnable {

    @Override public void run() { new picocli.CommandLine(this).usage(System.out); }

    // ── workflow review ────────────────────────────────────────────────────

    @Command(name = "review",
             description = "Parallel code review: bugs, security, performance, style → synthesis")
    static class ReviewCmd implements Runnable {

        @Inject GamelanWorkflowEngine engine;
        @Inject GamelanConfig         config;

        @Parameters(index = "0", description = "Path to review (file or directory)")
        String target;

        @Option(names = {"-m", "--model"})       String  model;
        @Option(names = {"--fix"},               description = "Auto-fix critical issues found in review")
        boolean applyFixes;
        @Option(names = {"--no-color"})          boolean noColor;

        @Override
        public void run() {
            AnsiPrinter  printer = new AnsiPrinter(!noColor && config.color());
            String effectiveModel = model != null ? model : config.defaultModel();
            printer.sectionHeader("Code Review: " + target);

            GamelanWorkflow.Builder wf = GamelanWorkflow.builder()
                .name("code-review")
                .parallel(
                    WorkflowStep.of("bugs",
                        "Read and analyze " + target + ". Find correctness bugs, logic errors, "
                        + "null pointer risks, and unhandled edge cases. List each with "
                        + "file:line and severity (critical/high/medium/low)."),
                    WorkflowStep.of("security",
                        "Review " + target + " for security vulnerabilities: SQL injection, "
                        + "XSS, path traversal, insecure deserialization, auth bypass, "
                        + "hardcoded secrets, excessive permissions. Rate each finding."),
                    WorkflowStep.of("performance",
                        "Analyze " + target + " for performance issues: O(n²) algorithms, "
                        + "N+1 queries, memory leaks, blocking I/O on hot paths, "
                        + "unnecessary object allocation. Note the impact of each."),
                    WorkflowStep.of("maintainability",
                        "Review " + target + " for code quality: overly complex methods, "
                        + "missing error handling, poor naming, duplicated logic, "
                        + "missing documentation on public APIs.")
                )
                .sequential(
                    WorkflowStep.of("synthesis",
                        "You have received parallel code review findings for " + target + ". "
                        + "Synthesize them into a structured report:\n"
                        + "1. Executive Summary (2-3 sentences)\n"
                        + "2. Critical Issues (must fix before shipping)\n"
                        + "3. High Priority (fix in next sprint)\n"
                        + "4. Recommendations (nice to have)\n"
                        + "Include file:line references for every issue.")
                );

            if (applyFixes) {
                wf.sequential(
                    WorkflowStep.required("fix",
                        "Based on the synthesis, fix the CRITICAL issues in " + target + ". "
                        + "Use apply_patch for targeted changes. Explain each fix.")
                );
            }

            ConversationSession session = new ConversationSession(null);
            WorkflowResult result = engine.execute(wf.build(), session, effectiveModel, step -> {
                String icon = step.success() ? "✓" : "✗";
                printer.info(icon + " " + step.stepName() + " (" + step.elapsed().getSeconds() + "s)");
            });

            printer.println();

            // Print each analysis step as a collapsible section
            for (StepResult step : result.stepResults()) {
                if ("synthesis".equals(step.stepName()) || "fix".equals(step.stepName())) continue;
                if (step.success() && !step.safeOutput().isBlank()) {
                    printer.sectionHeader("Analysis: " + step.stepName());
                    printer.println(step.safeOutput());
                }
            }

            // Print synthesis prominently
            result.stepResults().stream()
                .filter(s -> "synthesis".equals(s.stepName()) || "fix".equals(s.stepName()))
                .forEach(s -> {
                    printer.sectionHeader(s.stepName().substring(0, 1).toUpperCase()
                            + s.stepName().substring(1));
                    printer.println(s.safeOutput());
                });

            printer.println();
            printer.println(result.summary());

            System.exit(result.success() ? 0 : 1);
        }
    }

    // ── workflow refactor ──────────────────────────────────────────────────

    @Command(name = "refactor",
             description = "Sequential: analyse → plan → apply → verify")
    static class RefactorCmd implements Runnable {

        @Inject GamelanWorkflowEngine engine;
        @Inject GamelanConfig         config;

        @Parameters(index = "0", description = "File or directory to refactor")
        String target;

        @Option(names = {"--goal"}, defaultValue = "improve readability and maintainability")
        String goal;

        @Option(names = {"-m", "--model"})   String  model;
        @Option(names = {"--dry-run"},        description = "Plan only, don't apply changes")
        boolean dryRun;

        @Override
        public void run() {
            AnsiPrinter printer       = new AnsiPrinter(config.color());
            String effectiveModel     = model != null ? model : config.defaultModel();
            printer.sectionHeader("Refactoring: " + target);
            printer.info("Goal: " + goal + (dryRun ? "  [DRY RUN]" : ""));

            GamelanWorkflow.Builder wf = GamelanWorkflow.builder()
                .name("refactor")
                .sequential(
                    WorkflowStep.required("analyse",
                        "Read " + target + " completely. Identify specific areas that need "
                        + "refactoring to achieve: " + goal + ". List each issue with "
                        + "file:line and the proposed change."),
                    WorkflowStep.required("plan",
                        "Based on the analysis, create a concrete step-by-step refactoring plan. "
                        + "Order changes by: (1) safety risk, (2) impact. "
                        + "For each change state: what, why, and approximate effort.")
                );

            if (!dryRun) {
                wf.sequential(
                    WorkflowStep.required("apply",
                        "Execute the refactoring plan. Apply changes using apply_patch for "
                        + "targeted edits or write_file for larger rewrites. "
                        + "Apply one logical change at a time."),
                    WorkflowStep.of("verify",
                        "Verify the changes are correct: run any available tests with run_command, "
                        + "read the modified files to confirm they look right.")
                );
            }

            ConversationSession session = new ConversationSession(null);
            WorkflowResult result = engine.execute(wf.build(), session, effectiveModel, step -> {
                String icon = step.success() ? "✓" : "✗";
                printer.info(icon + " " + step.stepName() + " (" + step.elapsed().getSeconds() + "s)");
            });

            printer.println();
            result.stepResults().forEach(step -> {
                printer.sectionHeader(capitalize(step.stepName()));
                printer.println(step.safeOutput().isBlank() ? "(no output)" : step.safeOutput());
            });

            printer.println();
            printer.println(result.summary());
            System.exit(result.success() ? 0 : 1);
        }
    }

    // ── workflow document ──────────────────────────────────────────────────

    @Command(name = "document",
             description = "Map-reduce: generate docs for each file, then merge into one doc")
    static class DocumentCmd implements Runnable {

        @Inject GamelanWorkflowEngine engine;
        @Inject GamelanConfig         config;

        @Parameters(index = "0", description = "Directory to document")
        String target;

        @Option(names = {"--format"}, defaultValue = "markdown",
                description = "Output format: markdown | javadoc | jsdoc | openapi")
        String format;

        @Option(names = {"-o", "--output"}, description = "Output file (default: stdout)")
        Path outputFile;

        @Option(names = {"--max-files"}, defaultValue = "20")
        int maxFiles;

        @Option(names = {"-m", "--model"}) String model;

        @Override
        public void run() {
            AnsiPrinter printer   = new AnsiPrinter(config.color());
            String effectiveModel = model != null ? model : config.defaultModel();
            printer.sectionHeader("Documenting: " + target);

            List<Path> files = discoverFiles(target, maxFiles);
            if (files.isEmpty()) { printer.warn("No source files found in: " + target); return; }
            printer.info("Found " + files.size() + " files");

            WorkflowStep[] mapSteps = files.stream()
                .map(f -> WorkflowStep.of(f.getFileName().toString(),
                        "Generate " + format + " documentation for " + f + ". "
                        + "Cover: purpose, parameters, return values, exceptions, examples."))
                .toArray(WorkflowStep[]::new);

            GamelanWorkflow workflow = GamelanWorkflow.builder()
                .name("document")
                .mapReduce(mapSteps)
                .build();

            ConversationSession session = new ConversationSession(null);
            WorkflowResult result = engine.execute(workflow, session, effectiveModel, step ->
                    printer.info("✓ " + step.stepName()));

            String finalDoc = result.stepOutput("synthesis");
            if (finalDoc.isBlank()) {
                printer.error("Documentation synthesis failed");
                System.exit(1);
            }

            if (outputFile != null) {
                try {
                    Files.writeString(outputFile, finalDoc);
                    printer.success("Documentation written to: " + outputFile);
                } catch (IOException e) {
                    printer.error("Cannot write output: " + e.getMessage());
                    System.exit(1);
                }
            } else {
                System.out.println(finalDoc);
            }
        }

        private List<Path> discoverFiles(String target, int limit) {
            Path root = Path.of(target);
            try (Stream<Path> walk = Files.walk(root)) {    // properly closed
                return walk.filter(Files::isRegularFile)
                    .filter(p -> {
                        String n = p.getFileName().toString();
                        return n.endsWith(".java") || n.endsWith(".kt") || n.endsWith(".scala")
                            || n.endsWith(".py") || n.endsWith(".go") || n.endsWith(".ts")
                            || n.endsWith(".js") || n.endsWith(".rs") || n.endsWith(".cs");
                    })
                    .filter(p -> !p.toString().contains("/target/")
                            && !p.toString().contains("/node_modules/")
                            && !p.toString().contains("/__pycache__/"))
                    .limit(limit)
                    .toList();
            } catch (IOException e) {
                return List.of();
            }
        }
    }

    // ── workflow test ──────────────────────────────────────────────────────

    @Command(name = "test",
             description = "Generate missing unit tests for source files")
    static class TestCmd implements Runnable {

        @Inject GamelanWorkflowEngine engine;
        @Inject GamelanConfig         config;

        @Parameters(index = "0", description = "Source file or directory")
        String target;

        @Option(names = {"--framework"}, defaultValue = "junit5",
                description = "Test framework: junit5 | pytest | jest | go-test")
        String framework;

        @Option(names = {"--coverage"}, defaultValue = "80",
                description = "Target coverage % to aim for")
        int coverage;

        @Option(names = {"-m", "--model"}) String model;

        @Override
        public void run() {
            AnsiPrinter printer   = new AnsiPrinter(config.color());
            String effectiveModel = model != null ? model : config.defaultModel();
            printer.sectionHeader("Generating Tests: " + target);
            printer.info("Framework: " + framework + "  |  Target coverage: " + coverage + "%");

            GamelanWorkflow workflow = GamelanWorkflow.builder()
                .name("test-generation")
                .sequential(
                    WorkflowStep.required("analyse",
                        "Read " + target + ". Identify all public methods and functions "
                        + "that lack test coverage. List each with: class/module name, "
                        + "method signature, and key behaviors to test (happy path, "
                        + "edge cases, error cases)."),
                    WorkflowStep.required("generate",
                        "Generate comprehensive " + framework + " tests targeting " + coverage
                        + "% coverage. For each method identified:\n"
                        + "- Test the happy path\n"
                        + "- Test edge cases (null, empty, boundary values)\n"
                        + "- Test error conditions\n"
                        + "Write the complete test file(s) using write_file."),
                    WorkflowStep.of("run",
                        "Run the generated tests with run_command to verify they compile "
                        + "and pass. Fix any compilation errors or test failures.")
                )
                .build();

            ConversationSession session = new ConversationSession(null);
            WorkflowResult result = engine.execute(workflow, session, effectiveModel, step -> {
                printer.info((step.success() ? "✓" : "✗") + " " + step.stepName()
                        + " (" + step.elapsed().getSeconds() + "s)");
            });

            printer.println();
            result.stepResults().forEach(step -> {
                printer.sectionHeader(capitalize(step.stepName()));
                printer.println(step.safeOutput().isBlank() ? "(no output)" : step.safeOutput());
            });

            printer.println(result.summary());
            System.exit(result.success() ? 0 : 1);
        }
    }

    // ── workflow list ──────────────────────────────────────────────────────

    @Command(name = "list", aliases = {"ls"},
             description = "List all available workflow presets")
    static class ListCmd implements Runnable {
        @Override
        public void run() {
            AnsiPrinter p = new AnsiPrinter(true);
            p.sectionHeader("Workflow Presets");
            p.listItem("review <path>",         "Parallel bugs/security/perf/style → synthesis");
            p.listItem("refactor <path>",        "Sequential: analyse → plan → apply → verify");
            p.listItem("document <path>",        "Map-reduce: doc each file → merged document");
            p.listItem("test <path>",            "Sequential: analyse coverage → generate tests → run");
        }
    }

    private static String capitalize(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
