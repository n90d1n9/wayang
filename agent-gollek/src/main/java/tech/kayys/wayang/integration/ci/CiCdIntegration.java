package tech.kayys.gamelan.integration.ci;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.agent.orchestration.*;
import tech.kayys.gamelan.config.GamelanConfig;
import tech.kayys.gamelan.observability.metrics.AgentTelemetry;
import tech.kayys.gamelan.session.ConversationSession;

import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;
import java.util.stream.*;

/**
 * CiCdIntegration — CI/CD pipeline awareness and automated failure remediation.
 *
 * <h2>Core capabilities</h2>
 * <ol>
 *   <li><b>Pipeline monitoring</b>: poll CI status and get structured build results</li>
 *   <li><b>Test failure analysis</b>: parse JUnit XML, pytest, Go test output → structured findings</li>
 *   <li><b>Auto-fix workflow</b>: agent analyzes failures and proposes targeted fixes</li>
 *   <li><b>Flaky test detection</b>: track failure rates and flag non-deterministic tests</li>
 *   <li><b>Coverage gap analysis</b>: parse coverage reports and suggest missing tests</li>
 * </ol>
 *
 * <h2>Supported CI systems</h2>
 * <ul>
 *   <li>Local: parse output from {@code mvn test}, {@code pytest}, {@code go test}, {@code cargo test}</li>
 *   <li>GitHub Actions: via REST API (requires GITHUB_TOKEN env var)</li>
 *   <li>Generic: any CI that produces JUnit XML output</li>
 * </ul>
 *
 * <h2>Auto-fix pipeline</h2>
 * <pre>
 * CI fails
 *   → Parse test output → extract FailedTest records
 *   → For each failure:
 *       → Read the failing test file + the class under test
 *       → Ask LLM: "Why is this test failing? What's the minimal fix?"
 *       → Generate a targeted patch
 *       → Apply patch
 *       → Re-run that specific test to verify
 *   → Commit if all fixed
 * </pre>
 */
@ApplicationScoped
public class CiCdIntegration {

    private static final Logger log = LoggerFactory.getLogger(CiCdIntegration.class);
    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    @Inject SingleAgentOrchestrator orchestrator;
    @Inject GamelanConfig            config;
    @Inject AgentTelemetry           telemetry;

    // Flaky test tracking: test name → failure history
    private final Map<String, Deque<Boolean>> flakyHistory = new ConcurrentHashMap<>();

    // ── Test result parsing ────────────────────────────────────────────────

    /**
     * Parses test output from any supported format and returns structured results.
     *
     * @param output   raw test output text (from mvn test, pytest, etc.)
     * @param format   the test framework format
     * @return structured test run result
     */
    public TestRunResult parseTestOutput(String output, TestFormat format) {
        log.info("[ci] parsing test output: format={} length={}", format, output.length());
        telemetry.count("ci.test.parse." + format.name().toLowerCase());

        return switch (format) {
            case MAVEN    -> parseMavenOutput(output);
            case PYTEST   -> parsePytestOutput(output);
            case GO_TEST  -> parseGoTestOutput(output);
            case CARGO    -> parseCargoOutput(output);
            case JUNIT_XML -> parseJUnitXml(output);
            case GENERIC  -> parseGenericOutput(output);
        };
    }

    /**
     * Runs tests and parses the output in one step.
     *
     * @param command  the test command to run (e.g., "mvn test", "pytest tests/")
     * @param workDir  directory to run from
     * @param format   expected output format
     */
    public TestRunResult runAndParse(String command, Path workDir, TestFormat format) {
        log.info("[ci] running: {} in {}", command, workDir);
        String output = runCommand(command, workDir, 300);
        return parseTestOutput(output, format);
    }

