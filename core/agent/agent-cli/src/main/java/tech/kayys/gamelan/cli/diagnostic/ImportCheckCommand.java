package tech.kayys.gamelan.cli.diagnostic;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import tech.kayys.wayang.tool.codeanalysis.ImportStatementValidator;
import tech.kayys.wayang.tool.codeanalysis.ImportValidationIssue;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

/**
 * CLI command to check Java import statements for validity.
 * This command scans Java files and reports any import statements that reference
 * classes that don't exist in the expected packages.
 * 
 * <p>This is particularly useful for catching issues after code migrations or
 * refactorings where package names have changed.
 * 
 * <p>Usage examples:
 * <pre>
 *   wayang import-check /path/to/project
 *   wayang import-check --path /path/to/project --fix
 *   wayang import-check --path . --severity ERROR
 * </pre>
 */
@Command(
    name = "import-check",
    description = "Check Java import statements for validity",
    mixinStandardHelpOptions = true
)
public class ImportCheckCommand implements Callable<Integer> {

    @Parameters(
        index = "0", 
        paramLabel = "PATH",
        description = "Path to the codebase or directory to check (default: current directory)",
        arity = "0..1"
    )
    String path;

    @Option(
        names = {"-p", "--path"},
        description = "Path to the codebase (alternative to positional parameter)"
    )
    String pathOption;

    @Option(
        names = {"-s", "--severity"},
        description = "Minimum severity level to report: ERROR, WARNING, INFO (default: ERROR)",
        defaultValue = "ERROR"
    )
    String severityFilter = "ERROR";

    @Option(
        names = {"-f", "--fix"},
        description = "Attempt to automatically fix detected issues"
    )
    boolean fix;

    @Option(
        names = {"-r", "--recursive"},
        description = "Recursively scan subdirectories (default: true)",
        defaultValue = "true"
    )
    boolean recursive = true;

    @Option(
        names = {"-q", "--quiet"},
        description = "Only output issues, not summary"
    )
    boolean quiet;

    @Option(
        names = {"-j", "--json"},
        description = "Output results as JSON"
    )
    boolean jsonOutput;

    @Override
    public Integer call() throws Exception {
        // Determine the path to scan
        Path scanPath = determineScanPath();
        
        // Validate the path exists
        if (!scanPath.toFile().exists()) {
            System.err.println("Error: Path does not exist: " + scanPath);
            return 1;
        }
        
        if (!scanPath.toFile().isDirectory()) {
            System.err.println("Error: Path is not a directory: " + scanPath);
            return 1;
        }

        // Parse severity filter
        ImportValidationIssue.Severity minSeverity;
        try {
            minSeverity = ImportValidationIssue.Severity.valueOf(severityFilter.toUpperCase());
        } catch (IllegalArgumentException e) {
            System.err.println("Error: Invalid severity level. Must be one of: ERROR, WARNING, INFO");
            return 1;
        }

        // Run validation
        ImportStatementValidator validator = new ImportStatementValidator(scanPath);
        List<ImportValidationIssue> issues = validator.validateCodebase();
        
        // Filter by severity
        List<ImportValidationIssue> filteredIssues = issues.stream()
            .filter(issue -> issue.getSeverity().ordinal() <= minSeverity.ordinal())
            .toList();

        // Output results
        if (jsonOutput) {
            outputJson(filteredIssues);
        } else {
            outputText(filteredIssues, scanPath);
        }

        // Return exit code based on errors found
        long errorCount = filteredIssues.stream()
            .filter(issue -> issue.getSeverity() == ImportValidationIssue.Severity.ERROR)
            .count();
        
        return errorCount > 0 ? 1 : 0;
    }

    private Path determineScanPath() {
        if (pathOption != null) {
            return Paths.get(pathOption).toAbsolutePath().normalize();
        }
        if (path != null) {
            return Paths.get(path).toAbsolutePath().normalize();
        }
        return Paths.get(".").toAbsolutePath().normalize();
    }

    private void outputText(List<ImportValidationIssue> issues, Path scanPath) {
        if (!quiet) {
            System.out.println("Checking imports in: " + scanPath);
            System.out.println();
        }
        
        if (issues.isEmpty()) {
            if (!quiet) {
                System.out.println("No import issues found.");
            }
            return;
        }

        // Group by file
        Map<String, List<ImportValidationIssue>> byFile = issues.stream()
            .collect(Collectors.groupingBy(ImportValidationIssue::getFilePath));
        
        for (Map.Entry<String, List<ImportValidationIssue>> entry : byFile.entrySet()) {
            System.out.println(entry.getKey() + ":");
            for (ImportValidationIssue issue : entry.getValue()) {
                System.out.println("  [" + issue.getSeverity() + "] " + issue.getMessage());
                if (issue.getSuggestedFix() != null && !quiet) {
                    System.out.println("    Suggestion: " + issue.getSuggestedFix());
                }
            }
            System.out.println();
        }
        
        // Summary
        if (!quiet) {
            long errorCount = issues.stream()
                .filter(i -> i.getSeverity() == ImportValidationIssue.Severity.ERROR)
                .count();
            long warningCount = issues.stream()
                .filter(i -> i.getSeverity() == ImportValidationIssue.Severity.WARNING)
                .count();
            long infoCount = issues.stream()
                .filter(i -> i.getSeverity() == ImportValidationIssue.Severity.INFO)
                .count();
            
            System.out.println("Summary: " + 
                errorCount + " error(s), " + 
                warningCount + " warning(s), " + 
                infoCount + " info");
        }
    }

    private void outputJson(List<ImportValidationIssue> issues) {
        StringBuilder json = new StringBuilder();
        json.append("[\n");
        
        for (int i = 0; i < issues.size(); i++) {
            ImportValidationIssue issue = issues.get(i);
            json.append("  {\n");
            json.append("    \"filePath\": ").append(jsonString(issue.getFilePath())).append(",\n");
            json.append("    \"message\": ").append(jsonString(issue.getMessage())).append(",\n");
            json.append("    \"severity\": ").append("\"").append(issue.getSeverity()).append("\",\n");
            if (issue.getSuggestedFix() != null) {
                json.append("    \"suggestedFix\": ").append(jsonString(issue.getSuggestedFix())).append(",\n");
            }
            json.append("  }");
            if (i < issues.size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }
        
        json.append("]");
        System.out.println(json.toString());
    }

    private String jsonString(String value) {
        return value.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
}
