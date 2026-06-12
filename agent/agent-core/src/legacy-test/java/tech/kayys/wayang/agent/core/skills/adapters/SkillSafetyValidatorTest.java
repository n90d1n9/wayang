package tech.kayys.wayang.agent.core.skills.adapters;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import tech.kayys.wayang.agent.spi.skills.SkillContext;
import tech.kayys.wayang.agent.spi.skills.SkillMetadata;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SkillSafetyValidator Tests")
class SkillSafetyValidatorTest {

    private SkillContext mockContext;
    private SkillSafetyValidator validator;

    @BeforeEach
    void setUp() {
        SkillMetadata mockMetadata = new SkillMetadata(
            "test-skill",
            "Test Skill",
            "Test skill for safety validation",
            "1.0.0",
            "test",
            List.of("test"),
            null
        );

        mockContext = new SkillContext() {
            @Override
            public String skillId() {
                return "test-skill";
            }

            @Override
            public String userId() {
                return "test-user";
            }

            @Override
            public SkillMetadata metadata() {
                return mockMetadata;
            }

            @Override
            public Map<String, Object> variables() {
                return Map.of();
            }

            @Override
            public long timeoutMs() {
                return 5000;
            }
        };

        validator = new SkillSafetyValidator(mockContext);
    }

    @Test
    @DisplayName("Should validate pre-execution successfully when no rules")
    void testValidatePreExecutionNoRules() {
        var validation = validator.validatePreExecution();
        
        assertNotNull(validation);
        Boolean result = validation.await().indefinitely();
        assertTrue(result);
    }

    @Test
    @DisplayName("Should validate post-execution successfully with valid result")
    void testValidatePostExecutionValid() {
        var result = new tech.kayys.wayang.agent.spi.skills.SkillResult(
            "test-skill",
            "invoc-123",
            tech.kayys.wayang.agent.spi.skills.SkillResult.Status.SUCCESS,
            "Valid output",
            true
        );

        var validation = validator.validatePostExecution(result);
        
        assertNotNull(validation);
        Boolean validationResult = validation.await().indefinitely();
        assertTrue(validationResult);
    }

    @Test
    @DisplayName("Should add safety rule via fluent API")
    void testAddSafetyRule() {
        SkillSafetyValidator.SafetyRule mockRule = new SkillSafetyValidator.BaseSafetyRule() {
            @Override
            public io.smallrye.mutiny.Uni<Boolean> validate(SkillContext context) {
                return io.smallrye.mutiny.Uni.createFrom().item(true);
            }
        };

        SkillSafetyValidator result = validator.addSafetyRule(mockRule);
        
        assertSame(validator, result);
    }

    @Test
    @DisplayName("Should get validation error when invalid")
    void testGetValidationError() {
        var errorOpt = validator.getValidationError().await().indefinitely();
        
        assertNotNull(errorOpt);
        // Initially should pass (no rules)
        assertTrue(errorOpt.isEmpty());
    }

    @Test
    @DisplayName("Should chain multiple rules")
    void testChainMultipleRules() {
        SkillSafetyValidator rule1 = validator.addSafetyRule(new SkillSafetyValidator.BaseSafetyRule() {
            @Override
            public io.smallrye.mutiny.Uni<Boolean> validate(SkillContext context) {
                return io.smallrye.mutiny.Uni.createFrom().item(true);
            }
        });

        SkillSafetyValidator rule2 = rule1.addSafetyRule(new SkillSafetyValidator.BaseSafetyRule() {
            @Override
            public io.smallrye.mutiny.Uni<Boolean> validate(SkillContext context) {
                return io.smallrye.mutiny.Uni.createFrom().item(true);
            }
        });

        assertSame(validator, rule1);
        assertSame(validator, rule2);
    }

    @Test
    @DisplayName("Should validate empty observation as invalid")
    void testValidateEmptyObservation() {
        var result = new tech.kayys.wayang.agent.spi.skills.SkillResult(
            "test-skill",
            "invoc-123",
            tech.kayys.wayang.agent.spi.skills.SkillResult.Status.SUCCESS,
            "",  // Empty observation
            true
        );

        var validation = validator.validatePostExecution(result);
        Boolean validationResult = validation.await().indefinitely();
        
        assertFalse(validationResult);
    }
}
