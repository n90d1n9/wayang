package tech.kayys.wayang.agent.skills.management;

import tech.kayys.wayang.agent.spi.skills.SkillDefinition;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

/**
 * Shared properties codec for lightweight skill persistence backends.
 */
final class SkillDefinitionPropertiesCodec {

    static final String EXTENSION = ".properties";

    private static final String KEY_CATEGORY = "category";
    private static final String KEY_DEFAULT_PROVIDER = "defaultProvider";
    private static final String KEY_DESCRIPTION = "description";
    private static final String KEY_FALLBACK_PROVIDER = "fallbackProvider";
    private static final String KEY_ID = "id";
    private static final String KEY_MAX_TOKENS = "maxTokens";
    private static final String KEY_NAME = "name";
    private static final String KEY_ORCHESTRATION_CHILD_SKILLS = "orchestration.defaultChildSkills";
    private static final String KEY_ORCHESTRATION_MAX_DELEGATIONS = "orchestration.maxDelegations";
    private static final String KEY_ORCHESTRATION_MAX_ITERATIONS = "orchestration.maxIterations";
    private static final String KEY_ORCHESTRATION_STRATEGY = "orchestration.defaultStrategy";
    private static final String KEY_ORCHESTRATION_TYPE = "orchestration.defaultType";
    private static final String KEY_SYSTEM_PROMPT = "systemPrompt";
    private static final String KEY_TEMPERATURE = "temperature";
    private static final String KEY_TOOLS = "tools";
    private static final String KEY_USER_PROMPT_TEMPLATE = "userPromptTemplate";
    private static final String METADATA_PREFIX = "metadata.";
    private static final String SUB_SKILL_PROMPT_PREFIX = "subSkillPrompt.";

    Properties toProperties(SkillDefinition skill) {
        Objects.requireNonNull(skill, "skill");
        Properties properties = new Properties();
        SkillManagementPropertiesCodecSupport.putProperty(properties, KEY_ID, skill.id());
        SkillManagementPropertiesCodecSupport.putProperty(properties, KEY_NAME, skill.name());
        SkillManagementPropertiesCodecSupport.putProperty(properties, KEY_DESCRIPTION, skill.description());
        SkillManagementPropertiesCodecSupport.putProperty(properties, KEY_CATEGORY, skill.category());
        SkillManagementPropertiesCodecSupport.putProperty(properties, KEY_SYSTEM_PROMPT, skill.systemPrompt());
        SkillManagementPropertiesCodecSupport.putProperty(
                properties,
                KEY_USER_PROMPT_TEMPLATE,
                skill.userPromptTemplate());
        SkillManagementPropertiesCodecSupport.putProperty(properties, KEY_TEMPERATURE, skill.temperature());
        SkillManagementPropertiesCodecSupport.putProperty(properties, KEY_MAX_TOKENS, skill.maxTokens());
        SkillManagementPropertiesCodecSupport.putProperty(properties, KEY_DEFAULT_PROVIDER, skill.defaultProvider());
        SkillManagementPropertiesCodecSupport.putProperty(properties, KEY_FALLBACK_PROVIDER, skill.fallbackProvider());
        SkillManagementPropertiesCodecSupport.putLineTokens(properties, KEY_TOOLS, skill.tools());
        SkillManagementPropertiesCodecSupport.putPrefixedStringProperties(
                properties,
                SUB_SKILL_PROMPT_PREFIX,
                skill.subSkillPrompts());
        putOrchestration(properties, skill.orchestration());
        SkillManagementPropertiesCodecSupport.putPrefixedScalarProperties(
                properties,
                METADATA_PREFIX,
                skill.metadata());
        return properties;
    }

    byte[] toBytes(SkillDefinition skill) {
        return SkillManagementPropertiesCodecSupport.storeToBytes(
                toProperties(skill),
                "Wayang skill definition",
                "Failed to encode skill definition: " + skill.id());
    }

    byte[] toCanonicalBytes(SkillDefinition skill) {
        Properties properties = toProperties(skill);
        StringBuilder builder = new StringBuilder();
        properties.stringPropertyNames().stream()
                .sorted()
                .forEach(key -> builder
                        .append(key)
                        .append('=')
                        .append(properties.getProperty(key))
                        .append('\n'));
        return SkillManagementPropertiesCodecSupport.toUtf8Bytes(builder.toString());
    }