    /**
     * Analyzes test failures and generates targeted fix proposals.
     *
     * @param result   the failed test run
     * @param maxFixes max number of failures to attempt to fix
     * @return fix proposals for each failed test
     */
    public FixProposalSet analyzeAndPropose(TestRunResult result, int maxFixes) {
        if (result.passed()) return FixProposalSet.empty();

        log.info("[ci] analyzing {} failures, proposing fixes (max={})",
                result.failedTests().size(), maxFixes);
        telemetry.count("ci.fix.analyze.total");

        List<FixProposal> proposals = result.failedTests().stream()
                .limit(maxFixes)
                .map(this::proposeFixForFailure)
                .toList();

        return new FixProposalSet(proposals, result, Instant.now());
    }

    /**
     * Applies fix proposals and re-runs the affected tests to verify.
     *
     * @param proposalSet  the fix proposals to apply
     * @param testCommand  how to run a specific test (e.g., "mvn test -Dtest={test}")
     * @param dryRun       if true, show proposals without applying
     */
    public FixApplicationResult applyFixes(FixProposalSet proposalSet,
                                            String testCommand, boolean dryRun) {
        List<AppliedFix> applied = new ArrayList<>();

        for (FixProposal proposal : proposalSet.proposals()) {
            if (!proposal.hasPatch()) continue;

            if (dryRun) {
                applied.add(AppliedFix.dryRun(proposal));
                continue;
            }

            // Apply the patch
            boolean patchOk = applyPatch(proposal.patch());
            if (!patchOk) {
                applied.add(AppliedFix.patchFailed(proposal));
                continue;
            }

            // Re-run the specific test
            String testCmd = testCommand.replace("{test}", proposal.testName());
            String output = runCommand(testCmd, Path.of("."), 120);
            TestRunResult rerun = parseGenericOutput(output);

            boolean verified = rerun.passed() || rerun.failedTests().stream()
                    .noneMatch(f -> f.testName().equals(proposal.testName()));

            applied.add(new AppliedFix(proposal, patchOk, verified, output, Instant.now()));
            telemetry.count(verified ? "ci.fix.verified" : "ci.fix.unverified");
        }

        long fixedCount = applied.stream().filter(a -> a.verified() && !a.dryRun()).count();
        log.info("[ci] fix application: {}/{} verified", fixedCount, applied.size());
        return new FixApplicationResult(applied, fixedCount, dryRun);
    }

    /**
     * Tracks a test result for flaky test detection.
     * Call this after every test run.
     */
    public void trackResult(String testName, boolean passed) {
        Deque<Boolean> history = flakyHistory.computeIfAbsent(testName,
                k -> new ArrayDeque<>());
        history.addLast(passed);
        if (history.size() > 20) history.pollFirst(); // keep last 20 runs
    }

    /**
     * Returns tests identified as flaky (failure rate 20–80% over last 20 runs).
     */
    public List<FlakyTest> detectFlakyTests() {
        return flakyHistory.entrySet().stream()
                .filter(e -> e.getValue().size() >= 5)
                .map(e -> {
                    List<Boolean> runs = new ArrayList<>(e.getValue());
                    long failures = runs.stream().filter(b -> !b).count();
                    double failRate = (double) failures / runs.size();
                    return new FlakyTest(e.getKey(), failRate, runs.size(), failRate >= 0.2 && failRate <= 0.8);
                })
                .filter(FlakyTest::isFlaky)
                .sorted(Comparator.comparingDouble(FlakyTest::failureRate).reversed())
                .toList();
    }

    /**
     * Parses a JaCoCo/coverage.xml report and finds uncovered lines.
     */
    public CoverageReport parseCoverage(Path coverageFile) {
        if (!Files.exists(coverageFile)) {
            return new CoverageReport(List.of(), 0.0, 0.0, coverageFile.toString());
        }
        try {
            String content = Files.readString(coverageFile);
            return parseCoverageXml(content, coverageFile.toString());
        } catch (IOException e) {
            log.warn("[ci] coverage parse failed: {}", e.getMessage());
            return new CoverageReport(List.of(), 0.0, 0.0, coverageFile.toString());
        }
    }

