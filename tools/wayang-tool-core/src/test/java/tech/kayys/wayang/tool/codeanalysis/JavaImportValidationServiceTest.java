/*
 * Wayang Code Analysis - Java Import Validation Service Tests
 * Copyright (c) 2026 Kayys.tech
 * SPDX-License-Identifier: Apache-2.0
 */
package tech.kayys.wayang.tool.codeanalysis;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JavaImportValidationServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void testValidateCodebase() throws IOException {
        JavaImportValidationService service = new JavaImportValidationService(tempDir.toString());
        List<ImportValidationIssue> issues = service.validateCodebase();
        assertTrue(issues.isEmpty(), "Should find no issues in empty codebase");
    }

    @Test
    void testValidateFile() throws IOException {
        Path pkgDir = tempDir.resolve("com/example");
        pkgDir.toFile().mkdirs();
        Path testClass = pkgDir.resolve("TestClass.java");
        Files.writeString(testClass, "package com.example;\npublic class TestClass {}");

        Path testFile = tempDir.resolve("Test.java");
        Files.writeString(testFile, "package test;\nimport com.example.TestClass;\npublic class Test {}");

        JavaImportValidationService service = new JavaImportValidationService(tempDir.toString());
        List<ImportValidationIssue> issues = service.validateFile(testFile);
        assertTrue(issues.isEmpty(), "Should find no issues for correct import");
    }

    @Test
    void testValidateContent() throws IOException {
        Path pkgDir = tempDir.resolve("com/example");
        pkgDir.toFile().mkdirs();
        Files.writeString(pkgDir.resolve("TestClass.java"), "package com.example;\npublic class TestClass {}");

        JavaImportValidationService service = new JavaImportValidationService(tempDir.toString());
        List<ImportValidationIssue> issues = service.validateContent(
            Path.of("Test.java"),
            "package test;\nimport com.example.TestClass;\npublic class Test {}"
        );
        assertTrue(issues.isEmpty(), "Should find no issues for correct import in content");
    }

    @Test
    void testGetErrors() throws IOException {
        Path pkgDir = tempDir.resolve("com/example");
        pkgDir.toFile().mkdirs();
        Files.writeString(pkgDir.resolve("GoodClass.java"), "package com.example;\npublic class GoodClass {}");

        Path badFile = tempDir.resolve("Bad.java");
        Files.writeString(badFile, "package test;\nimport com.wrong.Class;\npublic class Bad {}");

        JavaImportValidationService service = new JavaImportValidationService(tempDir.toString());
        List<ImportValidationIssue> errors = service.getErrors();
        assertEquals(1, errors.size(), "Should find one error");
        assertEquals(ImportValidationIssue.Severity.ERROR, errors.get(0).getSeverity());
    }

    @Test
    void testGetWarnings() throws IOException {
        JavaImportValidationService service = new JavaImportValidationService(tempDir.toString());
        List<ImportValidationIssue> warnings = service.getWarnings();
        assertTrue(warnings.isEmpty(), "Should find no warnings in empty codebase");
    }

    @Test
    void testGenerateReport_NoIssues() {
        JavaImportValidationService service = new JavaImportValidationService(tempDir.toString());
        String report = service.generateReport();
        assertTrue(report.contains("No import validation issues found"));
    }

    @Test
    void testGenerateReport_WithIssues() throws IOException {
        Path badFile = tempDir.resolve("Bad.java");
        Files.writeString(badFile, "package test;\nimport com.nonexistent.Class;\npublic class Bad {}");

        JavaImportValidationService service = new JavaImportValidationService(tempDir.toString());
        String report = service.generateReport();
        assertTrue(report.contains("Summary:"), "Report should contain summary");
        assertTrue(report.contains("1 error"), "Report should show error count");
    }

    @Test
    void testClearCache() throws IOException {
        Path pkgDir = tempDir.resolve("com/test");
        pkgDir.toFile().mkdirs();
        Files.writeString(pkgDir.resolve("Test.java"), "package com.test;\npublic class Test {}");

        JavaImportValidationService service = new JavaImportValidationService(tempDir.toString());
        service.validateCodebase();
        service.clearCache();
        List<ImportValidationIssue> issues = service.validateCodebase();
        assertTrue(issues.isEmpty(), "Should work after cache clear");
    }

    @Test
    void testHasIssues() throws IOException {
        JavaImportValidationService service = new JavaImportValidationService(tempDir.toString());
        assertFalse(service.hasIssues(), "Should have no issues in empty codebase");

        Path badFile = tempDir.resolve("Bad.java");
        Files.writeString(badFile, "package test;\nimport com.wrong.Class;\npublic class Bad {}");
        assertTrue(service.hasIssues(), "Should have issues after adding bad import");
    }

    @Test
    void testGetIssueCount() throws IOException {
        JavaImportValidationService service = new JavaImportValidationService(tempDir.toString());
        assertEquals(0, service.getIssueCount(), "Should have 0 issues in empty codebase");

        Path badFile = tempDir.resolve("Bad.java");
        Files.writeString(badFile, "package test;\nimport com.wrong.Class;\npublic class Bad {}");
        assertEquals(1, service.getIssueCount(), "Should have 1 issue after adding bad import");
    }
}
