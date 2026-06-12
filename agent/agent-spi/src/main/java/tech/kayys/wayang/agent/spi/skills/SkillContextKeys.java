package tech.kayys.wayang.agent.spi.skills;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Stable context, memory, RAG, and HITL payload keys shared by skill adapters.
 */
public final class SkillContextKeys {

    public static final String KEY_METADATA = "metadata";
    public static final String KEY_PROMPT_TEMPLATE = "template";
    public static final String KEY_SKILL_DESCRIPTION = "skillDescription";
    public static final String KEY_SKILL_ID = "skillId";
    public static final String KEY_SKILL_NAME = "skillName";
    public static final String KEY_SKILL_TAGS = "skillTags";
    public static final String KEY_SKILL_VERSION = "skillVersion";

    public static final String WIRE_SKILL_ID = "skill_id";
    public static final String WIRE_SKILL_NAME = "skill_name";
    public static final String WIRE_SKILL_TAGS = "skill_tags";
    public static final String WIRE_SKILL_VERSION = "skill_version";

    public static final String RAG_DOCUMENTS = "documents";
    public static final String RAG_QUERY = "query";

    public static final String HITL_APPROVED = "approved";
    public static final String HITL_ERROR = "error";
    public static final String HITL_EXECUTION_ID = "execution_id";
    public static final String HITL_FEEDBACK_ID = "feedback_id";
    public static final String HITL_ORIGINAL_RESULT = "original_result";
    public static final String HITL_REFINED_RESULT = "refined_result";
    public static final String HITL_REQUEST_ID = "request_id";
    public static final String HITL_REVIEWER_INPUT = "reviewer_input";

    public static final String MEMORY_LAST_RESULT = "last_result";
    public static final String MEMORY_LAST_STATUS = "last_status";
    public static final String MEMORY_LAST_SUCCESS = "last_success";
    public static final String MEMORY_METRICS = "metrics";

    public static final String UNKNOWN_SKILL_ID = "unknown";

    private SkillContextKeys() {
    }

    public static Optional<SkillMetadata> metadata(Map<String, ?> context) {
        Object metadata = context == null ? null : context.get(KEY_METADATA);
        return metadata instanceof SkillMetadata skillMetadata
                ? Optional.of(skillMetadata)
                : Optional.empty();
    }

    public static String normalizedSkillId(String skillId) {
        return hasText(skillId) ? skillId.trim() : UNKNOWN_SKILL_ID;
    }

    public static String scopedMemoryKey(String prefix, String skillId) {
        Objects.requireNonNull(prefix, "prefix");
        if (!hasText(prefix)) {
            throw new IllegalArgumentException("Memory key prefix is required");
        }
        return prefix.trim() + "_" + normalizedSkillId(skillId);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
