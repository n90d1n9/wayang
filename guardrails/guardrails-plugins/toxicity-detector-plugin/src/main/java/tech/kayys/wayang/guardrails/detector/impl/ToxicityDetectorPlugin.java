package tech.kayys.wayang.guardrails.detector.impl;

import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.guardrails.plugin.api.DetectionResult;
import tech.kayys.wayang.guardrails.plugin.api.DetectionSeverity;
import tech.kayys.wayang.guardrails.plugin.api.Finding;
import tech.kayys.wayang.guardrails.plugin.api.GuardrailDetectorPlugin;
import tech.kayys.wayang.guardrails.plugin.api.CheckPhase;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.*;

/**
 * Toxicity Detector Plugin implementation.
 * Detects toxic or harmful content in text.
 */
@ApplicationScoped
public class ToxicityDetectorPlugin implements GuardrailDetectorPlugin {

    private static final Set<String> TOXIC_WORDS = Set.of(
            "hate", "kill", "murder", "attack", "violence", "abuse", "threat", "dangerous",
            "harmful", "destructive", "aggressive", "hostile", "offensive", "disgusting",
            "terrible", "awful", "horrible", "evil", "devil", "bastard", "bitch", "damn",
            "shit", "fuck", "asshole", "dick", "cunt", "slut", "whore", "rape", "rapist");

    private static final Set<String> HATE_SPEECH_PATTERNS = Set.of(
            "racist", "discriminat", "bigot", "prejudice", "scapegoat", "oppress", "suppress",
            "inferior", "superior", "supremacist", "nazi", "fascist", "extremist");

    @Override
    public String id() {
        return "toxicity-detector-plugin";
    }

    @Override
    public String name() {
        return "Toxicity Detector Plugin";
    }

    @Override
    public String version() {
        return "1.0.0";
    }

    @Override
    public String description() {
        return "Detects toxic or harmful content in text";
    }

    @Override
    public CheckPhase[] applicablePhases() {
        return new CheckPhase[] { CheckPhase.PRE_EXECUTION, CheckPhase.POST_EXECUTION };
    }

    @Override
    public Uni<DetectionResult> detect(String text, Map<String, Object> metadata) {
        if (text == null || text.trim().isEmpty()) {
            return Uni.createFrom().item(DetectionResult.safe("toxicity-detector", "TOXICITY"));
        }

        String lowerText = text.toLowerCase();
        int toxicityScore = calculateToxicityScore(lowerText);

        List<Finding> findings = new ArrayList<>();
        if (toxicityScore >= 8) {
            findings.add(new Finding("TOXICITY", "High toxicity", 0, text.length(), (double) toxicityScore));
            return Uni.createFrom().item(
                    DetectionResult.blocked("toxicity-detector", "TOXICITY", "High toxicity content detected",
                            findings));
        } else if (toxicityScore >= 5) {
            findings.add(new Finding("TOXICITY", "Moderate toxicity", 0, text.length(), (double) toxicityScore));
            return Uni.createFrom().item(
                    DetectionResult.warning("toxicity-detector", "TOXICITY", "Moderate toxicity detected", findings));
        } else if (toxicityScore > 0) {
            findings.add(new Finding("TOXICITY", "Low toxicity", 0, text.length(), (double) toxicityScore));
            return Uni.createFrom().item(new DetectionResult(
                    "toxicity-detector",
                    "TOXICITY",
                    false, // Not safe
                    "Low toxicity detected",
                    findings));
        }

        return Uni.createFrom().item(DetectionResult.safe("toxicity-detector", "TOXICITY"));
    }

    private int calculateToxicityScore(String text) {
        int score = 0;

        // Check for toxic words
        for (String toxicWord : TOXIC_WORDS) {
            if (text.contains(toxicWord)) {
                score += 1;
            }
        }

        // Check for hate speech patterns
        for (String pattern : HATE_SPEECH_PATTERNS) {
            if (text.contains(pattern)) {
                score += 2; // Higher weight for hate speech
            }
        }

        // Check for repeated exclamation marks or question marks which might indicate
        // aggression
        if (text.contains("!!!") || text.contains("???")) {
            score += 1;
        }

        return Math.min(score, 10); // Cap at 10
    }

    @Override
    public String getCategory() {
        return "toxicity";
    }

    @Override
    public DetectionSeverity getSeverity() {
        return DetectionSeverity.MEDIUM;
    }
}