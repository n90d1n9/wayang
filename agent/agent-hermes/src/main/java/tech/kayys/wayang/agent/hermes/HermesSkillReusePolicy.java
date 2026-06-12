package tech.kayys.wayang.agent.hermes;

import tech.kayys.wayang.agent.spi.skills.SkillDefinition;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Scores existing learned skills for reuse before creating a new one.
 */
public final class HermesSkillReusePolicy {

    private static final double DEFAULT_REUSE_THRESHOLD = 0.72;
    private static final Set<String> STOP_WORDS = Set.of(
            "and", "for", "from", "into", "that", "the", "this", "with",
            "hermes", "learned", "workflow");

    private final double reuseThreshold;

    public HermesSkillReusePolicy() {
        this(DEFAULT_REUSE_THRESHOLD);
    }

    public HermesSkillReusePolicy(double reuseThreshold) {
        if (reuseThreshold <= 0.0 || reuseThreshold > 1.0) {
            throw new IllegalArgumentException("reuseThreshold must be between 0.0 and 1.0");
        }
        this.reuseThreshold = reuseThreshold;
    }

    public Optional<HermesSkillReuseMatch> findReusable(
            SkillDefinition candidate,
            List<SkillDefinition> existingSkills) {
        if (candidate == null || existingSkills == null || existingSkills.isEmpty()) {
            return Optional.empty();
        }
        return existingSkills.stream()
                .filter(Objects::nonNull)
                .filter(existing -> !Objects.equals(candidate.id(), existing.id()))
                .map(existing -> match(candidate, existing))
                .filter(match -> match.score() >= reuseThreshold)
                .max(Comparator
                        .comparingDouble(HermesSkillReuseMatch::score)
                        .thenComparing(match -> match.skill().id()));
    }

    HermesSkillReuseMatch match(SkillDefinition candidate, SkillDefinition existing) {
        double taskScore = jaccard(taskTokens(candidate), taskTokens(existing));
        double toolScore = toolScore(candidate.tools(), existing.tools());
        double score = (taskScore * 0.8) + (toolScore * 0.2);
        return new HermesSkillReuseMatch(
                existing,
                Math.min(1.0, score),
                "task=" + rounded(taskScore) + ", tools=" + rounded(toolScore));
    }

    private static Set<String> taskTokens(SkillDefinition skill) {
        String task = metadataText(skill, "hermes.task");
        if (task.isBlank()) {
            task = HermesText.oneLine(clean(skill.name()) + " " + clean(skill.description()));
        }
        return tokens(task);
    }

    private static Set<String> tokens(String value) {
        if (value == null || value.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(value.toLowerCase(Locale.ROOT).split("[^a-z0-9]+"))
                .map(String::trim)
                .filter(token -> token.length() > 2)
                .filter(token -> !STOP_WORDS.contains(token))
                .map(HermesSkillReusePolicy::singular)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static double toolScore(List<String> candidateTools, List<String> existingTools) {
        Set<String> candidate = normalizedSet(candidateTools);
        Set<String> existing = normalizedSet(existingTools);
        if (candidate.isEmpty() && existing.isEmpty()) {
            return 1.0;
        }
        if (candidate.isEmpty() || existing.isEmpty()) {
            return 0.75;
        }
        return jaccard(candidate, existing);
    }

    private static Set<String> normalizedSet(List<String> values) {
        if (values == null) {
            return Set.of();
        }
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(value -> value.toLowerCase(Locale.ROOT).trim())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static double jaccard(Set<String> left, Set<String> right) {
        if (left.isEmpty() && right.isEmpty()) {
            return 1.0;
        }
        if (left.isEmpty() || right.isEmpty()) {
            return 0.0;
        }
        Set<String> intersection = new LinkedHashSet<>(left);
        intersection.retainAll(right);
        Set<String> union = new LinkedHashSet<>(left);
        union.addAll(right);
        return (double) intersection.size() / (double) union.size();
    }

    private static String metadataText(SkillDefinition skill, String key) {
        Object value = skill.metadata().get(key);
        return value == null ? "" : HermesText.oneLine(value.toString());
    }

    private static String singular(String token) {
        if (token.length() > 4 && token.endsWith("s")) {
            return token.substring(0, token.length() - 1);
        }
        return token;
    }

    private static String clean(String value) {
        return value == null ? "" : value;
    }

    private static String rounded(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }
}
