package tech.kayys.wayang.agent.core.skills.integration;

import tech.kayys.wayang.agent.core.skills.manifest.SkillManifest;
import tech.kayys.wayang.agent.core.skills.validation.SkillParameterSchema;
import tech.kayys.wayang.agent.spi.skills.SkillDefinition;
import tech.kayys.wayang.agent.spi.skills.SkillMetadataKeys;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Projects filesystem skill manifests into runtime skill definitions.
 */
public final class SkillManifestRuntimeSkillMapper {

    public static final String CATEGORY_FILESYSTEM = "filesystem";
    public static final String KEY_COMPATIBILITY = "compatibility";
    public static final String KEY_FILESYSTEM_SKILL = "filesystemSkill";
    public static final String KEY_LICENSE = "license";
    public static final String KEY_QUALIFIED_ID = "qualifiedId";
    public static final String KEY_SOURCE_DIRECTORY = "sourceDirectory";
    public static final String KEY_SOURCE_REPO = "sourceRepo";

    public SkillDefinition toSkillDefinition(SkillManifest manifest) {
        Objects.requireNonNull(manifest, "manifest");
        String name = requireName(manifest);
        Map<String, Object> metadata = metadataFrom(manifest);
        return SkillDefinition.builder()
                .id(name)
                .name(name)
                .description(descriptionFrom(manifest, name))
                .category(SkillMetadataKeys.category(metadata, CATEGORY_FILESYSTEM))
                .systemPrompt(systemPromptFrom(manifest, name))
                .userPromptTemplate("{{instruction}}\n\n{{context}}")
                .tools(toolsFrom(manifest))
                .metadata(metadata)
                .build();
    }

    private Map<String, Object> metadataFrom(SkillManifest manifest) {
        Map<String, Object> metadata = new LinkedHashMap<>(manifest.getMetadata());
        metadata.putIfAbsent(KEY_FILESYSTEM_SKILL, true);
        metadata.putIfAbsent(SkillMetadataKeys.KEY_VERSION, manifest.getVersion());
        metadata.putIfAbsent(SkillMetadataKeys.KEY_CATEGORY, CATEGORY_FILESYSTEM);
        metadata.putIfAbsent(KEY_QUALIFIED_ID, manifest.getQualifiedId());
        putIfPresent(metadata, KEY_LICENSE, manifest.getLicense());
        putIfPresent(metadata, KEY_COMPATIBILITY, manifest.getCompatibility());
        putIfPresent(metadata, KEY_SOURCE_REPO, manifest.getSourceRepo());
        Path sourceDirectory = manifest.getSourceDirectory();
        if (sourceDirectory != null) {
            metadata.putIfAbsent(KEY_SOURCE_DIRECTORY, sourceDirectory.toString());
        }

        Map<String, Object> inputSchema = SkillParameterSchema.fromManifest(manifest).toJsonSchema();
        if (!inputSchema.isEmpty()) {
            metadata.put(SkillMetadataKeys.KEY_INPUT_SCHEMA, inputSchema);
        }
        metadata.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
        return Map.copyOf(metadata);
    }

    private static String requireName(SkillManifest manifest) {
        String name = manifest.getName();
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Manifest skill name is required");
        }
        return name.trim();
    }

    private static String descriptionFrom(SkillManifest manifest, String name) {
        String description = manifest.getDescription();
        if (description != null && !description.isBlank()) {
            return description.trim();
        }
        return "Filesystem skill " + name;
    }

    private static String systemPromptFrom(SkillManifest manifest, String name) {
        String body = manifest.getBodyContent();
        if (body != null && !body.isBlank()) {
            return body.trim();
        }
        String description = manifest.getDescription();
        if (description != null && !description.isBlank()) {
            return description.trim();
        }
        return "Execute filesystem skill " + name + ".";
    }

    private static List<String> toolsFrom(SkillManifest manifest) {
        if (manifest.getAllowedTools() != null && !manifest.getAllowedTools().isEmpty()) {
            return manifest.getAllowedTools();
        }
        String allowedTools = manifest.getAllowedToolsString();
        if (allowedTools == null || allowedTools.isBlank()) {
            return List.of();
        }
        return List.of(allowedTools.trim().split("\\s+"));
    }

    private static void putIfPresent(Map<String, Object> metadata, String key, String value) {
        if (value != null && !value.isBlank()) {
            metadata.putIfAbsent(key, value.trim());
        }
    }
}
