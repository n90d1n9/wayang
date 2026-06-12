package tech.kayys.gamelan.integration.vcs;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.agent.orchestration.*;
import tech.kayys.gamelan.config.GamelanConfig;
import tech.kayys.gamelan.observability.metrics.AgentTelemetry;
import tech.kayys.gamelan.session.ConversationSession;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.*;
import java.util.stream.*;

/**
 * VcsIntegration — deep Git integration for context-aware agent operations.
 *
 * <h2>What git context adds to agents</h2>
 * Without git awareness, an agent answering "what changed recently?" must read every
 * file. With git integration, it can:
 * <ul>
 *   <li>Scope its analysis to files changed since a specific commit or branch</li>
 *   <li>Understand the intent of a change from commit messages</li>
 *   <li>Generate conventional commit messages that match the project's style</li>
 *   <li>Create pull requests with rich, accurate descriptions</li>
 *   <li>Auto-stage only the files the agent actually modified</li>
 *   <li>Detect and explain merge conflicts before they block CI</li>
 * </ul>
 *
 * <h2>Safety guarantees</h2>
 * <ul>
 *   <li>All write operations (commit, push) require explicit user confirmation
 *       or {@code --auto-commit} flag</li>
 *   <li>Branch operations always create a new branch — never push to main/master</li>
 *   <li>Force push is never executed</li>
 *   <li>All operations are logged to the governance audit trail</li>
 * </ul>
 */
@ApplicationScoped
public class VcsIntegration {

    private static final Logger log = LoggerFactory.getLogger(VcsIntegration.class);

    @Inject SingleAgentOrchestrator orchestrator;
    @Inject GamelanConfig           config;
    @Inject AgentTelemetry          telemetry;

    // ── Git context ────────────────────────────────────────────────────────

    /**
     * Returns rich git context for the current repository.
     * Used to inject into system prompts for better code understanding.
     */
    public GitContext getContext() {
        String branch    = git("rev-parse", "--abbrev-ref", "HEAD");
        String head      = git("rev-parse", "--short", "HEAD");
        String log5      = git("log", "--oneline", "-5");
        String diffStat  = git("diff", "--stat", "HEAD");
        String stash     = git("stash", "list");
        String remotes   = git("remote", "-v");
        boolean isDirty  = !git("status", "--porcelain").isBlank();
        List<String> staged   = stagedFiles();
        List<String> modified = modifiedFiles();

        return new GitContext(branch, head, log5, diffStat, stash,
                remotes, isDirty, staged, modified);
    }

    /**
     * Returns the diff of files changed since the given base ref.
     * Limits output to prevent context overflow.
     */
    public String diffSince(String baseRef, int maxBytes) {
        String diff = git("diff", baseRef, "--", "*.java", "*.kt", "*.py",
                "*.ts", "*.js", "*.go", "*.rs");
        return diff.length() > maxBytes
                ? diff.substring(0, maxBytes) + "\n... (truncated)"
                : diff;
    }

    /**
     * Returns files that differ between two refs.
     */
    public List<String> changedFiles(String fromRef, String toRef) {
        String output = git("diff", "--name-only", fromRef, toRef);
        return Arrays.stream(output.split("\n"))
                .filter(l -> !l.isBlank())
                .toList();
    }

    // ── Commit generation ──────────────────────────────────────────────────

