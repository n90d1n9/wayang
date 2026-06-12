---
name: write-file
description: Write, create, or edit files on disk. Activate when user asks to create, write, modify, update, refactor, fix, add, or implement code or content in files.
license: Apache-2.0
compatibility: Requires write permissions in the working directory
metadata:
  author: gamelan-team
  version: "1.0"
allowed-tools: read_file write_file apply_patch glob list_dir
---

# Write File Skill

Use this skill when the user wants to create or modify files.

## When to activate
- "create a file", "write X to Y"
- "implement", "add", "fix", "refactor", "update" (when targeting specific files)
- Generating new code files from scratch
- Applying code changes

## Best practices

### For new files
1. Confirm the target path with the user if ambiguous
2. Check if a similar file already exists (use `glob`)
3. Write clean, idiomatic code that matches the project's style
4. Use `write_file` with the complete content

### For editing existing files
1. Always `read_file` first — never edit blind
2. For targeted changes: use `apply_patch` with a minimal unified diff
3. For larger rewrites: use `write_file` with the complete new content
4. Explain what changed and why

### For refactoring
1. Read the current file(s)
2. Identify the change scope
3. Apply changes in the smallest logical units
4. Keep formatting consistent with the existing code

## Safety rules
- Never overwrite files without reading them first
- Never delete files
- Prefer `apply_patch` for small targeted changes over full rewrites
- Always show the user what will be written before writing large files

## Example
User: "add a logger to UserService.java"
→ read_file UserService.java
→ identify import and class sections
→ apply_patch with the logger addition
→ confirm what was changed