    /**
     * Polls a GitHub Actions run and returns its status.
     * Requires GITHUB_TOKEN environment variable.
     */
    public Optional<CiRunStatus> getGitHubRunStatus(String owner, String repo, long runId) {
        String token = System.getenv("GITHUB_TOKEN");
        if (token == null || token.isBlank()) {
            log.warn("[ci] GITHUB_TOKEN not set — cannot poll GitHub Actions");
            return Optional.empty();
        }
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.github.com/repos/" + owner + "/" + repo +
                            "/actions/runs/" + runId))
                    .header("Authorization", "Bearer " + token)
                    .header("Accept", "application/vnd.github.v3+json")
                    .GET().build();
            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return Optional.of(parseGitHubRunStatus(response.body(), owner, repo, runId));
            }
        } catch (Exception e) {
            log.warn("[ci] GitHub API call failed: {}", e.getMessage());
        }
        return Optional.empty();
    }

    // ── Parsers ────────────────────────────────────────────────────────────

    private TestRunResult parseMavenOutput(String output) {
        List<FailedTest> failures = new ArrayList<>();
        int total = 0, failed = 0, errors = 0, skipped = 0;

        // Parse "Tests run: X, Failures: Y, Errors: Z, Skipped: W"
        Pattern summaryPat = Pattern.compile(
                "Tests run: (\\d+), Failures: (\\d+), Errors: (\\d+), Skipped: (\\d+)");
        Matcher m = summaryPat.matcher(output);
        while (m.find()) {
            total   += Integer.parseInt(m.group(1));
            failed  += Integer.parseInt(m.group(2));
            errors  += Integer.parseInt(m.group(3));
            skipped += Integer.parseInt(m.group(4));
        }

        // Extract individual failures
        Pattern failPat = Pattern.compile(
                "FAILED ([\\w.]+)#?(\\w+)\\s*[-–]\\s*(.+?)(?=\\n(?:FAILED|Tests run|BUILD|$))",
                Pattern.DOTALL);
        Matcher fm = failPat.matcher(output);
        while (fm.find()) {
            String className  = fm.group(1);
            String methodName = fm.group(2);
            String message    = fm.group(3).strip();
            String trace      = extractStackTrace(output, className, methodName);
            failures.add(new FailedTest(methodName, className,
                    message.length() > 200 ? message.substring(0, 200) : message, trace));
        }

        boolean buildFailed = output.contains("BUILD FAILURE");
        Duration duration = parseMavenDuration(output);
        return new TestRunResult(total, failed + errors, skipped, failures,
                buildFailed, output, TestFormat.MAVEN, duration);
    }

    private TestRunResult parsePytestOutput(String output) {
        List<FailedTest> failures = new ArrayList<>();
        int passed = 0, failed = 0, errors = 0;

        Pattern summaryPat = Pattern.compile("(\\d+) passed|" +
                "(\\d+) failed|" + "(\\d+) error");
        Matcher m = summaryPat.matcher(output);
        while (m.find()) {
            if (m.group(1) != null) passed  = Integer.parseInt(m.group(1));
            if (m.group(2) != null) failed  = Integer.parseInt(m.group(2));
            if (m.group(3) != null) errors  = Integer.parseInt(m.group(3));
        }

        Pattern failPat = Pattern.compile("FAILED ([\\w/]+\\.py)::([\\w:]+)\\s*-\\s*(.+)");
        Matcher fm = failPat.matcher(output);
        while (fm.find()) {
            failures.add(new FailedTest(fm.group(2), fm.group(1),
                    fm.group(3).strip(), ""));
        }

        return new TestRunResult(passed + failed + errors, failed + errors, 0,
                failures, failed + errors > 0, output, TestFormat.PYTEST, Duration.ZERO);
    }

    private TestRunResult parseGoTestOutput(String output) {
        List<FailedTest> failures = new ArrayList<>();
        int pass = 0, fail = 0;

        for (String line : output.split("\n")) {
            if (line.startsWith("--- FAIL:")) {
                Pattern p = Pattern.compile("--- FAIL: (\\w+) \\(([0-9.]+)s\\)");
                Matcher m = p.matcher(line);
                if (m.find()) {
                    failures.add(new FailedTest(m.group(1), "main",
                            "Test failed in " + m.group(2) + "s", ""));
                    fail++;
                }
            } else if (line.startsWith("--- PASS:")) pass++;
        }
        return new TestRunResult(pass + fail, fail, 0, failures,
                fail > 0, output, TestFormat.GO_TEST, Duration.ZERO);
    }

    private TestRunResult parseCargoOutput(String output) {
        List<FailedTest> failures = new ArrayList<>();
        Pattern failPat = Pattern.compile("test ([\\w:]+) \\.\\.\\. FAILED");
        Matcher m = failPat.matcher(output);
        while (m.find()) {
            failures.add(new FailedTest(m.group(1), "rust", "test FAILED", ""));
        }
        Pattern resultPat = Pattern.compile("test result: (?:FAILED|ok). (\\d+) passed; (\\d+) failed");
        Matcher rm = resultPat.matcher(output);
        int passed = 0, failed = failures.size();
        if (rm.find()) { passed = Integer.parseInt(rm.group(1)); failed = Integer.parseInt(rm.group(2)); }
        return new TestRunResult(passed + failed, failed, 0, failures,
                failed > 0, output, TestFormat.CARGO, Duration.ZERO);
    }

    private TestRunResult parseJUnitXml(String xml) {
        List<FailedTest> failures = new ArrayList<>();
        Pattern testPat = Pattern.compile(
                "<testcase[^>]*name=\"([^\"]+)\"[^>]*classname=\"([^\"]+)\"[^>]*>" +
                "(?:<failure[^>]*message=\"([^\"]*)\">([^<]*)</failure>)?</testcase>",
                Pattern.DOTALL);
        Matcher m = testPat.matcher(xml);
        int total = 0, failed = 0;
        while (m.find()) {
            total++;
            if (m.group(3) != null) {
                failed++;
                failures.add(new FailedTest(m.group(1), m.group(2),
                        m.group(3), m.group(4)));
            }
        }
        return new TestRunResult(total, failed, 0, failures,
                failed > 0, xml, TestFormat.JUNIT_XML, Duration.ZERO);
    }

    private TestRunResult parseGenericOutput(String output) {
        boolean hasFail = output.contains("FAIL") || output.contains("ERROR") ||
                output.contains("failed") || output.contains("BUILD FAILURE");
        return new TestRunResult(0, haFail(output) ? 1 : 0, 0, List.of(),
                haFail(output), output, TestFormat.GENERIC, Duration.ZERO);
    }

    private boolean haFail(String s) {
        return s.contains("FAIL") || s.contains("failed") || s.contains("BUILD FAILURE");
    }

    private FixProposal proposeFixForFailure(FailedTest failure) {
        String prompt = """
                A test is failing. Analyze and propose the minimal fix.
                
                Test: %s.%s
                Error: %s
                Stack trace:
                %s
                
                1. Read the test file and the class under test.
                2. Identify the root cause.
                3. Propose a minimal unified diff patch.
                
                Output:
                ROOT_CAUSE: [one sentence]
                FILE_TO_FIX: [path]
                PATCH:
                ```diff
                [unified diff]
                ```
                """.formatted(failure.className(), failure.testName(),
                failure.message(), failure.stackTrace());

        try {
            OrchestratorResult result = orchestrator.execute(
                    AgentRequest.builder(prompt)
                            .model(config.defaultModel())
                            .session(new ConversationSession(null, false, config.tokenBudget()))
                            .stream(false).maxSteps(5).build());

            return parseFixProposal(failure, result.answer());
        } catch (Exception e) {
            return FixProposal.unavailable(failure, e.getMessage());
        }
    }

    private FixProposal parseFixProposal(FailedTest failure, String raw) {
        String rootCause = extractLine(raw, "ROOT_CAUSE:");
        String filePath  = extractLine(raw, "FILE_TO_FIX:");
        String patch     = extractCodeBlock(raw, "diff");

        return new FixProposal(failure.testName(), failure.className(),
                rootCause, filePath, patch, raw, Instant.now());
    }

    private boolean applyPatch(String patch) {
        if (patch == null || patch.isBlank()) return false;
        try {
            Path tmp = Files.createTempFile("fix-", ".patch");
            Files.writeString(tmp, patch);
            String result = runCommand("patch -p1 < " + tmp, Path.of("."), 30);
            Files.deleteIfExists(tmp);
            return !result.toLowerCase().contains("failed") && !result.toLowerCase().contains("reject");
        } catch (IOException e) { return false; }
    }

    private CoverageReport parseCoverageXml(String xml, String source) {
        List<UncoveredClass> uncovered = new ArrayList<>();
        double lineCov = 0, branchCov = 0;

        Pattern classPat = Pattern.compile(
                "<class name=\"([^\"]+)\"[^>]*line-rate=\"([0-9.]+)\"[^>]*branch-rate=\"([0-9.]*)\"");
        Matcher m = classPat.matcher(xml);
        int count = 0;
        double totalLine = 0, totalBranch = 0;

        while (m.find()) {
            count++;
            double lr = Double.parseDouble(m.group(2));
            double br = m.group(3).isEmpty() ? 0 : Double.parseDouble(m.group(3));
            totalLine   += lr;
            totalBranch += br;
            if (lr < 0.8) uncovered.add(new UncoveredClass(m.group(1), lr, br));
        }

        if (count > 0) { lineCov = totalLine / count; branchCov = totalBranch / count; }
        return new CoverageReport(uncovered, lineCov, branchCov, source);
    }

    @SuppressWarnings("unchecked")
    private CiRunStatus parseGitHubRunStatus(String json, String owner, String repo, long runId) {
        try {
            Map<String, Object> data = MAPPER.readValue(json, Map.class);
            return new CiRunStatus(
                    owner + "/" + repo, runId,
                    (String) data.getOrDefault("status", "unknown"),
                    (String) data.getOrDefault("conclusion", null),
                    (String) data.getOrDefault("html_url", ""),
                    Instant.now());
        } catch (Exception e) {
            return new CiRunStatus(owner + "/" + repo, runId, "error", null, "", Instant.now());
        }
    }

    private String runCommand(String command, Path workDir, int timeoutSec) {
        try {
            ProcessBuilder pb = new ProcessBuilder("bash", "-c", command)
                    .directory(workDir.toAbsolutePath().toFile())
                    .redirectErrorStream(true);
            Process proc = pb.start();
            proc.waitFor(timeoutSec, TimeUnit.SECONDS);
            return new String(proc.getInputStream().readAllBytes()).strip();
        } catch (Exception e) { return "ERROR: " + e.getMessage(); }
    }

    private String extractStackTrace(String output, String className, String methodName) {
        int idx = output.indexOf(className + "." + methodName);
        if (idx < 0) return "";
        int end = output.indexOf("\n\n", idx);
        String trace = end > 0 ? output.substring(idx, end) : output.substring(idx);
        return trace.length() > 800 ? trace.substring(0, 800) + "…" : trace;
    }

    private Duration parseMavenDuration(String output) {
        Pattern p = Pattern.compile("Total time:\\s+([0-9.]+) s");
        Matcher m = p.matcher(output);
        if (m.find()) return Duration.ofMillis((long)(Double.parseDouble(m.group(1)) * 1000));
        return Duration.ZERO;
    }

    private String extractLine(String text, String prefix) {
        return Arrays.stream(text.split("\n"))
                .filter(l -> l.trim().startsWith(prefix))
                .findFirst()
                .map(l -> l.substring(l.indexOf(prefix) + prefix.length()).strip())
                .orElse("");
    }

    private String extractCodeBlock(String text, String lang) {
        Pattern p = Pattern.compile("```" + lang + "\\n(.*?)```", Pattern.DOTALL);
        Matcher m = p.matcher(text);
        return m.find() ? m.group(1).strip() : "";
    }

    // ── Data types ─────────────────────────────────────────────────────────

    public enum TestFormat { MAVEN, PYTEST, GO_TEST, CARGO, JUNIT_XML, GENERIC }

    public record FailedTest(String testName, String className, String message, String stackTrace) {}

    public record TestRunResult(
            int             total,
            int             failedCount,
            int             skipped,
            List<FailedTest> failedTests,
            boolean         buildFailed,
            String          rawOutput,
            TestFormat      format,
            Duration        duration
    ) {
        public boolean passed()  { return failedCount == 0 && !buildFailed; }
        public int     passed2() { return total - failedCount - skipped; }

        public String summary() {
            return String.format("TestRun[%s]: %d/%d passed, %d failed, %d skipped | %s",
                    format, passed2(), total, failedCount, skipped,
                    passed() ? "GREEN" : "RED");
        }
    }

    public record FixProposal(
            String  testName,
            String  className,
            String  rootCause,
            String  fileToFix,
            String  patch,
            String  fullAnalysis,
            Instant proposedAt
    ) {
        public boolean hasPatch() { return patch != null && !patch.isBlank(); }
        static FixProposal unavailable(FailedTest f, String reason) {
            return new FixProposal(f.testName(), f.className(), reason, "", "", "", Instant.now());
        }
    }

    public record FixProposalSet(List<FixProposal> proposals, TestRunResult run, Instant generatedAt) {
        static FixProposalSet empty() { return new FixProposalSet(List.of(), null, Instant.now()); }
        public boolean isEmpty() { return proposals.isEmpty(); }
    }

    public record AppliedFix(FixProposal proposal, boolean patchApplied, boolean verified,
                              String verifyOutput, Instant appliedAt) {
        public boolean dryRun() { return verifyOutput.startsWith("DRY_RUN"); }
        static AppliedFix dryRun(FixProposal p) {
            return new AppliedFix(p, false, false, "DRY_RUN", Instant.now());
        }
        static AppliedFix patchFailed(FixProposal p) {
            return new AppliedFix(p, false, false, "", Instant.now());
        }
    }

    public record FixApplicationResult(
            List<AppliedFix> applied,
            long             verifiedCount,
            boolean          dryRun
    ) {
        public String summary() {
            return dryRun
                    ? "DRY-RUN: would apply " + applied.size() + " fix(es)"
                    : verifiedCount + "/" + applied.size() + " fixes verified";
        }
    }

    public record FlakyTest(String testName, double failureRate, int runCount, boolean isFlaky) {
        public String summary() {
            return String.format("Flaky[%s]: %.0f%% failure rate over %d runs",
                    testName, failureRate * 100, runCount);
        }
    }

    public record UncoveredClass(String className, double lineCoverage, double branchCoverage) {}

    public record CoverageReport(
            List<UncoveredClass> uncoveredClasses,
            double               overallLineCoverage,
            double               overallBranchCoverage,
            String               source
    ) {
        public String summary() {
            return String.format("Coverage: %.0f%% lines, %.0f%% branches | %d classes below 80%%",
                    overallLineCoverage * 100, overallBranchCoverage * 100, uncoveredClasses.size());
        }
    }

    public record CiRunStatus(
            String  repository,
            long    runId,
            String  status,
            String  conclusion,
            String  url,
            Instant polledAt
    ) {
        public boolean isComplete() { return "completed".equals(status); }
        public boolean passed()     { return "success".equals(conclusion); }
    }
}
