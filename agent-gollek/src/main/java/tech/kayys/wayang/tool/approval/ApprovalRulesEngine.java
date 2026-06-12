package tech.kayys.gamelan.tool.approval;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.observability.metrics.AgentTelemetry;

import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

/**
 * ApprovalRulesEngine — runtime approval system with four rule types and three autonomy levels.
 *
 * <h2>From the OPENDEV paper (§2.4.1)</h2>
 * Before any tool call reaches its handler, the runtime approval system gates execution based on
 * user-configured trust boundaries. Three autonomy levels control the default posture: Manual
 * requires explicit approval for every tool call; Semi-Auto auto-approves read-only operations
 * (a curated allowlist of commands such as ls, cat, git status) while prompting for writes; and
 * Auto approves all operations for trusted workflows.
 *
 * <h2>Four rule types</h2>
 * <pre>
 * PATTERN  — regex match against the full command string
 * COMMAND  — exact match
 * PREFIX   — prefix match (e.g., "git" matching "git push")
 * DANGER   — regex match with auto-DENY semantics (highest priority, never overrideable)
 * </pre>
 *
 * <h2>Priority resolution</h2>
 * Rules are evaluated in ascending priority order (lower number = higher priority).
 * Default DANGER rules at priority 100 are always active and cannot be overridden by any
 * user configuration or autonomy-level change.
 *
 * <h2>Persistence</h2>
 * Rules persist across sessions via two JSON stores:
 * - User-global: {@code ~/.gamelan/permissions.json}
 * - Project-scoped: {@code .gamelan/permissions.json}
 * Project rules take precedence for the same pattern.
 *
 * <h2>Approval fatigue prevention (paper §3.3)</h2>
 * Approval rules that are marked "always allow" are persisted so they survive session restarts.
 * Without persistence, users must re-approve the same operations every session, causing approval
 * fatigue that leads to blanket auto-approval, defeating the safety system entirely.
 */
@ApplicationScoped
public class ApprovalRulesEngine {

    private static final Logger log = LoggerFactory.getLogger(ApprovalRulesEngine.class);

    @Inject AgentTelemetry telemetry;

    // Priority-ordered rules (lower number = higher priority)
    private final List<ApprovalRule> rules = new CopyOnWriteArrayList<>();

    // Built-in DANGER rules — always active, cannot be overridden (paper §2.4.1)
    private static final List<ApprovalRule> BUILTIN_DANGER_RULES = List.of(
            ApprovalRule.danger("rm -rf /",          "Recursive delete of root filesystem"),
            ApprovalRule.danger("rm -rf \\*",        "Wildcard recursive delete"),
            ApprovalRule.danger("rm -rf ~",          "Recursive delete of home directory"),
            ApprovalRule.danger("chmod 777",          "World-writable permissions"),
            ApprovalRule.danger(":(\\){:|:&};:",     "Fork bomb pattern"),
            ApprovalRule.danger("dd if=/dev",        "Raw device write"),
            ApprovalRule.danger("> /dev/sd",         "Direct disk overwrite"),
            ApprovalRule.danger("mkfs\\.",            "Filesystem format command"),
            ApprovalRule.danger("curl .* \\| (sudo )?bash", "Remote code execution via pipe"),
            ApprovalRule.danger("wget .* \\| (sudo )?bash", "Remote code execution via pipe"),
            ApprovalRule.danger("sudo rm -rf",       "Privileged recursive delete")
    );

    // Read-only safe commands (Semi-Auto level — auto-approved)
    private static final Set<String> SAFE_READ_COMMANDS = Set.of(
            "ls", "cat", "head", "tail", "less", "more", "find",
            "git status", "git diff", "git log", "git show", "git branch",
            "pwd", "echo", "which", "whoami", "env", "printenv",
            "mvn -v", "java -version", "node -v", "python --version",
            "grep", "awk", "sed -n", "wc", "sort", "uniq"
    );

    private AutonomyLevel autonomyLevel = AutonomyLevel.SEMI_AUTO;

    // ── Public API ─────────────────────────────────────────────────────────

