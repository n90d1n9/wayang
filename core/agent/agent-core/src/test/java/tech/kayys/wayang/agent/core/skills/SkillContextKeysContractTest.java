package tech.kayys.wayang.agent.core.skills;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.spi.skills.SkillContext;
import tech.kayys.wayang.agent.spi.skills.SkillContextKeys;
import tech.kayys.wayang.agent.spi.skills.SkillMetadata;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillContextKeysContractTest {

    @Test
    void exposesTypedMetadataLookupForSkillContexts() {
        SkillMetadata metadata = SkillMetadata.builder()
                .name("demo")
                .description("Demo skill")
                .build();
        SkillContext context = SkillContext.builder()
                .skillId("demo")
                .agentContext(Map.of(SkillContextKeys.KEY_METADATA, metadata))
                .build();

        assertSame(metadata, context.metadata());
        assertSame(metadata, SkillContextKeys.metadata(context.agentContext()).orElseThrow());
        assertTrue(SkillContextKeys.metadata(Map.of(SkillContextKeys.KEY_METADATA, "wrong-shape")).isEmpty());
    }

    @Test
    void normalizesSkillIdentityAndScopedMemoryKeys() {
        assertEquals("demo", SkillContextKeys.normalizedSkillId(" demo "));
        assertEquals(SkillContextKeys.UNKNOWN_SKILL_ID, SkillContextKeys.normalizedSkillId(" "));
        assertEquals("last_result_demo", SkillContextKeys.scopedMemoryKey(
                SkillContextKeys.MEMORY_LAST_RESULT,
                " demo "));
        assertEquals("metrics_unknown", SkillContextKeys.scopedMemoryKey(
                SkillContextKeys.MEMORY_METRICS,
                null));
        assertThrows(IllegalArgumentException.class, () -> SkillContextKeys.scopedMemoryKey(" ", "demo"));
    }

    @Test
    void keepsPromptAndWireKeysDistinctButCentralized() {
        assertEquals("skillId", SkillContextKeys.KEY_SKILL_ID);
        assertEquals("skill_id", SkillContextKeys.WIRE_SKILL_ID);
        assertEquals("skillTags", SkillContextKeys.KEY_SKILL_TAGS);
        assertEquals("skill_tags", SkillContextKeys.WIRE_SKILL_TAGS);
        assertEquals("feedback_id", SkillContextKeys.HITL_FEEDBACK_ID);
        assertEquals("documents", SkillContextKeys.RAG_DOCUMENTS);
    }
}
