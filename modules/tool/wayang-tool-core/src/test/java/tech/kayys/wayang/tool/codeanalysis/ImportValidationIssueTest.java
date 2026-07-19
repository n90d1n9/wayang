/*
 * Wayang Code Analysis - Import Validation Issue Tests
 * Copyright (c) 2026 Kayys.tech
 * SPDX-License-Identifier: Apache-2.0
 */
package tech.kayys.wayang.tool.codeanalysis;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ImportValidationIssueTest {

    @Test
    void testCreation() {
        ImportValidationIssue issue = new ImportValidationIssue(
            "/path/to/File.java",
            "Test error message",
            ImportValidationIssue.Severity.ERROR
        );
        
        assertEquals("/path/to/File.java", issue.getFilePath());
        assertEquals("Test error message", issue.getMessage());
        assertEquals(ImportValidationIssue.Severity.ERROR, issue.getSeverity());
        assertNull(issue.getSuggestedFix());
    }

    @Test
    void testCreationWithSuggestion() {
        ImportValidationIssue issue = new ImportValidationIssue(
            "/path/to/File.java",
            "Test error message",
            ImportValidationIssue.Severity.ERROR,
            "import correct.package.Class;"
        );
        
        assertEquals("/path/to/File.java", issue.getFilePath());
        assertEquals("Test error message", issue.getMessage());
        assertEquals(ImportValidationIssue.Severity.ERROR, issue.getSeverity());
        assertEquals("import correct.package.Class;", issue.getSuggestedFix());
    }

    @Test
    void testGetters() {
        ImportValidationIssue issue = new ImportValidationIssue(
            "file.txt",
            "message",
            ImportValidationIssue.Severity.WARNING,
            "fix"
        );
        
        assertEquals("file.txt", issue.getFilePath());
        assertEquals("message", issue.getMessage());
        assertEquals(ImportValidationIssue.Severity.WARNING, issue.getSeverity());
        assertEquals("fix", issue.getSuggestedFix());
    }

    @Test
    void testToString_WithSuggestion() {
        ImportValidationIssue issue = new ImportValidationIssue(
            "/path/File.java",
            "Class not found",
            ImportValidationIssue.Severity.ERROR,
            "import correct.Class;"
        );
        
        String str = issue.toString();
        assertTrue(str.contains("[ERROR]"), "Should contain severity");
        assertTrue(str.contains("/path/File.java"), "Should contain file path");
        assertTrue(str.contains("Class not found"), "Should contain message");
        assertTrue(str.contains("import correct.Class;"), "Should contain suggestion");
    }

    @Test
    void testToString_WithoutSuggestion() {
        ImportValidationIssue issue = new ImportValidationIssue(
            "/path/File.java",
            "Class not found",
            ImportValidationIssue.Severity.WARNING
        );
        
        String str = issue.toString();
        assertTrue(str.contains("[WARNING]"), "Should contain severity");
        assertTrue(str.contains("/path/File.java"), "Should contain file path");
        assertTrue(str.contains("Class not found"), "Should contain message");
        assertFalse(str.contains("Suggestion:"), "Should not contain suggestion label");
    }

    @Test
    void testEqualsAndHashCode() {
        ImportValidationIssue issue1 = new ImportValidationIssue(
            "/path/File.java", "Message", ImportValidationIssue.Severity.ERROR);
        ImportValidationIssue issue2 = new ImportValidationIssue(
            "/path/File.java", "Message", ImportValidationIssue.Severity.ERROR);
        ImportValidationIssue issue3 = new ImportValidationIssue(
            "/other/File.java", "Message", ImportValidationIssue.Severity.ERROR);
        
        assertEquals(issue1, issue2, "Equal issues should be equal");
        assertNotEquals(issue1, issue3, "Different issues should not be equal");
        
        assertEquals(issue1.hashCode(), issue2.hashCode(), "Equal issues should have equal hash codes");
    }

    @Test
    void testNullRejection() {
        assertThrows(NullPointerException.class, () -> {
            new ImportValidationIssue(null, "message", ImportValidationIssue.Severity.ERROR);
        }, "Should throw on null filePath");
        
        assertThrows(NullPointerException.class, () -> {
            new ImportValidationIssue("file", null, ImportValidationIssue.Severity.ERROR);
        }, "Should throw on null message");
        
        assertThrows(NullPointerException.class, () -> {
            new ImportValidationIssue("file", "message", null);
        }, "Should throw on null severity");
    }

    @Test
    void testAllSeverityLevels() {
        for (ImportValidationIssue.Severity severity : ImportValidationIssue.Severity.values()) {
            ImportValidationIssue issue = new ImportValidationIssue(
                "file", "msg", severity);
            assertEquals(severity, issue.getSeverity());
        }
    }
}
