package tech.kayys.wayang.guardrails.detector;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.guardrails.plugin.api.*;

import java.util.*;
import java.util.regex.*;

@ApplicationScoped
public class PIIDetector implements GuardrailDetectorPlugin {

    @Override
    public String id() {
        return "pii-detector";
    }

    @Override
    public String name() {
        return "PII Detector";
    }

    @Override
    public String version() {
        return "1.0.0";
    }

    @Override
    public String description() {
        return "Detects Personally Identifiable Information (PII)";
    }

    private static final Map<String, Pattern> PII_PATTERNS = Map.of(
            "SSN", Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b"),
            "CREDIT_CARD", Pattern.compile("\\b\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}\\b"),
            "EMAIL", Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b"),
            "PHONE", Pattern.compile("\\b\\d{3}[-.]?\\d{3}[-.]?\\d{4}\\b"),
            "IP_ADDRESS", Pattern.compile("\\b\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\b"));

    @Override
    public CheckPhase[] applicablePhases() {
        return new CheckPhase[] { CheckPhase.PRE_EXECUTION, CheckPhase.POST_EXECUTION };
    }

    @Override
    public String getCategory() {
        return "pii";
    }

    @Override
    public DetectionSeverity getSeverity() {
        return DetectionSeverity.BLOCK;
    }

    @Override
    public Uni<DetectionResult> detect(String text, Map<String, Object> metadata) {
        return Uni.createFrom().item(() -> {
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
                return DetectionResult.safe("pii-local", "PII");
            }

            boolean hasHighRiskPII = findings.stream()
                    .anyMatch(f -> f.type().equals("SSN") || f.type().equals("CREDIT_CARD"));

            if (hasHighRiskPII) {
                return DetectionResult.blocked("pii-local", "PII", "High-risk PII detected", findings);
            }

            return DetectionResult.warning("pii-local", "PII", "Potential PII detected", findings);
        });
    }

    // Keeping calculateConfidence and other helpers if needed for internal logic,
    // but the plugin interface is the primary entry point now.
}