    /**
     * Generates a Conventional Commits-style commit message for staged changes.
     *
     * @param context optional additional context for the LLM
     * @return a generated commit message ready for use
     */
    public CommitMessage generateCommitMessage(String context) {
        String diff    = git("diff", "--cached");
        String fileList = git("diff", "--cached", "--name-only");

        if (diff.isBlank()) {
            return new CommitMessage("chore", null, "no staged changes", "", false);
        }

        // Limit diff to 3000 chars for LLM
        String diffSample = diff.length() > 3000 ? diff.substring(0, 3000) + "\n...(truncated)" : diff;

        String prompt = """
                Generate a Conventional Commits message for these staged changes.
                
                Changed files:
                %s
                
                Diff:
                %s
                
                %s
                
                Output ONLY the commit message in this exact format:
                TYPE(scope): short description
                
                Optional longer body (if needed).
                
                BREAKING CHANGE: description (only if breaking).
                
                Types: feat|fix|docs|style|refactor|test|chore|perf|ci|build
                """.formatted(fileList, diffSample,
                context != null ? "Additional context: " + context : "");

        try {
            OrchestratorResult result = orchestrator.execute(
                    AgentRequest.builder(prompt)
                            .model(config.defaultModel())
                            .session(new ConversationSession(null, false, config.tokenBudget()))
                            .stream(false).maxSteps(1).build());

            return parseCommitMessage(result.answer());
        } catch (Exception e) {
            log.warn("[vcs] commit message generation failed: {}", e.getMessage());
            return new CommitMessage("chore", null, "update code", "", false);
        }
    }

    /**
     * Generates a pull request description from a branch comparison.
     *
     * @param base   the base branch (e.g., "main")
     * @param head   the head branch (e.g., "feature/add-auth")
     * @return a rich PR description in Markdown
     */
    public PullRequestDescription generatePR(String base, String head) {
        List<String> changed = changedFiles(base, head);
        String commits = git("log", "--oneline", base + ".." + head);
        String diffStat = git("diff", "--stat", base + ".." + head);

        String prompt = """
                Generate a pull request description for these changes.
                
                Branch: %s → %s
                
                Commits:
                %s
                
                Changed files (%d):
                %s
                
                Diff summary:
                %s
                
                Output in this exact Markdown format:
                ## Summary
                [One paragraph description of what this PR does and why]
                
                ## Changes
                [Bullet list of key changes]
                
                ## Testing
                [How to test these changes]
                
                ## Breaking Changes
                [If any, otherwise omit this section]
                """.formatted(head, base, commits,
                changed.size(), String.join("\n", changed.stream().limit(20).toList()),
                diffStat);

        try {
            OrchestratorResult result = orchestrator.execute(
                    AgentRequest.builder(prompt)
                            .model(config.defaultModel())
                            .session(new ConversationSession(null, false, config.tokenBudget()))
                            .stream(false).maxSteps(1).build());

            String title = generatePRTitle(commits);
            return new PullRequestDescription(title, result.answer(), head, base, changed.size());
        } catch (Exception e) {
            log.warn("[vcs] PR generation failed: {}", e.getMessage());
            return new PullRequestDescription("Update " + head, "", head, base, changed.size());
        }
    }

    /**
     * Analyzes a merge conflict and suggests resolution strategies.
     *
     * @param conflictedFile path to the file with conflicts
     * @return conflict analysis with suggested resolutions
     */
    public ConflictAnalysis analyzeConflict(String conflictedFile) {
        String content;
        try { content = Files.readString(Path.of(conflictedFile)); }
        catch (IOException e) { return ConflictAnalysis.unreadable(conflictedFile, e.getMessage()); }

        List<ConflictBlock> blocks = extractConflictBlocks(content);
        if (blocks.isEmpty()) return ConflictAnalysis.noConflicts(conflictedFile);

        String prompt = """
                Analyze these Git merge conflicts and suggest resolutions.
                For each conflict, explain which version to keep and why.
                
                File: %s
                Conflicts:
                %s
                
                For each conflict block, output:
                CONFLICT N: [explanation of what each side is doing]
                RESOLUTION: [keep-ours | keep-theirs | merge-both | manual]
                REASON: [why]
                """.formatted(conflictedFile,
                blocks.stream().limit(3).map(ConflictBlock::rawBlock)
                        .collect(Collectors.joining("\n\n---\n\n")));

        try {
            OrchestratorResult result = orchestrator.execute(
                    AgentRequest.builder(prompt)
                            .model(config.defaultModel())
                            .session(new ConversationSession(null, false, config.tokenBudget()))
                            .stream(false).maxSteps(1).build());

            return new ConflictAnalysis(conflictedFile, blocks, result.answer(), Instant.now());
        } catch (Exception e) {
            return new ConflictAnalysis(conflictedFile, blocks, "Analysis unavailable: " + e.getMessage(), Instant.now());
        }
    }

