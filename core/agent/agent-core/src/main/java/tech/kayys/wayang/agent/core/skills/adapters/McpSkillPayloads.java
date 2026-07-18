package tech.kayys.wayang.agent.core.skills.adapters;

import tech.kayys.wayang.agent.spi.skills.SkillDefinition;
import tech.kayys.wayang.agent.spi.skills.SkillContextKeys;
import tech.kayys.wayang.agent.spi.skills.SkillMetadataKeys;
import tech.kayys.wayang.agent.spi.skills.SkillResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Maps skill definitions and execution results into MCP-facing payloads.
 */
final class McpSkillPayloads {

    static final String KEY_ENDPOINT = "endpoint";
    static final String KEY_ERROR = "error";
    static final String KEY_OUTPUTS = SkillResultPayloads.KEY_OUTPUTS;
    static final String KEY_PROTOCOL_VERSION = "protocol_version";
    static final String KEY_RESULT = "result";
    static final String KEY_SKILL_ID = SkillContextKeys.WIRE_SKILL_ID;
    static final String KEY_STATUS = SkillResultPayloads.KEY_STATUS;
    static final String KEY_SUCCESS = "success";
    static final String STATUS_UNKNOWN = SkillResultPayloads.STATUS_UNKNOWN;

    private McpSkillPayloads() {
    }

    static MCPSkillProvider.MCPSkillResource resource(SkillDefinition skill) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put(SkillMetadataKeys.KEY_TAGS, SkillMetadataKeys.tags(skill.metadata()));
        metadata.put(
                SkillMetadataKeys.KEY_CATEGORY,
                SkillMetadataKeys.category(skill.metadata(), skill.category() == null ? "" : skill.category()));
        return new MCPSkillProvider.MCPSkillResource(
                skill.id(),
                skill.name() == null ? skill.id() : skill.name(),
                skill.description() == null ? "" : skill.description(),
                SkillMetadataKeys.version(skill.metadata()),
                Map.copyOf(metadata));
    }

    static Map<String, Object> executionResult(String skillId, SkillResult result) {
        if (result == null) {
            return error(skillId, new IllegalStateException(SkillResultPayloads.ERROR_NO_RESULT));
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put(KEY_SKILL_ID, skillId);
        response.put(KEY_SUCCESS, result.success());
        SkillResultPayloads.putResultData(response, result, KEY_RESULT);
        if (result.error() != null) {
            response.put(KEY_ERROR, SkillResultPayloads.errorMessage(result.error()));
        }
        return Map.copyOf(response);
    }

    static Map<String, Object> error(String skillId, Throwable error) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put(KEY_SKILL_ID, skillId);
        response.put(KEY_SUCCESS, false);
        response.put(KEY_STATUS, SkillResult.Status.ERROR.name());
        response.put(KEY_ERROR, SkillResultPayloads.errorMessage(error));
        return Map.copyOf(response);
    }
}
