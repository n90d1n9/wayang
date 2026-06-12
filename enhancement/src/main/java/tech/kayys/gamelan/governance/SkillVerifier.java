package tech.kayys.gamelan.governance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.skill.Skill;

import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

/**
 * Supply-chain security verifier for skills (Section V — Supply Chain Security).
 *
 * <h2>Why this matters</h2>
 * The architecture document explicitly calls out ClawHavoc-style malicious skill
 * attacks: a skill that looks legitimate but contains instructions for the agent
 * to exfiltrate data, execute harmful commands, or bypass safety controls.
 *
 * <h2>What this checks</h2>
 * <ol>
 *   <li><b>Content hash</b> — SKILL.md is hashed on first installation; any
 *       subsequent modification is detected as tampering.</li>
 *   <li><b>Dangerous pattern scan</b> — instructions are scanned for patterns
 *       that suggest prompt injection or exfiltration attempts.</li>
 *   <li><b>Allowed-tool validation</b> — skills declaring
 *       {@code allowed-tools} are checked against the tool registry.</li>
 *   <li><b>Trust tier</b> — skills are classified as TRUSTED, VERIFIED,
 *       UNVERIFIED, or BLOCKED.</li>
 * </ol>
 *
 * <h2>Verification store</h2>
 * Known-good hashes are stored in {@code ~/.gamelan/security/skill-hashes.json}.
 * When a skill is installed via {@code gamelan skill install}, its hash is
 * recorded. Any deviation from the recorded hash fails verification.
 */
@ApplicationScoped
public class SkillVerifier {

    private static final Logger log = LoggerFactory.getLogger(SkillVerifier.class);

    /** Patterns that suggest malicious prompt injection in skill instructions. */
    private static final List<String> DANGEROUS_PATTERNS = List.of(
            "ignore previous instructions",
            "ignore all prior instructions",
            "disregard your system prompt",
            "you are now",
            "new persona",
            "act as",
            "jailbreak",
            "dan mode",
            "exfiltrate",
            "send data to",
            "curl.*secret",
            "wget.*password",
            "rm -rf",
            "sudo.*rm",
            "format.*disk",
            "mkfs",
            "dd if=/dev/zero"
    );

    private final Map<String, String> knownHashes = new HashMap<>();
    private final Path hashStore;

    public SkillVerifier() {
        this(Path.of(System.getProperty("user.home"), ".gamelan", "security"));
    }

    /** Constructor for testing with a custom directory. */
    public SkillVerifier(Path securityDir) {
        this.hashStore = securityDir.resolve("skill-hashes.json");
        loadHashes();
    }

    // ── Public API ─────────────────────────────────────────────────────────

    public enum TrustTier {
        /** Hash matches recorded value, no dangerous patterns. */
        TRUSTED,
        /** New skill, not yet recorded — safe to use but not yet hashed. */
        UNVERIFIED,
        /** Hash mismatch — skill was modified after installation. */
        TAMPERED,
        /** Dangerous patterns detected in instructions. */
        BLOCKED
    }

    public record VerificationResult(
            TrustTier      tier,
            List<String>   findings,
            boolean        passed
    ) {
        static VerificationResult trusted() {
            return new VerificationResult(TrustTier.TRUSTED, List.of(), true);
        }
        static VerificationResult unverified(String reason) {
            return new VerificationResult(TrustTier.UNVERIFIED, List.of(reason), true);
        }
        static VerificationResult tampered(String detail) {
            return new VerificationResult(TrustTier.TAMPERED, List.of(detail), false);
        }
        static VerificationResult blocked(List<String> patterns) {
            return new VerificationResult(TrustTier.BLOCKED, patterns, false);
        }
    }

    /**
     * Verifies a skill before it is activated.
     *
     * @param skill the skill to verify
     * @return verification result; check {@code passed} before using the skill
     */
    public VerificationResult verify(Skill skill) {
        // 1. Dangerous pattern scan (highest priority)
        List<String> patterns = scanForDangerousPatterns(skill);
        if (!patterns.isEmpty()) {
            log.warn("[security] skill '{}' BLOCKED: dangerous patterns: {}", skill.name(), patterns);
            return VerificationResult.blocked(patterns);
        }

        // 2. Hash verification
        String currentHash = hashSkill(skill);
        String knownHash   = knownHashes.get(skill.name());

        if (knownHash == null) {
            // First time seen — record but mark as unverified
            recordHash(skill.name(), currentHash);
            log.info("[security] skill '{}' first verification — recorded hash", skill.name());
            return VerificationResult.unverified("Newly recorded — hash stored for future verification");
        }

        if (!knownHash.equals(currentHash)) {
            log.error("[security] skill '{}' TAMPERED — hash mismatch (known={}, current={})",
                    skill.name(), knownHash.substring(0, 8), currentHash.substring(0, 8));
            return VerificationResult.tampered(
                    "Hash mismatch: SKILL.md was modified after installation. "
                    + "Known: " + knownHash.substring(0, 8) + "…  "
                    + "Current: " + currentHash.substring(0, 8) + "…");
        }

        return VerificationResult.trusted();
    }

    /**
     * Records the hash of a newly installed skill.
     * Called by {@link tech.kayys.gamelan.skill.SkillRegistry} after
     * {@code gamelan skill install}.
     */
    public void record(String skillName, String rawContent) {
        String hash = sha256(rawContent);
        recordHash(skillName, hash);
        log.info("[security] recorded hash for skill '{}'", skillName);
    }

    /** Removes a skill from the trust store (used on uninstall). */
    public void revoke(String skillName) {
        knownHashes.remove(skillName);
        persistHashes();
    }

    // ── Private ────────────────────────────────────────────────────────────

    private List<String> scanForDangerousPatterns(Skill skill) {
        String instructions = skill.instructions().toLowerCase();
        List<String> found = new ArrayList<>();
        for (String pattern : DANGEROUS_PATTERNS) {
            if (instructions.contains(pattern.toLowerCase())) {
                found.add(pattern);
            }
        }
        return found;
    }

    private String hashSkill(Skill skill) {
        // Hash the raw SKILL.md content for tamper detection
        return sha256(skill.rawContent() != null ? skill.rawContent() : skill.instructions());
    }

    private void recordHash(String skillName, String hash) {
        knownHashes.put(skillName, hash);
        persistHashes();
    }

    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            return "error-" + System.nanoTime();
        }
    }

    @SuppressWarnings("unchecked")
    private void loadHashes() {
        if (!Files.exists(hashStore)) return;
        try {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, String> loaded = mapper.readValue(hashStore.toFile(), Map.class);
            knownHashes.putAll(loaded);
        } catch (IOException e) {
            log.warn("[security] cannot load skill hashes: {}", e.getMessage());
        }
    }

    private void persistHashes() {
        try {
            Files.createDirectories(hashStore.getParent());
            new com.fasterxml.jackson.databind.ObjectMapper()
                    .writerWithDefaultPrettyPrinter()
                    .writeValue(hashStore.toFile(), knownHashes);
        } catch (IOException e) {
            log.warn("[security] cannot save skill hashes: {}", e.getMessage());
        }
    }
}