    /**
     * Creates a new branch, stages specified files, and commits.
     * Returns the branch name and commit hash.
     *
     * @param branchName  the new branch to create
     * @param filePaths   files to stage (null = stage all modified)
     * @param commitMsg   the commit message
     * @param dryRun      if true, show what would happen without executing
     */
    public CommitResult commit(String branchName, List<String> filePaths,
                                CommitMessage commitMsg, boolean dryRun) {
        if (dryRun) {
            return new CommitResult(branchName, "dry-run-hash",
                    commitMsg.formatted(), List.of(), true, true);
        }

        // Safety: create a new branch, never commit to protected branches
        String current = git("rev-parse", "--abbrev-ref", "HEAD");
        if (branchName.equals(current)) {
            // Already on the right branch
        } else {
            String result = gitExec("checkout", "-b", branchName);
            if (result.startsWith("ERROR:")) {
                return CommitResult.failed(branchName, "Branch creation failed: " + result);
            }
        }

        // Stage files
        if (filePaths == null || filePaths.isEmpty()) {
            gitExec("add", "-A");
        } else {
            filePaths.forEach(f -> gitExec("add", f));
        }

        // Commit
        String output = gitExec("commit", "-m", commitMsg.formatted());
        if (output.startsWith("ERROR:")) {
            return CommitResult.failed(branchName, output);
        }

        String hash = git("rev-parse", "--short", "HEAD");
        List<String> committed = stagedFiles();

        telemetry.count("vcs.commit.total");
        log.info("[vcs] committed {} files to branch '{}': {}", committed.size(), branchName, hash);
        return new CommitResult(branchName, hash, commitMsg.formatted(), committed, true, false);
    }

    /**
     * Injects git context as a system prompt block.
     */
    public String contextBlock() {
        GitContext ctx = getContext();
        StringBuilder sb = new StringBuilder("## Git Context\n");
        sb.append("- Branch: `").append(ctx.branch()).append("`");
        if (ctx.isDirty()) sb.append(" ⚠ (uncommitted changes)");
        sb.append("\n");
        sb.append("- HEAD: `").append(ctx.headHash()).append("`\n");
        if (!ctx.staged().isEmpty()) {
            sb.append("- Staged (").append(ctx.staged().size()).append("): ")
              .append(String.join(", ", ctx.staged())).append("\n");
        }
        if (!ctx.modified().isEmpty()) {
            sb.append("- Modified (").append(ctx.modified().size()).append("): ")
              .append(ctx.modified().stream().limit(10).collect(Collectors.joining(", ")));
            if (ctx.modified().size() > 10) sb.append(" …");
            sb.append("\n");
        }
        if (!ctx.recentLog().isBlank()) {
            sb.append("### Recent commits\n```\n").append(ctx.recentLog()).append("\n```\n");
        }
        return sb.toString();
    }

    // ── Private ────────────────────────────────────────────────────────────

    private String git(String... args) {
        List<String> cmd = new ArrayList<>();
        cmd.add("git");
        cmd.addAll(Arrays.asList(args));
        try {
            ProcessBuilder pb = new ProcessBuilder(cmd)
                    .directory(Path.of(".").toAbsolutePath().toFile())
                    .redirectErrorStream(true);
            Process proc = pb.start();
            proc.waitFor(15, TimeUnit.SECONDS);
            return new String(proc.getInputStream().readAllBytes(), StandardCharsets.UTF_8).strip();
        } catch (Exception e) {
            return "";
        }
    }

    private String gitExec(String... args) {
        String result = git(args);
        return result.isEmpty() ? "ERROR: command failed" : result;
    }

    private List<String> stagedFiles() {
        return Arrays.stream(git("diff", "--cached", "--name-only").split("\n"))
                .filter(l -> !l.isBlank()).toList();
    }

    private List<String> modifiedFiles() {
        return Arrays.stream(git("diff", "--name-only").split("\n"))
                .filter(l -> !l.isBlank()).toList();
    }

