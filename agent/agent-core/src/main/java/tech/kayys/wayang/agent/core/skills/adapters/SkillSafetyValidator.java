package tech.kayys.wayang.agent.core.skills.adapters;

import tech.kayys.wayang.agent.spi.skills.SkillContext;
import tech.kayys.wayang.agent.spi.skills.SkillResult;
import io.smallrye.mutiny.Uni;

/**
 * Guardrails-aware skill executor.
 * 
 * Wraps skill execution with safety validation, ensuring skills comply with
 * configured guardrails before, during, and after execution.
 */
public class SkillSafetyValidator {

    private final SkillContext context;
    private final java.util.List<SafetyRule> safetyRules;

    public SkillSafetyValidator(SkillContext context) {
        this.context = context;
        this.safetyRules = new java.util.ArrayList<>();
    }

    /**
     * Add a safety rule to validate before execution.
     */
    public SkillSafetyValidator addSafetyRule(SafetyRule rule) {
        safetyRules.add(rule);
        return this;
    }

    /**
     * Validate skill can execute (pre-flight checks).
     */
    public Uni<Boolean> validatePreExecution() {
        return Uni.combine().all()
            .unis(safetyRules.stream()
                .map(rule -> rule.validate(context))
                .collect(java.util.stream.Collectors.toList()))
            .asList()
            .map(results -> results.stream().allMatch(r -> (Boolean) r));
    }

    /**
     * Validate skill result is safe (post-execution checks).
     */
    public Uni<Boolean> validatePostExecution(SkillResult result) {
        return Uni.combine().all()
            .unis(safetyRules.stream()
                .map(rule -> rule.validateResult(result))
                .collect(java.util.stream.Collectors.toList()))
            .asList()
            .map(results -> results.stream().allMatch(r -> (Boolean) r));
    }

    /**
     * Get safety validation error if any.
     */
    public Uni<java.util.Optional<String>> getValidationError() {
        return validatePreExecution()
            .map(valid -> valid 
                ? java.util.Optional.empty()
                : java.util.Optional.of("Safety validation failed")
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
            return Uni.createFrom().item(!result.observation().isBlank());
        }
    }
}
