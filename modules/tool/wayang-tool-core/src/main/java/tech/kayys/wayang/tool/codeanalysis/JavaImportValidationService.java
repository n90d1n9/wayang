/*
 * Wayang Code Analysis - Java Import Validation Service
 * Copyright (c) 2026 Kayys.tech
 * SPDX-License-Identifier: Apache-2.0
 */
package tech.kayys.wayang.tool.codeanalysis;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for validating Java import statements across a codebase.
 * This service can be injected into Wayang agents to provide import validation capabilities.
 * 
 * <p>Usage example:
 * <pre>{@code
 * &#64;Inject
 * JavaImportValidationService importValidator;
 * 
 * List<ImportValidationIssue> issues = importValidator.validateCodebase(Path.of("/path/to/codebase"));
 * issues.forEach(System.out::println);
 * }</pre>
 */
@ApplicationScoped
public class JavaImportValidationService {

    private final ImportStatementValidator validator;

    /**
     * Creates a new validation service with the configured codebase root.
     * 
     * @param codebaseRoot The root directory of the codebase to validate
     */
    @Inject
    public JavaImportValidationService(@ConfigProperty(name="wayang.tool.codebase.root", defaultValue=".") String codebaseRoot) {
        this.validator = new ImportStatementValidator(Path.of(codebaseRoot));
    }

    /**
     * Validates all Java files in the configured codebase.
     * 
     * @return List of import validation issues found
     */
    public List<ImportValidationIssue> validateCodebase() {
        return validator.validateCodebase();
    }

    /**
     * Validates a specific Java file.
     * 
     * @param filePath Path to the Java file to validate
     * @return List of import validation issues in the file
     */
    public List<ImportValidationIssue> validateFile(Path filePath) {
        return validator.validateFile(filePath)
            .collect(Collectors.toList());
    }

    /**
     * Validates Java content directly.
     * 
     * @param filePath The path to associate with the content (for error reporting)
     * @param content The Java source code content
     * @return List of import validation issues
     */
    public List<ImportValidationIssue> validateContent(Path filePath, String content) {
        return validator.validateContent(filePath, content);
    }

    /**
     * Validates a specific directory within the codebase.
     * 
     * @param directory The directory to validate
     * @return List of import validation issues found
     */
    public List<ImportValidationIssue> validateDirectory(Path directory) {
        return validator.validateDirectory(directory);
    }

    /**
     * Checks if there are any import validation issues in the codebase.
     * 
     * @return true if issues were found, false otherwise
     */
    public boolean hasIssues() {
        return !validator.validateCodebase().isEmpty();
    }

    /**
     * Gets the count of import validation issues in the codebase.
     * 
     * @return The number of issues found
     */
    public long getIssueCount() {
        return validator.validateCodebase().stream()
            .filter(issue -> issue.getSeverity() == ImportValidationIssue.Severity.ERROR)
            .count();
    }

    /**
     * Gets only error-level issues from the codebase.
     * 
     * @return List of error-level import validation issues
     */
    public List<ImportValidationIssue> getErrors() {
        return validator.validateCodebase().stream()
            .filter(issue -> issue.getSeverity() == ImportValidationIssue.Severity.ERROR)
            .collect(Collectors.toList());
    }

    /**
     * Gets only warning-level issues from the codebase.
     * 
     * @return List of warning-level import validation issues
     */
    public List<ImportValidationIssue> getWarnings() {
        return validator.validateCodebase().stream()
            .filter(issue -> issue.getSeverity() == ImportValidationIssue.Severity.WARNING)
            .collect(Collectors.toList());
    }

    /**
     * Clears any cached index data.
     */
    public void clearCache() {
        validator.clearCache();
    }

    /**
     * Creates a formatted report of all import validation issues.
     * 
     * @return A formatted string report
     */
    public String generateReport() {
        List<ImportValidationIssue> issues = validator.validateCodebase();
        
        if (issues.isEmpty()) {
            return "No import validation issues found.";
        }

        StringBuilder report = new StringBuilder();
        report.append("=== Import Validation Report ===\n\n");
        
        long errorCount = issues.stream()
            .filter(issue -> issue.getSeverity() == ImportValidationIssue.Severity.ERROR)
            .count();
        long warningCount = issues.stream()
            .filter(issue -> issue.getSeverity() == ImportValidationIssue.Severity.WARNING)
            .count();
        
        report.append(String.format("Summary: %d errors, %d warnings\n\n", errorCount, warningCount));
        
        // Group by severity
        Map<ImportValidationIssue.Severity, List<ImportValidationIssue>> bySeverity = 
            issues.stream()
                .collect(Collectors.groupingBy(ImportValidationIssue::getSeverity));
        
        if (bySeverity.containsKey(ImportValidationIssue.Severity.ERROR)) {
            report.append("--- Errors ---\n");
            for (ImportValidationIssue issue : bySeverity.get(ImportValidationIssue.Severity.ERROR)) {
                report.append("  ").append(issue.toString()).append("\n");
            }
            report.append("\n");
        }
        
        if (bySeverity.containsKey(ImportValidationIssue.Severity.WARNING)) {
            report.append("--- Warnings ---\n");
            for (ImportValidationIssue issue : bySeverity.get(ImportValidationIssue.Severity.WARNING)) {
                report.append("  ").append(issue.toString()).append("\n");
            }
        }
        
        return report.toString();
    }
}