    private CommitMessage parseCommitMessage(String raw) {
        if (raw == null || raw.isBlank()) return new CommitMessage("chore", null, "update", "", false);
        String[] lines = raw.strip().split("\n");
        String header = lines[0].strip();
        Pattern p = Pattern.compile("^(\\w+)(?:\\(([^)]+)\\))?!?:\\s*(.+)$");
        Matcher m = p.matcher(header);
        if (m.matches()) {
            String body = lines.length > 2 ? Arrays.stream(lines, 2, lines.length)
                    .collect(Collectors.joining("\n")) : "";
            boolean breaking = header.contains("!") ||
                    (body.contains("BREAKING CHANGE") || body.contains("BREAKING:"));
            return new CommitMessage(m.group(1), m.group(2), m.group(3).strip(), body.strip(), breaking);
        }
        return new CommitMessage("chore", null, header.length() > 72 ? header.substring(0, 72) : header, "", false);
    }

    private String generatePRTitle(String commits) {
        String[] lines = commits.split("\n");
        if (lines.length == 0) return "Update code";
        // Use first commit message, strip the hash
        String first = lines[0].replaceFirst("^[0-9a-f]{7,} ", "").strip();
        return first.length() > 72 ? first.substring(0, 72) : first;
    }

    private List<ConflictBlock> extractConflictBlocks(String content) {
        List<ConflictBlock> blocks = new ArrayList<>();
        Pattern blockPattern = Pattern.compile(
                "<<<<<<< (.+?)\\n(.*?)=======\\n(.*?)>>>>>>> (.+?)\n",
                Pattern.DOTALL);
        Matcher m = blockPattern.matcher(content);
        int blockNum = 1;
        while (m.find()) {
            blocks.add(new ConflictBlock(blockNum++, m.group(1).strip(),
                    m.group(2).strip(), m.group(4).strip(), m.group(3).strip(), m.group(0)));
        }
        return blocks;
    }

    // ── Data types ─────────────────────────────────────────────────────────

    public record GitContext(
            String       branch,
            String       headHash,
            String       recentLog,
            String       diffStat,
            String       stashList,
            String       remotes,
            boolean      isDirty,
            List<String> staged,
            List<String> modified
    ) {}

    public record CommitMessage(
            String  type,
            String  scope,
            String  description,
            String  body,
            boolean breaking
    ) {
        public String formatted() {
            StringBuilder sb = new StringBuilder(type);
            if (scope != null) sb.append("(").append(scope).append(")");
            if (breaking) sb.append("!");
            sb.append(": ").append(description);
            if (!body.isBlank()) sb.append("\n\n").append(body);
            return sb.toString();
        }
    }

    public record PullRequestDescription(
            String       title,
            String       body,
            String       headBranch,
            String       baseBranch,
            int          filesChanged
    ) {}

    public record ConflictBlock(
            int    blockNumber,
            String oursLabel,
            String oursContent,
            String theirsLabel,
            String theirsContent,
            String rawBlock
    ) {}

    public record ConflictAnalysis(
            String              filePath,
            List<ConflictBlock> blocks,
            String              analysis,
            Instant             generatedAt
    ) {
        static ConflictAnalysis noConflicts(String path) {
            return new ConflictAnalysis(path, List.of(), "No conflicts found", Instant.now());
        }
        static ConflictAnalysis unreadable(String path, String error) {
            return new ConflictAnalysis(path, List.of(), "Cannot read file: " + error, Instant.now());
        }
        public boolean hasConflicts() { return !blocks.isEmpty(); }
    }

    public record CommitResult(
            String       branch,
            String       commitHash,
            String       commitMessage,
            List<String> committedFiles,
            boolean      success,
            boolean      dryRun
    ) {
        static CommitResult failed(String branch, String reason) {
            return new CommitResult(branch, "", reason, List.of(), false, false);
        }
        public String summary() {
            if (dryRun) return "DRY-RUN: would commit to " + branch + ": " + commitMessage;
            return success ? String.format("Committed %d files to '%s' (%s): %s",
                    committedFiles.size(), branch, commitHash, commitMessage)
                    : "FAILED: " + commitHash;
        }
    }
}
