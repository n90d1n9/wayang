package tech.kayys.gollek.agent.skills.validation;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gollek.agent.skills.repo.spi.SkillContent;
import tech.kayys.gollek.agent.skills.repo.spi.SkillMetadata;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Skill validation service for pre-execution validation and security scanning.
 *
 * <p>Features:
 * <ul>
 *   <li>Metadata validation</li>
 *   <li>Content validation</li>
 *   <li>Security scanning</li>
 *   <li>Dependency checking</li>
 *   <li>Best practices validation</li>
 * </ul>
 */
@ApplicationScoped
public class SkillValidationService {

    private static final Logger log = LoggerFactory.getLogger(SkillValidationService.class);

    // Security patterns
    private static final Pattern DANGEROUS_CODE_PATTERN = Pattern.compile(
        "(Runtime\\.getRuntime|ProcessBuilder|System\\.exec|eval\\(|exec\\()",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
        "(SELECT|INSERT|UPDATE|DELETE|DROP|CREATE|ALTER)\\s+.*\\s+(FROM|INTO|TABLE)",
        Pattern.CASE_INSENSITIVE
    );

    private static final int MAX_CONTENT_SIZE = 1024 * 1024; // 1MB
    private static final int MAX_NAME_LENGTH = 255;
    private static final int MIN_DESCRIPTION_LENGTH = 10;

    /**
     * Validate skill content.
     */
    public ValidationResult validate(SkillContent content) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> info = new ArrayList<>();

        // Validate metadata
        validateMetadata(content.metadata(), errors, warnings, info);

        // Validate content
        validateContent(content.content(), errors, warnings, info);

        // Security scan
        securityScan(content, errors, warnings);

        // Validate dependencies
        validateDependencies(content, errors, warnings);

        ValidationStatus status = errors.isEmpty() ? 
            (warnings.isEmpty() ? ValidationStatus.VALID : ValidationStatus.WARNING) : 
            ValidationStatus.INVALID;

        return new ValidationResult(status, content.metadata().id(), errors, warnings, info);
    }

    /**
     * Validate skill asynchronously.
     */
    public Uni<ValidationResult> validateAsync(SkillContent content) {
        return Uni.createFrom().item(() -> validate(content));
    }

    /**
     * Quick validation (metadata only).
     */
    public ValidationResult quickValidate(SkillMetadata metadata) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        validateMetadataBasic(metadata, errors, warnings);

        return errors.isEmpty() ? 
            ValidationResult.valid(metadata.id()) : 
            ValidationResult.invalid(metadata.id(), errors);
    }

    private void validateMetadata(SkillMetadata metadata, List<String> errors, 
                                  List<String> warnings, List<String> info) {
        // Required fields
        if (metadata.id() == null || metadata.id().isBlank()) {
            errors.add("Skill ID is required");
        } else if (metadata.id().length() > 100) {
            errors.add("Skill ID must be less than 100 characters");
        } else if (!metadata.id().matches("^[a-zA-Z0-9_-]+$")) {
            errors.add("Skill ID can only contain alphanumeric characters, hyphens, and underscores");
        }

        if (metadata.name() == null || metadata.name().isBlank()) {
            errors.add("Skill name is required");
        } else if (metadata.name().length() > MAX_NAME_LENGTH) {
            errors.add("Skill name must be less than " + MAX_NAME_LENGTH + " characters");
        }

        if (metadata.description() == null || metadata.description().isBlank()) {
            errors.add("Skill description is required");
        } else if (metadata.description().length() < MIN_DESCRIPTION_LENGTH) {
            errors.add("Skill description must be at least " + MIN_DESCRIPTION_LENGTH + " characters");
        }

        // Version validation
        if (metadata.version() != null && !metadata.version().matches("^\\d+\\.\\d+\\.\\d+(-[a-zA-Z0-9]+)?$")) {
            warnings.add("Version should follow semver format (e.g., 1.0.0)");
        }

        // Category validation
        if (metadata.category() == null || metadata.category().isBlank()) {
            info.add("No category specified");
        }

        // Tags validation
        if (metadata.tags() != null && metadata.tags().size() > 20) {
            warnings.add("Too many tags (max 20 recommended)");
        }
    }

    private void validateMetadataBasic(SkillMetadata metadata, List<String> errors, List<String> warnings) {
        if (metadata.id() == null || metadata.id().isBlank()) {
            errors.add("Skill ID is required");
        }
        if (metadata.name() == null || metadata.name().isBlank()) {
            errors.add("Skill name is required");
        }
    }

    private void validateContent(String content, List<String> errors, 
                                 List<String> warnings, List<String> info) {
        if (content == null || content.isBlank()) {
            errors.add("Skill content is required");
            return;
        }

        // Size check
        if (content.length() > MAX_CONTENT_SIZE) {
            errors.add("Content size exceeds maximum allowed (" + MAX_CONTENT_SIZE + " bytes)");
        }

        // Check for incomplete code
        int openBraces = countChar(content, '{');
        int closeBraces = countChar(content, '}');
        if (openBraces != closeBraces) {
            warnings.add("Unbalanced braces in code (" + openBraces + " open, " + closeBraces + " close)");
        }

        // Check for TODO comments
        if (content.contains("TODO") || content.contains("FIXME")) {
            info.add("Code contains TODO/FIXME comments");
        }
    }

    private void securityScan(SkillContent content, List<String> errors, List<String> warnings) {
        String contentStr = content.content();

        // Dangerous code patterns
        if (DANGEROUS_CODE_PATTERN.matcher(contentStr).find()) {
            warnings.add("Content contains potentially dangerous code (Runtime.exec, eval, etc.)");
        }

        // SQL injection patterns
        if (SQL_INJECTION_PATTERN.matcher(contentStr).find()) {
            warnings.add("Content contains raw SQL queries - consider using parameterized queries");
        }

        // Hardcoded credentials
        if (contentStr.matches(".*(?i)(password|secret|api_key)\\s*=\\s*['\"].*['\"].*")) {
            warnings.add("Content may contain hardcoded credentials");
        }

        // Check manifest for security issues
        if (content.manifest() != null) {
            Object permissions = content.manifest().get("permissions");
            if (permissions != null) {
                info.add("Skill requests permissions: " + permissions);
            }
        }
    }

    private void validateDependencies(SkillContent content, List<String> errors, List<String> warnings) {
        if (content.manifest() == null) {
            return;
        }

        Object dependencies = content.manifest().get("dependencies");
        if (dependencies instanceof List<?> deps) {
            if (deps.size() > 50) {
                warnings.add("Large number of dependencies (" + deps.size() + ")");
            }

            // Check for known problematic dependencies
            for (Object dep : deps) {
                if (dep instanceof String depStr) {
                    if (depStr.contains("snapshot") || depStr.contains("SNAPSHOT")) {
                        warnings.add("Dependency on snapshot version: " + depStr);
                    }
                }
            }
        }
    }

    private int countChar(String str, char c) {
        return (int) str.chars().filter(ch -> ch == c).count();
    }
}
