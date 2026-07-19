package tech.kayys.wayang.guardrails.detector;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.guardrails.plugin.api.*;

import java.util.*;

@ApplicationScoped
public class BiasDetector implements GuardrailDetectorPlugin {

    @Override
    public String id() {
        return "bias-detector";
    }

    @Override
    public String name() {
        return "Bias Detector";
    }

    @Override
    public String version() {
        return "1.0.0";
    }

    @Override
    public String description() {
        return "Detects bias in text";
    }

    private static final Map<String, List<String>> GENDER_STEREOTYPES = Map.of(
            "female", List.of("nurturing", "emotional", "weak", "kitchen", "motherly"),
            "male", List.of("strong", "leader", "assertive", "provider", "dominant"));

    private static final List<String> RACIAL_BIAS_INDICATORS = List.of(
            "all", "always", "never", "typical", "those people", "they always");

    @Override
    public CheckPhase[] applicablePhases() {
        return new CheckPhase[] { CheckPhase.POST_EXECUTION };
    }

    @Override
    public String getCategory() {
        return "bias";
    }

    @Override
    public DetectionSeverity getSeverity() {
        return DetectionSeverity.WARN;
    }

    @Override
    public Uni<DetectionResult> detect(String text, Map<String, Object> metadata) {
        if (text == null || text.trim().isEmpty()) {
            return Uni.createFrom().item(DetectionResult.safe("bias-local", "BIAS"));
        }

        return Uni.createFrom().item(() -> {
            String lowerText = text.toLowerCase();

            List<String> genderBiases = detectGenderBias(lowerText);
            List<String> racialBiases = detectRacialBias(lowerText);
            boolean hasAgeBias = detectAgeBias(lowerText);

            List<Finding> findings = new ArrayList<>();
            genderBiases.forEach(b -> findings
                    .add(new Finding("GENDER_BIAS", b, lowerText.indexOf(b), lowerText.indexOf(b) + b.length(), 1.0)));
            racialBiases.forEach(b -> findings
                    .add(new Finding("RACIAL_BIAS", b, lowerText.indexOf(b), lowerText.indexOf(b) + b.length(), 1.0)));
            if (hasAgeBias) {
                findings.add(new Finding("AGE_BIAS", "age-related term", 0, text.length(), 1.0));
            }

            if (findings.isEmpty()) {
                return DetectionResult.safe("bias-local", getCategory());
            }

            double biasScore = calculateBiasScore(genderBiases, racialBiases, hasAgeBias);

            if (biasScore > 0.7) {
                return DetectionResult.blocked("bias-local", getCategory(), "Significant bias detected", findings);
            }

            return DetectionResult.warning("bias-local", getCategory(), "Potential bias detected", findings);
        });
    }

    private List<String> detectGenderBias(String text) {
        List<String> biases = new ArrayList<>();

        GENDER_STEREOTYPES.forEach((gender, stereotypes) -> {
            stereotypes.forEach(stereotype -> {
                if (text.contains(stereotype)) {
                    biases.add(stereotype);
                }
            });
        });

        return biases;
    }

    private List<String> detectRacialBias(String text) {
        return RACIAL_BIAS_INDICATORS.stream()
                .filter(text::contains)
                .toList();
    }

    private boolean detectAgeBias(String text) {
        List<String> ageTerms = List.of("old", "young", "elderly", "millennial", "boomer");
        List<String> negativeTerms = List.of("stubborn", "entitled", "outdated", "immature");

        return ageTerms.stream().anyMatch(text::contains) &&
                negativeTerms.stream().anyMatch(text::contains);
    }

    private double calculateBiasScore(List<String> genderBiases, List<String> racialBiases, boolean hasAgeBias) {
        double score = 0.0;
        score += genderBiases.size() * 0.2;
        score += racialBiases.size() * 0.3;
        score += hasAgeBias ? 0.4 : 0.0;

        return Math.min(score, 1.0);
    }
}