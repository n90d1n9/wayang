---
name: read-file
description: Read file contents from disk. Use when the user asks to view, inspect, show, cat, open, or read any file or directory. Also activate when analyzing code that requires seeing file contents.
license: Apache-2.0
compatibility: Requires filesystem access in the working directory
metadata:
  author: gamelan-team
  version: "1.0"
allowed-tools: read_file list_dir glob
---

# Read File Skill

Use this skill when the user wants to view or inspect file contents.

## When to activate
- "show me the contents of X"
- "read X", "cat X", "open X"
- "what's in X.java / X.py / X.go"
- Analyzing code before making changes

## Instructions

1. Use the `list_dir` tool first if the user refers to a directory or if the path is ambiguous.
2. Use `glob` to find files matching a description (e.g. "all Java test files").
3. Use `read_file` with `start_line`/`end_line` for large files — never read more than needed.
4. After reading, summarize the key points relevant to the user's question.

## File size guidance
- < 200 lines: read the whole file
- 200–500 lines: read relevant sections with line ranges
- > 500 lines: use `search_files` to locate relevant sections first

## Example
User: "show me the UserService class"
→ glob for `**/UserService.java`
→ read_file the result
→ summarize the class structure
