/*
 * Wayang Code Analysis - Import Validation Issue
 * Copyright (c) 2026 Kayys.tech
 * SPDX-License-Identifier: Apache-2.0
 */
package tech.kayys.wayang.tool.codeanalysis;

import java.util.Objects;

/**
 * Represents a validation issue found during import statement analysis.
 */
public class ImportValidationIssue {

    /**
     * Severity levels for validation issues.
     */
    public enum Severity {
        /** Critical error that will prevent compilation */
        ERROR,
        /** Warning that may indicate a problem but won't prevent compilation */
        WARNING,
        /** Informational note about potential improvements */
        INFO
    }

    private final String filePath;
    private final String message;
    private final Severity severity;
    private final String suggestedFix;

    public ImportValidationIssue(String filePath, String message, Severity severity) {
        this(filePath, message, severity, null);
    }

    public ImportValidationIssue(String filePath, String message, Severity severity, String suggestedFix) {
        this.filePath = Objects.requireNonNull(filePath, "filePath cannot be null");
        this.message = Objects.requireNonNull(message, "message cannot be null");
        this.severity = Objects.requireNonNull(severity, "severity cannot be null");
        this.suggestedFix = suggestedFix;
    }

    /**
     * @return The path to the file containing the issue
     */
    public String getFilePath() {
        return filePath;
    }

    /**
     * @return The description of the issue
     */
    public String getMessage() {
        return message;
    }

    /**
     * @return The severity level of the issue
     */
    public Severity getSeverity() {
        return severity;
    }

    /**
     * @return An optional suggested fix for the issue, or null if none available
     */
    public String getSuggestedFix() {
        return suggestedFix;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(severity).append("] ");
        sb.append(filePath).append(": ").append(message);
        if (suggestedFix != null) {
            sb.append(" - Suggestion: ").append(suggestedFix);
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ImportValidationIssue that = (ImportValidationIssue) o;
        return filePath.equals(that.filePath) && 
               message.equals(that.message) && 
               severity == that.severity && 
               Objects.equals(suggestedFix, that.suggestedFix);
    }

    @Override
    public int hashCode() {
        return Objects.hash(filePath, message, severity, suggestedFix);
    }
}
