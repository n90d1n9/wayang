package tech.kayys.gollek.skills.validator;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 * Command-line interface for Skills Validator.
 * 
 * Usage:
 *   java -jar skills-validator.jar [SKILLS_DIR]
 *   
 * Exit codes:
 *   0 - All skills valid
 *   1 - Validation errors found
 *   2 - Invalid usage or I/O error
 */
public final class SkillsValidatorCLI {

    private static final String VERSION = "0.1.0";
    private static final String SPEC_URL = "https://agentskills.io/specification";

    public static void main(String[] args) {
        try {
            int exitCode = run(args);
            System.exit(exitCode);
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            System.exit(2);
        }
    }

    private static int run(String[] args) throws IOException {
        // Parse arguments
        String skillsDirStr = args.length > 0 ? args[0] : ".";

        if (Arrays.asList(args).contains("--help") || Arrays.asList(args).contains("-h")) {
            printHelp();
            return 0;
        }

        if (Arrays.asList(args).contains("--version") || Arrays.asList(args).contains("-v")) {
            printVersion();
            return 0;
        }

        Path skillsDir = Paths.get(skillsDirStr).toAbsolutePath();

        // Validate
        printHeader();
        System.out.println("Validating skills in: " + skillsDir + "\n");

        SkillsValidator validator = new SkillsValidator();
        boolean allValid = validator.validateAll(skillsDir);

        // Print results
        printResults(validator);

        return allValid ? 0 : 1;
    }

    private static void printHeader() {
        System.out.println("═".repeat(80));
        System.out.println("Agent Skills Specification Validator");
        System.out.println("Specification: " + SPEC_URL);
        System.out.println("═".repeat(80) + "\n");
    }

    private static void printResults(SkillsValidator validator) {
        SkillsValidator.ValidationSummary summary = validator.getSummary();

        // Print individual results
        for (ValidationResult result : validator.getResults()) {
            printSkillResult(result);
        }

        // Print summary
        System.out.println("\n" + "═".repeat(80));
        System.out.println("Validation Summary");
        System.out.println("═".repeat(80) + "\n");

        if (summary.isAllValid()) {
            System.out.println("✓ All " + summary.getValidSkills() + " skills are valid");
        } else {
            System.out.println("✗ Validation failed");
        }

        System.out.println("\nResults:");
        System.out.println(String.format("  Valid Skills:  %d", summary.getValidSkills()));
        System.out.println(String.format("  Total Skills:  %d", summary.getTotalSkills()));
        System.out.println(String.format("  Errors:        %d", summary.getErrorCount()));
        System.out.println(String.format("  Warnings:      %d", summary.getWarningCount()));
        System.out.println();

        // Print details if there are errors
        if (summary.getErrorCount() > 0) {
            System.out.println("Errors:");
            for (ValidationResult result : validator.getResults()) {
                if (!result.isValid()) {
                    System.out.println("  " + result.getSkillName());
                    for (String error : result.getErrors()) {
                        System.out.println("    • " + error);
                    }
                }
            }
            System.out.println();
        }

        // Print warnings if any
        if (summary.getWarningCount() > 0) {
            System.out.println("Warnings:");
            for (ValidationResult result : validator.getResults()) {
                if (result.hasWarnings()) {
                    System.out.println("  " + result.getSkillName());
                    for (String warning : result.getWarnings()) {
                        System.out.println("    • " + warning);
                    }
                }
            }
            System.out.println();
        }
    }

    private static void printSkillResult(ValidationResult result) {
        System.out.println("━".repeat(80));
        System.out.println("Validating: " + result.getSkillName());
        System.out.println("━".repeat(80));

        if (result.isValid()) {
            System.out.println("✓ Valid");
        } else {
            System.out.println("✗ Invalid");
            for (String error : result.getErrors()) {
                System.out.println("  ERROR: " + error);
            }
        }

        if (result.hasWarnings()) {
            for (String warning : result.getWarnings()) {
                System.out.println("  WARNING: " + warning);
            }
        }

        System.out.println();
    }

    private static void printHelp() {
        System.out.println("Skills Validator - Agent Skills Specification Compliance Checker");
        System.out.println();
        System.out.println("Usage:");
        System.out.println("  java -jar skills-validator.jar [OPTIONS] [SKILLS_DIR]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  -h, --help     Show this help message");
        System.out.println("  -v, --version  Show version");
        System.out.println();
        System.out.println("Arguments:");
        System.out.println("  SKILLS_DIR     Path to skills directory (default: current directory)");
        System.out.println();
        System.out.println("Specification:");
        System.out.println("  " + SPEC_URL);
        System.out.println();
        System.out.println("Exit codes:");
        System.out.println("  0 - All skills valid");
        System.out.println("  1 - Validation errors found");
        System.out.println("  2 - Invalid usage or I/O error");
    }

    private static void printVersion() {
        System.out.println("Skills Validator v" + VERSION);
    }
}
