package tech.kayys.wayang.agent.core.skills.loader;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillExecutionResultsContractTest {

    @Test
    void buildsImmutableSuccessAndFailureResults() {
        SkillExecutionOutcome success = SkillExecutionResults.started("unit")
                .success("ok", Map.of(SkillExecutionMetadata.KEY_EXIT_CODE, 0));

        assertTrue(success.success());
        assertEquals("unit", success.skillName());
        assertEquals("ok", success.output());
        assertEquals(0, SkillExecutionMetadata.exitCode(success).orElseThrow());
        assertThrows(UnsupportedOperationException.class, () -> success.metadata().put("later", true));

        SkillExecutionOutcome failure = SkillExecutionResults.started("missing").skillNotFound();

        assertFalse(failure.success());
        assertEquals("missing", failure.skillName());
        assertEquals(SkillFailureType.SKILL_NOT_FOUND, SkillExecutionOutcomes.failureType(failure).orElseThrow());
        assertTrue(failure.error().contains("Skill not found"));
    }

    @Test
    void failureMetadataKeepsDeclaredFailureTypeAuthoritative() {
        Map<String, Object> metadata = SkillExecutionOutcomes.failureMetadata(
                SkillFailureType.TIMEOUT,
                Map.of(
                        SkillExecutionOutcomes.KEY_FAILURE_TYPE, "wrong",
                        SkillExecutionMetadata.KEY_TIMEOUT_SECONDS, 3));

        assertEquals(SkillFailureType.TIMEOUT.code(), metadata.get(SkillExecutionOutcomes.KEY_FAILURE_TYPE));
        assertEquals(3, metadata.get(SkillExecutionMetadata.KEY_TIMEOUT_SECONDS));
    }
}
