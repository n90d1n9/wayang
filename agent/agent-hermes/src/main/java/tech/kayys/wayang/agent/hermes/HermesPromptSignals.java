package tech.kayys.wayang.agent.hermes;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Prompt-only Hermes heuristics shared by request planners and resolvers.
 */
final class HermesPromptSignals {

    private static final Pattern EVERY_INTERVAL = Pattern.compile(
            "\\bevery\\s+(\\d+)\\s+(minute|minutes|hour|hours|day|days|week|weeks|month|months)\\b",
            Pattern.CASE_INSENSITIVE);

    private HermesPromptSignals() {
    }

    static Optional<String> inferAutomationSchedule(String prompt) {
        String normalized = HermesRequestValues.normalizeText(prompt);
        if (normalized.isBlank()) {
            return Optional.empty();
        }

        Matcher interval = EVERY_INTERVAL.matcher(normalized);
        if (interval.find()) {
            return Optional.of("every " + interval.group(1) + " " + interval.group(2));
        }
        if (contains(normalized, "nightly", "every night")) {
            return Optional.of("nightly");
        }
        if (contains(normalized, "daily", "every day", "each day", "once a day")) {
            return Optional.of("daily");
        }
        if (contains(normalized, "weekly", "every week", "each week", "once a week")) {
            return Optional.of("weekly");
        }
        if (contains(normalized, "monthly", "every month", "each month", "once a month")) {
            return Optional.of("monthly");
        }
        if (contains(normalized, "hourly", "every hour", "each hour", "once an hour")) {
            return Optional.of("hourly");
        }
        return Optional.empty();
    }

    static boolean suggestsIsolation(String text) {
        return contains(text, "sandbox", "isolated", "container", "docker", "untrusted", "unsafe");
    }

    static boolean suggestsRemote(String text) {
        return contains(text, "remote", "ssh", "vps", "server");
    }

    static boolean suggestsServerless(String text) {
        return contains(text, "serverless", "modal", "daytona");
    }

    static boolean suggestsDelegation(String prompt) {
        return contains(prompt,
                "parallel",
                "sub-agent",
                "sub agent",
                "delegate",
                "fan out",
                "fan-out",
                "divide and conquer",
                "large workstream",
                "massive workstream",
                "concurrently",
                "independent tracks",
                "multiple agents");
    }

    static List<String> inferredDelegationLanes(String prompt) {
        String normalized = HermesRequestValues.normalizeText(prompt);
        LinkedHashSet<String> lanes = new LinkedHashSet<>();
        if (contains(normalized, "research", "investigate", "survey", "benchmark")) {
            lanes.add("research");
        }
        if (contains(normalized, "implement", "code", "patch", "build", "refactor")) {
            lanes.add("implementation");
        }
        if (contains(normalized, "test", "verify", "qa", "smoke", "validation")) {
            lanes.add("verification");
        }
        if (contains(normalized, "review", "audit", "risk")) {
            lanes.add("review");
        }
        if (contains(normalized, "document", "docs", "readme", "report")) {
            lanes.add("documentation");
        }
        if (lanes.isEmpty()) {
            lanes.addAll(defaultDelegationLanes(3));
        }
        return List.copyOf(lanes);
    }

    static Optional<String> provider(String prompt) {
        String normalized = HermesRequestValues.normalizeText(prompt);
        if (contains(normalized, "openrouter", "open router")) {
            return Optional.of("openrouter");
        }
        if (contains(normalized, "nous portal", "nousportal")) {
            return Optional.of("nous-portal");
        }
        if (contains(normalized, "ollama")) {
            return Optional.of("ollama");
        }
        if (contains(normalized, "vllm")) {
            return Optional.of("vllm");
        }
        return Optional.empty();
    }

    static boolean suggestsLocalProvider(String prompt) {
        return contains(prompt,
                "local model",
                "run locally",
                "offline",
                "private local",
                "zero-cost",
                "zero cost",
                "ollama",
                "vllm");
    }

    static boolean suggestsHighContext(String prompt) {
        return contains(prompt,
                "high-context",
                "high context",
                "long context",
                "large context",
                "massive context",
                "long-running",
                "long running",
                "multi-step",
                "complex workstream");
    }

    static boolean suggestsApiGateway(String prompt) {
        return contains(prompt, "api gateway", "openrouter", "open router", "nous portal");
    }

    static boolean suggestsMemoryReflection(String prompt) {
        return contains(prompt,
                "remember this",
                "remember that",
                "save to memory",
                "long-term memory",
                "long term memory",
                "update my profile",
                "user profile",
                "memory reflection",
                "reflect memory",
                "consolidate memory",
                "learn my preference",
                "learn this preference");
    }

    static String inferredMemoryPriority(String prompt) {
        return contains(prompt, "important", "critical", "always remember", "never forget")
                ? "high"
                : "normal";
    }

    static boolean suggestsTrajectoryExport(String prompt) {
        return contains(prompt,
                "export trajectory",
                "save trajectory",
                "trajectory export",
                "audit trail",
                "execution trace",
                "export trace",
                "save trace",
                "otel trace",
                "opentelemetry");
    }

    private static List<String> defaultDelegationLanes(int count) {
        List<String> defaults = List.of("analysis", "execution", "verification", "review");
        return defaults.subList(0, Math.min(Math.max(count, 0), defaults.size()));
    }

    private static boolean contains(String value, String... needles) {
        return HermesRequestValues.containsAny(HermesRequestValues.normalizeText(value), needles);
    }
}
