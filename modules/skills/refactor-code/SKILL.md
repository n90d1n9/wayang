---
name: refactor-code
description: Refactor code for clarity, performance, and maintainability. Activate when the user asks to refactor, simplify, clean up, improve, or restructure code without changing behaviour.
license: Apache-2.0
metadata:
  author: gamelan-team
  version: "1.0"
allowed-tools: read_file apply_patch run_command search_files glob
---

# Refactor Code Skill

## When to activate
- "refactor this", "clean this up", "simplify this"
- "this method is too long", "extract this logic"
- "reduce duplication", "make this more readable"
- "apply SOLID principles", "improve the design"

## Refactoring principles (in priority order)

### 1. Read first, change second
ALWAYS read the full file before making changes.
Understand the context: what calls this? What does it call?

### 2. Apply targeted patches
Use `apply_patch` for small changes.
Use `write_file` only for complete rewrites of short files.
Never rewrite a file you haven't fully read.

### 3. Common refactoring patterns

**Extract method**: when a block of code has a single purpose
```
before: 50-line method doing 5 things
after:  5 focused methods, each doing 1 thing
```

**Replace magic numbers with constants**
```java
// before
if (status == 3) { ... }
// after
if (status == OrderStatus.SHIPPED.code) { ... }
```

**Introduce null object pattern** to eliminate null checks:
```java
// before: check null everywhere
// after: return a NullUser/EmptyList that has safe defaults
```

**Flatten arrow code** (nested if/else) with early returns:
```java
// before: deeply nested
if (a) { if (b) { if (c) { doThing(); } } }
// after: guard clauses
if (!a) return;
if (!b) return;
if (!c) return;
doThing();
```

### 4. Test before and after
Run the test suite before AND after every refactoring.
If no tests exist, write them first — a refactoring without tests is a rewrite.

### 5. Commit each logical step
After each successful refactoring, confirm with the user before moving on.

## Anti-patterns to remove
- God classes (> 500 lines doing everything)
- Primitive obsession (use value objects)
- Feature envy (method uses another class more than its own)
- Shotgun surgery (one change requires many files to update)
- Dead code (unused methods, commented-out blocks)
