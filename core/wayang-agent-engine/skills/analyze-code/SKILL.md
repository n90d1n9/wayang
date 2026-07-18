---
name: analyze-code
description: Analyze, explain, review, and reason about code. Activate for code review, architecture explanation, bug investigation, performance analysis, security review, or understanding unfamiliar codebases.
license: Apache-2.0
metadata:
  author: gamelan-team
  version: "1.0"
allowed-tools: read_file search_files glob list_dir
---

# Analyze Code Skill

Use this skill when the user wants to understand, review, or investigate code.

## When to activate
- "explain this code", "what does X do"
- "review my code", "find bugs in..."
- "why is X slow / failing / wrong"
- "how does the architecture work"
- "is this code secure"

## Analysis workflow

### 1. Orient yourself
```
list_dir .
read_file README.md  (if exists)
```

### 2. Identify key files
- Entry points: `main()`, `Application.java`, `index.js`, etc.
- Configuration: `pom.xml`, `package.json`, `build.gradle`
- Core logic: search for key class/function names

### 3. Deep dive
- Read the most relevant files completely
- Trace data flow from input to output
- Look for: error handling, edge cases, performance bottlenecks

### 4. Report findings

Structure your analysis as:
1. **Summary** — what this code does (2–3 sentences)
2. **Strengths** — what's done well
3. **Issues** — bugs, risks, anti-patterns (with file:line references)
4. **Suggestions** — concrete improvements with code examples

## Code review checklist
- [ ] Correctness: does it do what it claims?
- [ ] Error handling: are failures handled gracefully?
- [ ] Security: any injection, auth bypass, or data exposure risks?
- [ ] Performance: O(n²) loops, N+1 queries, blocking calls?
- [ ] Maintainability: clear naming, no magic numbers, documented?
- [ ] Tests: are edge cases covered?

See [references/patterns.md](references/patterns.md) for common anti-patterns.
