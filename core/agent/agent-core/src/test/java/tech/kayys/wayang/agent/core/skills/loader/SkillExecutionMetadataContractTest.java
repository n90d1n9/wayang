package tech.kayys.wayang.agent.core.skills.loader;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class SkillExecutionMetadataContractTest {

    @Test
    void readsStableTypedMetadataFieldsFromOutcomes() {
        SkillExecutionOutcome outcome = SkillExecutionOutcomes.failure(
                "unit",
                "partial",
                12,
                "failed",
                SkillExecutionOutcomes.failureMetadata(SkillFailureType.PROCESS_EXIT, Map.of(
                        SkillExecutionMetadata.KEY_ERROR_COUNT, 2,
                        SkillExecutionMetadata.KEY_EXCEPTION_TYPE, "java.lang.IllegalStateException",
                        SkillExecutionMetadata.KEY_EXIT_CODE, 7,
                        SkillExecutionMetadata.KEY_LAYOUT_ERROR, "missing_executable",
                        SkillExecutionMetadata.KEY_OUTPUT_CHARS, 42L,
                        SkillExecutionMetadata.KEY_OUTPUT_TRUNCATED, true,
                        SkillExecutionMetadata.KEY_TIMEOUT_SECONDS, 3)));

        assertEquals(2, SkillExecutionMetadata.errorCount(outcome).orElseThrow());
        assertEquals("java.lang.IllegalStateException", SkillExecutionMetadata.exceptionType(outcome).orElseThrow());
        assertEquals(7, SkillExecutionMetadata.exitCode(outcome).orElseThrow());
        assertEquals("missing_executable", SkillExecutionMetadata.layoutError(outcome).orElseThrow());
        assertEquals(42L, SkillExecutionMetadata.outputChars(outcome).orElseThrow());
        assertEquals(true, SkillExecutionMetadata.outputTruncated(outcome).orElseThrow());
        assertEquals(3, SkillExecutionMetadata.timeoutSeconds(outcome).orElseThrow());
    }

    @Test
    void ignoresMissingOrUnexpectedMetadataShapes() {
        SkillExecutionOutcome outcome = SkillExecutionOutcomes.success(
                "unit",
                "ok",
                1,
                Map.of(SkillExecutionMetadata.KEY_EXIT_CODE, "zero"));

        assertFalse(SkillExecutionMetadata.errorCount(null).isPresent());
        assertFalse(SkillExecutionMetadata.exitCode(outcome).isPresent());
        assertFalse(SkillExecutionMetadata.layoutError(outcome).isPresent());
        assertFalse(SkillExecutionMetadata.outputTruncated(outcome).isPresent());
    }
}