    /**
     * Evaluates whether a command/tool call should be approved.
     *
     * @param command     the full command string or tool+args representation
     * @param toolName    the name of the tool being invoked
     * @return an ApprovalDecision indicating the action and matching rule
     */
    public ApprovalDecision evaluate(String command, String toolName) {
        if (command == null) command = "";
        String finalCommand = command;

        // Step 1: DANGER rules always run first regardless of autonomy level
        for (ApprovalRule danger : BUILTIN_DANGER_RULES) {
            if (danger.matches(finalCommand)) {
                telemetry.count("approval.danger.blocked");
                log.warn("[approval] DANGER blocked: '{}' matched '{}'",
                        truncate(command, 60), danger.description());
                return new ApprovalDecision(ApprovalAction.DENY, danger,
                        "Blocked by danger rule: " + danger.description(), Instant.now());
            }
        }

        // Step 2: User/project rules by ascending priority
        List<ApprovalRule> sorted = rules.stream()
                .sorted(Comparator.comparingInt(ApprovalRule::priority))
                .toList();
        for (ApprovalRule rule : sorted) {
            if (rule.matches(finalCommand)) {
                telemetry.count("approval.rule." + rule.action().name().toLowerCase());
                log.debug("[approval] rule matched: {} → {} ({})",
                        truncate(command, 40), rule.action(), rule.description());
                return new ApprovalDecision(rule.action(), rule,
                        rule.description(), Instant.now());
            }
        }

        // Step 3: Autonomy level default
        ApprovalAction defaultAction = switch (autonomyLevel) {
            case AUTO     -> ApprovalAction.APPROVE;
            case SEMI_AUTO -> isSafeReadCommand(command, toolName)
                              ? ApprovalAction.APPROVE : ApprovalAction.REQUIRE_APPROVAL;
            case MANUAL   -> ApprovalAction.REQUIRE_APPROVAL;
        };
        telemetry.count("approval.default." + defaultAction.name().toLowerCase());
        return new ApprovalDecision(defaultAction, null,
                "Default (" + autonomyLevel + ")", Instant.now());
    }

    /**
     * Adds a rule at runtime. Rules are persisted to the appropriate store.
     *
     * @param rule  the new rule
     * @param scope USER_GLOBAL or PROJECT
     */
    public void addRule(ApprovalRule rule, RuleScope scope) {
        rules.add(rule);
        rules.sort(Comparator.comparingInt(ApprovalRule::priority));
        persist(scope);
        log.info("[approval] added rule: {} priority={} scope={}", rule.description(),
                rule.priority(), scope);
        telemetry.count("approval.rule.added");
    }

    /** Removes all rules matching the given pattern string. */
    public int removeRule(String patternString) {
        int before = rules.size();
        rules.removeIf(r -> patternString.equals(r.pattern()));
        int removed = before - rules.size();
        if (removed > 0) persist(RuleScope.USER_GLOBAL);
        return removed;
    }

    /** Sets the autonomy level. */
    public void setAutonomyLevel(AutonomyLevel level) {
        this.autonomyLevel = level;
        log.info("[approval] autonomy level set to {}", level);
        telemetry.count("approval.level.changed." + level.name().toLowerCase());
    }

    public AutonomyLevel getAutonomyLevel() { return autonomyLevel; }

    /** Returns all active rules (builtin danger + user/project rules). */
    public List<ApprovalRule> allRules() {
        List<ApprovalRule> all = new ArrayList<>(BUILTIN_DANGER_RULES);
        all.addAll(rules);
        return Collections.unmodifiableList(all);
    }

    /** Loads rules from both user-global and project-scoped stores. */
    public void loadPersistedRules() {
        Path userGlobal = Path.of(System.getProperty("user.home"), ".gamelan", "permissions.json");
        Path project    = Path.of(".gamelan", "permissions.json");
        rules.clear();
        loadFromFile(userGlobal);
        loadFromFile(project); // project rules override user-global for same pattern
        log.info("[approval] loaded {} persisted rules", rules.size());
    }

    // ── Private ────────────────────────────────────────────────────────────

    private boolean isSafeReadCommand(String command, String toolName) {
        if (toolName != null) {
            String tl = toolName.toLowerCase();
            if (tl.contains("read") || tl.contains("list") || tl.contains("search") ||
                tl.contains("glob") || tl.contains("find") || tl.contains("git")) return true;
        }
        String lower = command.trim().toLowerCase();
        return SAFE_READ_COMMANDS.stream().anyMatch(lower::startsWith);
    }

