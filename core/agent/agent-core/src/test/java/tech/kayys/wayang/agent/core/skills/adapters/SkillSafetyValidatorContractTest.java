package tech.kayys.wayang.agent.core.skills.adapters;

import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.core.skills.support.TestSkillContexts;
import tech.kayys.wayang.agent.spi.skills.SkillContext;
import tech.kayys.wayang.agent.spi.skills.SkillResult;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillSafetyValidatorContractTest {

    @Test
    void passesPreExecutionWhenNoRulesAreRegistered() {
        SkillSafetyValidator validator = validator();

        assertTrue(validator.validatePreExecution().await().indefinitely());
        assertTrue(validator.getValidationError().await().indefinitely().isEmpty());
    }

    @Test
    void validatesPostExecutionObservationEvenWithoutRules() {
        SkillSafetyValidator validator = validator();

        assertTrue(validator.validatePostExecution(success("safe output")).await().indefinitely());
        assertFalse(validator.validatePostExecution(success(" ")).await().indefinitely());
        assertFalse(validator.validatePostExecution(null).await().indefinitely());
    }

    @Test
    void chainsRulesAndRequiresAllPreExecutionRulesToPass() {
        SkillSafetyValidator validator = validator()
                .addSafetyRule(rule(true))
                .addSafetyRule(rule(false));

        assertFalse(validator.validatePreExecution().await().indefinitely());
        Optional<String> error = validator.getValidationError().await().indefinitely();
        assertEquals("Safety validation failed", error.orElseThrow());
    }

    @Test
    void chainsRulesAndRequiresAllPostExecutionRulesToPass() {
        SkillSafetyValidator validator = validator()
                .addSafetyRule(rule(true))
                .addSafetyRule(new SkillSafetyValidator.BaseSafetyRule() {
                    @Override
                    public Uni<Boolean> validate(SkillContext context) {
                        return Uni.createFrom().item(true);
                    }

                    @Override
                    public Uni<Boolean> validateResult(SkillResult result) {
                        return Uni.createFrom().item(false);
                    }
                });

        assertFalse(validator.validatePostExecution(success("safe output")).await().indefinitely());
    }

    @Test
    void failsClosedWhenRulesReturnNullOrThrow() {
        SkillSafetyValidator nullRuleValidator = validator()
                .addSafetyRule(new SkillSafetyValidator.BaseSafetyRule() {
                    @Override
                    public Uni<Boolean> validate(SkillContext context) {
                        return Uni.createFrom().nullItem();
                    }
                });
        SkillSafetyValidator throwingValidator = validator()
                .addSafetyRule(new SkillSafetyValidator.BaseSafetyRule() {
                    @Override
                    public Uni<Boolean> validate(SkillContext context) {
                        throw new IllegalStateException("boom");
                    }
                });

        assertFalse(nullRuleValidator.validatePreExecution().await().indefinitely());
        assertFalse(throwingValidator.validatePreExecution().await().indefinitely());
    }

    @Test
    void ignoresNullRulesAndKeepsFluentApi() {
        SkillSafetyValidator validator = validator();
        AtomicInteger calls = new AtomicInteger();

        SkillSafetyValidator returned = validator
                .addSafetyRule(null)
                .addSafetyRule(new SkillSafetyValidator.BaseSafetyRule() {
                    @Override
                    public Uni<Boolean> validate(SkillContext context) {
                        calls.incrementAndGet();
                        return Uni.createFrom().item(true);
                    }
                });

        assertSame(validator, returned);
        assertTrue(validator.validatePreExecution().await().indefinitely());
        assertEquals(1, calls.get());
    }

    @Test
    void requiresContext() {
        assertThrows(NullPointerException.class, () -> new SkillSafetyValidator(null));
    }

    private static SkillSafetyValidator validator() {
        return new SkillSafetyValidator(TestSkillContexts.context("safe-skill", null));
    }

    private static SkillResult success(String observation) {
        return SkillResult.builder()
                .skillId("safe-skill")
                .status(SkillResult.Status.SUCCESS)
                .observation(observation)
                .build();
    }

    private static SkillSafetyValidator.SafetyRule rule(boolean valid) {
        return new SkillSafetyValidator.BaseSafetyRule() {
            @Override
            public Uni<Boolean> validate(SkillContext context) {
                return Uni.createFrom().item(valid);
            }

            @Override
            public Uni<Boolean> validateResult(SkillResult result) {
                return Uni.createFrom().item(valid);
            }
        };
    }
}
