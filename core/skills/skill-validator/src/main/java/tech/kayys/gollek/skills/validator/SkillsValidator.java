package tech.kayys.gollek.skills.validator;

import tech.kayys.wayang.agent.core.skills.validation.SkillValidator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Orchestrates validation of multiple skills and generates reports.
 * 
 * <p>Uses the unified SkillValidator from agent-core for spec compliance.
 */
public final class SkillsValidator {

    private final SkillValidator validator = new SkillValidator();
    private final List<SkillValidator.ValidationResult> results = new ArrayList<>();

    /**
     * Validates all skills in a directory.
     *
     * @param skillsDirectory Path to skills directory
     * @return true if all skills are valid (no errors), false otherwise
     * @throws IOException if directory cannot be accessed
     */
    public boolean validateAll(Path skillsDirectory) throws IOException {
        if (!Files.isDirectory(skillsDirectory)) {
            throw new IllegalArgumentException("Path is not a directory: " + skillsDirectory);
        }

        results.clear();

        // Find skill directories
        try (Stream<Path> stream = Files.list(skillsDirectory)) {
            stream.filter(Files::isDirectory)
                    .filter(p -> !p.getFileName().toString().equals(".DS_Store"))
                    .sorted()
                    .forEach(skillPath -> {
                        SkillValidator.ValidationResult result = validator.validateSkillDirectory(skillPath);
                        results.add(result);
                    });
        }

        return results.stream().allMatch(SkillValidator.ValidationResult::isValid);
    }

    /**
     * Gets validation results.
     *
     * @return List of validation results
     */
    public List<SkillValidator.ValidationResult> getResults() {
        return new ArrayList<>(results);
    }

    /**
     * Gets summary statistics.
     *
     * @return ValidationSummary containing counts
     */
    public ValidationSummary getSummary() {
        int totalSkills = results.size();
        int validSkills = (int) results.stream().filter(SkillValidator.ValidationResult::isValid).count();
        int errorCount = (int) results.stream().mapToLong(r -> r.getErrors().size()).sum();
        int warningCount = (int) results.stream().mapToLong(r -> r.getWarnings().size()).sum();

        return new ValidationSummary(totalSkills, validSkills, errorCount, warningCount);
    }

    /**
     * Validation summary statistics.
     */
    public static final class ValidationSummary {
        private final int totalSkills;
        private final int validSkills;
        private final int errorCount;
        private final int warningCount;

        public ValidationSummary(int totalSkills, int validSkills, int errorCount, int warningCount) {
            this.totalSkills = totalSkills;
            this.validSkills = validSkills;
            this.errorCount = errorCount;
            this.warningCount = warningCount;
        }

        public int getTotalSkills() {
            return totalSkills;
        }

        public int getValidSkills() {
            return validSkills;
        }

        public int getErrorCount() {
            return errorCount;
        }

        public int getWarningCount() {
            return warningCount;
        }

        public boolean isAllValid() {
            return errorCount == 0;
        }

        @Override
        public String toString() {
            return String.format("ValidationSummary{total=%d, valid=%d, errors=%d, warnings=%d}",
                    totalSkills, validSkills, errorCount, warningCount);
        }
    }
}
