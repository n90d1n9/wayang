package tech.kayys.wayang.guardrails.detector.impl;

import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.guardrails.plugin.api.DetectionResult;
import tech.kayys.wayang.guardrails.plugin.api.DetectionSeverity;
import tech.kayys.wayang.guardrails.plugin.api.GuardrailDetectorPlugin;
import tech.kayys.wayang.guardrails.plugin.api.CheckPhase;

import tech.kayys.wayang.guardrails.plugin.api.Finding;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.*;
import java.util.regex.*;

/**
 * PII (Personally Identifiable Information) Detector Plugin implementation.
 * Detects various types of PII in text content.
 */
@ApplicationScoped
public class PIIDetectorPlugin implements GuardrailDetectorPlugin {

    private static final Map<String, Pattern> PII_PATTERNS = Map.of(
            "SSN", Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b"),
            "CREDIT_CARD", Pattern.compile("\\b\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}\\b"),
            "EMAIL", Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b"),
            "PHONE", Pattern.compile("\\b\\d{3}[-.]?\\d{3}[-.]?\\d{4}\\b"),
            "IP_ADDRESS", Pattern.compile("\\b\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\b"));

    @Override
    public String id() {
        return "pii-detector-plugin";
    }

    @Override
    public String name() {
        return "PII Detector Plugin";
    }

    @Override
    public String version() {
        return "1.0.0";
    }

    @Override
    public String description() {
        return "Detects personally identifiable information in text content";
    }

    @Override
    public CheckPhase[] applicablePhases() {
        return new CheckPhase[] { CheckPhase.PRE_EXECUTION, CheckPhase.POST_EXECUTION };
    }

    @Override
    public Uni<DetectionResult> detect(String text, Map<String, Object> metadata) {
        if (text == null || text.trim().isEmpty()) {
            return Uni.createFrom().item(DetectionResult.safe("pii-detector", getCategory()));
        }

        List<Finding> findings = new ArrayList<>();

        for (Map.Entry<String, Pattern> entry : PII_PATTERNS.entrySet()) {
            Matcher matcher = entry.getValue().matcher(text);

            while (matcher.find()) {
                findings.add(new Finding(
                        entry.getKey(),
                        matcher.group(),
                        matcher.start(),
                        matcher.end(),
                        1.0));
            }
        }

        if (findings.isEmpty()) {
            return Uni.createFrom().item(DetectionResult.safe("pii-detector", getCategory()));
        }

        boolean hasHighRiskPII = findings.stream()
                .anyMatch(f -> f.type().equals("SSN") || f.type().equals("CREDIT_CARD"));

        if (hasHighRiskPII) {
            return Uni.createFrom().item(DetectionResult.blocked("pii-detector", "PII", "High-risk PII detected"));
        }

        return Uni.createFrom().item(
                DetectionResult.warning("pii-detector", getCategory(), "PII detected", findings));
    }

    @Override
    public String getCategory() {
        return "pii";
    }

    @Override
    public DetectionSeverity getSeverity() {
        return DetectionSeverity.HIGH;
    }
}