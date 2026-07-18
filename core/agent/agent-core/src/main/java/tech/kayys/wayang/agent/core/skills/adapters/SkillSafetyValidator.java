package tech.kayys.wayang.agent.core.skills.adapters;

import tech.kayys.wayang.agent.spi.skills.SkillContext;
import tech.kayys.wayang.agent.spi.skills.SkillResult;
import io.smallrye.mutiny.Uni;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Guardrails-aware skill executor.
 * 
 * Wraps skill execution with safety validation, ensuring skills comply with
 * configured guardrails before, during, and after execution.
 */
public class SkillSafetyValidator {

    private final SkillContext context;
    private final List<SafetyRule> safetyRules;

    public SkillSafetyValidator(SkillContext context) {
        this.context = Objects.requireNonNull(context, "context");
        this.safetyRules = new ArrayList<>();
    }

    /**
     * Add a safety rule to validate before execution.
     */
    public SkillSafetyValidator addSafetyRule(SafetyRule rule) {
        if (rule != null) {
            safetyRules.add(rule);
        }
        return this;
    }

    /**
     * Validate skill can execute (pre-flight checks).
     */
    public Uni<Boolean> validatePreExecution() {
        if (safetyRules.isEmpty()) {
            return Uni.createFrom().item(true);
        }
        return Uni.join()
            .all(safetyRules.stream()
                .map(this::validateRule)
                .collect(Collectors.toList()))
            .andCollectFailures()
            .map(SkillSafetyValidator::allValid);
    }

    /**
     * Validate skill result is safe (post-execution checks).
     */
    public Uni<Boolean> validatePostExecution(SkillResult result) {
        if (!hasObservation(result)) {
            return Uni.createFrom().item(false);
        }
        if (safetyRules.isEmpty()) {
            return Uni.createFrom().item(true);
        }
        return Uni.join()
            .all(safetyRules.stream()
                .map(rule -> validateRuleResult(rule, result))
                .collect(Collectors.toList()))
            .andCollectFailures()
            .map(SkillSafetyValidator::allValid);
    }

    /**
     * Get safety validation error if any.
     */
    public Uni<Optional<String>> getValidationError() {
        return validatePreExecution()
            .map(valid -> valid 
                ? Optional.empty()
                : Optional.of("Safety validation failed")
            );
    }

    /**
     * Safety rule definition.
     */
    public interface SafetyRule {
        Uni<Boolean> validate(SkillContext context);
        Uni<Boolean> validateResult(SkillResult result);
    }

    /**
     * Base safety rule implementation.
     */
    public static abstract class BaseSafetyRule implements SafetyRule {
        @Override
        public Uni<Boolean> validateResult(SkillResult result) {
            return Uni.createFrom().item(hasObservation(result));
        }
    }

    private static Uni<Boolean> safeValidate(Uni<Boolean> validation) {
        if (validation == null) {
            return Uni.createFrom().item(false);
        }
        return validation
                .onItem().ifNull().continueWith(false)
                .onFailure().recoverWithItem(false);
    }

    private Uni<Boolean> validateRule(SafetyRule rule) {
        try {
            return safeValidate(rule.validate(context));
        } catch (RuntimeException error) {
            return Uni.createFrom().item(false);
        }
    }

    private static Uni<Boolean> validateRuleResult(SafetyRule rule, SkillResult result) {
        try {
            return safeValidate(rule.validateResult(result));
        } catch (RuntimeException error) {
            return Uni.createFrom().item(false);
        }
    }

    private static boolean allValid(List<Boolean> results) {
        return results.stream().allMatch(Boolean.TRUE::equals);
    }

    private static boolean hasObservation(SkillResult result) {
        return result != null && result.observation() != null && !result.observation().isBlank();
    }
}
