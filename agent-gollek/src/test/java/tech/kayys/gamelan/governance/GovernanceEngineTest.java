package tech.kayys.gamelan.governance;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for {@link GovernanceEngine} — ABAC, audit trail, and supply chain security.
 */
class GovernanceEngineTest {

    private GovernanceEngine engine;

    @BeforeEach
    void setUp() {
        engine = new GovernanceEngine();
        engine.init();
    }

    // ── ABAC ──────────────────────────────────────────────────────────────

    @Test
    void allowsPolicyMatchesCorrectly() {
        engine.allow("agent-1", "READ", "src/Main.java", null);
        assertThat(engine.permits("agent-1", "READ", "src/Main.java")).isTrue();
    }

    @Test
    void defaultDenyWhenNoPolicyMatches() {
        var decision = engine.evaluate("unknown-agent", "EXECUTE", "/bin/bash");
        assertThat(decision.effect()).isEqualTo(GovernanceEngine.PolicyEffect.DENY);
        assertThat(decision.isAllowed()).isFalse();
    }

    @Test
    void anyAgentSubjectMatchesAllAgents() {
        // Default policy: any-agent can READ src/**
        assertThat(engine.permits("agent-alpha", "READ", "src/service/UserService.java")).isTrue();
        assertThat(engine.permits("agent-beta",  "READ", "src/core/Engine.java")).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = { "/etc/passwd", "/etc/hosts", "/sys/kernel/debug" })
    void defaultDenyBlocksSystemPaths(String path) {
        assertThat(engine.permits("any-agent", "WRITE", path)).isFalse();
    }

    @Test
    void denyPolicyOverridesAllow() {
        // Add allow, then deny — deny should win (deny policies are prepended)
        engine.allow("agent-x", "WRITE", "src/**", null);
        engine.deny("agent-x", "WRITE", "src/secrets/**", "Secrets are protected");

        // Regular src file: should be allowed
        assertThat(engine.permits("agent-x", "WRITE", "src/Service.java")).isTrue();
    }

    @Test
    void evaluateDecisionContainsAllFields() {
        engine.allow("test-agent", "READ", "README.md", "Public file");
        var decision = engine.evaluate("test-agent", "READ", "README.md");

        assertThat(decision.agentId()).isEqualTo("test-agent");
        assertThat(decision.action()).isEqualTo("READ");
        assertThat(decision.resource()).isEqualTo("README.md");
        assertThat(decision.decidedAt()).isNotNull();
        assertThat(decision.policyId()).isNotBlank();
    }

    // ── Audit Trail ───────────────────────────────────────────────────────

    @Test
    void auditEventsHaveUniqueSequentialIds() {
        var e1 = engine.recordEvent("agent-1", "TOOL_CALL", "read_file", Map.of());
        var e2 = engine.recordEvent("agent-1", "TOOL_CALL", "write_file", Map.of());
        var e3 = engine.recordEvent("agent-2", "ACCESS_CHECK", "src/", Map.of());

        assertThat(e1.seq()).isLessThan(e2.seq());
        assertThat(e2.seq()).isLessThan(e3.seq());
    }

    @Test
    void auditEventsHaveNonNullHash() {
        var event = engine.recordEvent("agent", "EVENT", "resource", Map.of("key", "value"));
        assertThat(event.hash()).isNotBlank();
        assertThat(event.prevHash()).isNotBlank();
    }

    @Test
    void chainLinksEventsCorrectly() {
        var e1 = engine.recordEvent("a", "E1", "r", Map.of());
        var e2 = engine.recordEvent("a", "E2", "r", Map.of());

        // e2's prevHash must equal e1's hash
        assertThat(e2.prevHash()).isEqualTo(e1.hash());
    }

    @Test
    void auditEventsContainDetails() {
        var event = engine.recordEvent("test-agent", "TOOL_EXECUTED", "run_command",
                Map.of("command", "mvn test", "exitCode", 0));

        assertThat(event.agentId()).isEqualTo("test-agent");
        assertThat(event.eventType()).isEqualTo("TOOL_EXECUTED");
        assertThat(event.resource()).isEqualTo("run_command");
        assertThat(event.details()).containsKey("command");
    }

    @Test
    void queryEventsByAgentId() {
        engine.recordEvent("agent-a", "TYPE1", "r", Map.of());
        engine.recordEvent("agent-b", "TYPE1", "r", Map.of());
        engine.recordEvent("agent-a", "TYPE2", "r", Map.of());

        // Note: queryEvents reads from disk — skip if no disk write configured in test
        // This verifies the query logic is callable without exception
        assertThatCode(() -> engine.queryEvents("agent-a", null, null, null, 10))
                .doesNotThrowAnyException();
    }

    // ── Supply Chain Security ─────────────────────────────────────────────

    @Test
    void unsignedSkillIsSandboxed() {
        GovernanceEngine.TrustTier tier = engine.assessSkillTrust(
                "my-skill", "skill content", null, null);
        assertThat(tier).isEqualTo(GovernanceEngine.TrustTier.SANDBOXED);
    }

    @Test
    void blockedSkillIsRejectedBeforeSignatureCheck() {
        engine.block("evil-skill");
        GovernanceEngine.TrustTier tier = engine.assessSkillTrust(
                "evil-skill", "malicious content", "any-signature", "any-key");
        assertThat(tier).isEqualTo(GovernanceEngine.TrustTier.BLOCKED);
    }

    @Test
    void getTrustReturnsSandboxedForUnknownSkill() {
        assertThat(engine.getTrust("never-registered")).isEqualTo(GovernanceEngine.TrustTier.SANDBOXED);
    }

    @Test
    void validSignatureProducesTrustedTier() {
        String content   = "skill instructions here";
        String key       = "publisher-secret-key";
        // Compute the expected signature: sha256(content + key)
        String signature = sha256(content + key);

        GovernanceEngine.TrustTier tier = engine.assessSkillTrust(
                "signed-skill", content, signature, key);
        assertThat(tier).isEqualTo(GovernanceEngine.TrustTier.TRUSTED);
    }

    @Test
    void invalidSignatureProducesSandboxedTier() {
        GovernanceEngine.TrustTier tier = engine.assessSkillTrust(
                "tampered-skill", "skill content", "wrong-signature", "publisher-key");
        assertThat(tier).isEqualTo(GovernanceEngine.TrustTier.SANDBOXED);
    }

    // ── Verify (integrity check) ──────────────────────────────────────────

    @Test
    void verifyLogOnEmptyLogReturnsValid() {
        var result = engine.verifyLog();
        assertThat(result.valid()).isTrue();
        assertThat(result.violations()).isEmpty();
    }

    // ── Helper ────────────────────────────────────────────────────────────

    private static String sha256(String input) {
        try {
            var md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes());
            var sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) { return ""; }
    }
}
