package tech.kayys.wayang.agent.core.skills.validation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tech.kayys.wayang.agent.core.skills.manifest.SkillManifest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillValidatorContractTest {

    private final SkillValidator validator = new SkillValidator();

    @Test
    void validatesJsonSchemaStringWithRequiredTypesAndClosedInputs() {
        String schema = """
                {
                  "type": "object",
                  "properties": {
                    "prompt": { "type": "string" },
                    "maxTokens": { "type": "integer" }
                  },
                  "required": ["prompt"],
                  "additionalProperties": false
                }
                """;

        SkillValidator.ValidationResult result = validator.validateParameters(
                "run-inference",
                schema,
                Map.of("maxTokens", "lots", "extra", true));

        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("Missing required parameter: prompt"));
        assertTrue(result.getErrors().contains("Unexpected parameter: extra"));
        assertTrue(result.getErrors().stream()
                .anyMatch(error -> error.contains("Parameter 'maxTokens' must be integer")));
    }

    @Test
    void supportsSimplePerParameterSchemaAndWarnsAboutOpenInputs() {
        Map<String, Object> schema = Map.of(
                "query", Map.of("type", "string", "required", true),
                "limit", "integer");

        SkillValidator.ValidationResult result = validator.validateParameters(
                "search",
                schema,
                Map.of("query", "agent validation", "limit", 5, "debug", true));

        assertTrue(result.isValid());
        assertTrue(result.getWarnings().contains("Parameter 'debug' not defined in skill schema"));
    }

    @Test
    void validatesEnumValuesFromJsonSchema() {
        Map<String, Object> schema = Map.of(
                "type", "object",
                "properties", Map.of(
                        "mode", Map.of("type", "string", "enum", List.of("fast", "deep"))),
                "required", List.of("mode"));

        SkillValidator.ValidationResult result = validator.validateParameters(
                "planner",
                schema,
                Map.of("mode", "careless"));

        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("Parameter 'mode' must be one of [fast, deep]"));
    }

    @Test
    void unwrapsInputSchemaFromMetadataEnvelope() {
        Map<String, Object> metadata = Map.of(
                "version", "1.0.0",
                "category", "filesystem",
                "inputSchema", Map.of(
                        "type", "object",
                        "properties", Map.of("path", Map.of("type", "string")),
                        "required", List.of("path")));

        SkillValidator.ValidationResult result = validator.validateParameters(
                "read-file",
                metadata,
                Map.of());

        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("Missing required parameter: path"));
    }

    @Test
    void rejectsMalformedJsonSchemaInsteadOfSilentlySkippingValidation() {
        SkillValidator.ValidationResult result = validator.validateParameters(
                "broken",
                "{ not-json",
                Map.of());

        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("Invalid parameter schema"));
    }

    @Test
    void resolvesInputSchemaFromManifestMetadata() {
        SkillManifest manifest = SkillManifest.builder()
                .name("summarize")
                .description("Use this skill for concise summary generation")
                .metadata(Map.of(
                        "inputSchema", Map.of(
                                "type", "object",
                                "properties", Map.of("text", Map.of("type", "string")),
                                "required", List.of("text"))))
                .build();

        SkillValidator.ValidationResult result = validator.validateParameters(
                "summarize",
                manifest,
                Map.of());

        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("Missing required parameter: text"));
    }

    @Test
    void ignoresPlainAllowedToolsWhenManifestHasNoParameterSchema() {
        SkillManifest manifest = SkillManifest.builder()
                .name("read-file")
                .description("Use this skill when reading workspace files")
                .allowedToolsString("read_file list_dir glob")
                .build();

        SkillValidator.ValidationResult result = validator.validateParameters(
                "read-file",
                manifest,
                Map.of("path", "README.md"));

        assertTrue(result.isValid());
        assertTrue(result.getWarnings().isEmpty());
    }

    @Test
    void reportsMalformedYamlFrontmatter(@TempDir Path tempDir) throws Exception {
        Path skillDir = tempDir.resolve("broken-skill");
        Files.createDirectories(skillDir);
        Files.writeString(skillDir.resolve("SKILL.md"), """
                ---
                name: broken-skill
                metadata: [unterminated
                ---

                ## Usage
                Use this skill for testing malformed frontmatter.
                """);

        SkillValidator.ValidationResult result = validator.validateSkillDirectory(skillDir);

        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("Failed to parse YAML frontmatter"));
    }

    @Test
    void validationResultListsAreImmutable() {
        SkillValidator.ValidationResult result = validator.validateParameters(
                "broken",
                "{ nope",
                Map.of());

        assertThrows(UnsupportedOperationException.class, () -> result.getErrors().add("later"));
        assertThrows(UnsupportedOperationException.class, () -> result.getWarnings().add("later"));
    }
}
