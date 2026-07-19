---
name: run-command
description: Execute shell commands, build tools, test runners, and scripts. Activate when the user asks to run, build, test, execute, compile, install, start, or stop something.
license: Apache-2.0
compatibility: Requires bash available in PATH
metadata:
  author: gamelan-team
  version: "1.0"
allowed-tools: run_command list_dir read_file
---

# Run Command Skill

Use this skill when the user wants to execute commands or scripts.

## When to activate
- "run the tests", "build the project", "start the server"
- "execute this script", "run mvn/gradle/npm/cargo"
- Verifying that changes work (after writing code)
- Debugging by running commands

## Command safety guidelines

**Always:**
- Show the command before running it
- Capture and interpret the output
- Run in the appropriate working directory

**Never run:**
- Commands that delete data (`rm -rf`, `DROP TABLE`, etc.)
- Commands that make network requests without user awareness
- Long-running daemons without user confirmation

## Common development commands

### Java / Maven
```
mvn compile                    # Compile
mvn test -Dtest=ClassName      # Run specific tests
mvn package -DskipTests        # Build JAR
mvn quarkus:dev               # Start Quarkus dev mode
```

### JavaScript / Node
```
npm install                    # Install deps
npm test                       # Run tests
npm run build                  # Build
```

### Python
```
python -m pytest tests/        # Run tests
python -m pip install -e .     # Install package
```

### Shell / Git
```
git status                     # Check changes
git diff HEAD                  # View diff
git log --oneline -10          # Recent commits
```

## Error handling
- If a command fails, read the error output carefully
- Suggest fixes or alternative approaches
- Don't retry the same failing command without changing something