    private void persist(RuleScope scope) {
        Path file = scope == RuleScope.PROJECT
                ? Path.of(".gamelan", "permissions.json")
                : Path.of(System.getProperty("user.home"), ".gamelan", "permissions.json");
        try {
            Files.createDirectories(file.getParent());
            StringBuilder sb = new StringBuilder("[\n");
            for (ApprovalRule r : rules) {
                sb.append(String.format(
                        "  {\"type\":\"%s\",\"pattern\":\"%s\",\"action\":\"%s\"," +
                        "\"description\":\"%s\",\"priority\":%d},\n",
                        r.type().name(), escape(r.pattern()), r.action().name(),
                        escape(r.description()), r.priority()));
            }
            if (sb.toString().endsWith(",\n")) sb.setLength(sb.length() - 2);
            sb.append("\n]");
            Files.writeString(file, sb.toString(),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            log.warn("[approval] persist failed: {}", e.getMessage());
        }
    }

    private void loadFromFile(Path file) {
        if (!Files.exists(file)) return;
        try {
            String json = Files.readString(file);
            // Simple JSON parse — extract type/pattern/action/description/priority
            String[] entries = json.split("\\},\\s*\\{");
            for (String entry : entries) {
                try {
                    RuleType   type   = extractEnum(entry, "type",   RuleType.class,   RuleType.PATTERN);
                    String     pat    = extractString(entry, "pattern");
                    ApprovalAction act = extractEnum(entry, "action", ApprovalAction.class, ApprovalAction.REQUIRE_APPROVAL);
                    String     desc   = extractString(entry, "description");
                    int        pri    = extractInt(entry, "priority", 50);
                    if (!pat.isBlank()) rules.add(new ApprovalRule(type, pat, act, desc, pri));
                } catch (Exception ignored) {}
            }
        } catch (IOException e) {
            log.debug("[approval] load failed from {}: {}", file, e.getMessage());
        }
    }

    private String extractString(String json, String key) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(
                "\"" + key + "\"\\s*:\\s*\"([^\"]*)\"").matcher(json);
        return m.find() ? m.group(1) : "";
    }
    private <E extends Enum<E>> E extractEnum(String json, String key, Class<E> cls, E def) {
        try { return Enum.valueOf(cls, extractString(json, key).toUpperCase()); }
        catch (Exception e) { return def; }
    }
    private int extractInt(String json, String key, int def) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(
                "\"" + key + "\"\\s*:\\s*(\\d+)").matcher(json);
        try { return m.find() ? Integer.parseInt(m.group(1)) : def; }
        catch (NumberFormatException e) { return def; }
    }
    private String escape(String s) { return s == null ? "" : s.replace("\"", "\\\""); }
    private String truncate(String s, int max) {
        return s.length() > max ? s.substring(0, max) + "…" : s;
    }

    // ── Data types ─────────────────────────────────────────────────────────

    public enum AutonomyLevel  { MANUAL, SEMI_AUTO, AUTO }
    public enum ApprovalAction { APPROVE, DENY, REQUIRE_APPROVAL, REQUIRE_EDIT }
    public enum RuleType       { PATTERN, COMMAND, PREFIX, DANGER }
    public enum RuleScope      { USER_GLOBAL, PROJECT }

    public record ApprovalRule(
            RuleType       type,
            String         pattern,
            ApprovalAction action,
            String         description,
            int            priority
    ) {
        private transient Pattern compiled;

        public boolean matches(String command) {
            if (command == null) return false;
            return switch (type) {
                case EXACT, COMMAND -> command.trim().equals(pattern);
                case PREFIX         -> command.trim().startsWith(pattern);
                case PATTERN, DANGER -> {
                    if (compiled == null) {
                        try { yield Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(command).find(); }
                        catch (Exception e) { yield command.contains(pattern); }
                    }
                    yield compiled.matcher(command).find();
                }
            };
        }

        static ApprovalRule danger(String pattern, String desc) {
            return new ApprovalRule(RuleType.DANGER, pattern, ApprovalAction.DENY, desc, 100);
        }
        static ApprovalRule alwaysAllow(String pattern, String desc) {
            return new ApprovalRule(RuleType.PATTERN, pattern, ApprovalAction.APPROVE, desc, 10);
        }
        static ApprovalRule alwaysDeny(String pattern, String desc) {
            return new ApprovalRule(RuleType.PATTERN, pattern, ApprovalAction.DENY, desc, 10);
        }
        static ApprovalRule requireApproval(String pattern, String desc) {
            return new ApprovalRule(RuleType.PATTERN, pattern, ApprovalAction.REQUIRE_APPROVAL, desc, 20);
        }
        static ApprovalRule prefix(String prefix, ApprovalAction action, String desc) {
            return new ApprovalRule(RuleType.PREFIX, prefix, action, desc, 30);
        }
    }

    public record ApprovalDecision(
            ApprovalAction action,
            ApprovalRule   matchedRule,
            String         reason,
            Instant        timestamp
    ) {
        public boolean isApproved()   { return action == ApprovalAction.APPROVE; }
        public boolean isDenied()     { return action == ApprovalAction.DENY; }
        public boolean needsHuman()   { return action == ApprovalAction.REQUIRE_APPROVAL ||
                                               action == ApprovalAction.REQUIRE_EDIT; }
    }
}
