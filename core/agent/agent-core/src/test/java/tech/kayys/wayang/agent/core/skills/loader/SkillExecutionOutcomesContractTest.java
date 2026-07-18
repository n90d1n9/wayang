package tech.kayys.wayang.agent.core.skills.loader;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SkillExecutionOutcomesContractTest {

    @Test
    void createsImmutableReadOnlyOutcome() {
        SkillExecutionOutcome outcome = SkillExecutionOutcomes.failure(
                "demo",
                "partial output",
                17,
                "failed",
                SkillExecutionOutcomes.failureMetadata(SkillFailureType.EXECUTION_ERROR));

        assertEquals("demo", outcome.skillName());
        assertEquals("partial output", outcome.output());
        assertEquals(17, outcome.executionTimeMs());
        assertFalse(outcome.success());
        assertEquals("failed", outcome.error());
        assertEquals(SkillFailureType.EXECUTION_ERROR, SkillExecutionOutcomes.failureType(outcome).orElseThrow());
        assertThrows(UnsupportedOperationException.class, () -> outcome.metadata().put("later", true));
    }

    @Test
    void createsSkillNotLoadedOutcome() {
        SkillExecutionOutcome outcome = SkillExecutionOutcomes.skillNotLoaded("missing");

        assertFalse(outcome.success());
        assertEquals("missing", outcome.skillName());
        assertEquals(SkillFailureType.SKILL_NOT_LOADED, SkillExecutionOutcomes.failureType(outcome).orElseThrow());
        assertEquals("Skill not loaded in orchestrator context: missing", outcome.error());
    }

    @Test
    void preservesStableWireCodesForFailureTypes() {
        assertEquals("skill_not_found", SkillFailureType.SKILL_NOT_FOUND.code());
        assertEquals("skill_not_loaded", SkillFailureType.SKILL_NOT_LOADED.code());
        assertEquals("parameter_validation", SkillFailureType.PARAMETER_VALIDATION.code());
        assertEquals("invalid_input", SkillFailureType.INVALID_INPUT.code());
        assertEquals("skill_layout", SkillFailureType.SKILL_LAYOUT.code());
        assertEquals("process_exit", SkillFailureType.PROCESS_EXIT.code());
        assertEquals("timeout", SkillFailureType.TIMEOUT.code());
        assertEquals("execution_error", SkillFailureType.EXECUTION_ERROR.code());
        assertEquals(SkillFailureType.TIMEOUT, SkillFailureType.fromCode("timeout").orElseThrow());
    }
}
