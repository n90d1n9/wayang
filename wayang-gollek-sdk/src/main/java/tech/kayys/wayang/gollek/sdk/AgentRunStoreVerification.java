package tech.kayys.wayang.gollek.sdk;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pass/fail verification report for an agent run-store diagnostics snapshot.
 */
public record AgentRunStoreVerification(
        AgentRunStoreDiagnostics diagnostics,
        List<AgentRunStoreVerificationIssue> issues) {

    public AgentRunStoreVerification {
        diagnostics = diagnostics == null
                ? AgentRunStore.memory().diagnostics()
                : diagnostics;
        issues = issues == null ? List.of() : List.copyOf(issues);
    }

    public boolean passed() {
        return passed(AgentRunStoreVerificationPolicy.lenient());
    }

    public boolean passed(AgentRunStoreVerificationPolicy policy) {
        AgentRunStoreVerificationPolicy resolvedPolicy = policy == null
                ? AgentRunStoreVerificationPolicy.lenient()
                : policy;
        return errorCount() == 0
                && !(resolvedPolicy.failOnWarnings() && warningCount() > 0);
    }

    public int exitCode() {
        return exitCode(AgentRunStoreVerificationPolicy.lenient());
    }

    public int exitCode(AgentRunStoreVerificationPolicy policy) {
        return passed(policy) ? 0 : 1;
    }

    public int issueCount() {
        return issues.size();
    }

    public int errorCount() {
        return (int) issues.stream()
                .filter(AgentRunStoreVerificationIssue::error)
                .count();
    }

    public int warningCount() {
        return (int) issues.stream()
                .filter(AgentRunStoreVerificationIssue::warning)
                .count();
    }

    public Map<String, Object> toMap() {
        return toMap(AgentRunStoreVerificationPolicy.lenient());
    }

    public Map<String, Object> toMap(AgentRunStoreVerificationPolicy policy) {
        AgentRunStoreVerificationPolicy resolvedPolicy = policy == null
                ? AgentRunStoreVerificationPolicy.lenient()
                : policy;
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("passed", passed(resolvedPolicy));
        values.put("exitCode", exitCode(resolvedPolicy));
        values.put("issueCount", issueCount());
        values.put("errorCount", errorCount());
        values.put("warningCount", warningCount());
        values.put("policy", resolvedPolicy.toMap());
        values.put("issues", issues.stream()
                .map(AgentRunStoreVerificationIssue::toMap)
                .toList());
        values.put("diagnostics", diagnostics.toMap());
        return AgentRunEnvelopeMaps.copy(values);
    }
}