    SkillDefinition fromBytes(byte[] content, String sourceDescription) {
        Properties properties = SkillManagementPropertiesCodecSupport.loadFromBytes(
                content,
                "Failed to decode skill definition from " + sourceDescription);
        return fromProperties(properties, sourceDescription);
    }

    SkillDefinition fromProperties(Properties properties, String sourceDescription) {
        Objects.requireNonNull(properties, "properties");
        return SkillDefinition.builder()
                .id(SkillManagementPropertiesCodecSupport.requiredProperty(properties, KEY_ID, sourceDescription))
                .name(properties.getProperty(KEY_NAME))
                .description(properties.getProperty(KEY_DESCRIPTION))
                .category(properties.getProperty(KEY_CATEGORY))
                .systemPrompt(SkillManagementPropertiesCodecSupport.requiredProperty(
                        properties,
                        KEY_SYSTEM_PROMPT,
                        sourceDescription))
                .subSkillPrompts(subSkillPrompts(properties))
                .userPromptTemplate(properties.getProperty(KEY_USER_PROMPT_TEMPLATE))
                .temperature(SkillManagementPropertiesCodecSupport.doubleOrNull(
                        properties.getProperty(KEY_TEMPERATURE)))
                .maxTokens(SkillManagementPropertiesCodecSupport.integerOrNull(
                        properties.getProperty(KEY_MAX_TOKENS)))
                .defaultProvider(properties.getProperty(KEY_DEFAULT_PROVIDER))
                .fallbackProvider(properties.getProperty(KEY_FALLBACK_PROVIDER))
                .tools(SkillManagementPropertiesCodecSupport.lineTokens(properties.getProperty(KEY_TOOLS)))
                .orchestration(orchestration(properties))
                .metadata(metadata(properties))
                .build();
    }

    private void putOrchestration(Properties properties, SkillDefinition.OrchestrationConfig orchestration) {
        if (orchestration == null) {
            return;
        }
        SkillManagementPropertiesCodecSupport.putProperty(
                properties,
                KEY_ORCHESTRATION_TYPE,
                orchestration.defaultType());
        SkillManagementPropertiesCodecSupport.putProperty(
                properties,
                KEY_ORCHESTRATION_STRATEGY,
                orchestration.defaultStrategy());
        SkillManagementPropertiesCodecSupport.putLineTokens(
                properties,
                KEY_ORCHESTRATION_CHILD_SKILLS,
                orchestration.defaultChildSkills());
        SkillManagementPropertiesCodecSupport.putProperty(
                properties,
                KEY_ORCHESTRATION_MAX_ITERATIONS,
                orchestration.maxIterations());
        SkillManagementPropertiesCodecSupport.putProperty(
                properties,
                KEY_ORCHESTRATION_MAX_DELEGATIONS,
                orchestration.maxDelegations());
    }

    private Map<String, String> subSkillPrompts(Properties properties) {
        return SkillManagementPropertiesCodecSupport.prefixedStringProperties(
                properties,
                SUB_SKILL_PROMPT_PREFIX);
    }

    private SkillDefinition.OrchestrationConfig orchestration(Properties properties) {
        String type = properties.getProperty(KEY_ORCHESTRATION_TYPE);
        String strategy = properties.getProperty(KEY_ORCHESTRATION_STRATEGY);
        List<String> childSkills =
                SkillManagementPropertiesCodecSupport.lineTokens(
                        properties.getProperty(KEY_ORCHESTRATION_CHILD_SKILLS));
        Integer maxIterations =
                SkillManagementPropertiesCodecSupport.integerOrNull(
                        properties.getProperty(KEY_ORCHESTRATION_MAX_ITERATIONS));
        Integer maxDelegations =
                SkillManagementPropertiesCodecSupport.integerOrNull(
                        properties.getProperty(KEY_ORCHESTRATION_MAX_DELEGATIONS));
        if (SkillManagementPropertiesCodecSupport.isBlank(type)
                && SkillManagementPropertiesCodecSupport.isBlank(strategy)
                && childSkills.isEmpty()
                && maxIterations == null && maxDelegations == null) {
            return null;
        }
        return new SkillDefinition.OrchestrationConfig(
                type,
                strategy,
                childSkills,
                maxIterations,
                maxDelegations);
    }

    private Map<String, Object> metadata(Properties properties) {
        Map<String, Object> metadata = new LinkedHashMap<>(
                SkillManagementPropertiesCodecSupport.prefixedStringProperties(
                        properties,
                        METADATA_PREFIX));
        return Map.copyOf(metadata);
    }
}
