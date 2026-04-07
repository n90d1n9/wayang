# Skills Validator

A Java-based validator for the [Agent Skills Specification](https://agentskills.io/specification).

## Overview

This tool validates skill directories against the official Agent Skills specification requirements:

- **SKILL.md structure**: Valid YAML frontmatter and Markdown body
- **Required fields**: `name` and `description`
- **Name validation**: Lowercase alphanumeric + hyphens, matches directory name, no leading/trailing/consecutive hyphens
- **Description validation**: 1-1024 characters, non-empty
- **Optional fields**: `license`, `compatibility`, `metadata`, `allowed-tools`
- **Directory structure**: Checks for optional `scripts/`, `references/`, `assets/` directories

## Building

### Build with Maven

```bash
cd tools/skills-validator
mvn clean package
```

This creates an executable JAR: `target/skills-validator.jar`

### Run Tests

```bash
mvn test
```

## Usage

### Command Line

```bash
java -jar skills-validator.jar [SKILLS_DIR]
```

**Arguments:**
- `SKILLS_DIR` - Path to skills directory (default: current directory)

**Options:**
- `-h, --help` - Show help message
- `-v, --version` - Show version

**Exit Codes:**
- `0` - All skills valid
- `1` - Validation errors found
- `2` - Invalid usage or I/O error

### Examples

Validate current directory:
```bash
java -jar skills-validator.jar
```

Validate a specific directory:
```bash
java -jar skills-validator.jar ./skills
```

Validate gollek skills:
```bash
java -jar skills-validator.jar /path/to/gollek/skills
```

### Java API

```java
import tech.kayys.gollek.skills.validator.SkillValidator;
import tech.kayys.gollek.skills.validator.ValidationResult;
import java.nio.file.Paths;

// Validate single skill
SkillValidator validator = new SkillValidator();
ValidationResult result = validator.validate(Paths.get("my-skill"));

if (result.isValid()) {
    System.out.println("✓ Skill is valid");
} else {
    System.out.println("✗ Errors:");
    for (String error : result.getErrors()) {
        System.out.println("  - " + error);
    }
}
```

```java
import tech.kayys.gollek.skills.validator.SkillsValidator;
import java.nio.file.Paths;

// Validate multiple skills
SkillsValidator validator = new SkillsValidator();
boolean allValid = validator.validateAll(Paths.get("skills"));

SkillsValidator.ValidationSummary summary = validator.getSummary();
System.out.println("Valid: " + summary.getValidSkills());
System.out.println("Errors: " + summary.getErrorCount());
System.out.println("Warnings: " + summary.getWarningCount());
```

## Project Structure

```
tools/skills-validator/
├── pom.xml                                    # Maven configuration
├── src/
│   ├── main/java/tech/kayys/gollek/skills/validator/
│   │   ├── ValidationResult.java              # Result data class
│   │   ├── SkillValidator.java                # Single skill validator
│   │   ├── SkillsValidator.java               # Multi-skill orchestrator
│   │   └── SkillsValidatorCLI.java            # Command-line interface
│   └── test/java/.../
│       └── SkillValidatorTest.java            # Unit tests
└── README.md                                  # This file
```

## Implementation Details

### SkillValidator

Validates a single skill directory:
- Checks SKILL.md existence
- Extracts and validates YAML frontmatter
- Validates required fields with specification rules
- Checks optional fields
- Validates body content

### SkillsValidator

Orchestrates validation of multiple skills:
- Finds all skill directories
- Validates each using SkillValidator
- Collects results and statistics
- Generates summary

### SkillsValidatorCLI

Provides command-line interface:
- Parses arguments
- Invokes SkillsValidator
- Formats output with colors and structure
- Returns appropriate exit codes

## Dependencies

- **snakeyaml** (2.2) - YAML parsing
- **JUnit 5** (5.10.2) - Testing framework

## Specification Compliance

This validator implements the complete specification from:
https://agentskills.io/specification

Version: 1.0

## License

Same as gollek parent project
