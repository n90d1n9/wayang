package tech.kayys.wayang.agent.core.skills.adapters;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.spi.skills.SkillResult;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SkillResultPayloadsContractTest {

    @Test
    void mapsSuccessfulSkillResultIntoImmutableAdapterData() {
        SkillResult result = SkillResult.builder()
                .skillId("summarize")
                .status(SkillResult.Status.SUCCESS)
                .observation("Short answer")
                .output("tokens", 12)
                .build();

        Map<String, Object> data = SkillResultPayloads.resultData(result, SkillResultPayloads.KEY_OBSERVATION);

        assertEquals("Short answer", data.get(SkillResultPayloads.KEY_OBSERVATION));
        assertEquals("SUCCESS", data.get(SkillResultPayloads.KEY_STATUS));
        assertEquals(Map.of("tokens", 12), data.get(SkillResultPayloads.KEY_OUTPUTS));
        assertThrows(UnsupportedOperationException.class, () -> data.put("later", true));
    }

    @Test
    void normalizesMissingStatusAndFailureMessages() {
        SkillResult missingStatus = SkillResult.builder()
                .skillId("missing-status")
                .status(null)
                .build();
        SkillResult errorOnly = SkillResult.builder()
                .skillId("error-only")
                .status(SkillResult.Status.ERROR)
                .error(new IllegalStateException("Registry unavailable"))
                .build();

        assertEquals(SkillResultPayloads.STATUS_UNKNOWN, SkillResultPayloads.statusName(missingStatus));
        assertEquals(SkillResultPayloads.ERROR_NO_RESULT, SkillResultPayloads.failureMessage(null));
        assertEquals("Registry unavailable", SkillResultPayloads.failureMessage(errorOnly));
        assertEquals(SkillResultPayloads.ERROR_EXECUTION_FAILED, SkillResultPayloads.errorMessage(null));
    }
}
